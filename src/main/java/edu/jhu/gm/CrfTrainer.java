package edu.jhu.gm;

import org.apache.log4j.Logger;

import edu.jhu.gm.BeliefPropagation.BeliefPropagationPrm;
import edu.jhu.gm.BeliefPropagation.FgInferencerFactory;
import edu.jhu.optimize.Function;
import edu.jhu.optimize.FunctionOpts.AddFunctions;
import edu.jhu.optimize.L2;
import edu.jhu.optimize.MalletLBFGS;
import edu.jhu.optimize.MalletLBFGS.MalletLBFGSPrm;
import edu.jhu.optimize.Maximizer;
import edu.jhu.optimize.Regularizer;

/**
 * Trainer for a conditional random field (CRF) represented as a factor graph.
 * 
 * @author mgormley
 *
 */
// TODO: This currently does NOT support VarType.LATENT variables. Assert this. Then implement a version that does.
public class CrfTrainer {

    private static final Logger log = Logger.getLogger(CrfTrainer.class);

    public static class CrfTrainerPrm {
        public FgInferencerFactory infFactory = new BeliefPropagationPrm();
        public Maximizer maximizer = new MalletLBFGS(new MalletLBFGSPrm());
        public Regularizer regularizer = new L2(1.0);
        //TODO: public InitParams initParams = InitParams.UNIFORM;
    }
        
    private CrfTrainerPrm prm; 
        
    public CrfTrainer(CrfTrainerPrm prm) {
        this.prm = prm;
    }
    
    // TODO: finish this method.
    public FgModel train(FgModel model, FgExamples data) {
        Function objective = new CrfObjective(model.getNumParams(), data, prm.infFactory);
        if (prm.regularizer != null) {
            prm.regularizer.setNumDimensions(model.getNumParams());
            objective = new AddFunctions(objective, prm.regularizer);
        }
        double[] initial = model.getParams();
        // TODO: how to initialize the model parameters?
        double[] params = prm.maximizer.maximize(objective, initial);
        model.setParams(params);
        return model;
    }
    
}
