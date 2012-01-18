
package net.micode.fileexplorer;

public class FavoriteItem {
    // id in the database
    public long id;

    public String title;

    // path
    public String location;
    
    public FileInfo fileInfo;
    
    public FavoriteItem(String t, String l) {
        title = t;
        location = l;
    }

    public FavoriteItem(long i, String t, String l) {
        id = i;
        title = t;
        location = l;
    }
}
