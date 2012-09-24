package edu.jhu.hltcoe.gridsearch.dmv;

import edu.jhu.hltcoe.data.DepTree;
import edu.jhu.hltcoe.data.DepTreebank;
import edu.jhu.hltcoe.data.Sentence;
import edu.jhu.hltcoe.gridsearch.Projector;
import edu.jhu.hltcoe.gridsearch.RelaxedSolution;
import edu.jhu.hltcoe.gridsearch.Solution;
import edu.jhu.hltcoe.math.Vectors;
import edu.jhu.hltcoe.train.DmvTrainCorpus;

public class DmvProjector implements Projector {

    private DmvTrainCorpus corpus;
    private DmvRelaxation dwRelax;
    
    public DmvProjector(DmvTrainCorpus corpus, DmvRelaxation dwRelax) {
        super();
        this.corpus = corpus;
        this.dwRelax = dwRelax;
    }

    @Override
    public Solution getProjectedSolution(RelaxedSolution relaxSol) {
        return getProjectedDmvSolution((RelaxedDmvSolution) relaxSol);
    }


    public DmvSolution getProjectedDmvSolution(RelaxedDmvSolution relaxSol) {
        if (!relaxSol.getStatus().hasSolution()) {
            return null;
        }
        // Project the Dantzig-Wolfe model parameters back into the bounded
        // sum-to-exactly-one space
        // TODO: must use bounds here?
   
        double[][] logProbs = relaxSol.getLogProbs();
        // Project the model parameters back onto the feasible (sum-to-one)
        // region ignoring the model parameter bounds (we just want a good solution)
        // there's no reason to constrain it.
        for (int c = 0; c < logProbs.length; c++) {
            double[] probs = Vectors.getExp(logProbs[c]);
            probs = Projections.getProjectedParams(probs);
            logProbs[c] = Vectors.getLog(probs); 
        }
        // Create a new DmvModel from these model parameters
        IndexedDmvModel idm = dwRelax.getIdm();
   
        // Project the fractional parse back to the feasible region
        // where the weight of each edge is given by the indicator variable
        // TODO: How would we do randomized rounding on the Dantzig-Wolfe parse
        // solution?
        DepTreebank treebank = getProjectedParses(relaxSol.getFracRoots(), relaxSol.getFracChildren());
   
        // TODO: write a new DmvMStep that stays in the bounded parameter space
        double score = dwRelax.computeTrueObjective(logProbs, treebank);
        
        DmvSolution sol = new DmvSolution(logProbs, idm, treebank, score);
        return sol;
    }

    private DepTreebank getProjectedParses(double[][] fracRoots, double[][][] fracChildren) {
        DepTreebank treebank = new DepTreebank();
        for (int s = 0; s < fracChildren.length; s++) {
            if (corpus.isLabeled(s)) {
                treebank.add(corpus.getTree(s));
            } else {
                Sentence sentence = corpus.getSentence(s);
                double[] fracRoot = fracRoots[s];
                double[][] fracChild = fracChildren[s];
    
                // For projective case we use a DP parser
                DepTree tree = Projections.getProjectiveParse(sentence, fracRoot, fracChild);
                treebank.add(tree);
                
                // For non-projective case we'd do something like this.
                // int[] parents = new int[weights.length];
                // Edmonds eds = new Edmonds();
                // CompleteGraph graph = new CompleteGraph(weights);
                // eds.getMaxBranching(graph, 0, parents);
            }
        }
        return treebank;
    }

}
