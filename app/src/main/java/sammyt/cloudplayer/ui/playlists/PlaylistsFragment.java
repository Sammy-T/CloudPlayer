package sammyt.cloudplayer.ui.playlists;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import java.util.ArrayList;

import de.voidplus.soundcloud.Playlist;
import de.voidplus.soundcloud.User;
import sammyt.cloudplayer.R;
import sammyt.cloudplayer.data_sc.PlaylistsTask;

public class PlaylistsFragment extends Fragment {

    private final String LOG_TAG = this.getClass().getSimpleName();

    private PlaylistsViewModel playlistsViewModel;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        playlistsViewModel = ViewModelProviders.of(this).get(PlaylistsViewModel.class);

        View root = inflater.inflate(R.layout.fragment_playlists, container, false);

        final TextView textView = root.findViewById(R.id.text_notifications);
        playlistsViewModel.getText().observe(this, new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                textView.setText(s);
            }
        });

        loadPlaylistData();

        return root;
    }

    private void loadPlaylistData(){
        PlaylistsTask playlistsTask = new PlaylistsTask(
                getString(R.string.client_id),
                getString(R.string.client_secret),
                getString(R.string.login_name),
                getString(R.string.login_password));
        playlistsTask.execute();
        playlistsTask.setOnFinishListener(new PlaylistsTask.onFinishListener() {
            @Override
            public void onFinish(User user, ArrayList<Playlist> playlists) {}

            @Override
            public void onFailure() {}
        });
    }
}