package sammyt.cloudplayer;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.media2.exoplayer.external.ExoPlayerFactory;
import androidx.media2.exoplayer.external.SimpleExoPlayer;
import androidx.media2.exoplayer.external.source.MediaSource;
import androidx.media2.exoplayer.external.source.ProgressiveMediaSource;
import androidx.media2.exoplayer.external.source.SingleSampleMediaSource;
import androidx.media2.exoplayer.external.upstream.DataSource;
import androidx.media2.exoplayer.external.upstream.DefaultDataSourceFactory;
import androidx.media2.exoplayer.external.util.Util;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import de.voidplus.soundcloud.SoundCloud;
import de.voidplus.soundcloud.Track;
import de.voidplus.soundcloud.User;

public class MainActivity extends AppCompatActivity {

    private final String LOG_TAG = this.getClass().getSimpleName();

    ArrayList<Track> tracks = new ArrayList<>();
    TrackAdapter mAdapter;

    User mUser;
    ArrayList<Track> mFaveTracks;

    SimpleExoPlayer mPlayer;
    DataSource.Factory mDataSourceFactory;

    TextView mInfoView;

    private PlayerService mService;
    private boolean mBound = false;

    @SuppressLint("RestrictedApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mInfoView = findViewById(R.id.track_info);
        Button button = findViewById(R.id.button);
        RecyclerView trackRecycler = findViewById(R.id.track_recycler);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                mPlayer.setPlayWhenReady(!mPlayer.getPlayWhenReady());
                if(mBound){
                    mService.togglePlay(!mService.mIsPlaying);
                }
            }
        });

        LinearLayoutManager layoutManager = new LinearLayoutManager(MainActivity.this);
        trackRecycler.setLayoutManager(layoutManager);

        mAdapter = new TrackAdapter(null);
        mAdapter.setOnTrackClickListener(new TrackAdapter.onTrackClickListener() {
            @Override
            public void onTrackClick(Track track) {
//                loadTrack(track);
                if(mBound){
                    String info = track.getUser().getUsername() + " - " + track.getTitle();
                    mInfoView.setText(info);

                    mService.loadTrack(track);
                }
            }
        });
        trackRecycler.setAdapter(mAdapter);
    }

    @SuppressLint("RestrictedApi")
    @Override
    public void onResume(){
        super.onResume();

        init3();
    }

    @SuppressLint("RestrictedApi")
    @Override
    public void onPause(){
//        if(mPlayer != null){
//            mPlayer.setPlayWhenReady(false);
//            mPlayer.release();
//        }

        Log.d(LOG_TAG, "is playing: " + mService.mIsPlaying);
        if(!mService.mIsPlaying) {
            Log.d(LOG_TAG, "unbind service");
            mService.releasePlayer();
            unbindService(mConnection);
            mBound = false;
        }

        super.onPause();
    }

    @Override
    public void onDestroy(){
        super.onDestroy();

        mService.releasePlayer();
        unbindService(mConnection);
    }

    @SuppressLint("RestrictedApi")
    private void init(){
        final Handler handler = new Handler();
        new Thread(new Runnable() {
            @Override
            public void run() {
                //// TODO: This client info already has access to my account, I'd like to test requesting access
                SoundCloud soundCloud = new SoundCloud(
                        getString(R.string.client_id),
                        getString(R.string.client_secret)
                );

                soundCloud.login(getString(R.string.login_name), getString(R.string.login_password));

                User me = soundCloud.getMe();

                Log.d(LOG_TAG, "---- ME -----\n" + me.toString());

                int count = me.getPublicFavoritesCount();
                int limit = 50;
                int pages = count / limit + 1;

                for(int i=0; i < pages; i++){
                    ArrayList<Track> tempTracks = soundCloud.getMeFavorites(i*limit, limit);
                    tracks.addAll(tempTracks);
                }

                Log.d(LOG_TAG, "----- TRACKS -----\n" + tracks.get(1).toString());

                final Uri uri = Uri.parse(tracks.get(1).getStreamUrl());
                Log.d(LOG_TAG, "---- URI ----\n" + uri);

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        mPlayer = ExoPlayerFactory.newSimpleInstance(MainActivity.this);
                        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(MainActivity.this,
                                Util.getUserAgent(MainActivity.this, "Cloud Player"));

                        MediaSource mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory)
                                .createMediaSource(uri);

                        mPlayer.prepare(mediaSource);
                        mPlayer.setPlayWhenReady(true);
                    }
                });
            }
        }).start();
    }

    private void init2(){
        initPlayer();

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

//                loadTrack(mFaveTracks.get(1));
                mAdapter.updateTracks(mFaveTracks);
            }
        });
    }

    private void init3(){
        if(!mBound) {
            Log.d(LOG_TAG, "bind service");
            Intent intent = new Intent(MainActivity.this, PlayerService.class);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        }

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
            }
        });
    }

    @SuppressLint("RestrictedApi")
    private void initPlayer(){
        mPlayer = ExoPlayerFactory.newSimpleInstance(MainActivity.this);
        mDataSourceFactory = new DefaultDataSourceFactory(MainActivity.this,
                Util.getUserAgent(MainActivity.this, "Cloud Player"));
    }

    private void loadTrack(final Track track){
        final Handler handler = new Handler();
        new Thread(new Runnable() {
            @Override
            public void run() {
                final Uri uri = Uri.parse(track.getStreamUrl());
                Log.d(LOG_TAG, "---- Track URI ----\n" + uri);

                handler.post(new Runnable() {
                    @SuppressLint("RestrictedApi")
                    @Override
                    public void run() {
                        String info = track.getUser().getUsername() + " - " + track.getTitle();
                        mInfoView.setText(info);

                        mPlayer.setPlayWhenReady(false);

                        MediaSource mediaSource = new ProgressiveMediaSource.Factory(mDataSourceFactory)
                                .createMediaSource(uri);

                        mPlayer.prepare(mediaSource);
                        mPlayer.setPlayWhenReady(true);
                    }
                });
            }
        }).start();
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            PlayerService.PlayerBinder binder = (PlayerService.PlayerBinder) service;
            mService = binder.getService();
            mBound = true;

            mService.initPlayer();
            Log.d(LOG_TAG, "service connected");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBound = false;
            Log.d(LOG_TAG, "service disconnected");
        }
    };
}
