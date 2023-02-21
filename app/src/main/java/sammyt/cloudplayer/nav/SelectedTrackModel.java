package sammyt.cloudplayer.nav;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.media3.common.MediaItem;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class SelectedTrackModel extends ViewModel {

    private final String LOG_TAG = this.getClass().getSimpleName();

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

    public void updateSelectedTrack(MediaItem mediaItem, String selectionSource) {
        // Find the track with the matching attributes
        ArrayList<JSONObject> trackList = mSelectedTrack.getValue().getTrackList();

        int trackPos = findTrackPosFromMedia(mediaItem, trackList);

        if(trackPos < 0) {
            return;
        }

        JSONObject track = trackList.get(trackPos);

        setSelectedTrack(trackPos, track, trackList, selectionSource);
    }

    public LiveData<SelectedTrack> getSelectedTrack(){
        return mSelectedTrack;
    }

    private int findTrackPosFromMedia(MediaItem mediaItem, ArrayList<JSONObject> trackList) {
        int found = -1;

        try {
            for(int i=0; i < trackList.size(); i++) {
                String streamUrl = trackList.get(i).getString("stream_url");
                if(mediaItem.mediaId.equals(streamUrl)) {
                    found = i;
                    break;
                }
            }
        } catch(JSONException e) {
            Log.e(LOG_TAG, "Unable to process JSONObject", e);
        }

        return found;
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
