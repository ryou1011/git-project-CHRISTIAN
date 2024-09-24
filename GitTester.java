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
    }

    public static boolean initWorking() throws IOException {
        File git = new File("./git");
        File objects = new File("./git/objects");
        File index = new File("./git/index");
        if (!git.exists() || !objects.exists() || !index.exists()) {
            Git.wipe(objects);
            return false;
        }
        Git.init();
        if (!objects.exists()) {
            Git.wipe(git);
            return false;
        }
        Git.wipe(git);
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
                    Git.wipe(testBlob);
                    Git.wipe(new File("./git"));
                    return true;
                }
            }
        }
        Git.wipe(testBlob);
        Git.wipe(new File("./git"));
        return false;
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
        Git.wipe(new File("./git"));
        Git.wipe(testBlob);
        return al.get(0) != al.get(1);
    }
}