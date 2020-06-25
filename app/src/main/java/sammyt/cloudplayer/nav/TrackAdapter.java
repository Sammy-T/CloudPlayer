package sammyt.cloudplayer.nav;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.math.MathUtils;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;

import de.voidplus.soundcloud.Track;
import sammyt.cloudplayer.R;

public class TrackAdapter extends RecyclerView.Adapter<TrackAdapter.ViewHolder>{

    private final String LOG_TAG = this.getClass().getSimpleName();

    private Context mContext;
    private ArrayList<JSONObject> mTracks = new ArrayList<>();
    private JSONObject mSelectedTrack;

    private int mStartColor = Color.parseColor("#000066");
    private int mEndColor = Color.parseColor("#660033");

    private onTrackClickListener mListener;

    public static class ViewHolder extends RecyclerView.ViewHolder{
        RelativeLayout trackItem;
        TextView trackTitle;
        TextView trackArtist;
        ImageView trackImage;

        public ViewHolder(View view){
            super(view);
            trackItem = view.findViewById(R.id.track_item);
            trackTitle = view.findViewById(R.id.track_item_title);
            trackArtist = view.findViewById(R.id.track_item_artist);
            trackImage = view.findViewById(R.id.track_item_image);
        }
    }

    public TrackAdapter(Context context, ArrayList<JSONObject> tracks){
        mContext = context;

        if(tracks != null) {
            mTracks = tracks;
        }
    }

    public interface onTrackClickListener{
        void onTrackClick(int position, JSONObject track);
    }

    public void setOnTrackClickListener(onTrackClickListener l){
        mListener = l;
    }

    // Create new views (invoked by Layout Manager)
    @Override
    @NonNull
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
        // set the view's size, margins, paddings and layout parameters here if needed

        int layout = R.layout.track_item;

        View view = LayoutInflater.from(parent.getContext())
                .inflate(layout, parent, false);

        ViewHolder viewHolder = new ViewHolder(view);

        return  viewHolder;
    }

    // Replace contents of view (invoked by Layout Manager)
    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position){
        // Set the item background color interpolating between start and end values
        int[] startRGB = {Color.red(mStartColor), Color.green(mStartColor), Color.blue(mStartColor)};
        int[] endRGB = {Color.red(mEndColor), Color.green(mEndColor), Color.blue(mEndColor)};

        int[] valueRGB = getInterpolatedRGB(startRGB, endRGB, position);
        int valueColor = Color.argb(115, valueRGB[0], valueRGB[1], valueRGB[2]);

        holder.trackItem.setBackgroundColor(valueColor);

//        Log.d(LOG_TAG, "Start rgb: " + Arrays.toString(startRGB) + " End rgb: " + Arrays.toString(endRGB)
//                +" rgb val: " + Arrays.toString(valueRGB));

        String title;
        String artist;
        String trackImage;

        final JSONObject track = mTracks.get(position);

        try{
            title = track.getString("title");
            artist = track.getJSONObject("user").getString("username");
            trackImage = track.getString("artwork_url");
        }catch(JSONException e){
            Log.e(LOG_TAG, "Unable to retrieve track data.", e);
            return;
        }

        // Set the item's title and artist info
        holder.trackTitle.setText(title);
        holder.trackArtist.setText(artist);

        // Set the item's text color
        int textColor = ContextCompat.getColor(mContext, R.color.colorText);

        long trackId = -1;
        long selectedTrackId = -2;

        try{
            trackId = track.getLong("id");
            if(mSelectedTrack != null) {
                selectedTrackId = mSelectedTrack.getLong("id");
            }
        }catch(JSONException e){
            Log.e(LOG_TAG, "Unable to retrieve track or selected track id", e);
        }

        if(mSelectedTrack != null && trackId == selectedTrackId){
            textColor = ContextCompat.getColor(mContext, R.color.colorPrimary);
        }

        holder.trackTitle.setTextColor(textColor);
        holder.trackArtist.setTextColor(textColor);

        // Set the item's track image
        if(trackImage != null){
            holder.trackImage.setVisibility(View.VISIBLE);

            // Request measuring of the item's view so we have some dimensions to use
            holder.itemView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            int width = holder.itemView.getMeasuredHeight();
            int height = holder.itemView.getMeasuredHeight();

            // Load the track's image
            Picasso.get()
                    .load(trackImage)
                    .resize(width, height)
                    .onlyScaleDown()
                    .centerCrop()
                    .error(android.R.drawable.stat_notify_error)
                    .into(holder.trackImage);
        }else{
            holder.trackImage.setVisibility(View.INVISIBLE);
        }

        holder.trackItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mListener != null){
                    mListener.onTrackClick(holder.getAdapterPosition(), track);
                }
            }
        });
    }

    // Return the size of the dataset (invoked by Layout Manager)
    @Override
    public int getItemCount(){
        if(mTracks == null){
            Log.wtf(LOG_TAG, "How the f*ck are you null?!");
            return 0;
        }
        return mTracks.size();
    }

    public void updateTracks(ArrayList<JSONObject> tracks){
        mTracks = tracks;
        notifyDataSetChanged();
    }

    public void setSelectedTrack(JSONObject selectedTrack){
        mSelectedTrack = selectedTrack;
        notifyDataSetChanged();
    }

    private int[] getInterpolatedRGB(int[] startRGB, int[] endRGB, int position){
        // Determine the direction relative to the start RGB value
        int[] rgbDirection = {1, 1, 1};

        if(startRGB[0] > endRGB[0]){
            rgbDirection[0] = -1;
        }

        if(startRGB[1] > endRGB[1]){
            rgbDirection[1] = -1;
        }

        if(startRGB[2] > endRGB[2]){
            rgbDirection[2] = -1;
        }

        // Calculate the increments
        float[] rgbStep = new float[3];
        rgbStep[0] = (float) Math.abs(startRGB[0] - endRGB[0]) / mTracks.size();
        rgbStep[1] = (float) Math.abs(startRGB[1] - endRGB[1]) / mTracks.size();
        rgbStep[2] = (float) Math.abs(startRGB[2] - endRGB[2]) / mTracks.size();

        // Calculate the RGB value for the set position
        int[] value = new int[3];
        value[0] = startRGB[0] + (int) ((rgbStep[0] * position) * rgbDirection[0]);
        value[1] = startRGB[1] + (int) ((rgbStep[1] * position) * rgbDirection[1]);
        value[2] = startRGB[2] + (int) ((rgbStep[2] * position) * rgbDirection[2]);

        // Clamp the values within RGB bounds
        value[0] = MathUtils.clamp(value[0], 0, 255);
        value[1] = MathUtils.clamp(value[1], 0, 255);
        value[2] = MathUtils.clamp(value[2], 0, 255);

//        Log.d(LOG_TAG, "Step: " + Arrays.toString(rgbStep));

        return value;
    }
}
