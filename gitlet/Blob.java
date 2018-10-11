package gitlet;

import java.io.Serializable;

/**
 * Blob class for Gitlet, for blob object.
 *
 * @author Benny
 */
public class Blob extends MyObject implements Serializable {

    /**
     * The THINGS in the file.
     */
    private byte[] things;

    /**
     * A BLOB object with CON.
     */
    Blob(byte[] con) {
        this.things = con;
    }

    /**
     * CONTENT of the file in a blob, and @return things.
     */
    public byte[] content() {
        return this.things;
    }
}
