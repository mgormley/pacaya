package edu.jhu.autodiff.erma;

import java.util.List;

import edu.jhu.autodiff.AbstractTensorModule;
import edu.jhu.autodiff.Module;
import edu.jhu.autodiff.Tensor;
import edu.jhu.hypergraph.Hyperalgo;
import edu.jhu.hypergraph.Hyperedge;
import edu.jhu.hypergraph.Hypernode;
import edu.jhu.hypergraph.Hyperpotential;
import edu.jhu.hypergraph.Hyperalgo.HyperedgeDoubleFn;
import edu.jhu.hypergraph.Hyperalgo.Scores;
import edu.jhu.hypergraph.depparse.FirstOrderDepParseHypergraph;
import edu.jhu.hypergraph.depparse.FirstOrderDepParseHypergraph.PCBasicHypernode;
import edu.jhu.parse.dep.EdgeScores;
import edu.jhu.util.collections.Lists;
import edu.jhu.util.semiring.Algebra;

/**
 * Inside-outside dependency parsing.
 * 
 * The input to this module is expected to be a tensor containing the edge weights for dependency
 * parsing. The tensor is expected to be an nxn matrix, capable of being converted to EdgeScores
 * internally by EdgeScores.tensorToEdgeScores().
 * 
 * @author mgormley
 */
public class InsideOutsideDepParse extends AbstractTensorModule implements Module<Tensor> {
    
    public static final int BETA_IDX = 0;
    public static final int ALPHA_IDX = 1;
    public static final int ROOT_IDX = 2;
    
    private Module<Tensor> weightsIn;
    
    private Scores scores;
    // Cached for efficiency.
    private FirstOrderDepParseHypergraph graph;
    
    public InsideOutsideDepParse(Module<Tensor> weightsIn) {
        super(weightsIn.getAlgebra());
        this.weightsIn = weightsIn;
    }
    
    @Override
    public Tensor forward() {
        scores = new Scores();
        EdgeScores es = EdgeScores.tensorToEdgeScores(weightsIn.getOutput());            
        graph = new FirstOrderDepParseHypergraph(es.root, es.child, s);            
        Hyperpotential w = graph.getPotentials();
        Hyperalgo.insideAlgorithm(graph, w, s, scores);
        Hyperalgo.outsideAlgorithm(graph, w, s, scores);
        int n = graph.getNumTokens();
        
        y = new Tensor(s, 3, n, n);
        y.fill(s.zero());
        for (Hypernode node : graph.getNodes()) {
            if (node instanceof PCBasicHypernode) {   
                PCBasicHypernode pc = (PCBasicHypernode) node;
                int p = pc.getP();
                int c = pc.getC();
                int pp = EdgeScores.getTensorParent(p, c);
                
                int id = node.getId();
                y.set(scores.beta[id], BETA_IDX, pp, c);
                y.set(scores.alpha[id], ALPHA_IDX, pp, c);
            }
        }
        y.set(scores.beta[graph.getRoot().getId()], ROOT_IDX, 0, 0);
        return y;
    }

    @Override
    public void backward() {
        final int n = graph.getNumTokens();
        scores.alphaAdj = new double[graph.getNodes().size()];
        scores.betaAdj = new double[graph.getNodes().size()];
        // Update output adjoints in scores.
        for (Hypernode node : graph.getNodes()) {
            if (node instanceof PCBasicHypernode) {   
                PCBasicHypernode pc = (PCBasicHypernode) node;
                int p = pc.getP();
                int c = pc.getC();
                int pp = EdgeScores.getTensorParent(p, c);
                
                int id = node.getId();
                scores.alphaAdj[id] = yAdj.get(ALPHA_IDX, pp, c);
                scores.betaAdj[id] = yAdj.get(BETA_IDX, pp, c);
            }
        }
        scores.betaAdj[graph.getRoot().getId()] = yAdj.get(ROOT_IDX, 0, 0);

        // Run backward pass.
        Hyperpotential w = graph.getPotentials();
        Hyperalgo.outsideAdjoint(graph, w, s, scores);
        Hyperalgo.insideAdjoint(graph, w, s, scores);
        Hyperalgo.weightAdjoint(graph, w, s, scores);
        
        // Update input adjoints on weightsIn.
        final Tensor wAdj = weightsIn.getOutputAdj();
        HyperedgeDoubleFn lambda = new HyperedgeDoubleFn() {
            public void apply(Hyperedge e, double adj_w_e) {
                Hypernode head = e.getHeadNode();
                if (head instanceof PCBasicHypernode) {   
                    PCBasicHypernode pc = (PCBasicHypernode) head;
                    int p = pc.getP();
                    int c = pc.getC();
                    int pp = EdgeScores.getTensorParent(p, c);
                    wAdj.add(adj_w_e, pp, c);
                }
            }
        };
        Hyperalgo.weightAdjoint(graph, w, s, scores, lambda);
    }

    @Override
    public List<Module<Tensor>> getInputs() {
        return Lists.getList(weightsIn);
    }
    
}