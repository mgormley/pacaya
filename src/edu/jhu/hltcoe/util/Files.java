package edu.jhu.hltcoe.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
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

    public static Matcher getFirstMatch(File gurobiLog, Pattern pattern) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(gurobiLog));
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
    
}