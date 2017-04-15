package com.vergo.simulwatchserver;

import java.util.UUID;

/**
 * Created by Varun on 14/04/2017.
 */
public class SocketResponses {
}

class ReturnCurrentVideoElapsedTimeObject {
    private double elapsedTime;
    private UUID newClientSocketId;

    public ReturnCurrentVideoElapsedTimeObject() {

    }

    public ReturnCurrentVideoElapsedTimeObject(double elapsedTime, UUID newClientSocketId) {
        super();
        this.elapsedTime = elapsedTime;
        this.newClientSocketId = newClientSocketId;
    }

    public double getElapsedTime() {
        return elapsedTime;
    }

    public void setElapsedTime(double elapsedTime) {
        this.elapsedTime = elapsedTime;
    }

    public UUID getNewClientSocketId() {
        return newClientSocketId;
    }

    public void setNewClientSocketId(UUID newClientSocketId) {
        this.newClientSocketId = newClientSocketId;
    }
}
