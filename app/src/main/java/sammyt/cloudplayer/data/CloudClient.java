package sammyt.cloudplayer.data;

import android.util.Log;

import okhttp3.OkHttpClient;

public class CloudClient {

    private static final String LOG_TAG = CloudClient.class.getSimpleName();

    private static volatile CloudClient mInstance;

    private OkHttpClient client;

    private CloudClient(){}

    public static CloudClient getInstance(){
        if(mInstance == null){
            synchronized(CloudClient.class){
                if(mInstance == null){
                    mInstance = new CloudClient();
                }
            }
        }

        return mInstance;
    }

    /**
     * Returns the OkHttpClient, creating one if it doesn't already exist.
     * @return The OkHttpClient
     */
    public OkHttpClient getClient() {
        if(client == null) {
            Log.d(LOG_TAG, "Creating new HTTP Client.");
            client = new OkHttpClient();
        }

        return client;
    }
}
