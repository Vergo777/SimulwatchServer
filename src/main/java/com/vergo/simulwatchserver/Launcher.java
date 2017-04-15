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

                if(masterSocket.getSocketId() != null && !isSocketMaster(socketIOClient.getSessionId()) && serverQueue.getServerQueueLength() > 0) {
                    server.getClient(masterSocket.getSocketId()).sendEvent("getCurrentVideoElapsedTime", socketIOClient.getSessionId());
                }
            }
        });

        server.addEventListener("returnCurrentVideoElapsedTime", ReturnCurrentVideoElapsedTimeObject.class, new DataListener<ReturnCurrentVideoElapsedTimeObject>() {
            @Override
            public void onData(SocketIOClient client, ReturnCurrentVideoElapsedTimeObject data, AckRequest ackSender) throws Exception {
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("elapsedTime", data.getElapsedTime());

                ServerQueueObject currentObjectQueued = serverQueue.peekCurrentQueuedVideo();

                jsonObject.addProperty("username", currentObjectQueued.getUsername());
                jsonObject.addProperty("videoURL", currentObjectQueued.getVideoURL());

                server.getClient(data.getNewClientSocketId()).sendEvent("playCurrentVideoForNewClient", jsonObject.toString());
            }
        });

        server.addDisconnectListener(new DisconnectListener() {
            @Override
            public void onDisconnect(SocketIOClient socketIOClient) {
                if(isSocketMaster(socketIOClient.getSessionId())) {
                    logger.info("Master socket has disconnected, all events will be stopped");
                    serverQueue.clearServerQueue();
                    masterSocket.clearSocketDetails();
                    server.getBroadcastOperations().sendEvent("killEverything");
                }
            }
        });

        server.addEventListener("videoPaused", JsonObject.class, new DataListener<JsonObject>() {
            @Override
            public void onData(SocketIOClient client, JsonObject data, AckRequest ackSender) throws Exception {
                if(isSocketMaster(client.getSessionId())) {
                    server.getBroadcastOperations().sendEvent("pauseVideoForAllClients");
                }
            }
        });

        server.addEventListener("videoPlayed", JsonObject.class, new DataListener<JsonObject>() {
            @Override
            public void onData(SocketIOClient client, JsonObject data, AckRequest ackSender) throws Exception {
                if(isSocketMaster(client.getSessionId())) {
                    server.getBroadcastOperations().sendEvent("playVideoForAllClients");
                }
            }
        });

        server.addEventListener("videoSeeked", ReturnCurrentVideoElapsedTimeObject.class, new DataListener<ReturnCurrentVideoElapsedTimeObject>() {
            @Override
            public void onData(SocketIOClient client, ReturnCurrentVideoElapsedTimeObject data, AckRequest ackSender) throws Exception {
                if(isSocketMaster(client.getSessionId())) {

                    JsonObject jsonObject = new JsonObject();
                    jsonObject.addProperty("elapsedTime", data.getElapsedTime());
                    jsonObject.addProperty("masterSocketID", data.getNewClientSocketId().toString());

                    server.getBroadcastOperations().sendEvent("seekVideoForAllClients", jsonObject.toString());
                }
            }
        });

        server.addEventListener("setNickname", String.class, new DataListener<String>() {
            @Override
            public void onData(SocketIOClient socketIOClient, String nickname, AckRequest ackRequest) throws Exception {
                if(nickname.equals("amadeus")) {
                    logger.info("New master socket confirmed. This too must be the choice of Steins;Gate");
                    masterSocket.setSocketDetails(socketIOClient.getSessionId());
                }
            }
        });

        server.addEventListener("videoFinishedPlaying", JsonObject.class, new DataListener<JsonObject>() {
            @Override
            public void onData(SocketIOClient client, JsonObject data, AckRequest ackSender) throws Exception {
                if(isSocketMaster(client.getSessionId())) {
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
        });

        server.addEventListener("newVideoQueue", ServerQueueObject.class, new DataListener<ServerQueueObject>() {
            @Override
            public void onData(SocketIOClient client, ServerQueueObject newServerQueueObject, AckRequest ackSender) throws Exception {
                if(masterSocket.getSocketId() == null) {
                    logger.info("No master socket set, request is ignored");
                    return;
                }

                addNewVideoToQueue(newServerQueueObject, server);
            }
        });



        server.start();
    }

    public static void addNewVideoToQueue(ServerQueueObject newServerQueueObject, SocketIOServer server) {
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

    public static boolean isSocketMaster(UUID socketId) {
        if(masterSocket.getSocketId() == null) {
            return false;
        }
        return masterSocket.getSocketId().equals(socketId);
    }
}
