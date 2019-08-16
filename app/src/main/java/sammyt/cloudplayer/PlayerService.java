package sammyt.cloudplayer;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.media2.exoplayer.external.ExoPlayerFactory;
import androidx.media2.exoplayer.external.Player;
import androidx.media2.exoplayer.external.SimpleExoPlayer;
import androidx.media2.exoplayer.external.source.MediaSource;
import androidx.media2.exoplayer.external.source.ProgressiveMediaSource;
import androidx.media2.exoplayer.external.upstream.DataSource;
import androidx.media2.exoplayer.external.upstream.DefaultDataSourceFactory;
import androidx.media2.exoplayer.external.util.Util;

import java.util.ArrayList;

import de.voidplus.soundcloud.Track;
import de.voidplus.soundcloud.User;

public class PlayerService extends Service {

    private final String LOG_TAG = this.getClass().getSimpleName();

    private final IBinder mBinder = new PlayerBinder();

    private PlayerServiceListener mListener;

    private Context mContext = PlayerService.this;
    private boolean mIsPlaying = false;
    private int mCurrentTrack;
    private ArrayList<Track> mTracks;

    private SimpleExoPlayer mPlayer;
    private DataSource.Factory mDataSourceFactory;
    private Handler mPlaybackHandler = new Handler();

    public enum AdjustTrack{
        previous, next
    }

    public class PlayerBinder extends Binder{
        PlayerService getService(){
            return PlayerService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent){
        return mBinder;
    }

    public interface PlayerServiceListener{
        void onTrackLoaded(Track track);
        void onPlayback(float duration, float currentPos, float bufferPos);
    }

    public void setPlayerServiceListener(PlayerServiceListener l){
        mListener = l;
    }

    @SuppressLint("RestrictedApi")
    public void initPlayer(){
        if(mPlayer == null) {
            mPlayer = ExoPlayerFactory.newSimpleInstance(mContext);
            mDataSourceFactory = new DefaultDataSourceFactory(mContext,
                    Util.getUserAgent(mContext, "Cloud Player"));
        }
    }

    public void setTrackList(ArrayList<Track> trackList){
        mTracks = trackList;
    }

    public boolean isPlaying(){
        return mIsPlaying;
    }

    public void loadTrack(int trackPos){
        mCurrentTrack = trackPos;

        final Handler handler = new Handler();
        new Thread(new Runnable() {
            @Override
            public void run() {
                final Uri uri = Uri.parse(mTracks.get(mCurrentTrack).getStreamUrl());
                Log.d(LOG_TAG, "---- Track URI ----\n" + uri);

                handler.post(new Runnable() {
                    @SuppressLint("RestrictedApi")
                    @Override
                    public void run() {
                        String trackInfo = mTracks.get(mCurrentTrack).getUser().getUsername() + " - "
                                + mTracks.get(mCurrentTrack).getTitle();

                        mPlayer.setPlayWhenReady(false); // Stop playing the previous track

                        MediaSource mediaSource = new ProgressiveMediaSource.Factory(mDataSourceFactory)
                                .createMediaSource(uri);

                        mPlayer.prepare(mediaSource);
                        mPlayer.setPlayWhenReady(true);

                        mIsPlaying = true;

                        // Call the track loaded interface
                        if(mListener != null){
                            mListener.onTrackLoaded(mTracks.get(mCurrentTrack));
                        }

                        // Start playback monitoring
                        mPlaybackHandler.postDelayed(mProgressRunnable, 0);

                        startForeground(111, buildNotification());
                    }
                });
            }
        }).start();
    }

    // Keeps track of the player's current playback information
    // This Runnable will repeatedly call itself as long as the player isn't idle or ended
    private Runnable mProgressRunnable = new Runnable(){
        @SuppressLint("RestrictedApi")
        @Override
        public void run() {
            mPlaybackHandler.removeCallbacks(mProgressRunnable); // Make sure we're only using one runnable

            long delay = 1000; // Delay for one second

            Log.d(LOG_TAG,"duration: " + mPlayer.getDuration()
                    + " current pos: " + mPlayer.getCurrentPosition()
                    + " buffered pos: " + mPlayer.getBufferedPosition());

            // Provide the playback info to the listener callback
            if(mListener != null){
                mListener.onPlayback(mPlayer.getDuration(), mPlayer.getCurrentPosition(),
                        mPlayer.getBufferedPosition());
            }

            int playbackState = mPlayer.getPlaybackState();

            // Call this Runnable again if the player is in a valid state
            if(playbackState != Player.STATE_IDLE && playbackState != Player.STATE_ENDED){
                mPlaybackHandler.postDelayed(mProgressRunnable, delay);
            }
        }
    };

    @SuppressLint("RestrictedApi")
    public void seekTo(float progress){
        long progressPos = (long)(progress / 100 * mPlayer.getDuration());

        mPlayer.seekTo(progressPos);
    }

    public void adjustTrack(AdjustTrack direction){
        int adjustTrackPos = mCurrentTrack;

        switch(direction){
            case previous:
                if(mCurrentTrack == 0){
                    adjustTrackPos = mTracks.size() - 1;
                }else{
                    adjustTrackPos = mCurrentTrack - 1;
                }
                break;

            case next:
                if(mCurrentTrack == mTracks.size() - 1){
                    adjustTrackPos = 0;
                }else{
                    adjustTrackPos = mCurrentTrack + 1;
                }
                break;
        }

        loadTrack(adjustTrackPos);
    }

    @SuppressLint("RestrictedApi")
    public void togglePlay(boolean play){
        mIsPlaying = play;
        mPlayer.setPlayWhenReady(mIsPlaying);

        // Start or stop the playback monitoring depending on whether the track is playing
        if(mIsPlaying){
            mPlaybackHandler.postDelayed(mProgressRunnable, 0);
        }else{
            mPlaybackHandler.removeCallbacks(mProgressRunnable);
        }
    }

    @SuppressLint("RestrictedApi")
    public void releasePlayer(){
        if(mPlayer != null) {
            mIsPlaying = false;

            mPlayer.setPlayWhenReady(false);
            mPlayer.release();
        }
    }

    private Notification buildNotification(){

        // Create Notification Channel on API 26+
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            NotificationManager notificationManager = (NotificationManager) this
                    .getSystemService(Context.NOTIFICATION_SERVICE);

            // Register the channels with the system.
            // Importance & Notification behaviors can't be changed after this
            notificationManager.createNotificationChannel(createChannel());
        }

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        //// TODO: Build a Media Control Notification
        NotificationCompat.Builder notifyBuilder = new NotificationCompat.Builder(this, "CloudPlayer");
        notifyBuilder.setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Cloud player title")
                .setContentText("Cloud player text")
                .setColor(ContextCompat.getColor(this, R.color.colorPrimary))
                .setContentIntent(pendingIntent);

        return notifyBuilder.build();
    }

    @TargetApi(26)
    private NotificationChannel createChannel(){
        //// TODO: Put useful values in here
        String channelName = "CloudPlayer channel name";
        String channelDescription = "CloudPlayer channel description";
        int importance = NotificationManager.IMPORTANCE_LOW;

        NotificationChannel channel = new NotificationChannel("CloudPlayer", channelName, importance);
        channel.setDescription(channelDescription);
        channel.enableVibration(true);
        channel.enableLights(true);
        channel.setLightColor(R.color.colorPrimary);

        return channel;
    }
}
