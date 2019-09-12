package sammyt.cloudplayer.nav;

import android.content.Context;
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

import java.util.ArrayList;

import de.voidplus.soundcloud.Track;
import sammyt.cloudplayer.R;

public class TrackAdapter extends RecyclerView.Adapter<TrackAdapter.ViewHolder>{

    private final String LOG_TAG = this.getClass().getSimpleName();

    private Context mContext;
    private ArrayList<Track> mTracks = new ArrayList<>();
    private Track mSelectedTrack;

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

    public TrackAdapter(Context context, ArrayList<Track> tracks){
        mContext = context;

        if(tracks != null) {
            mTracks = tracks;
        }
    }

    public interface onTrackClickListener{
        void onTrackClick(int position, Track track);
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
        final Track track = mTracks.get(position);
        String title = track.getTitle();
        String artist = track.getUser().getUsername();
        String trackImage = track.getArtworkUrl();

        // Set the item's title and artist info
        holder.trackTitle.setText(title);
        holder.trackArtist.setText(artist);

        // Set the item's text color
        int textColor = ContextCompat.getColor(mContext, R.color.colorText);

        if(mSelectedTrack != null && track.getId().equals(mSelectedTrack.getId())){
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
        return mTracks.size();
    }

    public void updateTracks(ArrayList<Track> tracks){
        mTracks = tracks;
        notifyDataSetChanged();
    }

    public void setSelectedTrack(Track selectedTrack){
        mSelectedTrack = selectedTrack;
        notifyDataSetChanged();
    }
}
