package sammyt.cloudplayer.ui;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;

import de.voidplus.soundcloud.Track;

public class SelectedTrackModel extends ViewModel {

    private MutableLiveData<SelectedTrack> mSelectedTrack;

    public SelectedTrackModel(){
        mSelectedTrack = new MutableLiveData<>();
        mSelectedTrack.setValue(null);
    }

    public void setSelectedTrack(int position, Track track, ArrayList<Track> trackList, String selectionSource){
        SelectedTrack selectedTrack = new SelectedTrack(position, track, trackList, selectionSource);
        mSelectedTrack.setValue(selectedTrack);
    }

    public void updateSelectedTrack(int position, Track track, String selectionSource){
        ArrayList<Track> trackList = mSelectedTrack.getValue().getTrackList();
        setSelectedTrack(position, track, trackList, selectionSource);
    }

    public LiveData<SelectedTrack> getSelectedTrack(){
        return mSelectedTrack;
    }

    // This is a helper class so our observers only need to monitor one Live Data variable
    // that will hold all the values we're interested in
    public class SelectedTrack{
        private int mPos;
        private Track mTrack;
        private ArrayList<Track> mTrackList;
        private String mSelectionSource;

        SelectedTrack(int position, Track selectedTrack, ArrayList<Track> trackList, String selectionSource){
            mPos = position;
            mTrack = selectedTrack;
            mTrackList = trackList;
            mSelectionSource = selectionSource;
        }

        public int getPos(){
            return mPos;
        }

        public Track getTrack(){
            return mTrack;
        }

        public ArrayList<Track> getTrackList(){
            return mTrackList;
        }

        public String getSelectionSource(){
            return mSelectionSource;
        }
    }
}
