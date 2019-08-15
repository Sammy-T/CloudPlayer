package sammyt.cloudplayer;

import android.content.Context;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.util.Log;

import java.util.ArrayList;

import de.voidplus.soundcloud.SoundCloud;
import de.voidplus.soundcloud.Track;
import de.voidplus.soundcloud.User;

public class UserAndTracksTask extends AsyncTask<Void, Void, Void>{

    private final String LOG_TAG = this.getClass().getSimpleName();

    private String mClientId;
    private String mClientSecret;
    private String mLoginName;
    private String mPassword;
    private User mUser;
    private ArrayList<Track> mFaveTracks = new ArrayList<>();

    private onFinishListener mListener;

    public UserAndTracksTask(String clientId, String clientSecret, String loginName, String password){
        mClientId = clientId;
        mClientSecret = clientSecret;
        mLoginName = loginName;
        mPassword = password;
    }

    public interface onFinishListener{
        void onFinish(User user, ArrayList<Track> faveTracks);
    }

    public void setOnFinishListener(onFinishListener l){
        mListener = l;
    }

    @Override
    protected Void doInBackground(Void... params){

        SoundCloud soundCloud = new SoundCloud(mClientId, mClientSecret);

        soundCloud.login(mLoginName, mPassword);

        mUser = soundCloud.getMe();
        SystemClock.sleep(500);

        int count = mUser.getPublicFavoritesCount();
        int limit = 50;
        int pages = count / limit + 1;

        Log.d(LOG_TAG, "favorites count: " + count + " favorites pages: " + pages);

        for(int i=0; i < pages; i++){
            ArrayList<Track> tempTracks = soundCloud.getMeFavorites(i*limit, limit);
            SystemClock.sleep(500);
            mFaveTracks.addAll(tempTracks);
        }

        return null;
    }

    @Override
    protected void onPostExecute(Void result){
        if(mListener != null){
            mListener.onFinish(mUser, mFaveTracks);
        }
    }
}