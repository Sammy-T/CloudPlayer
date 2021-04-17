package sammyt.cloudplayer.nav.playlists;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.json.JSONObject;

import java.util.ArrayList;

public class PlaylistsViewModel extends ViewModel {

    private MutableLiveData<ArrayList<JSONObject>> mPlaylists;

    public PlaylistsViewModel() {
        mPlaylists = new MutableLiveData<>();
        mPlaylists.setValue(null);
    }

    public void setPlaylists(ArrayList<JSONObject> playlists){
        mPlaylists.setValue(playlists);
    }

    public LiveData<ArrayList<JSONObject>> getPlaylists() {
        return mPlaylists;
    }
}