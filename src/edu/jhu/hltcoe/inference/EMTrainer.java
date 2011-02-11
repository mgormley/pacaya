package edu.jhu.hltcoe.inference;

import edu.jhu.hltcoe.data.SentenceCollection;
import edu.jhu.hltcoe.model.Model;
import edu.jhu.hltcoe.model.ModelFactory;

/**
 * 
 * @author mgormley
 *
 * @param <M> The model type
 * @param <C> The type of expected counts
 */
public class EMTrainer<M extends Model,C> implements Trainer {

    private EStep<M,C> eStep;
    private MStep<M,C> mStep;
    private ModelFactory<M> modelFactory;
    private int iterations;
    
    public EMTrainer(EStep<M,C> eStep, MStep<M,C> mStep, ModelFactory<M> modelFactory, int iterations) {
        this.eStep = eStep;
        this.mStep = mStep;
        this.modelFactory = modelFactory;
        this.iterations = iterations;
    }
    
    @Override
    public void train(SentenceCollection sentences) {
        // Initialize the parameters of the model
        M model = modelFactory.getInstance();
        
        // Run iterations of EM 
        for (int i=0; i<iterations; i++) {
            C counts = eStep.getCounts(sentences, model);
            model = mStep.getModel(counts);
        }
    }
    
}
