package com.vergo.simulwatchserver;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DataListener;
import com.corundumstudio.socketio.listener.DisconnectListener;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Created by Varun on 14/04/2017.
 */
public class Launcher {
    private final static Logger logger = LoggerFactory.getLogger(Launcher.class);
    private static ServerQueue serverQueue = new ServerQueue();
    private static SocketDetails masterSocket = new SocketDetails();

    public static void main(String[] args) throws InterruptedException {
        Configuration configuration = new Configuration();
        configuration.setHostname("168.235.65.43");
        configuration.setPort(2499);

        final SocketIOServer server = new SocketIOServer(configuration);

        server.addConnectListener(new ConnectListener() {
            @Override
            public void onConnect(SocketIOClient socketIOClient) {
                logger.info("Received connection from {}", socketIOClient.getSessionId());
                GsonBuilder gsonBuilder = new GsonBuilder();
                gsonBuilder.registerTypeAdapter(ServerQueue.class, new ServerQueueSerializer());
                logger.info("Sending initial server queue data {}, length : {}", gsonBuilder.create().toJson(serverQueue), serverQueue.getServerQueueLength());
                socketIOClient.sendEvent("initialQueueData", gsonBuilder.create().toJson(serverQueue));

                if(masterSocket.getSocketId() != null && !isSocketMaster(socketIOClient.getSessionId(), masterSocket) && serverQueue.getServerQueueLength() > 0) {
                    server.getClient(masterSocket.getSocketId()).sendEvent("getCurrentVideoElapsedTime", socketIOClient.getSessionId());
                }
            }
        });

        server.addDisconnectListener(new DisconnectListener() {
            @Override
            public void onDisconnect(SocketIOClient socketIOClient) {
                if(isSocketMaster(socketIOClient.getSessionId(), masterSocket)) {
                    logger.info("Master socket has disconnected, all events will be stopped");
                    serverQueue.clearServerQueue();
                    masterSocket.clearSocketDetails();
                    server.getBroadcastOperations().sendEvent("killEverything");
                }
            }
        });

        server.addEventListener("returnCurrentVideoElapsedTime", ReturnCurrentVideoElapsedTimeObject.class, new DataListener<ReturnCurrentVideoElapsedTimeObject>() {
            @Override
            public void onData(SocketIOClient client, ReturnCurrentVideoElapsedTimeObject data, AckRequest ackSender) throws Exception {
                returnCurrentVideoElapsedTimeCallback(data, server, serverQueue);
            }
        });

        server.addEventListener("videoPaused", JsonObject.class, new DataListener<JsonObject>() {
            @Override
            public void onData(SocketIOClient client, JsonObject data, AckRequest ackSender) throws Exception {
                videoPlayerChangedCallback("videoPaused", client, server, masterSocket, null);
            }
        });

        server.addEventListener("videoPlayed", JsonObject.class, new DataListener<JsonObject>() {
            @Override
            public void onData(SocketIOClient client, JsonObject data, AckRequest ackSender) throws Exception {
                videoPlayerChangedCallback("videoPlayed", client, server, masterSocket, null);
            }
        });

        server.addEventListener("videoSeeked", ReturnCurrentVideoElapsedTimeObject.class, new DataListener<ReturnCurrentVideoElapsedTimeObject>() {
            @Override
            public void onData(SocketIOClient client, ReturnCurrentVideoElapsedTimeObject data, AckRequest ackSender) throws Exception {
                videoPlayerChangedCallback("videoSeeked", client, server, masterSocket, data);
            }
        });

        server.addEventListener("setNickname", String.class, new DataListener<String>() {
            @Override
            public void onData(SocketIOClient socketIOClient, String nickname, AckRequest ackRequest) throws Exception {
                setNicknameCallback(nickname, socketIOClient, logger, masterSocket);
            }
        });

        server.addEventListener("videoFinishedPlaying", JsonObject.class, new DataListener<JsonObject>() {
            @Override
            public void onData(SocketIOClient client, JsonObject data, AckRequest ackSender) throws Exception {
                videoFinishedPlayingCallback(client, server, serverQueue, logger, masterSocket);
            }
        });

        server.addEventListener("newVideoQueue", ServerQueueObject.class, new DataListener<ServerQueueObject>() {
            @Override
            public void onData(SocketIOClient client, ServerQueueObject newServerQueueObject, AckRequest ackSender) throws Exception {
                newVideoQueueCallback(masterSocket, serverQueue, newServerQueueObject, server, logger);
            }
        });

        server.start();
    }

    // function that deals with pause, play and seek callbacks of video player
    public static void videoPlayerChangedCallback(String videoAction, SocketIOClient client, SocketIOServer server, SocketDetails masterSocket, ReturnCurrentVideoElapsedTimeObject data) {
        if(isSocketMaster(client.getSessionId(), masterSocket)) {

            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("masterSocketID", masterSocket.getSocketId().toString());

            String sendEvent = "";
            switch (videoAction) {
                case "videoPaused" : sendEvent = "pauseVideoForAllClients";
                    break;
                case "videoPlayed" : sendEvent = "playVideoForAllClients";
                    break;
                case "videoSeeked" :
                    sendEvent = "seekVideoForAllClients";
                    jsonObject.addProperty("elapsedTime", data.getElapsedTime());
                    break;
            }

            server.getBroadcastOperations().sendEvent(sendEvent, jsonObject.toString());
        }
    }

    public static void videoFinishedPlayingCallback(SocketIOClient client, SocketIOServer server, ServerQueue serverQueue, Logger logger, SocketDetails masterSocket) throws InterruptedException {
        if(isSocketMaster(client.getSessionId(), masterSocket)) {
            logger.info("Detected master finished playing, loading next video in queue");
            Thread.sleep(5000);
            ServerQueueObject nextVideo = serverQueue.getNextQueuedVideo();

                    /*
                    3 cases to deal with here -
                    1. music queue is empty and we try getting next video - will return null
                    2. music queue has one last element left and after getting that, queue is empty - queue will return null
                    3. music queue has more than one element left
                     */

            JsonObject jsonObject = new JsonObject();

            if(nextVideo != null) {
                GsonBuilder gsonBuilder = new GsonBuilder();
                gsonBuilder.registerTypeAdapter(ServerQueue.class, new ServerQueueSerializer());

                jsonObject.addProperty("videoQueue", gsonBuilder.create().toJson(serverQueue));
                jsonObject.addProperty("username", nextVideo.getUsername());
                jsonObject.addProperty("videoURL", nextVideo.getVideoURL());
                logger.info("Sending new video {} to play", nextVideo.getVideoURL());
            }

            server.getBroadcastOperations().sendEvent("playNextVideoInQueue", jsonObject.toString());
        }
    }

    public static void setNicknameCallback(String nickname, SocketIOClient socketIOClient, Logger logger, SocketDetails masterSocket) {
        if(nickname.equals("amadeus")) {
            logger.info("New master socket confirmed. This too must be the choice of Steins;Gate");
            masterSocket.setSocketDetails(socketIOClient.getSessionId());
        } else {
            logger.info("Incorrect attempt to set master with nickname {}", nickname);
        }
    }

    public static void newVideoQueueCallback(SocketDetails masterSocket, ServerQueue serverQueue, ServerQueueObject newServerQueueObject, SocketIOServer server, Logger logger) {
        if(masterSocket.getSocketId() == null) {
            logger.info("No master socket set, request is ignored");
            return;
        }

        logger.info("New video added to queue : {}", newServerQueueObject.getVideoURL());
        boolean playVideo = false;

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("username", newServerQueueObject.getUsername());
        jsonObject.addProperty("videoURL", newServerQueueObject.getVideoURL());

        if(serverQueue.getServerQueueLength() == 0) {
            playVideo = true;
        }

        serverQueue.queueNewVideo(newServerQueueObject.getUsername(), newServerQueueObject);

        jsonObject.addProperty("playVideo", playVideo);

        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(ServerQueue.class, new ServerQueueSerializer());
        jsonObject.addProperty("videoQueue", gsonBuilder.create().toJson(serverQueue));

        server.getBroadcastOperations().sendEvent("newVideoQueueForClient", jsonObject.toString());
    }

    public static void returnCurrentVideoElapsedTimeCallback(ReturnCurrentVideoElapsedTimeObject data, SocketIOServer server, ServerQueue serverQueue) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("elapsedTime", data.getElapsedTime());

        ServerQueueObject currentObjectQueued = serverQueue.peekCurrentQueuedVideo();

        jsonObject.addProperty("username", currentObjectQueued.getUsername());
        jsonObject.addProperty("videoURL", currentObjectQueued.getVideoURL());

        server.getClient(data.getNewClientSocketId()).sendEvent("playCurrentVideoForNewClient", jsonObject.toString());
    }


    public static boolean isSocketMaster(UUID socketId, SocketDetails masterSocket) {
        if(masterSocket.getSocketId() == null) {
            return false;
        }
        return masterSocket.getSocketId().equals(socketId);
    }
}
