package gitlet;

import java.io.Serializable;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;

/** Represents a gitlet repository. Can only be one per directory.
 * Assume this Gitlet object is created in the CURRENT directory
 * when initialized.
 * @author Kevin Moy**/

@SuppressWarnings("ALL")
public class Gitlet implements Serializable {

    /** USAGE: java gitlet.Main init <p></p>
     * Gitlet constructor serves as gitlet INIT method.
     * We create this object (call this constructor) in the
     * current directory. All the initialization features coming
     * with creating a gitlet repository- the universal first
     * commit, setting up staging area, branch names, etc.
     * are handled. Repo existence is checked in the Main class.
     */
    public Gitlet() {
        Commit ufc = new Commit();
        createDirectories();
        serializeAndWrite(ufc);
        this._head = "master";
        this._currentbranch = "master";
        this._stagingArea = new HashMap<>();
        this._branches = new HashMap<>();
        this._untracked = new ArrayList<String>();
        _branches.put(_head, ufc.getID());
        this._numEdits = 0;
        this._unstagedMods = new HashMap<>();
    }

    /** USAGE: java gitlet.Main add [file name] <p></p>
     * Handling of the gitlet add function.
     * Stages the file.
     * If file already in staging area, overwrite it.
     * If file is identical to (current) commit file (hasn't been edited)
     * then there is no reason to have
     * it in the staging area (remove if it is).
     * @param filename filename to add.
     **/
    public void add(String filename) {
        File file = new File(filename);
        if (!file.exists()) {
            Utils.message("File does not exist.");
            throw new GitletException();
        }
        Commit head = acquireHeadCommit();
        HashMap<String, Blob> headFiles = head.getFiles();
        String newHashed = new Blob(filename).getID();

        File stagePATH = new File(".gitlet/stagingArea/" + newHashed);

        if (headFiles == null || !headFiles.containsKey(filename)
                || !headFiles.get(filename).getID().equals(newHashed)) {
            _stagingArea.put(filename, new Blob(filename));
            stage(stagePATH, file);
        } else {
            if (stagePATH.exists()) {
                _stagingArea.remove(filename);
            }
        }
        if (_untracked.contains(filename)) {
            _untracked.remove(filename);
        }
    }

    /** OFFICIALLY stage FILE into staging area specified by stagePATH.
     * @param stagePATH PATH of file in staging area.
     * @param file file to stage. **/
    private void stage(File stagePATH, File file) {
        Utils.writeContents(stagePATH, Utils.readContentsAsString(file));
    }

    /** USAGE: java gitlet.Main commit [message] <p></p>
     * Accumulate tracked files (staging area +
     * head commit files) into single snapshot.
     * ONLY staged and removed files updated in commit.
     * Remove command (rm) can exclude certain
     * files from the commit.
     * @param msg commit message
     **/
    public void commit(String msg) {
        if (msg.equals("")) {
            Utils.message("Please enter a commit message.");
            throw new GitletException("No Commit Message Entered");
        }
        if (!stagedFilesExist() && !untrackedFilesExist()) {
            Utils.message("No changes added to the commit.");
            throw new GitletException();
        }
        Commit head = acquireHeadCommit();
        HashMap<String, Blob> commitFiles = head.getFiles();
        if (commitFiles == null) {
            commitFiles = new HashMap<String, Blob>();
        }
        Set<String> trackedFilenames = _stagingArea.keySet();
        if (stagedFilesExist() || untrackedFilesExist()) {
            for (String filename : trackedFilenames) {
                commitFiles.put(filename, _stagingArea.get(filename));
            }
            for (String ignoredFile : _untracked) {
                commitFiles.remove(ignoredFile);
            }
        }
        String parent = head.getID();
        Commit newCommit = new Commit(msg, parent, commitFiles);
        simpleWrite(newCommit);
        _stagingArea.clear();
        _untracked.clear();
        _unstagedMods.clear();
        _branches.put(_head, newCommit.getID());
    }

    /**
     * Functionality for commit type 2: A merge commit.
     * MAY NEED TO CLONE FILES.
     * @param msg commit message
     * @param parentCommits parent commits.
     **/
    public void mergeCommit(String msg, String[] parentCommits) {
        if (msg.trim().equals("")) {
            Utils.message("Please enter a commit message.");
            throw new GitletException();
        }
        Commit head = acquireHeadCommit();
        HashMap<String, Blob> headFiles = head.getFiles();
        if (headFiles == null) {
            headFiles = new HashMap<String, Blob>();
        }
        if (stagedFilesExist() || untrackedFilesExist()) {
            Set<String> filesToStage = _stagingArea.keySet();
            for (String fileName : filesToStage) {
                headFiles.put(fileName, _stagingArea.get(fileName));
            }
            for (String fileName : _untracked) {
                headFiles.remove(fileName);
            }
        } else {
            Utils.message("No changes added to the commit.");
            throw new GitletException();
        }
        Commit newCommit = new Commit(msg, parentCommits, headFiles);
        String hashedMergeCommit = newCommit.getID();
        File newCommFile = new File(".gitlet/commitLog/" + hashedMergeCommit);
        Utils.writeObject(newCommFile, newCommit);
        _untracked.clear();
        _stagingArea.clear();
        _branches.put(_head, newCommit.getID());
    }

    /** USAGE: java gitlet.Main log <p></p>
     * Print log of commits, starting from HEAD to UFC.
     *
     **/
    public void log() {
        String commitPtr = _branches.get(_head);
        while (!commitPtr.equals("") && commitPtr != null) {
            Commit current = convertHashToCommit(commitPtr);
            print(current);
            if (current.getParents().length > 0) {
                commitPtr = current.getFirstParent();
            } else {
                commitPtr = current.getParent();
            }
        }
    }

    /** USAGE: java gitlet.Main global-log <p></p>
     * Print log of all commits ever made, in any order.
     **/
    public void globalLog() {
        File[] allCommitsEverMade = new File(".gitlet/commitLog").listFiles();
        String commitHash;
        for (File commitFile : allCommitsEverMade) {
            commitHash = commitFile.getName();
            print(convertHashToCommit(commitHash));
        }
    }

    /**
     * Print a commit.
     * @param commit Commit to print.
     **/
    public void print(Commit commit) {
        if (commit.getParents() != new String[0]
                && commit.getParents().length > 1) {
            System.out.println("===");
            System.out.println("commit " + commit.getID());
            String abbrevP1 = commit.getParents()[0].substring(0, 7);
            String abbrevP2 = commit.getParents()[1].substring(0, 7);
            System.out.println("Merge: " + abbrevP1 + " " + abbrevP2);
            System.out.println("Date: " + commit.getTimestamp());
            System.out.println(commit.getMsg());
            System.out.println();
        } else {
            System.out.println("===");
            System.out.println("commit " + commit.getID());
            System.out.println("Date: " + commit.getTimestamp());
            System.out.println(commit.getMsg());
            System.out.println();
        }
    }

    /** Essentially deserialization. <p></p>
     * Return Commit object based on unique SHA-1 hash ID.
     * Acquire file of commit from commit log, then cast back into commit.
     * @param hash commit UID
     **/
    private Commit convertHashToCommit(String hash) {
        File file = new File(".gitlet/commitLog/" + hash);
        File file2 = new File(".gitlet/commitLog/" + fullHash(hash));

        if (file.exists()) {
            return Utils.readObject(file, Commit.class);
        }

        if (file2.exists()) {
            return Utils.readObject(file2, Commit.class);
        }
        Utils.message("No commit with that id exists.");
        throw new GitletException();
    }

    /**
     * Checkout v1.
     * Get head commit's version of file, put in working directory.
     * DO NOT stage this file (place in untracked)
     * @param filename file name.
     **/
    public void checkoutFile(String filename) {
        String headHash = fullHash(_branches.get(_head));
        Commit head = convertHashToCommit(headHash);
        HashMap<String, Blob> headFiles = head.getFiles();
        if (headFiles.containsKey(filename)) {
            File file = new File(filename);
            File headVersionFile = new File(".gitlet/stagingArea/"
                    + headFiles.get(filename).getID());
            String desiredFile = Utils.readContentsAsString(headVersionFile);
            Utils.writeContents(file, desiredFile);
        } else {
            Utils.message("File does not exist in that commit.");
            throw new GitletException();
        }
    }

    /** USAGE: java gitlet.Main checkout [commit id] -- [file name] <p></p>
     * Checkout v2.
     * For commit specified by COMMITID, gets specified FILENAME
     * and put in working directory.
     * DO NOT stage this file (place in untracked)
     **/
    public void checkoutCommitFile(String commitID, String filename) {
        Commit commit = convertHashToCommit(commitID);
        HashMap<String, Blob> commitFiles = commit.getFiles();
        if (commitFiles.containsKey(filename)) {
            File file = new File(filename);
            File commitVersionFile = new File(".gitlet/stagingArea/"
                    + commitFiles.get(filename).getID());
            String desiredFile = Utils.readContentsAsString(commitVersionFile);
            Utils.writeContents(file, desiredFile);
        } else {
            Utils.message("File does not exist in that commit.");
            throw new GitletException();
        }
    }

    /** USAGE: java gitlet.Main checkout [branch name] <p></p>
     * Checkout v3.
     * Paste all files in branch head to working directory.
     * OVERWRITE files in working directory if needed.
     * Set current branch (head) to passed in branch.
     * @param branchname given branch name
     **/
    public void checkoutBranch(String branchname) {
        boolean branchExists = _branches.containsKey(branchname);
        if (!branchExists) {
            Utils.message("No such branch exists.");
            throw new GitletException();
        }
        if (branchname.equals(_head)) {
            Utils.message("No need to checkout the current branch.");
            throw new GitletException();
        }
        String branchHeadHash = _branches.get(branchname);
        Commit givenBH = getHC(branchname);
        HashMap<String, Blob> givenBHFiles = givenBH.getFiles();
        File workingDir = new File(System.getProperty("user.dir"));
        untrackedInWay(workingDir);
        File[] wdFiles = workingDir.listFiles();
        pasteFromBranch(givenBHFiles, wdFiles);
        if (givenBHFiles != null) {
            for (String filename : givenBHFiles.keySet()) {
                File f = new File(".gitlet/stagingArea/"
                        + givenBHFiles.get(filename).getID());
                String desiredFile = Utils.readContentsAsString(f);
                File file = new File(filename);
                Utils.writeContents(file, desiredFile);
            }
        }
        _stagingArea.clear();
        _untracked.clear();
        _head = branchname;
    }

    /**
     * Return entire hashcode of commit, or itself if already full.
     * @param commitHash shortened commit hash
     **/
    private String fullHash(String commitHash) {
        if (commitHash.length() == Utils.UID_LENGTH) {
            return commitHash;
        }
        File[] commitFiles = new File(".gitlet/commitLog").listFiles();
        for (File commitFile : commitFiles) {
            String fullName = commitFile.getName();
            if (fullName.contains(commitHash)) {
                return fullName;
            }
        }
        Utils.message("No commit with that id exists.");
        throw new GitletException();
    }

    /**USAGE: java gitlet.Main rm [file name]
     * Unstage file. Mark untracked (for next commit).
     * Remove file from working directory IF tracked -> untracked.
     * @param filename Filename in English.
     */
    public void rm(String filename) {
        File file = new File(filename);
        Commit head = acquireHeadCommit();
        HashMap<String, Blob> headFiles = head.getFiles();
        if (!file.exists() && !headFiles.containsKey(filename)) {
            Utils.message("File does not exist.");
            throw new GitletException();
        }
        boolean removedIndicator = false;
        if (headFiles != null && headFiles.containsKey(filename)) {
            _untracked.add(filename);
            File gone = new File(filename);
            Utils.restrictedDelete(gone);
            removedIndicator = true;
        }
        if (_stagingArea.containsKey(filename)) {
            _stagingArea.remove(filename);
            removedIndicator = true;
        }
        if (!removedIndicator) {
            Utils.message("No reason to remove the file.");
            throw new GitletException();
        }
    }

    /** Return head commit. */
    private Commit acquireHeadCommit() {
        String headHash = _branches.get(_head);
        Commit headCommit = convertHashToCommit(headHash);
        return headCommit;
    }

    /** Delete branch BRANCHNAME's POINTER- not associated commits.
     * @param branchname Branch name in English.
     */
    public void rmBranch(String branchname) {
        if (_head.equals(branchname)) {
            Utils.message("Cannot remove the current branch.");
            throw new GitletException();
        }
        if (_branches.containsKey(branchname)) {
            _branches.remove(branchname);
        } else {
            Utils.message("A branch with that name does not exist.");
            throw new GitletException();
        }
    }

    /** USAGE: java gitlet.Main find [commit message] <p></p>
     * Given the commit message, find ALL commits with that message and
     * print out each.
     * Print IDs ONLY (unlike log).
     * LIterally the same logic as rm except no deletion is done.
     * @param msg Filename in English.
     */
    public void find(String msg) {
        File[] allCommitFiles = new File(".gitlet/commitLog").listFiles();
        boolean foundFile = false;
        String fileHash;
        for (File file : allCommitFiles) {
            fileHash = file.getName();
            Commit commit = convertHashToCommit(fileHash);
            if (commit.getMsg().equals(msg)) {
                System.out.println(file.getName());
                foundFile = true;
            }
        }
        if (!foundFile) {
            Utils.message("Found no commit with that message.");
            throw new GitletException();
        }
    }

    /** USAGE: java gitlet.Main status
     * Display all branches (head starred).
     * Display all staged files.
     * Display all removed files (from next commit)
     * Display all MODIFIED BUT NOT STAGED files.
     * Display all UNTRACKED files.
     */
    public void status() {
        System.out.println("=== Branches ===");
        printAllBranches();
        System.out.println();
        System.out.println("=== Staged Files ===");
        printStagedFiles();
        System.out.println();
        System.out.println("=== Removed Files ===");
        printRemovedFiles();
        System.out.println();
        System.out.println("=== Modifications Not Staged For Commit ===");
        printUnstagedMods();
        System.out.println();
        System.out.println("=== Untracked Files ===");
        printUntrackedFiles();
        System.out.println();
    }

    /**EXTRA CREDIT. <p></p>
     *  Print all untracked files in the repo. **/
    private void printUntrackedFiles() {
    }
    /** EXTRA CREDIT. <p>
     * </p>
     * Print all modified but unstaged
     *  files in the repo.
     *  A file qualifies for this if it is: <p></p>
     *  Tracked in the current commit,
     *  changed in the working directory, but not staged <p></p>
     *  Staged for addition, but with different contents
     *  than in the working directory <p></p>
     *  Staged for addition, but deleted
     *  in the working directory <p></p>
     *  Not staged for removal, but tracked in the current
     *  commit and deleted from the working directory.
     *  **/
    private void printUnstagedMods() {
        if (!_unstagedMods.isEmpty()) {
            for (String um : _unstagedMods.keySet()) {
                System.out.println(um + "(" + _unstagedMods.get(um) + ")");
            }
        }
    }
    /** Print all removed files in the repo. **/
    private void printRemovedFiles() {
        String[] untrackedFiles =
                _untracked.toArray(new String[_untracked.size()]);
        Arrays.sort(_untracked.toArray());
        for (String removed : _untracked) {
            System.out.println(removed);
        }
    }

    /** Print all staged files in the repo. **/
    private void printStagedFiles() {
        String[] stagedFiles = stagingAreaFiles();
        Arrays.sort(stagedFiles);
        for (String staged : stagedFiles) {
            System.out.println(staged);
        }
    }

    /** Print all branches in the repo. **/
    private void printAllBranches() {
        String[] branchnames = allBranches();
        Arrays.sort(branchnames);
        for (String branch : branchnames) {
            if (_head.equals(branch)) {
                System.out.println("*" + branch);
            } else {
                System.out.println(branch);
            }
        }
    }

    /** Create branch with given name.
     * @param branchname given branch name**/
    public void branch(String branchname) {
        if (!_branches.containsKey(branchname)) {
            _branches.put(branchname, acquireHeadCommit().getID());
        } else {
            Utils.message("A branch with that name already exists.");
            throw new GitletException();
        }
    }

    /** USAGE: java gitlet.Main reset [commit id]
     * Acquire commit given Hash ID. Get full ID from abbreviation if needed.
     * REMOVE ALL tracked files not in that commit.
     * Move head pointer to given commit.
     * Clear staging area.
     * @param commitID commit UID
     * **/
    public void reset(String commitID) {
        Commit givenCommit = convertHashToCommit(fullHash(commitID));
        HashMap<String, Blob> givenFiles = givenCommit.getFiles();
        File workingDir = new File(System.getProperty("user.dir"));
        untrackedInWay(workingDir);
        File[] wdFiles = workingDir.listFiles();
        for (File trackedFile : wdFiles) {
            if (!givenFiles.containsKey(trackedFile.getName())) {
                Utils.restrictedDelete(trackedFile);
            }
        }
        for (String file : givenFiles.keySet()) {
            File resetFile = new File(".gitlet/stagingArea/"
                    + givenFiles.get(file).getID());
            String contents = Utils.readContentsAsString(resetFile);
            Utils.writeContents(new File(file), contents);
        }
        _branches.put(_head, fullHash(commitID));
        _stagingArea.clear();
    }

    /** USAGE: java gitlet.Main merge [branch name] <p></p>
     * Merge files from BRANCH into head.
     * Acquire split point commit.
     * Handle files according to files in split commit.
     * **/
    public void merge(String branch) {
        handleMergeFailures(branch);
        String scHash = getSPCommit(branch, _head);
        Commit splitCommit = convertHashToCommit(scHash);
        HashMap<String, Blob> splitFiles = splitCommit.getFiles();
        String givenBHHash = _branches.get(branch);
        Commit givenBH = getHC(branch);
        HashMap<String, Blob> givenBHFiles = givenBH.getFiles();
        Commit head = acquireHeadCommit();
        HashMap<String, Blob> headFiles = head.getFiles();

        mergeFileUpdates(branch);
        if (scHash.equals(givenBHHash)) {
            Utils.message("Given branch is an ancestor "
                    + "of the current branch.");
            return;
        }
        if (scHash.equals(head.getID())) {
            _branches.put(_head, givenBHHash);
            Utils.message("Current branch fast-forwarded.");
            return;
        }
        for (String fileName : givenBHFiles.keySet()) {
            boolean inSC = splitFiles.containsKey(fileName);
            boolean inHead = headFiles.containsKey(fileName);
            boolean stillThere = givenBHFiles.containsKey(fileName);
            if (!inSC) {
                if (!inHead && stillThere) {
                    checkoutCommitFile(_branches.get(branch),  fileName);
                    _stagingArea.put(fileName, givenBHFiles.get(fileName));
                } else if (editDifs(fileName, givenBHFiles,
                        headFiles)) {
                    File given = new File(".gitlet/stagingArea/"
                            + givenBHFiles.get(fileName).getID());
                    File curr = new File(".gitlet/stagingArea/"
                            + headFiles.get(fileName).getID());
                    String currentFC = Utils.readContentsAsString(curr);
                    String givenFC = Utils.readContentsAsString(given);
                    String cfcReplacement = formatConflict(currentFC,
                            givenFC);
                    Utils.writeContents(new File(fileName), cfcReplacement);
                    add(fileName);
                    Utils.message("Encountered a merge conflict.");
                }
            }
        }
        checkHead(branch);
        String[] parents = new String[] {acquireHeadCommit().getID(),
                _branches.get(branch)};
        mergeCommit("Merged " + branch + " into " + _head + ".", parents);
    }

    /** Check head files now (for merge)
     * @param branch given branch name.**/
    private void checkHead(String branch) {
        String scHash = getSPCommit(branch, _head);
        Commit splitCommit = convertHashToCommit(scHash);
        HashMap<String, Blob> splitFiles = splitCommit.getFiles();
        String givenBHHash = _branches.get(branch);
        Commit givenBH = getHC(branch);
        HashMap<String, Blob> givenBHFiles = givenBH.getFiles();
        Commit head = acquireHeadCommit();
        HashMap<String, Blob> headFiles = head.getFiles();
        for (String fileName : headFiles.keySet()) {
            boolean inSC = splitFiles.containsKey(fileName);
            boolean stillThere = headFiles.containsKey(fileName);
            boolean inGiven = givenBHFiles.containsKey(fileName);
            if (!inSC && !inGiven) {
                checkoutCommitFile(_branches.get(branch),  fileName);
                _stagingArea.put(fileName, givenBHFiles.get(fileName));
            }
            if (inSC && !inGiven) {
                if (!head.getID().equals(splitCommit.getID())) {
                    rm(fileName);
                } else {
                    //Merge Conflict
                }
            }
        }
    }
    /** Helper method that gets head commit of BRANCHNAME.
     * @param branchname given branch name
     * @return pointer to head commit of that branch.**/
    private Commit getHC(String branchname) {
        String hash = _branches.get(branchname);
        hash = fullHash(hash);
        Commit c = convertHashToCommit(hash);
        return c;
    }

    /** Helper method that handles failure cases of merge command.
     * @param branchname part of failure cases 2 and 3**/
    private void handleMergeFailures(String branchname) {
        if (stagedFilesExist() || untrackedFilesExist()) {
            Utils.message("You have uncommitted changes.");
            throw new GitletException();
        }
        if (!_branches.containsKey(branchname)) {
            Utils.message("A branch with that name does not exist.");
            throw new GitletException();
        }
        if (_head.equals(branchname)) {
            Utils.message("Cannot merge a branch with itself.");
            throw new GitletException();
        }
    }


    /**Given split commit, analyze how its files have
     * been changed in the given branch and the current branch
     * and take action accordingly.
     * @param givenBranchName given branch we want to merge with.
     * **/
    private void mergeFileUpdates(String givenBranchName) {
        Commit splitCommit =
                convertHashToCommit(getSPCommit(givenBranchName, _head));
        HashMap<String, Blob> splitFiles = splitCommit.getFiles();

        Commit head = acquireHeadCommit();
        HashMap<String, Blob> headFiles = head.getFiles();

        Commit givenBH = convertHashToCommit(_branches.get(givenBranchName));
        HashMap<String, Blob> givenBHFiles = givenBH.getFiles();

        File workingDir = new File(System.getProperty("user.dir"));
        untrackedInWay(workingDir);

        Set<String> scFileNames = splitFiles.keySet();
        for (String fileName : scFileNames) {
            boolean givenModded = editDifs(fileName,
                    splitFiles, givenBHFiles);
            boolean headModded = editDifs(fileName,
                    splitFiles, headFiles);
            if (!headModded && !givenBHFiles.containsKey(fileName)) {
                Utils.restrictedDelete(new File(fileName));
                rm(fileName);
                continue;
            }
            if (!headModded && givenModded) {
                checkoutCommitFile(_branches.get(givenBranchName), fileName);
                add(fileName);
            }
            if (headModded && givenModded) {
                if (editDifs(fileName, givenBHFiles, headFiles)) {
                    handleMergeConflict(givenBranchName, fileName);
                }
            }
        }
    }

    /** Handles a merge conflict at a given BRANCHNAME with File FILENAME.
     * We need 3 commits: Split point, current head, and given branch head.
     * @param branchName messed up branch (one we try to merge with).
     * @param fileName merge-conflicted file [name] **/
    private void handleMergeConflict(String branchName, String fileName) {
        String splitHash = getSPCommit(branchName, _head);
        Commit splitPoint = convertHashToCommit(splitHash);
        HashMap<String, Blob> splitFiles = splitPoint.getFiles();
        Commit head = acquireHeadCommit();
        HashMap<String, Blob> headFiles = head.getFiles();
        Commit givenBH = convertHashToCommit(_branches.get(branchName));
        HashMap<String, Blob> givenBHFiles = givenBH.getFiles();
        String currentBranchFileContents;
        String givenBHFileContents;
        if (headFiles.containsKey(fileName)) {
            File currentBranchFile = new File(".gitlet/stagingArea/"
                    + headFiles.get(fileName).getID());
            currentBranchFileContents =
                    Utils.readContentsAsString(currentBranchFile);
        } else {
            currentBranchFileContents = "";
        }
        if (givenBHFiles.containsKey(fileName)) {
            File givenBHFile = new File(".gitlet/stagingArea/"
                    + givenBHFiles.get(fileName).getID());
            givenBHFileContents =
                    Utils.readContentsAsString(givenBHFile);
        } else {
            givenBHFileContents = "";
        }
        String cfcReplacement = formatConflict(currentBranchFileContents,
                givenBHFileContents);
        Utils.writeContents(new File(fileName), cfcReplacement);
        add(fileName);
        Utils.message("Encountered a merge conflict.");
    }

    /** Helper method that returns merge-conflicted file replacement contents.
     * @param current contents of conflicted file, head version
     * @param given contents of conflicted file, given branch version**/
    private String formatConflict(String current, String given) {
        String contents = "<<<<<<< HEAD\n";
        contents += current;
        contents += "=======\n" + given;
        contents += ">>>>>>>\n";
        return contents;
    }

    /** HELPER method (for checkout and merge): error if there are
     * untracked files in the working directory and would be overwritten.
     * KEY: Compares working directory files to head files.
     * @param wdFile working directory file. **/
    private void untrackedInWay(File wdFile) {
        Commit head = acquireHeadCommit();
        HashMap<String, Blob> headFiles = head.getFiles();
        File[] workingDirFiles = wdFile.listFiles();
        for (File f : workingDirFiles) {
            if (headFiles == null) {
                if (workingDirFiles.length > 1) {
                    Utils.message("Therer is an untracked file in the way;"
                            + " delete it or add it first.");
                    throw new GitletException();
                }
            } else {
                String filename = f.getName();
                boolean notTracked = !headFiles.containsKey(filename);
                boolean notStaged = !_stagingArea.containsKey(filename);
                if (notTracked && notStaged && !filename.equals(".gitlet")) {
                    Utils.message("There is an untracked file in the way;"
                            + " delete it or add it first.");
                    throw new GitletException();
                }
            }
        }
    }

    /** Helper for merge to see if file F is in untracked and
     * in the way.
     * @param f File
     * @param branchname branchname
     * @return if untracked and an issue.
     */
    private boolean isUntrackedSimple(File f, String branchname) {
        boolean notStaged = !_stagingArea.containsKey(f);
        boolean headUntracked = acquireHeadCommit().getFiles().containsKey(f);
        boolean givenTracked = getHC(branchname).getFiles().containsKey(f);
        return notStaged && headUntracked && givenTracked;
    }

    /** Return true IF file FILENAME
     * has been edited in a branch
     * FILES1 and FILES2 (head).
     * @param filename name of file to check for merge conflicts.
     * @param files1 first set of files to check.
     * @param files2 second set of files (to check)
     * @return whether there's a editing difference.**/
    private boolean editDifs(String filename, HashMap<String, Blob> files1,
                             HashMap<String, Blob> files2) {
        if (files1.containsKey(filename) && files2.containsKey(filename)) {
            String hashF1 = files1.get(filename).getID();
            String hashF2 = files2.get(filename).getID();
            if (!hashF1.equals(hashF2)) {
                return true;
            }
        } else if (files1.containsKey(filename)
                || files2.containsKey(filename)) {
            return true;
        }
        return false;
    }

    /** Return split point commit (SHA-1 ID)
     * of BRANCH1 and BRANCH2.
     * @param branch1 first branch hash.
     * @param branch2 second branch hash.
     * @return Split point commit **/
    private String getSPCommit(String branch1, String branch2) {
        ArrayList<String> branch1Commits;
        ArrayList<String> branch2Commits;
        branch1Commits = getAllCommits(branch1);
        branch2Commits = getAllCommits(branch2);
        String common = findCommonCommit(branch1Commits, branch2Commits);
        return common;
    }

    /** Get all commits given BRANCHNAME, from head to UFC.
     *
     * @param branchname branch name
     * @return arraylist of all commits
     */
    private ArrayList<String> getAllCommits(String branchname) {
        ArrayList<String> allCommits = new ArrayList<String>();
        String commitPtr = _branches.get(branchname);
        while (!commitPtr.equals("") && commitPtr != null) {
            allCommits.add(commitPtr);
            commitPtr = convertHashToCommit(commitPtr).getFirstParent();
        }
        return allCommits;
    }

    /** Given two arraylists of commits IDs,
     * return the first common commit (hash ID)
     * among them.
     * @param branch1Commits list of all commits in first branch.
     * @param branch2Commits list of all commits in second branch.
     * @return common commit (SHA-1 ID).**/
    private String findCommonCommit(ArrayList<String> branch1Commits,
                                    ArrayList<String> branch2Commits) {
        for (String commit : branch1Commits) {
            if (branch2Commits.contains(commit)) {
                return commit;
            }
        }
        return "";
    }


    /****** HELPER FUNCTIONS **********/
    /** Self-explanatory.
     * @return if untracked files are
     * in current directory or not.**/
    private boolean untrackedFilesExist() {
        return !_untracked.isEmpty();
    }

    /** Return contents of filename, if it exists.
     * @param fileName file name
     * @return file contents as string.
     */
    private String contents(String fileName) {
        return Utils.readContentsAsString(new File(fileName));
    }

    /** Self-explanatory.
     * @return if files are
     * in staging area or not.
     */
    private boolean stagedFilesExist() {
        return _stagingArea.size() > 0;
    }

    /** Creates the .gitlet file and associated subdirectories. **/
    private void createDirectories() {
        File gitlet = new File(".gitlet");
        gitlet.mkdir();
        File commitLog = new File(".gitlet/commitLog");
        commitLog.mkdir();
        File stage = new File(".gitlet/stagingArea");
        stage.mkdir();
        File untracked = new File(".gitlet/untracked");
        untracked.mkdir();
        File merge = new File(".gitlet/merge");
        merge.mkdir();
    }

    /** Serializes Commit COMMIT and
     * writes to a UNIQUE filepath in gitlet/commitLog.**/
    private void serializeAndWrite(Commit commit) {
        File fp = new File(".gitlet/commitLog/" + fullHash(commit.getID()));
        byte[] serializedCommit = Utils.serialize(commit);
        Utils.writeContents(fp, serializedCommit);
    }

    /** Write commit object to commit log filepath WITHOUT serializing.
     * @param commit Commit to write. **/
    private void simpleWrite(Commit commit) {
        File fp = new File(".gitlet/commitLog/" + fullHash(commit.getID()));
        Utils.writeObject(fp, commit);
    }
    /** Helper method for pasting all files in commitFiles over to
     * working directory.
     * @param files given branch files
     * @param wdFiles working dir files**/
    private void pasteFromBranch(HashMap files, File[] wdFiles) {
        String filename;
        if (files == null) {
            nuke(wdFiles);
        }
        syncFilesWithBranch(files, wdFiles);
    }

    /** Helper method (for pasteFromBranch)
     * to delete all files in working directory.
     * @param wdFiles working directory files.
     */
    private void nuke(File[] wdFiles) {
        for (File gone : wdFiles) {
            Utils.restrictedDelete(gone);
        }
    }

    /**Returns the SHA-1 hashed form of File FILE.
     * In actuality this hashes FILE's contents ONLY.
     * In comparison, blob.hashMe hashes the file name
     * and byte-read file contents
     * for a few extra layers of "security".
     * @param file File to hash.
     * @return (UNIQUE) hash for FILE.**/
    public String hashThisFile(File file) {
        String blobb = Utils.readContentsAsString(file);
        return Utils.sha1(blobb);
    }

    /** Helper method (for pasteFromBranch)
     * to delete all files in working directory that are NOT
     * valid keys in FILES (our desired branch files).
     * @param files files
     * @param wdFiles working dir files
     */
    private void syncFilesWithBranch(HashMap files, File[] wdFiles) {
        String filename;
        for (File file : wdFiles) {
            filename = file.getName();
            if (!files.containsKey(filename) && !filename.equals(".gitlet")) {
                Utils.restrictedDelete(file);
            }
        }
    }

    /** Checks for identicality of files by content.
     * @param f1 file 1
     * @param f2 file2
     * @return whether file 1 is the same exact file as file 2**/
    private boolean filesEq(File f1, File f2) {
        String b1Hash = new Blob(f1.getName()).getID();
        String b2Hash = new Blob(f2.getName()).getID();
        return b1Hash.equals(b2Hash);
    }

    /** Return a list of staging area file keys.
     * @return stage keys array**/
    private String[] stagingAreaFiles() {
        return _stagingArea.keySet().toArray(new String[_stagingArea.size()]);
    }

    /** Return a list of all branch names.
     * @return branch names array**/
    private String[] allBranches() {
        return _branches.keySet().
                toArray(new String[_branches.size()]);
    }


    /**** END HELPER FUNCTIONS ****/

    /** Staging Area, which maps filenames (SHA-1 HASHES)
     * to corresponding blobs. **/
    private HashMap<String, Blob> _stagingArea;
    /** Head pointer of commit tree.
     * Head is actually a BRANCH name, "master" by default.**/
    private String _head;
    /** Represents branches in Gitlet.
     * Will map a branch's name to the appropriate head commit (hashcode).
     * Note we ONLY need to map to the head because we can
     * access all of its parents, and thus the entire branch,
     * from that head.**/
    private HashMap<String, String> _branches;
    /**current branch.**/
    private String _currentbranch;
    /** Files that user has removed with the rm command.**/
    private ArrayList<String> _removed;
    /** Files user has selected to not be added to next commit. */
    private ArrayList<String> _untracked;
    /** Files user has edited but
     * are still unstaged for whatever reason.*/
    private HashMap<String, String> _unstagedMods;
    /** Number of edits made (to head commit).
     * This attribute should only be needed
     * when REMOVE (rm) is called. **/
    private int _numEdits;
}
