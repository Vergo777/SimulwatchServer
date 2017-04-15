package com.vergo.simulwatchserver;

import java.util.UUID;

/**
 * Created by Varun on 14/04/2017.
 */
public class SocketDetails {
    private UUID socketId;

    public UUID getSocketId() {
        return socketId;
    }

    public void setSocketDetails(UUID socketId) {
        this.socketId = socketId;
    }

    public void clearSocketDetails() {
        socketId = null;
    }
}
