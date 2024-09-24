package sammyt.cloudplayer.nav.playlists;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ViewFlipper;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import sammyt.cloudplayer.R;
import sammyt.cloudplayer.data.CloudClient;
import sammyt.cloudplayer.nav.SelectedTrackModel;
import sammyt.cloudplayer.nav.TrackAdapter;

public class PlaylistsFragment extends Fragment {

    private static final String LOG_TAG = PlaylistsFragment.class.getSimpleName();

    private ViewFlipper viewFlipper;
    private RecyclerView mPlaylistRecycler;
    private TextView mPlaylistSelectedTitle;
    private TextView mPlaylistSelectedCount;
    private RecyclerView mPlaylistTrackRecycler;

    private final Handler fgHandler = new Handler(Looper.getMainLooper());

    private PlaylistsViewModel playlistsViewModel;
    private SelectedTrackModel selectedTrackModel;

    private PlaylistAdapter mAdapter;
    private TrackAdapter mTrackAdapter;

    private JSONObject mSelectedPlaylist;

    private final ArrayList<JSONObject> mPlaylists = new ArrayList<>();

    private enum VisibleView {
        loading, playlist, selection, error
    }

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_playlists, container, false);

        viewFlipper = root.findViewById(R.id.playlist_flipper);
        Button retryLoading = root.findViewById(R.id.retry);

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
                String logMessage = "ViewModel onChanged - ";

                if(playlists == null){
                    loadPlaylistCollectionData(null);
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
                loadPlaylistCollectionData(null);
            }
        };

        // Allow manually refreshing the data by clicking on the title
        titleView.setOnClickListener(reloadListener);

        // The button shown if the data fails to load
        // Allows the user to manually retry loading the data
        retryLoading.setOnClickListener(reloadListener);

        OnBackPressedCallback onBack = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if(getVisibleView() == VisibleView.selection) {
                    setVisibleView(VisibleView.playlist); // Navigate back to the artist list
                    return; // Consume the back press event
                }

                remove(); // Remove the callback
                requireActivity().getOnBackPressedDispatcher().onBackPressed(); // Allow normal response
            }
        };

        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), onBack);

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

    private void loadPlaylistCollectionData(String url) {
        if(url == null) {
            Log.d(LOG_TAG, "Loading playlist collection data...");

            setVisibleView(VisibleView.loading);

            String limit = "12";
            String offset = "2015-04-05T08:33:55.000Z,playlists,00000000000095707607";

            String endpoint = "/me/library/all";

            String params = "?offset=" + offset
                    + "&limit=" + limit
                    + "&client_id=" + getString(R.string.client_id)
                    + "&app_version=" + getString(R.string.app_version)
                    + "&app_locale=" + getString(R.string.app_locale);

            // Set url to load initial page
            url = getString(R.string.api_root) + endpoint + params;

            mPlaylists.clear(); // Make sure we're not appending to possibly stale data
        }

        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "OAuth " + getString(R.string.token))
                .build();

        OkHttpClient client = CloudClient.getInstance().getClient();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(LOG_TAG, "Error loading playlists.", e);
                fgUpdateView(VisibleView.error);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try {
                    if(!response.isSuccessful()) throw new IOException("Unexpected code" + response);

                    ResponseBody responseBody = response.body();
                    String rawResponse = responseBody.string();

                    JSONObject parsed = new JSONObject(rawResponse);

                    String nextPage = parsed.optString("next_href");
                    Log.d(LOG_TAG, "SC next page: " + nextPage);

                    JSONArray collection = parsed.getJSONArray("collection");

                    for(int i=0; i < collection.length(); i++){
                        JSONObject item = collection.getJSONObject(i);
                        JSONObject playlist = item.getJSONObject("playlist");
                        mPlaylists.add(playlist);
                    }

                    //// TODO: 'next_href' might point to duplicate data for some dumbass reason so just ignore it for now
//                    // Load next page if one exists
//                    // or update the ViewModel.
//                    if(!nextPage.isEmpty() && !nextPage.equals("null")) {
//                        loadPlaylistData(nextPage);
//                    } else {
//                        fgUpdateTrackModel();
//                    }

                    fgUpdatePlaylistModel();
                } catch(IOException | org.json.JSONException error) {
                    Log.e(LOG_TAG, "Error parsing response.", error);
                    fgUpdateView(VisibleView.error);
                }
            }
        });
    }

    private void selectPlaylist(JSONObject playlist){
        mSelectedPlaylist = playlist; 

        try {
            Long id = playlist.getLong("id");
            String secretToken = playlist.getString("secret_token");
            String title = playlist.getString("title");
            String count = playlist.getString("track_count") + " tracks";

            mPlaylistSelectedTitle.setText(title);
            mPlaylistSelectedCount.setText(count);

            loadPlaylistData(id, secretToken);
        } catch(JSONException e) {
            Log.e(LOG_TAG, "Error parsing json", e);
            return;
        }

        setVisibleView(VisibleView.selection);
    }

    private void loadPlaylistData(Long playlistId, String secretToken) {
        setVisibleView(VisibleView.loading);

        String representation = "full";

        String endpoint = "/playlists/" + playlistId;

        String params = "?representation=" + representation
                + "&secret_token=" + secretToken
                + "&client_id=" + getString(R.string.client_id)
                + "&app_version=" + getString(R.string.app_version)
                + "&app_locale=" + getString(R.string.app_locale);

        String url = getString(R.string.api_root) + endpoint + params;

        Log.d(LOG_TAG, "Loading playlist data...\n" + url);

        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "OAuth " + getString(R.string.token))
                .build();

        OkHttpClient client = CloudClient.getInstance().getClient();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(LOG_TAG, "Error loading playlist data.", e);
                fgUpdateView(VisibleView.error);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try {
                    if(!response.isSuccessful()) throw new IOException("Unexpected code" + response);

                    ResponseBody responseBody = response.body();
                    String rawResponse = responseBody.string();

                    JSONObject parsed = new JSONObject(rawResponse);

                    JSONArray playlistTracks = parsed.getJSONArray("tracks");
                    ArrayList<JSONObject> tracks = new ArrayList<>();

                    for(int i=0; i < playlistTracks.length(); i++) {
                        JSONObject track = playlistTracks.getJSONObject(i);

                        // Check if there's valid data
                        // because, for some reason, some of the tracks return with missing/redacted data.
                        if(track.has("title")) tracks.add(playlistTracks.getJSONObject(i));
                    }

                    fgUpdatePlaylistTracks(tracks);
                } catch(IOException | org.json.JSONException error) {
                    Log.e(LOG_TAG, "Error parsing response.", error);
                    fgUpdateView(VisibleView.error);
                }
            }
        });
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

            default:
                return VisibleView.loading;
        }
    }

    /** A helper that uses a handler to avoid updating from a bg thread */
    private void fgUpdateView(VisibleView visibleView) {
        fgHandler.post(new Runnable() {
            @Override
            public void run() {
                setVisibleView(visibleView);
            }
        });
    }

    /** A helper that uses a handler to avoid updating from a bg thread */
    private void fgUpdatePlaylistModel() {
        fgHandler.post(new Runnable() {
            @Override
            public void run() {
                playlistsViewModel.setPlaylists(mPlaylists);
            }
        });
    }

    /** A helper that uses a handler to avoid updating from a bg thread */
    private void fgUpdatePlaylistTracks(ArrayList<JSONObject> tracks) {
        fgHandler.post(new Runnable() {
            @Override
            public void run() {
                mTrackAdapter.updateTracks(tracks);
            }
        });
    }
}