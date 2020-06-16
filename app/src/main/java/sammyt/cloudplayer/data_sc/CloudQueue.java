package sammyt.cloudplayer.data_sc;

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

    public RequestQueue getRequestQueue(){
        // If the Request Queue is null,
        // create a new one using the Application Context
        if(mRequestQueue == null){
            mRequestQueue = Volley.newRequestQueue(mAppContext);
        }

        return mRequestQueue;
    }
}
