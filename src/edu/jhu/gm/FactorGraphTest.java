package edu.jhu.gm;

import static org.junit.Assert.*;

import org.junit.Test;

import edu.jhu.gm.Var.VarType;

public class FactorGraphTest {

    @Test
    public void testConstruction() {
        FactorGraph fg = getSimpleGraph();
        
        assertEquals(5, fg.getNumFactors());
        assertEquals(3, fg.getNumVars());
        assertEquals(8, fg.getNumNodes());
        // There is a pair of edges for each emission factor and a 
        assertEquals(3*2 + 2*2*2, fg.getNumEdges());
    }

    public static FactorGraph getSimpleGraph() {
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
        tran1.set(1);
        tran1.setValue(3, 1.2);
        
        fg.addFactor(emit0);
        fg.addFactor(emit1);
        fg.addFactor(emit2);
        fg.addFactor(tran0);
        fg.addFactor(tran1);
        return fg;
    }

}
