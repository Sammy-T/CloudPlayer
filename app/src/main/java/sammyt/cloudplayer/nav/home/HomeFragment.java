package sammyt.cloudplayer.nav.home;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ViewFlipper;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import sammyt.cloudplayer.NavActivity;
import sammyt.cloudplayer.R;
import sammyt.cloudplayer.data.CloudClient;
import sammyt.cloudplayer.nav.SelectedTrackModel;
import sammyt.cloudplayer.nav.TrackAdapter;
import sammyt.cloudplayer.nav.TrackViewModel;

public class HomeFragment extends Fragment {

    private static final String LOG_TAG = HomeFragment.class.getSimpleName();

    private ViewFlipper viewFlipper;

    private TrackViewModel trackViewModel;
    private SelectedTrackModel selectedTrackModel;

    private TrackAdapter mAdapter;

    private final ArrayList<JSONObject> mTracks = new ArrayList<>();

    private final Handler fgHandler = new Handler(Looper.getMainLooper());

    private enum VisibleView {
        loading, loaded, error
    }

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);

        viewFlipper = root.findViewById(R.id.home_flipper);
        TextView titleView = root.findViewById(R.id.title_liked_text);
        RecyclerView trackRecycler = root.findViewById(R.id.liked_tracks_recycler);
        Button retryLoading = root.findViewById(R.id.retry);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        trackRecycler.setLayoutManager(layoutManager);

        mAdapter = new TrackAdapter(getContext(), null);
        mAdapter.setOnTrackClickListener(mTrackClickListener);
        trackRecycler.setAdapter(mAdapter);

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
                    loadTrackData(null);
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

        View.OnClickListener reloadListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadTrackData(null);
            }
        };

        // Allow manually refreshing the data by clicking on the title
        titleView.setOnClickListener(reloadListener);

        // The button shown if the data fails to load
        // Allows the user to manually retry loading the data
        retryLoading.setOnClickListener(reloadListener);

        return root;
    }

    private TrackAdapter.onTrackClickListener mTrackClickListener = new TrackAdapter.onTrackClickListener() {
        @Override
        public void onTrackClick(int position, JSONObject track) {
            Log.d(LOG_TAG, "Track Clicked - " + position + " " + track.optString("title") + " " + track);

            selectedTrackModel.setSelectedTrack(position, track, trackViewModel.getTracks().getValue(), LOG_TAG);
        }
    };

    private void loadTrackData(String url) {
        if(url == null) {
            Log.d(LOG_TAG, "Loading track data...");

            setVisibleView(VisibleView.loading);

            String limit = "24";
            String offset = "2019-08-22T06:36:46.882Z,user-track-likes,728-00000000000042908683-00000000000432120552";

            String endpoint = "/users/" + getString(R.string.user_id) + "/track_likes";

            String params = "?offset=" + offset
                    + "&limit=" + limit
                    + "&client_id=" + getString(R.string.client_id)
                    + "&app_version=" + getString(R.string.app_version)
                    + "&app_locale=" + getString(R.string.app_locale);

            // Set url to load initial page
            url = getString(R.string.api_root) + endpoint + params;

            mTracks.clear(); // Make sure we're not appending to possibly stale data
        }

        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "OAuth " + getString(R.string.token))
                .build();

        OkHttpClient client = CloudClient.getInstance().getClient();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(LOG_TAG, "Error loading liked tracks.", e);
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
                        JSONObject track = item.getJSONObject("track");
                        mTracks.add(track);
                    }

                    // Load next page if one exists
                    // or update the ViewModel.
                    if(!nextPage.isEmpty() && !nextPage.equals("null")) {
                        loadTrackData(nextPage);
                    } else {
                        fgUpdateTrackModel();
                    }
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

            case loaded:
                viewFlipper.setDisplayedChild(1);
                break;

            case error:
                viewFlipper.setDisplayedChild(2);
                break;
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
    private void fgUpdateTrackModel() {
        fgHandler.post(new Runnable() {
            @Override
            public void run() {
                trackViewModel.setTracks(mTracks);
            }
        });
    }
}