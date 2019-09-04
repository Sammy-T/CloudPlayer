package sammyt.cloudplayer.ui.artists;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;

import de.voidplus.soundcloud.Track;

public class ArtistsViewModel extends ViewModel {

    private final String LOG_TAG = this.getClass().getSimpleName();

    private MutableLiveData<ArrayList<Track>> mTracks;

    public ArtistsViewModel() {
        mTracks = new MutableLiveData<>();
        mTracks.setValue(null);
    }

    public void setTracks(ArrayList<Track> tracks){
        mTracks.setValue(tracks);
    }

    public LiveData<ArrayList<Track>> getTracks(){
        return mTracks;
    }
}