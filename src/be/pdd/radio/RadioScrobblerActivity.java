package be.pdd.radio;

import java.io.IOException;
import java.util.Observable;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnInfoListener;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import be.pdd.radio.model.RadioChannel;
import be.pdd.radio.model.infofetchers.InfoFetcher;
import be.pdd.radio.model.infofetchers.SomaFmFetcher;
import be.pieterdedecker.androidtest.R;

public class RadioScrobblerActivity extends Activity implements java.util.Observer {
	private MediaPlayer _mediaPlayer;
	private RadioChannel[] listEntries = {
			new RadioChannel("Cliqhop", "http://ice.somafm.com/cliqhop", new SomaFmFetcher("cliqhop")),
			new RadioChannel("Drone Zone", "http://ice.somafm.com/dronezone", new SomaFmFetcher("dronezone")),
			new RadioChannel("Groove Salad", "http://ice.somafm.com/groovesalad", new SomaFmFetcher("groovesalad")),
			new RadioChannel("Indie Pop Rocks", "http://ice.somafm.com/indiepop", new SomaFmFetcher("indiepop")),
			new RadioChannel("Digitalis", "http://ice.somafm.com/digitalis", new SomaFmFetcher("digitalis")),
			new RadioChannel("Covers", "http://ice.somafm.com/covers", new SomaFmFetcher("covers")),
			new RadioChannel("Cliqhop (no song info)", "http://ice.somafm.com/cliqhop", null),
	};
	private RadioChannel _activeChannel = null;
	private TextView _lblStatus;
	
	/** Sync lock for _activeChannel. We cannot sync on the object itself because it can be null. */
	private Object _channelSyncLock = new Object();
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
         
        final RadioScrobblerActivity selfReference = this;
        final ListView lv = (ListView) findViewById(R.id.lstTest);
        lv.setTextFilterEnabled(true);
        lv.setAdapter(new ArrayAdapter<RadioChannel>(this, R.layout.simple_list_item_1, listEntries));
        lv.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adapterView, View view, int pos, long arg3) {
				RadioChannel rc = (RadioChannel) lv.getItemAtPosition(pos);
				assert(rc != null);
	        	_lblStatus.setText(getString(R.string.starting) + " " + rc.getFriendlyName() + "...");
	        	synchronized (_channelSyncLock) {
	        		if (_activeChannel != null && _activeChannel.getInfoFetcher() != null) {
	        			_activeChannel.getInfoFetcher().stopFetching();
	        			_activeChannel.getInfoFetcher().deleteObserver(selfReference);
	        		}
	        		_activeChannel = rc;
	        		if (_activeChannel.getInfoFetcher() != null)
	        			_activeChannel.getInfoFetcher().addObserver(selfReference);
	        		else
	        			_lblStatus.setText(getString(R.string.songInfoUnavailable));
				}
	        	startMediaPlayer(rc.getUrl());
			}
        });
        
        _mediaPlayer = new MediaPlayer();
        _lblStatus = (TextView) findViewById(R.id.lblStatus);
    }
    
    /** 
     * Starts up the media player asynchronously
     * @param url		The stream URL to load.
     * @param infoFetcher 
     */
    private void startMediaPlayer(String url) {
    	try {
    		_mediaPlayer.reset();
    		
			_mediaPlayer.setDataSource(url);
			_mediaPlayer.prepareAsync();
			_mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
				@Override
				public void onPrepared(MediaPlayer mp) {
					_mediaPlayer.start();
					
					RadioChannel rc;
					synchronized (_channelSyncLock) {
						rc = _activeChannel;
					}
					assert(rc != null);
					if (rc.getInfoFetcher() != null)
						rc.getInfoFetcher().startFetching();		
				}
			});
			_mediaPlayer.setOnInfoListener(new OnInfoListener() {
				@Override
				public boolean onInfo(MediaPlayer mp, int what, int extra) {
					if (what == MediaPlayer.MEDIA_INFO_BUFFERING_START)
						Toast.makeText(getApplicationContext(), getString(R.string.buffering), Toast.LENGTH_SHORT).show();
					else if (what == MediaPlayer.MEDIA_INFO_BUFFERING_END)
						Toast.makeText(getApplicationContext(), getString(R.string.resumingPlayback), Toast.LENGTH_SHORT).show();
					return true;
				}
			});
		} catch (IllegalStateException e) {
			_lblStatus.setText(getString(R.string.playbackFailed));
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			_lblStatus.setText(getString(R.string.playbackFailed));
		} catch (IOException e) {
			_lblStatus.setText(getString(R.string.playbackFailed));
		}
    }
    
	/**
	 * Sends a Last.fm scrobble notification for a given song to the Simple Last.fm
	 * Scrobbler on the GUI thread, if installed.
	 * 
	 * See https://github.com/tgwizard/sls/wiki/Developer%27s-API for instructions on how
	 * to use the Simple Last.fm Scrobbler API.
	 * 
	 * @param state		One of the states described in the API doc. Will likely be 0 (track
	 * 					started playing) or 3 (track finished playing).
	 */
	protected void scrobbleTrack(final String artist, final String title, final int state) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Intent bCast = new Intent("com.adam.aslfms.notify.playstatechanged");
				bCast.putExtra("state", state);
				bCast.putExtra("app-name", getString(R.string.app_name));
				bCast.putExtra("app-package", getClass().getPackage().getName());
				bCast.putExtra("artist", artist);
				bCast.putExtra("album", "");
				bCast.putExtra("track", title);
				bCast.putExtra("duration", 0);
				sendBroadcast(bCast);
			}
		});
	}
    
    /**
     * Activity destructor.
     */
    public void onDestroy() {
    	_mediaPlayer.release();
    	super.onDestroy();
    }

    /**
     * Will be called when the active InfoFetcher detects a new song.
     */
	@Override
	public void update(Observable observable, Object data) {
		// Prepare some information for the Last.fm scrobbler
		if (!(observable instanceof InfoFetcher))
			return;
		
		// Note that InfoFetcher has been made thread-safe, so we don't
		// need to lock anything up here.
		InfoFetcher ifetch = (InfoFetcher) observable;
		
		// If there was a song that just finished playing, scrobble it in
		// a COMPLETE state. Scrobble the new song in a START state.
		if (ifetch.lastSongIsValid())
			scrobbleTrack(ifetch.getLastArtist(), ifetch.getLastTitle(), 3);
		if (ifetch.currentSongIsValid())
			scrobbleTrack(ifetch.getCurrentArtist(), ifetch.getCurrentTitle(), 0);
		
		// Schedule the song changes to be reflected in the GUI
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				RadioChannel rc2 = null;
				synchronized (_channelSyncLock) {
					rc2 = _activeChannel;
				}
				
				// Update the "Song Title by Artist" indicator
				TextView lblStatus = (TextView) findViewById(R.id.lblStatus);
				InfoFetcher ifetch = rc2.getInfoFetcher();
				if (!ifetch.getCurrentTitle().equals("") || !ifetch.getCurrentArtist().equals(""))
					lblStatus.setText(ifetch.getCurrentTitle() + " by " + ifetch.getCurrentArtist());
				else
					lblStatus.setText(getString(R.string.noSongPlaying));
			}
		});
	}
}