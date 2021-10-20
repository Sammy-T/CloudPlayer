package sammyt.cloudplayer.nav.playlists;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ViewFlipper;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import sammyt.cloudplayer.LoginActivity;
import sammyt.cloudplayer.NavActivity;
import sammyt.cloudplayer.R;
import sammyt.cloudplayer.data.CloudQueue;
import sammyt.cloudplayer.nav.SelectedTrackModel;
import sammyt.cloudplayer.nav.TrackAdapter;

public class PlaylistsFragment extends Fragment {

    private static final String LOG_TAG = PlaylistsFragment.class.getSimpleName();

    private String token;

    private ViewFlipper viewFlipper;
    private RecyclerView mPlaylistRecycler;
    private TextView mPlaylistSelectedTitle;
    private TextView mPlaylistSelectedCount;
    private RecyclerView mPlaylistTrackRecycler;

    private PlaylistsViewModel playlistsViewModel;
    private SelectedTrackModel selectedTrackModel;

    private PlaylistAdapter mAdapter;
    private TrackAdapter mTrackAdapter;

    private JSONObject mSelectedPlaylist; 

    private enum VisibleView{
        loading, playlist, selection, error, error_auth
    }

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_playlists, container, false);

        viewFlipper = root.findViewById(R.id.playlist_flipper);
        Button retryLoading = root.findViewById(R.id.retry);
        Button retryLoading2 = root.findViewById(R.id.retry2);
        Button refreshAuth = root.findViewById(R.id.refresh_auth);

        // Switcher's playlist list layout
        TextView titleView = root.findViewById(R.id.title_playlists_text);
        mPlaylistRecycler = root.findViewById(R.id.playlists_recycler);

        // Switcher's selected playlist layout
        mPlaylistSelectedTitle = root.findViewById(R.id.playlist_selected_title);
        mPlaylistSelectedCount = root.findViewById(R.id.playlist_track_count);
        mPlaylistTrackRecycler = root.findViewById(R.id.playlist_track_recycler);

        // Set the View Switchers animations
        Animation inAnim = AnimationUtils.loadAnimation(getContext(), android.R.anim.slide_in_left);
        Animation outAnim = AnimationUtils.loadAnimation(getContext(), android.R.anim.slide_out_right);
        viewFlipper.setInAnimation(inAnim);
        viewFlipper.setOutAnimation(outAnim);

        // Set up the playlist recycler view
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        mPlaylistRecycler.setLayoutManager(layoutManager);

        mAdapter = new PlaylistAdapter();
        mAdapter.setOnPlaylistClickListener(mPlaylistClickListener);
        mPlaylistRecycler.setAdapter(mAdapter);

        // Set up the selected playlist's track recycler view
        LinearLayoutManager layoutManager2 = new LinearLayoutManager(getContext());
        mPlaylistTrackRecycler.setLayoutManager(layoutManager2);

        mTrackAdapter = new TrackAdapter(getContext(), null);
        mTrackAdapter.setOnTrackClickListener(mTrackClickListener);
        mPlaylistTrackRecycler.setAdapter(mTrackAdapter);

        // Set up ViewModels
        ViewModelProvider activityModelProvider = new ViewModelProvider(requireActivity());
        playlistsViewModel = activityModelProvider.get(PlaylistsViewModel.class);
        selectedTrackModel = activityModelProvider.get(SelectedTrackModel.class);

        // Observe the View Model to update the adapter
        playlistsViewModel.getPlaylists().observe(getViewLifecycleOwner(), new Observer<ArrayList<JSONObject>>() {
            @Override
            public void onChanged(ArrayList<JSONObject> playlists) {
                // Since we're initializing the View Model before we're able to retrieve the
                // activity's token, ignore callbacks without it (i.e. the initial callback).
                if(token == null) return;

                String logMessage = "ViewModel onChanged - ";

                if(playlists == null){
                    loadPlaylistDataFromVolley();
                }else{
                    setVisibleView(VisibleView.playlist);
                }

                if(mAdapter != null){
                    logMessage += "Adapter update";
                    mAdapter.updateTracks(playlists);
                }

                Log.d(LOG_TAG, logMessage);
            }
        });

        // Observe the shared View Model to update the adapter's selected item
        selectedTrackModel.getSelectedTrack().observe(requireActivity(), new Observer<SelectedTrackModel.SelectedTrack>() {
            @Override
            public void onChanged(SelectedTrackModel.SelectedTrack selectedTrack) {
                if(selectedTrack != null && mTrackAdapter != null) {
                    mTrackAdapter.setSelectedTrack(selectedTrack.getTrack());
                }
                //// TODO: set selected to null if selection isn't from this fragment or home activity?
            }
        });

        View.OnClickListener reloadListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadPlaylistDataFromVolley();
            }
        };

        // Allow manually refreshing the data by clicking on the title
        titleView.setOnClickListener(reloadListener);

        // The button shown if the data fails to load
        // Allows the user to manually retry loading the data
        retryLoading.setOnClickListener(reloadListener);
        retryLoading2.setOnClickListener(reloadListener);

        refreshAuth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((NavActivity) requireActivity()).redirectToLogin(true);
            }
        });

        // Respond to back presses
        NavActivity.onBackListener onBackListener = new NavActivity.onBackListener() {
            @Override
            public boolean onBack() {
                if(getVisibleView() == VisibleView.selection) {
                    setVisibleView(VisibleView.playlist); // Navigate back to the playlist list
                    return true; // Consume the back press event
                }

                return false; // Allow normal response
            }
        };

        ((NavActivity) requireActivity()).setOnBackListener(onBackListener);

        return root;
    }

    @Override
    public void onStart() {
        super.onStart();

        // Retrieve the token from the activity
        token = ((NavActivity) requireActivity()).token;

        // Perform the initial load if necessary
        if(playlistsViewModel.getPlaylists().getValue() == null) {
            Log.d(LOG_TAG, "New load from onStart");
            loadPlaylistDataFromVolley();
        }
    }

    private PlaylistAdapter.onPlaylistClickListener mPlaylistClickListener = new PlaylistAdapter.onPlaylistClickListener() {
        @Override
        public void onPlaylistClick(int position, JSONObject playlist) {
            selectPlaylist(playlist);
        }
    };

    private TrackAdapter.onTrackClickListener mTrackClickListener = new TrackAdapter.onTrackClickListener() {
        @Override
        public void onTrackClick(int position, JSONObject track) {
            // Build an ArrayList that's compatible with the ViewModel method
            ArrayList<JSONObject> tracks = new ArrayList<>();

            try {
                JSONArray tracksJsonArray = mSelectedPlaylist.getJSONArray("tracks");

                for(int i=0; i < tracksJsonArray.length(); i++) {
                    tracks.add(tracksJsonArray.getJSONObject(i));
                }
            } catch(JSONException e) {
                Log.e(LOG_TAG, "Error parsing json", e);
                return;
            }

            selectedTrackModel.setSelectedTrack(position, track, tracks, LOG_TAG);
        }
    };

    private void loadPlaylistDataFromVolley() {
        RequestQueue queue = CloudQueue.getInstance(getContext()).getRequestQueue();

        Log.d(LOG_TAG, "Loading playlist data from volley.");
        setVisibleView(VisibleView.loading);

        String endpoint = "/me/playlists";
        String url = getString(R.string.api_root) + endpoint;

        Response.Listener<JSONArray> responseListener = new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                Log.d(LOG_TAG, "Volley response:\n" + response);

                try {
                    ArrayList<JSONObject> playlists = new ArrayList<>();

                    for(int i=0; i < response.length(); i++) {
                        JSONObject playlistObject = response.getJSONObject(i);
                        Log.d(LOG_TAG, "playlist object:\n" + playlistObject);

                        playlists.add(playlistObject);
                    }

                    playlistsViewModel.setPlaylists(playlists); // Update the ViewModel

                } catch(JSONException e) {
                    Log.e(LOG_TAG, "Error parsing response json", e);
                    setVisibleView(VisibleView.error);
                }
            }
        };

        Response.ErrorListener errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(LOG_TAG, "Error loading playlists", error);

                if(error.networkResponse.statusCode == 401) {
                    Log.w(LOG_TAG, "Unauthorized access. Token:" + token);
                    setVisibleView(VisibleView.error_auth);
                } else {
                    setVisibleView(VisibleView.error);
                }
            }
        };

        JsonArrayRequest jsonRequest = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                responseListener,
                errorListener) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                // Include auth in the header
                Map<String, String> params = new HashMap<>();
                params.put("Authorization", "OAuth " + token);

                return params;
            }
        };

        queue.add(jsonRequest);
    }

    private void refreshTokenAtLogin() {
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.putExtra(LoginActivity.EXTRA_ACTION, LoginActivity.ACTION_REFRESH);

        startActivity(intent);
        requireActivity().finish();
    }

    private void selectPlaylist(JSONObject playlist){
        mSelectedPlaylist = playlist; 

        try {
            String title = playlist.getString("title");
            String count = playlist.getString("track_count") + " tracks";

            mPlaylistSelectedTitle.setText(title);
            mPlaylistSelectedCount.setText(count);

            JSONArray tracksJsonArray = playlist.getJSONArray("tracks");
            ArrayList<JSONObject> tracks = new ArrayList<>();

            for(int i=0; i < tracksJsonArray.length(); i++) {
                tracks.add(tracksJsonArray.getJSONObject(i));
            }

            mTrackAdapter.updateTracks(tracks); 
        } catch(JSONException e) {
            Log.e(LOG_TAG, "Error parsing json", e);
            return;
        }

        setVisibleView(VisibleView.selection);
    }

    private void setVisibleView(VisibleView visibleView){
        switch(visibleView){
            case loading:
                viewFlipper.setDisplayedChild(0);
                break;

            case playlist:
                viewFlipper.setDisplayedChild(1);
                break;

            case selection:
                viewFlipper.setDisplayedChild(2);
                break;

            case error:
                viewFlipper.setDisplayedChild(3);
                break;

            case error_auth:
                viewFlipper.setDisplayedChild(4);
                break;
        }
    }

    private VisibleView getVisibleView() {
        switch(viewFlipper.getDisplayedChild()) {
            case 0:
                return VisibleView.loading;

            case 1:
                return VisibleView.playlist;

            case 2:
                return VisibleView.selection;

            case 3:
                return VisibleView.error;

            case 4:
                return VisibleView.error_auth;

            default:
                return VisibleView.loading;
        }
    }
}