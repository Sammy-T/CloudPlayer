package sammyt.cloudplayer.player;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import android.os.Bundle;

import sammyt.cloudplayer.R;

public class PlayerActivity extends AppCompatActivity {

    private static final String LOG_TAG = PlayerActivity.class.getSimpleName();

    private static final String PLAYER_FRAGMENT = "PLAYER_FRAGMENT";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        if(savedInstanceState == null){
            FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.beginTransaction()
                    .replace(R.id.content_container, new PlayerFragment(), PLAYER_FRAGMENT)
                    .commit();
        }
    }
}
