package edu.jhu.induce.train.dmv;

import org.apache.log4j.Logger;

import edu.jhu.data.DepTreebank;
import edu.jhu.globalopt.LazyBranchAndBoundSolver;
import edu.jhu.globalopt.Projector;
import edu.jhu.globalopt.LazyBranchAndBoundSolver.LazyBnbSolverFactory;
import edu.jhu.globalopt.LazyBranchAndBoundSolver.LazyBnbSolverPrm;
import edu.jhu.globalopt.LazyBranchAndBoundSolver.SearchStatus;
import edu.jhu.globalopt.cpt.BasicCptBoundsDeltaFactory;
import edu.jhu.globalopt.cpt.CptBoundsDeltaFactory;
import edu.jhu.globalopt.cpt.MidpointVarSplitter;
import edu.jhu.globalopt.cpt.RegretVariableSelector;
import edu.jhu.globalopt.cpt.MidpointVarSplitter.MidpointChoice;
import edu.jhu.globalopt.dmv.DmvProblemNode;
import edu.jhu.globalopt.dmv.DmvRelaxation;
import edu.jhu.globalopt.dmv.DmvSolFactory;
import edu.jhu.globalopt.dmv.DmvSolution;
import edu.jhu.globalopt.dmv.BasicDmvProjector.DmvProjectorFactory;
import edu.jhu.globalopt.dmv.DmvDantzigWolfeRelaxation.DmvDwRelaxPrm;
import edu.jhu.globalopt.dmv.DmvDantzigWolfeRelaxation.DmvRelaxationFactory;
import edu.jhu.globalopt.dmv.DmvSolFactory.DmvSolFactoryPrm;
import edu.jhu.globalopt.dmv.ViterbiEmDmvProjector.ViterbiEmDmvProjectorPrm;
import edu.jhu.induce.model.Model;
import edu.jhu.induce.train.SemiSupervisedCorpus;
import edu.jhu.induce.train.Trainer;

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
    public void train(SemiSupervisedCorpus corpus) {
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
        // TODO: subtract off the time used in init().
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
