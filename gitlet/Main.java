package gitlet;

import java.io.File;
import java.util.Arrays;

/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author Kevin Moy
 */
public class Main {
    /** Number of valid commands.**/
    private static int numValidCmds = 13;
    /** Array of valid commands. **/
    private static final String[] VALID_COMMANDS = {"init", "add",
        "commit", "rm", "log", "global-log",
        "find", "status", "checkout", "branch", "rm-branch", "reset",
        "merge"};

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    public static void main(String... args) {
        try {
            if (args.length == 0) {
                Utils.message("Please enter a command.");
                throw new GitletException();
            }
            String cmd = args[0];
            String[] cmdParams = Arrays.copyOfRange(args, 1, args.length);
            if (Arrays.asList(VALID_COMMANDS).contains(cmd)) {
                if (!repoExists()) {
                    if (cmd.equals("init")) {
                        repo = new Gitlet();
                        File repoInDisk = new File(".gitlet/repo");
                        Utils.writeObject(repoInDisk, repo);
                    } else {
                        Utils.message("Not in an "
                                + "initialized Gitlet directory.");
                        throw new GitletException();
                    }
                } else {
                    File overwrittenRepo = new File(".gitlet/repo");
                    repo = lastRepoState();
                    handleCommand(cmd, cmdParams);
                    Utils.writeObject(overwrittenRepo, repo);
                }
            } else {
                Utils.message("No command with that name exists.");
                throw new GitletException();
            }
        } catch (GitletException e) {
            System.exit(0);
        }
    }

    /** Execute command (with associated list of operands/flags)
     *  in the gitlet repo.
     *  @param cmd command
     *  @param ops operations**/
    private static void handleCommand(String cmd, String[] ops) {
        String filename;
        String msg;
        String branchname;
        String commitId;
        switch (cmd) {
        case "init":
            Utils.message("A Gitlet version-control "
                    + "system already exists "
                    + "in the current directory");
            throw new GitletException();
        case "add":
            filename = ops[0];
            repo.add(filename);
            break;
        case "commit":
            msg = ops[0];
            repo.commit(msg);
            break;
        case "rm":
            filename = ops[0];
            repo.rm(filename);
            break;
        case "log":
            repo.log();
            break;
        case "global-log":
            repo.globalLog();
            break;
        case "find":
            msg = ops[0];
            repo.find(msg);
            break;
        case "status":
            repo.status();
            break;
        default:
            handleCommand2(cmd, ops);
        }
    }

    /** Literally for the sole stupid purpose of keeping method
     * lengths below 60 lines.
     * @param cmd cmd
     * @param ops ops
     */
    private static void handleCommand2(String cmd, String[] ops) {
        String filename;
        String msg;
        String branchname;
        String commitId;
        switch (cmd) {
        case "checkout":
            if (ops.length == 1) {
                branchname = ops[0];
                repo.checkoutBranch(branchname);
            } else if (ops.length == 2 && ops[0].equals("--")) {
                filename = ops[1];
                repo.checkoutFile(filename);
            } else if (ops.length == 3 && ops[1].equals("--")) {
                commitId = ops[0];
                filename = ops[2];
                repo.checkoutCommitFile(commitId, filename);
            } else {
                Utils.message("Incorrect operands");
                throw new GitletException();
            }
            break;
        case "branch":
            branchname = ops[0];
            repo.branch(branchname);
            break;
        case "rm-branch":
            branchname = ops[0];
            repo.rmBranch(branchname);
            break;
        case "reset":
            commitId = ops[0];
            repo.reset(commitId);
            break;
        case "merge":
            branchname = ops[0];
            repo.merge(branchname);
            break;
        default:
            Utils.message("Invalid GITLET command,"
                    + "OR invalid command arguments.");
        }
    }

    /** Return true iff gitlet repo exists FOR THE CURRENT WORKING DIRECTORY.
     * I.e. return true if working directory has ".gitlet", repo initialized.
     * @return if we already made a repo*/
    private static boolean repoExists() {
        String currentDirectory = System.getProperty("user.dir");
        File checker = new File(currentDirectory + "/.gitlet");
        return checker.exists();
    }

    /** Returns the existing Gitlet repo
     * (call before a new gitlet command).
     * @return our repo.**/
    public static Gitlet lastRepoState() {
        File repoPATH =  new File(".gitlet/repo");
        return Utils.readObject(repoPATH, Gitlet.class);
    }

    /** Ultimate repo object. */
    private static Gitlet repo;

}
