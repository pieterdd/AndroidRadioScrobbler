package be.pdd.radio.model.infofetchers;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import android.util.Log;

/**
 * A song info fetcher that supports SomaFM's now playing system.
 * @author Pieter De Decker
 */
public class SomaFmFetcher extends InfoFetcher {
	private String _stationId;
	
	/**
	 * @param stationId	This station's identifier. Will be used to fetch the station's XML data
	 * 					at http://somafm.com/songs/[STATION ID].xml.
	 */
	public SomaFmFetcher(String stationId) {
		_stationId = stationId;
	}

	@Override
	public void fetchSongInfo() {
		// Get the root element of song info file. Cancel if something went wrong.
		Element re = getXMLTree("http://www.somafm.com/songs/" + _stationId + ".xml");
		if (re == null)
			return;

		if (!re.getNodeName().equals("songs")) {
			Log.e(this.toString(), "Station file contained unexpected root node. Root was " + re.getNodeName() + ", station ID " + _stationId + ".");
			return;
		}

		// The first child node should be <song>, which contains all the info we need.
		if (!re.hasChildNodes() || !re.getChildNodes().item(1).getNodeName().equals("song")) {
			Log.e(this.toString(), "Station file contained unexpected structure. Station ID was " + _stationId + ".");
			return;
		}
		Node songNode = re.getChildNodes().item(1);
		NodeList songInfoList = songNode.getChildNodes();
		if (songInfoList.item(1).getNodeName().equals("title") && songInfoList.item(3).getNodeName().equals("artist")) {
			String newTitle = songInfoList.item(1).getFirstChild().getNodeValue();
			String newArtist = songInfoList.item(3).getFirstChild().getNodeValue();
			
			// We can now safely fetch the new song info
			synchronized (_songInfoLock) {
				// Only new songs should trigger an update
				if (!newTitle.equals(_title) || !newArtist.equals(_artist)) {
					_lastArtist = _artist;
					_lastTitle = _title;
					_artist = songInfoList.item(3).getFirstChild().getNodeValue();
					_title = songInfoList.item(1).getFirstChild().getNodeValue();
					setChanged();
				}
			}
			
			// Notify our observers if anything changed. Note that since this is the only place where we'll be
			// using observers, we don't need to synchronize here.
			notifyObservers();
		}
	}
}
