package edu.jhu.hltcoe.gm;

/**
 * Trainer for a conditional random field (CRF) represented as a factor graph.
 * 
 * @author mgormley
 *
 */
public class CrfTrainer {

    public static class CrfObjective implements Function {

        private FgModel model;

        public CrfObjective(FgModel model) {
            this.model = model;
        }
        
        /**
         * Gets the conditional log-likelihood of the model for the given model parameters.
         * @inheritDoc  
         */
        @Override
        public double getValue(double[] params) {
            // TODO Auto-generated method stub
            return 0;
        }

        /**
         * Gets the derivative of the conditional log-likelihood.
         * @inheritDoc
         */
        @Override
        public double[] getDerivative(double[] params) {
            // TODO Auto-generated method stub
            return null;
        }

        /**
         * Gets the number of model parameters.
         */
        @Override
        public int getNumDimensions() {
            return model.getNumParams();
        }
        
    }
    
    private FgModel model;
    
    
}
