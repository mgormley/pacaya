package edu.jhu.hltcoe.gridsearch.dmv;

import ilog.concert.IloException;
import depparsing.extended.DepInstance;
import depparsing.extended.DepSentenceDist;
import depparsing.model.NonterminalMap;
import edu.jhu.hltcoe.data.DepTree;
import edu.jhu.hltcoe.data.DepTreebank;
import edu.jhu.hltcoe.data.Sentence;
import edu.jhu.hltcoe.gridsearch.Projector;
import edu.jhu.hltcoe.gridsearch.RelaxedSolution;
import edu.jhu.hltcoe.gridsearch.Solution;
import edu.jhu.hltcoe.gridsearch.cpt.CptBounds;
import edu.jhu.hltcoe.gridsearch.cpt.Projections;
import edu.jhu.hltcoe.gridsearch.cpt.Projections.ProjectionsPrm;
import edu.jhu.hltcoe.gridsearch.dmv.DmvObjective.DmvObjectivePrm;
import edu.jhu.hltcoe.parse.DmvCkyParser;
import edu.jhu.hltcoe.train.DmvTrainCorpus;
import edu.jhu.hltcoe.util.Pair;
import edu.jhu.hltcoe.util.Utilities;
import edu.jhu.hltcoe.util.math.Vectors;

public class BasicDmvProjector implements DmvProjector {

    public static interface DmvProjectorFactory {
        public Projector getInstance(DmvTrainCorpus corpus, DmvRelaxation relax);
    }
        
    public static class DmvProjectorPrm implements DmvProjectorFactory {
        public ProjectionsPrm projPrm = new ProjectionsPrm();
        public CptBounds rootBounds = null;
        public DmvObjectivePrm objPrm = new DmvObjectivePrm();
        @Override
        public Projector getInstance(DmvTrainCorpus corpus, DmvRelaxation relax) {
            return new BasicDmvProjector(this, corpus);
        }
    }
    
    private DmvProjectorPrm prm;

    private DmvTrainCorpus corpus;
    private IndexedDmvModel idm;
    private DmvObjective obj;
    private Projections projector;
    
    public BasicDmvProjector(DmvProjectorPrm prm, DmvTrainCorpus corpus) {
        this.prm = prm;
        this.corpus = corpus;
        this.projector = new Projections(prm.projPrm);

        // TODO: we shouldn't have to create a new IndexedDmvModel here.
        this.idm = new IndexedDmvModel(this.corpus);
        this.obj = new DmvObjective(prm.objPrm, idm);
        
        if (prm.rootBounds == null) {
            this.prm.rootBounds = new CptBounds(idm);
        }
    }

    @Override
    public Solution getProjectedSolution(RelaxedSolution relaxSol) {
        return getProjectedDmvSolution((DmvRelaxedSolution) relaxSol);
    }

    public DmvSolution getProjectedDmvSolution(DmvRelaxedSolution relaxSol) {
        if (!relaxSol.getStatus().hasSolution()) {
            return null;
        }
        // Project the relaxed model parameters back into the bounded
        // sum-to-exactly-one space
        double[][] logProbs = getProjectedParams(relaxSol.getLogProbs());
   
        // Project the fractional parse back to the feasible region
        // where the weight of each edge is given by the indicator variable
        // TODO: How would we do randomized rounding on the Dantzig-Wolfe parse
        // solution?
        DepTreebank treebank = getProjectedParses(relaxSol.getTreebank());
   
        // TODO: write a new DmvMStep that stays in the bounded parameter space
        double score = obj.computeTrueObjective(logProbs, treebank);
        
        DmvSolution sol = new DmvSolution(logProbs, idm, treebank, score);
        return sol;
    }

	private double[][] getProjectedParams(double[][] relaxedLogProbs) {
		// TODO: must use bounds here?   
        double[][] logProbs = Utilities.copyOf(relaxedLogProbs);
        // Project the model parameters back onto the feasible (sum-to-one)
        // region ignoring the model parameter bounds (we just want a good solution)
        // there's no reason to constrain it.
        for (int c = 0; c < logProbs.length; c++) {
            double[] probs = Vectors.getExp(logProbs[c]);
            try {
                probs = projector.getDefaultProjection(prm.rootBounds, c, probs);
            } catch (IloException e) {
                throw new RuntimeException(e);
            }
            logProbs[c] = Vectors.getLog(probs); 
        }
        // Create a new DmvModel from these model parameters
		return logProbs;
	}

    public DepTreebank getProjectedParses(RelaxedDepTreebank fracTreebank) {
        double[][] fracRoots = fracTreebank.getFracRoots(); 
        double[][][] fracChildren = fracTreebank.getFracChildren();
        
        DepTreebank treebank = new DepTreebank(corpus.getLabelAlphabet());
        for (int s = 0; s < fracChildren.length; s++) {
            if (corpus.isLabeled(s)) {
                treebank.add(corpus.getTree(s));
            } else {
                Sentence sentence = corpus.getSentence(s);
                double[] fracRoot = fracRoots[s];
                double[][] fracChild = fracChildren[s];
    
                // For projective case we use a DP parser
                DepTree tree = BasicDmvProjector.getProjectiveParse(sentence, fracRoot, fracChild);
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

    public static DepTree getProjectiveParse(Sentence sentence, double[] fracRoot, double[][] fracChild) {
        DmvCkyParser parser = new DmvCkyParser();
        int[] tags = new int[sentence.size()];
        DepInstance depInstance = new DepInstance(tags);
        DepSentenceDist sd = new DepSentenceDist(depInstance, new NonterminalMap(2, 1), fracRoot, fracChild);
        Pair<DepTree, Double> pair = parser.parse(sentence, sd);
        DepTree tree = pair.get1();
        return tree;
    }

}
