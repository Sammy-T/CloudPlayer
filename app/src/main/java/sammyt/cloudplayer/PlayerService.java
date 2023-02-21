package sammyt.cloudplayer;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.Format;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.DecoderCounters;
import androidx.media3.exoplayer.DecoderReuseEvaluation;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.analytics.AnalyticsCollector;
import androidx.media3.exoplayer.analytics.AnalyticsListener;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.session.MediaController;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;
import androidx.media3.session.SessionToken;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sammyt.cloudplayer.data.CloudQueue;
import sammyt.cloudplayer.data.PlayerSessionId;

public class PlayerService extends MediaSessionService implements MediaSession.Callback {

    private static final String LOG_TAG = PlayerService.class.getSimpleName();

    private static final String CHANNEL_ID = "CloudPlayer";
    private static final int NOTIFICATION_ID = 111;

    private static final String PLAYER_ACTION_EXTRA = "EXTRA_PLAYER_ACTION";
    private static final int PLAYER_ACTION_PLAY_PAUSE = 0;
    private static final int PLAYER_ACTION_NEXT = 1;
    private static final int PLAYER_ACTION_PREV = 2;

    private final Context context = PlayerService.this;

    private ExoPlayer player;
    private MediaSession mediaSession;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(LOG_TAG, "PlayerService created.");

        initPlayerAndSession();
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

    @NonNull
    @Override
    public ListenableFuture<List<MediaItem>> onAddMediaItems(@NonNull MediaSession mediaSession,
            @NonNull MediaSession.ControllerInfo controller, @NonNull List<MediaItem> mediaItems) {
        Log.d(LOG_TAG, mediaItems.toString());
        List<MediaItem> updatedMediaItems = new ArrayList<>();

        for(int i=0; i < mediaItems.size(); i++) {
            MediaItem mediaItem = mediaItems.get(i);
            MediaItem updatedItem = mediaItem.buildUpon()
                    .setUri(mediaItem.requestMetadata.mediaUri).build();
            updatedMediaItems.add(updatedItem);
        }

        return Futures.immediateFuture(updatedMediaItems);
    }

    @OptIn(markerClass = UnstableApi.class)
    private void initPlayerAndSession() {
        player = new ExoPlayer.Builder(context)
                .setAudioAttributes(AudioAttributes.DEFAULT, true)
                .setMediaSourceFactory(getMediaSourceFactory())
                .build();

        mediaSession = new MediaSession.Builder(context, player)
                .setCallback(this)
                .build();

        // Use our custom helper as a store for the session id value
        PlayerSessionId.getInstance().setSessionId(player.getAudioSessionId());
    }

    @OptIn(markerClass = UnstableApi.class)
    private MediaSource.Factory getMediaSourceFactory() {
        // Retrieve the token
        SharedPreferences sharedPrefs = context.getSharedPreferences(getString(R.string.pref_file_key), Context.MODE_PRIVATE);
        String token = sharedPrefs.getString(getString(R.string.token_key), "");

        // Create the request header parameters
        Map<String, String> params = new HashMap<>();
        params.put("Authorization", "OAuth " + token);

        // Create the Data Source factory and add the header parameters
        DefaultHttpDataSource.Factory dataSourceFactory = new DefaultHttpDataSource.Factory();
        dataSourceFactory.setDefaultRequestProperties(params);

        // Prepare the MediaSource.Factory using the DefaultHttpDataSource.Factory
        // so the custom authorization parameter is included in the request header.
        return new DefaultMediaSourceFactory(dataSourceFactory);
    }
}
