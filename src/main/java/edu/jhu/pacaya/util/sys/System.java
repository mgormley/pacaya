package edu.jhu.pacaya.util.sys;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import edu.jhu.pacaya.util.files.QFiles;

public class System {

    private System() {
        // private constructor
    }

    /**
     * Checks the exit code and throws an Exception if the process failed.
     */
    public static void runCommand(String[] cmdArray, File logFile, File dir) {
        Process proc = System.runProcess(cmdArray, logFile, dir);
        if (proc.exitValue() != 0) {
            throw new RuntimeException("Command failed with exit code " + proc.exitValue() + ": "
                    + System.cmdToString(cmdArray) + "\n" + QFiles.tail(logFile));
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
                tail = QFiles.tail(logFile);
            } catch (Throwable t) {
                // Ignore new exceptions
            }
            throw new RuntimeException("Exception thrown while trying to exec command " + System.cmdToString(cmdArray) + "\n"
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


}
