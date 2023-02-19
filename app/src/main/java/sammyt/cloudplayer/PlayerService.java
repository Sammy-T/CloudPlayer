package sammyt.cloudplayer;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.analytics.AnalyticsListener;
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MediaSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class PlayerService extends Service {

    private static final String LOG_TAG = PlayerService.class.getSimpleName();

    private static final String PLAYER_ACTION_EXTRA = "EXTRA_PLAYER_ACTION";
    private static final int PLAYER_ACTION_PLAY_PAUSE = 0;
    private static final int PLAYER_ACTION_NEXT = 1;
    private static final int PLAYER_ACTION_PREV = 2;

    private final IBinder mBinder = new PlayerBinder();

    private PlayerServiceListener mListener;

    private Context mContext = PlayerService.this;
    private JSONObject mUser;
    private boolean mIsPlaying = false;
    private boolean mRepeat = false;
    private boolean mShuffle = false;
    private int mCurrentTrack;
    private ArrayList<JSONObject> mTracks;
    private ArrayList<JSONObject> mOriginalTracks;

    private Uri mUri;
    private ExoPlayer mPlayer;
    private DefaultHttpDataSource.Factory dataSourceFactory;
    private Handler mHandler = new Handler();
    private ScheduledExecutorService mExecutor;
    private ScheduledFuture<?> mFuture;

    private AudioManager mAudioManager;
    private AudioManager.OnAudioFocusChangeListener mFocusListener;
    private AudioFocusRequest mFocusRequest;

    public enum AdjustTrack{
        previous, next
    }

    public class PlayerBinder extends Binder{
        public PlayerService getService(){
            return PlayerService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(LOG_TAG, "PlayerService created.");

        mExecutor = Executors.newSingleThreadScheduledExecutor();

        // Retrieve the token
        SharedPreferences sharedPrefs = mContext.getSharedPreferences(getString(R.string.pref_file_key), Context.MODE_PRIVATE);
        String token = sharedPrefs.getString(getString(R.string.token_key), "");

        // Create the request header parameters
        Map<String, String> params = new HashMap<>();
        params.put("Authorization", "OAuth " + token);

        // Create the Data Source factory and add the header parameters
        dataSourceFactory = new DefaultHttpDataSource.Factory();
        dataSourceFactory.setDefaultRequestProperties(params);
    }

    @Override
    public IBinder onBind(Intent intent){
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        Log.d(LOG_TAG, "PlayerService onStartCommand // intent: " + intent + " flags: "
                + flags + " startId: " + startId);

        // Respond to intents from the media notification
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

            buildMediaNotification(mIsPlaying); // Update the notification
        }
        return START_STICKY;
    }

    @Override
    public boolean onUnbind(Intent intent){
        Log.d(LOG_TAG, "on unbind");
        stopSelf();

        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy(){
        Log.d(LOG_TAG, "PlayerService destroyed");
        releasePlayer();

        // Cancel and shutdown the playback monitoring executor
        if(mFuture != null){
            mFuture.cancel(true);
        }
        mExecutor.shutdown();

        super.onDestroy();
    }

    public interface PlayerServiceListener{
        void onTrackLoaded(int trackPos, JSONObject track);
        void onPlayback(float duration, float currentPos, float bufferPos);
    }

    public void setPlayerServiceListener(PlayerServiceListener l){
        mListener = l;
    }

    @SuppressLint("RestrictedApi")
    public void initPlayer(){
        if(mPlayer == null) {
            AnalyticsListener analyticsListener = new AnalyticsListener() {
                @Override
                public void onPlaybackStateChanged(@NonNull EventTime eventTime, int state) {
                    // Loads the next track when current track has ended
                    if(state == Player.STATE_ENDED){
                        if(mRepeat){
                            loadTrack(mCurrentTrack); // Repeat the current track
                        }else if(mShuffle && mCurrentTrack == mTracks.size() - 1){
                            // Restart from the first position and shuffle the list again
                            mCurrentTrack = 0;
                            toggleShuffle(true);
                        }else{
                            adjustTrack(AdjustTrack.next); // Play the next track
                        }
                    }
                }
            };

            mPlayer = new ExoPlayer.Builder(mContext).build();
            mPlayer.addAnalyticsListener(analyticsListener);

            initFocus();
        }
    }

    // Initializes the Audio Manager, Audio Focus Listener, & Audio Focus Request(Api 26+)
    private void initFocus(){
        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);

        mFocusListener = new AudioManager.OnAudioFocusChangeListener() {
            @Override
            public void onAudioFocusChange(int focusChange) {
                switch(focusChange){
                    case AudioManager.AUDIOFOCUS_GAIN:
                        if(!mIsPlaying){
                            togglePlay(true);
                        }
                        break;
                    case AudioManager.AUDIOFOCUS_LOSS:
                        if(mIsPlaying) {
                            togglePlay(false);
                        }
                        break;
                        //// TODO: Do I want to implement the other states?
                }
            }
        };

        // Audio Focus Request is only supported on Api 26+
        if(Build.VERSION.SDK_INT >= 26){
            AudioAttributes playbackAttr = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build();

            mFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(playbackAttr)
                    .setOnAudioFocusChangeListener(mFocusListener)
                    .build();
        }
    }

    // Requests audio focus returning whether or not it was granted
    private boolean requestFocus(){
        int focusResult;

        if(Build.VERSION.SDK_INT >= 26){
            focusResult = mAudioManager.requestAudioFocus(mFocusRequest);
        }else{
            focusResult = mAudioManager.requestAudioFocus(mFocusListener,
                    AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        }

        if(focusResult == AudioManager.AUDIOFOCUS_REQUEST_GRANTED){
            return true;
        }else{
            Toast.makeText(mContext, "Unable to get audio focus", Toast.LENGTH_SHORT).show();
            Log.w(LOG_TAG, "Unable to get audio focus");
            return false;
        }
    }

    // Removes audio focus
    private void abandonFocus(){
        if(Build.VERSION.SDK_INT >= 26){
            mAudioManager.abandonAudioFocusRequest(mFocusRequest);
        }else{
            mAudioManager.abandonAudioFocus(mFocusListener);
        }
    }

    public void setUser(JSONObject user){
        mUser = user;
    }

    public JSONObject getUser(){
        return mUser;
    }

    public void setTrackList(ArrayList<JSONObject> trackList){
        if(mTracks == null){
            mTracks = new ArrayList<>();
        }

        if(mOriginalTracks == null){
            mOriginalTracks = new ArrayList<>();
        }

        mTracks.clear();
        mTracks.addAll(trackList);

        mOriginalTracks.clear();
        mOriginalTracks.addAll(trackList);
    }

    public ArrayList<JSONObject> getTrackList(){
        return mTracks;
    }

    public JSONObject getCurrentTrack(){
        if(mTracks == null){
            return null;
        }

        return mTracks.get(mCurrentTrack);
    }

    public int getTrackPosition(){
        return mCurrentTrack;
    }

    public void removeTrackFromList(int position){
        mTracks.remove(position);

        if(position < mCurrentTrack){
            mCurrentTrack--;
        }else if(position == mCurrentTrack){
            loadTrack(mCurrentTrack);
        }
    }

    public boolean isPlaying(){
        return mIsPlaying;
    }

    public void toggleRepeat(boolean repeat){
        mRepeat = repeat;
    }

    public boolean getRepeat(){
        return mRepeat;
    }

    public void toggleShuffle(boolean shuffle){
        if(mTracks == null){
            return;
        }

        mShuffle = shuffle;

        if(mShuffle){
            Collections.shuffle(mTracks);
        }else{
            mTracks.clear();
            mTracks.addAll(mOriginalTracks);
        }

        loadTrack(mCurrentTrack);
    }

    public boolean getShuffle(){
        return mShuffle;
    }

    public int getSessionId(){
        return mPlayer.getAudioSessionId();
    }

    public void loadTrack(int trackPos){
        if(!requestFocus()){
            return; // Return early if we don't have authorization to play
        }

        mCurrentTrack = trackPos;

        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try{
                    mUri = Uri.parse(mTracks.get(mCurrentTrack).getString("stream_url"));
                    Log.d(LOG_TAG, "---- Track URI ----\n" + mUri);
                }catch(JSONException | NullPointerException e){
                    String message = "Error loading track. Possible Server Issue.";
                    Log.e(LOG_TAG, message, e);
                    showTrackLoadError(message);
                    return;
                }

                mHandler.post(new Runnable() {
                    @SuppressLint("RestrictedApi")
                    @Override
                    public void run() {
//                        String trackInfo = mTracks.get(mCurrentTrack).getUser().getUsername() + " - "
//                                + mTracks.get(mCurrentTrack).getTitle();

                        mPlayer.stop(); // Stop playing the previous track

                        // Prepare the Media Source using the DefaultHttpDataSource.Factory
                        // so the custom authorization parameter is included in the request header.
                        MediaSource.Factory mediaSourceFactory = new DefaultMediaSourceFactory(dataSourceFactory);
                        MediaSource mediaSource = mediaSourceFactory.createMediaSource(MediaItem.fromUri(mUri));

                        // Prepare the player with the track and start playing
                        mPlayer.setMediaSource(mediaSource);

                        mPlayer.prepare();
                        mPlayer.play();

                        mIsPlaying = true;

                        // Call the track loaded interface
                        if(mListener != null){
                            mListener.onTrackLoaded(mCurrentTrack, mTracks.get(mCurrentTrack));
                        }

                        // Start playback monitoring
                        if(mFuture != null){
                            mFuture.cancel(true);
                        }
                        mFuture = mExecutor.scheduleAtFixedRate(mProgressHelperRunnable, 0, 1, TimeUnit.SECONDS);

                        // Build the Media Notification & place the service in the foreground
                        buildMediaNotification(mIsPlaying);
                    }
                });
            }
        });
    }

    private void showTrackLoadError(final String message){
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // This Runnable is a helper to make sure we're accessing the Player from the correct thread
    // by using the Handler as the go-between
    private Runnable mProgressHelperRunnable = new Runnable() {
        @Override
        public void run() {
            mHandler.post(mProgressRunnable);
        }
    };

    // Keeps track of the player's current playback information
    // This Runnable will repeatedly call itself as long as the player isn't idle or ended
    private Runnable mProgressRunnable = new Runnable(){
        @SuppressLint("RestrictedApi")
        @Override
        public void run() {
            // Provide the playback info to the listener callback
            if(mListener != null){
                mListener.onPlayback(mPlayer.getDuration(), mPlayer.getCurrentPosition(),
                        mPlayer.getBufferedPosition());
            }

            int playbackState = mPlayer.getPlaybackState();

            // Cancel playback monitoring if the player is in an idle or ended state
            if(playbackState == Player.STATE_IDLE || playbackState == Player.STATE_ENDED){
                Log.d(LOG_TAG, "Cancelling playback progress future.");
                mFuture.cancel(true);
            }
        }
    };

    @SuppressLint("RestrictedApi")
    public void seekTo(float progress){
        long progressPos = (long)(progress / 1000 * mPlayer.getDuration());

        mPlayer.seekTo(progressPos);
    }

    // Navigates the player to the previous or next track
    public void adjustTrack(AdjustTrack direction){
        if(mTracks == null){
            return;
        }

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
        // Check if we're trying to start or stop playback
        if(play){
            mIsPlaying = requestFocus(); // Check if we have authorization to play
        }else{
            mIsPlaying = play;
        }

        if(mTracks != null) {
            buildMediaNotification(mIsPlaying); // Update the media notification
        }

        // Start or stop the playback and monitoring depending on whether the track is set to play
        if(mIsPlaying){
            mPlayer.play();

            mFuture = mExecutor.scheduleAtFixedRate(mProgressHelperRunnable, 0, 1, TimeUnit.SECONDS);
        }else{
            mPlayer.pause();

            if(mFuture != null){
                Log.d(LOG_TAG, "Cancelling playback progress future.");
                mFuture.cancel(true);
            }

            abandonFocus(); // Remove the audio focus
        }
    }

    @SuppressLint("RestrictedApi")
    public void releasePlayer(){
        if(mPlayer != null) {
            mIsPlaying = false;

            mPlayer.setPlayWhenReady(false);
            mPlayer.release();

            abandonFocus(); // Remove the audio focus
        }
    }

    private void buildMediaNotification(final boolean setForeground){
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                NotificationManager notificationManager = (NotificationManager) mContext
                        .getSystemService(Context.NOTIFICATION_SERVICE);

                // Create Notification Channel on API 26+
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                    // Register the channels with the system.
                    // Importance & Notification behaviors can't be changed after this
                    notificationManager.createNotificationChannel(createChannel());
                }

                int intentflags = PendingIntent.FLAG_UPDATE_CURRENT;

                // Mutability must be set for PendingIntents when the device is Android 12 or higher
                // https://developer.android.com/guide/components/intents-filters#DeclareMutabilityPendingIntent
                if(Build.VERSION.SDK_INT >= 23) {
                    intentflags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
                }

                Intent notificationIntent = new Intent(mContext, NavActivity.class);
                PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 650,
                        notificationIntent, intentflags);

                JSONObject track = mTracks.get(mCurrentTrack);
                String trackTitle;
                String trackArtist;
                String trackImage;

                try{
                    trackTitle = track.getString("title");
                    trackArtist = track.getJSONObject("user").getString("username");
                    trackImage = track.getString("artwork_url");
                }catch(JSONException e){
                    Log.e(LOG_TAG, "Can't build notification.", e);
                    return;
                }

                NotificationCompat.Builder notifyBuilder = new NotificationCompat
                        .Builder(mContext, "CloudPlayer");
                notifyBuilder.setSmallIcon(R.drawable.ic_cloud_player)
                        .setContentTitle(trackTitle)
                        .setContentText(trackArtist)
                        .setShowWhen(false)
                        .setColor(ContextCompat.getColor(mContext, R.color.colorPrimary))
                        .setContentIntent(pendingIntent)
                        .setOngoing(true)
                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT);

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

                int playOrPauseIcon = R.drawable.ic_play_black_24dp;

                if(mIsPlaying){
                    playOrPauseIcon = R.drawable.ic_pause_black_24dp;
                }

                notifyBuilder.addAction(R.drawable.ic_skip_previous_black_24dp, "Previous", previousPendingIntent); // #0
                notifyBuilder.addAction(playOrPauseIcon, "Play", playPendingIntent); // #1
                notifyBuilder.addAction(R.drawable.ic_skip_next_black_24dp, "Next", nextPendingIntent); // # 2

                // Set this as a Media Style notification with 3 actions visible in its compact mode
                notifyBuilder.setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setShowActionsInCompactView(0, 1, 2)); // The indices of the actions. Can use up to 3 actions

                // Delivers the notification with as foreground or regular as appropriate
                if(setForeground) {
                    startForeground(111, notifyBuilder.build());
                }else{
                    stopForeground(false);
                    notificationManager.notify(111, notifyBuilder.build());
                }
            }
        });
    }

    @TargetApi(26)
    private NotificationChannel createChannel(){
        String channelName = "CloudPlayer Media Player";
        String channelDescription = "CloudPlayer basic media player and control";
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

        int intentflags = PendingIntent.FLAG_UPDATE_CURRENT;

        // Mutability must be set for PendingIntents when the device is Android 12 or higher
        // https://developer.android.com/guide/components/intents-filters#DeclareMutabilityPendingIntent
        if(Build.VERSION.SDK_INT >= 23) {
            intentflags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        }

        return PendingIntent.getService(mContext, requestCode, actionIntent, intentflags);
    }
}
