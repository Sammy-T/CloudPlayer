package sammyt.cloudplayer.nav.playlists;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ViewSwitcher;

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
import com.android.volley.toolbox.JsonArrayRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import de.voidplus.soundcloud.Playlist;
import de.voidplus.soundcloud.User;
import sammyt.cloudplayer.NavActivity;
import sammyt.cloudplayer.R;
import sammyt.cloudplayer.data_sc.CloudQueue;
import sammyt.cloudplayer.data_sc.PlaylistsTask;
import sammyt.cloudplayer.nav.SelectedTrackModel;
import sammyt.cloudplayer.nav.TrackAdapter;

public class PlaylistsFragment extends Fragment {

    private static final String LOG_TAG = PlaylistsFragment.class.getSimpleName();

    private PlaylistsViewModel playlistsViewModel;
    private SelectedTrackModel selectedTrackModel;

    private ProgressBar mLoadingView;
    private LinearLayout mErrorView;
    private ViewSwitcher mSwitcher;
    private RecyclerView mPlaylistRecycler;
    private TextView mPlaylistSelectedTitle;
    private TextView mPlaylistSelectedCount;
    private RecyclerView mPlaylistTrackRecycler;

    private PlaylistAdapter mAdapter;
    private TrackAdapter mTrackAdapter;

    private JSONObject mSelectedPlaylist; 

    private enum VisibleView{
        loading, loaded, error
    }

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        ViewModelProvider activityModelProvider = new ViewModelProvider(getActivity());

        playlistsViewModel = activityModelProvider.get(PlaylistsViewModel.class);
        selectedTrackModel = activityModelProvider.get(SelectedTrackModel.class);

        View root = inflater.inflate(R.layout.fragment_playlists, container, false);
        mLoadingView = root.findViewById(R.id.loading);
        mErrorView = root.findViewById(R.id.error);
        Button retryLoading = root.findViewById(R.id.retry_playlists);
        mSwitcher = root.findViewById(R.id.playlists_switcher);

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
        mSwitcher.setInAnimation(inAnim);
        mSwitcher.setOutAnimation(outAnim);

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

        // Observe the View Model to update the adapter
        playlistsViewModel.getPlaylists().observe(getViewLifecycleOwner(), new Observer<ArrayList<JSONObject>>() {
            @Override
            public void onChanged(ArrayList<JSONObject> playlists) {
                String logMessage = "ViewModel onChanged - ";

                if(playlists == null){
                    loadPlaylistDataFromVolley();
                }else{
                    setVisibleView(VisibleView.loaded);
                }

                if(mAdapter != null){
                    logMessage += "Adapter update";
                    mAdapter.updateTracks(playlists);
                }

                Log.d(LOG_TAG, logMessage);
            }
        });

        // Observe the shared View Model to update the adapter's selected item
        selectedTrackModel.getSelectedTrack().observe(getActivity(), new Observer<SelectedTrackModel.SelectedTrack>() {
            @Override
            public void onChanged(SelectedTrackModel.SelectedTrack selectedTrack) {
                if(selectedTrack != null && mTrackAdapter != null) {
                    mTrackAdapter.setSelectedTrack(selectedTrack.getTrack());
                }
                //// TODO: set selected to null if selection isn't from this fragment or home activity?
            }
        });

        // Allow manually refreshing the data by clicking on the title
        titleView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadPlaylistDataFromVolley();
            }
        });

        // The button shown if the data fails to load
        // Allows the user to manually retry loading the data
        retryLoading.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadPlaylistDataFromVolley();
            }
        });

        // Respond to back presses
        NavActivity.onBackListener onBackListener = new NavActivity.onBackListener() {
            @Override
            public boolean onBack() {
                if (mSwitcher.getDisplayedChild() == 1) {
                    mSwitcher.setDisplayedChild(0); // Navigate back to the playlist list
                    return true; // Consume the back press event
                }

                return false; // Allow normal response
            }
        };

        try{
            ((NavActivity) getActivity()).setOnBackListener(onBackListener);
        }catch(ClassCastException e){
            Log.e(LOG_TAG, "Cannot be cast to NavActivity", e);
        }

        return root;
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

    private void loadPlaylistData(){
        setVisibleView(VisibleView.loading);

        PlaylistsTask playlistsTask = new PlaylistsTask(
                getString(R.string.client_id),
                getString(R.string.client_secret),
                getString(R.string.login_name),
                getString(R.string.login_password));
        playlistsTask.execute();
        playlistsTask.setOnFinishListener(new PlaylistsTask.onFinishListener() {
            @Override
            public void onFinish(User user, ArrayList<Playlist> playlists) {
//                playlistsViewModel.setPlaylists(playlists);
            }

            @Override
            public void onFailure() {
                setVisibleView(VisibleView.error);
            }
        });
    }

    private void loadPlaylistDataFromVolley() {
        RequestQueue queue = CloudQueue.getInstance(getContext()).getRequestQueue();

        final String auth = "oauth_token=" + getString(R.string.temp_access_token);

        Log.d(LOG_TAG, "Loading playlist data from volley.");

        setVisibleView(VisibleView.loading);

        String endpoint = "/me/playlists";
        String url = getString(R.string.api_root) + endpoint + "?" + auth;

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

                    playlistsViewModel.setPlaylists(playlists);

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
                setVisibleView(VisibleView.error);
            }
        };

        JsonArrayRequest jsonRequest = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                responseListener,
                errorListener);

        queue.add(jsonRequest);
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

        mSwitcher.setDisplayedChild(1);
    }

    private void setVisibleView(VisibleView visibleView){
        switch(visibleView){
            case loading:
                mLoadingView.setVisibility(View.VISIBLE);
                mErrorView.setVisibility(View.GONE);
                mSwitcher.setVisibility(View.GONE);
                break;

            case loaded:
                mLoadingView.setVisibility(View.GONE);
                mErrorView.setVisibility(View.GONE);
                mSwitcher.setVisibility(View.VISIBLE);
                break;

            case error:
                mLoadingView.setVisibility(View.GONE);
                mErrorView.setVisibility(View.VISIBLE);
                mSwitcher.setVisibility(View.GONE);
                break;
        }
    }
}