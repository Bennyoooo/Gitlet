package gitlet;

import java.io.File;

/**
 * Driver class for Gitlet, the tiny stupid version-control system.
 *
 * @author Benny
 */
public class Main {


    /**
     * Usage: java gitlet.Main ARGS, where ARGS contains
     * <COMMAND> <OPERAND> ....
     */
    public static void main(String... args) {
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            System.exit(0);
        }
        String command = args[0];
        Gitlet working = new Gitlet();
        try {
            if (command.equals("init")) {
                working.init();
            } else {
                if (command.equals("add")) {
                    cangoon();
                    working.add(args);
                } else if (command.equals("commit")) {
                    cangoon();
                    working.commit(args);
                } else if (command.equals("checkout")) {
                    cangoon();
                    working.checkout(args);
                } else if (command.equals("log")) {
                    cangoon();
                    working.log();
                } else if (command.equals("global-log")) {
                    cangoon();
                    working.globallog();
                } else if (command.equals("status")) {
                    cangoon();
                    working.status();
                } else if (command.equals("find")) {
                    cangoon();
                    working.find(args);
                } else if (command.equals("rm")) {
                    cangoon();
                    working.rm(args);
                } else if (command.equals("branch")) {
                    cangoon();
                    working.branch(args);
                } else if (command.equals("rm-branch")) {
                    cangoon();
                    working.rmbranch(args);
                } else if (command.equals("reset")) {
                    cangoon();
                    working.reset(args);
                } else if (command.equals("merge")) {
                    cangoon();
                    working.merge(args);
                } else {
                    throw new GitletException("No command "
                            + "with that name exists.");
                }
            }
            System.exit(0);
        } catch (GitletException | IllegalArgumentException e) {
            System.out.println(e.getMessage());
            System.exit(0);
        }
    }

    /** CHECK IF IT CAN GO ON.*/
    private static void cangoon() {
        File initial = new File(".gitlet");
        if (!initial.exists() || (!initial.isDirectory())) {
            throw new GitletException("Not in an "
                    + "initialized Gitlet directory.");
        }
    }

}
