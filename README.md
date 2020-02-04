# CloudPlayer
CloudPlayer is an Android media player app which streams content from SoundCloud. The app displays and allows playback of the logged-in user's publicly available liked tracks and playlists.

## Notice
If you're looking to build and run this project, you should take note of the following concerns:
- SoundCloud Client ID and Client Secret
<br/>(These are represented by Strings within the project. If you happen to have your own already, you can insert your own values. The last I checked, SoundCloud isn't reviewing new app registrations. The best I can recommend, if you don't have a Client ID and Secret available, would be to try contacting SoundCloud directly.)
- Login and Authentication
<br/>(There is no Login activity in this project. There are Strings representing the Username and Password of the SoundCloud account you want to login with. You can insert your own values or implement your own Login activity. You may also need to provide a Redirect Uri for the user to allow the app access to their SoundCloud account.)

## Dependencies
- [SoundCloud Java Library](https://github.com/nok/soundcloud-java-library)
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
