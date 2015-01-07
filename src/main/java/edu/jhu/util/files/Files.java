package edu.jhu.util.files;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;


public class Files {

    private static final int NUM_DIGITS = 3;
    
    private Files() {
        // private constructor
    }

    public static void cat(File logFile) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(logFile));
            char[] cbuf = new char[512];
            while (reader.ready()) {
                int numRead = reader.read(cbuf);
                if (numRead < cbuf.length) {
                    char[] tmp = Arrays.copyOfRange(cbuf, 0, numRead);
                    System.out.print(tmp);
                } else {
                    System.out.print(cbuf);
                }
            }
            reader.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    public static String tail(File logFile) {
        try {
            RandomAccessFile raf = new RandomAccessFile(logFile, "r");
            byte[] bytes = new byte[500];
            raf.skipBytes((int)raf.length() - bytes.length);
            int read = raf.read(bytes);
            raf.close();
            return new String(Arrays.copyOf(bytes, read));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static File createTempDir(String prefix, File parentDir) {
        File tempDir;
        try {
            tempDir = Files.getTempPath(prefix, parentDir);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (!tempDir.mkdir()) {
            throw new RuntimeException("Could not mkdir as expected: " + tempDir);
        }
        return tempDir;
    }
    
    public static BufferedWriter createTempFileBufferedWriter(String prefix, File parentDir) {
        try {
            File tempDir = Files.getTempPath(prefix, parentDir);
            tempDir.getParentFile().mkdirs();
            return java.nio.file.Files.newBufferedWriter(Paths.get(tempDir.getAbsolutePath()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates a file object of a currently available path, but does
     * not create the file. This method is not thread safe.
     */
    private static File getTempPath(String prefix, File parentDir) throws IOException {
        final int maxI = (int)Math.pow(10, NUM_DIGITS);
        String formatStr = "%s_%0"+NUM_DIGITS+"d";
        File path;
        int i;
        for (i=0; i<maxI; i++) {
            path = new File(parentDir, String.format(formatStr, prefix, i));
            if (!path.exists()) {
                return path;
            }
        }
        // If we ran out of short file names, just create a long one
        path = File.createTempFile(prefix, "", parentDir);
        if (!path.delete()) {
            throw new RuntimeException("Could not delete temp file as expected: " + path);
        }
        return path;
    }

    public static boolean fileContains(File file, String text) {
        Process proc = edu.jhu.util.sys.System.runProcess(new String[]{"grep", "-r", text, file.getAbsolutePath()}, null, new File("."));
        if (proc.exitValue() == 0) {
            return true;
        } else {
            return false;
        }
    }

    public static Matcher getFirstMatch(File logFile, Pattern pattern) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(logFile));
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    reader.close();
                    return matcher;
                }
            }
            reader.close();
            return null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Read until the current line equals the breakpoint string.
     */
    public static void readUntil(BufferedReader reader, String breakpoint) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.equals(breakpoint)) {
                return;
            }
        }
    }

    public static String getResourceAsString(String resourceName) throws IOException {
        InputStream inputStream = Files.class.getResourceAsStream(resourceName);
        return getInputStreamAsString(inputStream);
    }
    
    public static String getInputStreamAsString(InputStream inputStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
            sb.append("\n");
        }
        return sb.toString();
    }    

    public static String getResourceAsString(String resourceName, String charset) throws IOException {
        InputStream inputStream = Files.class.getResourceAsStream(resourceName);
        return getInputStreamAsString(inputStream, charset);
    }
    
    public static String getInputStreamAsString(InputStream inputStream, String charset) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, charset));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
            sb.append("\n");
        }
        return sb.toString();
    }

    public static void readUntilCharacter(Reader reader, char stopChar) throws IOException {
        char[] cbuf = new char[1];
        while (reader.read(cbuf) != -1) {
            char c = cbuf[0];
            if (c == stopChar) {
                break;
            }
        }
    }

    public static void serialize(Object obj, String stateFile) {
        serialize(obj, new File(stateFile));
    }

    public static void serialize(Object obj, File stateFile) {
        try {
            OutputStream os = new FileOutputStream(stateFile);
            if (stateFile.getName().endsWith(".gz")) {
                os = new GZIPOutputStream(os);
            }
            ObjectOutputStream out = new ObjectOutputStream(os);
            out.writeObject(obj);
            out.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    public static Object deserialize(String stateFile) {
        return deserialize(new File(stateFile));
    }
    
    public static Object deserialize(File stateFile) {
        try {
            InputStream is = new FileInputStream(stateFile);
            if (stateFile.getName().endsWith(".gz")) {
                is = new GZIPInputStream(is);
            }
            return deserialize(is);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Object deserializeResource(String resource) {
        try {
            InputStream is = Files.class.getResourceAsStream(resource);
            if (resource.endsWith(".gz")) {
                is = new GZIPInputStream(is);
            }
            return deserialize(is);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    private static Object deserialize(InputStream is) {
        try {
            ObjectInputStream in = new ObjectInputStream(is);
            Object inObj = in.readObject();
            in.close();
            return inObj;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<File> getMatchingFiles(File file, String regexStr) {
        Pattern regex = Pattern.compile(regexStr);
        return getMatchingFiles(file, regex);
    }

    private static List<File> getMatchingFiles(File file, Pattern regex) {
        ArrayList<File> files = new ArrayList<File>();
        if (file.exists()) {
            if (file.isFile()) {
                if (regex.matcher(file.getName()).matches()) {
                    files.add(file);
                }
            } else {
                for (File subFile : file.listFiles()) {
                    files.addAll(getMatchingFiles(subFile, regex));
                }
            }
        }
        return files;
    }

    /** CAUTION: This is equivalent to calling rm -r on file. */
    public static void deleteRecursively(File file) {
        if (file.isDirectory()) {
            for (File c : file.listFiles()) {
                deleteRecursively(c);
            }
        }
        if (!file.delete()) {
            System.err.println("WARN: unable to delete file: " + file.getPath());
        }
    }
    
}
