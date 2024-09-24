package sammyt.cloudplayer.data;

import org.json.JSONObject;

import java.util.ArrayList;

public class MediaQueue {

    private ArrayList<JSONObject> queue;
    private int position;

    private final ArrayList<Listener> listeners = new ArrayList<>();

    private MediaQueue() {}

    private static final class MediaQueueHolder {
        private static final MediaQueue instance = new MediaQueue();
    }

    public interface Listener {
        void onPositionChanged(int pos);
    }

    public static MediaQueue getInstance() {
        return MediaQueueHolder.instance;
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    public void setQueue(ArrayList<JSONObject> queue) {
        this.queue = queue;
    }

    public void setPosition(int position) {
        this.position = position;

        for(Listener listener: listeners) {
            listener.onPositionChanged(this.position);
        }
    }

    public void advancePosition() {
        int pos = (position < queue.size() - 1) ? position + 1 : 0;

        setPosition(pos);
    }

    public void decreasePosition() {
        int pos = (position != 0) ? position - 1 : queue.size() - 1;

        setPosition(pos);
    }

    public ArrayList<JSONObject> getQueue() {
        return queue;
    }

    public int getPosition() {
        return position;
    }
}
