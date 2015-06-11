package edu.jhu.pacaya.gm.model.globalfac;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Ignore;
import org.junit.Test;

import edu.jhu.pacaya.autodiff.Module;
import edu.jhu.pacaya.autodiff.Tensor;
import edu.jhu.pacaya.autodiff.erma.FgModelIdentity;
import edu.jhu.pacaya.autodiff.erma.LazyVarTensor;
import edu.jhu.pacaya.gm.inf.BruteForceInferencer;
import edu.jhu.pacaya.gm.model.Factor;
import edu.jhu.pacaya.gm.model.FgModel;
import edu.jhu.pacaya.gm.model.Var;
import edu.jhu.pacaya.gm.model.Var.VarType;
import edu.jhu.pacaya.gm.model.VarConfig;
import edu.jhu.pacaya.gm.model.VarSet;
import edu.jhu.pacaya.gm.model.VarTensor;
import edu.jhu.pacaya.util.semiring.Algebra;
import edu.jhu.pacaya.util.semiring.LogSemiring;
import edu.jhu.pacaya.util.semiring.RealAlgebra;


public class ConsecutiveSiblingsFactorTest {

    @Test
    public void testGetScore() throws Exception {
        Algebra s = RealAlgebra.getInstance();
        int n = 7;

        // 1-indexed by head, modifier, sibling.
        Tensor scores = new Tensor(s, n+1, n+1, n+1);
        scores.fill(s.one());
        scores.set(s.fromReal(2), 0, 1, 2);
        scores.set(s.fromReal(3),  0, 1, 3);
        scores.set(s.fromReal(5),  1, 2, 3);
        scores.set(s.fromReal(7),  1, 3, 4);
        scores.set(s.fromReal(11),  1, 4, 6);
        scores.set(s.fromReal(13),  1, 3, 5);
        
        ConsecutiveSiblingsFactor treeFac = getDefaultCs(scores);    
        // Create vc for left branching tree.
        VarConfig vc = new VarConfig();
        for (int i=-1; i<n; i++) {
            for (int j=0; j<n; j++) {
                if (i == j) { continue; }
                vc.put(treeFac.getLinkVar(i, j), (i == j - 1) ? LinkVar.TRUE : LinkVar.FALSE);
            }
        }

        treeFac.updateFromModel(null);        
        assertEquals(1, Math.exp(treeFac.getLogUnormalizedScore(vc)), 1e-13);
        // Add edge from 1 to 3.
        vc.put(treeFac.getLinkVar1Idx(1, 3), LinkVar.TRUE);
        assertEquals(5, Math.exp(treeFac.getLogUnormalizedScore(vc)), 1e-13);
        // Add edge from 1 to 4.
        vc.put(treeFac.getLinkVar1Idx(1, 4), LinkVar.TRUE);
        assertEquals(5*7, Math.exp(treeFac.getLogUnormalizedScore(vc)), 1e-13);
        // Add edge from 1 to 6.
        vc.put(treeFac.getLinkVar1Idx(1, 6), LinkVar.TRUE);
        assertEquals(5*7*11, Math.exp(treeFac.getLogUnormalizedScore(vc)), 1e-10);
        // Add edge from 1 to 5.
        vc.put(treeFac.getLinkVar1Idx(1, 5), LinkVar.TRUE);
        assertEquals(5*7, Math.exp(treeFac.getLogUnormalizedScore(vc)), 1e-13);
        // Remove edge from 1 to 4.
        vc.put(treeFac.getLinkVar1Idx(1, 4), LinkVar.FALSE);
        assertEquals(5*13, Math.exp(treeFac.getLogUnormalizedScore(vc)), 1e-13);
    }
    
    @Test
    public void testGetFactorModule() throws Exception {
        final ConsecutiveSiblingsFactor cs = getDefaultCs();
        FgModel model = new FgModel(100); // TODO: Correctly set number of parameters.
        FgModelIdentity id1 = new FgModelIdentity(model); 
        Module<?> m = cs.getFactorModule(id1, RealAlgebra.getInstance());
        Object o = m.forward();
        assertTrue(o instanceof LazyVarTensor);
        // We do not check that it correctly back propagates into the scores, since the lazy var
        // tensor will never be used in subsequent modules.
    }

    @Ignore("not implemented")
    @Test
    public void testCreateMessages() throws Exception {
        Algebra s = LogSemiring.getInstance();
        int n = 4;
        ConsecutiveSiblingsFactor f = getDefaultCs(getDefaultScores(s, n));
        
        // Create some arbitrary VarTensor[] as input messages.
        VarTensor[] inMsgs = getMsgsForFactor(f, s);
        // Fill input messages with arbitrary values.
        double val = 2;
        for (int i=0; i<inMsgs.length; i++) {
            for (int c=0; c<inMsgs[i].size(); c++){
                inMsgs[i].setValue(c, val++);
            }
        }
        // Fill output messages with NaN.
        VarTensor[] outMsgs1 = getMsgsForFactor(f, s);
        for (int i=0; i<outMsgs1.length; i++) {
            outMsgs1[i].fill(Double.NaN);
        }
        VarTensor[] outMsgs2 = getMsgsForFactor(f, s);
        for (int i=0; i<outMsgs2.length; i++) {
            outMsgs2[i].fill(Double.NaN);
        }

        // Compute the output messages by computing them explicitly.
        createMessagesFactorToVars(f, inMsgs, outMsgs1);
        // Compute the output messages using the GlobalFactor's efficient method.
        f.createMessages(inMsgs, outMsgs2);
        
        // Compare the result with the messages created by instantiating an exact factor.
        double tolerance = 1e-13;
        for (int i=0; i<outMsgs1.length; i++) {
            if (!outMsgs1[i].equals(outMsgs2[i], tolerance)) {
                assertEquals(outMsgs1[i], outMsgs2[i]);
            }
        }
    }
   
    /** Computes all the messages from a factor to its variables. */ 
    public static void createMessagesFactorToVars(Factor f, VarTensor[] inMsgs, VarTensor[] outMsgs) {
        Algebra s = inMsgs[0].getAlgebra();
        for (int i=0; i<inMsgs.length; i++) {
            assert inMsgs[i].getVars().size() == 1; 
            assert outMsgs[i].getVars().size() == 1;
            assert inMsgs[i].getVars().get(0) == outMsgs[i].getVars().get(0); 
            VarTensor prod = BruteForceInferencer.safeNewVarTensor(s, f);
            for (int j=0; j<inMsgs.length; j++) {
                if (i == j) { continue; }
                prod.prod(inMsgs[j]);
            }
            VarTensor msg = prod.getMarginal(new VarSet(inMsgs[i].getVars().get(0)), false);
            assert !msg.containsBadValues() : "msg = " + msg;
            outMsgs[i] = msg;
        }
    }

    /**
     * Gets a VarTensor[] representing the input or output messages for a factor. The order follows
     * that of the VarSet for the factor.
     */
    public static VarTensor[] getMsgsForFactor(Factor f, Algebra s) {
        int numVars = f.getVars().size();
        VarTensor[] inMsgs = new VarTensor[numVars];
        for (int i=0; i<numVars; i++) {
            Var v = f.getVars().get(i);
            inMsgs[i] = new VarTensor(s, new VarSet(v));
        }
        return inMsgs;
    }
    
    @Ignore("not implemented")
    @Test
    public void testCreateMessagesInInference() throws Exception {
        // Create a 5 word sentence.
        
        // Run BP once with the global factor wrapped in a non-global factor.
        
        // Run BP again with the global factor as normal.
        
        // Compare the results.
        throw new RuntimeException("not yet implemented");
    }
    
    @Ignore("not implemented")
    @Test
    public void testGetCreateMessagesModule() throws Exception {
        Algebra s = RealAlgebra.getInstance();
        FgModel model = new FgModel(100); // TODO: Correctly set number of parameters.
        FgModelIdentity mid1 = new FgModelIdentity(model);         
        final ConsecutiveSiblingsFactor cs = getDefaultCs();
        final Module<?> fm1 = cs.getFactorModule(mid1, s);
//        final Module<MVecArray<VarTensor>> vid1 = ; 
//        
//        // Test all semirings via finite differences.        
//        OneToOneFactory<MVecFgModel,LazyVarTensor> fact = new OneToOneFactory<MVecFgModel,LazyVarTensor>() {
//            public Module<LazyVarTensor> getModule(Module<MVecFgModel> m1) {
//                return cs.getCreateMessagesModule(vid1, fm1);
//            }
//        };
//        AbstractModuleTest.evalOneToOneByFiniteDiffs(fact, mid1);
        
        throw new RuntimeException("not yet implemented");
    }
    
    @Test
    public void testGetClamped() throws Exception {
        final ConsecutiveSiblingsFactor cs = getDefaultCs();
        {
            // Clamp no variables.
            Factor csClamped = cs.getClamped(new VarConfig());
            assertTrue(cs == csClamped);
        }{
            // Clamp all variables.
            VarConfig vc = new VarConfig();
            for (Var v : cs.getVars()) {
                vc.put(v, LinkVar.TRUE);
            }
            Factor csClamped = cs.getClamped(vc);
            assertTrue(csClamped.getVars().size() == 0);
        }
    }

    private static ConsecutiveSiblingsFactor getDefaultCs() {
        return getDefaultCs(getDefaultScores(RealAlgebra.getInstance(), 4));
    }
    
    private static Tensor getDefaultScores(Algebra s, int n) {
        Tensor scores = new Tensor(s, n+1, n+1, n+1);
        int i=2;
        for (int p=0; p<=n; p++) {
            for (int c=1; c<=n; c++) {
                for (int b=c+1; b<=n; b++) {
                    scores.set(s.fromReal(i++), p, c, b);
                }
            }
        }
        return scores;
    }
    
    private static ConsecutiveSiblingsFactor getDefaultCs(Tensor scores) {
        int n = scores.getDims()[0]-1;
        ProjDepTreeFactor pdtf = new ProjDepTreeFactor(n, VarType.PREDICTED);
        return new ConsecutiveSiblingsFactor(pdtf.getRootVars(), pdtf.getChildVars(), scores);
    }
    
}
