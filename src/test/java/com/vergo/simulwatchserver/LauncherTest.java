package com.vergo.simulwatchserver;

import com.corundumstudio.socketio.BroadcastOperations;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;

import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * Created by Varun on 16/04/2017.
 */
@RunWith(MockitoJUnitRunner.class)
public class LauncherTest {

    private final double DUMMY_ELAPSED_TIME = 1;
    private final UUID DUMMY_MASTER_SOCKET_ID = UUID.randomUUID();
    private final String DUMMY_USERNAME = "Kumiko";
    private final String DUMMY_VIDEOURL = "Kazuma dess";

    @Mock
    private SocketIOServer mockServer;
    @Mock
    private SocketIOClient mockSocketIOClient;
    @Mock
    private BroadcastOperations mockBroadcastOperations;
    @Mock
    private ServerQueue mockServerQueue;
    @Mock
    private ServerQueueObject mockServerQueueObject;
    @Mock
    private Logger mockLogger;
    @Mock
    private SocketDetails mockMasterSocketDetails;

    @Before
    public void setup() throws Exception {
        when(mockServer.getClient(Mockito.any())).thenReturn(mockSocketIOClient);
        when(mockServer.getBroadcastOperations()).thenReturn(mockBroadcastOperations);
        when(mockServerQueueObject.getUsername()).thenReturn(DUMMY_USERNAME);
        when(mockServerQueueObject.getVideoURL()).thenReturn(DUMMY_VIDEOURL);
        Mockito.doNothing().when(mockLogger).info(Mockito.anyString());
        Mockito.doNothing().when(mockSocketIOClient).sendEvent(Mockito.anyString(), Mockito.anyString());
        Mockito.doNothing().when(mockBroadcastOperations).sendEvent(Mockito.anyString(), Mockito.anyString());
    }

    @Test
    public void returnCurrentVideoElapsedTimeCallbackTest() throws Exception {
        when(mockServerQueue.peekCurrentQueuedVideo()).thenReturn(mockServerQueueObject);

        ReturnCurrentVideoElapsedTimeObject returnCurrentVideoElapsedTimeObject = new ReturnCurrentVideoElapsedTimeObject(DUMMY_ELAPSED_TIME, DUMMY_MASTER_SOCKET_ID);

        Launcher.returnCurrentVideoElapsedTimeCallback(returnCurrentVideoElapsedTimeObject, mockServer, mockServerQueue);

        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockSocketIOClient).sendEvent(eq("playCurrentVideoForNewClient"), jsonCaptor.capture());
        String expectedJson = "{\"elapsedTime\":" + DUMMY_ELAPSED_TIME + ",\"username\":\"" + DUMMY_USERNAME + "\",\"videoURL\":\"" + DUMMY_VIDEOURL + "\"}";
        assertEquals(expectedJson, jsonCaptor.getValue());
    }

    @Test
    public void setNicknameCallbackTest() throws Exception {
        when(mockSocketIOClient.getSessionId()).thenReturn(DUMMY_MASTER_SOCKET_ID);
        Mockito.doNothing().when(mockMasterSocketDetails).setSocketDetails(DUMMY_MASTER_SOCKET_ID);
        Launcher.setNicknameCallback("amadeus", mockSocketIOClient, mockLogger, mockMasterSocketDetails);
        Launcher.setNicknameCallback("notAmadeus", mockSocketIOClient, mockLogger, mockMasterSocketDetails);

        ArgumentCaptor<String> loggerCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockLogger).info(loggerCaptor.capture());
        verify(mockLogger).info(loggerCaptor.capture(), eq("notAmadeus"));

        List<String> loggerCaptorValues = loggerCaptor.getAllValues();
        assertEquals("New master socket confirmed. This too must be the choice of Steins;Gate", loggerCaptorValues.get(0));
        assertEquals("Incorrect attempt to set master with nickname {}", loggerCaptorValues.get(1));
    }

    @Test
    public void newVideoQueueCallbackTestMasterSocketIdNull() throws Exception {
        when(mockMasterSocketDetails.getSocketId()).thenReturn(null);

        Launcher.newVideoQueueCallback(mockMasterSocketDetails, mockServerQueue, mockServerQueueObject, mockServer, mockLogger);
        ArgumentCaptor<String> loggerCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockLogger).info(loggerCaptor.capture());
        assertEquals("No master socket set, request is ignored", loggerCaptor.getValue());
    }

    @Test
    public void newVideoQueueCallbackTest() throws Exception {
        ServerQueue serverQueue = new ServerQueue();
        when(mockMasterSocketDetails.getSocketId()).thenReturn(DUMMY_MASTER_SOCKET_ID);
        String eventName = "newVideoQueueForClient";

        JsonObject expectedJsonObject = new JsonObject();
        expectedJsonObject.addProperty("username", DUMMY_USERNAME);
        expectedJsonObject.addProperty("videoURL", DUMMY_VIDEOURL);
        expectedJsonObject.addProperty("videoQueue", "[[{\"username\":\"" + DUMMY_USERNAME + "\",\"videoURL\":\"" + DUMMY_VIDEOURL + "\"}]]");

        // first we test the case where this is the first element added to the queue, so "playVideo" will be true
        expectedJsonObject.addProperty("playVideo", true);
        Launcher.newVideoQueueCallback(mockMasterSocketDetails, serverQueue, mockServerQueueObject, mockServer, mockLogger);

        // and then test that this is not first element, so "playVideo" will be false
        // we already added element to the actual queue in the above step, so queue already has size > 0
        Launcher.newVideoQueueCallback(mockMasterSocketDetails, serverQueue, mockServerQueueObject, mockServer, mockLogger);

        JsonParser jsonParser = new JsonParser();
        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockBroadcastOperations, times(2)).sendEvent(eq(eventName), jsonCaptor.capture());

        assertEquals(expectedJsonObject, jsonParser.parse(jsonCaptor.getAllValues().get(0)));

        expectedJsonObject.remove("playVideo");
        expectedJsonObject.addProperty("playVideo", false);
        expectedJsonObject.remove("videoQueue");
        expectedJsonObject.addProperty("videoQueue", "[[{\"username\":\"" + DUMMY_USERNAME + "\",\"videoURL\":\"" + DUMMY_VIDEOURL + "\"}],[{\"username\":\"" + DUMMY_USERNAME + "\",\"videoURL\":\"" + DUMMY_VIDEOURL + "\"}]]");
        assertEquals(expectedJsonObject, jsonParser.parse(jsonCaptor.getAllValues().get(1)));
    }
}