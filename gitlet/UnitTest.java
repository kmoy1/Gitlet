package gitlet;

import ucb.junit.textui;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import static org.junit.Assert.*;

/** The suite of all JUnit tests for the gitlet package.
 *  @author Kevin Moy
 */
public class UnitTest {

    /** Run the JUnit tests in the loa package. Add xxxTest.class entries to
     *  the arguments of runClasses to run other JUnit tests. */
    public static void main(String[] ignored) {
        System.exit(textui.runClasses(UnitTest.class));
    }

    /** A dummy test to avoid complaint. */
    @Test
    public void stupidTest() {
        System.out.println(1 + 1);
    }

    /** Test for correct usage of hash IDs. **/
    @Test
    public void testHashID() {
        Commit ufc = new Commit();
        Commit c2 = new Commit("initial commit", "", null);
        assertNotEquals(ufc, c2);
    }

    /** Test if blob hashes correctly differentiate. **/
    @Test
    public void testBlobCompare() throws IOException {
        File f1 = new File("testFile1");
        File f2 = new File("testFile2");
        f1.createNewFile();
        f2.createNewFile();
        Utils.writeContents(f1, "Gucci Gang");
        Utils.writeContents(f2, "Gucci Bang");
        Blob b1 = new Blob(f1.getName());
        Blob b2 = new Blob(f2.getName());
        assertEquals("Gucci Gang", b1.convertToString());
        assertEquals("Gucci Bang", b2.convertToString());
        assertNotEquals(b1.getID(), b2.getID());
    }

    /** Tests the file contents as string method. **/
    @Test
    public void testFileContents() throws IOException {
        File f1 = new File("testFile1");
        f1.createNewFile();
        Utils.writeContents(f1, "HOES MAD");
        assertEquals("HOES MAD", Utils.readContentsAsString(f1));
    }

    /** Tests for equality of files. **/
    @Test
    public void testFilesEq() throws IOException {
        File f1 = new File("testFile1");
        File f2 = new File("testFile1");
        f1.createNewFile();
        f2.createNewFile();
        assertEquals(f1, f2);
    }

    /** Tests the workingness of getSPCommit function. **/
    @Test
    public void testSimple() throws IOException {
        File f1 = new File("testFile1");
        File f2 = new File("testFile2");
        f1.createNewFile();
        f2.createNewFile();
    }

    /** Tests the universal first commit object. **/
    @Test
    public void testUFC() throws IOException {
        Commit ufc = new Commit();
        Commit ufcIdentical = new Commit();
        assertEquals(ufc.getID(), ufcIdentical.getID());
        assertEquals("initial commit", ufc.getMsg());
        assertEquals("initial commit", ufcIdentical.getMsg());
    }

    /** Tests a generic blob object. **/
    @Test
    public void testBlobInfo() throws IOException {
        File f1 = new File("testFile1");
        f1.createNewFile();
        Utils.writeContents(f1, "HEY HEY HEY");
        Blob b1 = new Blob(f1.getName());
        assertEquals("HEY HEY HEY", b1.convertToString());
    }

    /** Tests the workingness of File contents. **/
    @Test
    public void testFileConts() throws IOException {
        File f1 = new File("testFile1");
        File f2 = new File("testFile2");
        f1.createNewFile();
        f2.createNewFile();
        assertEquals("testFile1", f1.getName());
    }

    /** Tests the inequality of different commits with the same branch. **/
    @Test
    public void testUnequalCommits() throws IOException {
        Commit c1 = new Commit();
        Commit c2 = new Commit("initial commit", c1.getID(), null);
        assertNotEquals(c1, c2);
    }

    /** Tests the ability to write to our files. **/
    @Test
    public void testWrite() throws IOException {
        File file1 = new File("testFile1");
        File f2 = new File("testFile2");
        Utils.writeContents(file1, "AND IIIIIIIIIII WILL ALWAYS");
        assertNotEquals("Dumb stuff", Utils.readContentsAsString(file1));
    }
    /** Tests the workingness of getSPCommit function. **/
    @Test
    public void testRead() throws IOException {
        File f2 = new File("testFile2");
        Utils.writeContents(f2, "HYPE BEAST");
        assertNotEquals("Dumb stuff", Utils.readContentsAsString(f2));
    }

    /** Tests a shit command. **/
    @Test
    public void testShittyCommand() {
        String shitCmd = "SHITTYCOMMAND";
        System.out.println("SHITTYCOMMAND");
        assertNotEquals(shitCmd, "init");
        assertNotEquals(shitCmd, "add");
    }


}


