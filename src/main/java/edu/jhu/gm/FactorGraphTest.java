package edu.jhu.gm;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import edu.jhu.gm.Var.VarType;
import edu.jhu.gm.data.BayesNetReaderTest;

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
        Factor emit0 = new Factor(new VarSet(t0)); 
        Factor emit1 = new Factor(new VarSet(t1)); 
        Factor emit2 = new Factor(new VarSet(t2)); 
        
        // Transition factors.
        Factor tran0 = new Factor(new VarSet(t0, t1)); 
        Factor tran1 = new Factor(new VarSet(t1, t2)); 
        
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
                
        assertEquals(5, fgClmp.getNumFactors());
        assertEquals(2, fgClmp.getNumVars());
        assertEquals(7, fgClmp.getNumNodes());
        // There is a pair of edges for each emission factor and a 
        assertEquals(8, fgClmp.getNumEdges());
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
        Factor emit0 = new Factor(new VarSet(t0)); 
        Factor emit1 = new Factor(new VarSet(t1)); 
        Factor emit2 = new Factor(new VarSet(t2)); 

        emit0.setValue(0, 0.1);
        emit0.setValue(1, 0.9);
        emit1.setValue(0, 0.3);
        emit1.setValue(1, 0.7);
        emit2.setValue(0, 0.5);
        emit2.setValue(1, 0.5);
        
        // Transition factors.
        Factor tran0 = new Factor(new VarSet(t0, t1)); 
        Factor tran1 = new Factor(new VarSet(t1, t2)); 
        
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
        return fg;
    }
    
    public static class FgAndVars {
        
        public FactorGraph fg;
        
        // Three words.
        public Var w0;
        public Var w1;
        public Var w2;
        
        // Three tags.
        public Var t0;
        public Var t1;
        public Var t2;
    }
    
    public static FgAndVars getLinearChainFgWithVars() {

        FactorGraph fg = new FactorGraph();

        // Create three words.
        Var w0 = new Var(VarType.OBSERVED, 2, "w0", getList("man", "dog"));
        Var w1 = new Var(VarType.OBSERVED, 2, "w1", getList("run", "jump"));
        Var w2 = new Var(VarType.OBSERVED, 2, "w2", getList("fence", "bucket"));
        
        // Create three tags.
        Var t0 = new Var(VarType.PREDICTED, 2, "t0", getList("N", "V"));
        Var t1 = new Var(VarType.PREDICTED, 2, "t1", getList("N", "V"));
        Var t2 = new Var(VarType.PREDICTED, 2, "t2", getList("N", "V"));

        // Emission factors. 
        Factor emit0 = new Factor(new VarSet(t0, w0)); 
        Factor emit1 = new Factor(new VarSet(t1, w1)); 
        Factor emit2 = new Factor(new VarSet(t2, w2)); 

        emit0.setValue(0, 0.1);
        emit0.setValue(1, 0.9);
        emit1.setValue(0, 0.3);
        emit1.setValue(1, 0.7);
        emit2.setValue(0, 0.5);
        emit2.setValue(1, 0.5);
        
        // Transition factors.
        Factor tran0 = new Factor(new VarSet(t0, t1)); 
        Factor tran1 = new Factor(new VarSet(t1, t2)); 
        
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
    
    // TODO: move to Utilities.
    public static <T> List<T> getList(T... args) {
        return Arrays.asList(args);
    }

}
