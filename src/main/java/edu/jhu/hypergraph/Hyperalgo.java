package edu.jhu.hypergraph;

import java.io.PrintStream;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.jhu.hypergraph.Hypergraph.HyperedgeFn;
import edu.jhu.util.semiring.Algebra;
import edu.jhu.util.semiring.Semiring;

public class Hyperalgo {

    private static final Logger log = LoggerFactory.getLogger(Hyperalgo.class);
    
    private Hyperalgo() {
        // Private constructor.
    }
    
    public static void forward(final Hypergraph graph, final Hyperpotential w, final Algebra s,
            final Scores scores) {
        insideAlgorithm(graph, w, s, scores);
        outsideAlgorithm(graph, w, s, scores);
        marginals(graph, w, s, scores);
    }
    
    public static void backward(final Hypergraph graph, final Hyperpotential w, final Algebra s,
            final Scores scores) {
        if (scores.marginalAdj == null) {
            throw new IllegalStateException("scores.marginalAdj must be non-null.");
        }
        marginalsAdjoint(graph, w, s, scores);
        outsideAdjoint(graph, w, s, scores);
        insideAdjoint(graph, w, s, scores);
        weightAdjoint(graph, w, s, scores);
    }
    
    /**
     * Runs the inside algorithm on a hypergraph.
     * 
     * @param graph The hypergraph.
     * @return The beta value for each Hypernode. Where beta[i] is the inside
     *         score for the i'th node in the Hypergraph, graph.getNodes().get(i).
     */
    public static double[] insideAlgorithm(final Hypergraph graph, final Hyperpotential w, final Semiring s) {
        final int n = graph.getNodes().size();
        final double[] beta = new double[n];
        // \beta_i = 0 \forall i
        Arrays.fill(beta, s.zero());
        graph.applyTopoSort(new HyperedgeFn() {

            @Override
            public void apply(Hyperedge e) {
                // \beta_{H(e)} += w_e \prod_{j \in T(e)} \beta_j
                double prod = s.one();
                for (Hypernode jNode : e.getTailNodes()) {
                    prod = s.times(prod, beta[jNode.getId()]);
                }
                int i = e.getHeadNode().getId();
                prod = s.times(w.getScore(e, s), prod);
                beta[i] = s.plus(beta[i], prod);
                //if (log.isTraceEnabled()) { log.trace(String.format("inside: %s w_e=%f prod=%f beta[%d] = %f", e.getLabel(), ((Algebra)s).toReal(w.getScore(e, s)), prod, i, ((Algebra)s).toReal(beta[i]))); }
            }
            
        });
        return beta;
    }
    
    /**
     * Runs the outside algorithm on a hypergraph.
     * 
     * @param graph The hypergraph
     * @param w The potential function.
     * @param s The semiring.
     * @param beta The beta value for each Hypernode. Where beta[i] is the inside
     *         score for the i'th node in the Hypergraph, graph.getNodes().get(i).
     * @return The alpha value for each Hypernode. Where alpha[i] is the outside
     *         score for the i'th node in the Hypergraph, graph.getNodes().get(i).
     */
    public static double[] outsideAlgorithm(final Hypergraph graph, final Hyperpotential w, final Semiring s, final double[] beta) {
        final int n = graph.getNodes().size();
        final double[] alpha = new double[n];
        // \alpha_i = 0 \forall i
        Arrays.fill(alpha, s.zero());
        // \alpha_{root} = 1
        alpha[graph.getRoot().getId()] = s.one();
        graph.applyRevTopoSort(new HyperedgeFn() {
            
            @Override
            public void apply(Hyperedge e) {
                int i = e.getHeadNode().getId();
                // \forall j \in T(e): 
                // \alpha_j += \alpha_{H(e)} * w_e * \prod_{k \in T(e) : k \neq j} \beta_k
                for (Hypernode jNode : e.getTailNodes()) {
                    int j = jNode.getId();
                    double prod = s.one();
                    for (Hypernode kNode : e.getTailNodes()) {
                        int k = kNode.getId();
                        if (k == j) { continue; }
                        prod = s.times(prod, beta[k]);
                    }
                    prod = s.times(prod, alpha[i]);
                    prod = s.times(prod, w.getScore(e, s));
                    alpha[j] = s.plus(alpha[j], prod);
                    //if (log.isTraceEnabled()) { log.trace(String.format("outside: %s w_e=%f prod=%f alpha[%d]=%f", e.getLabel(), w.getScore(e,s), prod, j, alpha[j])); }
                }
            }
            
        });
        return alpha;
    }
    
    /**
     * Runs the inside algorithm on a hypergraph with the first-order expectation semiring.
     * 
     * @param graph The hypergraph.
     * @return The beta value for each Hypernode. Where beta[i] is the inside
     *         score for the i'th node in the Hypergraph, graph.getNodes().get(i).
     */
    public static void insideAlgorithmFirstOrderExpect(final Hypergraph graph, final HyperpotentialFoe w,
            final Algebra s, final Scores scores) {
        final int n = graph.getNodes().size();
        final double[] beta = new double[n];
        final double[] betaFoe = new double[n];
        // \beta_i = 0 \forall i
        Arrays.fill(beta, s.zero());
        Arrays.fill(betaFoe, s.zero());
        graph.applyTopoSort(new HyperedgeFn() {

            @Override
            public void apply(Hyperedge e) {
                // \beta_{H(e)} += w_e \prod_{j \in T(e)} \beta_j
                double prod = s.one();
                double prodFoe = s.zero();
                for (Hypernode jNode : e.getTailNodes()) {
                    int j = jNode.getId();
                    // prod = s.times(prod, beta[j]);
                    // prodFoe = s.plus(s.times(prod, betaFoe[j]), s.times(beta[j], prodFoe));
                    double p1 = prod;
                    double p2 = beta[j];
                    double r1 = prodFoe;
                    double r2 = betaFoe[j];
                    prod = s.times(p1, p2);
                    prodFoe = s.plus(s.times(p1, r2), s.times(p2, r1));
                }
                // prod = s.times(prod, w.getScore(e, s));
                // prodFoe = s.plus(s.times(prod, w.getScoreFoe(e, s)), s.times(w.getScore(e, s), prodFoe));
                double p1 = prod;
                double p2 = w.getScore(e, s);
                double r1 = prodFoe;
                double r2 = w.getScoreFoe(e, s);
                prod = s.times(p1, p2);
                prodFoe = s.plus(s.times(p1, r2), s.times(p2, r1));
                
                int i = e.getHeadNode().getId();
                //if (log.isTraceEnabled()) { log.trace(String.format("old beta[%d] = %f",  i, s.toReal(beta[i]))); }
                beta[i] = s.plus(beta[i], prod);
                betaFoe[i] = s.plus(betaFoe[i], prodFoe);
                assert !Double.isNaN(beta[i]) && !Double.isNaN(s.toReal(betaFoe[i]));
                // log.debug(String.format("%s w_e=%f beta[%d] = %.3f betaFoe[%d] = %.3f", e.getLabel(), s.toReal(w.getScore(e, s)), i, s.toReal(beta[i]), i, s.toReal(betaFoe[i]))); 
            }
            
        });
        scores.beta = beta;
        scores.betaFoe = betaFoe;
    }
    
    /**
     * Runs the inside algorithm on a hypergraph.
     * INPUT: 
     * OUTPUT: scores.beta.
     * 
     * @param graph The hypergraph.
     * @param w The potential function.
     * @param s The semiring.
     * @param scores Input and output struct.
     */
    public static void insideAlgorithm(final Hypergraph graph, final Hyperpotential w, final Semiring s,
            final Scores scores) {
        scores.beta = insideAlgorithm(graph, w, s);
    }
    
    /**
     * Runs the outside algorithm on a hypergraph.
     * INPUT: scores.beta.
     * OUTPUT: scores.alpha.
     * 
     * @param graph The hypergraph.
     * @param w The potential function.
     * @param s The semiring.
     * @param scores Input and output struct.
     */
    public static void outsideAlgorithm(final Hypergraph graph, final Hyperpotential w, final Semiring s,
            final Scores scores) {
        scores.alpha = outsideAlgorithm(graph, w, s, scores.beta);
    }

    /**
     * Computes the marginal for each hypernode.
     * INPUT: scores.alpha, scores.beta.
     * OUTPUT: scores.marginal.
     * 
     * @param graph The hypergraph.
     * @param w The potential function.
     * @param s The semiring.
     * @param scores Input and output struct.
     */
    public static void marginals(final Hypergraph graph, final Hyperpotential w, final Algebra s,
            final Scores scores) {
        final int n = graph.getNodes().size();
        final double[] alpha = scores.alpha;
        final double[] beta = scores.beta;
        final double[] marginal = new double[n];
        int root = graph.getRoot().getId();
        
        // p(i) = \alpha_i * \beta_i / \beta_{root}
        for (Hypernode iNode : graph.getNodes()) {
            int i = iNode.getId();
            marginal[i] = s.divide(s.times(alpha[i], beta[i]), beta[root]); 
        }
        
        scores.marginal = marginal;
    }
    
    public static class Scores {
        public double[] alpha;         // Outside scores.
        public double[] beta;          // Inside scores.
        public double[] marginal;      // Marginals of the hyperedges.
        public double[] alphaAdj;      // Adjoints of the outside scores.
        public double[] betaAdj;       // Adjoints of the inside scores.
        public double[] marginalAdj;   // Adjoints of the marginals.
        public double[] weightAdj;     // Weights of the hyperedges.
        public double[] betaFoe;       // Inside scores for first-order expectation semiring.
        
        public void prettyPrint(Hypergraph graph) {
            PrintStream w = System.out;
            w.printf("|%20s|%10s|%10s|%10s|\n", "node-name", "alpha", "beta", "marginal");
            w.printf("|%20s+%10s+%10s+%10s|\n", "-------------------", "----------", "----------", "----------");
            for (Hypernode n : graph.getNodes()) {
                int i = n.getId();
                w.printf("|%20s|%10f|%10f|%10f|\n", n.getLabel(), alpha[i], beta[i], marginal[i]);
            }
            w.printf("|%20s+%10s+%10s+%10s|\n", "-------------------", "----------", "----------", "----------");
        }
    }

    /**
     * Computes the adjoints of the inside and outside scores.
     * INPUT: scores.beta, scores.alpha scores.marginalAdj.
     * OUTPUT: scores.betaAdj, scores.alphaAdj.
     * 
     * @param graph The hypergraph
     * @param w The potential function.
     * @param s The semiring.
     * @param scores Input and output struct.
     */
    public static void marginalsAdjoint(final Hypergraph graph, final Hyperpotential w, final Algebra s,
            final Scores scores) {        
        final int n = graph.getNodes().size();
        final double[] marginalAdj = scores.marginalAdj;
        final double[] alpha = scores.alpha;
        final double[] beta = scores.beta;
        final double[] alphaAdj = new double[n];        
        final double[] betaAdj = new double[n];
        int root = graph.getRoot().getId();

        // ----- Compute the adjoints of the outside scores -----
        
        // \adj{alpha_i} = 0 \forall i
        Arrays.fill(alphaAdj, s.zero());
        for (Hypernode iNode : graph.getNodes()) {
            int i = iNode.getId();
            // \adj{\alpha_i} += \adj{p(i)} * \beta_i / \beta_{root}
            double prod = s.times(marginalAdj[i], beta[i]);
            prod = s.divide(prod, beta[graph.getRoot().getId()]);
            alphaAdj[i] = s.plus(alphaAdj[i], prod);            
        }

        // ----- Compute the adjoints of the inside scores -----

        // \adj{beta_j} = 0 \forall j
        Arrays.fill(betaAdj, s.zero());
        
        for (Hypernode jNode : graph.getNodes()) {
            // \adj{beta_{root}} = - \sum_{j \in G : j \neq root} \adj{p(j)} * \alpha_j * \beta_j / (\beta_{root}^2)
            int j = jNode.getId();
            if (j == root) { continue; }
            double prod = s.times(marginalAdj[j], alpha[j]);
            prod = s.times(prod, beta[j]);
            prod = s.divide(prod, s.times(beta[root], beta[root]));
            betaAdj[root] = s.minus(betaAdj[root], prod);
        }
        
        for (Hypernode jNode : graph.getNodes()) {
            // \adj{\beta_j} += \adj{p(j)} * \alpha_j / \beta_{root}, \forall j \neq root
            int j = jNode.getId();
            if (j == root) { continue; }
            double prod = s.divide(s.times(marginalAdj[j], alpha[j]), beta[root]);
            betaAdj[j] = s.plus(betaAdj[j], prod);
        }

        scores.alphaAdj = alphaAdj;        
        scores.betaAdj = betaAdj;
    }
    
    /**
     * Computes the adjoints of the outside scores by doing a backward pass 
     * through the outside algorithm.
     * 
     * INPUT: scores.beta, scores.alphaAdj.
     * OUTPUT: scores.alphaAdj.
     * 
     * @param graph The hypergraph
     * @param w The potential function.
     * @param s The semiring.
     * @param scores Input and output struct.
     */
    public static void outsideAdjoint(final Hypergraph graph, final Hyperpotential w, final Algebra s,
            final Scores scores) {
        final double[] beta = scores.beta;
        final double[] alphaAdj = scores.alphaAdj;

        graph.applyTopoSort(new HyperedgeFn() {
            
            @Override
            public void apply(Hyperedge e) {
                int i = e.getHeadNode().getId();
                // \forall j \in T(e):
                // \adj{\alpha_i} += \adj{\alpha_j} * w_e * \prod_{k \in T(e): k \neq j} \beta_k
                for (Hypernode j : e.getTailNodes()) {
                    double prod = s.times(alphaAdj[j.getId()], w.getScore(e, s));
                    for (Hypernode k : e.getTailNodes()) {
                        if (k == j) { continue; }
                        prod = s.times(prod, beta[k.getId()]);
                    }
                    alphaAdj[i] = s.plus(alphaAdj[i], prod);
                }
            }
            
        });
        scores.alphaAdj = alphaAdj;
    }
    
    /**
     * Computes the adjoints of the inside scores by doing a backward pass 
     * through the inside algorithm.
     * 
     * INPUT: scores.alpha, scores.beta, scores.betaAdj.
     * OUTPUT: scores.betaAdj.
     * 
     * @param graph The hypergraph
     * @param w The potential function.
     * @param s The semiring.
     * @param scores Input and output struct.
     */
    public static void insideAdjoint(final Hypergraph graph, final Hyperpotential w, final Algebra s,
            final Scores scores) {
        final double[] alpha = scores.alpha;
        final double[] beta = scores.beta;
        final double[] alphaAdj = scores.alphaAdj;
        final double[] betaAdj = scores.betaAdj;
        
        graph.applyRevTopoSort(new HyperedgeFn() {
            
            @Override
            public void apply(Hyperedge e) {
                int i = e.getHeadNode().getId();
                for (Hypernode jNode : e.getTailNodes()) {
                    int j = jNode.getId();
                    // \adj{\beta_{j}} += \sum_{e \in O(j)} \adj{\beta_{H(e)}} * w_e * \prod_{k \in T(e) : k \neq j} \beta_k  
                    double prod = s.times(betaAdj[i], w.getScore(e, s));
                    for (Hypernode kNode : e.getTailNodes()) {
                        int k = kNode.getId();
                        if (j == k) { continue; }
                        prod = s.times(prod, beta[k]);
                    }
                    betaAdj[j] = s.plus(betaAdj[j], prod);
                    
                    // \adj{\beta_{j}} += \sum_{e \in O(j)} \sum_{k \in T(e) : k \neq j} \adj{\alpha_k} * w_e * \alpha_{H(e)} * \prod_{l \in T(e) : l \neq k, l \neq j} \beta_l
                    for (Hypernode kNode : e.getTailNodes()) {
                        int k = kNode.getId();
                        if (k == j) { continue; };
                        prod = s.times(alphaAdj[k], w.getScore(e, s));
                        prod = s.times(prod, alpha[i]);
                        for (Hypernode lNode : e.getTailNodes()) {
                            int l = lNode.getId();
                            if (l == j || l == k) { continue; }
                            prod = s.times(prod, beta[l]);
                        }
                        betaAdj[j] = s.plus(betaAdj[j], prod);
                    }
                }
            }
        });
        
        scores.betaAdj = betaAdj;
    }
    
    
    /**
     * Computes the adjoints of the hyperedge weights.
     * INPUT: scores.alpha, scores.beta, scores.alphaAdj, scores.betaAdj.
     * OUTPUT: scores.weightsAdj.
     * 
     * @param graph The hypergraph
     * @param w The potential function.
     * @param s The semiring.
     * @param scores Input and output struct.
     */
    public static void weightAdjoint(final Hypergraph graph, final Hyperpotential w, final Algebra s,
            final Scores scores) {
        final double[] weightAdj = new double[graph.getNumEdges()];
        HyperedgeDoubleFn lambda = new HyperedgeDoubleFn() {
            public void apply(Hyperedge e, double val) {
                weightAdj[e.getId()] = val;
            }
        };
        weightAdjoint(graph, w, s, scores, lambda);
        scores.weightAdj = weightAdj;
    }
    
    public interface HyperedgeDoubleFn {
        void apply(Hyperedge e, double val);
    }
    
    /**
     * Computes the adjoints of the hyperedge weights.
     * INPUT: scores.alpha, scores.beta, scores.alphaAdj, scores.betaAdj.
     * OUTPUT: The adjoints of the weights.
     * 
     * @param graph The hypergraph
     * @param w The potential function.
     * @param s The semiring.
     * @param scores Input struct.
     * @param lambda Function to call with the edge and the edge adjoint.
     */
    public static void weightAdjoint(final Hypergraph graph, final Hyperpotential w, 
            final Algebra s, final Scores scores, final HyperedgeDoubleFn lambda) {
        final double[] alpha = scores.alpha;
        final double[] beta = scores.beta;
        final double[] alphaAdj = scores.alphaAdj;
        final double[] betaAdj = scores.betaAdj;
        
        graph.applyTopoSort(new HyperedgeFn() {
            
            @Override
            public void apply(Hyperedge e) {
                // \adj{w_e} += \adj{\beta_{H(e)} * \prod_{j \in T(e)} \beta_j
                int i = e.getHeadNode().getId();
                double prod = betaAdj[i];
                for (Hypernode jNode : e.getTailNodes()) {
                    int j = jNode.getId();
                    prod = s.times(prod, beta[j]);
                }
                double w_e = prod;
                
                // \adj{w_e} += \adj{\alpha_j} * \alpha_{H(e)} * \prod_{k \in T(e) : k \neq j} \beta_k
                for (Hypernode jNode : e.getTailNodes()) {
                    int j = jNode.getId();                
                    prod = s.times(alphaAdj[j], alpha[i]);
                    for (Hypernode kNode : e.getTailNodes()) {
                        int k = kNode.getId();
                        if (k == j) { continue; }
                        prod = s.times(prod, beta[k]);
                    }
                    w_e = s.plus(w_e, prod);
                }
                
                lambda.apply(e, w_e);
            }
            
        });        
    }
    
}
