package edu.jhu.hltcoe.ilp;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import edu.jhu.hltcoe.util.Command;


public class ZimplRunnerTest {

    private File tempDir; 
        
    @Before
    public void setUp() {
        tempDir = Command.createTempDir("workspace", new File("."));
    }


    @Test
    public void testFixMstFile() throws IOException {
        runZimplSolver(tempDir, new CplexIlpSolver(tempDir, 2, 128));
        File mstFile = new File(tempDir, "startvals.mst");
        Assert.assertTrue(fileContains(mstFile, "xvar"));
        Assert.assertTrue(fileContains(mstFile, "foodVals"));
        Assert.assertTrue(!fileContains(mstFile, "yvar"));
        Assert.assertTrue(!fileContains(mstFile, "otherVals"));
    }

    private static void runZimplSolver(File tempDir, IlpSolver ilpSolver) {
        ZimplSolver solver = new ZimplSolver(tempDir, ilpSolver);
        URL url = ZimplSolverTest.class.getResource("/edu/jhu/hltcoe/ilp/startvals.zpl");
        File zimplFile = new File(url.getFile());
        solver.solve(zimplFile);
    }
    
    private static boolean fileContains(File file, String text) {
        Process proc = Command.runProcess(new String[]{"grep", "-r", text, file.getAbsolutePath()}, null, new File("."));
        if (proc.exitValue() == 0) {
            return true;
        } else {
            return false;
        }
    }
    
}
