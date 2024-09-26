# CloudPlayer
CloudPlayer is an Android media player app which streams content from SoundCloud. The app displays and allows playback of the logged-in user's publicly available liked tracks and playlists.

<img src="/images/Screenshot_20200203-214952.png" width="30%" height="30%" /> <img src="/images/Screenshot_20200203-215012.png" width="30%" height="30%" /> <img src="/images/Screenshot_20200203-215019.png" width="30%" height="30%" /> <img src="/images/Screenshot_20200203-215124.png" width="30%" height="30%" /> <img src="/images/Screenshot_20200203-224906.png" width="30%" height="30%" />

## Getting Started

If you're looking to build and run this project, you should take note of the following concerns:

> [!IMPORTANT]
> Battery optimization settings may need to be disabled for the app to function correctly. 
> 
> It seems that Doze Mode may kill the app regardless of having the correct permissions declared
> and using a MediaSessionService.
> 
> I don't know the correct way to handle this so this'll have to do for the time being.

### String Resources

| Name | Description |
| --- | --- |
| pref_file_key | The Shared Preferences file name. |
| token_key | The Shared Preferences key name to access the stored token. |
| refresh_token_key | The Shared Preferences key name to access the stored refresh token. |
| api_root | The root path of the API. (ex. `https://api.soundcloud.com`) |
| client_id | Your app's Client ID |
| client_secret | Your app's Client Secret |
| redirect_uri | Your app's Redirect URI |

> [!NOTE]
> Any custom values can be used for `pref_file_key`, `token_key`, and `refresh_token_key`.

> [!IMPORTANT]
> If you happen to have an app registered with SoundCloud already, you can insert your own values for Client ID, Client Secret, and Redirect URI.\
> ***However, the last I checked, SoundCloud isn't reviewing new app registrations.***
>
> The best I can recommend, if you don't have a Client ID and Secret available, would be to try contacting SoundCloud directly.

### Login and Authentication

1. Authentication is initiated via the Login activity's `Connect` button.
2. It opens the browser to SoundCloud's 'Connect' page for the user to approve access to their account.
3. After approval, the page redirects to the app's `Redirect URI`.
4. The value of the `code` url parameter must be copied into the Login activity's `Authorization code` edit text field to login.

## Dependencies

- [Volley](https://github.com/google/volley)
- [Picasso](https://github.com/square/picasso)
- [Nier Visualizer](https://github.com/bogerchan/Nier-Visualizer)
- [Sticky-LayoutManager](https://github.com/qiujayen/sticky-layoutmanager)
