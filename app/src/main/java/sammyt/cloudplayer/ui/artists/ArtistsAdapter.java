package sammyt.cloudplayer.ui.artists;

import android.os.Handler;
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

import java.util.ArrayList;
import java.util.TreeSet;

import de.voidplus.soundcloud.Track;
import de.voidplus.soundcloud.User;
import sammyt.cloudplayer.R;

public class ArtistsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final String LOG_TAG = this.getClass().getSimpleName();

    private ArrayList<Object[]> mItems = new ArrayList<>(); // [itemType, itemObject]

    private OnArtistClickListener mListener;

    private static final int HEADER = 0;
    private static final int ITEM = 1;

    public static class ArtistViewHolder extends RecyclerView.ViewHolder{
        LinearLayout artistItem;
        ImageView artistImage;
        TextView artistName;

        public ArtistViewHolder(View view){
            super(view);
            artistItem = view.findViewById(R.id.artist_item);
            artistImage = view.findViewById(R.id.artist_item_image);
            artistName = view.findViewById(R.id.artist_item_name);
        }
    }

    public static class HeaderViewHolder extends RecyclerView.ViewHolder{
        TextView headerTitle;

        public HeaderViewHolder(View view){
            super(view);
            headerTitle = view.findViewById(R.id.artist_header_item_title);
        }
    }

    public ArtistsAdapter(ArrayList<Track> tracks){
        if(tracks != null){
            buildItems(tracks);
        }
    }

    public interface OnArtistClickListener{
        void onArtistClick(int position, User artist);
    }

    public void setOnArtistClickListener(OnArtistClickListener l){
        mListener = l;
    }

    @Override
    public int getItemViewType(int position){
        return (int) mItems.get(position)[0];
    }

    // Create new views (invoked by Layout Manager)
    @Override
    @NonNull
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
        // set the view's size, margins, paddings and layout parameters here if needed

        int layout = R.layout.artist_item;

        if(viewType == HEADER){
            layout = R.layout.artist_header_item;
        }

        View view = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);

        if(viewType == HEADER){
            return new HeaderViewHolder(view);
        }else{
            return new ArtistViewHolder(view);
        }
    }

    // Replace contents of view (invoked by Layout Manager)
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position){
        if(holder.getItemViewType() == HEADER){
            String header = (String) mItems.get(position)[1];
            ((HeaderViewHolder) holder).headerTitle.setText(header);

        }else{
            final ArtistViewHolder artistHolder = (ArtistViewHolder) holder;
            final User artist = (User) mItems.get(position)[1];

            String artistName = artist.getUsername();
            String artistImage = artist.getAvatarUrl();

            artistHolder.artistName.setText(artistName);

            if(artistImage != null){
                // Request measuring of the item's view so we have some dimensions to use
                holder.itemView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                int width = holder.itemView.getMeasuredHeight();
                int height = holder.itemView.getMeasuredHeight();

                // Load the track's image
                Picasso.get()
                        .load(artistImage)
                        .resize(width, height)
                        .onlyScaleDown()
                        .centerCrop()
                        .error(android.R.drawable.stat_notify_error)
                        .into(artistHolder.artistImage);
            }

            artistHolder.artistItem.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(mListener != null){
                        mListener.onArtistClick(artistHolder.getAdapterPosition(), artist);
                    }
                }
            });
        }
    }

    // Return the size of the dataset (invoked by Layout Manager)
    @Override
    public int getItemCount(){
        return mItems.size();
    }

    public void updateArtistTracks(final ArrayList<Track> tracks){
        final Handler handler = new Handler();
        if(tracks != null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    buildItems(tracks);

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            notifyDataSetChanged();
                        }
                    });
                }
            }).start();
        }
    }

    private void buildItems(ArrayList<Track> tracks){
        TreeSet<Object[]> temp = new TreeSet<>(new ArtistObjectComparator());

        for(Track track: tracks){
            Object[] tempArtist = {ITEM, track.getUser()};
            temp.add(tempArtist);

            String alpha = Character.toString(track.getUser().getUsername().charAt(0)).toUpperCase();
            Object[] tempHeader = {HEADER, alpha};
            temp.add(tempHeader);
        }

        for(Object[] val: temp){
            Log.d(LOG_TAG, val[0] + " - " + val[1]);
        }

        mItems.addAll(temp);
    }
}
