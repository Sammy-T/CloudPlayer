package sammyt.cloudplayer.player;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import org.json.JSONObject;

import de.voidplus.soundcloud.Track;
import sammyt.cloudplayer.PlayerService;
import sammyt.cloudplayer.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class QueueFragment extends Fragment implements PlayerService.PlayerServiceListener {

    private final String LOG_TAG = this.getClass().getSimpleName();

    private PlayerService mService;
    private boolean mBound = false;

    private RecyclerView mQueueRecycler;
    private QueueAdapter mAdapter;

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
    public void onResume(){
        super.onResume();

        if(!mBound){
            Log.d(LOG_TAG, "Bind Service");

            Intent intent = new Intent(getContext(), PlayerService.class);
            getContext().bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        }
    }

    @Override
    public void onPause(){
        if(mBound){
            Log.d(LOG_TAG, "Unbind Service");

            getContext().unbindService(mConnection);
            mBound = false;
        }

        super.onPause();
    }

    private QueueAdapter.onQueueClickListener mQueueClickListener = new QueueAdapter.onQueueClickListener() {
        @Override
        public void onQueueClick(int position, JSONObject track) {
            Log.d(LOG_TAG, "Queue click - " + position + " " + track);

            if(mBound) {
                mService.loadTrack(position);
            }
        }

        @Override
        public void onQueueRemove(int position, JSONObject track) {
            Log.d(LOG_TAG, "Queue Remove click - " + position + " " + track);

            if(mBound){
                mService.removeTrackFromList(position);
                mAdapter.updateTracks(mService.getTrackList());
            }
        }
    };

    // Player Service Interface method
    public void onTrackLoaded(int trackPos, JSONObject track){
        Log.d(LOG_TAG, "Track loaded: " + trackPos + " " + track);

        mAdapter.setSelectedTrack(track);
    }

    // Player Service Interface method
    public void onPlayback(float duration, float currentPos, float bufferPos){}

    // Player Service Interface method
    public void onSessionId(int sessionId){}

    // Player Service Connection
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(LOG_TAG, "Service Connected");

            // We've bound to the service, cast the IBinder to our defined Binder
            // and get our Service instance
            PlayerService.PlayerBinder binder = (PlayerService.PlayerBinder) service;
            mService = binder.getService();
            mBound = true;

            mService.setPlayerServiceListener(QueueFragment.this);

            mAdapter.updateTracks(mService.getTrackList());
            mAdapter.setSelectedTrack(mService.getCurrentTrack());
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(LOG_TAG, "Service Disconnected");

            mBound = false;
        }
    };
}
