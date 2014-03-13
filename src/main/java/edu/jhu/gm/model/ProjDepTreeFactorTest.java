package edu.jhu.gm.model;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import edu.jhu.gm.inf.BeliefPropagation;
import edu.jhu.gm.inf.BeliefPropagation.BeliefPropagationPrm;
import edu.jhu.gm.inf.BeliefPropagation.BpScheduleType;
import edu.jhu.gm.inf.BeliefPropagation.BpUpdateOrder;
import edu.jhu.gm.inf.BeliefPropagationTest;
import edu.jhu.gm.inf.BfsBpSchedule;
import edu.jhu.gm.inf.BruteForceInferencer;
import edu.jhu.gm.model.FactorGraph.FgEdge;
import edu.jhu.gm.model.ProjDepTreeFactor.LinkVar;
import edu.jhu.gm.model.Var.VarType;
import edu.jhu.gm.train.CrfObjective;
import edu.jhu.gm.train.CrfObjectiveTest;
import edu.jhu.prim.util.math.FastMath;
import edu.jhu.util.collections.Lists;
import edu.jhu.util.dist.Gaussian;

public class ProjDepTreeFactorTest {

    @Test
    public void testHasParentPerToken() {
        int n = 10;
        ProjDepTreeFactor treeFac = new ProjDepTreeFactor(n, VarType.PREDICTED);
        // Create vc for left branching tree.
        VarConfig vc = new VarConfig();
        for (int i=-1; i<n; i++) {
            for (int j=0; j<n; j++) {
                if (i == j) { continue; }
                vc.put(treeFac.getLinkVar(i, j), (i == j - 1) ? LinkVar.TRUE : LinkVar.FALSE);
            }
        }
        assertTrue(ProjDepTreeFactor.hasOneParentPerToken(n, vc));
        // Add two parents for token 3.
        vc.put(treeFac.getLinkVar(3, 6), LinkVar.TRUE);
        assertFalse(ProjDepTreeFactor.hasOneParentPerToken(n, vc));
        // No parents for token 3.
        vc.put(treeFac.getLinkVar(3, 6), LinkVar.FALSE);
        vc.put(treeFac.getLinkVar(3, 4), LinkVar.FALSE);
        assertFalse(ProjDepTreeFactor.hasOneParentPerToken(n, vc));
    }
    
    @Test
    public void testGetParents() {
        int n = 6;
        ProjDepTreeFactor treeFac = new ProjDepTreeFactor(n, VarType.PREDICTED);
        // Create vc for left branching tree.
        VarConfig vc = new VarConfig();
        for (int i=-1; i<n; i++) {
            for (int j=0; j<n; j++) {
                if (i == j) { continue; }
                vc.put(treeFac.getLinkVar(i, j), (i == j - 1) ? LinkVar.TRUE : LinkVar.FALSE);
            }
        }
        assertArrayEquals(new int[]{-1, 0, 1, 2, 3, 4}, ProjDepTreeFactor.getParents(n, vc));
    }
    
    @Test
    public void testGetScore() {
        // For n >= 5, we will hit an integer overflow.
        int n = 4;
        ProjDepTreeFactor treeFac = new ProjDepTreeFactor(n, VarType.PREDICTED);
        // Create vc for left branching tree.
        VarConfig vc = new VarConfig();
        for (int i=-1; i<n; i++) {
            for (int j=0; j<n; j++) {
                if (i == j) { continue; }
                vc.put(treeFac.getLinkVar(i, j), (i == j - 1) ? LinkVar.TRUE : LinkVar.FALSE);
            }
        }
        int vcid1 = vc.getConfigIndex();
        VarConfig vc2 = treeFac.getVars().getVarConfig(vcid1);
        int vcid2 = vc2.getConfigIndex();
        assertEquals(vcid1, vcid2);
        assertEquals(vc.getVars(), vc2.getVars());
        assertEquals(vc, vc2);
        // Not log domain.
        treeFac.updateFromModel(null, false);
        assertEquals(1.0, treeFac.getUnormalizedScore(vc.getConfigIndex()), 1e-13);
        // Add two parents for token 3.
        vc.put(treeFac.getLinkVar(1, 3), LinkVar.TRUE);
        assertEquals(0.0, treeFac.getUnormalizedScore(vc.getConfigIndex()), 1e-13);
        // No parents for token 3.
        vc.put(treeFac.getLinkVar(1, 3), LinkVar.FALSE);
        vc.put(treeFac.getLinkVar(1, 2), LinkVar.FALSE);
        assertEquals(0.0, treeFac.getUnormalizedScore(vc.getConfigIndex()), 1e-13);
    }
    
    @Test
    public void testPartitionFunctionWithoutUnaryFactorsProb() {
        partitionFunctionWithoutUnaryFactors(false);       
    }
    
    @Test
    public void testPartitionFunctionWithoutUnaryFactorsLogProb() {
        partitionFunctionWithoutUnaryFactors(true);
    }
    
    public void partitionFunctionWithoutUnaryFactors(boolean logDomain) {
        assertEquals(1, getNumberOfTreesByBruteForce(1, logDomain), 1e-13);
        assertEquals(2, getNumberOfTreesByBruteForce(2, logDomain), 1e-13);
        assertEquals(7, getNumberOfTreesByBruteForce(3, logDomain), 1e-13);
        //Slow: assertEquals(30, getNumberOfTreesByBruteForce(4, logDomain), 1e-13);
        
        assertEquals(1, getNumberOfTreesByBp(1, logDomain), 1e-13);
        assertEquals(2, getNumberOfTreesByBp(2, logDomain), 1e-13);
        assertEquals(7, getNumberOfTreesByBp(3, logDomain), 1e-13);
        assertEquals(30, getNumberOfTreesByBp(4, logDomain), 1e-13); // TODO: is this correct
        assertEquals(143, getNumberOfTreesByBp(5, logDomain), 1e-13); 
        assertEquals(728, getNumberOfTreesByBp(6, logDomain), 1e-13);
        
        assertEquals(1, getNumberOfTreesByLoopyBp(1, logDomain), 1e-13);
        assertEquals(2, getNumberOfTreesByLoopyBp(2, logDomain), 1e-13);
        assertEquals(7, getNumberOfTreesByLoopyBp(3, logDomain), 1e-13);
        assertEquals(30, getNumberOfTreesByLoopyBp(4, logDomain), 1e-13); 
        assertEquals(143, getNumberOfTreesByLoopyBp(5, logDomain), 1e-10); 
        assertEquals(728, getNumberOfTreesByLoopyBp(6, logDomain), 1e-10);
    }

    private double getNumberOfTreesByBruteForce(int n, boolean logDomain) {
        ProjDepTreeFactor treeFac = new ProjDepTreeFactor(n, VarType.PREDICTED);
        treeFac.updateFromModel(null, logDomain);
        FactorGraph fg = new FactorGraph();
        fg.addFactor(treeFac);
        
        BruteForceInferencer bf = new BruteForceInferencer(fg, logDomain);
        bf.run();
        if (logDomain) {
            return Math.exp(bf.getPartition());
        } else {
            return bf.getPartition();
        }
    }    

    private double getNumberOfTreesByBp(int n, boolean logDomain) {
        ProjDepTreeFactor treeFac = new ProjDepTreeFactor(n, VarType.PREDICTED);
        treeFac.updateFromModel(null, logDomain);
        FactorGraph fg = new FactorGraph();
        fg.addFactor(treeFac);
        
        BeliefPropagationPrm prm = new BeliefPropagationPrm();
        prm.maxIterations = 1;
        prm.logDomain = logDomain;
        prm.schedule = BpScheduleType.TREE_LIKE;
        prm.updateOrder = BpUpdateOrder.SEQUENTIAL;
        prm.normalizeMessages = false;
        BeliefPropagation bp = new BeliefPropagation(fg, prm);
        bp.run();
        if (logDomain) {
            return Math.exp(bp.getPartition());
        } else {
            return bp.getPartition();
        }
    }
    

    private double getNumberOfTreesByLoopyBp(int n, boolean logDomain) {
        ProjDepTreeFactor treeFac = new ProjDepTreeFactor(n, VarType.PREDICTED);
        treeFac.updateFromModel(null, logDomain);
        FactorGraph fg = new FactorGraph();
        fg.addFactor(treeFac);
        
        BeliefPropagationPrm prm = new BeliefPropagationPrm();
        prm.maxIterations = 1;
        prm.logDomain = logDomain;
        prm.schedule = BpScheduleType.TREE_LIKE;
        prm.updateOrder = BpUpdateOrder.SEQUENTIAL;
        prm.normalizeMessages = true;
        BeliefPropagation bp = new BeliefPropagation(fg, prm);
        bp.run();
        if (logDomain) {
            return Math.exp(bp.getPartition());
        } else {
            return bp.getPartition();
        }
    }
    
    // TODO: This and the next test are failing due to to an incorrect Bethe free energy computation.
    // It seems as though some edges have weight zero.
    @Test
    public void testResultingMarginalsProb() {
        boolean logDomain = false;        
        testResultMarginalsHelper(logDomain);        
    }

    @Test
    public void testResultingMarginalsLogProb() {
        boolean logDomain = true;        
        testResultMarginalsHelper(logDomain);        
    }

    private void testResultMarginalsHelper(boolean logDomain) {
        double[] root = new double[] {1, 2, 3}; 
        double[][] child = new double[][]{ {0, 4, 5}, {6, 0, 7}, {8, 9, 0} };
        
        // Create an edge factored dependency tree factor graph.
        //FactorGraph fg = getEdgeFactoredDepTreeFactorGraph(root, child);
        FactorGraph fg = new FactorGraph();
        int n = root.length;
        ProjDepTreeFactor treeFac = new ProjDepTreeFactor(n, VarType.PREDICTED);
        treeFac.updateFromModel(null, logDomain);
        LinkVar[] rootVars = treeFac.getRootVars();
        LinkVar[][] childVars = treeFac.getChildVars();
                
        // Add unary factors to each edge.
        for (int i=-1; i<n; i++) {
            for (int j=0; j<n; j++) {
                if (i != j) {
                    ExplicitFactor f;
                    if (i == -1) {
                        f = new ExplicitFactor(new VarSet(rootVars[j]));
                        f.setValue(LinkVar.TRUE, root[j]);
                        f.setValue(LinkVar.FALSE, 1.0);
                    } else {
                        f = new ExplicitFactor(new VarSet(childVars[i][j]));
                        f.setValue(LinkVar.TRUE, child[i][j]);
                        f.setValue(LinkVar.FALSE, 1.0);
                    }
                    if (logDomain) {
                        f.convertRealToLog();
                    }
                    fg.addFactor(f);
                }
            }
        }
        
        // Add this at the end, just to exercise the BFS schedule a bit more.
        fg.addFactor(treeFac);
                
        BeliefPropagationPrm prm = new BeliefPropagationPrm();
        prm.maxIterations = 1;
        prm.logDomain = logDomain;
        prm.schedule = BpScheduleType.TREE_LIKE;
        prm.updateOrder = BpUpdateOrder.SEQUENTIAL;
        prm.normalizeMessages = true;
        BeliefPropagation bp = new BeliefPropagation(fg, prm);
        bp.run();
        
        // Print schedule:
        BfsBpSchedule schedule = new BfsBpSchedule(fg);
        
        System.out.println();
        for (FgEdge edge : schedule.getOrder()) {
            System.out.println(edge.toString());
        }
        System.out.println();
        
        // Print marginals
        for (int i=-1; i<n; i++) {
            for (int j=0; j<n; j++) {
                if (i != j) {
                    System.out.format("%d %d: %.2f\n", i, j, getExpectedCount(bp, rootVars, childVars, logDomain, i, j));
                }
            }
        }

        // Check expected counts.
        double Z = 45+28+20+84+162+216+96;
        assertEquals((28+84)/Z, getExpectedCount(bp, rootVars, childVars, logDomain, 1, 2), 1e-3);
        assertEquals((45+162+216)/Z, getExpectedCount(bp, rootVars, childVars, logDomain, 2, 1), 1e-3);
        assertEquals((28+20+96)/Z, getExpectedCount(bp, rootVars, childVars, logDomain, 0, 1), 1e-3);
        assertEquals((96+216)/Z, getExpectedCount(bp, rootVars, childVars, logDomain, 2, 0), 1e-3);  
        
        assertEquals((45+28+20)/Z, getExpectedCount(bp, rootVars, childVars, logDomain, -1, 0), 1e-13);
        assertEquals((162+216+96)/Z, getExpectedCount(bp, rootVars, childVars, logDomain, -1, 2), 1e-3);

        // Check partition function.
        double[] trees = new double[] {45, 28, 20, 84, 162, 216, 96};
        double expectedRbar = 0;
        for (int t=0; t<trees.length; t++) {
            expectedRbar += trees[t] * FastMath.log(trees[t]);
        }
        System.out.println("expectedRbar: " + expectedRbar);
        assertEquals(45+28+20+84+162+216+96, logDomain ? FastMath.exp(bp.getPartition()) : bp.getPartition(), 1e-3);
        
        // Run brute force inference and compare.
        BruteForceInferencer bf = new BruteForceInferencer(fg, logDomain);
        bf.run();
        BeliefPropagationTest.assertEqualMarginals(fg, bf, bp, 1e-10);
    }

    @Test
    public void testPartitionWithAdditionalVariable() {
        boolean logDomain = false;        

        double[] root = new double[] {1, 2}; 
        double[][] child = new double[][]{ {0, 3}, {4, 0} };
        
        // Create an edge factored dependency tree factor graph.
        //FactorGraph fg = getEdgeFactoredDepTreeFactorGraph(root, child);
        FactorGraph fg = new FactorGraph();
        int n = root.length;
        ProjDepTreeFactor treeFac = new ProjDepTreeFactor(n, VarType.PREDICTED);
        treeFac.updateFromModel(null, logDomain);
        LinkVar[] rootVars = treeFac.getRootVars();
        LinkVar[][] childVars = treeFac.getChildVars();
                
        // Add unary factors to each edge.
        for (int i=-1; i<n; i++) {
            for (int j=0; j<n; j++) {
                if (i != j) {
                    ExplicitFactor f;
                    if (i == -1) {
                        f = new ExplicitFactor(new VarSet(rootVars[j]));
                        f.setValue(LinkVar.TRUE, root[j]);
                        f.setValue(LinkVar.FALSE, 1.0);
                    } else {
                        f = new ExplicitFactor(new VarSet(childVars[i][j]));
                        f.setValue(LinkVar.TRUE, child[i][j]);
                        f.setValue(LinkVar.FALSE, 1.0);
                    }
                    if (logDomain) {
                        f.convertRealToLog();
                    }
                    fg.addFactor(f);
                }
            }
        }
        
        // Add this at the end, just to exercise the BFS schedule a bit more.
        fg.addFactor(treeFac);
                
        // Add an extra variable over which we will marginalize.        
        Var roleVar = new Var(VarType.PREDICTED, 2, "Role_0_1", Lists.getList("arg0", "_"));
        ExplicitFactor roleFac = new ExplicitFactor(new VarSet(roleVar, childVars[0][1]));
        roleFac.setValue(0, 2);
        roleFac.setValue(1, 3);
        roleFac.setValue(2, 5);
        roleFac.setValue(3, 7);
        System.out.println(roleFac);
        if (logDomain) {
            roleFac.convertRealToLog();
        }
        fg.addFactor(roleFac);
        
        BeliefPropagationPrm prm = new BeliefPropagationPrm();
        prm.maxIterations = 1;
        prm.logDomain = logDomain;
        prm.schedule = BpScheduleType.TREE_LIKE;
        prm.updateOrder = BpUpdateOrder.SEQUENTIAL;
        prm.normalizeMessages = false;
        BeliefPropagation bp = new BeliefPropagation(fg, prm);
        bp.run();
        
        // Print schedule:
        BfsBpSchedule schedule = new BfsBpSchedule(fg);
        
        System.out.println();
        for (FgEdge edge : schedule.getOrder()) {
            System.out.println(edge.toString());
        }
        System.out.println();
        
        // Print marginals
        for (int i=-1; i<n; i++) {
            for (int j=0; j<n; j++) {
                if (i != j) {
                    System.out.format("%d %d: %.2f\n", i, j, getExpectedCount(bp, rootVars, childVars, logDomain, i, j));
                }
            }
        }

        double Z = 3*3 + 3*7 + 8*2 + 8*5;

        // Check partition function.
        assertEquals(Z, logDomain ? FastMath.exp(bp.getPartition()) : bp.getPartition(), 1e-3);
        // Check expected counts.
        assertEquals((3*3 + 3*7)/Z, getExpectedCount(bp, rootVars, childVars, logDomain, -1, 0), 1e-3);
        assertEquals((8*2 + 8*5)/Z, getExpectedCount(bp, rootVars, childVars, logDomain, 1, 0), 1e-3);

        // Run brute force inference and compare.
        BruteForceInferencer bf = new BruteForceInferencer(fg, logDomain);
        bf.run();
        BeliefPropagationTest.assertEqualMarginals(fg, bf, bp);
    }


    @Test
    public void testPartitionWithAllOnes() {
        boolean logDomain = false;        

        double[] root = new double[] {1, 1}; 
        double[][] child = new double[][]{ {1, 1}, {1, 1} };

        Var roleVar = new Var(VarType.PREDICTED, 2, "Role_1_0", Lists.getList("arg0", "_"));

        // Create an edge factored dependency tree factor graph.
        //FactorGraph fg = getEdgeFactoredDepTreeFactorGraph(root, child);
        FactorGraph fg = new FactorGraph();
        int n = root.length;
        ProjDepTreeFactor treeFac = new ProjDepTreeFactor(n, VarType.PREDICTED);
        treeFac.updateFromModel(null, logDomain);
        LinkVar[] rootVars = treeFac.getRootVars();
        LinkVar[][] childVars = treeFac.getChildVars();
              
        // Add unary factors to each edge.
        for (int i=-1; i<n; i++) {
            for (int j=0; j<n; j++) {
                if (i != j) {
                    ExplicitFactor f;
                    if (i == -1) {
                        f = new ExplicitFactor(new VarSet(rootVars[j]));
                        f.setValue(LinkVar.TRUE, root[j]);
                        f.setValue(LinkVar.FALSE, 1.0);
                    } else {
                        f = new ExplicitFactor(new VarSet(childVars[i][j]));
                        f.setValue(LinkVar.TRUE, child[i][j]);
                        f.setValue(LinkVar.FALSE, 1.0);
                    }
                    if (logDomain) {
                        f.convertRealToLog();
                    }
                    fg.addFactor(f);
                }
            }
        }

        // Add this at the end, just to exercise the BFS schedule a bit more.
        fg.addFactor(treeFac);
                
        // Add an extra variable over which we will marginalize.        
        ExplicitFactor roleLinkFac = new ExplicitFactor(new VarSet(childVars[1][0], roleVar));
        roleLinkFac.setValue(0, 1);
        roleLinkFac.setValue(1, 1);
        roleLinkFac.setValue(2, 1);
        roleLinkFac.setValue(3, 1);
        System.out.println(roleLinkFac);
        if (logDomain) {
            roleLinkFac.convertRealToLog();
        }
        fg.addFactor(roleLinkFac);
        ExplicitFactor roleFac = new ExplicitFactor(new VarSet(roleVar));
        roleFac.set(1.0);
        if (logDomain) {
            roleFac.convertRealToLog();
        }
        fg.addFactor(roleFac);
        
        BeliefPropagationPrm prm = new BeliefPropagationPrm();
        prm.maxIterations = 1;
        prm.logDomain = logDomain;
        prm.schedule = BpScheduleType.TREE_LIKE;
        prm.updateOrder = BpUpdateOrder.SEQUENTIAL;
        prm.normalizeMessages = false;
        BeliefPropagation bp = new BeliefPropagation(fg, prm);
        bp.run();        
        
        // Print marginals
        for (int i=-1; i<n; i++) {
            for (int j=0; j<n; j++) {
                if (i != j) {
                    System.out.format("%d %d: %.2f\n", i, j, getExpectedCount(bp, rootVars, childVars, logDomain, i, j));
                }
            }
        }
        
        // Print schedule:
        BfsBpSchedule schedule = new BfsBpSchedule(fg);        
        System.out.println();
        for (FgEdge edge : schedule.getOrder()) {
            System.out.println(edge.toString());
        }
        System.out.println();
        
        // Print factors
        for (Factor f : fg.getFactors()) {
            System.out.println(f);
        }
        
        double Z = 4;
        // Check partition function.
        assertEquals(Z, logDomain ? FastMath.exp(bp.getPartition()) : bp.getPartition(), 1e-3);
        for (Var v : fg.getVars()) {
            double partition = bp.getPartitionFunctionAtVarNode(fg.getNode(v));
            System.out.format("Var=%s partition=%.4f\n", v.toString(), partition);
            assertEquals(Z, logDomain ? FastMath.exp(partition) : partition, 1e-3);
        }
        // Check expected counts.
        System.out.println(getExpectedCount(bp, rootVars, childVars, logDomain, -1, 0));
        assertEquals(2/Z, getExpectedCount(bp, rootVars, childVars, logDomain, -1, 0), 1e-3);
        assertEquals(2/Z, getExpectedCount(bp, rootVars, childVars, logDomain, 1, 0), 1e-3);

        // Run brute force inference and compare.
        BruteForceInferencer bf = new BruteForceInferencer(fg, logDomain);
        bf.run();
        BeliefPropagationTest.assertEqualMarginals(fg, bf, bp);
    }
    
    private double getExpectedCount(BeliefPropagation bp, LinkVar[] rootVars, LinkVar[][] childVars, boolean logDomain, int i, int j) {
        DenseFactor marg;
        if (i == -1) {
            marg = bp.getMarginals(rootVars[j]);
        } else {
            marg = bp.getMarginals(childVars[i][j]);
        }        
        return logDomain ? FastMath.exp(marg.getValue(LinkVar.TRUE)) : marg.getValue(LinkVar.TRUE);
    }

}
