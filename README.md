Features
========

- Streaming several SomaFM radio channels
- Displaying song info for supported stations
- Scrobbling tracks to Last.fm using the Simple Last.fm Scrobbler app, if installed

Limitations
===========

- The media player and scrobbler might suddenly stop playing if the application is not in the foreground. One might want to set up a [Service](http://developer.android.com/guide/topics/media/mediaplayer.html) to deal with this limitation.

How to compile
==============

This was originally an Eclipse project, but I had to remove the project files because it didn't work on other machines. You might wanna poke around in Eclipse's import wizard to load the project.