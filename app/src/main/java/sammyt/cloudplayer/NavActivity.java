package sammyt.cloudplayer;

import android.animation.ObjectAnimator;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.analytics.PlaybackSessionManager;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionCommands;
import androidx.media3.session.SessionToken;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.common.util.concurrent.ListenableFuture;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.ExecutionException;

import sammyt.cloudplayer.nav.SelectedTrackModel;
import sammyt.cloudplayer.player.PlayerActivity;

public class NavActivity extends AppCompatActivity {

    private final String LOG_TAG = this.getClass().getSimpleName();

    public String token;

    private SelectedTrackModel selectedTrackModel;

    private ImageButton mPlay;
    private RelativeLayout mInfoArea;
    private TextView mTitle;
    private TextView mArtist;
    private ProgressBar mProgress;

    private ObjectAnimator mProgressAnim;

    private ListenableFuture<MediaController> controllerFuture;
    private MediaController mediaController;

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

        setContentView(R.layout.activity_nav);
        BottomNavigationView navView = findViewById(R.id.nav_view);
        mPlay = findViewById(R.id.mini_play_pause);
        mInfoArea = findViewById(R.id.mini_info_area);
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

        selectedTrackModel = new ViewModelProvider(this).get(SelectedTrackModel.class);

        // Observe the shared View Model to update the service's track list & load the selected track
        selectedTrackModel.getSelectedTrack().observe(this, new Observer<SelectedTrackModel.SelectedTrack>() {
            @Override
            public void onChanged(SelectedTrackModel.SelectedTrack selectedTrack) {
                if(selectedTrack == null || selectedTrack.getSelectionSource().equals(LOG_TAG) ||
                        mediaController == null) {
                    return; // Prevent an endless loop if this was triggered by this activity
                }

                try {
                    JSONObject track = selectedTrack.getTrack();

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

                    MediaItem mediaItem = new MediaItem.Builder()
                            .setMediaId(track.getString("stream_url"))
                            .setMediaMetadata(mediaMetadata)
                            .setRequestMetadata(requestMetadata)
                            .build();
                    mediaController.setMediaItem(mediaItem);
                    mediaController.prepare();
                    mediaController.play();
                } catch(JSONException e) {
                    Log.e(LOG_TAG, "Unable to set MediaItem", e);
                }
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

        mInfoArea.setOnClickListener(new View.OnClickListener() {
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
            }
        });
    }

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
