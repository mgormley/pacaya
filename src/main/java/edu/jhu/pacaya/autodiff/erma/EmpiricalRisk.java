package edu.jhu.pacaya.autodiff.erma;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.jhu.pacaya.autodiff.AbstractModule;
import edu.jhu.pacaya.autodiff.MVec;
import edu.jhu.pacaya.autodiff.Module;
import edu.jhu.pacaya.autodiff.Scalar;
import edu.jhu.pacaya.autodiff.Tensor;
import edu.jhu.pacaya.autodiff.TopoOrder;
import edu.jhu.pacaya.gm.model.FactorGraph;
import edu.jhu.pacaya.gm.model.VarConfig;
import edu.jhu.pacaya.gm.train.MtFactory;
import edu.jhu.pacaya.util.collections.Lists;
import edu.jhu.pacaya.util.semiring.Algebra;
import edu.jhu.prim.util.Timer;
import edu.jhu.prim.util.TimerAdder;

/**
 * Module for computing the empirical risk for a single training example given a model as input.
 * 
 * @author mgormley
 */
public class EmpiricalRisk extends TopoOrder<Tensor> implements Module<Tensor> {
        
    private static final Logger log = LoggerFactory.getLogger(EmpiricalRisk.class);

    public static class EmpiricalRiskFactory implements MtFactory {

        private BeliefsModuleFactory bFactory;
        private DlFactory dlFactory;
        
        public EmpiricalRiskFactory(BeliefsModuleFactory bFactory, DlFactory dlFactory) {
            this.bFactory = bFactory;
            this.dlFactory = dlFactory;
        }
        
        @Override
        public Module<Tensor> getInstance(FgModelIdentity mid, FactorGraph fg, VarConfig goldConfig, double weight,
                int curIter, int maxIter) {
            if (weight != 1.0) {
                throw new IllegalArgumentException("Weight not supported by EmpiricalRisk.");
            }
            return new EmpiricalRisk(fg, goldConfig, mid, bFactory, dlFactory, curIter, maxIter);
        }
        
    }
    
    private Module<MVecFgModel> mid;
    
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

    private FactorsModule effm;
    private Module<Beliefs> inf;
    private Module<Tensor> dl;
    
    public EmpiricalRisk(FactorGraph fg, VarConfig goldConfig, Module<MVecFgModel> mid, BeliefsModuleFactory bFactory,
            DlFactory dlFactory, int curIter, int maxIter) {
        super();
        this.mid = mid;

        // Get the modules.
        try (TimerAdder t0 = new TimerAdder(tot)) {
            try (TimerAdder t = new TimerAdder(initTimer)) {
                // Model initialization.
                effm = new FactorsModule(mid, fg, bFactory.getAlgebra());
                // Inference.
                inf = bFactory.getBeliefsModule(effm, fg);
                // Decoding and Loss.
                dl = dlFactory.getDl(goldConfig, effm, inf, curIter, maxIter);
                // Define topo order.
                shallowCopy(new TopoOrder<Tensor>(Lists.getList(mid), dl, "EmpiricalRisk"));
            }
        }
    }
    
    @Override
    public Tensor forward() {
        try (TimerAdder t0 = new TimerAdder(tot)) {            
            // Update the inferences with the current model parameters.
            // (This is usually where feature extraction happens.)
            try (TimerAdder t = new TimerAdder(updTimer)) {
                effm.forward();
            }
            
            // Run inference.
            try (TimerAdder t = new TimerAdder(infTimer)) {
                inf.forward();
            }
            
            // Decode and compute the loss for this example.
            try (TimerAdder t = new TimerAdder(valTimer)) {
                return dl.forward();
            }
        }
    }

    @Override
    public void backward() {
        try (TimerAdder t0 = new TimerAdder(tot)) {
            try (TimerAdder t = new TimerAdder(gradTimer)) {                
                super.backward();
            }
        }
    }
    
    // TODO: Use this somewhere.
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

    @Override
    public List<? extends Module<? extends MVec>> getInputs() {
        return Lists.getList(mid);
    }
}
