package gitlet;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Date;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.Formatter;


/**
 * Commands class for Gitlet.
 *
 * @author Benny Jiang
 */
public class Gitlet {

    /**
     * STAGING.
     */
    private File staging = new File(".gitlet/staging");

    /**
     * BLOBS.
     */
    private File blobs = new File(".gitlet/blobs");

    /**
     * BRANCHES.
     */
    private File branches = new File(".gitlet/branches");

    /**
     * WORKINGDIR.
     */
    private File workingdir = new File(".");

    /**
     * INIT.
     */
    public void init() {
        Path path = Paths.get(".gitlet");
        if (Files.exists(path)) {
            throw new GitletException(
                    "A Gitlet version-control system already "
                            + "exists in the current directory.");
        }
        File gitletirect = new File(".gitlet");
        gitletirect.mkdir();

        File commitdirect = new File(".gitlet/commits");
        commitdirect.mkdir();

        staging = new File(".gitlet/staging");
        staging.mkdir();

        Removed removed = new Removed();
        File removedfile = new File(".gitlet/removed");
        Utils.writeObject(removedfile, removed);

        blobs = new File(".gitlet/blobs");
        blobs.mkdir();

        Date initialtime = new Date(0);
        Commit initial = new Commit("initial commit",
                initialtime);
        byte[] initialcommit = Utils.serialize(initial);
        String filename = Utils.sha1(initialcommit);

        Branch branchmap = new Branch();
        File branchess = new File(".gitlet/branches");

        branchmap.addbranch("master", filename);
        Utils.writeObject(branchess, branchmap);
        File outFile = new File(".gitlet/commits/" + filename);
        try {
            ObjectOutputStream outs =
                    new ObjectOutputStream(new FileOutputStream(outFile));
            outs.writeObject(initial);
            outs.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * ADD ARGS.
     */
    public void add(String[] args) {
        if (args.length != 2) {
            throw new GitletException("Incorrect operands.");
        }
        String filename = args[1];
        File infile = new File("./" + filename);
        if (!infile.exists()) {
            throw new GitletException("File does not exist.");
        }

        File removed = new File(".gitlet/removed");
        Removed removedfile = Utils.readObject(removed, Removed.class);
        removedfile.deleteremove(filename);
        Utils.writeObject(removed, removedfile);

        byte[] filecontent = Utils.readContents(infile);

        Commit currentcommit = currentcommit();
        HashMap<String, String> currentblobss = currentcommit.blobs();
        if (currentblobss.containsKey(filename)) {
            File blobfile = new File(".gitlet/blobs/"
                    + currentblobss.get(filename));
            Blob blob = Utils.readObject(blobfile, Blob.class);
            if (Arrays.equals(blob.content(), filecontent)) {
                return;
            }
        }
        File outfile = new File(".gitlet/staging/" + filename);
        Utils.writeContents(outfile, filecontent);
    }

    /**
     * COMMIT ARGS.
     */
    public void commit(String[] args) {
        if (args.length == 1) {
            throw new GitletException("Please enter a commit message.");
        } else if (args.length != 2) {
            throw new GitletException("Incorrect operands.");
        }
        commit(args[1], null);
    }

    /**
     * COMMIT with MESSAGE and SECONDPARENT.
     */
    private void commit(String message, String secondparent) {
        List<String> files = Utils.plainFilenamesIn(".gitlet/staging");
        File removed = new File(".gitlet/removed");
        Removed removedfile = Utils.readObject(removed, Removed.class);
        if (files.size() == 0 && !removedfile.cancommit()) {
            throw new GitletException("No changes added to the commit.");
        }
        File branchess = new File(".gitlet/branches");
        Branch branch = Utils.readObject(branchess, Branch.class);

        String parentname = branch.getCurrentcommit();
        HashMap<String, String> blobss = new HashMap<>();
        if (parentname != null) {
            File parentfile = new File(".gitlet/commits/" + parentname);
            Commit parent = Utils.readObject(parentfile, Commit.class);
            HashMap<String, String> postblobs = parent.blobs();
            Set<String> postfilenames = postblobs.keySet();
            for (String name : postfilenames) {
                blobss.put(name, postblobs.get(name));
            }
        }
        ArrayList<String> currentremove = removedfile.getCurrentremoved();

        for (String file : files) {
            File blobing = new File(".gitlet/staging/" + file);
            byte[] content = Utils.readContents(blobing);
            Blob blob = new Blob(content);
            byte[] blobcontent = Utils.serialize(blob);
            String blobname = Utils.sha1(blobcontent);

            File outFile = new File(".gitlet/blobs/" + blobname);
            blobing.delete();
            Utils.writeObject(outFile, blob);
            blobss.put(file, blobname);
        }
        if (currentremove.size() > 0) {
            for (String rm : currentremove) {
                blobss.remove(rm);
            }
        }
        removedfile.clearcurrent();
        Utils.writeObject(removed, removedfile);

        Date currenttime = new Date(System.currentTimeMillis());

        Commit now = new Commit(message, currenttime,
                parentname, secondparent, blobss);
        byte[] nowcommit = Utils.serialize(now);
        String commitfile = Utils.sha1(nowcommit);

        branch.addbranch(branch.getCurrent(), commitfile);
        Utils.writeObject(branchess, branch);

        File outFile2 = new File(".gitlet/commits/" + commitfile);
        Utils.writeObject(outFile2, now);
    }

    /**
     * LOG.
     */
    public void log() {
        Branch branch = currentbranch();
        String commitname = branch.getCurrentcommit();

        while (commitname != null) {
            File commitfile = new File(".gitlet/commits/" + commitname);
            Commit commit = Utils.readObject(commitfile, Commit.class);
            Formatter outs = new Formatter();
            outs.format("===%n");
            Date time = commit.date();
            SimpleDateFormat dateFormat =
                    new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z");
            outs.format("commit %s%n", commitname);
            if (commit.merged()) {
                String mergedparent = commit.getMergedparent().substring(0, 7);
                String parent = commit.parent().substring(0, 7);
                outs.format("Merge: %s %s%n", parent, mergedparent);
            }
            outs.format("Date: %s%n%s%n", dateFormat.format(time),
                    commit.log());
            System.out.println(outs);
            commitname = commit.parent();
        }
    }

    /**
     * GLOBALLOG.
     */
    public void globallog() {
        File commits = new File(".gitlet/commits");
        List<String> commitnames = Utils.plainFilenamesIn(commits);
        for (String commitname : commitnames) {
            File commitfile = new File(".gitlet/commits/" + commitname);
            Commit commit = Utils.readObject(commitfile, Commit.class);
            Formatter outs = new Formatter();
            outs.format("===%n");
            Date time = commit.date();
            SimpleDateFormat dateFormat =
                    new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z");
            outs.format("commit %s%n", commitname, dateFormat.format(time),
                    commit.log());
            if (commit.merged()) {
                String mergedparent = commit.getMergedparent().substring(0, 7);
                String parent = commit.parent().substring(0, 7);
                outs.format("Merge: %s %s%n", parent, mergedparent);
            }
            outs.format("Date: %s%n%s%n",
                    dateFormat.format(time), commit.log());
            System.out.println(outs);
        }
    }

    /**
     * FIND ARGS.
     */
    public void find(String[] args) {
        if (args.length != 2) {
            throw new GitletException("Incorrect operands.");
        }
        String message = args[1];
        File commits = new File(".gitlet/commits");
        List<String> commitnames = Utils.plainFilenamesIn(commits);
        boolean error = true;
        if (commitnames != null) {
            for (String commitname : commitnames) {
                File commitfile = new File(".gitlet/commits/" + commitname);
                Commit commit = Utils.readObject(commitfile, Commit.class);
                if (commit.log().equals(message)) {
                    error = false;
                    System.out.println(commitname);
                }
            }
        }
        if (error) {
            throw new GitletException("Found no commit with that message.");
        }
    }

    /**
     * CHECKOUT ARGS.
     */
    public void checkout(String[] args) {
        String filename;
        String commitname;
        if (args.length == 3) {
            if (!args[1].equals("--")) {
                throw new GitletException("Incorrect operands.");
            }
            filename = args[2];
            Branch branch = currentbranch();
            commitname = branch.getCurrentcommit();
            checkout(filename, commitname);

        } else if (args.length == 4) {
            if (!args[2].equals("--")) {
                throw new GitletException("Incorrect operands.");
            }
            filename = args[3];
            commitname = args[1];
            checkout(filename, commitname);
        } else if (args.length == 2) {
            checkoutbranch(args[1], false);
        } else {
            throw new GitletException("Incorrect operands.");
        }
    }

    /** UNTRACKEDMESSAGE.*/
    private String untrackedmessage =
            "There is an untracked file in the way; delete it or add it first.";

    /** CHECKOUTMESSAGE.*/
    private String checkoutmessage = " No need to checkout the current branch.";

    /** CHECKOUTBRANCH with BRANCHNAME and IFRESET.*/
    private void checkoutbranch(String branchname, boolean ifreset) {
        Branch branch = Utils.readObject(branches, Branch.class);
        Set<String> branchnames = branch.getBranches().keySet();
        Commit current = currentcommit();
        String currentcommitname = branch.getCurrentcommit();
        if (!ifreset) {
            if (!branchnames.contains(branchname)) {
                throw new GitletException("No such branch exists.");
            }
            if (branch.getCurrent().equals(branchname)) {
                throw new GitletException(checkoutmessage);
            }
        } else {
            branch.reset();
        }
        String checkingcommitname = branch.getcommit(branchname);
        if (!currentcommitname.equals(checkingcommitname)) {
            File commitfile = new File(".gitlet/commits/" + checkingcommitname);
            Commit commit = Utils.readObject(commitfile, Commit.class);
            HashMap<String, String> blobss = commit.blobs();
            Set<String> filenames = blobss.keySet();
            HashMap<String, String> currentblobss = current.blobs();
            Set<String> currentfilenamess = currentblobss.keySet();
            workingfiles = workingdir.listFiles();
            for (File each : workingfiles) {
                if (!each.isDirectory()) {
                    String name = each.getName();
                    if (!currentfilenamess.contains(name)) {
                        if (filenames.contains(name)) {
                            System.out.println(untrackedmessage);
                            return;
                        }
                    }
                }
            }
            for (File each : workingfiles) {
                if (!each.isDirectory()) {
                    if (!filenames.contains(each.getName())) {
                        Utils.restrictedDelete(each);
                    }
                }
            }
            for (String filename : filenames) {
                String blobname = blobss.get(filename);
                File inFile = new File(".gitlet/blobs/" + blobname);
                Blob blob = Utils.readObject(inFile, Blob.class);
                byte[] filecontent = blob.content();
                File checkouting = new File("./" + filename);
                Utils.writeContents(checkouting, filecontent);
            }
        }
        File[] files = staging.listFiles();
        if (files != null) {
            for (int index = 0; index < files.length; index++) {
                files[index].delete();
            }
        }
        branch.changecurrent(branchname);
        Utils.writeObject(branches, branch);
    }

    /**
     * CHECKOUT with FILENAME and COMMITNAME.
     */
    private void checkout(String filename, String commitname) {
        if (commitname.length() < Utils.getUidLength()) {
            File commits = new File(".gitlet/commits");
            List<String> fullnames = Utils.plainFilenamesIn(commits);
            int nums = commitname.length();
            for (String name : fullnames) {
                if (name.substring(0, nums).equals(commitname)) {
                    commitname = name;
                    break;
                }
            }
            if (commitname.length() < Utils.getUidLength()) {
                throw new GitletException("No commit with that id exists.");
            }
        }
        File commitfile = new File(".gitlet/commits/" + commitname);
        if (!commitfile.exists()) {
            throw new GitletException("No commit with that id exists.");
        }
        Commit commit = Utils.readObject(commitfile, Commit.class);

        HashMap<String, String> blobss = commit.blobs();

        if (!blobss.containsKey(filename)) {
            throw new GitletException("File does not exist in that commit.");
        }
        String blobname = blobss.get(filename);
        Blob blob;
        File inFile = new File(".gitlet/blobs/" + blobname);
        blob = Utils.readObject(inFile, Blob.class);
        byte[] filecontent = blob.content();
        File checkouting = new File("./" + filename);
        Utils.writeContents(checkouting, filecontent);
    }

    /** RM ARGS.*/
    public void rm(String[] args) {
        String filename = args[1];
        remove(filename);
    }

    /** Remove FILENAME.*/
    private void remove(String filename) {

        File stagings = new File(".gitlet/staging");
        List<String> filenames = Utils.plainFilenamesIn(stagings);
        boolean removed = false;
        if (filenames != null) {
            if (filenames.contains(filename)) {
                removed = true;
            }
        }
        Commit commit = currentcommit();
        Set<String> files = commit.blobs().keySet();
        boolean removed2 = false;
        if (files.size() > 0) {
            removed2 = files.contains(filename);
        }
        if (removed2 || removed) {
            if (removed2) {
                File rvfile = new File(".gitlet/removed");
                Removed removedfile = Utils.readObject(rvfile, Removed.class);
                removedfile.addremove(filename);
                Utils.writeObject(rvfile, removedfile);

                File removefile = new File("./" + filename);
                Utils.restrictedDelete(removefile);
            }
            if (removed) {
                File filetoremove = new File(".gitlet/staging/" + filename);
                boolean here = filetoremove.delete();
            }
        } else {
            throw new GitletException("No reason to remove the file.");
        }
    }

    /** @return CURRENTCOMMIT.*/
    private Commit currentcommit() {
        Branch branch = currentbranch();
        String currentnames = branch.getCurrentcommit();
        File commitfile = new File(".gitlet/commits/" + currentnames);
        Commit commit = Utils.readObject(commitfile, Commit.class);
        return commit;
    }

    /** @return CURRENTBRANCH.*/
    private Branch currentbranch() {
        File branchess = new File(".gitlet/branches");
        Branch branch = Utils.readObject(branchess, Branch.class);
        return branch;
    }

    /** @return CURRENTREMOVED.*/
    private Removed currentremoved() {
        File removed = new File(".gitlet/removed");
        Removed removedfile = Utils.readObject(removed, Removed.class);
        return removedfile;
    }

    /** OUT.*/
    private Formatter out;

    /** WORKINGFILES.*/
    private File[] workingfiles;

    /** STATUS.*/
    public void status() {
        out = new Formatter();
        Branch branch = currentbranch();
        String current = branch.getCurrent();
        out.format("=== Branches ===%n");
        out.format("*%s%n", current);
        for (String branchname : branch.getBranches().keySet()) {
            if (!branchname.equals(current)) {
                out.format("%s%n", branchname);
            }
        }
        out.format("%n=== Staged Files ===%n");
        List<String> stagefilenames = Utils.plainFilenamesIn(staging);
        if (stagefilenames != null) {
            for (String filename : stagefilenames) {
                out.format("%s%n", filename);
            }
        }
        out.format("%n=== Removed Files ===%n");
        ArrayList<String> allremovedremove
                = currentremoved().getCurrentremoved();
        if (allremovedremove != null) {
            for (String name : allremovedremove) {
                out.format("%s%n", name);
            }
        }
        workingfiles = workingdir.listFiles();
        Commit currentcommit = currentcommit();
        currentfilenames = currentcommit.blobs().keySet();
        out.format("%n=== Modifications Not Staged For Commit ===%n");
        getmodified(currentcommit, stagefilenames);
        List<String> workfilenames = Utils.plainFilenamesIn(workingdir);
        if (stagefilenames != null) {
            for (String filename : stagefilenames) {
                if (workfilenames == null
                        || (!workfilenames.contains(filename))) {
                    out.format("%s (deleted)%n", filename);
                }
            }
        }
        for (String filename : currentfilenames) {
            if (workfilenames == null || (!workfilenames.contains(filename))) {
                if (!allremovedremove.contains(filename)) {
                    out.format("%s (deleted)%n", filename);
                }
            }
        }
        out.format("%n=== Untracked Files ===%n");
        getuntracked(stagefilenames, allremovedremove);
        System.out.println(out);
    }

    /** GETUNTRACKEDFILES WITH STAGEFILENAMES AND ALLREMOVEDREMOVE.*/
    private void getuntracked(List<String> stagefilenames,
                              ArrayList<String> allremovedremove
                              ) {
        if (workingfiles != null) {
            for (File file : workingfiles) {
                if (!file.isDirectory()) {
                    String name = file.getName();
                    if (!currentfilenames.contains(name)
                            && (stagefilenames == null
                            || !stagefilenames.contains(name))) {
                        out.format("%s%n", name);
                    }
                    if (allremovedremove != null
                            && allremovedremove.contains(name)) {
                        out.format("%s%n", name);
                    }
                }
            }
        }
    }

    /** GETMODIFIED WITH CURRENTFILENAMES, CURRENTCOMMIT AND  STAGEFILENAMES.*/
    private void getmodified(Commit currentcommit,
                             List<String> stagefilenames) {
        workingfiles = workingdir.listFiles();
        if (workingfiles != null) {
            for (File each : workingfiles) {
                if (!each.isDirectory()) {
                    String name = each.getName();
                    byte[] workingcontent = Utils.readContents(each);
                    if (currentfilenames.contains(name)) {
                        String blobname = currentcommit.blobs().get(name);
                        Blob blob = getblob(blobname);
                        byte[] commmitcontent = blob.content();
                        if (!Arrays.equals(commmitcontent, workingcontent)) {
                            if (stagefilenames == null
                                    || !stagefilenames.contains(name)) {
                                out.format("%s (modified)%n", name);
                            }
                        }
                    }
                    if (stagefilenames != null
                            && stagefilenames.contains(name)) {
                        File thisfile = new File(".gitlet/staging/" + name);
                        byte[] thisfilecontent = Utils.readContents(thisfile);
                        if (!Arrays.equals(thisfilecontent, workingcontent)) {
                            out.format("%s (modified)%n", name);
                        }
                    }
                }
            }
        }

    }

    /** GETBLOB WITH BLOBNAME.
     * @return blob.*/
    private Blob getblob(String blobname) {
        File blobfile = new File(".gitlet/blobs/" + blobname);
        Blob blob = Utils.readObject(blobfile, Blob.class);
        return blob;
    }

    /** ARGS.*/
    public void branch(String[] args) {
        String branchname = args[1];
        File branchess = new File(".gitlet/branches");
        Branch branch = Utils.readObject(branchess, Branch.class);

        Set<String> branchnames = branch.getBranches().keySet();
        if (branchnames.contains(branchname)) {
            throw new GitletException("A branch with "
                    + "that name already exists.");
        }
        String current = branch.getCurrentcommit();
        branch.putbranch(branchname, current);
        Utils.writeObject(branchess, branch);
    }

    /** ARGS.*/
    public void rmbranch(String[] args) {
        if (args.length != 2) {
            throw new GitletException("Incorrect operands.");
        }
        String branchname = args[1];
        Branch branch = Utils.readObject(branches, Branch.class);
        Set<String> branchnames = branch.getBranches().keySet();
        if (!branchnames.contains(branchname)) {
            throw new GitletException("A branch with "
                    + "that name does not exist.");
        }
        if (branch.getCurrent().equals(branchname)) {
            throw new GitletException("Cannot remove the current branch.");
        }
        branch.removebranch(branchname);
        Utils.writeObject(branches, branch);
    }

    /** ARGS.*/
    public void reset(String[] args) {
        if (args.length != 2) {
            throw new GitletException("Incorrect operands.");
        }
        String commitname = args[1];
        File commits = new File(".gitlet/commits");
        List<String> commitnames = Utils.plainFilenamesIn(commits);
        if (commitnames == null || !commitnames.contains(commitname)) {
            throw new GitletException("No commit with that id exists.");
        }
        Branch branch = Utils.readObject(branches, Branch.class);
        String currentbranch = branch.getCurrent();
        branch.setReset(commitname);
        Utils.writeObject(branches, branch);
        checkoutbranch(currentbranch, true);
    }

    /** ARGS.*/
    public void merge(String[] args) {
        if (args.length != 2) {
            throw new GitletException("Incorrect operands.");
        }
        String givenname = args[1];
        Branch branch = Utils.readObject(branches, Branch.class);
        if (!branch.getBranches().containsKey(givenname)) {
            throw new GitletException(" A branch "
                    + "with that name does not exist.");
        }
        String currentbranch = branch.getCurrent();
        if (givenname.equals(currentbranch)) {
            throw new GitletException("Cannot merge a branch with itself.");
        }
        merge(givenname);
    }

    /** CURRENTBLOBS.*/
    private HashMap<String, String> currentblobs;
    /** FORCHECK.*/
    private Commit forcheck;
    /** SPLITNAME.*/
    private String splitname = null;
    /** CHECKBRANCH.*/
    private Branch checkbranch;

    /** GIVENNAME.*/
    private void merge(String givenname) {
        checkbranch = Utils.readObject(branches, Branch.class);
        String currentbranch = checkbranch.getCurrent();
        currentname = checkbranch.getCurrentcommit();
        givencommitname = checkbranch.getcommit(givenname);
        File commitfile = new File(".gitlet/commits/" + currentname);
        Commit currentcommit = Utils.readObject(commitfile, Commit.class);
        File givenfile = new File(".gitlet/commits/" + givencommitname);
        Commit givencommit = Utils.readObject(givenfile, Commit.class);
        allcommits = new ArrayList<>();
        currentcheckname = currentname;
        forcheck = currentcommit;

        givenblobs = givencommit.blobs();
        givenfilenames = givenblobs.keySet();
        currentblobs = currentcommit.blobs();
        currentfilenames = currentblobs.keySet();
        List<String> stagedfiles = Utils.plainFilenamesIn(staging);
        if (stagedfiles != null && stagedfiles.size() != 0) {
            throw new GitletException("You have uncommitted changes.");
        }
        Removed remove = currentremoved();
        if (remove.getCurrentremoved().size() > 0) {
            throw new GitletException("You have uncommitted changes.");
        }
        if (specialone()) {
            return;
        }
        forcheck = givencommit;
        if (specialthree()) {
            return;
        }
        if (specialtwo()) {
            return;
        }
        File splitfile = new File(".gitlet/commits/" + splitname);
        Commit splitcommit = Utils.readObject(splitfile, Commit.class);
        splitblos = splitcommit.blobs();
        boolean conflict1 = false;
        conflict1 = checkgiven();
        boolean conflict2 = checkcurrent();
        commit("Merged " + givenname + " into "
                + currentbranch + ".", givencommitname);
        if (conflict1 || conflict2) {
            System.out.println("Encountered a merge conflict.");
        }
    }

    /** CURRENTCHECKNAME.*/
    private String currentcheckname;
    /** GIVENCOMMITNAME.*/
    private String givencommitname;
    /** ALLCOMMITS.*/
    private ArrayList<String> allcommits;

    /** SPECIALONE.
     * @RETURN TRUE OR FALSE.*/
    private boolean specialone() {
        while (forcheck.parent() != null) {
            if (currentcheckname.equals(givencommitname)) {
                System.out.println("Given branch is "
                        + "an ancestor of the current branch.");
                return true;
            }
            allcommits.add(currentcheckname);
            String parent = forcheck.parent();
            File parentfile = new File(".gitlet/commits/" + parent);
            forcheck = Utils.readObject(parentfile, Commit.class);
            currentcheckname = parent;
        }
        if (currentcheckname.equals(givencommitname)) {
            System.out.println("Given branch is "
                    + "an ancestor of the current branch.");
            return true;
        }
        return false;
    }

    /** CURRENTNAME.*/
    private String currentname;

    /** SPECIALTHREE.
     * @RETURN TRUE OR FALSE.*/
    private boolean specialthree() {
        while (forcheck.parent() != null) {
            String checkparent = forcheck.parent();
            if (allcommits.contains(checkparent)) {
                if (checkparent.equals(currentname)) {
                    checkbranch.changecurrent(givencommitname);
                    System.out.println("Current branch fast-forwarded.");
                    return true;
                } else {
                    splitname = checkparent;
                }
                break;
            }
            File parentfile = new File(".gitlet/commits/" + checkparent);
            forcheck = Utils.readObject(parentfile, Commit.class);
        }
        return false;
    }

    /** SPECIALTWO.
     * @return true or false.*/
    private boolean specialtwo() {
        workingfiles = workingdir.listFiles();
        if (workingfiles != null) {
            for (File each : workingfiles) {
                if (!each.isDirectory()) {
                    String name = each.getName();
                    if (!currentfilenames.contains(name)) {
                        if (givenfilenames.contains(name)) {
                            System.out.println(untrackedmessage);
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /** GIVENFILENAMES.*/
    private Set<String> givenfilenames;

    /** CHECKGIVEN.
     * @RETURN TRUE OR FALSE.*/
    private boolean checkgiven() {
        boolean conflict = false;
        for (String filename : givenfilenames) {
            String givenblobcont = givenblobs.get(filename);
            String splitblobcont = splitblos.get(filename);
            String currentblobcont = currentblobs.get(filename);
            if (!givenblobcont.equals(currentblobcont)) {
                if (!givenblobcont.equals(splitblobcont)) {
                    File blobfile = new File(".gitlet/blobs/" + givenblobcont);
                    Blob blob = Utils.readObject(blobfile, Blob.class);
                    byte[] filecontent = blob.content();
                    if ((currentblobcont != null
                            && currentblobcont.equals(splitblobcont))
                            || (currentblobcont == null
                            && splitblobcont == null)) {
                        File stagings = new File(".gitlet/staging/" + filename);
                        Utils.writeContents(stagings, filecontent);
                        File checkouting = new File("./" + filename);
                        Utils.writeContents(checkouting, filecontent);

                    } else {
                        File mergefile = new File("./" + filename);
                        File conflictstaging = new File(".gitlet/staging/"
                                + filename);
                        conflict = true;
                        if (currentblobs.containsKey(filename)) {
                            File currentblobfile = new File(".gitlet/blobs/"
                                    + currentblobcont);
                            Blob currentblob = Utils.readObject(currentblobfile,
                                    Blob.class);
                            byte[] currentcontent = currentblob.content();
                            Utils.writeContents(mergefile, "<<<<<<< HEAD\n",
                                    currentcontent, "=======\n",
                                    filecontent, ">>>>>>>\n");
                            Utils.writeContents(conflictstaging,
                                    "<<<<<<< HEAD\n",
                                    currentcontent, "=======\n",
                                    filecontent, ">>>>>>>\n");
                        } else {
                            Utils.writeContents(mergefile, "<<<<<<< HEAD\n",
                                    "=======\n", filecontent, ">>>>>>>\n");
                            Utils.writeContents(conflictstaging,
                                    "<<<<<<< HEAD\n", "=======\n",
                                    filecontent, ">>>>>>>\n");
                        }
                    }
                }
            }
        }
        return conflict;
    }

    /** CURRENTFILENAMES.*/
    private Set<String> currentfilenames;
    /** SPLITBLOBS.*/
    private HashMap<String, String> splitblos;
    /** GIVENBLOBS.*/
    private HashMap<String, String> givenblobs;

    /** CHECKCURRENT.
     * @RETURN TRUE OR FALSE.*/
    private boolean checkcurrent() {
        boolean conflict = false;
        for (String filename : currentfilenames) {
            String splitblobcont = splitblos.get(filename);
            if (!givenblobs.containsKey(filename)) {
                if (currentblobs.get(filename).equals(splitblobcont)) {
                    remove(filename);
                    File working = new File("./" + filename);
                    if (working.exists()) {
                        Utils.restrictedDelete(working);
                    }
                } else if (splitblobcont != null) {
                    conflict = true;
                    File conflictstaging = new File(".gitlet/staging/"
                            + filename);
                    File mergefile = new File("./" + filename);
                    String currentblobcont = currentblobs.get(filename);
                    File currentblobfile = new File(".gitlet/blobs/"
                            + currentblobcont);
                    Blob currentblob = Utils.readObject(currentblobfile,
                            Blob.class);
                    byte[] currentcontent = currentblob.content();
                    Utils.writeContents(mergefile,
                            "<<<<<<< HEAD\n", currentcontent,
                            "=======\n", ">>>>>>>\n");
                    Utils.writeContents(conflictstaging,
                            "<<<<<<< HEAD\n", currentcontent,
                            "=======\n", ">>>>>>>\n");
                }
            }
        }
        return conflict;
    }
}
