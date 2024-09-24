import java.io.*;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.math.BigInteger;
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
        if (exists)
            System.out.println("Git Repository already exists");
    }

    // recursively remove git folder
    public static void wipe(File current) throws IOException {
        if (!current.exists())
            return;
        if (current.listFiles() != null)
            for (File file : current.listFiles())
                wipe(file);
        current.delete();
    }

    public static void makeBlob(File file) throws IOException, NoSuchAlgorithmException {
        byte[] data = Files.readAllBytes(file.toPath());
        if (compress)
            data = zip(data);
        String hash = hashBlob(data);
        File blobject = new File("./git/objects/" + hash);

        // copy data to objects file
        FileOutputStream out = new FileOutputStream(blobject);
        out.write(data);
        out.close();

        // add entry to index
        PrintWriter toIndex = new PrintWriter(new BufferedWriter(new FileWriter("./git/index", true)));
        toIndex.println(hash + " " + file.getName());
        toIndex.close();
    }

    public static String hashBlob(byte[] data) throws IOException, NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] messageDigest = md.digest(data);
        BigInteger n = new BigInteger(1, messageDigest);
        String hash = n.toString(16);
        while (hash.length() < 40)
            hash = "0" + hash;
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

}