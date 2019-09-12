package sammyt.cloudplayer.nav.artists;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.jay.widget.StickyHeadersLinearLayoutManager;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import de.voidplus.soundcloud.Track;
import de.voidplus.soundcloud.User;
import sammyt.cloudplayer.NavActivity;
import sammyt.cloudplayer.R;
import sammyt.cloudplayer.nav.TrackAdapter;
import sammyt.cloudplayer.data_sc.TracksTask;
import sammyt.cloudplayer.nav.SelectedTrackModel;
import sammyt.cloudplayer.nav.TrackViewModel;

public class ArtistsFragment extends Fragment {

    private final String LOG_TAG = this.getClass().getSimpleName();

    private TrackViewModel trackViewModel;
    private SelectedTrackModel selectedTrackModel;

    private ProgressBar mLoadingView;
    private LinearLayout mErrorView;
    private ViewSwitcher mSwitcher;
    private ImageView mArtistImage;
    private TextView mArtistTitle;

    private ArtistsAdapter mAdapter;
    private TrackAdapter mTrackAdapter;

    private ArrayList<Track> mArtistTracks = new ArrayList<>();

    private enum VisibleView{
        loading, loaded, error
    }

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        trackViewModel = ViewModelProviders.of(getActivity()).get(TrackViewModel.class);
        selectedTrackModel = ViewModelProviders.of(getActivity()).get(SelectedTrackModel.class);

        View root = inflater.inflate(R.layout.fragment_artists, container, false);
        mLoadingView = root.findViewById(R.id.loading);
        mErrorView = root.findViewById(R.id.error);
        Button retryLoading = root.findViewById(R.id.retry_artists);
        mSwitcher = root.findViewById(R.id.artists_switcher);

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
        mSwitcher.setInAnimation(inAnim);
        mSwitcher.setOutAnimation(outAnim);

        // Set up artist recycler view
        StickyHeadersLinearLayoutManager<ArtistsAdapter> layoutManager =
                new StickyHeadersLinearLayoutManager<>(getContext());
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

        trackViewModel.getTracks().observe(this, new Observer<ArrayList<Track>>() {
            @Override
            public void onChanged(ArrayList<Track> tracks) {
                String logMessage = "ViewModel onChanged - ";

                if(tracks == null){
                    loadTrackData();
                }else{
                    setVisibleView(VisibleView.loaded);
                }

                if(mAdapter != null){
                    logMessage += "Adapter update";
                    mAdapter.updateArtistTracks(tracks);
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
                loadTrackData();
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

    private ArtistsAdapter.OnArtistClickListener mArtistClickListener = new ArtistsAdapter.OnArtistClickListener() {
        @Override
        public void onArtistClick(int position, User artist) {
            selectArtist(artist);
        }
    };

    private TrackAdapter.onTrackClickListener mTrackClickListener = new TrackAdapter.onTrackClickListener() {
        @Override
        public void onTrackClick(int position, Track track) {
            selectedTrackModel.setSelectedTrack(position, track, mArtistTracks, LOG_TAG);
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
                trackViewModel.setTracks(faveTracks);
            }

            @Override
            public void onFailure() {
                setVisibleView(VisibleView.error);
            }
        });
    }

    private void selectArtist(User artist){
        String title = artist.getUsername();
        final String rawUrl = artist.getAvatarUrl();

        mArtistTitle.setText(title);

        setArtistTracks(artist);

        mSwitcher.setDisplayedChild(1);

        mArtistImage.post(new Runnable() {
            @Override
            public void run() {
                loadArtistImage(rawUrl);
            }
        });
    }

    private void setArtistTracks(final User artist){
        new Thread(new Runnable() {
            @Override
            public void run() {
                ArrayList<Track> allTracks = trackViewModel.getTracks().getValue();

                mArtistTracks.clear();

                if(allTracks == null){
                    Log.w(LOG_TAG, "No tracks to compare.");
                    return;
                }

                for(Track track: allTracks){
                    if(track.getUser().getId().equals(artist.getId())){
                        mArtistTracks.add(track);
                    }
                }

                mTrackAdapter.updateTracks(mArtistTracks);
            }
        }).start();
    }

    private void loadArtistImage(String rawUrl){
        int width = mArtistImage.getWidth();
        int height = mArtistImage.getHeight();

        String defaultUrl = "https://a1.sndcdn.com/images/default_avatar_large.png";

        if(rawUrl != null && !rawUrl.equals(defaultUrl)){
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