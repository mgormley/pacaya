package edu.jhu.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import org.apache.commons.cli.CommandLine;

import edu.jhu.util.files.Files;

public class Command {

    private Command() {
        // private constructor
    }

    /**
     * Checks the exit code and throws an Exception if the process failed.
     */
    public static void runCommand(String[] cmdArray, File logFile, File dir) {
        Process proc = runProcess(cmdArray, logFile, dir);
        if (proc.exitValue() != 0) {
            throw new RuntimeException("Command failed with exit code " + proc.exitValue() + ": "
                    + cmdToString(cmdArray) + "\n" + Files.tail(logFile));
        }
    }

    /**
     * Returns the Process which contains the exit code.
     */
    public static Process runProcess(String[] cmdArray, File logFile, File dir) {
        try {

            ProcessBuilder pb = new ProcessBuilder(cmdArray);
            pb.redirectErrorStream(true);
            pb.directory(dir);

            Process proc = pb.start();
            if (logFile != null) {
                // Redirect stderr and stdout to logFile
                // TODO: in Java 7, use redirectOutput(Redirect.to(logFile))
                InputStream inputStream = new BufferedInputStream(proc.getInputStream());
                BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(logFile));

                int read = 0;
                byte[] bytes = new byte[1024];

                while ((read = inputStream.read(bytes)) != -1) {
                    out.write(bytes, 0, read);
                }

                inputStream.close();
                out.flush();
                out.close();
            }
            proc.waitFor();
            return proc;

        } catch (Exception e) {
            String tail = "";
            try {
                tail = Files.tail(logFile);
            } catch (Throwable t) {
                // Ignore new exceptions
            }
            throw new RuntimeException("Exception thrown while trying to exec command " + cmdToString(cmdArray) + "\n"
                    + tail, e);
        }
    }

    public static String cmdToString(String[] cmdArray) {
        StringBuilder sb = new StringBuilder();
        for (String s : cmdArray) {
            sb.append(s);
            sb.append(" ");
        }
        return sb.toString();
    }

    public static String getOptionValue(CommandLine cmd, String name, String defaultValue) {
        return cmd.hasOption(name) ? cmd.getOptionValue(name) : defaultValue;
    }

    public static int getOptionValue(CommandLine cmd, String name, int defaultValue) {
        return cmd.hasOption(name) ? Integer.parseInt(cmd.getOptionValue(name)) : defaultValue;
    }

    public static long getOptionValue(CommandLine cmd, String name, long defaultValue) {
        return cmd.hasOption(name) ? Long.parseLong(cmd.getOptionValue(name)) : defaultValue;
    }

    public static double getOptionValue(CommandLine cmd, String name, double defaultValue) {
        return cmd.hasOption(name) ? Double.parseDouble(cmd.getOptionValue(name)) : defaultValue;
    }

    public static boolean getOptionValue(CommandLine cmd, String name, boolean defaultValue) {
        return cmd.hasOption(name) ? Boolean.parseBoolean(cmd.getOptionValue(name)) : defaultValue;
    }

}
