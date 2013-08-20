package edu.jhu.gm;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;

import edu.jhu.gm.Var.VarType;
import edu.jhu.gm.data.BayesNetReaderTest;
import edu.jhu.util.Utilities;

public class FactorGraphTest {

    @Test
    public void testIsUndirectedTree() throws IOException {
        FactorGraph fg = getLinearChainGraph(false);        
        for (int i=0; i<fg.getNumNodes(); i++) {
            assertEquals(true, fg.isUndirectedTree(fg.getNode(i)));
        }
        fg = BayesNetReaderTest.readSimpleFg(false);
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
        ExpFamFactor emit0 = new ExpFamFactor(new VarSet(t0), "emit"); 
        ExpFamFactor emit1 = new ExpFamFactor(new VarSet(t1), "emit"); 
        ExpFamFactor emit2 = new ExpFamFactor(new VarSet(t2), "emit"); 
        
        // Transition factors.
        ExpFamFactor tran0 = new ExpFamFactor(new VarSet(t0, t1), "tran"); 
        ExpFamFactor tran1 = new ExpFamFactor(new VarSet(t1, t2), "tran"); 
        
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
        FgAndVars fgv = getLinearChainFgWithVars(false);
        
        VarConfig clmpConfig = new VarConfig();
        clmpConfig.put(fgv.w0, 0);
        clmpConfig.put(fgv.w1, 1);
        clmpConfig.put(fgv.w2, 0);
        clmpConfig.put(fgv.t1, 1);
        
        FactorGraph fgClmp = fgv.fg.getClamped(clmpConfig);
                
        assertEquals(5, fgClmp.getNumFactors());
        assertEquals(2, fgClmp.getNumVars());
        assertEquals(7, fgClmp.getNumNodes());
        // There is a pair of edges for each emission factor and a 
        assertEquals(8, fgClmp.getNumEdges());
    }
    
    @Test
    public void testConstruction() {
        FactorGraph fg = getLinearChainGraph(false);
        
        assertEquals(5, fg.getNumFactors());
        assertEquals(3, fg.getNumVars());
        assertEquals(8, fg.getNumNodes());
        // There is a pair of edges for each emission factor and a 
        assertEquals(3*2 + 2*2*2, fg.getNumEdges());
    }

    /** Gets a simple linear chain CRF consisting of 3 words and 3 tags. */
    public static FactorGraph getLinearChainGraph(boolean logDomain) {
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
        ExpFamFactor emit0 = new ExpFamFactor(new VarSet(t0), "emit"); 
        ExpFamFactor emit1 = new ExpFamFactor(new VarSet(t1), "emit"); 
        ExpFamFactor emit2 = new ExpFamFactor(new VarSet(t2), "emit"); 

        emit0.setValue(0, 0.1);
        emit0.setValue(1, 0.9);
        emit1.setValue(0, 0.3);
        emit1.setValue(1, 0.7);
        emit2.setValue(0, 0.5);
        emit2.setValue(1, 0.5);
        
        // Transition factors.
        ExpFamFactor tran0 = new ExpFamFactor(new VarSet(t0, t1), "tran"); 
        ExpFamFactor tran1 = new ExpFamFactor(new VarSet(t1, t2), "tran"); 
        
        tran0.set(1);
        tran0.setValue(0, 0.2);
        tran0.setValue(1, 0.3);
        tran0.setValue(2, 0.4);
        tran0.setValue(3, 0.5);
        tran1.set(1);
        tran1.setValue(0, 1.2);
        tran1.setValue(1, 1.3);
        tran1.setValue(2, 1.4);
        tran1.setValue(3, 1.5);
                
        fg.addFactor(emit0);
        fg.addFactor(emit1);
        fg.addFactor(emit2);
        fg.addFactor(tran0);
        fg.addFactor(tran1);
        
        if (logDomain) {
            for (Factor f : fg.getFactors()) {
                ((ExpFamFactor)f).convertRealToLog();
            }
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

    public static FgAndVars getLinearChainFgWithVars(boolean logDomain) {

        FactorGraph fg = new FactorGraph();

        // Create three words.
        Var w0 = new Var(VarType.OBSERVED, 2, "w0", Utilities.getList("man", "dog"));
        Var w1 = new Var(VarType.OBSERVED, 2, "w1", Utilities.getList("run", "jump"));
        Var w2 = new Var(VarType.OBSERVED, 2, "w2", Utilities.getList("fence", "bucket"));
        
        // Create three tags.
        Var t0 = new Var(VarType.PREDICTED, 2, "t0", Utilities.getList("N", "V"));
        Var t1 = new Var(VarType.PREDICTED, 2, "t1", Utilities.getList("N", "V"));
        Var t2 = new Var(VarType.PREDICTED, 2, "t2", Utilities.getList("N", "V"));

        // Emission factors. 
        ExpFamFactor emit0 = new ExpFamFactor(new VarSet(t0, w0), "emit"); 
        ExpFamFactor emit1 = new ExpFamFactor(new VarSet(t1, w1), "emit"); 
        ExpFamFactor emit2 = new ExpFamFactor(new VarSet(t2, w2), "emit"); 

        emit0.setValue(0, 0.1);
        emit0.setValue(1, 0.9);
        emit1.setValue(0, 0.3);
        emit1.setValue(1, 0.7);
        emit2.setValue(0, 0.5);
        emit2.setValue(1, 0.5);
        
        // Transition factors.
        ExpFamFactor tran0 = new ExpFamFactor(new VarSet(t0, t1), "tran"); 
        ExpFamFactor tran1 = new ExpFamFactor(new VarSet(t1, t2), "tran"); 
        
        tran0.set(1);
        tran0.setValue(0, 0.2);
        tran0.setValue(1, 0.3);
        tran0.setValue(2, 0.4);
        tran0.setValue(3, 0.5);
        tran1.set(1);
        tran1.setValue(0, 1.2);
        tran1.setValue(1, 1.3);
        tran1.setValue(2, 1.4);
        tran1.setValue(3, 1.5);
                
        fg.addFactor(emit0);
        fg.addFactor(emit1);
        fg.addFactor(emit2);
        fg.addFactor(tran0);
        fg.addFactor(tran1);
        
        if (logDomain) {
            for (Factor f : fg.getFactors()) {
                ((ExpFamFactor)f).convertRealToLog();
            }
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
    
    public static FgAndVars getLinearChainFgWithVarsLatent(boolean logDomain) {

        FactorGraph fg = new FactorGraph();

        // Create three words.
        Var w0 = new Var(VarType.OBSERVED, 2, "w0", Utilities.getList("man", "dog"));
        Var w1 = new Var(VarType.OBSERVED, 2, "w1", Utilities.getList("run", "jump"));
        Var w2 = new Var(VarType.OBSERVED, 2, "w2", Utilities.getList("fence", "bucket"));

        // Create latent classes.
        Var z0 = new Var(VarType.LATENT, 2, "z0", Utilities.getList("C1", "C2"));
        Var z1 = new Var(VarType.LATENT, 2, "z1", Utilities.getList("C1", "C2"));
        Var z2 = new Var(VarType.LATENT, 2, "z2", Utilities.getList("C1", "C2"));
        
        // Create three tags.
        Var t0 = new Var(VarType.PREDICTED, 2, "t0", Utilities.getList("N", "V"));
        Var t1 = new Var(VarType.PREDICTED, 2, "t1", Utilities.getList("N", "V"));
        Var t2 = new Var(VarType.PREDICTED, 2, "t2", Utilities.getList("N", "V"));

        // Emission factors. 
        ExpFamFactor emit0 = new ExpFamFactor(new VarSet(z0, w0), "emit"); 
        ExpFamFactor emit1 = new ExpFamFactor(new VarSet(z1, w1), "emit"); 
        ExpFamFactor emit2 = new ExpFamFactor(new VarSet(z2, w2), "emit"); 

        emit0.setValue(0, 0.1);
        emit0.setValue(1, 0.9);
        emit1.setValue(0, 0.3);
        emit1.setValue(1, 0.7);
        emit2.setValue(0, 0.5);
        emit2.setValue(1, 0.5);
        
        // Latent emission factors. 
        ExpFamFactor emitL0 = new ExpFamFactor(new VarSet(t0, z0), "emit"); 
        ExpFamFactor emitL1 = new ExpFamFactor(new VarSet(t1, z1), "emit"); 
        ExpFamFactor emitL2 = new ExpFamFactor(new VarSet(t2, z2), "emit"); 

        emitL0.setValue(0, 1.1);
        emitL0.setValue(1, 1.9);
        emitL1.setValue(0, 1.3);
        emitL1.setValue(1, 1.7);
        emitL2.setValue(0, 1.5);
        emitL2.setValue(1, 1.5);
        
        // Transition factors.
        ExpFamFactor tran0 = new ExpFamFactor(new VarSet(t0, t1), "tran"); 
        ExpFamFactor tran1 = new ExpFamFactor(new VarSet(t1, t2), "tran"); 
        
        tran0.set(1);
        tran0.setValue(0, 0.2);
        tran0.setValue(1, 0.3);
        tran0.setValue(2, 0.4);
        tran0.setValue(3, 0.5);
        tran1.set(1);
        tran1.setValue(0, 1.2);
        tran1.setValue(1, 1.3);
        tran1.setValue(2, 1.4);
        tran1.setValue(3, 1.5);
                
        fg.addFactor(emit0);
        fg.addFactor(emit1);
        fg.addFactor(emit2);
        fg.addFactor(emitL0);
        fg.addFactor(emitL1);
        fg.addFactor(emitL2);
        fg.addFactor(tran0);
        fg.addFactor(tran1);

        if (logDomain) {
            for (Factor f : fg.getFactors()) {
                ((ExpFamFactor)f).convertRealToLog();
            }
        }
        
        FgAndVars fgv = new FgAndVars();
        fgv.fg = fg;
        fgv.w0 = w0;
        fgv.w1 = w1;
        fgv.w2 = w2;
        fgv.z0 = z0;
        fgv.z1 = z1;
        fgv.z2 = z2;
        fgv.t0 = t0;
        fgv.t1 = t1;
        fgv.t2 = t2;
        return fgv;
    }

}
