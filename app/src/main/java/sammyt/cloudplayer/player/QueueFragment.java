package sammyt.cloudplayer.player;

import android.content.ComponentName;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import sammyt.cloudplayer.PlayerService;
import sammyt.cloudplayer.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class QueueFragment extends Fragment {

    private final String LOG_TAG = this.getClass().getSimpleName();

    private RecyclerView mQueueRecycler;
    private QueueAdapter mAdapter;

    private SessionToken sessionToken;
    private ListenableFuture<MediaController> controllerFuture;
    private MediaController mediaController;

    public QueueFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_queue, container, false);

        ImageButton back = root.findViewById(R.id.queue_back);
        mQueueRecycler = root.findViewById(R.id.queue_recycler);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        mQueueRecycler.setLayoutManager(layoutManager);

        mAdapter = new QueueAdapter(null);
        mAdapter.setOnQueueClickListener(mQueueClickListener);
        mQueueRecycler.setAdapter(mAdapter);

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().onBackPressed();
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
    public void onStop() {
        MediaController.releaseFuture(controllerFuture);
        super.onStop();
    }

    private void initController() {
        sessionToken = new SessionToken(requireContext(),
                new ComponentName(requireContext(), PlayerService.class));

        controllerFuture = new MediaController.Builder(requireContext(), sessionToken).buildAsync();
        controllerFuture.addListener(() -> {
            try {
                setController(controllerFuture.get());
            } catch(ExecutionException | InterruptedException e) {
                Log.e(LOG_TAG, "Unable to get mediaController", e);
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void setController(MediaController controller) {
        mediaController = controller;

        ArrayList<MediaItem> mediaItems = new ArrayList<>();

        for(int i=0; i < mediaController.getMediaItemCount(); i++) {
            mediaItems.add(mediaController.getMediaItemAt(i));
        }

        mAdapter.updateTracks(mediaItems);
        mAdapter.setSelectedTrack(mediaController.getCurrentMediaItem());

        mediaController.addListener(new Player.Listener() {
            @Override
            public void onMediaItemTransition(MediaItem mediaItem, int reason) {
                Player.Listener.super.onMediaItemTransition(mediaItem, reason);
                mAdapter.setSelectedTrack(mediaController.getCurrentMediaItem());
            }
        });
    }

    private QueueAdapter.onQueueClickListener mQueueClickListener = new QueueAdapter.onQueueClickListener() {
        @Override
        public void onQueueClick(int position, MediaItem track) {
            Log.d(LOG_TAG, "Queue click - " + position + " " + track);

            if(mediaController == null) {
                return;
            }

            mediaController.seekTo(position, 0);
        }

        @Override
        public void onQueueRemove(int position, MediaItem track) {
            Log.d(LOG_TAG, "Queue Remove click - " + position + " " + track);

            if(mediaController == null) {
                return;
            }

            mediaController.removeMediaItem(position);
            mAdapter.removeTrack(position);
        }
    };
}
