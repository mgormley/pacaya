package edu.jhu.pacaya.gm.model;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;

import edu.jhu.pacaya.gm.data.bayesnet.BayesNetReaderTest;
import edu.jhu.pacaya.gm.model.Var.VarType;
import edu.jhu.pacaya.util.collections.Lists;

public class FactorGraphTest {

    @Test
    public void testIsUndirectedTree() throws IOException {
        FactorGraph fg = getLinearChainGraph();        
        for (int i=0; i<fg.getNumNodes(); i++) {
            assertEquals(true, fg.isUndirectedTree(fg.getNode(i)));
        }
        fg = BayesNetReaderTest.readSimpleFg();
        for (int i=0; i<fg.getNumNodes(); i++) {
            assertEquals(false, fg.isUndirectedTree(fg.getNode(i)));
        }
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
        FgAndVars fgv = getLinearChainFgWithVars();
        
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
        assertEquals(15, fgClmp.getNumNodes());
        // There is a pair of edges for each emission factor and a 
        assertEquals(16, fgClmp.getNumEdges());
    }
    
    @Test
    public void testConstruction() {
        FactorGraph fg = getLinearChainGraph();
        
        assertEquals(5, fg.getNumFactors());
        assertEquals(3, fg.getNumVars());
        assertEquals(8, fg.getNumNodes());
        // There is a pair of edges for each emission factor and a 
        assertEquals(3*2 + 2*2*2, fg.getNumEdges());
    }

    /** Gets a simple linear chain CRF consisting of 3 words and 3 tags. */
    public static FactorGraph getLinearChainGraph() {
        FactorGraph fg = new FactorGraph();

        // Create three words.
        Var w0 = new Var(VarType.OBSERVED, 2, "w0", null);
        Var w1 = new Var(VarType.OBSERVED, 2, "w1", null);
        Var w2 = new Var(VarType.OBSERVED, 2, "w2", null);
        
        // Create three tags.
        Var t0 = new Var(VarType.PREDICTED, 2, "t0", null);
        Var t1 = new Var(VarType.PREDICTED, 2, "t1", null);
        Var t2 = new Var(VarType.PREDICTED, 2, "t2", null);

        // Emission factors. 
        ExplicitFactor emit0 = new ExplicitFactor(new VarSet(t0)); 
        ExplicitFactor emit1 = new ExplicitFactor(new VarSet(t1)); 
        ExplicitFactor emit2 = new ExplicitFactor(new VarSet(t2)); 

        emit0.setValue(0, 0.1);
        emit0.setValue(1, 0.9);
        emit1.setValue(0, 0.3);
        emit1.setValue(1, 0.7);
        emit2.setValue(0, 0.5);
        emit2.setValue(1, 0.5);
        
        // Transition factors.
        ExplicitFactor tran0 = new ExplicitFactor(new VarSet(t0, t1)); 
        ExplicitFactor tran1 = new ExplicitFactor(new VarSet(t1, t2)); 
        
        tran0.fill(1);
        tran0.setValue(0, 0.2);
        tran0.setValue(1, 0.3);
        tran0.setValue(2, 0.4);
        tran0.setValue(3, 0.5);
        tran1.fill(1);
        tran1.setValue(0, 1.2);
        tran1.setValue(1, 1.3);
        tran1.setValue(2, 1.4);
        tran1.setValue(3, 1.5);
                
        fg.addFactor(emit0);
        fg.addFactor(emit1);
        fg.addFactor(emit2);
        fg.addFactor(tran0);
        fg.addFactor(tran1);
        
        for (Factor f : fg.getFactors()) {
            ((ExplicitFactor)f).convertRealToLog();
        }
        
        return fg;
    }
    
    public static class FgAndVars {
        
        public FactorGraph fg;
        
        // Three words.
        public Var w0;
        public Var w1;
        public Var w2;
        
        // Three latent vars.
        public Var z0;
        public Var z1;
        public Var z2;
                
        // Three tags.
        public Var t0;
        public Var t1;
        public Var t2;
    }

    public static FgAndVars getLinearChainFgWithVars() {

        FactorGraph fg = new FactorGraph();

        // Create three words.
        Var w0 = new Var(VarType.OBSERVED, 2, "w0", Lists.getList("man", "dog"));
        Var w1 = new Var(VarType.OBSERVED, 2, "w1", Lists.getList("run", "jump"));
        Var w2 = new Var(VarType.OBSERVED, 2, "w2", Lists.getList("fence", "bucket"));
        
        // Create three tags.
        Var t0 = new Var(VarType.PREDICTED, 2, "t0", Lists.getList("N", "V"));
        Var t1 = new Var(VarType.PREDICTED, 2, "t1", Lists.getList("N", "V"));
        Var t2 = new Var(VarType.PREDICTED, 2, "t2", Lists.getList("N", "V"));

        // Emission factors. 
        ExplicitFactor emit0 = new ExplicitFactor(new VarSet(t0, w0)); 
        ExplicitFactor emit1 = new ExplicitFactor(new VarSet(t1, w1)); 
        ExplicitFactor emit2 = new ExplicitFactor(new VarSet(t2, w2)); 

        emit0.setValue(0, 0.1);
        emit0.setValue(1, 0.9);
        emit1.setValue(0, 0.3);
        emit1.setValue(1, 0.7);
        emit2.setValue(0, 0.5);
        emit2.setValue(1, 0.5);
        
        // Transition factors.
        ExplicitFactor tran0 = new ExplicitFactor(new VarSet(t0, t1)); 
        ExplicitFactor tran1 = new ExplicitFactor(new VarSet(t1, t2)); 
        
        tran0.fill(1);
        tran0.setValue(0, 0.2);
        tran0.setValue(1, 0.3);
        tran0.setValue(2, 0.4);
        tran0.setValue(3, 0.5);
        tran1.fill(1);
        tran1.setValue(0, 1.2);
        tran1.setValue(1, 1.3);
        tran1.setValue(2, 1.4);
        tran1.setValue(3, 1.5);
                
        fg.addFactor(emit0);
        fg.addFactor(emit1);
        fg.addFactor(emit2);
        fg.addFactor(tran0);
        fg.addFactor(tran1);
        
        for (Factor f : fg.getFactors()) {
            ((ExplicitFactor)f).convertRealToLog();
        }

        FgAndVars fgv = new FgAndVars();
        fgv.fg = fg;
        fgv.w0 = w0;
        fgv.w1 = w1;
        fgv.w2 = w2;
        fgv.t0 = t0;
        fgv.t1 = t1;
        fgv.t2 = t2;
        return fgv;
    }
    
}
