package sammyt.cloudplayer.ui.home;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

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
import sammyt.cloudplayer.ui.SelectedTrackModel;

public class HomeFragment extends Fragment {

    private final String LOG_TAG = this.getClass().getSimpleName();

    private HomeViewModel homeViewModel;
    private SelectedTrackModel selectedTrackModel;

    private ProgressBar mLoadingView;
    private LinearLayout mErrorView;
    private RecyclerView mTrackRecycler;
    private TrackAdapter mAdapter;

    private enum VisibleView{
        loading, loaded, error
    }

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        homeViewModel = ViewModelProviders.of(this).get(HomeViewModel.class);
        selectedTrackModel = ViewModelProviders.of(getActivity()).get(SelectedTrackModel.class);

        View root = inflater.inflate(R.layout.fragment_home, container, false);
        mTrackRecycler = root.findViewById(R.id.liked_tracks_recycler);
        mLoadingView = root.findViewById(R.id.loading);
        mErrorView = root.findViewById(R.id.error);
        Button retryLoading = root.findViewById(R.id.retry_liked);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        mTrackRecycler.setLayoutManager(layoutManager);

        // Observe the View Model to initialize or update the adapter
        homeViewModel.getTracks().observe(this, new Observer<ArrayList<Track>>() {
            @Override
            public void onChanged(ArrayList<Track> tracks) {
                String logMessage = "ViewModel onChanged - ";

                if(tracks == null){
                    loadTrackData();
                }else{
                    setVisibleView(VisibleView.loaded);
                }

                if(mAdapter == null){
                    logMessage += "Adapter initialization";

                    mAdapter = new TrackAdapter(getContext(), tracks);
                    mAdapter.setOnTrackClickListener(mTrackClickListener);
                    mTrackRecycler.setAdapter(mAdapter);
                }else{
                    logMessage = "Adapter update";
                    mAdapter.updateTracks(tracks);
                }

                Log.d(LOG_TAG, logMessage);
            }
        });

        // Observe the shared View Model to update the adapter's selected item
        selectedTrackModel.getSelectedTrack().observe(getActivity(), new Observer<SelectedTrackModel.SelectedTrack>() {
            @Override
            public void onChanged(SelectedTrackModel.SelectedTrack selectedTrack) {
                if(selectedTrack != null) {
                    mAdapter.setSelectedTrack(selectedTrack.getTrack());
                }
            }
        });

        // The button shown if the data fails to load
        // Allows the user to manually retry loading the data
        retryLoading.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadTrackData();
            }
        });

        return root;
    }

    private TrackAdapter.onTrackClickListener mTrackClickListener = new TrackAdapter.onTrackClickListener() {
        @Override
        public void onTrackClick(int position, Track track) {
            Log.d(LOG_TAG, "Track Clicked - " + position + " " + track.getTitle() + " " + track);

            selectedTrackModel.setSelectedTrack(position, track, homeViewModel.getTracks().getValue(), LOG_TAG);
        }
    };

    private void loadTrackData(){
        Log.d(LOG_TAG, "Load track data");

        setVisibleView(VisibleView.loading);

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
                setVisibleView(VisibleView.error);
            }
        });
    }

    private void setVisibleView(VisibleView visibleView){
        switch(visibleView){
            case loading:
                mLoadingView.setVisibility(View.VISIBLE);
                mTrackRecycler.setVisibility(View.GONE);
                mErrorView.setVisibility(View.GONE);
                break;

            case loaded:
                mLoadingView.setVisibility(View.GONE);
                mTrackRecycler.setVisibility(View.VISIBLE);
                mErrorView.setVisibility(View.GONE);
                break;

            case error:
                mLoadingView.setVisibility(View.GONE);
                mTrackRecycler.setVisibility(View.GONE);
                mErrorView.setVisibility(View.VISIBLE);
                break;
        }
    }
}