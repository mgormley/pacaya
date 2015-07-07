package edu.jhu.pacaya.gm.train;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.jhu.pacaya.autodiff.Module;
import edu.jhu.pacaya.autodiff.Tensor;
import edu.jhu.pacaya.gm.data.FgExampleList;
import edu.jhu.pacaya.gm.data.LFgExample;
import edu.jhu.pacaya.gm.model.FactorGraph;
import edu.jhu.pacaya.gm.model.FgModel;
import edu.jhu.pacaya.gm.model.FgModelIdentity;
import edu.jhu.pacaya.gm.model.VarConfig;
import edu.jhu.pacaya.gm.train.AvgBatchObjective.ExampleObjective;
import edu.jhu.pacaya.util.semiring.Algebra;
import edu.jhu.prim.util.Timer;
import edu.jhu.prim.util.TimerAdder;

public class ModuleObjective implements ExampleObjective {
    
    private static final Logger log = LoggerFactory.getLogger(ModuleObjective.class);

    private FgExampleList data;
    private MtFactory mtFactory;
    
    // Timers.
    private Timer forwardTimer = new Timer();
    private Timer backwardTimer = new Timer();
    private Timer tot = new Timer(); 
    
    public ModuleObjective(FgExampleList data, MtFactory mtFactory) {
        this.data = data;        
        this.mtFactory = mtFactory;
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
        try (TimerAdder t0 = new TimerAdder(tot)) {        
            final LFgExample ex = data.get(i);
            final FactorGraph fg = ex.getFactorGraph();
            final VarConfig goldConfig = ex.getGoldConfig();
            final double weight = ex.getWeight();
            
            // Model initialization.
            FgModelIdentity mid = new FgModelIdentity(model);
            // Inference, decoding, loss, etc.
            Module<Tensor> mt;
            // Run the forward pass.
            try (TimerAdder t = new TimerAdder(forwardTimer)) {
                mt = mtFactory.getInstance(mid, fg, goldConfig, weight, ac.curIter, ac.maxIter);
                mid.forward();
                mt.forward();
            }
            
            // Output algebra.
            Algebra outS = mt.getAlgebra();
            // Loss in real algebra.
            double loss = outS.toReal(mt.getOutput().get(0));
            if (ac.accumValue) {
                // Add the loss.
                ac.value += loss;
            }
            if (ac.accumGradient) {
                // Compute the gradient for this example.
                try (TimerAdder t = new TimerAdder(backwardTimer)) {
                    mt.getOutputAdj().fill(outS.one());
                    // Set the output adjoint of the model to be our accumulator.
                    // Currently, model adj is always returned in the real semiring.
                    mid.getOutputAdj().setModel(ac.gradient);
                    mt.backward();
                }
            }
            if (ac.accumWeight) {
                ac.weight += ex.getWeight();
            }
            if (ac.accumLoss) {
                ac.loss += loss;
            }
        }
    }
    
    /** Gets the number of examples in the training dataset. */
    @Override
    public int getNumExamples() {
        return data.size();
    }

    public void report() {
        if (log.isTraceEnabled()) {
            log.trace(String.format("Timers avg (ms): forward=%.1f backward=%.1f", 
                    forwardTimer.avgMs(), backwardTimer.avgMs()));
        }
        double mult = 100.0 / tot.totMs();
        log.debug(String.format("Timers: forward=%.1f%% backward=%.1f%% avg(ms)=%.1f max(ms)=%.1f stddev(ms)=%.1f", 
                forwardTimer.totMs()*mult, backwardTimer.totMs()*mult,
                tot.avgMs(), tot.maxSplitMs(), tot.stdDevMs()));
    }
    
}
