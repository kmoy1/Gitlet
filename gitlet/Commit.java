package gitlet;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.HashMap;


/** A Git Commit object.
 * @author Kevin Moy**/

public class Commit implements Serializable {

    /** Create initial commit. **/
    public Commit() {
        this._message = "initial commit";
        this._parentCommit = "";
        this._parentCommits = new String[]{};
        this._fileToBlob = new HashMap<>();
        this._timeStamp = "Thu Jan 1 00:00:00 1970 -0800";
        this._hashed = hashMe();
    }

    /** Commit where previous commit already exists.**/
    public Commit(int unused) {
    }

    /** Commit with essential info: Message,
     * string-converted parent commit,
     * hashmap mapping hash filenames to blobs.
     * @param msg commit message
     * @param parent commit parent.
     * @param fileMap commit file snapshot.**/
    public Commit(String msg, String parent, HashMap<String, Blob> fileMap) {
        this._message = msg;
        this._parentCommit = parent;
        this._parentCommits = new String[]{parent};
        this._fileToBlob = fileMap;
        Date date = new Date();
        this._timeStamp = new SimpleDateFormat("EEE MMM d "
                + "HH:mm:ss yyyy").format(date) + " -0800";
        this._hashed = hashMe();
    }
    /** Creating the head commit after a merge.
     * @param msg commit message
     * @param parents list of commit parents.
     * @param fileMap commit file snapshot.**/
    public Commit(String msg, String[] parents,
                  HashMap<String, Blob> fileMap) {
        Date date = new Date();
        this._message = msg;
        this._parentCommit = parents[0];
        this._parentCommits = parents;
        this._fileToBlob = fileMap;
        this._timeStamp = new SimpleDateFormat("EEE MMM d"
                + " HH:mm:ss yyyy").format(date) + " -0800";
        this._hashed = hashMe();
    }

    /** Return STRING HASH of commit.
     * @return unique Commit ID.*/
    public String hashMe() {
        String filesString = "";
        if (_fileToBlob != null) {
            filesString = _fileToBlob.toString();
        }
        return Utils.sha1(_message, filesString,
                _timeStamp, _parentCommit, Arrays.toString(_parentCommits));
    }

    /** Getter method for files.
     * @return message**/
    public String getMsg() {
        return this._message;
    }

    /**Getter method for parent SHA-1 Hash IDs.
     * @return parent ID**/
    public String getParent() {
        return this._parentCommit;
    }
    /** Getter method for the FIRST parent, if multiple.
     * Use only when merging.
     * @return first parent ID**/
    public String getFirstParent() {
        if (_parentCommits.length == 0) {
            return "";
        }
        return _parentCommits[0];
    }
    /**Getter method for list of parents' SHA-1 Hash IDs.
     * @return list of parents.**/
    public String[] getParents() {
        return this._parentCommits;
    }
    /**Getter method for unique SHA-1 ID.
     * @return unique ID **/
    public String getID() {
        return this._hashed;
    }

    /** Getter method for files.
     * @return files.**/
    public HashMap<String, Blob> getFiles() {
        return this._fileToBlob;
    }

    /** Getter method for timestamp.
     * @return timestamp**/
    public String getTimestamp() {
        return this._timeStamp;
    }

    /** Commit Message. **/
    private String _message;

    /** Maps filename to blob references
     * (i.e. SHA-1 hash of blob). **/
    private HashMap<String, Blob> _fileToBlob = new HashMap<>();

    /** Name of HEAD branch. **/
    private String headName;

    /**Name of current branch this commit is on.**/
    private String branchName;

    /** String-encoded parent commit,
     * or commit made immediately before commit.**/
    private String _parentCommit;

    /** List of potential multiple parent commits (merge).**/
    private String[] _parentCommits;

    /** Time Stamp of Commit.**/
    private String _timeStamp;

    /** Hashed version of this commit. **/
    private String _hashed;

}
