package edu.jhu.hltcoe.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Files {

    private Files() {
        // private constructor
    }

    public static String tail(File logFile) {
        try {
            RandomAccessFile raf = new RandomAccessFile(logFile, "r");
            byte[] bytes = new byte[500];
            raf.skipBytes((int)raf.length() - bytes.length);
            int read = raf.read(bytes);
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

    /**
     * Creates a file object of a currently available path, but does
     * not create the file. This method is not thread safe.
     */
    private static File getTempPath(String prefix, File parentDir) throws IOException {
        final int maxI = (int)Math.pow(10, Command.NUM_DIGITS);
        String formatStr = "%s_%0"+Command.NUM_DIGITS+"d";
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
        Process proc = Command.runProcess(new String[]{"grep", "-r", text, file.getAbsolutePath()}, null, new File("."));
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
                    return matcher;
                }
            }
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
    
}
