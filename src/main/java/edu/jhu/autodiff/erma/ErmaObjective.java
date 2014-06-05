package edu.jhu.autodiff.erma;

import org.apache.log4j.Logger;

import edu.jhu.autodiff.Module;
import edu.jhu.autodiff.Tensor;
import edu.jhu.gm.data.FgExampleList;
import edu.jhu.gm.data.LFgExample;
import edu.jhu.gm.inf.FgInferencerFactory;
import edu.jhu.gm.model.FactorGraph;
import edu.jhu.gm.model.FgModel;
import edu.jhu.gm.model.VarConfig;
import edu.jhu.gm.train.Accumulator;
import edu.jhu.gm.train.AvgBatchObjective.ExampleObjective;
import edu.jhu.util.Timer;
import edu.jhu.util.semiring.Algebra;
import edu.jhu.util.semiring.RealAlgebra;

public class ErmaObjective implements ExampleObjective {
    
    public interface DlFactory {
        /** Get a module which decodes then evaluates the loss. */
        Module<Tensor> getDl(FactorGraph fg, VarConfig goldConfig, Module<Beliefs> inf, int curIter, int maxIter);
    }
    
    private static final Logger log = Logger.getLogger(ErmaObjective.class);

    private FgExampleList data;
    private FgInferencerFactory infFactory;
    private DlFactory dlFactory;
    
    // Timer: initialize.
    private Timer initTimer = new Timer();
    // Timer: update the model.
    private Timer updTimer = new Timer();
    // Timer: run inference.
    private Timer infTimer = new Timer();
    // Timer: compute the log-likelihood.
    private Timer valTimer = new Timer();
    // Timer: compute the gradient.
    private Timer gradTimer = new Timer();
    // Timer: total time for an example.
    private Timer tot = new Timer(); 
    
    // TODO: Change to Module<Belief> factory instead of FgInferencerFactory.
    public ErmaObjective(FgExampleList data, FgInferencerFactory infFactory, DlFactory dlFactory) {
        this.data = data;        
        this.infFactory = infFactory;
        this.dlFactory = dlFactory;
    }
    
    /** @inheritDoc */
    // Assumed by caller to be threadsafe.
    @Override
    public void accum(FgModel model, int i, Accumulator ac) {
        final Timer t0 = new Timer(); t0.start();        
        final Timer t = new Timer();
        
        final LFgExample ex = data.get(i);
        final FactorGraph fg = ex.getFgLatPred();
        final VarConfig goldConfig = ex.getGoldConfig();

        Algebra s = infFactory.getAlgebra();
        
        // Get the modules.
        t.reset(); t.start();
        // Model initialization.
        ExpFamFactorsModule effm = new ExpFamFactorsModule(fg, model, s);
        // Inference.
        ErmaBp inf = (ErmaBp) infFactory.getInferencer(fg);
        inf.setEffm(effm);
        // Decoding and Loss.
        Module<Tensor> dl = dlFactory.getDl(fg, goldConfig, inf, ac.curIter, ac.maxIter);
        t.stop(); initTimer.add(t);
        
        // Update the inferences with the current model parameters.
        // (This is usually where feature extraction happens.)
        t.reset(); t.start();
        effm.forward();
        t.stop(); updTimer.add(t);
        
        // Run inference.
        t.reset(); t.start();
        inf.forward();
        t.stop(); infTimer.add(t);

        // Decode and compute the loss for this example.
        t.reset(); t.start();
        dl.forward();
        t.stop(); valTimer.add(t);
        
        if (ac.accumValue) {
            // Add the loss.
            t.reset(); t.start();
            ac.value += dl.getOutput().get(0);
            t.stop(); valTimer.add(t);
        }
        if (ac.accumGradient) {
            // Compute the gradient for this example.
            t.reset(); t.start();
            dl.getOutputAdj().fill(1.0);
            dl.backward();
            inf.backward();
            effm.backward();
            ac.gradient.add(effm.getModelAdj());
            t.stop(); gradTimer.add(t);
        }
        if (ac.accumWeight) {
            ac.weight += ex.getWeight();
        }
        if (ac.accumTrainLoss) {
            ac.trainLoss += dl.getOutput().get(0);
        }
        t0.stop(); tot.add(t0);
    }
    
    /** Gets the number of examples in the training dataset. */
    @Override
    public int getNumExamples() {
        return data.size();
    }

    public void report() {
        if (log.isTraceEnabled()) {
            log.trace(String.format("Timers avg (ms): model=%.1f inf=%.1f val=%.1f grad=%.1f", 
                    updTimer.avgMs(), infTimer.avgMs(), valTimer.avgMs(), gradTimer.avgMs()));
        }
        double sum = initTimer.totMs() + updTimer.totMs() + infTimer.totMs() + valTimer.totMs() + gradTimer.totMs();
        double mult = 100.0 / sum;
        log.debug(String.format("Timers total%% (ms): init=%.1f model=%.1f inf=%.1f val=%.1f grad=%.1f avg=%.1f max=%.1f stddev=%.1f", 
                initTimer.totMs()*mult, updTimer.totMs()*mult, infTimer.totMs()*mult, valTimer.totMs()*mult, gradTimer.totMs()*mult,
                tot.avgMs(), tot.maxSplitMs(), tot.stdDevMs()));
    }
}