package sammyt.cloudplayer.ui.home;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import de.voidplus.soundcloud.Track;
import de.voidplus.soundcloud.User;
import sammyt.cloudplayer.R;
import sammyt.cloudplayer.TrackAdapter;
import sammyt.cloudplayer.UserAndTracksTask;

public class HomeFragment extends Fragment {

    private final String LOG_TAG = this.getClass().getSimpleName();

    private HomeViewModel homeViewModel;

    private RecyclerView mTrackRecycler;
    private TrackAdapter mAdapter;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        homeViewModel = ViewModelProviders.of(this).get(HomeViewModel.class);

        View root = inflater.inflate(R.layout.fragment_home, container, false);
        mTrackRecycler = root.findViewById(R.id.liked_tracks_recycler);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        mTrackRecycler.setLayoutManager(layoutManager);

        homeViewModel.getTracks().observe(this, new Observer<ArrayList<Track>>() {
            @Override
            public void onChanged(ArrayList<Track> tracks) {
                String logMessage = "ViewModel onChanged - ";

                if(tracks == null){
                    loadTrackData();
                }

                if(mAdapter == null){
                    logMessage += "Adapter initialization";

                    mAdapter = new TrackAdapter(tracks);
                    mAdapter.setOnTrackClickListener(mTrackClickListener);
                    mTrackRecycler.setAdapter(mAdapter);
                }else{
                    logMessage = "Adapter update";
                    mAdapter.updateTracks(tracks);
                }

                Log.d(LOG_TAG, logMessage);
            }
        });

        return root;
    }

    private TrackAdapter.onTrackClickListener mTrackClickListener = new TrackAdapter.onTrackClickListener() {
        @Override
        public void onTrackClick(int position, Track track) {
            Log.d(LOG_TAG, "Track Clicked - " + position + " " + track.getTitle() + " " + track);
            //// TODO: Load the track in the service
        }
    };

    private void loadTrackData(){
        Log.d(LOG_TAG, "Load track data");

        UserAndTracksTask trackDataTask = new UserAndTracksTask(
                getString(R.string.client_id),
                getString(R.string.client_secret),
                getString(R.string.login_name),
                getString(R.string.login_password)
        );
        trackDataTask.execute();
        trackDataTask.setOnFinishListener(new UserAndTracksTask.onFinishListener() {
            @Override
            public void onFinish(User user, ArrayList<Track> faveTracks) {
                homeViewModel.setTracks(faveTracks);
            }

            @Override
            public void onFailure() {
                //// TODO: Try loading from the service
            }
        });
    }
}