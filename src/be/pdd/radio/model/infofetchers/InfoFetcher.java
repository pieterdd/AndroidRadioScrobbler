package be.pdd.radio.model.infofetchers;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import android.util.Log;

/**
 * Abstract class that provides a standardized way to fetch song info for
 * any given radio station that supports it.
 * 
 * Derivates of this class must be thread-safe. They must also notify all
 * observers when a new track is detected. 
 * 
 * @author Pieter De Decker
 */
public abstract class InfoFetcher extends java.util.Observable {
	protected String _artist = "";
	protected String _title = "";
	protected String _lastArtist = "";
	protected String _lastTitle = "";
	boolean _allowUpdates = false;
	private Timer _infoFetchTimer = new Timer();
	
	/** Internal sync lock for fields listed above. See http://stackoverflow.com/a/442601/217649 for reasoning. */
	protected Object _songInfoLock = new Object();
	
	/** Tag for log messages originating from static functions in this class. */ 
	private final static String LOGID = "SongInfoFetcher";
	
	/**
	 * Getter for the artist of the current song. Returns empty string if song
	 * information is currently unavailable. 
	 */
	public String getCurrentArtist() {
		String returnValue = "";
		synchronized (_songInfoLock) {
			returnValue = _artist;
		}
		return returnValue;
	}
	
	/**
	 * Getter for the name of the current song. Returns empty string if song
	 * information is currently unavailable. 
	 */
	public String getCurrentTitle() {
		String returnValue = "";
		synchronized (_songInfoLock) {			
			returnValue = _title;
		}
		return returnValue;
	}
	
	/**
	 * Getter for the artist of the previous song. Returns empty string if song
	 * information is currently unavailable. 
	 */
	public String getLastArtist() {
		String returnValue = "";
		synchronized (_songInfoLock) {			
			returnValue = _lastArtist;
		}
		return returnValue;
	}
	
	/**
	 * Getter for the name of the previous song. Returns empty string if song
	 * information is currently unavailable. 
	 */
	public String getLastTitle() {
		String returnValue = "";
		synchronized (_songInfoLock) {			
			returnValue = _lastTitle;
		}
		return returnValue;
	}
	
	/**
	 * Returns true if the artist or title of the current song is a
	 * non-empty field.
	 */
	public boolean currentSongIsValid() {
		boolean returnValue = false;
		synchronized (_songInfoLock) {
			returnValue = (!_artist.equals("") || !_title.equals(""));
		}
		return returnValue;
	}
	
	/**
	 * Returns true if the artist or title of the previous song is a
	 * non-empty field.
	 */
	public boolean lastSongIsValid() {
		boolean returnValue = false;
		synchronized (_songInfoLock) {
			returnValue = (!_lastArtist.equals("") || !_lastTitle.equals(""));
		}
		return returnValue;
	}

	/** Triggers a reload of this station's song data. Must be thread-safe! */
	public abstract void fetchSongInfo();
	
	/**
	 * Downloads and parses an XML file from a given URI. This is a convenience
	 * function that is used by many song info fetchers. 
	 */
	protected static Element getXMLTree(String uri) {
		try {
			// Put in the HTTP GET request
			DefaultHttpClient client = new DefaultHttpClient();
			HttpResponse resp = client.execute(new HttpGet(uri));
			if (resp.getStatusLine().getStatusCode() != 200) {
				Log.e(LOGID, "Could not download XML file. URI was " + uri + ".");
				return null;
			}

			// Read the XML file
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(resp.getEntity().getContent());
			doc.getDocumentElement().normalize();

			// Return the root
			return doc.getDocumentElement();
		} catch (SAXException e) {
			Log.e(LOGID, "Could not process XML file. URI was " + uri + ".");
		} catch (IOException e) {
			Log.e(LOGID, "Could not process XML file. URI was " + uri + ".");
		} catch (ParserConfigurationException e) {
			Log.e(LOGID, "Could not process XML file. URI was " + uri + ".");
		}
		
		return null;
	}
	
	/**
	 * Starts fetching song info at regular intervals until told to quit.
	 */
	public void startFetching() { 
		// Disregard call if we're already fetching updates. Otherwise
		// set it to true and go on.
		synchronized (_songInfoLock) {
			if (_allowUpdates)
				return;
			_allowUpdates = true;
		}
		
		fetchSongInfo();
		scheduleUpdate();
	}
	
	/**
	 * Stops fetching song info at regular intervals.
	 */
	public void stopFetching() {
		synchronized (_songInfoLock) {
			_allowUpdates = false;
		}
	}
	
	/**
     * Schedules a song info update. This update will be cancelled if the media player has
     * stopped since then.
     */
    private void scheduleUpdate() {
    	// If the update strategy is given, start fetching song info updates.
    	_infoFetchTimer.schedule(new TimerTask() {
    		@Override
    		public void run() {
    			// Proceed with the update unless the user has cancelled them
    			boolean allowUpdate = false;
    			synchronized (_songInfoLock) {
    				allowUpdate = _allowUpdates;
				}
    			
    			if (allowUpdate) {
    				fetchSongInfo();
    				scheduleUpdate();
    			}
    		}
    	}, 10000);
    }
}
