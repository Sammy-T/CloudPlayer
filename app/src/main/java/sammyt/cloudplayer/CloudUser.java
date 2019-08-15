package sammyt.cloudplayer;

import androidx.annotation.NonNull;

import de.voidplus.soundcloud.User;

public class CloudUser { //// TODO: DELETE

    private final String LOG_TAG = this.getClass().getSimpleName();

    private String avatarUrl;
    private String city;
    private String country;
    private String description;
    private String discogsName;
    private String firstName;
    private int followersCount;
    private int followingsCount;
    private String fullName;
    private int id;
    private String kind;
    private String lastModified;
    private String lastName;
    private String myspaceName;
    private boolean online;
    private String permalink;
    private String permaLinkUrl;
    private String plan;
    private int playlistCount;
    private int publicFavoritesCount;
    private int repostsCount;
    //// TODO: Subscriptions?
    private int trackCount;
    private String uri;
    private String username;
    private String website;
    private String websiteTitle;
    
    public CloudUser(User user){
        avatarUrl = user.getAvatarUrl();
        city = user.getCity();
        country = user.getCountry();
        description = user.getDescription();
        discogsName = user.getDiscogsName();
        firstName = ""; //// TODO: Add these?
        if(user.getFollowersCount() != null) {
            followersCount = user.getFollowersCount();
        }
        if(user.getFollowingsCount() != null) {
            followingsCount = user.getFollowingsCount();
        }
        fullName = user.getFullName();
        id = user.getId();
        kind = user.getKind();
        lastModified = ""; //// TODO: Add these?
        lastName = ""; //// TODO: Add these?
        myspaceName = user.getMyspaceName();
        if(user.isOnline() != null) {
            online = user.isOnline();
        }
        permalink = user.getPermalink();
        permaLinkUrl = user.getPermalinkUrl();
        plan = user.getPlan();
        if(user.getPlaylistCount() != null) {
            playlistCount = user.getPlaylistCount();
        }
        if(user.getPublicFavoritesCount() != null) {
            publicFavoritesCount = user.getPublicFavoritesCount();
        }
        repostsCount = 0; //// TODO: Add these?
        if(user.getTrackCount() != null) {
            trackCount = user.getTrackCount();
        }
        uri = user.getUri();
        username = user.getUsername();
        website = user.getWebsite();
        websiteTitle = user.getWebsiteTitle();
    }

    public String getAvatarUrl(){
        return avatarUrl;
    }
    
    public String getCity() {
        return city;
    }
    
    public String getCountry() {
        return country;
    }
    
    public String getDescription() {
        return description;
    }
    
    public String getDiscogsName() {
        return discogsName;
    }
    
    public String getFirstName() {
        return firstName;
    }
    
    public int getFollowersCount() {
        return followersCount;
    }
    
    public int getFollowingsCount() {
        return followingsCount;
    }
    
    public String getFullName() {
        return fullName;
    }
    
    public int getId() {
        return id;
    }
    
    public String getKind(){
        return kind;
    }

    public String getLastModified(){
        return lastModified;
    }

    public String getLastName(){
        return lastName;
    }

    public String getMyspaceName(){
        return myspaceName;
    }

    public Boolean getOnline(){
        return online;
    }

    public String getPermalink(){
        return permalink;
    }

    public String getPermaLinkUrl(){
        return permaLinkUrl;
    }

    public String getPlan(){
        return plan;
    }

    public int getPlaylistCount(){
        return playlistCount;
    }

    public int getPublicFavoritesCount(){
        return publicFavoritesCount;
    }

    public int getRepostsCount(){
        return repostsCount;
    }

    public int getTrackCount(){
        return trackCount;
    }

    public String getUri(){
        return uri;
    }

    public String getUsername(){
        return username;
    }

    public String getWebsite(){
        return website;
    }

    public String getWebsiteTitle(){
        return websiteTitle;
    }
    
    @NonNull
    @Override
    public String toString(){
        String summary = "[";
        
        summary += "avatar url: " + avatarUrl + ", ";
        summary += "city: " + city + ", ";
        summary += "country: " + country + ", ";
        summary += "description: " + description + ", ";
        summary += "discogs name: " + discogsName + ", ";
        summary += "first name: " + firstName + ", ";
        summary += "followers count: " + followersCount + ", ";
        summary += "followings count: " + followingsCount + ", ";
        summary += "full name: " + fullName + ", ";
        summary += "id: " + id + ", ";
        summary += "kind: " + kind + ", ";
        summary += "last modified: " + lastModified + ", ";
        summary += "last name: " + lastName + ", ";
        summary += "myspace name: " + myspaceName + ", ";
        summary += "online: " + online + ", ";
        summary += "permalink: " + permalink + ", ";
        summary += "permalink url: " + permaLinkUrl + ", ";
        summary += "plan: " + plan + ", ";
        summary += "playlist count: " + playlistCount + ", ";
        summary += "public favorites count: " + publicFavoritesCount + ", ";
        summary += "resposts count: " + repostsCount + ", ";
        summary += "track count: " + trackCount + ", ";
        summary += "uri: " + uri + ", ";
        summary += "username: " + username + ", ";
        summary += "website: " + website + ", ";
        summary += "website title: " + websiteTitle + "]";
        
        return summary;
    }
}