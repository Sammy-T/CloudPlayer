package sammyt.cloudplayer;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.squareup.picasso.Picasso;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import de.voidplus.soundcloud.Track;
import sammyt.cloudplayer.ui.SelectedTrackModel;
import sammyt.cloudplayer.ui.player.PlayerActivity;

public class NavActivity extends AppCompatActivity implements PlayerService.PlayerServiceListener {

    private final String LOG_TAG = this.getClass().getSimpleName();

    private SelectedTrackModel selectedTrackModel;

    private ImageButton mPlay;
    private RelativeLayout mInfoArea;
    private TextView mTitle;
    private TextView mArtist;
    private ProgressBar mProgress;

    private PlayerService mService;
    private boolean mBound = false;

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

        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
//        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
//                R.id.navigation_home, R.id.navigation_artists, R.id.navigation_playlists)
//                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
//        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navView, navController);

        selectedTrackModel = ViewModelProviders.of(this).get(SelectedTrackModel.class);

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
    }

    @Override
    public void onPause(){
        // Unbind the service if nothing is playing
        if (mBound && !mService.isPlaying()) {
            Log.d(LOG_TAG, "unbind service");

            unbindService(mConnection);
            mBound = false;
        }

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

            Track track = mService.getCurrentTrack();
            if(track != null){
                selectedTrackModel.updateSelectedTrack(mService.getTrackPosition(), track, LOG_TAG);
            }
        }
    }

    // Updates the Mini Player's playback buttons & track info
    private void updateUI(){
        if(mService.isPlaying()){
            Picasso.get()
                    .load(R.drawable.ic_pause_white_24dp)
                    .into(mPlay);
        }else{
            Picasso.get()
                    .load(R.drawable.ic_play_white_24dp)
                    .into(mPlay);
        }

        if(mService.getCurrentTrack() != null){
            String title = mService.getCurrentTrack().getTitle();
            String artist = mService.getCurrentTrack().getUser().getUsername();

            mTitle.setText(title);
            mArtist.setText(artist);
        }
    }

    // From the Player Service Interface
    public void onTrackLoaded(int trackPos, Track track){
        updateUI();

        // Update the shared View Model so other observers can respond to the updated data
        selectedTrackModel.updateSelectedTrack(trackPos, track, LOG_TAG);
    }

    // From the Player Service Interface
    public void onPlayback(float duration, float currentPos, float bufferPos){
        int progress = (int) (currentPos / duration * 100);
        mProgress.setProgress(progress);
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
            mBound = true;

            // Set up the interface so we can receive call backs
            // then initialize the player
            mService.setPlayerServiceListener(NavActivity.this);
            mService.initPlayer();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBound = false;

            Log.d(LOG_TAG, "Service disconnected");
        }
    };
}
