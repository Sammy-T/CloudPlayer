package sammyt.cloudplayer.ui.artists;

import android.util.Log;

import java.util.Comparator;

import de.voidplus.soundcloud.User;

public class ArtistObjectComparator implements Comparator<Object[]> {

    private final String LOG_TAG = this.getClass().getSimpleName();

    // Object[itemType, itemObject] : [String, String/User]

    // Compares the second value of each array
    @Override
    public int compare(Object[] objects1, Object[] objects2){
        Object object1Val = objects1[1];
        Object object2Val = objects2[1];

        String object1Str = tryStringCast(object1Val);
        String object2Str = tryStringCast(object2Val);

        User object1User = tryUserCast(object1Val);
        User object2User = tryUserCast(object2Val);

        if(object1User != null){
            if(object2User != null){
                // compare user, user
                return customCompare(object1User, object2User);

            }else if(object2Str != null){
                // compare user, string
                return customCompare(object1User, object2Str);
            }
        }else if(object1Str != null){
            if(object2User != null){
                // compare string, user
                return customCompare(object1Str, object2User);

            }else if(object2Str != null){
                // compare string, string
                return customCompare(object1Str, object2Str);
            }
        }

        // This shouldn't be reached so throw an exception
        throw new IllegalArgumentException("Each argument must be [String, String] or [String, User]");
    }

    private String tryStringCast(Object object){
        String casted = null;

        try{
            casted = (String) object;
        }catch(ClassCastException e){
//            Log.w(LOG_TAG, "Unable to cast to string.", e);
        }

        return casted;
    }

    private User tryUserCast(Object object){
        User casted = null;

        try{
            casted = (User) object;
        }catch(ClassCastException e){
//            Log.w(LOG_TAG, "Unable to cast to user.", e);
        }

        return casted;
    }

    // String, String
    private int customCompare(String string1, String string2){
        return string1.compareToIgnoreCase(string2);
    }

    // User, User
    private int customCompare(User user1, User user2){
        String userName1 = user1.getUsername();
        String userName2 = user2.getUsername();
        return userName1.compareToIgnoreCase(userName2);
    }

    // String, User
    private int customCompare(String string1, User user2){
        String userName2 = user2.getUsername();
        return string1.compareToIgnoreCase(userName2);
    }

    // User, String
    private int customCompare(User user1, String string2){
        String userName1 = user1.getUsername();
        return userName1.compareToIgnoreCase(string2);
    }
}
