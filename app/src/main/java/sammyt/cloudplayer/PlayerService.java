package sammyt.cloudplayer;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
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
import android.view.Surface;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.media2.exoplayer.external.ExoPlaybackException;
import androidx.media2.exoplayer.external.ExoPlayerFactory;
import androidx.media2.exoplayer.external.Format;
import androidx.media2.exoplayer.external.PlaybackParameters;
import androidx.media2.exoplayer.external.Player;
import androidx.media2.exoplayer.external.SimpleExoPlayer;
import androidx.media2.exoplayer.external.Timeline;
import androidx.media2.exoplayer.external.analytics.AnalyticsListener;
import androidx.media2.exoplayer.external.decoder.DecoderCounters;
import androidx.media2.exoplayer.external.metadata.Metadata;
import androidx.media2.exoplayer.external.source.MediaSource;
import androidx.media2.exoplayer.external.source.MediaSourceEventListener;
import androidx.media2.exoplayer.external.source.ProgressiveMediaSource;
import androidx.media2.exoplayer.external.source.TrackGroupArray;
import androidx.media2.exoplayer.external.trackselection.TrackSelectionArray;
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
    private User mUser;
    private boolean mIsPlaying = false;
    private int mCurrentTrack;
    private ArrayList<Track> mTracks;

    private SimpleExoPlayer mPlayer;
    private DataSource.Factory mDataSourceFactory;
    private Handler mPlaybackHandler = new Handler();
    private int mSessionId;

    private AudioManager mAudioManager;
    private AudioManager.OnAudioFocusChangeListener mFocusListener;
    private AudioFocusRequest mFocusRequest;

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

        super.onDestroy();
    }

    public interface PlayerServiceListener{
        void onTrackLoaded(Track track);
        void onPlayback(float duration, float currentPos, float bufferPos);
        void onSessionId(int sessionId);
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

            mPlayer.addAnalyticsListener(new PlayerAnalyticsListener());

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

    public void setUser(User user){
        mUser = user;
    }

    public User getUser(){
        return mUser;
    }

    public void setTrackList(ArrayList<Track> trackList){
        mTracks = trackList;
    }

    public ArrayList<Track> getTrackList(){
        return mTracks;
    }

    public boolean isPlaying(){
        return mIsPlaying;
    }

    public int getSessionId(){
        return mSessionId;
    }

    public void loadTrack(int trackPos){

        if(!requestFocus()){
            return; // Return early if we don't have authorization to play
        }

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

    // Navigates the player to the previous or next track
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
        // Check if we're trying to start or stop playback
        if(play){
            mIsPlaying = requestFocus(); // Check if we have authorization to play
        }else{
            mIsPlaying = play;
        }
        mPlayer.setPlayWhenReady(mIsPlaying);

        if(mTracks != null) {
            buildMediaNotification(); // Update the media notification
        }

        // Start or stop the playback monitoring depending on whether the track is playing
        if(mIsPlaying){
            mPlaybackHandler.postDelayed(mProgressRunnable, 0);
        }else{
            mPlaybackHandler.removeCallbacks(mProgressRunnable);
            stopForeground(false);
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

    @SuppressLint("RestrictedApi")
    class PlayerAnalyticsListener implements AnalyticsListener{
        
        public void onPlayerStateChanged(EventTime eventTime, boolean playWhenReady, int playbackState) {
            // Loads the next track when current track has ended
            if(playWhenReady && playbackState == Player.STATE_ENDED){
                adjustTrack(AdjustTrack.next);
            }
        }

        public void onTimelineChanged(EventTime eventTime, int reason) { }

        public void onPositionDiscontinuity(EventTime eventTime, int reason) { }

        public void onSeekStarted(EventTime eventTime) { }

        public void onSeekProcessed(EventTime eventTime) { }

        public void onPlaybackParametersChanged(EventTime eventTime, PlaybackParameters playbackParameters) { }

        public void onRepeatModeChanged(EventTime eventTime, int repeatMode) { }

        public void onShuffleModeChanged(EventTime eventTime, boolean shuffleModeEnabled) { }

        public void onLoadingChanged(EventTime eventTime, boolean isLoading) { }

        public void onPlayerError(EventTime eventTime, ExoPlaybackException error) { }

        public void onTracksChanged(EventTime eventTime, TrackGroupArray trackGroups,
                                    TrackSelectionArray trackSelections) { }

        public void onLoadStarted(EventTime eventTime, MediaSourceEventListener.LoadEventInfo loadEventInfo,
                                  MediaSourceEventListener.MediaLoadData mediaLoadData) { }

        public void onLoadCompleted(EventTime eventTime, MediaSourceEventListener.LoadEventInfo loadEventInfo,
                                    MediaSourceEventListener.MediaLoadData mediaLoadData) { }

        public void onLoadCanceled(EventTime eventTime, MediaSourceEventListener.LoadEventInfo loadEventInfo,
                                   MediaSourceEventListener.MediaLoadData mediaLoadData) { }

        public void onLoadError(EventTime eventTime, MediaSourceEventListener.LoadEventInfo loadEventInfo,
                                MediaSourceEventListener.MediaLoadData mediaLoadData, IOException error,
                                boolean wasCanceled) { }

        public void onDownstreamFormatChanged(EventTime eventTime,
                                              MediaSourceEventListener.MediaLoadData mediaLoadData) { }

        public void onUpstreamDiscarded(EventTime eventTime, MediaSourceEventListener.MediaLoadData mediaLoadData) { }

        public void onMediaPeriodCreated(EventTime eventTime) { }

        public void onMediaPeriodReleased(EventTime eventTime) { }

        public void onReadingStarted(EventTime eventTime) { }

        public void onBandwidthEstimate(EventTime eventTime, int totalLoadTimeMs, long totalBytesLoaded,
                                        long bitrateEstimate) { }

        public void onSurfaceSizeChanged(EventTime eventTime, int width, int height) { }

        public void onMetadata(EventTime eventTime, Metadata metadata) { }

        public void onDecoderEnabled(EventTime eventTime, int trackType, DecoderCounters decoderCounters) { }

        public void onDecoderInitialized(EventTime eventTime, int trackType, String decoderName,
                                         long initializationDurationMs) { }

        public void onDecoderInputFormatChanged(EventTime eventTime, int trackType, Format format) { }

        public void onDecoderDisabled(EventTime eventTime, int trackType, DecoderCounters decoderCounters) { }

        public void onAudioSessionId(EventTime eventTime, int audioSessionId) {
            Log.d(LOG_TAG, "Audio session id: " + audioSessionId);
            mSessionId = audioSessionId;

            if(mListener != null){
                mListener.onSessionId(audioSessionId);
            }
        }

        public void onAudioAttributesChanged(EventTime eventTime,
                                             androidx.media2.exoplayer.external.audio.AudioAttributes audioAttributes) { }

        public void onVolumeChanged(EventTime eventTime, float volume) { }

        public void onAudioUnderrun(EventTime eventTime, int bufferSize, long bufferSizeMs,
                                    long elapsedSinceLastFeedMs) { }

        public void onDroppedVideoFrames(EventTime eventTime, int droppedFrames, long elapsedMs) { }

        public void onVideoSizeChanged(EventTime eventTime, int width, int height,
                                       int unappliedRotationDegrees, float pixelWidthHeightRatio) { }

        public void onRenderedFirstFrame(EventTime eventTime, @Nullable Surface surface) { }

        public void onDrmSessionAcquired(EventTime eventTime) { }

        public void onDrmKeysLoaded(EventTime eventTime) { }

        public void onDrmSessionManagerError(EventTime eventTime, Exception error) { }

        public void onDrmKeysRestored(EventTime eventTime) { }

        public void onDrmKeysRemoved(EventTime eventTime) { }

        public void onDrmSessionReleased(EventTime eventTime) { }
    }
}
