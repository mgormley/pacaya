package edu.jhu.gm;

import static org.junit.Assert.*;

import org.junit.Test;

import edu.jhu.gm.BeliefPropagation.BeliefPropagationPrm;
import edu.jhu.gm.BeliefPropagation.BpScheduleType;
import edu.jhu.gm.BeliefPropagation.BpUpdateOrder;
import edu.jhu.gm.FactorGraph.FgEdge;
import edu.jhu.gm.ProjDepTreeFactor.LinkVar;
import edu.jhu.gm.Var.VarType;
import edu.jhu.parse.dep.ProjectiveDependencyParser;
import edu.jhu.parse.dep.ProjectiveDependencyParser.DepIoChart;
import edu.jhu.srl.SrlFactorGraph;
import edu.jhu.srl.SrlFactorGraph.SrlFactorTemplate;
import edu.jhu.util.Utilities;
import edu.jhu.util.math.Vectors;

public class ProjDepTreeFactorTest {

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
        LinkVar[] rootVars = treeFac.getRootVars();
        LinkVar[][] childVars = treeFac.getChildVars();
                
        // Add unary factors to each edge.
        for (int i=-1; i<n; i++) {
            for (int j=0; j<n; j++) {
                if (i != j) {
                    ExpFamFactor f;
                    if (i == -1) {
                        f = new ExpFamFactor(new VarSet(rootVars[j]), SrlFactorTemplate.LINK_UNARY);
                        f.setValue(LinkVar.TRUE, root[j]);
                        f.setValue(LinkVar.FALSE, 1.0);
                    } else {
                        f = new ExpFamFactor(new VarSet(childVars[i][j]), SrlFactorTemplate.LINK_UNARY);
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
        prm.normalizeMessages = false;
        BeliefPropagation bp = new BeliefPropagation(fg, prm);
        bp.run();
        bp.clear();
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
        assertEquals(45+28+20+84+162+216+96, logDomain ? Utilities.exp(bp.getPartition()) : bp.getPartition(), 1e-3);
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
        LinkVar[] rootVars = treeFac.getRootVars();
        LinkVar[][] childVars = treeFac.getChildVars();
                
        // Add unary factors to each edge.
        for (int i=-1; i<n; i++) {
            for (int j=0; j<n; j++) {
                if (i != j) {
                    ExpFamFactor f;
                    if (i == -1) {
                        f = new ExpFamFactor(new VarSet(rootVars[j]), SrlFactorTemplate.LINK_UNARY);
                        f.setValue(LinkVar.TRUE, root[j]);
                        f.setValue(LinkVar.FALSE, 1.0);
                    } else {
                        f = new ExpFamFactor(new VarSet(childVars[i][j]), SrlFactorTemplate.LINK_UNARY);
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
        Var roleVar = new Var(VarType.PREDICTED, 2, "Role_0_1", Utilities.getList("arg0", "_"));
        ExpFamFactor roleFac = new ExpFamFactor(new VarSet(roleVar, childVars[0][1]), SrlFactorTemplate.LINK_ROLE_BINARY);
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
        assertEquals(Z, logDomain ? Utilities.exp(bp.getPartition()) : bp.getPartition(), 1e-3);
        // Check expected counts.
        assertEquals((3*3 + 3*7)/Z, getExpectedCount(bp, rootVars, childVars, logDomain, -1, 0), 1e-3);
        assertEquals((8*2 + 8*5)/Z, getExpectedCount(bp, rootVars, childVars, logDomain, 1, 0), 1e-3);

    }


    @Test
    public void testPartitionWithAllOnes() {
        boolean logDomain = false;        

        double[] root = new double[] {1, 1}; 
        double[][] child = new double[][]{ {1, 1}, {1, 1} };

        Var roleVar = new Var(VarType.PREDICTED, 2, "Role_1_0", Utilities.getList("arg0", "_"));

        // Create an edge factored dependency tree factor graph.
        //FactorGraph fg = getEdgeFactoredDepTreeFactorGraph(root, child);
        FactorGraph fg = new FactorGraph();
        int n = root.length;
        ProjDepTreeFactor treeFac = new ProjDepTreeFactor(n, VarType.PREDICTED);
        LinkVar[] rootVars = treeFac.getRootVars();
        LinkVar[][] childVars = treeFac.getChildVars();
              
        // Add unary factors to each edge.
        for (int i=-1; i<n; i++) {
            for (int j=0; j<n; j++) {
                if (i != j) {
                    ExpFamFactor f;
                    if (i == -1) {
                        f = new ExpFamFactor(new VarSet(rootVars[j]), SrlFactorTemplate.LINK_UNARY);
                        f.setValue(LinkVar.TRUE, root[j]);
                        f.setValue(LinkVar.FALSE, 1.0);
                    } else {
                        f = new ExpFamFactor(new VarSet(childVars[i][j]), SrlFactorTemplate.LINK_UNARY);
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
        ExpFamFactor roleLinkFac = new ExpFamFactor(new VarSet(childVars[1][0], roleVar), SrlFactorTemplate.LINK_ROLE_BINARY);
        roleLinkFac.setValue(0, 1);
        roleLinkFac.setValue(1, 1);
        roleLinkFac.setValue(2, 1);
        roleLinkFac.setValue(3, 1);
        System.out.println(roleLinkFac);
        if (logDomain) {
            roleLinkFac.convertRealToLog();
        }
        fg.addFactor(roleLinkFac);
        ExpFamFactor roleFac = new ExpFamFactor(new VarSet(roleVar), 1.0);
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
        assertEquals(Z, logDomain ? Utilities.exp(bp.getPartition()) : bp.getPartition(), 1e-3);
        for (Var v : fg.getVars()) {
            double partition = bp.getPartitionFunctionAtVarNode(fg.getNode(v));
            System.out.format("Var=%s partition=%.4f\n", v.toString(), partition);
            assertEquals(Z, logDomain ? Utilities.exp(partition) : partition, 1e-3);
        }
        // Check expected counts.
        System.out.println(getExpectedCount(bp, rootVars, childVars, logDomain, -1, 0));
        assertEquals(2/Z, getExpectedCount(bp, rootVars, childVars, logDomain, -1, 0), 1e-3);
        assertEquals(2/Z, getExpectedCount(bp, rootVars, childVars, logDomain, 1, 0), 1e-3);

    }
    
    private double getExpectedCount(BeliefPropagation bp, LinkVar[] rootVars, LinkVar[][] childVars, boolean logDomain, int i, int j) {
        DenseFactor marg;
        if (i == -1) {
            marg = bp.getMarginals(rootVars[j]);
        } else {
            marg = bp.getMarginals(childVars[i][j]);
        }        
        return logDomain ? Utilities.exp(marg.getValue(LinkVar.TRUE)) : marg.getValue(LinkVar.TRUE);
    }

}
