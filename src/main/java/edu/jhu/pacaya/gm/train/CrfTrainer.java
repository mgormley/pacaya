package edu.jhu.pacaya.gm.train;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.jhu.hlt.optimize.AdaGradComidL2;
import edu.jhu.hlt.optimize.AdaGradComidL2.AdaGradComidL2Prm;
import edu.jhu.hlt.optimize.Optimizer;
import edu.jhu.hlt.optimize.SGD;
import edu.jhu.hlt.optimize.function.BatchFunctionOpts;
import edu.jhu.hlt.optimize.function.DifferentiableBatchFunction;
import edu.jhu.hlt.optimize.function.DifferentiableFunction;
import edu.jhu.hlt.optimize.function.DifferentiableFunctionOpts;
import edu.jhu.hlt.optimize.function.Function;
import edu.jhu.hlt.optimize.function.FunctionAsBatchFunction;
import edu.jhu.hlt.optimize.function.Regularizer;
import edu.jhu.pacaya.gm.data.FgExampleList;
import edu.jhu.pacaya.gm.inf.BeliefsModuleFactory;
import edu.jhu.pacaya.gm.inf.BeliefPropagation.BeliefPropagationPrm;
import edu.jhu.pacaya.gm.inf.FgInferencerFactory;
import edu.jhu.pacaya.gm.model.FgModel;
import edu.jhu.pacaya.gm.train.AvgBatchObjective.ExampleObjective;
import edu.jhu.pacaya.gm.train.EmpiricalRisk.EmpiricalRiskFactory;
import edu.jhu.pacaya.gm.train.ExpectedRecall.ExpectedRecallFactory;
import edu.jhu.pacaya.util.Prm;

/**
 * Trainer for a conditional random field (CRF) represented as a factor graph.
 * 
 * @author mgormley
 *
 */
public class CrfTrainer {

    public static enum Trainer { CLL, ERMA };

    public static class CrfTrainerPrm extends Prm {
        private static final long serialVersionUID = 1L;
        public FgInferencerFactory infFactory = new BeliefPropagationPrm();
        public BeliefsModuleFactory bFactory = null;
        public Optimizer<DifferentiableFunction> optimizer = null;
        public Optimizer<DifferentiableBatchFunction> batchOptimizer = new AdaGradComidL2(new AdaGradComidL2Prm());
        public Regularizer regularizer = null;
        /** The type of trainer. */
        public Trainer trainer = Trainer.CLL;
        /** The decoder and loss function used by ERMA training. */
        public DlFactory dlFactory = new ExpectedRecallFactory();
    }
    
    private static final Logger log = LoggerFactory.getLogger(CrfTrainer.class);
    
    private CrfTrainerPrm prm; 
    
    public CrfTrainer(CrfTrainerPrm prm) {
        this.prm = prm;
        if (prm.optimizer != null && prm.batchOptimizer != null) {
            throw new IllegalStateException("Only one of optimizer and batchOptimizer may be set in CrfTrainerPrm.");
        }
    }
    
    @Deprecated
    public FgModel train(FgModel model, FgExampleList data) {
        return train(model, data, null);
    }
    
    public FgModel train(FgModel model, FgExampleList data, Function validation) {        
        ExampleObjective exObj;
        boolean isMinimize;
        MtFactory mtFactory;
        if (prm.trainer == Trainer.ERMA) {
            mtFactory = new EmpiricalRiskFactory(prm.bFactory, prm.dlFactory);
            isMinimize = true;
        } else {
            mtFactory = new LogLikelihoodFactory(prm.infFactory);
            isMinimize = false;
        }
        mtFactory = new ScaleByWeightFactory(mtFactory);
        exObj = new ModuleObjective(data, mtFactory);
        AvgBatchObjective objective = new AvgBatchObjective(exObj, model);
        
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
            if (prm.batchOptimizer instanceof SGD && validation != null) {
                SGD sgd = (SGD) prm.batchOptimizer;
                sgd.optimize(fn, model.getParams(), !isMinimize, validation);
            } else {
                if (isMinimize == true) {
                    prm.batchOptimizer.minimize(fn, model.getParams());   
                } else {
                    prm.batchOptimizer.maximize(fn, model.getParams());   
                }
            }
        }
        return model;
    }
    
}
