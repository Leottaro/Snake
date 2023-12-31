import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Storage {
    private static final String key = String.format("\"%s%s%s\"", System.getProperty("os.arch"),
            System.getProperty("os.name"), System.getProperty("user.home"));

    private static Path storagePath() {
        String OS = System.getProperty("os.name").toLowerCase();
        String dirName = "SnakeGame";
        String fileName = "bestscore";
        if (OS.indexOf("win") != -1) {
            // Windows
            return Paths.get(String.format("%s\\%s\\%s", System.getenv("APPDATA"), dirName, fileName));
        } else if (OS.indexOf("mac") != -1) {
            // Mac
            return Paths.get(String.format("%s/Library/Application Support/%s/%s", System.getProperty("user.home"),
                    dirName, fileName));
        } else if (OS.indexOf("nix") >= 0 || OS.indexOf("nux") >= 0 || OS.indexOf("aix") > 0) {
            // Linux
            return Paths.get(String.format("%s/var/lib/%s/%s", System.getProperty("user.home"), dirName, fileName));
        } else {
            return null;
        }
    }

    private static int decrypted(ByteBuffer bytes) {
        try {
            bytes.position(0);
            String binaryString = "";
            for (int i = 0; i < 32; i++) {
                int temp = ((int) bytes.getInt() - (int) key.charAt(i % key.length()));
                binaryString += (char) temp;
            }
            int n = Integer.parseInt(binaryString, 2);
            return n;
        } catch (Exception e) {
            System.out.format("an error occured in Storage.decrypted() : %s\n", e);
        }
        return -1;
    }

    private static ByteBuffer crypted(int n) {
        String uncrypted = String.format("%32s", Integer.toBinaryString(n)).replaceAll(" ", "0");
        ByteBuffer bytes = ByteBuffer.allocate(128);
        for (int i = 0; i < uncrypted.length(); i++) {
            int temp1 = (int) uncrypted.charAt(i);
            int temp2 = (int) key.charAt(i % key.length());
            bytes.putInt(temp1 + temp2);
        }
        return bytes;
    }

    public static int read() {
        ByteBuffer bytes = ByteBuffer.allocate(128);
        try (FileInputStream fis = new FileInputStream(storagePath().toFile())) {
            byte reading = (byte) fis.read();
            while (reading != -1) {
                bytes.put(reading);
                reading = (byte) fis.read();
            }
            fis.close();
        } catch (IOException e) {
            System.out.format("an error occured in Storage.read() : %s\n", e);
        }
        return decrypted(bytes);
    }

    public static void write(int n) {
        try (FileOutputStream fos = new FileOutputStream(storagePath().toFile(), false)) {
            fos.write(crypted(n).array());
            fos.close();
        } catch (Exception e) {
            System.out.format("an error occured in Storage.write() : %s\n", e);
        }
    }

    public static boolean createFile(int n) {
        Path path = storagePath();
        if (path == null)
            return false;
        try {
            if (!Files.exists(path.getParent()))
                Files.createDirectory(path.getParent());
            if (!path.toFile().canRead()) {
                path.toFile().createNewFile();
                write(n);
            }
            if (read() == -1) {
                path.toFile().delete();
                path.toFile().createNewFile();
                write(n);
            }
            write(read());
        } catch (Exception e) {
            System.out.format("an error occured in Storage.createFile() : %s\n", e);
            return false;
        }
        return true;
    }
}
