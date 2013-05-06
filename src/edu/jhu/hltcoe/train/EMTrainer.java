package edu.jhu.hltcoe.train;

import org.apache.log4j.Logger;
import edu.jhu.hltcoe.util.Timer;

import edu.jhu.hltcoe.model.Model;
import edu.jhu.hltcoe.model.ModelFactory;
import edu.jhu.hltcoe.util.Pair;
import edu.jhu.hltcoe.util.Utilities;

/**
 * 
 * @author mgormley
 *
 * @param <C> The type of expected counts
 */
public class EMTrainer<C> implements Trainer<C> {

    public static class EMTrainerPrm {
        public int iterations = 50;
        public double convergenceRatio = 0.99999;
        public int numRestarts = 0;
        public double timeoutSeconds = Double.POSITIVE_INFINITY;
        public EMTrainerPrm() { }
        public EMTrainerPrm(int iterations, double convergenceRatio, int numRestarts, double timeoutSeconds) {
            this.iterations = iterations;
            this.convergenceRatio = convergenceRatio;
            this.numRestarts = numRestarts;
            this.timeoutSeconds = timeoutSeconds;
        }
        /** Copy constructor */
        public EMTrainerPrm(EMTrainerPrm other) {
            this.iterations = other.iterations;
            this.convergenceRatio = other.convergenceRatio;
            this.numRestarts = other.numRestarts;
            this.timeoutSeconds = other.timeoutSeconds;
        }
    }
    
    private Logger log = Logger.getLogger(EMTrainer.class);

    private EMTrainerPrm prm;
    
    private EStep<C> eStep;
    private MStep<C> mStep;
    private ModelFactory modelFactory;
    private Model model;
    private int iterCount;
    private Double logLikelihood;
    private C counts;
    
    public EMTrainer(EMTrainerPrm prm, EStep<C> eStep, MStep<C> mStep, ModelFactory modelFactory) {
        this.prm = prm;
        this.eStep = eStep;
        this.mStep = mStep;
        this.modelFactory = modelFactory;
    }
    
    @Override
    public void train(TrainCorpus corpus) {
        double bestLogLikelihood = Double.NEGATIVE_INFINITY;
        Model bestModel = null;
        C bestCounts = null;
        Timer roundTimer = new Timer();
        for (int r=0; r<=prm.numRestarts; r++) {
            roundTimer.start();
            trainOnce(corpus);
            if (logLikelihood > bestLogLikelihood) {
                bestLogLikelihood = logLikelihood;
                bestModel = model;
                bestCounts = counts;
                evalIncumbent(bestModel, bestCounts, bestLogLikelihood);
            }
            if (roundTimer.totSec() > prm.timeoutSeconds) {
                // Timeout reached.
                break;
            }
            roundTimer.stop();
        }
        evalIncumbent(bestModel, bestCounts, bestLogLikelihood);
        log.info("bestLogLikelihood: " + bestLogLikelihood);
        logLikelihood = bestLogLikelihood;
        model = bestModel;
        counts = bestCounts;
    }
    
    /**
     * Override this method.
     */
    protected void evalIncumbent(Model bestModel, C bestCounts, double bestLogLikelihood) {
        return;
    }

    public void trainOnce(TrainCorpus corpus) {
        // Initialize the parameters of the model
        model = modelFactory.getInstance(corpus);
        
        // Run iterations of EM
        iterCount = 0;
        Timer iterTimer = new Timer();
        double prevLogLikelihood = Double.NEGATIVE_INFINITY;

        while (true) {
            iterTimer.start();
            log.info("iteration = " + getIterationsCompleted());
            
            // E-step 
            Pair<C,Double> pair = eStep.getCountsAndLogLikelihood(corpus, model, iterCount);
            counts = pair.get1();
            logLikelihood = pair.get2();
            
            // Check for convergence or iteration limit
            log.info("logLikelihood = " + logLikelihood);
            log.info("likelihood ratio = " + Utilities.exp(prevLogLikelihood - logLikelihood));
            if (prevLogLikelihood - logLikelihood > Utilities.log(prm.convergenceRatio)) {
                log.info("Stopping training due to convergence");
                break;
            } else if (prevLogLikelihood == Double.NEGATIVE_INFINITY && logLikelihood == Double.NEGATIVE_INFINITY && iterCount > 0) {
                log.info("Stopping training due to consecutive likelihoods of negative infinity");
                break;
            } else if ( iterCount >= prm.iterations) {
                log.info("Stopping training at max iterations");
                break;
            }
            prevLogLikelihood = logLikelihood;
            
            // M-step
            model = mStep.getModel(corpus, counts);
            
            log.debug("Time remaining: " + Timer.durAsStr(iterTimer.avgMs()*(prm.iterations - iterCount)));
            iterTimer.stop();
            iterCount++;
        }
    }

    public int getIterationsCompleted() {
        return iterCount;
    }
    
    public double getLogLikelihood() {
        return logLikelihood;
    }

    @Override
    public Model getModel() {
        return model;
    }
    
    public C getCounts() {
        return counts;
    }
            
    public void setLogger(Logger logger) {
        this.log = logger;
    }
        
}
