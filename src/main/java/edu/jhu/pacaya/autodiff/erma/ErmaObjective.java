package edu.jhu.pacaya.autodiff.erma;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.jhu.pacaya.autodiff.Module;
import edu.jhu.pacaya.autodiff.Tensor;
import edu.jhu.pacaya.gm.data.FgExampleList;
import edu.jhu.pacaya.gm.data.LFgExample;
import edu.jhu.pacaya.gm.model.FactorGraph;
import edu.jhu.pacaya.gm.model.FgModel;
import edu.jhu.pacaya.gm.model.VarConfig;
import edu.jhu.pacaya.gm.train.Accumulator;
import edu.jhu.pacaya.gm.train.AvgBatchObjective.ExampleObjective;
import edu.jhu.pacaya.util.semiring.Algebra;
import edu.jhu.prim.util.Timer;

public class ErmaObjective implements ExampleObjective {
    
    private static final Logger log = LoggerFactory.getLogger(ErmaObjective.class);

    private FgExampleList data;
    private BeliefsModuleFactory bFactory;
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
    
    public ErmaObjective(FgExampleList data, BeliefsModuleFactory bFactory, DlFactory dlFactory) {
        this.data = data;        
        this.bFactory = bFactory;
        this.dlFactory = dlFactory;
    }
    
    /** @inheritDoc */
    // Assumed by caller to be threadsafe.
    @Override
    public void accum(FgModel model, int i, Accumulator ac) {
        try {
            accumWithException(model, i, ac);
        } catch(Throwable t) {
            log.error("Skipping example " + i + " due to throwable: " + t.getMessage());
            t.printStackTrace();
        }
    }
    
    public void accumWithException(FgModel model, int i, Accumulator ac) {
        final Timer t0 = new Timer(); t0.start();        
        final Timer t = new Timer();
        
        final LFgExample ex = data.get(i);
        final FactorGraph fg = ex.getFactorGraph();
        final VarConfig goldConfig = ex.getGoldConfig();
        
        // Get the modules.
        t.reset(); t.start();
        // Model initialization.
        FgModelIdentity mid = new FgModelIdentity(model);
        FactorsModule effm = new FactorsModule(mid, fg, bFactory.getAlgebra());
        // Inference.
        Module<Beliefs> inf = bFactory.getBeliefsModule(effm, fg);
        // Decoding and Loss.
        Module<Tensor> dl = dlFactory.getDl(goldConfig, effm, inf, ac.curIter, ac.maxIter);
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
        
        // Output algebra.
        Algebra outS = dl.getAlgebra();
        // Loss in real algebra.
        double loss = outS.toReal(dl.getOutput().get(0));
        if (ac.accumValue) {
            // Add the loss.
            t.reset(); t.start();
            ac.value += loss;
            t.stop(); valTimer.add(t);
        }
        if (ac.accumGradient) {
            // Compute the gradient for this example.
            t.reset(); t.start();
            dl.getOutputAdj().fill(outS.one());
            dl.backward();
            inf.backward();
            effm.backward();
            // Currently, model adj is always returned in the real semiring.
            ac.gradient.add(mid.getOutputAdj().getModel());
            t.stop(); gradTimer.add(t);
        }
        if (ac.accumWeight) {
            ac.weight += ex.getWeight();
        }
        if (ac.accumLoss) {
            ac.loss += loss;
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
        log.debug(String.format("Timers: init=%.1f%% model=%.1f%% inf=%.1f%% val=%.1f%% grad=%.1f%% avg(ms)=%.1f max(ms)=%.1f stddev(ms)=%.1f", 
                initTimer.totMs()*mult, updTimer.totMs()*mult, infTimer.totMs()*mult, valTimer.totMs()*mult, gradTimer.totMs()*mult,
                tot.avgMs(), tot.maxSplitMs(), tot.stdDevMs()));
    }
}
