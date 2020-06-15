package sammyt.cloudplayer.nav.home;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import de.voidplus.soundcloud.Track;
import de.voidplus.soundcloud.User;

import sammyt.cloudplayer.R;
import sammyt.cloudplayer.nav.TrackAdapter;
import sammyt.cloudplayer.data_sc.TracksTask;
import sammyt.cloudplayer.nav.TrackViewModel;
import sammyt.cloudplayer.nav.SelectedTrackModel;

public class HomeFragment extends Fragment {

    private final String LOG_TAG = this.getClass().getSimpleName();

    private TrackViewModel trackViewModel;
    private SelectedTrackModel selectedTrackModel;

    private ProgressBar mLoadingView;
    private LinearLayout mErrorView;
    private RecyclerView mTrackRecycler;
    private TrackAdapter mAdapter;

    private enum VisibleView{
        loading, loaded, error
    }

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        trackViewModel = ViewModelProviders.of(getActivity()).get(TrackViewModel.class);
        selectedTrackModel = ViewModelProviders.of(getActivity()).get(SelectedTrackModel.class);

        View root = inflater.inflate(R.layout.fragment_home, container, false);
        TextView titleView = root.findViewById(R.id.title_liked_text);
        mTrackRecycler = root.findViewById(R.id.liked_tracks_recycler);
        mLoadingView = root.findViewById(R.id.loading);
        mErrorView = root.findViewById(R.id.error);
        Button retryLoading = root.findViewById(R.id.retry_liked);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        mTrackRecycler.setLayoutManager(layoutManager);

        mAdapter = new TrackAdapter(getContext(), null);
        mAdapter.setOnTrackClickListener(mTrackClickListener);
        mTrackRecycler.setAdapter(mAdapter);

        // Observe the View Model to update the adapter
        trackViewModel.getTracks().observe(getViewLifecycleOwner(), new Observer<ArrayList<JSONObject>>() {
            @Override
            public void onChanged(ArrayList<JSONObject> tracks) {
                String logMessage = "ViewModel onChanged - ";

                if(tracks == null){
//                    loadTrackData();
                    loadTrackDataFromVolley();
                }else{
                    setVisibleView(VisibleView.loaded);
                }

                if(mAdapter != null){
                    logMessage += "Adapter update";
                    mAdapter.updateTracks(tracks);
                }

                // If a track was previously selected, set the selected track in the adapter
                // (This covers re-creations of this fragment while navigating w/ bottom nav)
                SelectedTrackModel.SelectedTrack selectedTrack = selectedTrackModel.getSelectedTrack().getValue();
                if(selectedTrack != null){
                    mAdapter.setSelectedTrack(selectedTrack.getTrack());
                }

                Log.d(LOG_TAG, logMessage);
            }
        });

        // Observe the shared View Model to update the adapter's selected item
        selectedTrackModel.getSelectedTrack().observe(getActivity(), new Observer<SelectedTrackModel.SelectedTrack>() {
            @Override
            public void onChanged(SelectedTrackModel.SelectedTrack selectedTrack) {
                if(selectedTrack != null && mAdapter != null) {
                    mAdapter.setSelectedTrack(selectedTrack.getTrack());
                }
            }
        });

        // Allow manually refreshing the data by clicking on the title
        titleView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                loadTrackData();
                loadTrackDataFromVolley();
            }
        });

        // The button shown if the data fails to load
        // Allows the user to manually retry loading the data
        retryLoading.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                loadTrackData();
                loadTrackDataFromVolley();
            }
        });

        return root;
    }

    private TrackAdapter.onTrackClickListener mTrackClickListener = new TrackAdapter.onTrackClickListener() {
        @Override
        public void onTrackClick(int position, JSONObject track) {
            //Log.d(LOG_TAG, "Track Clicked - " + position + " " + track.getTitle() + " " + track);

            selectedTrackModel.setSelectedTrack(position, track, trackViewModel.getTracks().getValue(), LOG_TAG);
        }
    };

    private void loadTrackData(){
        Log.d(LOG_TAG, "Load track data");

        setVisibleView(VisibleView.loading);

        TracksTask trackDataTask = new TracksTask(
                getString(R.string.client_id),
                getString(R.string.client_secret),
                getString(R.string.login_name),
                getString(R.string.login_password)
        );
        trackDataTask.execute();
        trackDataTask.setOnFinishListener(new TracksTask.onFinishListener() {
            @Override
            public void onFinish(User user, ArrayList<Track> faveTracks) {
//                trackViewModel.setTracks(faveTracks);
            }

            @Override
            public void onFailure() {
                setVisibleView(VisibleView.error);
            }
        });
    }

    private void loadTrackDataFromVolley(){
        RequestQueue queue = Volley.newRequestQueue(getContext());

        String url = "https://api.soundcloud.com/users/42908683/favorites.json?client_id=" + getString(R.string.client_id);

        JsonArrayRequest jsonRequest = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                new Response.Listener<JSONArray>(){
                    @Override
                    public void onResponse(JSONArray response) {
                        Log.d(LOG_TAG, "Volley response:\n" + response);

                        try{
                            ArrayList<JSONObject> tracks = new ArrayList<>();

                            for(int i = 0; i < response.length(); i++) {
                                JSONObject jsonObject = response.getJSONObject(i);
                                Log.d(LOG_TAG, "Volley item: " + jsonObject);

                                tracks.add(jsonObject);
                            }

                            trackViewModel.setTracks(tracks);

                        }catch(JSONException e){
                            Log.e(LOG_TAG, "JSON EXC: \n", e);
                            setVisibleView(VisibleView.error);
                        }
                    }
                },
                new Response.ErrorListener(){
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(LOG_TAG, "Volley error loading tracks.", error);
                        setVisibleView(VisibleView.error);
                    }
                });

        queue.add(jsonRequest);
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