package sammyt.cloudplayer;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

//import com.gauravk.audiovisualizer.visualizer.BarVisualizer;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import de.voidplus.soundcloud.Track;
import de.voidplus.soundcloud.User;
import me.bogerchan.niervisualizer.NierVisualizerManager;
import me.bogerchan.niervisualizer.renderer.IRenderer;
import me.bogerchan.niervisualizer.renderer.columnar.ColumnarType1Renderer;

public class MainActivity extends AppCompatActivity implements PlayerService.PlayerServiceListener {

    private final String LOG_TAG = this.getClass().getSimpleName();

    private TrackAdapter mAdapter;

    private User mUser;
    private ArrayList<Track> mFaveTracks;

    private PlayerService mService;
    private boolean mBound = false;

    private boolean mIsDragging = false;

//    private BarVisualizer mVisualizer;
    private SurfaceView mSurface;
    private ImageView mImageView;
    private TextView mInfoView;
    private SeekBar mSeekBar;
    private TextView mTrackTime;
    private Button mPlay;
    private RecyclerView mTrackRecycler;
    private ProgressBar mLoading;

    private NierVisualizerManager mVisualizerManager;

    private enum VisibleView{
        recycler, loading
    }

    @SuppressLint("RestrictedApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        mVisualizer = findViewById(R.id.visualizer);
        mSurface = findViewById(R.id.surface);
        mImageView = findViewById(R.id.track_image);
        mInfoView = findViewById(R.id.track_info);
        mSeekBar = findViewById(R.id.track_seekbar);
        mTrackTime = findViewById(R.id.track_time);
        mPlay = findViewById(R.id.play);
        Button previous = findViewById(R.id.previous);
        Button next = findViewById(R.id.next);
        mTrackRecycler = findViewById(R.id.track_recycler);
        mLoading = findViewById(R.id.loading);

        mSurface.setZOrderOnTop(true);
        mSurface.getHolder().setFormat(PixelFormat.TRANSLUCENT);

        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(mBound && fromUser){
                    mService.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                mIsDragging = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mIsDragging = false;
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

        previous.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mBound){
                    mService.adjustTrack(PlayerService.AdjustTrack.previous);
                }
            }
        });

        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mBound){
                    mService.adjustTrack(PlayerService.AdjustTrack.next);
                }
            }
        });

        LinearLayoutManager layoutManager = new LinearLayoutManager(MainActivity.this);
        mTrackRecycler.setLayoutManager(layoutManager);

        mAdapter = new TrackAdapter(null);
        mAdapter.setOnTrackClickListener(new TrackAdapter.onTrackClickListener() {
            @Override
            public void onTrackClick(int position, Track track) {
                if(mBound){
                    mService.loadTrack(position);
                }
            }
        });

        mTrackRecycler.setAdapter(mAdapter);
    }

    @SuppressLint("RestrictedApi")
    @Override
    public void onResume(){
        super.onResume();

        init();

        if(mVisualizerManager != null && mService.isPlaying()){
            mVisualizerManager.resume();
        }
    }

    @SuppressLint("RestrictedApi")
    @Override
    public void onPause(){
        if(mBound) {
            Log.d(LOG_TAG, "is playing: " + mService.isPlaying());
        }

        // Release the player & unbind the service if nothing is playing
        if (mBound && !mService.isPlaying()) {
            Log.d(LOG_TAG, "unbind service");

            unbindService(mConnection);
            mBound = false;
        }

        if(mVisualizerManager != null){
            mVisualizerManager.pause();
        }

        super.onPause();
    }

    @Override
    public void onDestroy(){
        // The activity is being destroyed,
        // release the player & unbind the service
        if(mBound) {
            Log.d(LOG_TAG, "unbind service");

            unbindService(mConnection);
        }

        if(mVisualizerManager != null) {
            mVisualizerManager.stop();
            mVisualizerManager.release();
        }

        super.onDestroy();
    }

    private void init(){
        setVisibleView(VisibleView.loading);

        if(!mBound){
            Log.d(LOG_TAG, "bind service");

            Intent intent = new Intent(MainActivity.this, PlayerService.class);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        }else{
            updateUI();
        }

        // Get the user and favorite tracks
        UserAndTracksTask initTask = new UserAndTracksTask(
                getString(R.string.client_id),
                getString(R.string.client_secret),
                getString(R.string.login_name),
                getString(R.string.login_password));
        initTask.execute();
        initTask.setOnFinishListener(new UserAndTracksTask.onFinishListener() {
            @Override
            public void onFinish(User user, ArrayList<Track> faveTracks) {
                Log.d(LOG_TAG, "---- User ----\n" + user.toString());
                Log.d(LOG_TAG, "---- Track 1 ----\n" + faveTracks.get(1).toString());

                mUser = user;
                mFaveTracks = faveTracks;

                mAdapter.updateTracks(mFaveTracks);
                mService.setTrackList(mFaveTracks);

                mService.setUser(mUser);

                setVisibleView(VisibleView.recycler);
            }

            @Override
            public void onFailure(){
                // Try to get the values from the service
                if(mBound){
                    mUser = mService.getUser();
                    mFaveTracks = mService.getTrackList();

                    Log.d(LOG_TAG, "Tried retrieving from Service." +
                            "\nuser: " + mUser + "\ntracks: " + mFaveTracks);

                    if(mFaveTracks != null){
                        mAdapter.updateTracks(mFaveTracks);

                        setVisibleView(VisibleView.recycler);

                        Toast.makeText(MainActivity.this, "Retrieved data from service",
                                Toast.LENGTH_SHORT).show();
                    }
                    //// TODO: Else, provide a button to request a manual load
                }
            }
        });
    }

    private void setVisibleView(VisibleView visibleView){
        switch(visibleView){
            case loading:
                mTrackRecycler.setVisibility(View.GONE);
                mLoading.setVisibility(View.VISIBLE);
                break;

            case recycler:
                mLoading.setVisibility(View.GONE);
                mTrackRecycler.setVisibility(View.VISIBLE);
                break;
        }
    }

    private void updateUI(){
        if(mService.isPlaying()){
            mPlay.setText("Pause");
        }else{
            mPlay.setText("Play");
        }
    }

    // From the Player Service Interface
    public void onTrackLoaded(Track track){
        Log.d(LOG_TAG, "art url: " + track.getArtworkUrl());

        String rawUrl = track.getArtworkUrl();
        if(rawUrl != null){
            //// TODO: Create a helper function to check if path exists and return the largest available image?
            final String trackArtUrl = rawUrl.replace("large", "t500x500");

            // Load the track image
            Picasso.get()
                    .load(trackArtUrl)
                    .resize(mImageView.getWidth(), mImageView.getHeight())
                    .onlyScaleDown()
                    .centerCrop()
                    .error(android.R.drawable.stat_notify_error)
                    .into(mImageView, new Callback() {
                        @Override
                        public void onSuccess() {
                            Log.d(LOG_TAG, "Picasso successfully loaded " + trackArtUrl);
                        }

                        @Override
                        public void onError(Exception e) {
                            Log.e(LOG_TAG, "Picasso error " + trackArtUrl, e);
                        }
                    });
        }else{
            // Load the fallback image
            Picasso.get()
                    .load(R.drawable.ic_play_grey600_36dp)
                    .resize(mImageView.getWidth(), mImageView.getHeight())
                    .onlyScaleDown()
                    .centerInside()
                    .error(android.R.drawable.stat_notify_error)
                    .into(mImageView, new Callback() {
                        @Override
                        public void onSuccess() {
                            Log.d(LOG_TAG, "Picasso successfully loaded fallback image");
                        }

                        @Override
                        public void onError(Exception e) {
                            Log.e(LOG_TAG, "Picasso error loading fallback image", e);
                        }
                    });
        }

        String info = track.getUser().getUsername() + " - " + track.getTitle();
        mInfoView.setText(info);

        updateUI();
    }

    // From the Player Service Interface
    public void onPlayback(float duration, float currentPos, float bufferPos){
        int progress = (int) (currentPos / duration * 100);
        int bufferProgress = (int) (bufferPos / duration * 100);

        if(!mIsDragging){
            mSeekBar.setProgress(progress);
            mSeekBar.setSecondaryProgress(bufferProgress);
        }

        // Build the time display of the track
        String currentText = new SimpleDateFormat("mm:ss", Locale.getDefault())
                .format(new Date((long)currentPos));

        String durationText = new SimpleDateFormat("mm:ss", Locale.getDefault())
                .format(new Date((long)duration));

        String timeText = currentText + " / " + durationText;

        mTrackTime.setText(timeText);
    }

    //// TODO: Check & Request record permission
    // From the Player Service Interface
    public void onSessionId(int sessionId){

        if(mVisualizerManager != null){
            mVisualizerManager.stop();
            mVisualizerManager.release();
        }

        Log.d(LOG_TAG, "onSessionId: " + sessionId);

        mVisualizerManager = new NierVisualizerManager();

        int state = mVisualizerManager.init(sessionId);
        if (NierVisualizerManager.SUCCESS != state) {
            Log.e(LOG_TAG, "Error initializing visualizer manager");
            return;
        }
        Log.d(LOG_TAG, "state: " + state);

        Paint visPaint = new Paint();
        visPaint.setColor(ContextCompat.getColor(this, R.color.colorAccent));
        visPaint.setAlpha(150);

        mVisualizerManager.start(mSurface, new IRenderer[]{new ColumnarType1Renderer(visPaint)});
    }

    // Player Service Connection
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.d(LOG_TAG, "service connected");

            // We've bound to the service, cast the IBinder to our defined Binder
            // and get our Service instance
            PlayerService.PlayerBinder binder = (PlayerService.PlayerBinder) service;
            mService = binder.getService();
            mBound = true;

            // Set up the interface so we can receive call backs
            // then initialize the player
            mService.setPlayerServiceListener(MainActivity.this);
            mService.initPlayer();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBound = false;

            Log.d(LOG_TAG, "service disconnected");
        }
    };
}
