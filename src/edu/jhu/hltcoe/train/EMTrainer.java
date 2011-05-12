package edu.jhu.hltcoe.train;

import org.apache.log4j.Logger;
import org.jboss.dna.common.statistic.Stopwatch;

import edu.jhu.hltcoe.data.SentenceCollection;
import edu.jhu.hltcoe.model.Model;
import edu.jhu.hltcoe.model.ModelFactory;
import edu.jhu.hltcoe.util.Time;

/**
 * 
 * @author mgormley
 *
 * @param <C> The type of expected counts
 */
public class EMTrainer<C> implements Trainer {

    private static Logger log = Logger.getLogger(EMTrainer.class);

    private EStep<C> eStep;
    private MStep<C> mStep;
    private ModelFactory modelFactory;
    private int iterations;
    private Model model;
    
    public EMTrainer(EStep<C> eStep, MStep<C> mStep, ModelFactory modelFactory, int iterations) {
        this.eStep = eStep;
        this.mStep = mStep;
        this.modelFactory = modelFactory;
        this.iterations = iterations;
    }
    
    @Override
    public void train(SentenceCollection sentences) {
        // Initialize the parameters of the model
        model = modelFactory.getInstance(sentences);
        
        // Run iterations of EM
        Stopwatch iterTimer = new Stopwatch();
        for (int i=0; i<iterations; i++) {
            iterTimer.start();
            C counts = eStep.getCounts(sentences, model);
            model = mStep.getModel(counts);
            iterTimer.stop();
            log.debug("Time remaining: " + Time.durAsStr(Time.avgMs(iterTimer)*(iterations - i)));
        }
    }

    @Override
    public Model getModel() {
        return model;
    }
    
}
