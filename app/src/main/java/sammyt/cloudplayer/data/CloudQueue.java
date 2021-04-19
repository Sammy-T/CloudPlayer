package sammyt.cloudplayer.data;

import android.content.Context;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

public class CloudQueue {

    private static final String LOG_TAG = CloudQueue.class.getSimpleName();

    private static volatile CloudQueue mInstance;

    private static Context mAppContext;
    private RequestQueue mRequestQueue;

    private CloudQueue(Context context){
        mAppContext = context.getApplicationContext();
    }

    public static CloudQueue getInstance(Context context){
        if(mInstance == null){
            synchronized(CloudQueue.class){
                if(mInstance == null){
                    mInstance = new CloudQueue(context);
                }
            }
        }

        return mInstance;
    }

    /**
     * Returns the app's Volley RequestQueue, creating one if it doesn't already exist.
     * @return The app's Volley RequestQueue
     */
    public RequestQueue getRequestQueue() {
        // If the Request Queue is null,
        // create a new one using the Application Context
        if(mRequestQueue == null){
            mRequestQueue = Volley.newRequestQueue(mAppContext);
        }

        return mRequestQueue;
    }
}
