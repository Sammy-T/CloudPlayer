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
    private int mHueDirection = 1;
    private float[] mHsvStep = new float[3];
    private boolean mCalculateStep = true;

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
        float[] startHSV = new float[3];
        float[] endHSV = new float[3];

        Color.colorToHSV(mStartColor, startHSV);
        Color.colorToHSV(mEndColor, endHSV);

        float[] valueHSV = getInterpolatedHSV(startHSV, endHSV, position);
        int valueColor = Color.HSVToColor(115, valueHSV);

        holder.trackItem.setBackgroundColor(valueColor);

//        Log.d(LOG_TAG, "Start hsv: " + Arrays.toString(startHSV) + " End hsv: " + Arrays.toString(endHSV)
//                +"\nhsv val: " + Arrays.toString(valueHSV));

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
        mCalculateStep = true;
        notifyDataSetChanged();
    }

    public void setSelectedTrack(JSONObject selectedTrack){
        mSelectedTrack = selectedTrack;
        mCalculateStep = true;
        notifyDataSetChanged();
    }

    private float getHueDistance(float startHue, float endHue){
        float startOffset = 0;
        float endOffset = 0;

        if(startHue < endHue){
            startOffset = 360;
        }else if(endHue < startHue){
            endOffset = 360;
        }

        // Compare regularly
        // and compare with an offset to account for wrapping values towards the edges of 0 and 360
        float hueDist = Math.abs(startHue - endHue);
        float hueDistWrap = Math.abs((startHue + startOffset) - (endHue + endOffset));

//        Log.d(LOG_TAG, "Hue dist: " + hueDist + " Hue dist(w): " + hueDistWrap);

        float hueDistance = Math.min(hueDist, hueDistWrap);

        // Determine the direction we're travelling around the hue wheel
        if(hueDistance == hueDistWrap){
            if(startHue < endHue){
                mHueDirection = -1;
            }else{
                mHueDirection = 1;
            }
        }else{
            if(startHue < endHue){
                mHueDirection = 1;
            }else{
                mHueDirection = -1;
            }
        }

        return hueDistance;
    }

    private float[] getInterpolatedHSV(float[] startHSV, float[] endHSV, int position){
        // Calculate the increments
        if(mCalculateStep){
            mHsvStep[0] = getHueDistance(startHSV[0], endHSV[0]) / mTracks.size();
            mHsvStep[1] = Math.abs(startHSV[1] - endHSV[1]) / mTracks.size();
            mHsvStep[2] = Math.abs(startHSV[2] - endHSV[2]) / mTracks.size();

            mCalculateStep = false;
        }

        float[] value = new float[3];

        // Calculate the Hue at the set position
        value[0] = startHSV[0] + ((mHsvStep[0] * position) * mHueDirection);

        // Wrap the Hue if it's out of bounds
        if(value[0] < 0){
            value[0] = value[0] + 360;
        }else if(value[0] > 360){
            value[0] = value[0] - 360;
        }

        // Calculate the Saturation
        if(startHSV[1] < endHSV[1]){
            value[1] = startHSV[1] + (mHsvStep[1] * position);
        }else{
            value[1] = startHSV[1] - (mHsvStep[1] * position);
        }

        // Calculate the Value
        if(startHSV[2] < endHSV[2]){
            value[2] = startHSV[2] + (mHsvStep[2] * position);
        }else{
            value[2] = startHSV[2] - (mHsvStep[2] * position);
        }

//        Log.d(LOG_TAG, "hsv step: " + Arrays.toString(mHsvStep) + " hsv value: " + Arrays.toString(value));

        return value;
    }
}
