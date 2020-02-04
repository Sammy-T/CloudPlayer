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
MIT License

Copyright (c) 2020 Sammy-T

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
