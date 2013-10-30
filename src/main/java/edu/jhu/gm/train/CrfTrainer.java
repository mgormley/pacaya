package edu.jhu.gm.train;

import org.apache.log4j.Logger;

import edu.jhu.gm.data.FgExamples;
import edu.jhu.gm.inf.BeliefPropagation.BeliefPropagationPrm;
import edu.jhu.gm.inf.BeliefPropagation.FgInferencerFactory;
import edu.jhu.gm.model.FgModel;
import edu.jhu.optimize.BatchFunction;
import edu.jhu.optimize.BatchFunctionOpts;
import edu.jhu.optimize.BatchMaximizer;
import edu.jhu.optimize.Function;
import edu.jhu.optimize.FunctionAsBatchFunction;
import edu.jhu.optimize.FunctionOpts;
import edu.jhu.optimize.L2;
import edu.jhu.optimize.MalletLBFGS;
import edu.jhu.optimize.MalletLBFGS.MalletLBFGSPrm;
import edu.jhu.optimize.Maximizer;
import edu.jhu.optimize.Regularizer;
import edu.jhu.prim.util.Utilities;

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
        public Maximizer maximizer = new MalletLBFGS(new MalletLBFGSPrm());
        public BatchMaximizer batchMaximizer = null;//new SGD(new SGDPrm());
        public Regularizer regularizer = new L2(1.0);
    }
        
    private CrfTrainerPrm prm; 
        
    public CrfTrainer(CrfTrainerPrm prm) {
        this.prm = prm;
        if (prm.maximizer != null && prm.batchMaximizer != null) {
            throw new IllegalStateException("Only one of maximizer and batchMaximizer may be set in CrfTrainerPrm.");
        }
    }
    
    public FgModel train(FgModel model, FgExamples data) {
        double[] params = new double[model.getNumParams()];
        model.updateDoublesFromModel(params);

        CrfObjective objective = new CrfObjective(model, data, prm.infFactory);
        if (prm.maximizer != null) {
            Function fn = objective;
            if (prm.regularizer != null) {
                prm.regularizer.setNumDimensions(model.getNumParams());
                fn = new FunctionOpts.AddFunctions(objective, prm.regularizer);
            }
            prm.maximizer.maximize(fn, params);
            log.info("Final objective value: " + fn.getValue());
        } else {
            BatchFunction fn = objective;
            if (prm.regularizer != null) {
                // We don't need to rescale the regularizer because the CRF
                // objective is the average log-likelihood.
                prm.regularizer.setNumDimensions(model.getNumParams());
                BatchFunction br = new FunctionAsBatchFunction(prm.regularizer, objective.getNumExamples());
                fn = new BatchFunctionOpts.AddFunctions(objective, br);
            }
            prm.batchMaximizer.maximize(fn, params);   
            log.info("Final objective value: " + fn.getValue(Utilities.getIndexArray(fn.getNumExamples())));
        }
        model.updateModelFromDoubles(params);
        return model;
    }
    
}
