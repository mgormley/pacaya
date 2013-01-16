package edu.jhu.hltcoe.gridsearch.dmv;

import org.apache.log4j.Logger;

import edu.jhu.hltcoe.data.DepTreebank;
import edu.jhu.hltcoe.gridsearch.LazyBranchAndBoundSolver;
import edu.jhu.hltcoe.gridsearch.Projector;
import edu.jhu.hltcoe.gridsearch.LazyBranchAndBoundSolver.LazyBnbSolverFactory;
import edu.jhu.hltcoe.gridsearch.LazyBranchAndBoundSolver.LazyBnbSolverPrm;
import edu.jhu.hltcoe.gridsearch.LazyBranchAndBoundSolver.SearchStatus;
import edu.jhu.hltcoe.gridsearch.cpt.BasicCptBoundsDeltaFactory;
import edu.jhu.hltcoe.gridsearch.cpt.CptBoundsDeltaFactory;
import edu.jhu.hltcoe.gridsearch.cpt.MidpointVarSplitter;
import edu.jhu.hltcoe.gridsearch.cpt.RegretVariableSelector;
import edu.jhu.hltcoe.gridsearch.cpt.MidpointVarSplitter.MidpointChoice;
import edu.jhu.hltcoe.gridsearch.dmv.DmvDantzigWolfeRelaxation.DmvDwRelaxPrm;
import edu.jhu.hltcoe.gridsearch.dmv.DmvDantzigWolfeRelaxation.DmvRelaxationFactory;
import edu.jhu.hltcoe.gridsearch.dmv.DmvProjector.DmvProjectorFactory;
import edu.jhu.hltcoe.gridsearch.dmv.DmvSolFactory.DmvSolFactoryPrm;
import edu.jhu.hltcoe.gridsearch.dmv.ViterbiEmDmvProjector.ViterbiEmDmvProjectorPrm;
import edu.jhu.hltcoe.model.Model;
import edu.jhu.hltcoe.train.DmvTrainCorpus;
import edu.jhu.hltcoe.train.TrainCorpus;
import edu.jhu.hltcoe.train.Trainer;

public class BnBDmvTrainer implements Trainer<DepTreebank> {

    private static final Logger log = Logger.getLogger(BnBDmvTrainer.class);

    public static class BnBDmvTrainerPrm {
        public DmvSolFactoryPrm initSolPrm = new DmvSolFactoryPrm();
        public CptBoundsDeltaFactory brancher = new BasicCptBoundsDeltaFactory(new RegretVariableSelector(), new MidpointVarSplitter(MidpointChoice.HALF_PROB));
        public LazyBnbSolverFactory bnbSolverFactory = new LazyBnbSolverPrm();
        public DmvRelaxationFactory relaxFactory = new DmvDwRelaxPrm(); 
        public DmvProjectorFactory projectorFactory = new ViterbiEmDmvProjectorPrm();
    }
    
    private DmvSolution initFeasSol;
    private LazyBranchAndBoundSolver bnbSolver;
    private DmvRelaxation relax;
    
    private BnBDmvTrainerPrm prm;
        
    public BnBDmvTrainer(BnBDmvTrainerPrm prm) {
        this.prm = prm;
    }

    @Override
    /**
     * The user of BnBDmvTrainer must first call init(corpus) then train(). 
     * TODO: remove public init() and train() methods once we can remove getRootRelaxation. 
     */
    public void train(TrainCorpus corpus) {
        init((DmvTrainCorpus)corpus);
        train();
    }
    
    public void init(DmvTrainCorpus corpus) {
        // Get initial solution for B&B and (sometimes) the relaxation.
        DmvSolFactory dsf = new DmvSolFactory(prm.initSolPrm); 
        this.initFeasSol = dsf.getInitFeasSol(corpus);
        log.info("Initial solution score: " + initFeasSol.getScore());

        relax = prm.relaxFactory.getInstance(corpus, initFeasSol);
        Projector projector = prm.projectorFactory.getInstance(corpus, relax);
        bnbSolver = prm.bnbSolverFactory.getInstance(relax, projector);
    }
    
    public SearchStatus train() {
        DmvProblemNode rootNode = new DmvProblemNode(prm.brancher);
        SearchStatus status = bnbSolver.runBranchAndBound(rootNode, initFeasSol, initFeasSol.getScore());
        relax.end();
        return status;
    }
    
    @Override
    public Model getModel() {
        DmvSolution solution = (DmvSolution) bnbSolver.getIncumbentSolution();
        // Create a new DmvModel from these model parameters
        return solution.getIdm().getDmvModel(solution.getLogProbs());
    }
    
    /**
     * TODO: Remove this method -- it's a hack for updating the bounds on the root node relaxation.
     */
    public DmvRelaxation getRootRelaxation() {
        return relax;
    }

}
