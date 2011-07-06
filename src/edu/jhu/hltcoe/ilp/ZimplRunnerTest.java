package edu.jhu.hltcoe.ilp;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import edu.jhu.hltcoe.util.Files;


public class ZimplRunnerTest {

    private File tempDir; 
        
    @Before
    public void setUp() {
        tempDir = Files.createTempDir("workspace", new File("."));
    }

    @Test
    public void testFixMstFile() throws IOException {
        runZimplSolver(tempDir, new CplexIlpSolver(tempDir, 2, 128));
        File mstFile = new File(tempDir, "startvals.mst");
        Assert.assertTrue(Files.fileContains(mstFile, "xvar"));
        Assert.assertTrue(Files.fileContains(mstFile, "foodVals"));
        Assert.assertTrue(!Files.fileContains(mstFile, "yvar"));
        Assert.assertTrue(!Files.fileContains(mstFile, "otherVals"));
    }

    private static void runZimplSolver(File tempDir, IlpSolver ilpSolver) {
        ZimplSolver solver = new ZimplSolver(tempDir, ilpSolver);
        URL url = ZimplSolverTest.class.getResource("/edu/jhu/hltcoe/ilp/startvals.zpl");
        File zimplFile = new File(url.getFile());
        solver.solve(zimplFile);
    }
    
}
