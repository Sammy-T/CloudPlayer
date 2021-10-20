package sammyt.cloudplayer.nav.artists;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
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
import com.android.volley.toolbox.JsonObjectRequest;
import com.jay.widget.StickyHeadersLinearLayoutManager;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import sammyt.cloudplayer.NavActivity;
import sammyt.cloudplayer.R;
import sammyt.cloudplayer.data.CloudQueue;
import sammyt.cloudplayer.nav.SelectedTrackModel;
import sammyt.cloudplayer.nav.TrackAdapter;
import sammyt.cloudplayer.nav.TrackViewModel;

public class ArtistsFragment extends Fragment {

    private static final String LOG_TAG = ArtistsFragment.class.getSimpleName();

    private String token;

    private ViewFlipper viewFlipper;
    private ImageView mArtistImage;
    private TextView mArtistTitle;

    private Handler mHandler = new Handler();

    private TrackViewModel trackViewModel;
    private SelectedTrackModel selectedTrackModel;

    private ArtistsAdapter mAdapter;
    private TrackAdapter mTrackAdapter;

    private ArrayList<JSONObject> mArtistTracks = new ArrayList<>();

    private ArrayList<JSONObject> mTracks = new ArrayList<>();

    private enum VisibleView{
        loading, artist, selection, error, error_auth
    }

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_artists, container, false);

        viewFlipper = root.findViewById(R.id.artists_flipper);
        Button retryLoading = root.findViewById(R.id.retry);
        Button retryLoading2 = root.findViewById(R.id.retry2);
        Button refreshAuth = root.findViewById(R.id.refresh_auth);

        // Switcher's artist list layout
        TextView titleView = root.findViewById(R.id.title_artists_text);
        RecyclerView artistRecycler = root.findViewById(R.id.artists_recycler);

        // Switcher's selected artist layout
        mArtistImage = root.findViewById(R.id.artist_image);
        mArtistTitle = root.findViewById(R.id.artist_selected_title);
        RecyclerView artistTrackRecycler = root.findViewById(R.id.artist_track_recycler);

        // Set the View Switchers animations
        Animation inAnim = AnimationUtils.loadAnimation(getContext(), android.R.anim.slide_in_left);
        Animation outAnim = AnimationUtils.loadAnimation(getContext(), android.R.anim.slide_out_right);
        viewFlipper.setInAnimation(inAnim);
        viewFlipper.setOutAnimation(outAnim);

        // Set up artist recycler view
        StickyHeadersLinearLayoutManager<ArtistsAdapter> layoutManager = new StickyHeadersLinearLayoutManager<>(getContext());
        artistRecycler.setLayoutManager(layoutManager);

        mAdapter = new ArtistsAdapter(null);
        mAdapter.setOnArtistClickListener(mArtistClickListener);
        artistRecycler.setAdapter(mAdapter);

        // Set up artist track recycler view
        LinearLayoutManager layoutManager2 = new LinearLayoutManager(getContext());
        artistTrackRecycler.setLayoutManager(layoutManager2);

        mTrackAdapter = new TrackAdapter(getContext(), null);
        mTrackAdapter.setOnTrackClickListener(mTrackClickListener);
        artistTrackRecycler.setAdapter(mTrackAdapter);

        // Set up the ViewModels
        ViewModelProvider activityModelProvider = new ViewModelProvider(requireActivity());
        trackViewModel = activityModelProvider.get(TrackViewModel.class);
        selectedTrackModel = activityModelProvider.get(SelectedTrackModel.class);

        trackViewModel.getTracks().observe(getViewLifecycleOwner(), new Observer<ArrayList<JSONObject>>() {
            @Override
            public void onChanged(ArrayList<JSONObject> tracks) {
                // Since we're initializing the View Model before we're able to retrieve the
                // activity's token, ignore callbacks without it (i.e. the initial callback).
                if(token == null) return;

                String logMessage = "ViewModel onChanged - ";

                if(tracks == null){
                    logMessage += "New load ";
                    loadTrackDataFromVolley(null);
                }else{
                    setVisibleView(VisibleView.artist);
                }

                if(mAdapter != null){
                    logMessage += "Adapter update";
                    mAdapter.updateArtistTracks(tracks);
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
                loadTrackDataFromVolley(null);
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
                    setVisibleView(VisibleView.artist); // Navigate back to the artist list
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

        // Perform the initial load if necessary.
        if(trackViewModel.getTracks().getValue() == null) {
            Log.d(LOG_TAG, "New load from onStart");
            loadTrackDataFromVolley(null);
        }
    }

    private ArtistsAdapter.OnArtistClickListener mArtistClickListener = new ArtistsAdapter.OnArtistClickListener() {
        @Override
        public void onArtistClick(int position, JSONObject artist) {
            selectArtist(artist);
        }
    };

    private TrackAdapter.onTrackClickListener mTrackClickListener = new TrackAdapter.onTrackClickListener() {
        @Override
        public void onTrackClick(int position, JSONObject track) {
            selectedTrackModel.setSelectedTrack(position, track, mArtistTracks, LOG_TAG);
        }
    };

    private void loadTrackDataFromVolley(String url){
        RequestQueue queue = CloudQueue.getInstance(getContext()).getRequestQueue();

        if(url == null) {
            Log.d(LOG_TAG, "Loading track data from volley.");
            setVisibleView(VisibleView.loading);

            String endpoint = "/me/likes/tracks";
            url = getString(R.string.api_root) + endpoint + "?linked_partitioning=true";

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
                        loadTrackDataFromVolley(nextPage);
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

                if(error.networkResponse.statusCode == 401) {
                    Log.w(LOG_TAG, "Unauthorized access. Token:" + token);
                    setVisibleView(VisibleView.error_auth);
                } else {
                    setVisibleView(VisibleView.error);
                }
            }
        };

        JsonObjectRequest jsonRequest = new JsonObjectRequest(
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

    private void selectArtist(JSONObject artist){
        String title = artist.optString("username");
        final String rawUrl = artist.optString("avatar_url");

        mArtistTitle.setText(title);

        setArtistTracks(artist);

        setVisibleView(VisibleView.selection);

        mArtistImage.post(new Runnable() {
            @Override
            public void run() {
                loadArtistImage(rawUrl);
            }
        });
    }

    private void setArtistTracks(final JSONObject artist){
        new Thread(new Runnable() {
            @Override
            public void run() {
                ArrayList<JSONObject> allTracks = trackViewModel.getTracks().getValue();

                mArtistTracks.clear();

                if(allTracks == null){
                    Log.w(LOG_TAG, "No tracks to compare.");
                    return;
                }

                try{
                    for (JSONObject track : allTracks) {
                        if (track.getJSONObject("user").getLong("id") == artist.getLong("id")) {
                            mArtistTracks.add(track);
                        }
                    }
                }catch(JSONException e){
                    Log.e(LOG_TAG, "Unable to set artist tracks.", e);
                    return;
                }

                // Make sure we're updating the adapter from the correct thread
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mTrackAdapter.updateTracks(mArtistTracks);
                    }
                });
            }
        }).start();
    }

    private void loadArtistImage(String rawUrl){
        int width = mArtistImage.getWidth();
        int height = mArtistImage.getHeight();

        String defaultUrl = "https://a1.sndcdn.com/images/default_avatar_large.png";

        if(rawUrl != null && !rawUrl.equals(defaultUrl)){
            //// TODO: SC is odd and I don't remember why I'm doing this here
            final String trackArtUrl = rawUrl.replace("large", "t500x500");

            // Load the track image
            Picasso.get()
                    .load(trackArtUrl)
                    .resize(width, height)
                    .centerCrop()
                    .error(android.R.drawable.stat_notify_error)
                    .into(mArtistImage, new Callback() {
                        @Override
                        public void onSuccess() {
                            Log.d(LOG_TAG, "Picasso successfully loaded " + trackArtUrl);
                        }

                        @Override
                        public void onError(Exception e) {
                            Log.e(LOG_TAG, "Picasso error " + trackArtUrl, e);
                        }
                    });
        }else{
            // Load the fallback image
            Picasso.get()
                    .load(R.drawable.ic_play_grey600_48dp)
                    .resize(width, height)
                    .centerInside()
                    .error(android.R.drawable.stat_notify_error)
                    .into(mArtistImage, new Callback() {
                        @Override
                        public void onSuccess() {
                            Log.d(LOG_TAG, "Picasso successfully loaded fallback image");
                        }

                        @Override
                        public void onError(Exception e) {
                            Log.e(LOG_TAG, "Picasso error loading fallback image", e);
                        }
                    });
        }
    }

    private void setVisibleView(VisibleView visibleView){
        switch(visibleView){
            case loading:
                viewFlipper.setDisplayedChild(0);
                break;

            case artist:
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
                return VisibleView.artist;

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