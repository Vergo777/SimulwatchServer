/**
 * Created by Varun on 14/04/2017.
 */

var socket = io.connect('http://168.235.65.43:2499/');
videoPlayer = videojs('videoPlayer');

videoPlayer.on(['error', 'ended'], function () {
    socket.emit('videoFinishedPlaying');
});

videoPlayer.on('pause', function () {
   socket.emit('videoPaused');
});

socket.on('pauseVideoForAllClients', function () {
    videoPlayer.pause();
});

videoPlayer.on('play', function () {
   socket.emit('videoPlayed');
});

socket.on('playVideoForAllClients', function () {
   videoPlayer.play();
});

videoPlayer.on('seeked', function () {
    elapsedTime = videoPlayer.currentTime();
    socket.emit('videoSeeked', {
        "elapsedTime" : elapsedTime,
        "newClientSocketId" : socket.io.engine.id
    });
});

socket.on('seekVideoForAllClients', function (seekDataObject) {
    seekDataObject = $.parseJSON(seekDataObject);

    if(socket.io.engine.id != seekDataObject.masterSocketID) {
        videoPlayer.currentTime(seekDataObject.elapsedTime);
    }
});

socket.on('initialQueueData', function (videoQueue) {
    populateClientVideoQueue(videoQueue);
});

socket.on('killEverything', function() {
    videoPlayer.reset();
    emptyVideoQueue();
});

socket.on('getCurrentVideoElapsedTime', function (newClientSocketId) {
    elapsedTime = videoPlayer.currentTime();
    socket.emit('returnCurrentVideoElapsedTime', {
        "elapsedTime" : elapsedTime,
        "newClientSocketId" : newClientSocketId
    });
});

socket.on('playCurrentVideoForNewClient', function (currentVideoData) {

    currentVideoData = $.parseJSON(currentVideoData);

    loadNewVideoInPlayer(currentVideoData.videoURL);
    videoPlayer.currentTime(currentVideoData.elapsedTime);
    videoPlayer.play();
    updateNowPlayingHeader(currentVideoData.videoURL, currentVideoData.username);
});

socket.on('playNextVideoInQueue', function (nextVideoQueueObject) {

    nextVideoQueueObject = $.parseJSON(nextVideoQueueObject);

    videoPlayer.reset();
    if(nextVideoQueueObject.videoURL == null && nextVideoQueueObject.username == null) {
        emptyVideoQueue();
        return;
    }

    if(nextVideoQueueObject.videoQueue != null) {
        emptyVideoQueue();
        populateClientVideoQueue(nextVideoQueueObject.videoQueue);
    } else {
        $('#bucket0').find('li').first().remove();
    }

    loadNewVideoInPlayer(nextVideoQueueObject.videoURL);
    videoPlayer.play();
    updateNowPlayingHeader(nextVideoQueueObject.videoURL, nextVideoQueueObject.username);
});

socket.on('newVideoQueueForClient', function (newVideoQueueObject) {

    newVideoQueueObject = $.parseJSON(newVideoQueueObject);

    emptyVideoQueue();
    populateClientVideoQueue(newVideoQueueObject.videoQueue);

    if(newVideoQueueObject.playVideo) {
        loadNewVideoInPlayer(newVideoQueueObject.videoURL);
        videoPlayer.play();
        updateNowPlayingHeader(newVideoQueueObject.videoURL, newVideoQueueObject.username);
    }
});

$('#nicknameField').find('button').click(function() {
    socket.emit('setNickname', $('#nicknameField').find('.form-control').val());
    $('#nicknameField').find('.form-control').val('')
});

$('#newVideoQueue').find('button').click(function() {
    socket.emit('newVideoQueue', {
        "videoURL": $('#newVideoQueue').find('.form-control').val(),
        "username": 'test' // todo: replace username with proper discord integration or something
    });
    $('#newVideoQueue').find('.form-control').val('')
});

loadNewVideoInPlayer = function (videoURL) {
    var type = 'video/youtube';

    if(videoURL.split('.').pop() == "mp4") {
        type = "video/mp4";
    }

    videoPlayer.src({type: type, src: videoURL});
};

updateNowPlayingHeader = function (title, username) {
    $('#nowPlaying').empty();
    $('#nowPlaying').append('<h3>Now Playing : ' + title + ' <small>Queued by : ' + username + '</small></h3>');
};

populateClientVideoQueue = function (videoQueue) {

    videoQueue = $.parseJSON(videoQueue);

    for(var bucket = 0; bucket < videoQueue.length; bucket++) {
        for(var item = 0; item < videoQueue[bucket].length; item++) {
            addVideoToBucketList(bucket, videoQueue[bucket][item].videoURL, videoQueue[bucket][item].username);
        }
    }
};

addVideoToBucketList = function (bucket, videoURL, username) {
    if($('#bucket' + bucket).length == 0) {
        $('#videoQueue').append('<div class="col-xs-12"><h3 class="text-center">Bucket ' + bucket + '</h3></div>');
        $('#videoQueue').append('<div class="col-xs-12"><ul class="list-group" id="bucket' + bucket + '"></ul></div>');
    }

    $('#bucket' + bucket).append('<li class="list-group-item">' + videoURL + ' : ' + username + '</li>')
};

emptyVideoQueue = function() {
    $('#videoQueue').find('div').empty();
};