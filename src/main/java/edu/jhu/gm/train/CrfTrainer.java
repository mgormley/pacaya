package edu.jhu.gm.train;

import org.apache.log4j.Logger;

import edu.jhu.autodiff.erma.ErmaObjective;
import edu.jhu.autodiff.erma.ErmaObjective.DlFactory;
import edu.jhu.autodiff.erma.ExpectedRecall.ExpectedRecallFactory;
import edu.jhu.gm.data.FgExampleList;
import edu.jhu.gm.inf.BeliefPropagation.BeliefPropagationPrm;
import edu.jhu.gm.inf.BeliefPropagation.FgInferencerFactory;
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

    private static final Logger log = Logger.getLogger(CrfTrainer.class);

    public static class CrfTrainerPrm {
        public FgInferencerFactory infFactory = new BeliefPropagationPrm();
        public Optimizer<DifferentiableFunction> maximizer = new MalletLBFGS(new MalletLBFGSPrm());
        public Optimizer<DifferentiableBatchFunction> batchMaximizer = null;//new SGD(new SGDPrm());
        public Regularizer regularizer = new L2(1.0);
        public int numThreads = 1;
        /** Whether to use ERMA training. */
        public boolean ermaTraining = false;
        /** The decoder and loss function used by ERMA training. */
        public DlFactory dlFactory = new ExpectedRecallFactory();
        /**
         * Whether to use the mean squared error (MSE) in place of conditional
         * log-likelihood in the CRF objective. This is useful for loopy graphs
         * where the BP estimate of the partition function is unreliable.
         */
        public boolean useMseForValue = false;
    }
        
    private CrfTrainerPrm prm; 
        
    public CrfTrainer(CrfTrainerPrm prm) {
        this.prm = prm;
        if (prm.maximizer != null && prm.batchMaximizer != null) {
            throw new IllegalStateException("Only one of maximizer and batchMaximizer may be set in CrfTrainerPrm.");
        }
    }
    
    public FgModel train(FgModel model, FgExampleList data) {

        ExampleObjective exObj;
        if (prm.ermaTraining) {
            exObj = new ErmaObjective(data, prm.infFactory, prm.dlFactory);
        } else {
            exObj = new CrfObjective(data, prm.infFactory, prm.useMseForValue);
        }
        AvgBatchObjective objective = new AvgBatchObjective(exObj, model, prm.numThreads);
        if (prm.maximizer != null) {
            DifferentiableFunction fn = objective;
            if (prm.regularizer != null) {
                prm.regularizer.setNumDimensions(model.getNumParams());
                fn = new DifferentiableFunctionOpts.AddFunctions(objective, prm.regularizer);
            }
            if (prm.ermaTraining) {
                prm.maximizer.minimize(fn, model.getParams());
            } else {
                prm.maximizer.maximize(fn, model.getParams());
            }
            log.info("Final objective value: " + fn.getValue(model.getParams()));
        } else {
            DifferentiableBatchFunction fn = objective;
            if (prm.regularizer != null) {
                // We don't need to rescale the regularizer because the CRF
                // objective is the average log-likelihood.
                prm.regularizer.setNumDimensions(model.getNumParams());
                DifferentiableBatchFunction br = new FunctionAsBatchFunction(prm.regularizer, objective.getNumExamples());
                fn = new BatchFunctionOpts.AddFunctions(objective, br);
            }
            if (prm.ermaTraining) {
                prm.batchMaximizer.minimize(fn, model.getParams());   
            } else {
                prm.batchMaximizer.maximize(fn, model.getParams());   
            }
            log.info("Final objective value: " + fn.getValue(model.getParams(), IntSort.getIndexArray(fn.getNumExamples())));
        }
        objective.shutdown();
        return model;
    }
    
}
