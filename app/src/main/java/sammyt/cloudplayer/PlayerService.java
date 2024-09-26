package sammyt.cloudplayer;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.ForwardingPlayer;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import sammyt.cloudplayer.data.CloudClient;
import sammyt.cloudplayer.data.MediaQueue;

@UnstableApi
public class PlayerService extends MediaSessionService implements MediaSession.Callback, Player.Listener, MediaQueue.Listener {

    private static final String LOG_TAG = PlayerService.class.getSimpleName();

    private final Context context = PlayerService.this;

    private ForwardingPlayer player;
    private MediaSession mediaSession;

    private final MediaQueue queue = MediaQueue.getInstance();

    private final Handler handler = new Handler(Looper.getMainLooper());

    private int loadAttempts = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(LOG_TAG, "PlayerService created.");

        initPlayerAndSession();

        queue.addListener(this);
    }

    @Override
    public MediaSession onGetSession(@NonNull MediaSession.ControllerInfo controllerInfo) {
        return mediaSession;
    }

    @Override
    public void onDestroy() {
        Log.d(LOG_TAG, "PlayerService destroyed");

        if(mediaSession != null) {
            mediaSession.release();
        }

        if(player != null) {
            player.stop();
            player.release();
        }

        super.onDestroy();
    }

    @OptIn(markerClass = UnstableApi.class)
    private void initPlayerAndSession() {
        ExoPlayer exoPlayer = new ExoPlayer.Builder(context)
                .setAudioAttributes(AudioAttributes.DEFAULT, true)
                .setMediaSourceFactory(getMediaSourceFactory())
                .build();

        player = new ForwardingPlayer(exoPlayer) {
            @NonNull
            @Override
            public Commands getAvailableCommands() {
                Commands oc = super.getAvailableCommands();
                Commands c = oc
                        .buildUpon()
                        .add(COMMAND_SEEK_TO_NEXT)
                        .add(COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                        .add(COMMAND_GET_TIMELINE)
                        .build();

                return c;

//                return super.getAvailableCommands()
//                        .buildUpon()
//                        .add(COMMAND_SEEK_TO_NEXT)
//                        .add(COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
//                        .add(COMMAND_GET_TIMELINE)
//                        .build();
            }

            @Override
            public boolean isCommandAvailable(int command) {
//                if(command == COMMAND_SEEK_TO_NEXT || command == COMMAND_SEEK_TO_NEXT_MEDIA_ITEM) {
//                    Log.d("lskdj", "lskdjf");
//                }
                return super.isCommandAvailable(command);
            }

            @Override
            public boolean hasPreviousMediaItem() {
                // We want to automatically loop the queue.
                // So only the size is relevant here.
                return queue.getQueue().size() > 1;
            }

            @Override
            public boolean hasNextMediaItem() {
                // We want to automatically loop the queue.
                // So only the size is relevant here.
                return queue.getQueue().size() > 1;
            }

            @Override
            public long getMaxSeekToPreviousPosition() {
                return 5 * 1000;
            }

            @Override
            public void seekToPrevious() {
                if(hasPreviousMediaItem() && getCurrentPosition() < getMaxSeekToPreviousPosition()) {
                    queue.decreasePosition();
                } else {
                    seekToDefaultPosition();
                }
            }

            @Override
            public void seekToPreviousMediaItem() {
                if(!hasPreviousMediaItem()) return;

                queue.decreasePosition();
            }

            @Override
            public void seekToNext() {
                if(!hasNextMediaItem()) return;

                queue.advancePosition();
            }

            @Override
            public void seekToNextMediaItem() {
                if(!hasNextMediaItem()) return;

                queue.advancePosition();
            }
        };

        player.addListener(this);

        mediaSession = new MediaSession.Builder(context, player)
                .setCallback(this)
                .build();

        // Use our custom helper as a store for the session id value
//        PlayerSessionId.getInstance().setSessionId(exoPlayer.getAudioSessionId());
    }

    @OptIn(markerClass = UnstableApi.class)
    private MediaSource.Factory getMediaSourceFactory() {
        // Create the Data Source factory and add the header parameters
        DefaultHttpDataSource.Factory dataSourceFactory = new DefaultHttpDataSource.Factory();

        // Prepare the MediaSource.Factory using the DefaultHttpDataSource.Factory
        // so the custom authorization parameter is included in the request header.
        return new DefaultMediaSourceFactory(dataSourceFactory);
    }

    /**
     * Retrieves the track data, requests the stream URL from the transcodings,
     * then builds a Media Item and starts playback.
     * @param track The track to load and play.
     */
    private void loadMediaItem(JSONObject track) {
        try {
            loadAttempts++;

            String id = track.getString("id");
            String artworkUrl = track.getString("artwork_url");
            String username = track.getJSONObject("user").getString("username");
            String title = track.getString("title");

            String trackAuthorization = track.getString("track_authorization");

            JSONObject media = track.getJSONObject("media");
            JSONArray transcodings = media.getJSONArray("transcodings");

            if(transcodings.length() == 0) throw new Error("Empty transcodings for '" + title + "' " + username);

            JSONObject transcoding = transcodings.getJSONObject(0);

            String trackUrl = transcoding.getString("url");
            String protocol = transcoding.getJSONObject("format").getString("protocol");

            String params = "?client_id=" + getString(R.string.client_id)
                    + "&track_authorization=" + trackAuthorization;

            String url = trackUrl + params;

            Request request = new Request.Builder()
                    .url(url)
                    .header("Authorization", "OAuth " + getString(R.string.token))
                    .build();

            OkHttpClient client = CloudClient.getInstance().getClient();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e(LOG_TAG, "Error getting stream url.", e);
                    if(loadAttempts < 2) loadMediaItem(track);
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    try {
                        if(!response.isSuccessful()) throw new IOException("Unexpected code" + response);

                        ResponseBody responseBody = response.body();
                        String rawResponse = responseBody.string();

                        JSONObject parsed = new JSONObject(rawResponse);

                        String streamUrl = parsed.getString("url");

                        Bundle bundle = new Bundle();
                        bundle.putString("artwork_url", artworkUrl);

                        MediaItem.RequestMetadata requestMetadata = new MediaItem.RequestMetadata.Builder()
                                .setMediaUri(Uri.parse(streamUrl))
                                .build();

                        MediaMetadata mediaMetadata = new MediaMetadata.Builder()
                                .setArtist(username)
                                .setTitle(title)
                                .setArtworkUri(Uri.parse(artworkUrl))
                                .setExtras(bundle)
                                .build();

                        MediaItem.Builder mediaBuilder = new MediaItem.Builder()
                                .setMediaId(id)
                                .setMediaMetadata(mediaMetadata)
                                .setUri(Uri.parse(streamUrl))
                                .setRequestMetadata(requestMetadata);

                        if(protocol.equals("hls")) mediaBuilder.setMimeType(MimeTypes.APPLICATION_M3U8);

                        MediaItem mediaItem = mediaBuilder.build();

                        // Use a handler to ensure we're updating the player from the main thread
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                player.setMediaItem(mediaItem);

                                player.prepare();
                                player.play();

                                loadAttempts = 0;
                            }
                        });
                    } catch(IOException | JSONException e) {
                        Log.e(LOG_TAG, "Error preparing media item", e);
                    }
                }
            });
        } catch(JSONException e) {
            Log.e(LOG_TAG, "Error loading media item.", e);
        }
    }

    //// MediaSession.Callback interface

    @NonNull
    @Override
    public ListenableFuture<List<MediaItem>> onAddMediaItems(@NonNull MediaSession mediaSession,
                                                             @NonNull MediaSession.ControllerInfo controller,
                                                             @NonNull List<MediaItem> mediaItems) {
        Log.d(LOG_TAG, mediaItems.toString());
        List<MediaItem> updatedMediaItems = new ArrayList<>();

        for(int i=0; i < mediaItems.size(); i++) {
            MediaItem mediaItem = mediaItems.get(i);

            MediaItem updatedItem = mediaItem.buildUpon()
                    .setUri(mediaItem.requestMetadata.mediaUri)
                    .build();

            updatedMediaItems.add(updatedItem);
        }

        return Futures.immediateFuture(updatedMediaItems);
    }

    //// Player.Listener interface
    @Override
    public void onPlaybackStateChanged(int playbackState) {
        String stateMsg = "Player State: ";

        switch(playbackState) {
            case Player.STATE_IDLE:
                stateMsg += "IDLE";
                break;

            case Player.STATE_BUFFERING:
                stateMsg += "BUFFERING";
                break;

            case Player.STATE_READY:
                stateMsg += "READY";
                break;

            case Player.STATE_ENDED:
                stateMsg += "ENDED";

                // Advance to the next track if playback of the current media has ended.
                if(player.getRepeatMode() == Player.REPEAT_MODE_OFF) player.seekToNext();
                break;

            default:
                stateMsg += "UNKNOWN STATE";
                break;
        }

        Log.d(LOG_TAG, stateMsg);

        Player.Listener.super.onPlaybackStateChanged(playbackState);
    }

    //// MediaQueue.Listener interface

    @Override
    public void onPositionChanged(int pos) {
        // Load the current track when the queue's track position changes
        JSONObject track = queue.getQueue().get(pos);
        loadMediaItem(track);
    }
}
