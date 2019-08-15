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

    private Context mContext = PlayerService.this;
    public boolean mIsPlaying = false;
    private ArrayList<Track> mTracks;

    private SimpleExoPlayer mPlayer;
    private DataSource.Factory mDataSourceFactory;

    public class PlayerBinder extends Binder{
        PlayerService getService(){
            return PlayerService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent){
        return mBinder;
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

    public void loadTrack(final Track track){
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
                        String trackInfo = track.getUser().getUsername() + " - " + track.getTitle();

                        mPlayer.setPlayWhenReady(false);

                        MediaSource mediaSource = new ProgressiveMediaSource.Factory(mDataSourceFactory)
                                .createMediaSource(uri);

                        mPlayer.prepare(mediaSource);
                        mPlayer.setPlayWhenReady(true);

                        mIsPlaying = true;

                        startForeground(111, buildNotification());
                    }
                });
            }
        }).start();
    }

    @SuppressLint("RestrictedApi")
    public void togglePlay(boolean play){
        mIsPlaying = play;
        mPlayer.setPlayWhenReady(mIsPlaying);
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

            // Register the channels with the system
            // importance & notification behaviors can't be changed after this
            notificationManager.createNotificationChannel(createChannel());
        }

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder notifyBuilder = new NotificationCompat.Builder(this, "CloudPlayer");
        notifyBuilder.setSmallIcon(R.drawable.ic_launcher_background)
                .setContentTitle("Cloud player title")
                .setContentText("Cloud player text")
                .setColor(ContextCompat.getColor(this, R.color.colorPrimary))
                .setContentIntent(pendingIntent);

        return notifyBuilder.build();
    }

    @TargetApi(26)
    private NotificationChannel createChannel(){
        //// TODO:
        String channelName = "CloudPlayer channel name";
        String channelDescription = "CloudPlayer channel description";
        int importance = NotificationManager.IMPORTANCE_DEFAULT;

        NotificationChannel channel = new NotificationChannel("CloudPlayer", channelName, importance);
        channel.setDescription(channelDescription);
        channel.enableVibration(true);
        channel.enableLights(true);
        channel.setLightColor(R.color.colorPrimary);

        return channel;
    }
}
