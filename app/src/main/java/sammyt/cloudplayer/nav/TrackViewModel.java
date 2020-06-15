package sammyt.cloudplayer.nav;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.json.JSONObject;

import java.util.ArrayList;

import de.voidplus.soundcloud.Track;

public class TrackViewModel extends ViewModel {

    private final String LOG_TAG = this.getClass().getSimpleName();

    private MutableLiveData<ArrayList<JSONObject>> mTracks;

    public TrackViewModel() {
        mTracks = new MutableLiveData<>();
        mTracks.setValue(null);
    }

    public void setTracks(ArrayList<JSONObject> tracks){
        mTracks.setValue(tracks);
    }

    public LiveData<ArrayList<JSONObject>> getTracks(){
        return mTracks;
    }
}