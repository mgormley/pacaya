package edu.jhu.pacaya.autodiff.erma;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import edu.jhu.pacaya.autodiff.erma.ErmaBp.ErmaBpPrm;
import edu.jhu.pacaya.gm.inf.Messages;
import edu.jhu.pacaya.gm.inf.BeliefPropagation.BpUpdateOrder;
import edu.jhu.pacaya.gm.model.ExplicitFactor;
import edu.jhu.pacaya.gm.model.FactorGraph;
import edu.jhu.pacaya.gm.model.VarConfig;
import edu.jhu.pacaya.gm.model.VarSet;
import edu.jhu.pacaya.gm.model.VarTensor;
import edu.jhu.pacaya.gm.model.FactorGraph.FgEdge;
import edu.jhu.pacaya.gm.model.Var.VarType;
import edu.jhu.pacaya.gm.model.globalfac.LinkVar;
import edu.jhu.pacaya.gm.model.globalfac.ProjDepTreeFactor;
import edu.jhu.pacaya.gm.model.globalfac.ProjDepTreeFactorTest.FgAndLinks;
import edu.jhu.pacaya.util.semiring.Algebra;
import edu.jhu.pacaya.util.semiring.LogSignAlgebra;
import edu.jhu.pacaya.util.semiring.RealAlgebra;
import edu.jhu.prim.Primitives;
import edu.jhu.prim.arrays.DoubleArrays;

/**
 * This test is a derivative of
 * ErmaProjDepTreeFactorTest.testErmaCompareMessagesWithExplicitTreeFactor().
 * 
 * The point of this test is to find the simplest possible case that could break the projective
 * dependency parsing tree factor's dynamic programming approach to the adjoint computations.
 */
public class SimplerErmaProjDepTreeFactorTest {

    @Test
    public void testErmaCompareMessagesWithExplicitTreeFactor() {
        compareErmaMessagesWithExplicitTreeFactor(RealAlgebra.getInstance(), true, false);
        compareErmaMessagesWithExplicitTreeFactor(RealAlgebra.getInstance(), true, true);
        compareErmaMessagesWithExplicitTreeFactor(LogSignAlgebra.getInstance(), true, false);
        compareErmaMessagesWithExplicitTreeFactor(LogSignAlgebra.getInstance(), true, true);
    }

    public void compareErmaMessagesWithExplicitTreeFactor(Algebra s, boolean normalizeMessages, boolean makeLoopy) {
        ErmaBpPrm prm = new ErmaBpPrm();
        prm.s = s;
        prm.updateOrder = BpUpdateOrder.PARALLEL;
        prm.maxIterations = 1;
        prm.normalizeMessages = normalizeMessages;

        FactorGraph fgExpl = get2WordSentFactorGraph(true, makeLoopy);
        ErmaBp bpExpl = new ErmaBp(fgExpl, prm);
        bpExpl.forward();
        // printMessages(fgExpl, bpExpl);

        FactorGraph fgDp = get2WordSentFactorGraph(false, makeLoopy);
        ErmaBp bpDp = new ErmaBp(fgDp, prm);
        bpDp.forward();
        // printMessages(fgDp, bpDp);

        System.out.println("Messages");
        assertEqualMessages(fgExpl, bpExpl.getMessages(), bpDp.getMessages());
        System.out.println("Beliefs");
        assertEqualVarTensors(bpExpl.getOutput().varBeliefs, bpDp.getOutput().varBeliefs);
        assertEqualVarTensors(bpExpl.getOutput().facBeliefs, bpDp.getOutput().facBeliefs);
        System.out.println("Partition: " + bpExpl.getPartition());
        System.out.println("Partition: " + bpDp.getPartition());
        assertEquals(bpExpl.getLogPartition(), bpDp.getLogPartition(), 1e-10);

        for (int v = 0; v < fgDp.getNumVars(); v++) {
            LinkVar link = (LinkVar) fgDp.getVar(v);
            double adj = 0.0;
            if ((link.getParent() == -1 && link.getChild() == 1) || (link.getParent() == 1 && link.getChild() == 0)) {
                adj = 1.0;
            }
            bpExpl.getOutputAdj().varBeliefs[v].setValue(LinkVar.TRUE, adj);
            bpDp.getOutputAdj().varBeliefs[v].setValue(LinkVar.TRUE, adj);
        }
        System.out.println("------Expl Backward-----");
        bpExpl.backward();
        System.out.println("------DP Backward-----");
        bpDp.backward();
        System.out.println("Adjoints");
        assertEqualMessages(fgExpl, bpExpl.getMessagesAdj(), bpDp.getMessagesAdj());
        assertEqualVarTensors(bpExpl.getPotentialsAdj(), bpDp.getPotentialsAdj());
    }

    private void assertEqualMessages(FactorGraph fgExpl, Messages[] msgsExpl, Messages[] msgsDp) {
        for (int i = 0; i < msgsExpl.length; i++) {
            // NOTE: These are NEW messages which in the case of the adjoints will be incorrect if
            // not computed correctly in the case of the adjoints.
            VarTensor msgExpl = msgsExpl[i].newMessage;
            VarTensor msgDp = msgsDp[i].newMessage;
            assertEquals(msgExpl.size(), msgDp.size());
            for (int c = 0; c < msgExpl.size(); c++) {
                if (msgDp.getValue(c) == Double.NEGATIVE_INFINITY // && msgExpl.getValue(c) < -30
                        || msgExpl.getValue(c) == Double.NEGATIVE_INFINITY) {// && msgDp.getValue(c)
                                                                             // < -30) {
                    // continue;
                }

                if (!Primitives.equals(msgExpl.getValue(c), msgDp.getValue(c), 1e-13)) {
                    System.out.println("NOT EQUAL:");
                    System.out.println(fgExpl.getEdge(i));
                    System.out.println(msgExpl);
                    System.out.println(msgDp);
                }
                assertEquals(msgExpl.getValue(c), msgDp.getValue(c), 1e-13);
            }
            // TODO: This doesn't work because the vars aren't the same:
            // assertTrue(msgExpl.equals(msgDp, 1e-5));
        }
    }

    private void assertEqualVarTensors(VarTensor[] msgsExpl, VarTensor[] msgsDp) {
        for (int i = 0; i < msgsExpl.length; i++) {
            if (msgsExpl[i] == null || msgsDp[i] == null) {
                // Don't compare the potentials for the projective dependency tree factor.
                continue;
            }
            VarTensor msgExpl = msgsExpl[i];
            VarTensor msgDp = msgsDp[i];
            assertEquals(msgExpl.size(), msgDp.size());
            for (int c = 0; c < msgExpl.size(); c++) {
                if (msgDp.getValue(c) == Double.NEGATIVE_INFINITY // && msgExpl.getValue(c) < -30
                        || msgExpl.getValue(c) == Double.NEGATIVE_INFINITY) {// && msgDp.getValue(c)
                                                                             // < -30) {
                    // continue;
                }

                if (!Primitives.equals(msgExpl.getValue(c), msgDp.getValue(c), 1e-13)) {
                    System.out.println("NOT EQUAL:");
                    System.out.println(msgExpl);
                    System.out.println(msgDp);
                }
                assertEquals(msgExpl.getValue(c), msgDp.getValue(c), 1e-13);
            }
            // TODO: This doesn't work because the vars aren't the same:
            // assertTrue(msgExpl.equals(msgDp, 1e-5));
        }
    }

    private void printMessages(FactorGraph fg, Messages[] msgs) {
        for (int i = 0; i < fg.getNumEdges(); i++) {
            FgEdge edge = fg.getEdge(i);
            // if (edge.isVarToFactor() && edge.getFactor().getVars().size() == 4) {
            System.out.println(edge);
            System.out.println(msgs[i].message);
            System.out.println("Log odds: " + (msgs[i].message.getValue(1) - msgs[i].message.getValue(0)));
            // }
        }
    }

    public static FactorGraph get2WordSentFactorGraph(boolean useExplicitTreeFactor, boolean makeLoopy) {
        return get2WordSentFactorGraph(useExplicitTreeFactor, makeLoopy, false);
    }

    public static FactorGraph get2WordSentFactorGraph(boolean useExplicitTreeFactor, boolean makeLoopy,
            boolean negInfEdgeWeight) {
        return get2WordSentFgAndLinks(useExplicitTreeFactor, makeLoopy, negInfEdgeWeight).fg;
    }

    public static FgAndLinks get2WordSentFgAndLinks(boolean useExplicitTreeFactor, boolean makeLoopy,
            boolean negInfEdgeWeight) {
        // These are the log values, not the exp.
        double[] root = new double[] { 8.571183, 89.720164 };
        double[][] child = new double[][] { { 0, 145.842585 }, { 23.451215, 0 } };
        // TODO: These scaling factors are added to avoid the floating point error in some of the
        // tests above. This should really have multiple tests with and without the floating point
        // error.
        DoubleArrays.scale(root, .1);
        DoubleArrays.scale(child, .1);

        // For random values:
        // Prng.seed(14423444);
        // root = DoubleArrays.getLog(ModuleTestUtils.getAbsZeroOneGaussian(2).toNativeArray());
        // child[0] = DoubleArrays.getLog(ModuleTestUtils.getAbsZeroOneGaussian(2).toNativeArray());
        // child[1] = DoubleArrays.getLog(ModuleTestUtils.getAbsZeroOneGaussian(2).toNativeArray());

        if (negInfEdgeWeight) {
            child[0][1] = Double.NEGATIVE_INFINITY;
        }

        // Create an edge factored dependency tree factor graph.
        // FactorGraph fg = getEdgeFactoredDepTreeFactorGraph(root, child);
        FactorGraph fg = new FactorGraph();
        int n = root.length;
        ProjDepTreeFactor treeFac = new ProjDepTreeFactor(n, VarType.PREDICTED);
        treeFac.updateFromModel(null);
        LinkVar[] rootVars = treeFac.getRootVars();
        LinkVar[][] childVars = treeFac.getChildVars();

        // Add unary factors to each edge.
        for (int i = -1; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i != j) {
                    ExplicitFactor f;
                    if (i == -1) {
                        f = new ExplicitFactor(new VarSet(rootVars[j]));
                        f.setValue(LinkVar.TRUE, root[j]);
                        f.setValue(LinkVar.FALSE, 0.0);
                    } else {
                        f = new ExplicitFactor(new VarSet(childVars[i][j]));
                        f.setValue(LinkVar.TRUE, child[i][j]);
                        f.setValue(LinkVar.FALSE, 0.0);
                    }
                    // f.scale(0.01);
                    // TODO: REMOVE THIS COMMENT fg.addFactor(f);
                }
            }
        }

        if (makeLoopy) {
            ExplicitFactor f = new ExplicitFactor(new VarSet(rootVars[0], rootVars[1]));
            f.setValue(3, -DoubleArrays.sum(root));
            // TODO: REMOVE THIS COMMENT fg.addFactor(f);
            // f.scale(0.01);
        }

        if (useExplicitTreeFactor) {
            ExplicitFactor f = new ExplicitFactor(
                    new VarSet(rootVars[0], rootVars[1], childVars[0][1], childVars[1][0]));
            f.fill(Double.NEGATIVE_INFINITY);
            VarConfig vc = new VarConfig();
            vc.put(rootVars[0], LinkVar.TRUE);
            vc.put(rootVars[1], LinkVar.FALSE);
            vc.put(childVars[0][1], LinkVar.TRUE);
            vc.put(childVars[1][0], LinkVar.FALSE);
            f.setValue(vc.getConfigIndex(), 0.0);
            vc = new VarConfig();
            vc.put(rootVars[0], LinkVar.FALSE);
            vc.put(rootVars[1], LinkVar.TRUE);
            vc.put(childVars[0][1], LinkVar.FALSE);
            vc.put(childVars[1][0], LinkVar.TRUE);
            f.setValue(vc.getConfigIndex(), 0.0);
            fg.addFactor(f);
        } else {
            fg.addFactor(treeFac);
        }
        return new FgAndLinks(fg, rootVars, childVars, 2);
    }

}
