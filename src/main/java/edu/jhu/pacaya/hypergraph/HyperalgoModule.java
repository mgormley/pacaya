package edu.jhu.pacaya.hypergraph;

import java.util.List;

import edu.jhu.pacaya.autodiff.AbstractModule;
import edu.jhu.pacaya.autodiff.MVec;
import edu.jhu.pacaya.autodiff.Module;
import edu.jhu.pacaya.autodiff.Tensor;
import edu.jhu.pacaya.hypergraph.Hyperalgo.HyperedgeDoubleFn;
import edu.jhu.pacaya.hypergraph.Hyperalgo.Scores;
import edu.jhu.pacaya.util.collections.QLists;
import edu.jhu.pacaya.util.semiring.Semiring;

public class HyperalgoModule extends AbstractModule<Tensor> implements Module<Tensor> {

    public enum Output { 
        INSIDE(1), OUTSIDE(2), MARGINALS(3);
        int rank; Output(int rank) { this.rank = rank; }
    }
    
    private final Module<Tensor> modIn;
    private final Hypergraph graph;
    private final Output end;
    private Scores sc;
    private Hyperpotential w;

    public HyperalgoModule(Module<Tensor> modIn, Hypergraph graph) {
        this(modIn, graph, Output.MARGINALS);
    }
    
    public HyperalgoModule(final Module<Tensor> modIn, Hypergraph graph, Output end) {
        super(modIn.getAlgebra());
        this.modIn = modIn;
        this.graph = graph;
        this.end = end;
    }

    @Override
    public Tensor forward() {
        assert modIn.getOutput().size() == graph.getNumEdges();
        this.w = new Hyperpotential() {            
            @Override
            public double getScore(Hyperedge e, Semiring s) {
                assert s.equals(modIn.getAlgebra());
                return modIn.getOutput().get(e.getId());
            }
        };
        y = new Tensor(s, end.rank, graph.getNodes().size());
        sc = new Scores();
        Hyperalgo.insideAlgorithm(graph, w, s, sc);
        setTensorValues(y, 0, sc.beta);
        if (end == Output.INSIDE) {
            return y;
        }
        Hyperalgo.outsideAlgorithm(graph, w, s, sc);
        setTensorValues(y, 1, sc.alpha);
        if (end == Output.OUTSIDE) {
            return y;
        }
        Hyperalgo.marginals(graph, w, s, sc);
        setTensorValues(y, 2, sc.marginal);
        return y;
    }
    
    @Override
    public void backward() {
        // Copy adjoints into the arrays.
        if (Output.MARGINALS.rank <= end.rank) {
            sc.marginalAdj = new double[sc.marginal.length];
            setArrayValues(yAdj, 2, sc.marginalAdj);
        }
        if (Output.OUTSIDE.rank <= end.rank) {
            sc.alphaAdj = new double[sc.alpha.length];
            setArrayValues(yAdj, 1, sc.alphaAdj);
        }
        sc.betaAdj = new double[sc.beta.length];
        setArrayValues(yAdj, 0, sc.betaAdj);
        
        // Run backward pass.
        HyperedgeDoubleFn lambda = new HyperedgeDoubleFn() {            
            @Override
            public void apply(Hyperedge e, double val) {
                modIn.getOutputAdj().set(val, e.getId());
            }
        };
        if (Output.MARGINALS.rank <= end.rank) {
            Hyperalgo.backward(graph, w, s, sc, lambda);
        } else if (Output.OUTSIDE.rank <= end.rank) {
            Hyperalgo.insideOutsideBackward(graph, w, s, sc, lambda);
        } else {
            Hyperalgo.insideBackward(graph, w, s, sc, lambda);
        }
    }

    public static void setTensorValues(Tensor y, int d1, double[] values) {
        if (values.length != y.getDim(1)) {
            throw new IllegalArgumentException("Invalid dimensions");
        }
        for (int d2=0; d2<values.length; d2++) {
            y.set(values[d2], d1, d2);
        }
    }

    public static void setArrayValues(Tensor y, int d1, double[] values) {
        if (values.length != y.getDim(1)) {
            throw new IllegalArgumentException("Invalid dimensions");
        }
        for (int d2=0; d2<values.length; d2++) {
            values[d2] = y.get(d1, d2);
        }
    }

    @Override
    public List<? extends Module<? extends MVec>> getInputs() {
        return QLists.getList(modIn);
    }

}
