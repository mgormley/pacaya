package edu.jhu.hltcoe.ilp;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import edu.jhu.hltcoe.util.Files;


public class ZimplSolverTest {
    
    private File tempDir; 
    
    @BeforeClass
    public static void setUpStatic() {
        // This doesn't work. You must specify the following JVM arg:
        // -Djava.library.path=/Users/mgormley/installed/ILOG/CPLEX_Studio_AcademicResearch122/cplex/bin/x86-64_darwin9_gcc4.0
        // and the following environment variable
        // ILOG_LICENSE_FILE=/Users/mgormley/installed/ILOG/access.ilm
        System.setProperty("java.library.path", "/Users/mgormley/installed/ILOG/CPLEX_Studio_AcademicResearch122/cplex/bin/x86-64_darwin9_gcc4.0");
    }
    
    @Before
    public void setUp() {
        tempDir = Files.createTempDir("workspace", new File("."));
    }

    @Test
    public void testClGurobiIlpSolver() throws IOException {
        runZimplSolver(tempDir, new ClGurobiIlpSolver(tempDir, 2, 128));
    }
    
    @Test
    public void testJavaGurobiIlpSolver() throws IOException {
        runZimplSolver(tempDir, new JavaGurobiIlpSolver(tempDir));
    }
    
    @Test
    public void testCplexIlpSolver() throws IOException {
        runZimplSolver(tempDir, new CplexIlpSolver(tempDir, 2, 128));
    }
    
    @Test
    public void testInfeasible() {
        runZimplInfeasible(tempDir, new ClGurobiIlpSolver(tempDir, 2, 128));
        runZimplInfeasible(tempDir, new JavaGurobiIlpSolver(tempDir));
        runZimplInfeasible(tempDir, new CplexIlpSolver(tempDir, 2, 128));
    }

    private static void runZimplInfeasible(File tempDir, IlpSolver ilpSolver) {
        ZimplSolver solver = new ZimplSolver(tempDir, ilpSolver);
        URL url = ZimplSolverTest.class.getResource("/edu/jhu/hltcoe/ilp/infeasible.zpl");
        File zimplFile = new File(url.getFile());
        try {
            solver.solve(zimplFile);
        } catch(RuntimeException e) {
            Assert.assertTrue(e.getMessage().contains("no optimal solution found"));
        }
    }
    
    private static void runZimplSolver(File tempDir, IlpSolver ilpSolver) {
        ZimplSolver solver = new ZimplSolver(tempDir, ilpSolver);
        URL url = ZimplSolverTest.class.getResource("/edu/jhu/hltcoe/ilp/chvatal_diet.zpl");
        File zimplFile = new File(url.getFile());
        solver.solve(zimplFile);
        Map<String,Double> sol = solver.getResult();
        
        Map<String,Double> gold = new HashMap<String,Double>();
        gold.put("x$Oatmeal", 4.0);
        gold.put("x$Chicken", 0.0);
        gold.put("x$Eggs", 0.0);
        gold.put("x$Milk", 5.0);
        gold.put("x$Pie", 2.0);
        gold.put("x$Pork", 0.0);
        
        Assert.assertEquals(gold, sol);
        
        Assert.assertEquals(97.0, solver.getObjective(), 1E-13);
    }
    
}
