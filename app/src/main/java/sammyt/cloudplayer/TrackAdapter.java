package sammyt.cloudplayer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import de.voidplus.soundcloud.Track;

public class TrackAdapter extends RecyclerView.Adapter<TrackAdapter.ViewHolder>{

    private final String LOG_TAG = this.getClass().getSimpleName();

    private ArrayList<Track> mTracks = new ArrayList<>();

    private onTrackClickListener mListener;

    public static class ViewHolder extends RecyclerView.ViewHolder{
        TextView trackInfo;

        public ViewHolder(View view){
            super(view);
            trackInfo = view.findViewById(R.id.track_item_info);
        }
    }

    public TrackAdapter(ArrayList<Track> tracks){
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
        String info = track.getUser().getUsername() + " - " + track.getTitle();

        holder.trackInfo.setText(info);
        holder.trackInfo.setOnClickListener(new View.OnClickListener() {
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
        mTracks.clear();
        mTracks = tracks;
        notifyDataSetChanged();
    }
}
