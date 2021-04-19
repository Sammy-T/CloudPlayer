package sammyt.cloudplayer;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.android.material.button.MaterialButton;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import sammyt.cloudplayer.data.CloudQueue;

public class LoginActivity extends AppCompatActivity {

    private static final String LOG_TAG = LoginActivity.class.getSimpleName();

    private String apiRoot;
    private String clientId;
    private String clientSecret;
    private String redirectUri;

    private SharedPreferences sharedPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        MaterialButton connectButton = findViewById(R.id.connect_button);
        MaterialButton loginButton = findViewById(R.id.login_button);
        final EditText authCodeEditText = findViewById(R.id.auth_code_edit_text);

        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connectAccount();
            }
        });

        authCodeEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if(actionId == EditorInfo.IME_ACTION_DONE) {
                    requestToken(authCodeEditText.getText().toString());
                }
                return false;
            }
        });

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestToken(authCodeEditText.getText().toString());
            }
        });

        // Set up the client info
        apiRoot = getString(R.string.api_root);
        clientId = getString(R.string.client_id);
        clientSecret = getString(R.string.client_secret);

        try {
            redirectUri = URLEncoder.encode(getString(R.string.redirect_uri), "UTF-8");
        } catch(UnsupportedEncodingException e) {
            Log.e(LOG_TAG, "Error encoding redirect uri", e);
        }

        sharedPrefs = this.getSharedPreferences(getString(R.string.pref_file_key), Context.MODE_PRIVATE);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Check for a saved token and navigate to the Nav Activity if one is found
        String token = sharedPrefs.getString(getString(R.string.token_key), "");
        if(!token.equals("")) {
            redirectToNavActivity();
        }
    }

    /**
     * Opens the '/connect' endpoint in the browser for the user to approve access to their account.
     * Upon approval, the endpoint sends an auth code to the redirect uri.
     */
    private void connectAccount() {
        if(redirectUri.equals("")) {
            Log.e(LOG_TAG, "No redirect uri");
            return;
        }

        String responseType = "code";
        String scope = "non-expiring";

        String endpoint = "/connect";
        String url = apiRoot + endpoint
                + "?client_id=" + clientId
                + "&redirect_uri=" + redirectUri
                + "&response_type=" + responseType
                + "&scope=" + scope;

        // Open the '/connect' endpoint in the browser
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        startActivity(intent);
    }

    /**
     * Uses the auth code to request an access token.
     * @param authCode The entered auth code.
     */
    private void requestToken(String authCode) {
        if(redirectUri.equals("")) {
            Log.e(LOG_TAG, "No redirect uri");
            return;
        }

        if(authCode.equals("")) {
            String msg = "No auth code entered";
            Log.w(LOG_TAG, msg);
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            return;
        }

        RequestQueue queue = CloudQueue.getInstance(this).getRequestQueue();

        String endpoint = "/oauth2/token";
        String url = apiRoot + endpoint;

        // Add the request parameters
        final HashMap<String, String> params = new HashMap<>();
        params.put("grant_type", "authorization_code");
        params.put("client_id", clientId);
        params.put("client_secret", clientSecret);
        params.put("redirect_uri", redirectUri);
        params.put("code", authCode);

        // Build the request body in application/x-www-form-urlencoded format
        final StringBuilder builder = new StringBuilder();
        for(Map.Entry<String, String> entry: params.entrySet()) {
            builder.append("&");
            builder.append(entry.getKey());
            builder.append("=");
            builder.append(entry.getValue());
        }

        Response.Listener<String> responseListener = new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    JSONObject responseObject = new JSONObject(response);
                    String token = responseObject.getString("access_token");

                    if(token.equals("")) {
                        String msg = "No token received";
                        Log.e(LOG_TAG, msg);
                        Toast.makeText(LoginActivity.this, msg, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Save the token and navigate to the Nav Activity
                    SharedPreferences.Editor editor = sharedPrefs.edit();
                    editor.putString(getString(R.string.token_key), token);
                    editor.apply();

                    redirectToNavActivity();

                } catch(JSONException e) {
                    Log.e(LOG_TAG, "Error parsing response", e);
                }
            }
        };

        Response.ErrorListener errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(LOG_TAG, "Volley error requesting token", error);
            }
        };

        StringRequest stringRequest = new StringRequest(
                Request.Method.POST,
                url,
                responseListener,
                errorListener) {
            @Override
            public byte[] getBody() throws AuthFailureError {
                // Include the body in the request
                return builder.toString().substring(1).getBytes(StandardCharsets.UTF_8);
            }
        };

        queue.add(stringRequest);
    }

    /**
     * Navigates to the Nav Activity.
     */
    private void redirectToNavActivity() {
        Intent intent = new Intent(this, NavActivity.class);
        startActivity(intent);
    }
}