package be.pdd.radio.model;

import be.pdd.radio.model.infofetchers.InfoFetcher;

/**
 * Represents a radio channel with a friendly name, a streaming URL and
 * an optional "Now Playing" update strategy. This class is thread-safe
 * because its member functions are all inspectors.
 * 
 * @author Pieter De Decker
 */
public class RadioChannel {
	private String _friendlyName;
	private String _url;
	private InfoFetcher _infoFetcher;
	
	/**
	 * @param friendlyName		The radio station's name as shown in the GUI.
	 * @param url				The stream URL.
	 * @param infoFetcher		The strategy that will be used to load song info. Can be null.
	 */
	public RadioChannel(String friendlyName, String url, InfoFetcher infoFetcher) {
		_friendlyName = friendlyName;
		_url = url;
		_infoFetcher = infoFetcher;
	}
	
	public String toString() {
		return _friendlyName;
	}
	
	public String getFriendlyName() {
		return _friendlyName;
	}
	
	public String getUrl() {
		return _url;
	}
	
	public InfoFetcher getInfoFetcher() {
		return _infoFetcher;
	}
}
