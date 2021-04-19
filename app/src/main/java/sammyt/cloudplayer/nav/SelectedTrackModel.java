package sammyt.cloudplayer.nav;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.json.JSONObject;

import java.util.ArrayList;

public class SelectedTrackModel extends ViewModel {

    private MutableLiveData<SelectedTrack> mSelectedTrack;

    public SelectedTrackModel(){
        mSelectedTrack = new MutableLiveData<>();
        mSelectedTrack.setValue(null);
    }

    public void setSelectedTrack(int position, JSONObject track, ArrayList<JSONObject> trackList, String selectionSource){
        SelectedTrack selectedTrack = new SelectedTrack(position, track, trackList, selectionSource);
        mSelectedTrack.setValue(selectedTrack);
    }

    public void updateSelectedTrack(int position, JSONObject track, String selectionSource){
        ArrayList<JSONObject> trackList = mSelectedTrack.getValue().getTrackList();
        setSelectedTrack(position, track, trackList, selectionSource);
    }

    public LiveData<SelectedTrack> getSelectedTrack(){
        return mSelectedTrack;
    }

    // This is a helper class so our observers only need to monitor one Live Data variable
    // that will hold all the values we're interested in
    public class SelectedTrack{
        private int mPos;
        private JSONObject mTrack;
        private ArrayList<JSONObject> mTrackList;
        private String mSelectionSource;

        SelectedTrack(int position, JSONObject selectedTrack, ArrayList<JSONObject> trackList, String selectionSource){
            mPos = position;
            mTrack = selectedTrack;
            mTrackList = trackList;
            mSelectionSource = selectionSource;
        }

        public int getPos(){
            return mPos;
        }

        public JSONObject getTrack(){
            return mTrack;
        }

        public ArrayList<JSONObject> getTrackList(){
            return mTrackList;
        }

        public String getSelectionSource(){
            return mSelectionSource;
        }
    }
}
