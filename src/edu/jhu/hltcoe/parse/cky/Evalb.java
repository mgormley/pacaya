package edu.jhu.hltcoe.parse.cky;

import java.io.File;

import edu.jhu.hltcoe.util.Command;

public class Evalb {

    private File evalbFile;
    private File prmFile;

    public Evalb(File evalbDir) {
        this(new File(evalbDir, "evalb"), new File(evalbDir, "COLLINS.prm")); 
    }
    
    public Evalb(File evalbFile, File prmFile) {
        if (!evalbFile.isFile()) {
            throw new IllegalStateException("Invalid evalb file: " + evalbFile);
        }
        if (!prmFile.isFile()) {
            throw new IllegalStateException("Invalid prm file: " + prmFile);
        }
        this.evalbFile = evalbFile;
        this.prmFile = prmFile;
    }
    
    public void runEvalb(File goldTrees, File testTrees, File logFile) {
        String[] cmd = new String[]{
                evalbFile.getAbsolutePath(),
                "-p", prmFile.getAbsolutePath(),
                goldTrees.getAbsolutePath(),
                testTrees.getAbsolutePath(),
        };
        Command.runCommand(cmd, logFile, new File("."));    
    }
    
}
