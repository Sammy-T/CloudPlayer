package sammyt.cloudplayer.nav.playlists;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;

import de.voidplus.soundcloud.Playlist;

public class PlaylistsViewModel extends ViewModel {

    private MutableLiveData<ArrayList<Playlist>> mPlaylists;

    public PlaylistsViewModel() {
        mPlaylists = new MutableLiveData<>();
        mPlaylists.setValue(null);
    }

    public void setPlaylists(ArrayList<Playlist> playlists){
        mPlaylists.setValue(playlists);
    }

    public LiveData<ArrayList<Playlist>> getPlaylists() {
        return mPlaylists;
    }
}