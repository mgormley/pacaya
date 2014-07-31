package edu.jhu.gm.train;

import org.apache.log4j.Logger;

import edu.jhu.autodiff.erma.ErmaBp.ErmaBpPrm;
import edu.jhu.autodiff.erma.ErmaObjective;
import edu.jhu.autodiff.erma.ErmaObjective.BeliefsModuleFactory;
import edu.jhu.autodiff.erma.ErmaObjective.DlFactory;
import edu.jhu.autodiff.erma.ExpectedRecall.ExpectedRecallFactory;
import edu.jhu.gm.data.FgExampleList;
import edu.jhu.gm.inf.BeliefPropagation.BeliefPropagationPrm;
import edu.jhu.gm.inf.FgInferencerFactory;
import edu.jhu.gm.model.FgModel;
import edu.jhu.gm.train.AvgBatchObjective.ExampleObjective;
import edu.jhu.hlt.optimize.MalletLBFGS;
import edu.jhu.hlt.optimize.MalletLBFGS.MalletLBFGSPrm;
import edu.jhu.hlt.optimize.Optimizer;
import edu.jhu.hlt.optimize.function.BatchFunctionOpts;
import edu.jhu.hlt.optimize.function.DifferentiableBatchFunction;
import edu.jhu.hlt.optimize.function.DifferentiableFunction;
import edu.jhu.hlt.optimize.function.DifferentiableFunctionOpts;
import edu.jhu.hlt.optimize.function.FunctionAsBatchFunction;
import edu.jhu.hlt.optimize.function.Regularizer;
import edu.jhu.hlt.optimize.functions.L2;
import edu.jhu.prim.sort.IntSort;

/**
 * Trainer for a conditional random field (CRF) represented as a factor graph.
 * 
 * @author mgormley
 *
 */
public class CrfTrainer {

    public static enum Trainer { CLL, ERMA };

    public static class CrfTrainerPrm {
        public FgInferencerFactory infFactory = new BeliefPropagationPrm();
        public BeliefsModuleFactory bFactory = null;
        public Optimizer<DifferentiableFunction> optimizer = new MalletLBFGS(new MalletLBFGSPrm());
        public Optimizer<DifferentiableBatchFunction> batchOptimizer = null;//new SGD(new SGDPrm());
        public Regularizer regularizer = new L2(1.0);
        public int numThreads = 1;
        /** The type of trainer. */
        public Trainer trainer = Trainer.CLL;
        /** The decoder and loss function used by ERMA training. */
        public DlFactory dlFactory = new ExpectedRecallFactory();
        /**
         * Whether to use the mean squared error (MSE) in place of conditional
         * log-likelihood in the CRF objective. This is useful for loopy graphs
         * where the BP estimate of the partition function is unreliable.
         */
        public boolean useMseForValue = false;
    }
    
    private static final Logger log = Logger.getLogger(CrfTrainer.class);
    
    private CrfTrainerPrm prm; 
        
    public CrfTrainer(CrfTrainerPrm prm) {
        this.prm = prm;
        if (prm.optimizer != null && prm.batchOptimizer != null) {
            throw new IllegalStateException("Only one of optimizer and batchOptimizer may be set in CrfTrainerPrm.");
        }
    }
    
    public FgModel train(FgModel model, FgExampleList data) {        
        ExampleObjective exObj;
        boolean isMinimize;
        if (prm.trainer == Trainer.ERMA) {
            exObj = new ErmaObjective(data, prm.bFactory, prm.dlFactory);
            isMinimize = true;
        } else {
            exObj = new CrfObjective(data, prm.infFactory, prm.useMseForValue);
            isMinimize = false;
        }
        AvgBatchObjective objective = new AvgBatchObjective(exObj, model, prm.numThreads);
        
        Regularizer reg = prm.regularizer;
        if (prm.optimizer != null) {
            DifferentiableFunction fn = objective;
            if (reg != null) {
                reg.setNumDimensions(model.getNumParams());
                DifferentiableFunction nbr = isMinimize ? DifferentiableFunctionOpts.negate(reg) : reg;
                fn = new DifferentiableFunctionOpts.AddFunctions(objective, nbr);
            }
            if (isMinimize == true) {
                prm.optimizer.minimize(fn, model.getParams());
            } else {
                prm.optimizer.maximize(fn, model.getParams());
            }
            log.info("Final objective value: " + fn.getValue(model.getParams()));
        } else {
            DifferentiableBatchFunction fn = objective;
            if (reg != null) {
                // We don't need to rescale the regularizer because the CRF
                // objective is the average log-likelihood.
                reg.setNumDimensions(model.getNumParams());
                DifferentiableBatchFunction br = new FunctionAsBatchFunction(reg, objective.getNumExamples());
                DifferentiableBatchFunction nbr = isMinimize ? new BatchFunctionOpts.NegateFunction(br) : br;
                fn = new BatchFunctionOpts.AddFunctions(objective, nbr);
            }
            if (isMinimize == true) {
                prm.batchOptimizer.minimize(fn, model.getParams());   
            } else {
                prm.batchOptimizer.maximize(fn, model.getParams());   
            }
            log.info("Final objective value: " + fn.getValue(model.getParams(), IntSort.getIndexArray(fn.getNumExamples())));
        }
        objective.shutdown();
        return model;
    }
    
}
