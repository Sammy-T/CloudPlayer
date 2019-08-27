package sammyt.cloudplayer.ui.playlists;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import sammyt.cloudplayer.R;

public class PlaylistsFragment extends Fragment {

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

        return root;
    }
}