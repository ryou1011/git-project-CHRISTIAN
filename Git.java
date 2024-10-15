
import java.io.*;
import java.math.BigInteger;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.zip.Deflater;

public class Git implements GitInterface {

    public static boolean compress = false;
    private static File headFile = new File("./git/HEAD");

    // Constructor: Initializes the repository by calling init()
    public Git() throws IOException {
        init();
    }

    // Initializes the Git repository by creating necessary folders and files
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
        if (!headFile.exists()) {
            headFile.createNewFile();
        }
        if (exists) {
            System.out.println("Git Repository already exists");
        }
    }

    // Wipes out the Git folder recursively
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

    // Stages a file or directory for the next commit
    @Override
    public void stage(String filePath) {
        File fileToStage = new File(filePath);
        if (!fileToStage.exists()) {
            System.out.println("File or directory does not exist.");
            return;
        }
        if (fileToStage.isDirectory()) {
            try {
                addTree(fileToStage.getPath(), fileToStage.getName());
            } catch (IOException | NoSuchAlgorithmException ex) {
            }
        } else {
            try {
                makeBlob(fileToStage);
            } catch (IOException | NoSuchAlgorithmException ex) {
            }
        }
        System.out.println("Staging complete.");
    }

    // Creates a commit with the given author and message
    @Override
    public String commit(String author, String message) {
        String rootTreeHash = null;
        try {
            rootTreeHash = getDirectoryHash(new File("./git"));
        } catch (IOException | NoSuchAlgorithmException ex) {
        }
        String parentCommitHash = null;
        try {
            parentCommitHash = readHead();
        } catch (IOException ex) {
        }
        String commitContents = "tree: " + rootTreeHash + "\n";
        if (parentCommitHash.isEmpty()) {
            commitContents += "parent: \n";
        } else {
            commitContents += "parent: " + parentCommitHash + "\n";
        }
        commitContents += "author: " + author + "\n";
        commitContents += "date: " + new Date().toString() + "\n";
        commitContents += "message: " + message + "\n";

        String commitHash = null;
        try {
            commitHash = hashBlob(commitContents.getBytes());
        } catch (NoSuchAlgorithmException ex) {
        }
        File commitFile = new File("./git/objects/" + commitHash);
        try (FileWriter commitWriter = new FileWriter(commitFile)) {
            commitWriter.write(commitContents);
        } catch (IOException ex) {
        }
        try {
            updateHead(commitHash);
        } catch (IOException ex) {
        }
        return commitHash;
    }

    // Creates a blob object from the file and writes it into the objects folder
    public static void makeBlob(File file) throws IOException, NoSuchAlgorithmException {
        byte[] data = Files.readAllBytes(file.toPath());
        if (compress) {
            data = zip(data);
        }
        String hash = hashBlob(data);
        File blobject = new File("./git/objects/" + hash);

        // Write blob content to the objects file
        try (FileOutputStream out = new FileOutputStream(blobject)) {
            out.write(data);
        }

        // Update the index file with blob or tree information
        try (PrintWriter toIndex = new PrintWriter(new BufferedWriter(new FileWriter("./git/index", true)))) {
            if (file.isDirectory()) {
                toIndex.println("tree " + hash + " " + file.getName());
            } else if (file.isFile()) {
                toIndex.println("blob " + hash + " " + file.getName());
            }
        }
    }

    // Adds a tree to the Git repository using the directory path and name
    public static void addTree(String directoryPath, String directoryName) throws IOException, NoSuchAlgorithmException {
        File directory = new File(directoryPath);
        StringBuilder allFiles = new StringBuilder();
        String hash = getDirectoryHash(directory);
        File treeObject = new File("./git/objects/" + hash);

        try (FileOutputStream out = new FileOutputStream(treeObject)) {
            if (directory.listFiles() != null) {
                for (File subfile : directory.listFiles()) {
                    if (subfile.isDirectory()) {
                        addTree(subfile.getPath(), subfile.getName());
                        allFiles.append("tree ").append(getDirectoryHash(subfile)).append(" ").append(subfile.getName()).append("\n");
                    } else if (!subfile.getName().startsWith(".")) {
                        makeBlob(subfile);
                        allFiles.append("blob ").append(hashBlob(Files.readAllBytes(subfile.toPath()))).append(" ").append(subfile.getName()).append("\n");
                    }
                }
            }
            out.write(allFiles.toString().getBytes());
        }

        try (PrintWriter toIndex = new PrintWriter(new BufferedWriter(new FileWriter("./git/index", true)))) {
            toIndex.println("tree " + hash + " " + directoryPath);
        }
    }

    // Reads the HEAD file to get the most recent commit hash
    private static String readHead() throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(headFile))) {
            String head = reader.readLine();
            return head != null ? head : "";
        }
    }

    // Updates the HEAD file with the latest commit hash
    private static void updateHead(String commitHash) throws IOException {
        try (FileWriter writer = new FileWriter(headFile)) {
            writer.write(commitHash);
        }
    }

    // Generates the directory hash recursively by combining hashes of its files and subdirectories
    public static String getDirectoryHash(File file) throws IOException, NoSuchAlgorithmException {
        File directory = file;
        StringBuilder allFiles = new StringBuilder();

        for (File subfile : directory.listFiles()) {
            if (subfile.isDirectory()) {
                allFiles.append(getDirectoryHash(subfile));
            } else {
                allFiles.append("blob ").append(hashBlob(Files.readAllBytes(subfile.toPath()))).append(" ").append(subfile.getName()).append("\n");
            }
        }

        byte[] hashes = allFiles.toString().getBytes();
        return hashBlob(hashes);
    }

    // Generates the hash for a given blob of data using SHA-1
    public static String hashBlob(byte[] data) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] messageDigest = md.digest(data);
        BigInteger n = new BigInteger(1, messageDigest);
        String hash = n.toString(16);
        while (hash.length() < 40) {
            hash = "0" + hash;
        }
        return hash;
    }

    // Compresses the data using the Deflater algorithm
    public static byte[] zip(byte[] unzipped) throws IOException {
        Deflater deflater = new Deflater();
        deflater.setInput(unzipped);
        deflater.finish();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(deflater.deflate(new byte[unzipped.length]));
        return out.toByteArray();
    }
}
