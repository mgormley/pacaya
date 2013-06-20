package edu.jhu.parse.cky;

import java.io.File;
import java.util.List;

import edu.jhu.util.Command;
import edu.jhu.util.Files;

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
    
    //TODO: public void runEvalb(List<NaryTreeNode> goldTrees, List<NaryTreeNode> testTrees, File logFile) {
    //
    //}
    
    public void runEvalb(File goldTrees, File testTrees, File logFile) {
        String[] cmd = new String[]{
                evalbFile.getAbsolutePath(),
                "-p", prmFile.getAbsolutePath(),
                goldTrees.getAbsolutePath(),
                testTrees.getAbsolutePath(),
        };
        Command.runCommand(cmd, logFile, new File("."));    
        Files.cat(logFile);
    }
    
}
