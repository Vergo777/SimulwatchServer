package com.vergo.simulwatchserver;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Varun on 14/04/2017.
 */
public class ServerQueueSerializer implements JsonSerializer<ServerQueue> {

    @Override
    public JsonElement serialize(ServerQueue serverQueue, Type typeOfServerQueue, JsonSerializationContext context) {
        JsonArray serverQueueJsonArray = new JsonArray();

        for(HashMap<String, ServerQueueObject> bucket : serverQueue.getServerQueue()) {
            JsonArray bucketJsonArray = new JsonArray();
            for(Map.Entry<String, ServerQueueObject> entry : bucket.entrySet()) {
                String username = entry.getKey();
                ServerQueueObject serverQueueObject = entry.getValue();

                JsonObject bucketItem = new JsonObject();
                bucketItem.addProperty("username", username);
                bucketItem.addProperty("videoURL", serverQueueObject.getVideoURL());

                bucketJsonArray.add(bucketItem);
            }

            serverQueueJsonArray.add(bucketJsonArray);
        }

        return serverQueueJsonArray;
    }
}
