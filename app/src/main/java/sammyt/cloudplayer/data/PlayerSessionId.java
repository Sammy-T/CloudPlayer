package sammyt.cloudplayer.data;

/**
 * A helper to pass audio session id information from the service's player
 * to the frontend.
 */
public class PlayerSessionId {

    private int sessionId;

    private PlayerSessionId() {}

    private static final class PlayerSessionIdHolder {
        static final PlayerSessionId instance = new PlayerSessionId();
    }

    public static PlayerSessionId getInstance(){
        return PlayerSessionIdHolder.instance;
    }

    public void setSessionId(int sessionId) {
        this.sessionId = sessionId;
    }

    public int getSessionId() {
        return sessionId;
    }
}
