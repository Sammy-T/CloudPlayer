package sammyt.cloudplayer;

import android.animation.ObjectAnimator;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Player;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.common.util.concurrent.ListenableFuture;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import sammyt.cloudplayer.data.CloudClient;
import sammyt.cloudplayer.nav.SelectedTrackModel;
import sammyt.cloudplayer.player.PlayerActivity;

public class NavActivity extends AppCompatActivity {

    private final String LOG_TAG = this.getClass().getSimpleName();

    private SelectedTrackModel selectedTrackModel;

    private ImageButton mPlay;
    private TextView mTitle;
    private TextView mArtist;
    private ProgressBar mProgress;

    private ObjectAnimator mProgressAnim;

    private ListenableFuture<MediaController> controllerFuture;
    private MediaController mediaController;

    private ScheduledExecutorService executor;
    private ScheduledFuture<?> future;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        RelativeLayout infoArea;

        setContentView(R.layout.activity_nav);
        BottomNavigationView navView = findViewById(R.id.nav_view);
        mPlay = findViewById(R.id.mini_play_pause);
        infoArea = findViewById(R.id.mini_info_area);
        mTitle = findViewById(R.id.mini_title);
        mArtist = findViewById(R.id.mini_artist);
        mProgress = findViewById(R.id.mini_progress);

        // Set up the bottom navigation view with the Nav Controller
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupWithNavController(navView, navController);

        mProgressAnim = new ObjectAnimator();
        mProgressAnim.setTarget(mProgress);
        mProgressAnim.setPropertyName("progress");
        mProgressAnim.setDuration(1000);
        mProgressAnim.setInterpolator(new LinearInterpolator());

        selectedTrackModel = new ViewModelProvider(this).get(SelectedTrackModel.class);

        // Observe the shared View Model to update the service's track list & load the selected track
        selectedTrackModel.getSelectedTrack().observe(this, new Observer<SelectedTrackModel.SelectedTrack>() {
            @Override
            public void onChanged(SelectedTrackModel.SelectedTrack selectedTrack) {
                if(selectedTrack == null || selectedTrack.getSelectionSource().equals(LOG_TAG) ||
                        mediaController == null) {
                    return; // Prevent an endless loop if this was triggered by this activity
                }

                createMediaItem(selectedTrack.getTrack());
            }
        });

        mPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mediaController == null) {
                    return;
                }

                if(mediaController.isPlaying()) {
                    mediaController.pause();
                } else {
                    mediaController.play();
                }
            }
        });

        infoArea.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(NavActivity.this, PlayerActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        initController();
    }

    @Override
    protected void onResume() {
        super.onResume();
        executor = Executors.newSingleThreadScheduledExecutor();
    }

    @Override
    protected void onPause() {
        executor.shutdown();
        super.onPause();
    }

    @Override
    protected void onStop() {
        MediaController.releaseFuture(controllerFuture);
        super.onStop();
    }

    private void updateUI() {
        int playOrPause;

        if(mediaController.isPlaying()) {
            playOrPause = R.drawable.ic_pause_white_24dp;
        }else{
            playOrPause = R.drawable.ic_play_white_24dp;
        }

        mPlay.setImageResource(playOrPause);

        MediaItem mediaItem = mediaController.getCurrentMediaItem();
        if(mediaItem == null) {
            return;
        }

        mArtist.setText(mediaItem.mediaMetadata.artist);
        mTitle.setText(mediaItem.mediaMetadata.title);
    }

    public void updateProgress(float duration, float currentPos, float bufferPos){
        int progress = (int) ((currentPos / duration) * 1000);
        int limit = (int) ((5f / 100f) * 1000);

        // Set the progress without animating if there's a large change in progress
        // (i.e. returning to the activity)
        if(Math.abs(progress - mProgress.getProgress()) >= limit){
            mProgress.setProgress(progress);
            return;
        }

        // Animate the change in progress
        mProgressAnim.setIntValues(progress);
        mProgressAnim.start();
    }

    private void initController() {
        SessionToken sessionToken = new SessionToken(this, new ComponentName(this, PlayerService.class));

        controllerFuture = new MediaController.Builder(this, sessionToken).buildAsync();
        controllerFuture.addListener(() -> {
            try {
                setController(controllerFuture.get());
            } catch(ExecutionException | InterruptedException e) {
                Log.e(LOG_TAG, "Unable to get mediaController", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void setController(MediaController controller) {
        mediaController = controller;

        // Update the UI if there's already media playing. This keeps the UI in sync
        // when navigating away from then back to this activity.
        if(mediaController.isPlaying()) {
            updateUI();
            selectedTrackModel.updateSelectedTrack(mediaController.getCurrentMediaItem(), LOG_TAG);
            future = executor.scheduleWithFixedDelay(progressHelperRunnable, 0, 1, TimeUnit.SECONDS);
        }

        mediaController.addListener(new Player.Listener() {
            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                Player.Listener.super.onIsPlayingChanged(isPlaying);

                updateUI();

                if(!isPlaying && future != null) {
                    future.cancel(true);
                } else if(isPlaying) {
                    future = executor.scheduleWithFixedDelay(progressHelperRunnable, 0, 1, TimeUnit.SECONDS);
                }
            }

            @Override
            public void onMediaItemTransition(MediaItem mediaItem, int reason) {
                Player.Listener.super.onMediaItemTransition(mediaItem, reason);
                updateUI();
            }
        });
    }

    private void createMediaItem(JSONObject track) {
        try {
            String artworkUrl = track.getString("artwork_url");
            String username = track.getJSONObject("user").getString("username");
            String title = track.getString("title");

            String trackAuthorization = track.getString("track_authorization");
            String trackUrl = "";

            JSONObject media = track.getJSONObject("media");
            JSONArray transcodings = media.getJSONArray("transcodings");

            for(int i=0; i < transcodings.length(); i++) {
                JSONObject transcoding = transcodings.getJSONObject(i);

                String protocol = transcoding.getJSONObject("format").getString("protocol");

                if(protocol.equals("progressive")) trackUrl = transcoding.getString("url");
            }

            if(trackUrl.isEmpty()) {
                Log.w(LOG_TAG, "wtf\n" + title + "\n" + transcodings);
                throw new Error("Invalid track url");
            }

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

                        MediaItem mediaItem = new MediaItem.Builder()
                                .setMediaId(streamUrl)
                                .setMediaMetadata(mediaMetadata)
                                .setRequestMetadata(requestMetadata)
                                .build();

                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                mediaController.setMediaItem(mediaItem);

                                mediaController.prepare();
                                mediaController.play();
                            }
                        });
                    } catch(IOException | JSONException e) {
                        Log.e(LOG_TAG, "SC f*cking sucks.", e);
                    }
                }
            });
        } catch(JSONException | Error e) {
            Log.e(LOG_TAG, "Unable to create MediaItem", e);
        }
    }

    /**
     * This Runnable is a helper to make sure we're updating the UI from the correct thread
     * by using the Handler as the go-between
     */
    private final Runnable progressHelperRunnable = new Runnable() {
        @Override
        public void run() {
            handler.post(progressRunnable);
        }
    };

    private final Runnable progressRunnable = () -> {
        if(mediaController != null) {
            updateProgress(mediaController.getDuration(), mediaController.getCurrentPosition(),
                    mediaController.getBufferedPosition());
        }
    };
}
