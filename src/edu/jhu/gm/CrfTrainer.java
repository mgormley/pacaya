package edu.jhu.gm;

import edu.jhu.gm.BeliefPropagation.BeliefPropagationPrm;
import edu.jhu.util.Utilities;
import edu.jhu.util.vector.SortedIntDoubleVector;

/**
 * Trainer for a conditional random field (CRF) represented as a factor graph.
 * 
 * @author mgormley
 *
 */
// TODO: This currently does NOT support VarType.LATENT variables. Assert this. Then implement a version that does.
public class CrfTrainer {

    // TODO: Add an option which computes the gradient on only a subset of the
    // variables for use by SGD.
    public static class CrfObjective implements Function {

        private FgModel model;
        private FgExamples data;
        private FgInferencer inferencer;

        public CrfObjective(FgModel model, FgExamples data, FgInferencer inferencer) {
            this.model = model;
            this.data = data;
            this.inferencer = inferencer;
        }
        
        /**
         * Gets the conditional log-likelihood of the model for the given model parameters.
         * @inheritDoc
         */
        @Override
        public double getValue(double[] params) {
            double ll = 0.0;
            for (int i=0; i<data.size(); i++) {
                FgExample ex = data.get(i);
                // Add in the log of the numerator.
                for (int a=0; a<model.getNumFactors(); a++) {
                    ll += ex.getFeatureVector(a).dot(params);
                }
                // Subtract off the log of the denominator.                
                ll -= inferencer.getPartition();
            }
            return ll;
        }

        /**
         * Gets the gradient of the conditional log-likelihood.
         * @inheritDoc
         */
        @Override
        public double[] getGradient(double[] params) {
            double[] gradient = new double[params.length];
            for (int i=0; i<data.size(); i++) {
                FgExample ex = data.get(i);
                
                // TODO: factor out these options.
                // TODO: should inference be at the example level or is there a more elegant approach?
                BeliefPropagationPrm prm = new BeliefPropagationPrm(ex.getFactorGraph());
                FgInferencer inferencer = new BeliefPropagation(prm);
                inferencer.run();
                
                for (int a=0; a<model.getNumFactors(); a++) {
                    // TODO: Should the loop over factors be pushed into the FgExample and FgInferencer?
                    // Get the observed feature counts for this factor.
                    SortedIntDoubleVector observedFeats = ex.getFeatureVector(a);

                    // Compute the expected feature counts for this factor.
                    SortedIntDoubleVector expectedFeats = new SortedIntDoubleVector();//getExptCondFeatCounts(a, ex, model, inferencer);
                    Factor factorMarginal = inferencer.getMarginalsForFactorId(a);
                    for (int c=0; c<factorMarginal.getVars().getNumConfigs(); c++) {
                        // Get the log-probability of the c'th configuration for this factor.
                        // TODO: should factors just store probabilities?
                        double logProb = factorMarginal.getValue(c);
                        double prob = Utilities.exp(logProb);
                        // Get the feature counts when they are clamped to the c'th configuration for this factor.
                        SortedIntDoubleVector tmpFeats = ex.getFeatureVector(a, c);
                        // Scale the feature counts by the marginal probability of the c'th configuration.
                        tmpFeats.scale(prob);
                        // TODO: internally this add() will be very slow...we could speed it up if needed.
                        expectedFeats.add(tmpFeats);
                    }

                    // Update the gradient for each feature.
                    for (int j=0; j<params.length; j++) {
                        gradient[j] += observedFeats.get(j) - expectedFeats.get(j);
                    }
                }
            }
            return gradient;
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
    private FgExamples data;
    
    private Maximizer maximizer;
    private CrfObjective objective;
    
    // TODO: finish this method.
    public void train() {
        maximizer = new SGD();
        double[] initial = new double[model.getNumParams()];
        // TODO: how to initialize the model parameters?
        double[] params = maximizer.maximize(objective, initial);
        
    }
    


    
}
