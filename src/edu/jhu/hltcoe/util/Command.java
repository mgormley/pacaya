package edu.jhu.hltcoe.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.Arrays;

import org.apache.commons.cli.CommandLine;

public class Command {

    private static final int NUM_DIGITS = 3;
    
    private Command() {
        // private constructor
    }

    public static void runCommand(String[] cmdArray, File logFile, File dir) {
        ProcessBuilder pb = new ProcessBuilder(cmdArray);
        pb.redirectErrorStream(true);
        pb.directory(dir);
        try {
            Process proc = pb.start();
            // Redirect stderr and stdout to logFile
            // TODO: in Java 7, use redirectOutput(Redirect.to(logFile))
            InputStream inputStream = new BufferedInputStream(proc.getInputStream());
            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(logFile));
            
            int read=0;
            byte[] bytes = new byte[1024];
     
            while((read = inputStream.read(bytes))!= -1){
                out.write(bytes, 0, read);
            }
     
            inputStream.close();
            out.flush();
            out.close();

            if (proc.waitFor() != 0) {
                throw new RuntimeException("Command " + pb.command() + " failed with exit code: " + proc.exitValue() + "\n" + tail(logFile));
            }
        } catch (Exception e) {
            throw new RuntimeException("Exception thrown while trying to exec command " + pb.command() + "\n" + tail(logFile), e);
        }
    }

    private static String tail(File logFile) {
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
            tempDir = getTempPath(prefix, parentDir);
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

    public static String getOptionValue(CommandLine cmd, String name, String defaultValue) {
        return cmd.hasOption(name) ? cmd.getOptionValue(name) : defaultValue;
    }

    public static int getOptionValue(CommandLine cmd, String name, int defaultValue) {
        return cmd.hasOption(name) ? Integer.parseInt(cmd.getOptionValue(name)) : defaultValue;
    }

    public static double getOptionValue(CommandLine cmd, String name, double defaultValue) {
        return cmd.hasOption(name) ? Double.parseDouble(cmd.getOptionValue(name)) : defaultValue;
    }
   
}
