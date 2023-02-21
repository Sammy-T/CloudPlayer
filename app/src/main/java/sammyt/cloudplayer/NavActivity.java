package sammyt.cloudplayer;

import android.animation.ObjectAnimator;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

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

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import sammyt.cloudplayer.nav.SelectedTrackModel;
import sammyt.cloudplayer.player.PlayerActivity;

public class NavActivity extends AppCompatActivity {

    private final String LOG_TAG = this.getClass().getSimpleName();

    public String token;

    private ImageButton mPlay;
    private TextView mTitle;
    private TextView mArtist;
    private ProgressBar mProgress;

    private ObjectAnimator mProgressAnim;

    private ListenableFuture<MediaController> controllerFuture;
    private MediaController mediaController;

    private ScheduledExecutorService executor;
    private ScheduledFuture<?> future;
    private final Handler handler = new Handler();

    private onBackListener mBackListener;

    public interface onBackListener{
        boolean onBack();
    }

    public void setOnBackListener(onBackListener l){
        mBackListener = l;
    }

    @Override
    public void onBackPressed(){
        if(mBackListener != null && mBackListener.onBack()){
            return; // Consume the event
        }

        super.onBackPressed();
    }

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

        // Retrieve the token
        SharedPreferences sharedPrefs = this.getSharedPreferences(getString(R.string.pref_file_key), Context.MODE_PRIVATE);
        token = sharedPrefs.getString(getString(R.string.token_key), "");

        // Redirect back to login if no token was found
        if(token.equals("")) {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
        }

        mProgressAnim = new ObjectAnimator();
        mProgressAnim.setTarget(mProgress);
        mProgressAnim.setPropertyName("progress");
        mProgressAnim.setDuration(1000);
        mProgressAnim.setInterpolator(new LinearInterpolator());

        SelectedTrackModel selectedTrackModel = new ViewModelProvider(this).get(SelectedTrackModel.class);

        // Observe the shared View Model to update the service's track list & load the selected track
        selectedTrackModel.getSelectedTrack().observe(this, new Observer<SelectedTrackModel.SelectedTrack>() {
            @Override
            public void onChanged(SelectedTrackModel.SelectedTrack selectedTrack) {
                if(selectedTrack == null || selectedTrack.getSelectionSource().equals(LOG_TAG) ||
                        mediaController == null) {
                    return; // Prevent an endless loop if this was triggered by this activity
                }

                List<MediaItem> mediaItems = new ArrayList<>();

                for(int i=0; i < selectedTrack.getTrackList().size(); i++) {
                    JSONObject track = selectedTrack.getTrackList().get(i);
                    MediaItem mediaItem = createMediaItem(track);
                    mediaItems.add(mediaItem);
                }

                mediaController.setMediaItems(mediaItems, selectedTrack.getPos(), 0);

                mediaController.prepare();
                mediaController.play();
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
        SessionToken sessionToken = new SessionToken(this,
                new ComponentName(this, PlayerService.class));

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
        mediaController.addListener(new Player.Listener() {
            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                Player.Listener.super.onIsPlayingChanged(isPlaying);

                updateUI();

                if(!isPlaying && future != null) {
                    future.cancel(true);
                } else if(isPlaying) {
                    future = executor.scheduleAtFixedRate(progressHelperRunnable, 0, 1, TimeUnit.SECONDS);
                }
            }

            @Override
            public void onMediaItemTransition(MediaItem mediaItem, int reason) {
                Player.Listener.super.onMediaItemTransition(mediaItem, reason);
                updateUI();
            }
        });
    }

    private MediaItem createMediaItem(JSONObject track) {
        MediaItem mediaItem = null;

        try {
            Bundle bundle = new Bundle();
            bundle.putString("artwork_url", track.getString("artwork_url"));

            MediaItem.RequestMetadata requestMetadata = new MediaItem.RequestMetadata.Builder()
                    .setMediaUri(Uri.parse(track.getString("stream_url")))
                    .build();

            MediaMetadata mediaMetadata = new MediaMetadata.Builder()
                    .setArtist(track.getJSONObject("user").getString("username"))
                    .setTitle(track.getString("title"))
                    .setArtworkUri(Uri.parse(track.getString("artwork_url")))
                    .setExtras(bundle)
                    .build();

            mediaItem = new MediaItem.Builder()
                    .setMediaId(track.getString("stream_url"))
                    .setMediaMetadata(mediaMetadata)
                    .setRequestMetadata(requestMetadata)
                    .build();
        } catch(JSONException e) {
            Log.e(LOG_TAG, "Unable to create MediaItem", e);
        }

        return mediaItem;
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

    public void redirectToLogin(boolean refreshToken) {
        Intent intent = new Intent(this, LoginActivity.class);

        if(refreshToken) {
            intent.putExtra(LoginActivity.EXTRA_ACTION, LoginActivity.ACTION_REFRESH);
        } else {
            // Remove the stored token data
            SharedPreferences sharedPrefs = this.getSharedPreferences(getString(R.string.pref_file_key),
                    Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPrefs.edit();

            editor.remove(getString(R.string.token_key));
            editor.remove(getString(R.string.refresh_token_key));
            editor.apply();
        }

        startActivity(intent);
        finish();
    }
}
