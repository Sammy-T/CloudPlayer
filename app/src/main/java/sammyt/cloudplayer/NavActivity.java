package sammyt.cloudplayer;

import android.animation.ObjectAnimator;
import android.content.ComponentName;
import android.content.Intent;
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

import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import sammyt.cloudplayer.data.MediaQueue;
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

        MediaQueue queue = MediaQueue.getInstance();

        // Observe the shared View Model to update the queue's track list & selected track position
        selectedTrackModel.getSelectedTrack().observe(this, new Observer<SelectedTrackModel.SelectedTrack>() {
            @Override
            public void onChanged(SelectedTrackModel.SelectedTrack selectedTrack) {
                if(selectedTrack == null || selectedTrack.getSelectionSource().equals(LOG_TAG) || mediaController == null) {
                    return; // Prevent an endless loop if this was triggered by this activity
                }

                queue.setQueue(selectedTrack.getTrackList());
                queue.setPosition(selectedTrack.getPos());
            }
        });

        mPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mediaController == null) return;

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

    @OptIn(markerClass = UnstableApi.class)
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

                selectedTrackModel.updateSelectedTrack(mediaController.getCurrentMediaItem(), LOG_TAG);
            }
        });
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
            updateProgress(mediaController.getDuration(), mediaController.getCurrentPosition(), mediaController.getBufferedPosition());
        }
    };
}
