package gitlet;

import java.io.Serializable;
import java.io.File;
import java.util.ArrayList;
import java.util.List;


/** Class for representing file Contents.
 * @author Kevin Moy**/

public class Blob implements Serializable {

    /** Create a Blob given a filename.
     * @param name file name.**/
    public Blob(String name) {
        File f = new File(name);
        this._fileContents = Utils.readContents(f);
        this._stringEquivalent = Utils.readContentsAsString(f);
        this._fileName = name;
        this._hashed = hashMe();
    }

    /** Convert Blob to hashcode--
     * i.e. use SHA1 on accumulation container of blob features.
     * @return SHA-1 unique ID.**/
    public String hashMe() {
        List<Object> blobStuff = new ArrayList<>();
        blobStuff.add(_fileName);
        blobStuff.add(_fileContents);
        blobStuff.add(_stringEquivalent);
        return Utils.sha1(blobStuff);
    }
    /** Getter method for blob name.
     * @return name of file.**/
    public String getName() {
        return _fileName;
    }
    /** Getter method for contents in byte-array form.
     * @return byte form of blob.**/
    public byte[] getByted() {
        return _fileContents;
    }
    /** returns string form of blob object.
     * @return string form**/
    public String convertToString() {
        return _stringEquivalent;
    }
    /**Getter method for unique SHA-1 ID.
     * @return unique SHA-1 ID.**/
    public String getID() {
        return this._hashed;
    }

    /**File Name. **/
    private String _fileName;
    /** Holds byte array version of Blob.**/
    private byte[] _fileContents;
    /** blob string equivalent. **/
    private String _stringEquivalent;
    /** Hashed Blob. **/
    private String _hashed;

}
