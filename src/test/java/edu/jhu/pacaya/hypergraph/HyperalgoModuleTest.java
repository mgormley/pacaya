package edu.jhu.pacaya.hypergraph;

import org.junit.Test;

import edu.jhu.pacaya.autodiff.AbstractModuleTest;
import edu.jhu.pacaya.autodiff.AbstractModuleTest.OneToOneFactory;
import edu.jhu.pacaya.autodiff.Identity;
import edu.jhu.pacaya.autodiff.Module;
import edu.jhu.pacaya.autodiff.Tensor;
import edu.jhu.pacaya.hypergraph.HyperalgoModule.Output;
import edu.jhu.pacaya.util.semiring.RealAlgebra;


public class HyperalgoModuleTest {

    @Test
    public void testGradByFiniteDiffsAllSemirings() {
        Hypergraph graph;
        graph = HyperalgoTest.getThreeNodeGraph();
        helpGradByFiniteDiffsAllSemirings(graph, Output.INSIDE);
        helpGradByFiniteDiffsAllSemirings(graph, Output.OUTSIDE);
        helpGradByFiniteDiffsAllSemirings(graph, Output.MARGINALS);
        graph = HyperalgoTest.getTinyGraph();
        helpGradByFiniteDiffsAllSemirings(graph, Output.INSIDE);
        helpGradByFiniteDiffsAllSemirings(graph, Output.OUTSIDE);
        helpGradByFiniteDiffsAllSemirings(graph, Output.MARGINALS);
        graph = HyperalgoTest.getSimpleGraph();
        helpGradByFiniteDiffsAllSemirings(graph, Output.INSIDE);
        helpGradByFiniteDiffsAllSemirings(graph, Output.OUTSIDE);
        helpGradByFiniteDiffsAllSemirings(graph, Output.MARGINALS);
        graph = HyperalgoTest.getCkyGraph();
        helpGradByFiniteDiffsAllSemirings(graph, Output.INSIDE);
        helpGradByFiniteDiffsAllSemirings(graph, Output.OUTSIDE);
        helpGradByFiniteDiffsAllSemirings(graph, Output.MARGINALS);
    }

    protected void helpGradByFiniteDiffsAllSemirings(final Hypergraph graph, final Output end) {
        // The input must be a 1-d tensor which length equal to the number of hyperedges.
        Tensor tIn = new Tensor(RealAlgebra.getInstance(), graph.getNumEdges());
        final Identity<Tensor> modIn = new Identity<>(tIn);
        OneToOneFactory<Tensor,Tensor> fact = new OneToOneFactory<Tensor,Tensor>() {
            public Module<Tensor> getModule(Module<Tensor> m1) {
                return new HyperalgoModule(modIn, graph, end);
            }
        };
        AbstractModuleTest.evalOneToOneByFiniteDiffsAbs(fact, modIn);
    }
    
}
