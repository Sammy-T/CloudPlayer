package sammyt.cloudplayer.player;

import android.media.browse.MediaBrowser;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.media3.common.MediaItem;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import sammyt.cloudplayer.R;

public class QueueAdapter extends RecyclerView.Adapter<QueueAdapter.ViewHolder> {

    private final String LOG_TAG = this.getClass().getSimpleName();

    private ArrayList<MediaItem> mTracks = new ArrayList<>();
    private MediaItem mSelectedTrack;

    private onQueueClickListener mListener;

    public static class ViewHolder extends RecyclerView.ViewHolder{
        LinearLayout queueItem;
        ImageView currentIcon;
        TextView itemTitle;
        TextView itemArtist;
        ImageButton itemRemove;

        public ViewHolder(View view){
            super(view);
            queueItem = view.findViewById(R.id.queue_item);
            currentIcon = view.findViewById(R.id.queue_item_current);
            itemTitle = view.findViewById(R.id.queue_item_title);
            itemArtist = view.findViewById(R.id.queue_item_artist);
            itemRemove = view.findViewById(R.id.queue_item_remove);
        }
    }

    public QueueAdapter(ArrayList<MediaItem> tracks){
        if(tracks != null){
            mTracks = tracks;
        }
    }

    public interface onQueueClickListener{
        void onQueueClick(int position, MediaItem track);
        void onQueueRemove(int position, MediaItem track);
    }

    public void setOnQueueClickListener(onQueueClickListener l){
        mListener = l;
    }

    // Create new views (invoked by Layout Manager)
    @Override
    @NonNull
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
        // set the view's size, margins, paddings and layout parameters here if needed

        int layout = R.layout.queue_item;

        View view = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);

        ViewHolder viewHolder = new ViewHolder(view);

        return  viewHolder;
    }

    // Replace contents of view (invoked by Layout Manager)
    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position){
        final MediaItem track = mTracks.get(position);
        String title;
        String artist;

        title = (String) track.mediaMetadata.title;
        artist = (String) track.mediaMetadata.artist;

        holder.itemTitle.setText(title);
        holder.itemArtist.setText(artist);

        String trackId = track.mediaId;
        String selectedTrackId = mSelectedTrack.mediaId;

        if(trackId.equals(selectedTrackId)){
            holder.currentIcon.setVisibility(View.VISIBLE);
        }else{
            holder.currentIcon.setVisibility(View.INVISIBLE);
        }

        holder.queueItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mListener != null){
                    mListener.onQueueClick(holder.getBindingAdapterPosition(), track);
                }
            }
        });

        holder.itemRemove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mListener != null){
                    mListener.onQueueRemove(holder.getBindingAdapterPosition(), track);
                }
            }
        });
    }

    // Return the size of the dataset (invoked by Layout Manager)
    @Override
    public int getItemCount(){
        if(mTracks == null){
            return 0;
        }

        return mTracks.size();
    }

    public void removeTrack(int position) {
        mTracks.remove(position);
        notifyItemRemoved(position);
    }

    public void updateTracks(ArrayList<MediaItem> tracks){
        mTracks = tracks;
        notifyDataSetChanged();
    }

    public void setSelectedTrack(MediaItem selectedTrack){
        mSelectedTrack = selectedTrack;
        notifyDataSetChanged();
    }
}