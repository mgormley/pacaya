package edu.jhu.hltcoe.ilp;

import java.io.File;

import edu.jhu.hltcoe.util.Command;


public class ZimplRunner {
    
    private static final String zimplBinary = "/Users/mgormley/Documents/JHU4_S10/dep_parse/bin/zimpl-3.1.0.darwin.x86.gnu.opt";
    private String outputPrefix;
    private String zimplFile;
    
    public ZimplRunner(String zimplFile) {
        this.zimplFile = zimplFile;
        outputPrefix = zimplFile.replaceAll(".zpl$", "");
    }

    public String getLpFile() {
        return outputPrefix + ".lp";
    }
    
    public String getTblFile() {
        return outputPrefix + ".tbl";
    }
    
    public void runZimpl() {
        runZimpl("lp", 1);
    }
    
    private void runZimpl(String type, int verbosity) {
        String[] cmdArray = new String[] { 
                zimplBinary, 
                "-o", outputPrefix,
                "-t", type, 
                "-v"+String.valueOf(verbosity), 
                zimplFile };
        String zimplFileParent = new File(zimplFile).getParent();
        String zimplLog = zimplFileParent + "/zimpl.log";
        Command.runCommand(cmdArray, zimplLog, new File(zimplFileParent));
    }

}
