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
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import sammyt.cloudplayer.NavActivity;
import sammyt.cloudplayer.R;
import sammyt.cloudplayer.data.CloudQueue;
import sammyt.cloudplayer.nav.SelectedTrackModel;
import sammyt.cloudplayer.nav.TrackAdapter;
import sammyt.cloudplayer.nav.TrackViewModel;

public class HomeFragment extends Fragment {

    private static final String LOG_TAG = HomeFragment.class.getSimpleName();

    private String token;

    private TrackViewModel trackViewModel;
    private SelectedTrackModel selectedTrackModel;

    private ProgressBar mLoadingView;
    private LinearLayout mErrorView;
    private RecyclerView mTrackRecycler;
    private TrackAdapter mAdapter;

    private ArrayList<JSONObject> mTracks = new ArrayList<>();

    private enum VisibleView{
        loading, loaded, error
    }

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
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

        // Retrieve the token from the activity
        token = ((NavActivity) requireActivity()).token;

        // Set up the ViewModels
        ViewModelProvider activityModelProvider = new ViewModelProvider(requireActivity());
        trackViewModel = activityModelProvider.get(TrackViewModel.class);
        selectedTrackModel = activityModelProvider.get(SelectedTrackModel.class);

        // Observe the View Model to update the adapter
        trackViewModel.getTracks().observe(getViewLifecycleOwner(), new Observer<ArrayList<JSONObject>>() {
            @Override
            public void onChanged(ArrayList<JSONObject> tracks) {
                String logMessage = "ViewModel onChanged - ";

                if(tracks == null){
                    logMessage += "New load ";
                    loadTrackDataFromVolley(null);
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
        selectedTrackModel.getSelectedTrack().observe(requireActivity(), new Observer<SelectedTrackModel.SelectedTrack>() {
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
                loadTrackDataFromVolley(null);
            }
        });

        // The button shown if the data fails to load
        // Allows the user to manually retry loading the data
        retryLoading.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadTrackDataFromVolley(null);
            }
        });

        return root;
    }

    private TrackAdapter.onTrackClickListener mTrackClickListener = new TrackAdapter.onTrackClickListener() {
        @Override
        public void onTrackClick(int position, JSONObject track) {
            Log.d(LOG_TAG, "Track Clicked - " + position + " " + track.optString("title") + " " + track);

            selectedTrackModel.setSelectedTrack(position, track, trackViewModel.getTracks().getValue(), LOG_TAG);
        }
    };

    private void loadTrackDataFromVolley(String url){
        RequestQueue queue = CloudQueue.getInstance(getContext()).getRequestQueue();

        final String auth = "&oauth_token=" + token;

        if(url == null) {
            Log.d(LOG_TAG, "Loading track data from volley.");
            setVisibleView(VisibleView.loading);

            String endpoint = "/me/likes/tracks";
            url = getString(R.string.api_root) + endpoint + "?linked_partitioning=true" + auth;

            mTracks.clear(); // Make sure we're not appending to possibly stale data
        }

        Response.Listener<JSONObject> responseListener = new Response.Listener<JSONObject>(){
            @Override
            public void onResponse(JSONObject response) {
                Log.d(LOG_TAG, "Volley response:\n" + response);

                try {
                    JSONArray collection = response.getJSONArray("collection");

                    String nextPage = response.optString("next_href");
                    Log.d(LOG_TAG, "SC next page: " + nextPage);

                    for(int i=0; i < collection.length(); i++){
                        JSONObject jsonObject = collection.getJSONObject(i);
//                        Log.d(LOG_TAG, "Volley item: " + jsonObject);

                        mTracks.add(jsonObject);
                    }

                    // Load the next page if one exists
                    // or update the ViewModel
                    if(!nextPage.equals("") && !nextPage.equals("null")) {
                        loadTrackDataFromVolley(nextPage + auth);
                    } else {
                        trackViewModel.setTracks(mTracks);
                    }

                } catch(JSONException e) {
                    Log.e(LOG_TAG, "Error parsing response json", e);
                    setVisibleView(VisibleView.error);
                }
            }
        };

        Response.ErrorListener errorListener = new Response.ErrorListener(){
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(LOG_TAG, "Volley error loading tracks.", error);
                setVisibleView(VisibleView.error);
            }
        };

        JsonObjectRequest jsonRequest = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                responseListener,
                errorListener);

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