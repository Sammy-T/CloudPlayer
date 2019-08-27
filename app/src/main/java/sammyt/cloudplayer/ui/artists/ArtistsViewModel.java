package sammyt.cloudplayer.ui.artists;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class ArtistsViewModel extends ViewModel {

    private MutableLiveData<String> mText;

    public ArtistsViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is artists fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}