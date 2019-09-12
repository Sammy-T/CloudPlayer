package sammyt.cloudplayer.player;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import de.voidplus.soundcloud.Track;
import sammyt.cloudplayer.R;

public class QueueAdapter extends RecyclerView.Adapter<QueueAdapter.ViewHolder> {

    private final String LOG_TAG = this.getClass().getSimpleName();

    private ArrayList<Track> mTracks = new ArrayList<>();
    private Track mSelectedTrack;

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

    public QueueAdapter(ArrayList<Track> tracks){
        if(tracks != null){
            mTracks = tracks;
        }
    }

    public interface onQueueClickListener{
        void onQueueClick(int position, Track track);
        void onQueueRemove(int position, Track track);
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
        final Track track = mTracks.get(position);
        String title = track.getTitle();
        String artist = track.getUser().getUsername();

        holder.itemTitle.setText(title);
        holder.itemArtist.setText(artist);

        if(mSelectedTrack != null && track.getId().equals(mSelectedTrack.getId())){
            holder.currentIcon.setVisibility(View.VISIBLE);
        }else{
            holder.currentIcon.setVisibility(View.INVISIBLE);
        }

        holder.queueItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mListener != null){
                    mListener.onQueueClick(holder.getAdapterPosition(), track);
                }
            }
        });

        holder.itemRemove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mListener != null){
                    mListener.onQueueRemove(holder.getAdapterPosition(), track);
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

    public void updateTracks(ArrayList<Track> tracks){
        mTracks = tracks;
        notifyDataSetChanged();
    }

    public void setSelectedTrack(Track selectedTrack){
        mSelectedTrack = selectedTrack;
        notifyDataSetChanged();
    }
}