import java.io.*;
import java.util.Arrays;
import java.util.ArrayList;
import java.nio.file.Files;
import java.security.NoSuchAlgorithmException;

public class GitTester {
    
    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
        Git hub = new Git();
        if (initWorking())
            System.out.println("init success");
        else
            System.out.println("init failure");
        Git.init();
        if (blobWorking())
            System.out.println("blob success");
        else
            System.out.println("blob failure");
        Git.init();
        if (zipWorking())
            System.out.println("zip success");
        else
            System.out.println("zip failure");
        Git.init();
        if(treeworking())
        {
            System.out.println("tree success");
        }
        else
        {
            System.out.println("tree failure");
        }
    }

    public static boolean initWorking() throws IOException {
        File git = new File("./git");
        File objects = new File("./git/objects");
        File index = new File("./git/index");
        if (!git.exists() || !objects.exists() || !index.exists()) {
            //Git.wipe(objects);
            return false;
        }
        Git.init();
        if (!objects.exists()) {
            //Git.wipe(git);
            return false;
        }
        //Git.wipe(git);
        return true;
    }

    public static boolean blobWorking() throws IOException, NoSuchAlgorithmException {
        File testBlob = new File("testBlob");
        PrintWriter pw = new PrintWriter(testBlob);
        pw.print("Ziggity");
        pw.close();
        Git.makeBlob(testBlob);
        File objects = new File("./git/objects");
        if (objects.list().length > 0) {
            for (String blobHash : objects.list()) {
                if (Arrays.equals(Files.readAllBytes((new File("./git/objects/" + blobHash)).toPath()),
                        Files.readAllBytes(testBlob.toPath()))) {
                    //Git.wipe(testBlob);
                    //Git.wipe(new File("./git"));
                    return true;
                }
            }
        }
        //Git.wipe(testBlob);
        //Git.wipe(new File("./git"));
        return false;
    }
    public static boolean treeworking() throws NoSuchAlgorithmException, IOException
    {
        File testTree = new File("testTree");
        testTree.mkdir();
        File inTree1 = new File("testTree/inTree1");
        File inTree2 = new File("testTree/inTree2");
        if (inTree1.createNewFile() == true)
        {
            System.out.println("file in Tree made");
        }
        if (inTree2.createNewFile() == true)
        {
            System.out.println("file in Tree made");
        }
        FileOutputStream out = new FileOutputStream(inTree1);
        out.write("name".getBytes());
        FileOutputStream out2 = new FileOutputStream(inTree2);
        out2.write("Test2".getBytes());
        File testTreeDir = new File("testTree/testDir");
        testTreeDir.mkdir();
        File inTree3 = new File("testTree/testDir/inTree3");
        if (inTree3.createNewFile() == true)
        {
            System.out.println("file in Tree in dir made");
        }
        out.close();
        out2.close();
        Git.addTree(testTree.getPath(), testTree.getName());
        return true;
    }

    public static boolean zipWorking() throws IOException, NoSuchAlgorithmException {
        File testBlob = new File("testBlob");
        PrintWriter pw = new PrintWriter(testBlob);
        pw.print("Ziggity");
        pw.close();
        Git.makeBlob(testBlob);
        Git.compress = true;
        Git.makeBlob(testBlob);
        File objects = new File("./git/objects");
        ArrayList<String> al = new ArrayList<String>();
        for (String hashedBlob : objects.list()) {
            al.add(hashedBlob);
        }
        //Git.wipe(new File("./git"));
        //Git.wipe(testBlob);
        return al.get(0) != al.get(1);
    }
}