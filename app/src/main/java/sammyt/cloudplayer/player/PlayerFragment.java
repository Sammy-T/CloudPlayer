package sammyt.cloudplayer.player;


import android.Manifest;
import android.animation.ObjectAnimator;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.fragment.app.Fragment;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;

import com.google.android.material.snackbar.Snackbar;
import com.google.common.util.concurrent.ListenableFuture;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import me.bogerchan.niervisualizer.NierVisualizerManager;
import me.bogerchan.niervisualizer.renderer.IRenderer;
import me.bogerchan.niervisualizer.renderer.columnar.ColumnarType1Renderer;
import sammyt.cloudplayer.PlayerService;
import sammyt.cloudplayer.R;
import sammyt.cloudplayer.data.PlayerSessionId;

/**
 * A simple {@link Fragment} subclass.
 */
public class PlayerFragment extends Fragment {

    private static final String LOG_TAG = PlayerFragment.class.getSimpleName();

    private static final int PERMISSION_REQ_REC_AUDIO = 819;
    
    private boolean mIsDragging = false;
    
    private SurfaceView mSurface;
    private ImageView mImageView;
    private TextView mTitleView;
    private TextView mArtistView;
    private SeekBar mSeekBar;
    private TextView mTimeView;
    private ImageButton mShuffle;
    private ImageButton mPrevious;
    private ImageButton mPlay;
    private ImageButton mNext;
    private ImageButton mRepeat;
    private ImageButton mQueue;
    private ImageButton mBack;

    private SimpleDateFormat mDateFormat = new SimpleDateFormat("mm:ss", Locale.getDefault());

    private ObjectAnimator mProgressAnim;
    private ObjectAnimator mSecProgressAnim;

    private NierVisualizerManager mVisualizerManager;

    private SessionToken sessionToken;
    private ListenableFuture<MediaController> controllerFuture;
    private MediaController mediaController;

    private static final String QUEUE_FRAGMENT = "QUEUE_FRAGMENT";

    public PlayerFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_player, container, false);

        mSurface = root.findViewById(R.id.surface);
        mImageView = root.findViewById(R.id.track_image_2);
        mTitleView = root.findViewById(R.id.track_title);
        mArtistView = root.findViewById(R.id.track_artist);
        mSeekBar = root.findViewById(R.id.track_seekbar);
        mTimeView = root.findViewById(R.id.track_time);
        mShuffle = root.findViewById(R.id.shuffle);
        mPrevious = root.findViewById(R.id.previous);
        mPlay = root.findViewById(R.id.play);
        mNext = root.findViewById(R.id.next);
        mRepeat = root.findViewById(R.id.repeat);
        mQueue = root.findViewById(R.id.queue_button);
        mBack = root.findViewById(R.id.player_back);

        // Layer the surface view on top and set it to translucent
        mSurface.setZOrderOnTop(true);
        mSurface.getHolder().setFormat(PixelFormat.TRANSLUCENT);

        // Set the Title text view to marquee if it's too long
        mTitleView.setSingleLine(true);
        mTitleView.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        mTitleView.setSelected(true);

        // Set up the Seekbar animations
        mProgressAnim = new ObjectAnimator();
        mProgressAnim.setTarget(mSeekBar);
        mProgressAnim.setPropertyName("progress");
        mProgressAnim.setDuration(1000);
        mProgressAnim.setInterpolator(new LinearInterpolator());

        mSecProgressAnim = new ObjectAnimator();
        mSecProgressAnim.setTarget(mSeekBar);
        mSecProgressAnim.setPropertyName("secondaryProgress");
        mSecProgressAnim.setDuration(1000);
        mSecProgressAnim.setInterpolator(new LinearInterpolator());

        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
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

        mPrevious.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mediaController == null) {
                    return;
                }

                mediaController.seekToPreviousMediaItem();
            }
        });

        mNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mediaController == null) {
                    return;
                }

                mediaController.seekToNextMediaItem();
            }
        });

        mShuffle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mediaController == null) {
                    return;
                }

                mediaController.setShuffleModeEnabled(!mediaController.getShuffleModeEnabled());
            }
        });

        mRepeat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mediaController == null) {
                    return;
                }

                int repeatMode = Player.REPEAT_MODE_OFF;

                if(mediaController.getRepeatMode() == Player.REPEAT_MODE_OFF) {
                    repeatMode = Player.REPEAT_MODE_ONE;
                }

                mediaController.setRepeatMode(repeatMode);
            }
        });

        mQueue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.content_container, new QueueFragment(), QUEUE_FRAGMENT)
                        .addToBackStack(QUEUE_FRAGMENT)
                        .commit();
            }
        });

        mBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requireActivity().onBackPressed();
            }
        });

        return root;
    }

    @Override
    public void onStart() {
        super.onStart();
        initController();
    }

    @Override
    public void onResume(){
        super.onResume();

        if(!checkPermission(Manifest.permission.RECORD_AUDIO)){
            String message = "Record Permission required for audio visualization";
            String action = "Allow";

            Snackbar snackbar = Snackbar.make(mSurface, message, Snackbar.LENGTH_INDEFINITE);
            snackbar.setAction(action, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
                }
            });
            snackbar.show();
        }
    }

    @Override
    public void onPause(){
        if(mVisualizerManager != null) {
            mVisualizerManager.stop();
            mVisualizerManager.release();
        }

        super.onPause();
    }

    @Override
    public void onStop() {
        MediaController.releaseFuture(controllerFuture);
        super.onStop();
    }

    private void initVisualizer(int sessionId){
        if(sessionId <= 0){
            Log.w(LOG_TAG, "Invalid Session ID: " + sessionId);
            return;
        }

        if(mVisualizerManager != null){
            mVisualizerManager.stop();
            mVisualizerManager.release();
        }

        mVisualizerManager = new NierVisualizerManager();

        int state = mVisualizerManager.init(sessionId);
        if (NierVisualizerManager.SUCCESS != state){
            Log.e(LOG_TAG, "Error initializing visualizer manager");
            return;
        }
        Log.d(LOG_TAG, "state: " + state);

        Paint visPaint = new Paint();
        visPaint.setColor(ContextCompat.getColor(requireContext(), R.color.colorAccent));
        visPaint.setAlpha(150);

        mVisualizerManager.start(mSurface, new IRenderer[]{new ColumnarType1Renderer(visPaint)});
    }

    private void updateUI() {
        // Update the play button
        int playOrPause = R.drawable.ic_play_black_36dp;

        if(mediaController.isPlaying()){
            playOrPause = R.drawable.ic_pause_black_36dp;
        }

        mPlay.setImageResource(playOrPause);

        // Update the shuffle & repeat state
        int shuffleColor = ContextCompat.getColor(requireContext(), R.color.colorPrimaryTrans50);
        int repeatColor = ContextCompat.getColor(requireContext(), R.color.colorPrimaryTrans50);

        if(mediaController.getShuffleModeEnabled()) {
            shuffleColor = ContextCompat.getColor(requireContext(), R.color.colorPrimary);
        }

        if(mediaController.getRepeatMode() > Player.REPEAT_MODE_OFF) {
            repeatColor = ContextCompat.getColor(requireContext(), R.color.colorPrimary);
        }

        ImageViewCompat.setImageTintList(mShuffle, ColorStateList.valueOf(shuffleColor));
        ImageViewCompat.setImageTintList(mRepeat, ColorStateList.valueOf(repeatColor));

        MediaItem mediaItem = mediaController.getCurrentMediaItem();
        if(mediaItem == null) {
            return;
        }

        mTitleView.setText(mediaItem.mediaMetadata.title);
        mArtistView.setText(mediaItem.mediaMetadata.artist);

        // Update the track image
        int width = mImageView.getWidth();
        int height = mImageView.getHeight();

        String rawUrl = mediaItem.mediaMetadata.extras.getString("artwork_url");

        if(rawUrl != null && !rawUrl.equals("")){
            final String trackArtUrl = rawUrl.replace("large", "t500x500");

            // Load the track image
            Picasso.get()
                    .load(trackArtUrl)
                    .resize(width, height)
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
                    .load(R.drawable.ic_play_grey600_48dp)
                    .resize(width, height)
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
    }

    private void initController() {
        sessionToken = new SessionToken(requireContext(),
                new ComponentName(requireContext(), PlayerService.class));

        controllerFuture = new MediaController.Builder(requireContext(), sessionToken).buildAsync();
        controllerFuture.addListener(() -> {
            try {
                setController(controllerFuture.get());
                initVisualizer(PlayerSessionId.getInstance().getSessionId());
                updateUI();
            } catch(ExecutionException | InterruptedException e) {
                Log.e(LOG_TAG, "Unable to get mediaController", e);
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void setController(MediaController controller) {
        mediaController = controller;
        mediaController.addListener(new Player.Listener() {
            @Override
            public void onEvents(@NonNull Player player, @NonNull Player.Events events) {
                Player.Listener.super.onEvents(player, events);
                updateUI();
            }

            @OptIn(markerClass = UnstableApi.class)
            @Override
            public void onAudioSessionIdChanged(int audioSessionId) {
                Player.Listener.super.onAudioSessionIdChanged(audioSessionId);
                initVisualizer(audioSessionId);
            }
        });
    }

    // Player Service Interface method
    public void onPlayback(float duration, float currentPos, float bufferPos){
        int progress = (int) ((currentPos / duration) * 1000);
        int bufferProgress = (int) ((bufferPos / duration) * 1000);
        int limit = (int) ((5f / 100f) * 1000);

        if(!mIsDragging){
            if(Math.abs(progress - mSeekBar.getProgress()) >= limit){
                // Set the progress without animating if there's a large change
                mSeekBar.setProgress(progress);
                mSeekBar.setSecondaryProgress(bufferProgress);
            }else{
                // Animate the change in progress and buffer values
                mProgressAnim.setIntValues(progress);
                mProgressAnim.start();

                mSecProgressAnim.setIntValues(bufferProgress);
                mSecProgressAnim.start();
            }
        }

        // Build the time display of the track
        String currentText = mDateFormat.format(new Date((long)currentPos));
        String durationText = mDateFormat.format(new Date((long)duration));

        String timeText = currentText + " / " + durationText;
        mTimeView.setText(timeText);
    }

    // Helper function to check permissions
    private boolean checkPermission(String permission){
        if(ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED){
            return true; // Permission granted
        }
        Log.w(LOG_TAG, "Permission not granted: " + permission);
        return false;
    }

    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), isGranted -> {
                if(isGranted) {
                    Log.d(LOG_TAG, "Record Permission Granted");

                    initVisualizer(PlayerSessionId.getInstance().getSessionId());
                }
            });
}