package com.akaalapps.background_media_player;

import java.util.HashMap;
import java.util.Map;

public class MediaItem {
    final String title;
    final String artist;
    final String album;
    final String albumArt;
    final String source;

    private MediaItem(String title, String artist, String album, String albumArt, String source) {
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.albumArt = albumArt;
        this.source = source;
    }

    Map<String,String> toMap(){
     Map<String ,String> item = new HashMap<>();
     item.put("title",title);
     item.put("artist",artist);
     item.put("album",album);
     item.put("albumArt",albumArt);
     item.put("source",source);
     return item;
    }

    static MediaItem fromMap(Map<String,String> item){
        return new MediaItem(item.get("title"),item.get("artist"),item.get("album"),item.get("albumArt"),item.get("source"));
    }
}
