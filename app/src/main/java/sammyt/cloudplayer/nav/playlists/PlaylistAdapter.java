package sammyt.cloudplayer.nav.playlists;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import sammyt.cloudplayer.R;

public class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.ViewHolder> {

    private static final String LOG_TAG = PlaylistAdapter.class.getSimpleName();

    private ArrayList<JSONObject> mPlaylists = new ArrayList<>();

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

    public PlaylistAdapter(){}

    public interface onPlaylistClickListener{
        void onPlaylistClick(int position, JSONObject playlist);
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

        View view = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);

        return new ViewHolder(view);
    }

    // Replace contents of view (invoked by Layout Manager)
    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position){
        final JSONObject playlist = mPlaylists.get(position);

        String title;
        String count;
        String playlistImage;

        try {
            title = playlist.getString("title");
            count = playlist.getString("track_count") + " tracks";
            playlistImage = playlist.getString("artwork_url");

            if(playlistImage == null || playlistImage.equals("null")) {
                // Try to fallback to the first track's image
                playlistImage = playlist.getJSONArray("tracks").getJSONObject(0).getString("artwork_url");
            }
        } catch(JSONException e) {
            Log.e(LOG_TAG, "Error parsing json", e);
            return;
        }

        holder.playlistTitle.setText(title);
        holder.playlistTrackCount.setText(count);

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
        if(mPlaylists == null){
            Log.wtf(LOG_TAG, "How the f*ck are you null?!");
            return 0;
        }
        return mPlaylists.size();
    }

    public void updateTracks(ArrayList<JSONObject> playlists){
        mPlaylists = playlists;
        notifyDataSetChanged();
    }
}