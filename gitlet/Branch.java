package gitlet;

import java.util.HashMap;

/**
 * An object class for Gitlet.
 *
 * @author Benny
 */
public class Branch extends MyObject {

    /** BRANCHES.*/
    private HashMap<String, String> branches;

    /** CURRENT.*/
    private String current;

    /** RESET.*/
    private String reset;

    /** A BRANCH OBJECT.*/
    Branch() {
        branches = new HashMap<>();
        current = null;
        reset = null;
    }

    /** @RETURN BRANCHES.*/
    public HashMap<String, String> getBranches() {
        return this.branches;
    }

    /** ADDBRANCH WITH HEAD AND COMMIT.*/
    public void addbranch(String head, String commit) {
        this.branches.put(head, commit);
        current = head;
    }

    /** PUTBRANCH WITH HEAD AND COMMIT.*/
    public void putbranch(String head, String commit) {
        this.branches.put(head, commit);
    }

    /** REMOVEBRANCH WITH HEAD.*/
    public void removebranch(String head) {
        this.branches.remove(head);
    }

    /** @RETURN HEAD.*/
    public String getcommit(String head) {
        return this.branches.get(head);
    }

    /** GETCURRENTCOMMIT.
     * @RETURN CURRENT.*/
    public String getCurrentcommit() {
        return this.branches.get(current);
    }

    /** GETCURRENT.
     * @RETURN CURRENT.*/
    public String getCurrent() {
        return this.current;
    }

    /** CHANGECURRENT WITH CURR.*/
    public void changecurrent(String curr) {
        this.current = curr;
    }

    /** SETRESET WITH BRANCHNAME.*/
    public void setReset(String branchname) {
        this.reset = branchname;
    }

    /** RESET.*/
    public void reset() {
        this.branches.put(current, reset);
        reset = null;
    }
}
