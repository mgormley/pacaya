package edu.jhu.gm.inf;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import edu.jhu.gm.inf.BeliefPropagation.BeliefPropagationPrm;
import edu.jhu.gm.inf.BeliefPropagation.BpUpdateOrder;
import edu.jhu.gm.model.ExplicitFactor;
import edu.jhu.gm.model.Factor;
import edu.jhu.gm.model.FactorGraph;
import edu.jhu.gm.model.Var;
import edu.jhu.gm.model.Var.VarType;
import edu.jhu.gm.model.VarSet;

public class BetheFreeEnergyTest {

    @Test
    public void testPrintBfeOnChain() {
        boolean logDomain = false;
        
        FactorGraph fg = getLinearChainGraph();

        BeliefPropagationPrm prm = new BeliefPropagationPrm();
        prm.maxIterations = 1;
        prm.logDomain = logDomain;
        //prm.schedule = BpScheduleType.TREE_LIKE;
        //prm.updateOrder = BpUpdateOrder.SEQUENTIAL;        
        prm.updateOrder = BpUpdateOrder.PARALLEL;
        prm.maxIterations = 100;
        
        prm.normalizeMessages = true;
        BeliefPropagation bp = new BeliefPropagation(fg, prm);
        bp.run();
        System.out.println("BFE:" + bp.getBetheFreeEnergy());

        BruteForceInferencer bf = new BruteForceInferencer(fg, logDomain);
        bf.run();
                
        assertEquals(bf.getPartition(), bp.getPartition(), 1e-13);
    }

    public static void printBeliefs(BeliefPropagation bp) {
        FactorGraph fg = bp.getFactorGraph();
        for (int i=0; i<fg.getNumVars(); i++) {
            System.out.println(bp.getVarBeliefs(i));
        }
        for (int a=0; a<fg.getNumFactors(); a++) {
            System.out.println(bp.getFactorBeliefs(a));
        }        
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
                
        //fg.addFactor(emit0);
        //fg.addFactor(emit1);
        fg.addFactor(emit2);
        fg.addFactor(tran0);
        fg.addFactor(tran1);
        
        for (Factor f : fg.getFactors()) {
            ((ExplicitFactor)f).convertRealToLog();
        }
        
        return fg;
    }

}
