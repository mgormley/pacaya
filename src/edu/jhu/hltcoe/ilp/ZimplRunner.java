package edu.jhu.hltcoe.ilp;

import java.io.File;

import edu.jhu.hltcoe.util.Command;


public class ZimplRunner {
    
    private static final String zimplBinary;
    static {
        File zb = new File("/Users/mgormley/Documents/JHU4_S10/dep_parse/bin/zimpl-3.1.0.darwin.x86.gnu.opt");
        if (zb.exists()) {
            zimplBinary = zb.getAbsolutePath();
        } else {
            zimplBinary = "zimpl";
        }
    }
    private String outputPrefix;
    private File zimplFile;
    private File tempDir;
    
    public ZimplRunner(File zimplFile, File tempDir) {
        this.zimplFile = zimplFile;
        this.tempDir = tempDir;
        this.outputPrefix = new File(tempDir, zimplFile.getName().replace(".zpl", "")).getAbsolutePath();
    }

    public File getLpFile() {
        return new File(outputPrefix + ".lp");
    }
    
    public File getTblFile() {
        return new File(outputPrefix + ".tbl");
    }
    
    public void runZimpl() {
        runZimpl("lp", 1);
    }
    
    private void runZimpl(String type, int verbosity) {
        String[] cmdArray = new String[] { 
                zimplBinary, 
                "-r", // Write ORD file
                "-m", // Write MST file
                "-o", outputPrefix,
                "-t", type, 
                "-v"+String.valueOf(verbosity), 
                zimplFile.getAbsolutePath() };
        File zimplLog = new File(tempDir, "zimpl.log");
        Command.runCommand(cmdArray, zimplLog, tempDir);
    }

}
