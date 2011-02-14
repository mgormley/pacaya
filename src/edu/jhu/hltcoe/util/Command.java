package edu.jhu.hltcoe.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class Command {

    private Command() {
        // private constructor
    }

    public static void runCommand(String[] cmdArray, String logFile) {
        ProcessBuilder pb = new ProcessBuilder(cmdArray);
        pb.redirectErrorStream(true);
        try {
            Process proc = pb.start();
            if (proc.waitFor() != 0) {
                throw new RuntimeException("zimpl failed with exit code: " + proc.exitValue());
            }
            
            // Redirect stderr and stdout to logFile
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
        } catch (Exception e) {
            throw new RuntimeException("Exception thrown while trying to exec zimpl", e);
        }
    }

    public static File createTempDir() {
        File tempDir;
        try {
            tempDir = File.createTempFile("ilp_parse", "");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (!tempDir.delete()) {
            throw new RuntimeException("Could not delete temp file as expected: " + tempDir);
        }
        if (!tempDir.mkdir()) {
            throw new RuntimeException("Could not mkdir as expected: " + tempDir);
        }
        return tempDir;
    }
    
}
