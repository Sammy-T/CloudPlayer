package sammyt.cloudplayer.ui.playlists;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class PlaylistsViewModel extends ViewModel {

    private MutableLiveData<String> mText;

    public PlaylistsViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is playlists fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}