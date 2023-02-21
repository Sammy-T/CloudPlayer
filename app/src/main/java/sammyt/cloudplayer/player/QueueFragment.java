package sammyt.cloudplayer.player;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONObject;

import sammyt.cloudplayer.PlayerService;
import sammyt.cloudplayer.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class QueueFragment extends Fragment {

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
    }

    @Override
    public void onPause(){
        super.onPause();
    }

    private QueueAdapter.onQueueClickListener mQueueClickListener = new QueueAdapter.onQueueClickListener() {
        @Override
        public void onQueueClick(int position, JSONObject track) {
            Log.d(LOG_TAG, "Queue click - " + position + " " + track);

//            if(mBound) {
//                mService.loadTrack(position);
//            }
        }

        @Override
        public void onQueueRemove(int position, JSONObject track) {
            Log.d(LOG_TAG, "Queue Remove click - " + position + " " + track);

//            if(mBound){
//                mService.removeTrackFromList(position);
//                mAdapter.updateTracks(mService.getTrackList());
//            }
        }
    };
}
