package sammyt.cloudplayer.ui.playlists;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import de.voidplus.soundcloud.Playlist;
import sammyt.cloudplayer.R;

public class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.ViewHolder> {

    private final String LOG_TAG = this.getClass().getSimpleName();

    private ArrayList<Playlist> mPlaylists = new ArrayList<>();

    private onPlaylistClickListener mListener;

    public static class ViewHolder extends RecyclerView.ViewHolder{
        LinearLayout playlistItem;
        ImageView playlistImage;
        TextView playlistTitle;
        TextView playlistTrackCount;

        public ViewHolder(View view){
            super(view);
            playlistItem = view.findViewById(R.id.playlist_item);
            playlistImage = view.findViewById(R.id.playlist_item_image);
            playlistTitle = view.findViewById(R.id.playlist_item_title);
            playlistTrackCount = view.findViewById(R.id.playlist_item_track_count);
        }
    }

    public PlaylistAdapter(ArrayList<Playlist> playlists){
        if(playlists != null){
            mPlaylists = playlists;
        }
    }

    public interface onPlaylistClickListener{
        void onPlaylistClick(int position, Playlist playlist);
    }

    public void setOnPlaylistClickListener(onPlaylistClickListener l){
        mListener = l;
    }

    // Create new views (invoked by Layout Manager)
    @Override
    @NonNull
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
        // set the view's size, margins, paddings and layout parameters here if needed

        int layout = R.layout.playlist_item;

        View view = LayoutInflater.from(parent.getContext())
                .inflate(layout, parent, false);

        ViewHolder viewHolder = new ViewHolder(view);

        return  viewHolder;
    }

    // Replace contents of view (invoked by Layout Manager)
    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position){
        final Playlist playlist = mPlaylists.get(position);
        String title = playlist.getTitle();
        String count = playlist.getTracks().size() + " tracks";
        String playlistImage = playlist.getArtworkUrl();

        holder.playlistTitle.setText(title);
        holder.playlistTrackCount.setText(count);

        if(playlistImage == null){
            // Try to fallback to the first track's image
            playlistImage = playlist.getTracks().get(0).getArtworkUrl();
        }

        if(playlistImage != null){
            holder.playlistImage.setVisibility(View.VISIBLE);

            // Request measuring of the item's view so we have some dimensions to use
            holder.itemView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            int width = holder.itemView.getMeasuredHeight();
            int height = holder.itemView.getMeasuredHeight();

            // Load the track's image
            Picasso.get()
                    .load(playlistImage)
                    .resize(width, height)
                    .onlyScaleDown()
                    .centerCrop()
                    .error(android.R.drawable.stat_notify_error)
                    .into(holder.playlistImage);
        }else{
            holder.playlistImage.setVisibility(View.INVISIBLE);
        }

        holder.playlistItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mListener != null){
                    mListener.onPlaylistClick(holder.getAdapterPosition(), playlist);
                }
            }
        });
    }

    // Return the size of the dataset (invoked by Layout Manager)
    @Override
    public int getItemCount(){
        return mPlaylists.size();
    }

    public void updateTracks(ArrayList<Playlist> playlists){
        mPlaylists = playlists;
        notifyDataSetChanged();
    }
}