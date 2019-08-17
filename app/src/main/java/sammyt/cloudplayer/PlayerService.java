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
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
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

import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.ArrayList;

import de.voidplus.soundcloud.Track;
import de.voidplus.soundcloud.User;

public class PlayerService extends Service {

    private final String LOG_TAG = this.getClass().getSimpleName();

    private static final String PLAYER_ACTION_EXTRA = "EXTRA_PLAYER_ACTION";
    private static final int PLAYER_ACTION_PLAY_PAUSE = 0;
    private static final int PLAYER_ACTION_NEXT = 1;
    private static final int PLAYER_ACTION_PREV = 2;

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

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        Log.d(LOG_TAG, "PlayerService onStartCommand // intent: " + intent + " flags: "
                + flags + " startId: " + startId);

        if(intent.hasExtra(PLAYER_ACTION_EXTRA)){
            switch(intent.getIntExtra(PLAYER_ACTION_EXTRA, 0)){
                case PLAYER_ACTION_PLAY_PAUSE:
                    togglePlay(!mIsPlaying);
                    break;

                case PLAYER_ACTION_PREV:
                    adjustTrack(AdjustTrack.previous);
                    break;

                case PLAYER_ACTION_NEXT:
                    adjustTrack(AdjustTrack.next);
                    break;
            }

            buildMediaNotification(); // Update the notification
        }
        return START_STICKY;
    }

    //// TODO: I don't really need onDestroy so I should remove this
    @Override
    public void onDestroy(){
        Log.d(LOG_TAG, "PlayerService destroyed");
        super.onDestroy();
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

                        // Build the Media Notification & place the service in the foreground
                        buildMediaNotification();
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

    private void buildMediaNotification(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                // Create Notification Channel on API 26+
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                    NotificationManager notificationManager = (NotificationManager) mContext
                            .getSystemService(Context.NOTIFICATION_SERVICE);

                    // Register the channels with the system.
                    // Importance & Notification behaviors can't be changed after this
                    notificationManager.createNotificationChannel(createChannel());
                }

                Intent notificationIntent = new Intent(mContext, MainActivity.class);
                PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 650,
                        notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

                Track track = mTracks.get(mCurrentTrack);
                String trackTitle = track.getTitle();
                String trackArtist = track.getUser().getUsername();
                String trackImage = track.getArtworkUrl();

                NotificationCompat.Builder notifyBuilder = new NotificationCompat
                        .Builder(mContext, "CloudPlayer");
                notifyBuilder.setSmallIcon(R.drawable.play)
                        .setContentTitle(trackTitle)
                        .setContentText(trackArtist)
                        .setColor(ContextCompat.getColor(mContext, R.color.colorPrimary))
                        .setContentIntent(pendingIntent)
                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

                try {
                    Bitmap trackImageIcon = Picasso.get()
                            .load(trackImage)
                            .resize(1000, 1000)
                            .onlyScaleDown()
                            .centerInside()
                            .get();

                    notifyBuilder.setLargeIcon(trackImageIcon);

                }catch(IOException e){
                    Log.e(LOG_TAG, "Error getting bitmap", e);
                }

                PendingIntent previousPendingIntent = createActionPendingIntent(PLAYER_ACTION_PREV);
                PendingIntent playPendingIntent = createActionPendingIntent(PLAYER_ACTION_PLAY_PAUSE);
                PendingIntent nextPendingIntent = createActionPendingIntent(PLAYER_ACTION_NEXT);

                int playOrPauseIcon = R.drawable.play;

                if(mIsPlaying){
                    playOrPauseIcon = R.drawable.pause;
                }

                notifyBuilder.addAction(R.drawable.skip_previous, "Previous", previousPendingIntent); // #0
                notifyBuilder.addAction(playOrPauseIcon, "Play", playPendingIntent); // #1
                notifyBuilder.addAction(R.drawable.skip_next, "Next", nextPendingIntent); // # 2

                // Set this as a Media Style notification with 3 actions visible in its compact mode
                notifyBuilder.setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setShowActionsInCompactView(0, 1, 2)); // The indices of the actions. Can use up to 3 actions
                //// TODO: Try to get media session from player?

                startForeground(111, notifyBuilder.build());
            }
        }).start();
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

    // Creates the Pending Intent corresponding to the notification button's player action
    private PendingIntent createActionPendingIntent(int playerAction){
        Intent actionIntent = new Intent(mContext, PlayerService.class);
        actionIntent.putExtra(PLAYER_ACTION_EXTRA, playerAction);

        int requestCode = 651;

        switch(playerAction){
            case PLAYER_ACTION_PLAY_PAUSE:
                requestCode = 651;
                break;
            case PLAYER_ACTION_PREV:
                requestCode = 652;
                break;
            case PLAYER_ACTION_NEXT:
                requestCode = 653;
                break;
        }

        return PendingIntent.getService(mContext, requestCode, actionIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    }
}
