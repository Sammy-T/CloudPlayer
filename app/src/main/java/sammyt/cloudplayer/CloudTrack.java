package sammyt.cloudplayer;

import androidx.annotation.NonNull;

import de.voidplus.soundcloud.Track;

public class CloudTrack { //// TODO: DELETE

    private final String LOG_TAG = this.getClass().getSimpleName();

    private String artworkUrl;
    private String attachmentsUri;
    private String bpm;
    private boolean commentable;
    private int commentCount;
    private String createdAt;
    private String description;
    private boolean downloadable;
    private int downloadCount;
    private String downloadUrl;
    private int duration;
    private String embeddableBy;
    private int favoritingsCount;
    private String genre;
    private int id;
    private String isrc;
    private String keySignature;
    private String kind;
    private int labelId;
    private String labelName;
    private String lastModified;
    private String license;
    private int originalContentSize;
    private String originalFormat;
    private String permalink;
    private String permalinkUrl;
    private int playbackCount;
    private String purchaseTitle;
    private String purchaseUrl;
    private String release;
    private int releaseDay;
    private int releaseMonth;
    private int releaseYear;
    private int repostsCount;
    private String sharing;
    private String state;
    private boolean streamable;
    private String streamUrl;
    private String tagList;
    private String title;
    private String trackType;
    private String uri;
    private CloudUser user;
    private int userId;
    private String videoUrl;
    private String waveformUrl;

    public CloudTrack(Track track){
        artworkUrl = track.getArtworkUrl();
        attachmentsUri = track.getAttachmentsUri();
        bpm = track.getBpm();
        commentable = track.isCommentable();
        if(track.getCommentCount() != null) {
            commentCount = track.getCommentCount();
        }
        createdAt = track.getCreatedAt();
        description = track.getDescription();
        downloadable = track.isDownloadable();
        if(track.getDownloadCount() != null) {
            downloadCount = track.getDownloadCount();
        }
        downloadUrl = track.getDownloadUrl();
        duration = track.getDuration();
        embeddableBy = ""; //// TODO: Add these?
        if(track.getFavoritingsCount() != null) {
            favoritingsCount = track.getFavoritingsCount();
        }
        genre = track.getGenre();
        id = track.getId();
        isrc = track.getIsrc();
        keySignature = track.getKeySignature();
        kind = ""; //// TODO: Add these?
        if(track.getLabelId() != null) {
            labelId = track.getLabelId();
        }
        labelName = track.getLabelName();
        lastModified = ""; //// TODO: Add these?
        license = track.getLicense();
        originalContentSize = track.getOriginalContentSize();
        originalFormat = track.getOriginalFormat();
        permalink = track.getPermalink();
        permalinkUrl = track.getPermalinkUrl();
        if(track.getPlaybackCount() != null) {
            playbackCount = track.getPlaybackCount();
        }
        purchaseTitle = ""; //// TODO: Add these?
        purchaseUrl = track.getPurchaseUrl();
        release = track.getRelease();
        if(track.getReleaseDay() != null && track.getReleaseMonth() != null && track.getReleaseYear() != null){
            releaseDay = track.getReleaseDay();
            releaseMonth = track.getReleaseMonth();
            releaseYear = track.getReleaseYear();
        }
        repostsCount = 0; //// TODO: Add these?
        sharing = track.getSharing();
        state = track.getState();
        if(track.isStreamable() != null) {
            streamable = track.isStreamable();
        }
        if(track.getStreamUrl() != null) {
            streamUrl = track.getStreamUrl();
        }
        tagList = track.getTagList();
        title = track.getTitle();
        trackType = track.getTrackType();
        uri = track.getUri();
//        user = new CloudUser(track.getUser());
        userId = track.getUserId();
        videoUrl = track.getVideoUrl();
        waveformUrl = track.getWaveformUrl();
    }

    public String getArtworkUrl(){
        return artworkUrl;
    }
    
    public String getAttachmentsUri(){
        return attachmentsUri;
    }
    
    public String getBpm(){
        return bpm;
    }
    
    public boolean getCommentable(){
        return commentable;
    }
    
    public int getCommentCount(){
        return commentCount;
    }
    
    public String getCreatedAt(){
        return createdAt;
    }
    
    public String getDescription(){
        return description;
    }
    
    public boolean getDownloadable(){
        return downloadable;
    }
    
    public int getDownloadCount(){
        return downloadCount;
    }
    
    public String getDownloadUrl(){
        return downloadUrl;
    }
    
    public int getDuration(){
        return duration;
    }
    
    public String getEmbeddableBy(){
        return embeddableBy;
    }
    
    public int getFavoritingsCount(){
        return favoritingsCount;
    }
    
    public String getGenre(){
        return genre;
    }
    
    public int getId(){
        return id;
    }
    
    public String getIsrc(){
        return isrc;
    }
    
    public String getKeySignature(){
        return keySignature;
    }
    
    public String getKind(){
        return kind;
    }
    
    public int getLabelId(){
        return labelId;
    }
    
    public String getLabelName(){
        return labelName;
    }
    
    public String getLastModified(){
        return lastModified;
    }
    
    public String getLicense(){
        return license;
    }
    
    public int getOriginalContentSize(){
        return originalContentSize;
    }
    
    public String getOriginalFormat(){
        return originalFormat;
    }
    
    public String getPermalink(){
        return permalink;
    }
    
    public String getPermalinkUrl(){
        return permalinkUrl;
    }
    
    public int getPlaybackCount(){
        return playbackCount;
    }
    
    public String getPurchaseTitle(){
        return purchaseTitle;
    }
    
    public String getPurchaseUrl(){
        return purchaseUrl;
    }
    
    public String getRelease(){
        return release;
    }
    
    public int getReleaseDay(){
        return releaseDay;
    }
    
    public int getReleaseMonth(){
        return releaseMonth;
    }
    
    public int getReleaseYear(){
        return releaseYear;
    }
    
    public int getRepostsCount(){
        return repostsCount;
    }
    
    public String getSharing(){
        return sharing;
    }
    
    public String getState(){
        return state;
    }
    
    public boolean getStreamable(){
        return streamable;
    }
    
    public String getStreamUrl(){
        return streamUrl;
    }
    
    public String getTagList(){
        return tagList;
    }
    
    public String getTitle(){
        return title;
    }
    
    public String getTrackType(){
        return trackType;
    }
    
    public String getUri(){
        return uri;
    }
    
    public CloudUser getUser(){
        return user;
    }
    
    public int getUserId(){
        return userId;
    }
    
    public String getVideoUrl(){
        return videoUrl;
    }
    
    public String getWaveformUrl(){
        return waveformUrl;
    }
    
    @NonNull
    @Override
    public String toString(){
        String summary = "[";

        summary += "artwork url: " + artworkUrl;
        summary += "attachments uri: " + attachmentsUri;
        summary += "bpm: " + bpm;
        summary += "commentable: " + commentable;
        summary += "comment count: " + commentCount;
        summary += "created at: " + createdAt;
        summary += "description: " + description;
        summary += "downloadable: " + downloadable;
        summary += "downloadable count: " + downloadCount;
        summary += "download url: " + downloadUrl;
        summary += "duration: " + duration;
        summary += "embeddable by: " + embeddableBy;
        summary += "favoritings count: " + favoritingsCount;
        summary += "genre: " + genre;
        summary += "id: " + id;
        summary += "isrc: " + isrc;
        summary += "key signature: " + keySignature;
        summary += "kind: " + kind;
        summary += "label id: " + labelId;
        summary += "label name: " + labelName;
        summary += "last modified: " + lastModified;
        summary += "license: " + license;
        summary += "original content size: " + originalContentSize;
        summary += "original format: " + originalFormat;
        summary += "permalink: " + permalink;
        summary += "permalink url: " + permalinkUrl;
        summary += "playback count: " + playbackCount;
        summary += "purchase title: " + purchaseTitle;
        summary += "purchase url: " + purchaseUrl;
        summary += "release: " + release;
        summary += "release day: " + releaseDay;
        summary += "release month: " + releaseMonth;
        summary += "release year: " + releaseYear;
        summary += "reposts count: " + repostsCount;
        summary += "sharing: " + sharing;
        summary += "state: " + state;
        summary += "streamable: " + streamable;
        summary += "stream url: " + streamUrl;
        summary += "tag list: " + tagList;
        summary += "title: " + title;
        summary += "track type: " + trackType;
        summary += "uri: " + uri;
        summary += "video url: " + videoUrl;
        summary += "waveform url: " + waveformUrl;
        summary += "\nuser id: " + userId;
        summary += "user: " + user + "]";
        
        return summary;
    }
}
