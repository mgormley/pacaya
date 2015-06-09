package edu.jhu.pacaya.gm.train;

import java.util.List;

import edu.jhu.pacaya.autodiff.AbstractModule;
import edu.jhu.pacaya.autodiff.MVec;
import edu.jhu.pacaya.autodiff.Module;
import edu.jhu.pacaya.autodiff.Scalar;
import edu.jhu.pacaya.autodiff.erma.Factors;
import edu.jhu.pacaya.autodiff.erma.FactorsModule;
import edu.jhu.pacaya.autodiff.erma.MVecFgModel;
import edu.jhu.pacaya.gm.inf.FgInferencer;
import edu.jhu.pacaya.gm.inf.FgInferencerFactory;
import edu.jhu.pacaya.gm.model.Factor;
import edu.jhu.pacaya.gm.model.FactorGraph;
import edu.jhu.pacaya.gm.model.FgModel;
import edu.jhu.pacaya.gm.model.VarConfig;
import edu.jhu.pacaya.gm.model.VarTensor;
import edu.jhu.pacaya.gm.model.globalfac.GlobalFactor;
import edu.jhu.pacaya.util.collections.Lists;
import edu.jhu.pacaya.util.semiring.LogSemiring;
import edu.jhu.pacaya.util.semiring.RealAlgebra;


public class MarginalLikelihood extends AbstractModule<Scalar> implements Module<Scalar> {

    private FgInferencerFactory infFactory;
    private Module<MVecFgModel> mid;
    private VarConfig goldConfig;
    private double weight;
    
    // Cached variables from forward() pass.
    private FactorsModule fmLatPred;
    private FactorsModule fmLat;
    private FactorGraph fgLatPred;
    private FactorGraph fgLat;
    private FgInferencer infLatPred;
    private FgInferencer infLat;
    
    // TODO: Switch from FgInferencerFactory to BeliefsFactory.
    public MarginalLikelihood(Module<MVecFgModel> mid, FactorGraph fg, FgInferencerFactory infFactory, VarConfig goldConfig, double weight) {
        super(LogSemiring.LOG_SEMIRING);
        this.mid = mid;
        this.fgLatPred = fg;
        this.infFactory = infFactory;
        this.goldConfig = goldConfig;
        this.weight = weight;
    }

    @Override
    public Scalar forward() {
        // Compute the potential tables.
        // TODO: Use these cached factors.
        fmLatPred = new FactorsModule(mid, fgLatPred, s);
        fmLatPred.forward();
        fgLat = CrfObjective.getFgLat(fgLatPred, goldConfig);        
        fmLat = new FactorsModule(mid, fgLat, s);
        fmLat.forward();
        
        // Run inference to compute Z(x) by summing over the latent variables w and the predicted variables y.
        //fgLatPred = factors.getOutput().getFactorGraph();
        infLatPred = infFactory.getInferencer(fgLatPred);
        infLatPred.run();
        
        // Run inference to compute Z(y,x) by summing over the latent variables w.
        infLat = infFactory.getInferencer(fgLat);
        infLat.run();
        
        // Compute the conditional log-likelihood for this example.
        double ll = CrfObjective.getValue(fgLat, infLat, fgLatPred, infLatPred, -1, goldConfig, weight);
        y = new Scalar(ll);
        return y;
    }

    @Override
    public void backward() {
        FgModel gradient = mid.getOutputAdj().getModel();
        // For each factor...
        for (int a=0; a<fgLatPred.getNumFactors(); a++) {
            // If the factor is a global factor, backprop through CLL and the factor to the model.            
            // Otherwise, backprop through CLL only to the factor.
            Factor fLatPred = fgLatPred.getFactor(a);
            Factor fLat = fgLat.getFactor(a);
            if (fLatPred instanceof GlobalFactor) {
                assert mid.getAlgebra() == RealAlgebra.REAL_ALGEBRA;
                ((GlobalFactor) fLat).addExpectedPartials(gradient, 1.0 * weight, infLat, a);
                ((GlobalFactor) fLatPred).addExpectedPartials(gradient, -1.0 * weight, infLatPred, a);
            } else {
                Factors factorsAdj = fmLatPred.getOutputAdj();
                VarTensor fAdj = factorsAdj.f[a];
                assert fAdj.getAlgebra() == LogSemiring.LOG_SEMIRING;
                //fAdj.elemAdd(infLat.getLogMarginalsForFactorId(a));
                fAdj.elemSubtract(infLatPred.getLogMarginalsForFactorId(a));
            }
        }
        CrfObjective.addGradient(fgLat, infLat, fgLatPred, infLatPred, weight, gradient);
        
        // Backprop through the factors to the model. 
        fmLatPred.backward();
    }

    @Override
    public List<? extends Module<? extends MVec>> getInputs() {
        return Lists.getList(mid);
    }

}
