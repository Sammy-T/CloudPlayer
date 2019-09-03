package sammyt.cloudplayer.ui.playlists;

import android.content.Context;
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
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import de.voidplus.soundcloud.Playlist;
import de.voidplus.soundcloud.Track;
import de.voidplus.soundcloud.User;
import sammyt.cloudplayer.MainActivity;
import sammyt.cloudplayer.NavActivity;
import sammyt.cloudplayer.PlaylistAdapter;
import sammyt.cloudplayer.R;
import sammyt.cloudplayer.TrackAdapter;
import sammyt.cloudplayer.data_sc.PlaylistsTask;
import sammyt.cloudplayer.ui.SelectedTrackModel;

public class PlaylistsFragment extends Fragment {

    private final String LOG_TAG = this.getClass().getSimpleName();

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

    private Playlist mSelectedPlaylist;

    private enum VisibleView{
        loading, loaded, error
    }

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        playlistsViewModel = ViewModelProviders.of(this).get(PlaylistsViewModel.class);
        selectedTrackModel = ViewModelProviders.of(getActivity()).get(SelectedTrackModel.class);

        View root = inflater.inflate(R.layout.fragment_playlists, container, false);
        mLoadingView = root.findViewById(R.id.loading);
        mErrorView = root.findViewById(R.id.error);
        Button retryLoading = root.findViewById(R.id.retry_playlists);
        mSwitcher = root.findViewById(R.id.playlists_switcher);

        // Switcher's playlist list layout
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

        mAdapter = new PlaylistAdapter(null);
        mAdapter.setOnPlaylistClickListener(mPlaylistClickListener);
        mPlaylistRecycler.setAdapter(mAdapter);

        // Set up the selected playlist's track recycler view
        LinearLayoutManager layoutManager2 = new LinearLayoutManager(getContext());
        mPlaylistTrackRecycler.setLayoutManager(layoutManager2);

        mTrackAdapter = new TrackAdapter(getContext(), null);
        mTrackAdapter.setOnTrackClickListener(mTrackClickListener);
        mPlaylistTrackRecycler.setAdapter(mTrackAdapter);

        // Observe the View Model to update the adapter
        playlistsViewModel.getPlaylists().observe(this, new Observer<ArrayList<Playlist>>() {
            @Override
            public void onChanged(ArrayList<Playlist> playlists) {
                String logMessage = "ViewModel onChanged - ";

                if(playlists == null){
                    loadPlaylistData();
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

        // The button shown if the data fails to load
        // Allows the user to manually retry loading the data
        retryLoading.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadPlaylistData();
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
        public void onPlaylistClick(int position, Playlist playlist) {
            selectPlaylist(playlist);
        }
    };

    private TrackAdapter.onTrackClickListener mTrackClickListener = new TrackAdapter.onTrackClickListener() {
        @Override
        public void onTrackClick(int position, Track track) {
            selectedTrackModel.setSelectedTrack(position, track, mSelectedPlaylist.getTracks(), LOG_TAG);
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
                playlistsViewModel.setPlaylists(playlists);
            }

            @Override
            public void onFailure() {
                setVisibleView(VisibleView.error);
            }
        });
    }

    private void selectPlaylist(Playlist playlist){
        mSelectedPlaylist = playlist;

        String title = playlist.getTitle();
        String count = playlist.getTracks().size() + " tracks";

        mPlaylistSelectedTitle.setText(title);
        mPlaylistSelectedCount.setText(count);

        mTrackAdapter.updateTracks(playlist.getTracks());

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