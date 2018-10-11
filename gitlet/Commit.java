package gitlet;

import java.util.Date;
import java.util.HashMap;
/**
 * Commit class for Gitlet.
 *
 * @author Benny
 */
public class Commit extends MyObject {

    /** PARENT.*/
    private String parent;
    /** MERGEDPARENT.*/
    private String mergedparent;
    /** LOGMESSAGE.*/
    private String logmessage;
    /** TIMING.*/
    private Date timing;
    /** BLOBS.*/
    private HashMap<String, String> blobs;

    /** A COMMIT OBJECT WITH MESSAGES,
     * DATE, APARENT, AMERGEDPARENT AND ABLOBS.*/
    Commit(String messages, Date date, String aparent, String amergedparent,
           HashMap<String, String> ablobs) {
        if (messages == null || messages.isEmpty() || messages.equals("")) {
            throw new IllegalArgumentException(
                    "Please enter a commit message.");
        }
        this.parent = aparent;
        this.logmessage = messages;
        this.timing = date;
        this.blobs = ablobs;
        this.mergedparent = amergedparent;

    }

    /** A COMMIT OBJECT WITH MESSAGES, DATE, APARENT AND ABLOBS.*/
    Commit(String messages, Date date, String aparent,
           HashMap<String, String> ablobs) {
        this(messages, date, aparent, null, ablobs);
    }
    /** A COMMIT OBJECT WITH MESSAGE AND CURRENTDATE.*/
    Commit(String message, Date currentDate) {
        this(message, currentDate, null, new HashMap<String, String>());
    }

    /** @RETURN PARENT.*/
    public String parent() {
        return this.parent;
    }

    /** @RETURN TIMING.*/
    public Date date() {
        return this.timing;
    }

    /** @RETURN LOG_MESSAGE.*/
    public String log() {
        return logmessage;
    }

    /** @RETURN BLOBS.*/
    public HashMap<String, String> blobs() {
        return this.blobs;
    }

    /** @RETURN TRUE OR FALSE.*/
    public boolean merged() {
        return this.mergedparent != null;
    }

    /** @RETURN MERGEDPARENT.*/
    public String getMergedparent() {
        return this.mergedparent;
    }


}
