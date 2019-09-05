package sammyt.cloudplayer.data_sc;

import android.os.AsyncTask;
import android.os.SystemClock;
import android.util.Log;

import java.util.ArrayList;

import de.voidplus.soundcloud.Playlist;
import de.voidplus.soundcloud.SoundCloud;
import de.voidplus.soundcloud.Track;
import de.voidplus.soundcloud.User;

public class PlaylistsTask extends AsyncTask<Void, Void, Void> {

    private final String LOG_TAG = this.getClass().getSimpleName();

    private String mClientId;
    private String mClientSecret;
    private String mLoginName;
    private String mPassword;
    private User mUser;
    private ArrayList<Playlist> mPlaylists = new ArrayList<>();

    private onFinishListener mListener;

    public PlaylistsTask(String clientId, String clientSecret, String loginName, String password){
        mClientId = clientId;
        mClientSecret = clientSecret;
        mLoginName = loginName;
        mPassword = password;
    }

    public interface onFinishListener{
        void onFinish(User user, ArrayList<Playlist> playlists);
        void onFailure();
    }

    public void setOnFinishListener(onFinishListener l){
        mListener = l;
    }

    @Override
    protected Void doInBackground(Void... params){

        SoundCloud soundCloud = new SoundCloud(mClientId, mClientSecret);
        Log.d(LOG_TAG, "SoundCloud: " + soundCloud);

        boolean loginSuccess = soundCloud.login(mLoginName, mPassword);
        Log.d(LOG_TAG, "login success: " + loginSuccess);

        if(!loginSuccess){
            Log.e(LOG_TAG, "SoundCloud login failed, cancelling task.");
            cancel(true);
            return null;
        }

        mUser = soundCloud.getMe();
        SystemClock.sleep(100);
        Log.d(LOG_TAG, "user: " + mUser);

        int count = mUser.getPrivatePlaylistsCount() + mUser.getPlaylistCount();
        int limit = 50;
        int pages = count / limit + 1;

        Log.d(LOG_TAG, "playlist count: " + count + " playlist pages: " + pages);

        try {
            for(int i = 0; i < pages; i++) {
                ArrayList<Playlist> tempPlaylists = soundCloud.getMePlaylists(i * limit, limit);
                SystemClock.sleep(100);

                // For some reason the Sound Cloud object isn't automatically set
                // when I request tracks from playlists
                // so it throws an error when I try to retrieve the stream url later
                for(Playlist tempPlaylist: tempPlaylists){
                    for(Track tempTrack: tempPlaylist.getTracks()){
                        tempTrack.setSoundCloud(soundCloud); // Make sure each track has a valid SC object
                    }
                }

                mPlaylists.addAll(tempPlaylists);
            }
        }catch(NullPointerException e){
            Log.e(LOG_TAG, "Error getting playlists.", e);
            cancel(true);
            return null;
        }

        return null;
    }

    @Override
    protected void onCancelled(){
        if(mListener != null){
            mListener.onFailure();
        }
    }

    @Override
    protected void onPostExecute(Void result){
        if(mListener != null){
            mListener.onFinish(mUser, mPlaylists);
        }
    }
}
