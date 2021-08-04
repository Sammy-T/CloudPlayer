package sammyt.cloudplayer;

import android.animation.ObjectAnimator;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONException;
import org.json.JSONObject;

import sammyt.cloudplayer.nav.SelectedTrackModel;
import sammyt.cloudplayer.player.PlayerActivity;

public class NavActivity extends AppCompatActivity implements PlayerService.PlayerServiceListener {

    private final String LOG_TAG = this.getClass().getSimpleName();

    public String token;

    private SelectedTrackModel selectedTrackModel;

    private ImageButton mPlay;
    private RelativeLayout mInfoArea;
    private TextView mTitle;
    private TextView mArtist;
    private ProgressBar mProgress;

    private PlayerService mService;
    private boolean mBound = false;
    private boolean mListenerConnected = false;

    private ObjectAnimator mProgressAnim;

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
                if(mBound){
                    if(selectedTrack.getSelectionSource().equals(LOG_TAG)){
                        return; // Prevent an endless loop if this was triggered by this activity
                    }

                    mService.setTrackList(selectedTrack.getTrackList());
                    mService.loadTrack(selectedTrack.getPos());
                }
            }
        });

        mPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mBound){
                    mService.togglePlay(!mService.isPlaying());
                    updateUI();
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
    public void onResume(){
        super.onResume();

        init();

        // If we're resuming this activity with the service still bound
        // make sure we still have a listener
        if(mBound && !mListenerConnected){
            mService.setPlayerServiceListener(NavActivity.this);
            mListenerConnected = true;
        }
    }

    @Override
    public void onPause(){
        // Unbind the service if nothing is playing
        if (mBound && !mService.isPlaying()) {
            Log.d(LOG_TAG, "unbind service");
            unbindService(mConnection);
            mBound = false;
        }

        mListenerConnected = false;

        super.onPause();
    }

    @Override
    public void onDestroy(){
        // The activity is being destroyed,
        // unbind the service
        if(mBound) {
            Log.d(LOG_TAG, "unbind service");
            unbindService(mConnection);
        }

        super.onDestroy();
    }

    private void init(){
        if(!mBound){
            Log.d(LOG_TAG, "Bind Service");
            Intent intent = new Intent(this, PlayerService.class);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        }else{
            updateUI();

            // If we're resuming this activity
            // update the shared View Model so other observers can respond
            JSONObject track = mService.getCurrentTrack();
            if(track != null){
                selectedTrackModel.updateSelectedTrack(mService.getTrackPosition(), track, LOG_TAG);
            }
        }
    }

    // Updates the Mini Player's playback buttons & track info
    private void updateUI(){
        // Update the play button
        int playOrPause;

        if(mService.isPlaying()){
            playOrPause = R.drawable.ic_pause_white_24dp;
        }else{
            playOrPause = R.drawable.ic_play_white_24dp;
        }

        mPlay.setImageResource(playOrPause);

        // Update the track info
        if(mService.getCurrentTrack() != null){
            String title;
            String artist;

            try{
                title = mService.getCurrentTrack().getString("title");
                artist = mService.getCurrentTrack().getJSONObject("user").getString("username");
            }catch(JSONException e){
                Log.e(LOG_TAG, "Unable to retrieve current track info.", e);
                return;
            }

            mTitle.setText(title);
            mArtist.setText(artist);
        }
    }

    // From the Player Service Interface
    public void onTrackLoaded(int trackPos, JSONObject track){
        updateUI();

        // Update the shared View Model so other observers can respond to the updated data
        selectedTrackModel.updateSelectedTrack(trackPos, track, LOG_TAG);
    }

    // From the Player Service Interface
    public void onPlayback(float duration, float currentPos, float bufferPos){
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

    // From the Player Service Interface
    public void onSessionId(int sessionId){
    }

    // Player Service Connection
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.d(LOG_TAG, "Service connected");

            // We've bound to the service, cast the IBinder to our defined Binder
            // and get our Service instance
            PlayerService.PlayerBinder binder = (PlayerService.PlayerBinder) service;
            mService = binder.getService();

            // Set up the interface so we can receive call backs
            // Initialize the player
            mService.setPlayerServiceListener(NavActivity.this);
            mService.initPlayer();

            mBound = true;
            mListenerConnected = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBound = false;
            mListenerConnected = false;

            Log.d(LOG_TAG, "Service disconnected");
        }
    };
}
