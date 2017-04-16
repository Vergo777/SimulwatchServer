package com.vergo.simulwatchserver;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.Before;
import org.junit.Test;


import java.util.LinkedHashMap;

import static org.junit.Assert.*;

/**
 * Created by Varun on 14/04/2017.
 */
public class ServerQueueTest {
    private ServerQueue serverQueue;
    private ServerQueueObject dummyServerQueueObject = new ServerQueueObject("testUser", "testURL");

    @Before
    public void setup() throws Exception {
        serverQueue = new ServerQueue();
    }

    @Test
    public void testQueueNewVideoEmptyQueue() throws Exception {
        serverQueue.queueNewVideo("testUser", dummyServerQueueObject);
        assertEquals(serverQueue.getServerQueue().size(), 1);

        LinkedHashMap<String, ServerQueueObject> firstBucket = serverQueue.getServerQueue().get(0);
        assertEquals(firstBucket.size(), 1);
        assertNotNull(firstBucket.get("testUser"));
    }

    @Test
    public void testQueueNewVideoEmptyBucketAvailable() throws Exception {
        serverQueue.queueNewVideo("testUser1", dummyServerQueueObject);
        serverQueue.queueNewVideo("testUser2", dummyServerQueueObject);
        serverQueue.queueNewVideo("testUser1", dummyServerQueueObject);
        assertEquals(serverQueue.getServerQueue().size(), 2);

        LinkedHashMap<String, ServerQueueObject> firstBucket = serverQueue.getServerQueue().get(0);
        LinkedHashMap<String, ServerQueueObject> secondBucket = serverQueue.getServerQueue().get(1);
        assertEquals(firstBucket.size(), 2);
        assertNotNull(firstBucket.get("testUser1"));
        assertNotNull(firstBucket.get("testUser2"));
        assertEquals(secondBucket.size(), 1);
        assertNotNull(secondBucket.get("testUser1"));

        serverQueue.queueNewVideo("testUser2", dummyServerQueueObject);
        assertEquals(firstBucket.size(), 2);
        assertNotNull(firstBucket.get("testUser1"));
        assertNotNull(firstBucket.get("testUser2"));
        assertEquals(secondBucket.size(), 2);
        assertNotNull(secondBucket.get("testUser1"));
        assertNotNull(secondBucket.get("testUser2"));
    }

    @Test
    public void testQueueNewVideoEmptyBucketNotAvailable() throws Exception {
        serverQueue.queueNewVideo("testUser1", dummyServerQueueObject);
        serverQueue.queueNewVideo("testUser2", dummyServerQueueObject);
        serverQueue.queueNewVideo("testUser1", dummyServerQueueObject);
        serverQueue.queueNewVideo("testUser2", dummyServerQueueObject);

        serverQueue.queueNewVideo("testUser1", dummyServerQueueObject);
        assertEquals(serverQueue.getServerQueue().size(), 3);

        LinkedHashMap<String, ServerQueueObject> firstBucket = serverQueue.getServerQueue().get(0);
        LinkedHashMap<String, ServerQueueObject> secondBucket = serverQueue.getServerQueue().get(1);
        LinkedHashMap<String, ServerQueueObject> thirdBucket = serverQueue.getServerQueue().get(2);

        assertEquals(firstBucket.size(), 2);
        assertNotNull(firstBucket.get("testUser1"));
        assertNotNull(firstBucket.get("testUser2"));

        assertEquals(secondBucket.size(), 2);
        assertNotNull(secondBucket.get("testUser1"));
        assertNotNull(secondBucket.get("testUser2"));

        assertEquals(thirdBucket.size(), 1);
        assertNotNull(thirdBucket.get("testUser1"));
    }

    @Test
    public void testQueueNewVideoNewUser() throws Exception {
        serverQueue.queueNewVideo("testUser1", dummyServerQueueObject);
        serverQueue.queueNewVideo("testUser2", dummyServerQueueObject);
        serverQueue.queueNewVideo("testUser1", dummyServerQueueObject);
        serverQueue.queueNewVideo("testUser2", dummyServerQueueObject);

        serverQueue.queueNewVideo("testUser3", dummyServerQueueObject);
        assertEquals(serverQueue.getServerQueue().size(), 2);

        LinkedHashMap<String, ServerQueueObject> firstBucket = serverQueue.getServerQueue().get(0);
        LinkedHashMap<String, ServerQueueObject> secondBucket = serverQueue.getServerQueue().get(1);

        assertEquals(firstBucket.size(), 3);
        assertNotNull(firstBucket.get("testUser1"));
        assertNotNull(firstBucket.get("testUser2"));
        assertNotNull(firstBucket.get("testUser3"));

        assertEquals(secondBucket.size(), 2);
        assertNotNull(secondBucket.get("testUser1"));
        assertNotNull(secondBucket.get("testUser2"));
    }

    @Test
    public void testGetNextQueuedVideoBucketNotEmpty() throws Exception {
        serverQueue.queueNewVideo("testUser1", dummyServerQueueObject);
        serverQueue.queueNewVideo("testUser2", dummyServerQueueObject);

        ServerQueueObject serverQueueObject = serverQueue.getNextQueuedVideo();
        assertEquals(serverQueue.getServerQueue().size(), 1);

        LinkedHashMap<String, ServerQueueObject> firstBucket = serverQueue.getServerQueue().get(0);
        assertEquals(firstBucket.size(), 1);
        assertFalse(firstBucket.containsKey("testUser1"));
        assertTrue(firstBucket.containsKey("testUser2"));
    }

    @Test
    public void testGetNextQueuedVideoBucketEmptyAfterRemoval() throws Exception {
        serverQueue.queueNewVideo("testUser1", dummyServerQueueObject);
        serverQueue.queueNewVideo("testUser2", dummyServerQueueObject);
        serverQueue.queueNewVideo("testUser1", dummyServerQueueObject);
        serverQueue.queueNewVideo("testUser2", dummyServerQueueObject);

        serverQueue.getNextQueuedVideo();
        LinkedHashMap<String, ServerQueueObject> firstBucket = serverQueue.getServerQueue().get(0);
        assertEquals(serverQueue.getServerQueue().size(), 2);
        assertEquals(firstBucket.size(), 1);
        assertFalse(firstBucket.containsKey("testUser1"));
        assertTrue(firstBucket.containsKey("testUser2"));

        serverQueue.getNextQueuedVideo();
        assertEquals(serverQueue.getServerQueue().size(), 1);
        firstBucket = serverQueue.getServerQueue().get(0);
        assertEquals(firstBucket.size(), 2);
        assertTrue(firstBucket.containsKey("testUser1"));
        assertTrue(firstBucket.containsKey("testUser2"));
    }

    @Test
    public void testQueueJSONSerialization() {
        serverQueue.queueNewVideo("testUser1", dummyServerQueueObject);
        serverQueue.queueNewVideo("testUser2", dummyServerQueueObject);
        serverQueue.queueNewVideo("testUser1", dummyServerQueueObject);
        serverQueue.queueNewVideo("testUser2", dummyServerQueueObject);

        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(ServerQueue.class, new ServerQueueSerializer());

        JsonArray expectedServerQueueJson = new JsonArray();
        JsonArray expectedFirstBucketJson = new JsonArray();
        JsonArray expectedSecondBucketJson = new JsonArray();

        JsonObject expectedFirstBucketFirstItemJson = new JsonObject();
        expectedFirstBucketFirstItemJson.addProperty("username", "testUser1");
        expectedFirstBucketFirstItemJson.addProperty("videoURL", "testURL");

        JsonObject expectedFirstBucketSecondItemJson = new JsonObject();
        expectedFirstBucketSecondItemJson.addProperty("username", "testUser2");
        expectedFirstBucketSecondItemJson.addProperty("videoURL", "testURL");

        JsonObject expectedSecondBucketFirstItemJson = new JsonObject();
        expectedSecondBucketFirstItemJson.addProperty("username", "testUser1");
        expectedSecondBucketFirstItemJson.addProperty("videoURL", "testURL");

        JsonObject expectedSecondBucketSecondItemJson = new JsonObject();
        expectedSecondBucketSecondItemJson.addProperty("username", "testUser2");
        expectedSecondBucketSecondItemJson.addProperty("videoURL", "testURL");

        expectedFirstBucketJson.add(expectedFirstBucketFirstItemJson);
        expectedFirstBucketJson.add(expectedFirstBucketSecondItemJson);

        expectedSecondBucketJson.add(expectedSecondBucketFirstItemJson);
        expectedSecondBucketJson.add(expectedSecondBucketSecondItemJson);

        expectedServerQueueJson.add(expectedFirstBucketJson);
        expectedServerQueueJson.add(expectedSecondBucketJson);

        assertEquals(expectedServerQueueJson.toString(), gsonBuilder.create().toJson(serverQueue));
    }
}