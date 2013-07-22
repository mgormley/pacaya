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
                    DenseFactor f;
                    if (i == -1) {
                        f = new DenseFactor(new VarSet(rootVars[j]));
                        f.setValue(LinkVar.TRUE, root[j]);
                        f.setValue(LinkVar.FALSE, 1.0);
                    } else {
                        f = new DenseFactor(new VarSet(childVars[i][j]));
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
