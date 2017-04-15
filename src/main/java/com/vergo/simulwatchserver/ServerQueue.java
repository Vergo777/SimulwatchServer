package com.vergo.simulwatchserver;

import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * Created by Varun on 14/04/2017.
 */
public class ServerQueue {
    private ArrayList<LinkedHashMap<String, ServerQueueObject>> serverQueue;

    public ServerQueue() {
        serverQueue = new ArrayList<>();
    }

    public void queueNewVideo(String username, ServerQueueObject serverQueueObject) {
        // if size is 0, then simply add new bucket to queue with given user entry as first bucket entry
        if(serverQueue.size() == 0) {
            addNewBucketToQueue(username, serverQueueObject);
            return;
        }

        // iterate through current queue to try and find a bucket that doesn't contain an entry from this user
        for(LinkedHashMap<String, ServerQueueObject> bucket : serverQueue) {
            if(bucket.containsKey(username)) {
                continue;
            } else {
                // if we find a bucket without an entry for this user, add this new entry to this bucket
                bucket.put(username, serverQueueObject);
                return;
            }
        }

        // if we iterate through the whole queue without finding an empty bucket, create a new bucket with this entry
        addNewBucketToQueue(username, serverQueueObject);
    }

    public ServerQueueObject getNextQueuedVideo() {

        if(serverQueue.size() == 0) {
            return null;
        }

        LinkedHashMap<String, ServerQueueObject> firstBucket = serverQueue.get(0);

        // the way we want the queue to work is that we delete the element that just finished playing, and then move on
        // to the next one
        firstBucket.remove(firstBucket.entrySet().iterator().next().getKey());
        if(firstBucket.size() == 0) {
            serverQueue.remove(0);
            if(serverQueue.size() == 0) {
                return null;
            } else {
                firstBucket = serverQueue.get(0);
            }
        }

        ServerQueueObject nextQueuedVideo = firstBucket.entrySet().iterator().next().getValue();

        return nextQueuedVideo;
    }

    public ServerQueueObject peekCurrentQueuedVideo() {
        return serverQueue.get(0).entrySet().iterator().next().getValue();
    }

    public int getServerQueueLength() {
        return serverQueue.size();
    }

    public void clearServerQueue() {
        serverQueue.clear();
    }

    private void addNewBucketToQueue(String username, ServerQueueObject serverQueueObject) {
        LinkedHashMap<String, ServerQueueObject> newBucket = new LinkedHashMap<>();
        newBucket.put(username, serverQueueObject);
        serverQueue.add(newBucket);
    }

    ArrayList<LinkedHashMap<String, ServerQueueObject>> getServerQueue() {
        return serverQueue;
    }
}

