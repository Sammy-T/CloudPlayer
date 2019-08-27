package sammyt.cloudplayer.ui.artists;

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

public class ArtistsFragment extends Fragment {

    private ArtistsViewModel artistsViewModel;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        artistsViewModel = ViewModelProviders.of(this).get(ArtistsViewModel.class);

        View root = inflater.inflate(R.layout.fragment_artists, container, false);

        final TextView textView = root.findViewById(R.id.text_dashboard);
        artistsViewModel.getText().observe(this, new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                textView.setText(s);
            }
        });

        return root;
    }
}