package edu.jhu.hltcoe.inference;

import edu.jhu.hltcoe.data.SentenceCollection;
import edu.jhu.hltcoe.model.Model;
import edu.jhu.hltcoe.model.ModelFactory;

/**
 * 
 * @author mgormley
 *
 * @param <C> The type of expected counts
 */
public class EMTrainer<C> implements Trainer {

    private EStep<C> eStep;
    private MStep<C> mStep;
    private ModelFactory modelFactory;
    private int iterations;
    
    public EMTrainer(EStep<C> eStep, MStep<C> mStep, ModelFactory modelFactory, int iterations) {
        this.eStep = eStep;
        this.mStep = mStep;
        this.modelFactory = modelFactory;
        this.iterations = iterations;
    }
    
    @Override
    public void train(SentenceCollection sentences) {
        // Initialize the parameters of the model
        Model model = modelFactory.getInstance();
        
        // Run iterations of EM 
        for (int i=0; i<iterations; i++) {
            C counts = eStep.getCounts(sentences, model);
            model = mStep.getModel(counts);
        }
    }
    
}
