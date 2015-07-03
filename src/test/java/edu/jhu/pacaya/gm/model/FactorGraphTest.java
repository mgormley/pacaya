package edu.jhu.pacaya.gm.model;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;

import edu.jhu.pacaya.gm.data.bayesnet.BayesNetReaderTest;
import edu.jhu.pacaya.gm.model.FactorGraphsForTests.FgAndVars;
import edu.jhu.pacaya.gm.model.Var.VarType;

public class FactorGraphTest {

    @Test
    public void testIsAcyclic() throws IOException {
        FactorGraph fg = FactorGraphsForTests.getLinearChainGraph();
        assertTrue(fg.getBipgraph().isAcyclic());
        fg = BayesNetReaderTest.readSimpleFg();
        assertFalse(fg.getBipgraph().isAcyclic());
    }
    
    @Test
    public void testGetConnectedComponents() throws IOException {
        FactorGraph fg = new FactorGraph();

        // Create three tags.
        Var t0 = new Var(VarType.PREDICTED, 2, "t0", null);
        Var t1 = new Var(VarType.PREDICTED, 2, "t1", null);
        Var t2 = new Var(VarType.PREDICTED, 2, "t2", null);

        // Emission factors. 
        ExplicitFactor emit0 = new ExplicitFactor(new VarSet(t0)); 
        ExplicitFactor emit1 = new ExplicitFactor(new VarSet(t1)); 
        ExplicitFactor emit2 = new ExplicitFactor(new VarSet(t2)); 
        
        // Transition factors.
        ExplicitFactor tran0 = new ExplicitFactor(new VarSet(t0, t1)); 
        ExplicitFactor tran1 = new ExplicitFactor(new VarSet(t1, t2)); 
        
        fg.addFactor(emit0);
        assertEquals(1, fg.getConnectedComponents().size());
        fg.addFactor(emit1);
        assertEquals(2, fg.getConnectedComponents().size());
        fg.addFactor(emit2);
        assertEquals(3, fg.getConnectedComponents().size());
        fg.addFactor(tran0);
        assertEquals(2, fg.getConnectedComponents().size());
        fg.addFactor(tran1);
        assertEquals(1, fg.getConnectedComponents().size());
    }
    
    @Test
    public void testGetClamped() {
        FgAndVars fgv = FactorGraphsForTests.getLinearChainFgWithVars();
        
        VarConfig clmpConfig = new VarConfig();
        clmpConfig.put(fgv.w0, 0);
        clmpConfig.put(fgv.w1, 1);
        clmpConfig.put(fgv.w2, 0);
        clmpConfig.put(fgv.t1, 1);
        
        FactorGraph fgClmp = fgv.fg.getClamped(clmpConfig);
                
        // The original 5 factors, plus 4 ClampFactors.
        assertEquals(5 + 4, fgClmp.getNumFactors());
        // 2 unconstrained variables, plus 4 clamped variables.
        assertEquals(2 + 4, fgClmp.getNumVars());
        // The original edges plus 8 edges for the ClampFactors.
        assertEquals(20, fgv.fg.getNumEdges());
        assertEquals(20+8, fgClmp.getNumEdges());
    }
    
    @Test
    public void testConstruction() {
        FactorGraph fg = FactorGraphsForTests.getLinearChainGraph();
        
        assertEquals(5, fg.getNumFactors());
        assertEquals(3, fg.getNumVars());
        // There is a pair of edges for each emission factor and a 
        assertEquals(3*2 + 2*2*2, fg.getNumEdges());
    }
    
}
