package com.vergo.simulwatchserver;

/**
 * Created by Varun on 14/04/2017.
 */
public class ServerQueueObject {
    private String username;
    private String videoURL;

    public ServerQueueObject() {

    }

    public ServerQueueObject(String username, String videoURL) {
        super();
        this.username = username;
        this.videoURL = videoURL;
}

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getVideoURL() {
        return videoURL;
    }

    public void setVideoURL(String videoURL) {
        this.videoURL = videoURL;
    }
}
