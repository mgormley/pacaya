package edu.jhu.pacaya.gm.train;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.jhu.pacaya.autodiff.AbstractModule;
import edu.jhu.pacaya.autodiff.MVec;
import edu.jhu.pacaya.autodiff.Module;
import edu.jhu.pacaya.autodiff.Scalar;
import edu.jhu.pacaya.autodiff.Tensor;
import edu.jhu.pacaya.autodiff.erma.EmpiricalRisk;
import edu.jhu.pacaya.autodiff.erma.Factors;
import edu.jhu.pacaya.autodiff.erma.FactorsModule;
import edu.jhu.pacaya.autodiff.erma.MVecFgModel;
import edu.jhu.pacaya.gm.inf.FgInferencer;
import edu.jhu.pacaya.gm.inf.FgInferencerFactory;
import edu.jhu.pacaya.gm.model.Factor;
import edu.jhu.pacaya.gm.model.FactorGraph;
import edu.jhu.pacaya.gm.model.FgModel;
import edu.jhu.pacaya.gm.model.Var;
import edu.jhu.pacaya.gm.model.Var.VarType;
import edu.jhu.pacaya.gm.model.VarConfig;
import edu.jhu.pacaya.gm.model.VarTensor;
import edu.jhu.pacaya.gm.model.globalfac.GlobalFactor;
import edu.jhu.pacaya.util.collections.Lists;
import edu.jhu.pacaya.util.semiring.Algebra;
import edu.jhu.pacaya.util.semiring.LogSignAlgebra;
import edu.jhu.pacaya.util.semiring.RealAlgebra;

/**
 * Module for computing the log-likelihood of a factor graph.
 * 
 * <pre>
 * \log p_{\prms}(\vc{y}) = 
 *    \log \frac{1}{Z} \prod_{\alpha} \psi_{\alpha}(\vc{y}_{\alpha})
 * </pre>
 * 
 * @author mgormley
 */
public class LogLikelihood extends AbstractModule<Tensor> implements Module<Tensor> {

    private static final Logger log = LoggerFactory.getLogger(LogLikelihood.class);
    private static final double MAX_LOG_LIKELIHOOD = 1e-10;

    private FgInferencerFactory infFactory;
    private Module<MVecFgModel> mid;
    private VarConfig goldConfig;
    private FactorGraph fg;
    private FgInferencer inf;
    private Algebra tmpS;
    
    // Cached variables from forward() pass.
    private FactorsModule fm;

    /** Constructor which defaults to using the log-sign algebra internally. */
    // TODO: Switch from FgInferencerFactory to BeliefsFactory.
    public LogLikelihood(Module<MVecFgModel> mid, FactorGraph fg, FgInferencerFactory infFactory, VarConfig goldConfig) {
        this(mid, fg, infFactory, goldConfig, LogSignAlgebra.getInstance());
    }
    
    public LogLikelihood(Module<MVecFgModel> mid, FactorGraph fg, FgInferencerFactory infFactory, VarConfig goldConfig, Algebra tmpS) {
        super(RealAlgebra.getInstance());
        this.mid = mid;
        this.fg = fg;
        this.infFactory = infFactory;
        this.goldConfig = goldConfig;      
        this.tmpS = tmpS;
        for (Var v : fg.getVars()) {
            if (v.getType() == VarType.LATENT) {
                throw new IllegalStateException("Unable to handle factors graphs with latent variables");
            }
        }
    }

    @Override
    public Tensor forward() {        
        // Compute the potential tables.
        // TODO: Use these cached factors.
        fm = new FactorsModule(mid, fg, tmpS);
        Factors facs = fm.forward();
        
        // Compute the numerator.
        double numerator = tmpS.one();
        
        // "Multiply" in all the factors to the numerator. 
        for (int a=0; a<fg.getNumFactors(); a++) {
            Factor f = fg.getFactor(a);
            if (f instanceof GlobalFactor) {
                GlobalFactor gf = (GlobalFactor)f;
                VarConfig facConfig = goldConfig.getIntersection(fg.getFactor(a).getVars());
                numerator = tmpS.times(numerator, gf.getLogUnormalizedScore(facConfig));
            } else {
                VarTensor fac = facs.get(a);
                int facConfig = goldConfig.getConfigIndexOfSubset(f.getVars());
                numerator = tmpS.times(numerator, fac.getValue(facConfig));
            }
        }
        
        // Run inference to compute Z(x) by summing over the latent variables w and the predicted variables y.
        //fgLatPred = factors.getOutput().getFactorGraph();
        inf = infFactory.getInferencer(fg);
        inf.run();
        
        // Inference computes Z(x) by summing over the latent variables w and the predicted variables y.
        double denominator = tmpS.fromLogProb(inf.getLogPartition());

        // Compute the conditional log-likelihood for this example.
        double likelihood = tmpS.divide(numerator, denominator);
        double ll = tmpS.toLogProb(likelihood);
        log.trace(String.format("ll=%f numerator=%f denominator=%f", likelihood, numerator, denominator));

        if (ll > MAX_LOG_LIKELIHOOD) {
            // Note: this can occur if the graph is loopy because the
            // Bethe free energy has miss-estimated -log(Z) or because BP
            // has not yet converged.
            log.warn("Log-likelihood for example should be <= 0: " + ll);
        }
        
        return y = Scalar.getInstance(s, ll);
    }

    @Override
    public void backward() {
        FgModel gradient = mid.getOutputAdj().getModel();
        // For each factor...
        for (int a=0; a<fg.getNumFactors(); a++) {
            // If the factor is a global factor, backprop through CLL and the factor to the model.            
            // Otherwise, backprop through CLL only to the factor.
            Factor f = fg.getFactor(a);
            if (f instanceof GlobalFactor) {
                assert mid.getAlgebra() == RealAlgebra.getInstance();
                // TODO: Right now this only works for global factors which do not have any expected partials.
                ((GlobalFactor) f).addExpectedPartials(gradient, 1.0, null, a);
                ((GlobalFactor) f).addExpectedPartials(gradient, -1.0, inf, a);
            } else {
                // Compute 1.0 minus the marginal distrubtion.
                VarTensor marg = inf.getLogMarginalsForFactorId(a);
                Tensor addend = marg.copyAndConvertAlgebra(tmpS);
                addend.multiply(tmpS.fromReal(-1.0));
                int facConfig = goldConfig.getConfigIndexOfSubset(marg.getVars());
                addend.addValue(facConfig, tmpS.one());
                // Divide out the factor.
                VarTensor ft = fm.getOutput().f[a];
                addend.elemDivide(ft);
                
                // Multiply in the adjoint of the likelihood.
                assert s.equals(RealAlgebra.getInstance());
                addend.multiply(tmpS.fromReal(yAdj.get(0)));
                
                // Add the adjoint to the factors module.
                Factors factorsAdj = fm.getOutputAdj();
                VarTensor fAdj = factorsAdj.f[a];
                fAdj.elemAdd(addend);
                log.trace("margLatPred = {}\naddend = {}\nfAdj = {}", marg, addend, fAdj);
            }
        }
        // Backprop through the factors to the model. 
        fm.backward();
    }

    @Override
    public List<? extends Module<? extends MVec>> getInputs() {
        return Lists.getList(mid);
    }

}
