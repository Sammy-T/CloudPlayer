# CloudPlayer
CloudPlayer is an Android media player app which streams content from SoundCloud. The app displays and allows playback of the logged-in user's publicly available liked tracks and playlists.

<img src="/images/Screenshot_20200203-214952.png" width="30%" height="30%" /> <img src="/images/Screenshot_20200203-215012.png" width="30%" height="30%" /> <img src="/images/Screenshot_20200203-215019.png" width="30%" height="30%" /> <img src="/images/Screenshot_20200203-215124.png" width="30%" height="30%" /> <img src="/images/Screenshot_20200203-224906.png" width="30%" height="30%" />

## Notice
If you're looking to build and run this project, you should take note of the following concerns:
- #### api_root, pref_file_key, token_key
(These are represented by String resources within the project. The api_root is the root path of the api `https://api.soundcloud.com`. The pref_file_key and token_key are the values representing the Shared Preferences file name and the key name to access the token. Both of these can be set to any custom value.)
- #### SoundCloud Client ID, Client Secret, and Redirect URI
(These are represented by String resources within the project. If you happen to have your own already, you can insert your own values. The last I checked, SoundCloud isn't reviewing new app registrations. The best I can recommend, if you don't have a Client ID and Secret available, would be to try contacting SoundCloud directly.)
- #### Login and Authentication
(Authentication is initiated via the Login activity's `Connect` button. It opens the browser to SoundCloud's Connect page for the user to approve access to their account. After approval, the page redirects to the app's `Redirect URI`. The url parameter `code` value must be copied into the Login activity's `Authorization code` edit text field to login.)

## Dependencies
- [Volley](https://github.com/google/volley)
- [Picasso](https://github.com/square/picasso)
- [Nier Visualizer](https://github.com/bogerchan/Nier-Visualizer)
- [Sticky-LayoutManager](https://github.com/qiujayen/sticky-layoutmanager)

## License
Copyright 2020 Samuel Telusma

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
