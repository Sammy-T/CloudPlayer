package sammyt.cloudplayer.ui.home;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;

import de.voidplus.soundcloud.Track;
import de.voidplus.soundcloud.User;
import sammyt.cloudplayer.R;
import sammyt.cloudplayer.UserAndTracksTask;

public class HomeViewModel extends ViewModel {

    private final String LOG_TAG = this.getClass().getSimpleName();

    private MutableLiveData<ArrayList<Track>> mTracks;

    public HomeViewModel() {
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