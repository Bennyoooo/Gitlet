package gitlet;

import java.util.ArrayList;
/**
 * Removed class for Gitlet.
 *
 * @author Benny
 */
public class Removed extends MyObject {

    /** CURRENTREMOVED.*/
    private ArrayList<String> currentremoved;

    /** A REMOVED OBJECT.*/
    Removed() {
        currentremoved = new ArrayList<>();

    }

    /** ADDREMOVE WITH NAME.*/
    public void addremove(String name) {
        currentremoved.add(name);
    }

    /** @RETURN CURRENTREMOVED.*/
    public ArrayList<String> getCurrentremoved() {
        return currentremoved;
    }

    /** @RETURN NAME.*/
    public void deleteremove(String name) {
        currentremoved.remove(name);
    }

    /** @RETURN TRUE OR FALSE.*/
    public boolean cancommit() {
        return currentremoved.size() > 0;
    }

    /** CLEAR.*/
    public void clearcurrent() {
        currentremoved.clear();
    }
}

