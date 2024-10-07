
import java.io.*;
import java.math.BigInteger;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.Deflater;

public class Git {

    public static boolean compress = false;

    public Git() throws IOException {
        init();
    }

    public static void init() throws IOException {
        boolean exists = true;
        File git = new File("./git");
        if (!git.exists()) {
            git.mkdir();
            exists = false;
        }
        File objects = new File("./git/objects");
        if (!objects.exists()) {
            objects.mkdir();
            exists = false;
        }
        File index = new File("./git/index");
        exists = !(index.createNewFile()) && exists;
        if (exists) {
            System.out.println("Git Repository already exists");
        }

        File HEAD = new File("./git/HEAD");
        exists = !(index.createNewFile()) && exists;
        if (exists) {
            System.out.println("HEAD Repository already exists");
        }
    }

    // recursively remove git folder
    public static void wipe(File current) throws IOException {
        if (!current.exists()) {
            return;
        }
        if (current.listFiles() != null) {
            for (File file : current.listFiles()) {
                wipe(file);
            }
        }
        current.delete();
    }

    public static void makeBlob(File file) throws IOException, NoSuchAlgorithmException {
        byte[] data = Files.readAllBytes(file.toPath());
        if (compress) {
            data = zip(data);
        }
        String hash = hashBlob(data);
        File blobject = new File("./git/objects/" + hash);

        // copy data to objects file
        FileOutputStream out = new FileOutputStream(blobject);
        out.write(data);
        out.close();

        // add entry to index
        PrintWriter toIndex = new PrintWriter(new BufferedWriter(new FileWriter("./git/index", true)));
        if (file.isDirectory()) {
            toIndex.println("tree " + hash + " " + file.getName());
        } else if (file.isFile()) {
            toIndex.println("blob " + hash + " " + file.getName());
        } else {
            toIndex.println(hash + " " + file.getName());
        }
        toIndex.close();
    }

    //Makes a blob the exact same way but adds the full path.
    public static void makeBlobInDir(File file) throws IOException, NoSuchAlgorithmException {
        byte[] data = Files.readAllBytes(file.toPath());
        //if (compress)
        // data = zip(data);
        String hash = hashBlob(data);
        File blobject = new File("./git/objects/" + hash);

        // copy data to objects file
        FileOutputStream out = new FileOutputStream(blobject);
        out.write(data);
        out.close();

        // add entry to index
        PrintWriter toIndex = new PrintWriter(new BufferedWriter(new FileWriter("./git/index", true)));
        if (file.isDirectory()) {
            toIndex.println("tree " + hash + " " + file.getPath());
        } else if (file.isFile()) {
            toIndex.println("blob " + hash + " " + file.getPath());
        } else {
            toIndex.println(hash + " " + file.getName());
        }
        toIndex.close();
    }

    //Adds a tree using the path and name
    public static void addTree(String directoryPath, String directoryName) throws IOException, NoSuchAlgorithmException {
        File directory = new File(directoryPath);
        String allFiles = "";
        String hash = getDirectoryHash(directory);
        File treeObject = new File("./git/objects/" + hash);
        FileOutputStream out = new FileOutputStream(treeObject);
        System.out.println(directory.getPath());
        //checks everything within the directory
        if (directory.listFiles() == null) {
            System.out.println("this diectory is empty");
        } else {
            for (File subfile : directory.listFiles()) {
                // if there are directories within directories
                if (subfile.isDirectory()) {
                    addTree(subfile.getPath(), subfile.getName());
                    allFiles += ("tree " + getDirectoryHash(subfile) + " " + subfile.getName() + "\n");
                } else {
                    if (subfile.getName().substring(0, 1).equals(".")) {
                        makeBlobInDir(subfile);
                        allFiles += "";
                        System.out.println("this file is hidden");
                    } else {
                        makeBlobInDir(subfile);
                        allFiles += "blob " + hashBlob(Files.readAllBytes(subfile.toPath())) + " " + subfile.getName() + "\n";
                    }
                }
            }
        }
        out.write(allFiles.getBytes());
        System.out.println("tree made");
        PrintWriter toIndex = new PrintWriter(new BufferedWriter(new FileWriter("./git/index", true)));
        toIndex.println("tree " + hash + " " + directoryPath);
        toIndex.close();
        out.close();

    }

    public static String hashBlob(byte[] data) throws IOException, NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] messageDigest = md.digest(data);
        BigInteger n = new BigInteger(1, messageDigest);
        String hash = n.toString(16);
        while (hash.length() < 40) {
            hash = "0" + hash;
        }
        return hash;
    }

    public static byte[] zip(byte[] unzipped) throws IOException, NoSuchAlgorithmException {
        Deflater deflater = new Deflater();
        deflater.setInput(unzipped);
        deflater.finish();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(deflater.deflate(new byte[unzipped.length]));
        return out.toByteArray();
    }

    //Gets the specific hash for a directory
    public static String getDirectoryHash(File file) throws NoSuchAlgorithmException, IOException {
        File directory = file;
        String allFiles = "";
        byte[] hashes;
        for (File subfile : directory.listFiles()) {
            if (subfile.isDirectory()) {
                //Recursively calls itself
                allFiles += getDirectoryHash(subfile);
            } else {
                allFiles += "blob " + hashBlob(Files.readAllBytes(subfile.toPath())) + " " + subfile.getName() + "\n";
            }
        }
        hashes = allFiles.getBytes();
        String finalHash;
        finalHash = hashBlob(hashes);
        System.out.println("the final hash is " + finalHash);
        return finalHash;
    }

    public static void createRootTreeSnapshot(String rootDirPath) throws IOException, NoSuchAlgorithmException {
        File rootDir = new File(rootDirPath);
        if (!rootDir.exists() || !rootDir.isDirectory()) {
            throw new IOException("Invalid root directory: " + rootDirPath);
        }

        // Recursively create blobs and tree files for the entire directory structure
        String rootTreeHash = getDirectoryHash(rootDir); // Get the hash of the root directory

        // Add the tree structure for the root directory
        addTree(rootDir.getPath(), rootDir.getName());

        // Save the root tree hash in the HEAD file
        File rootTreeFile = new File("./git/HEAD");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(rootTreeFile))) {
            writer.write("tree " + rootTreeHash + " " + rootDirPath);
            writer.newLine();
        }

        System.out.println("Root tree snapshot created with hash: " + rootTreeHash);
    }

}
