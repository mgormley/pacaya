package edu.jhu.pacaya.gm.train;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.jhu.pacaya.autodiff.AbstractModule;
import edu.jhu.pacaya.autodiff.MVec;
import edu.jhu.pacaya.autodiff.Module;
import edu.jhu.pacaya.autodiff.Scalar;
import edu.jhu.pacaya.autodiff.Tensor;
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
import edu.jhu.pacaya.util.semiring.Algebra;
import edu.jhu.pacaya.util.semiring.LogSignAlgebra;
import edu.jhu.pacaya.util.semiring.RealAlgebra;

/**
 * Module for computing the marginal likelihood of a factor graph. If there are latent variables in
 * the factor, these are marginalized out. If there are no latent variables this reduces to the
 * likelihood though in this case {@link Likelihood} would do the same computation faster.
 * 
 * <pre>
 * p_{\prms}(\vc{y}) = 
 *    \sum_{\vc{z}} \frac{1}{Z} \prod_{\alpha} \psi_{\alpha}(\vc{y}_{\alpha}, \vc{z}_{\alpha})
 * </pre>
 * 
 * @author mgormley
 */
public class MarginalLikelihood extends AbstractModule<Tensor> implements Module<Tensor> {

    private static final Logger log = LoggerFactory.getLogger(MarginalLikelihood.class);
    private static final double MAX_LOG_LIKELIHOOD = 1e-10;

    private FgInferencerFactory infFactory;
    private Module<MVecFgModel> mid;
    private VarConfig goldConfig;
    private Algebra tmpS;
    
    // Cached variables from forward() pass.
    private FactorsModule fmLatPred;
    private FactorsModule fmLat;
    private FactorGraph fgLatPred;
    private FactorGraph fgLat;
    private FgInferencer infLatPred;
    private FgInferencer infLat;

    // TODO: Switch from FgInferencerFactory to BeliefsFactory.
    public MarginalLikelihood(Module<MVecFgModel> mid, FactorGraph fg, FgInferencerFactory infFactory, VarConfig goldConfig) {
        this(mid, fg, infFactory, goldConfig, LogSignAlgebra.getInstance());
    }
    
    public MarginalLikelihood(Module<MVecFgModel> mid, FactorGraph fg, FgInferencerFactory infFactory, VarConfig goldConfig, Algebra tmpS) {
        super(RealAlgebra.getInstance());
        this.mid = mid;
        this.fgLatPred = fg;
        this.infFactory = infFactory;
        this.goldConfig = goldConfig;
        this.tmpS = tmpS;
    }

    @Override
    public Tensor forward() {
        // Compute the potential tables.
        // TODO: Use these cached factors.
        fmLatPred = new FactorsModule(mid, fgLatPred, tmpS);
        fmLatPred.forward();
        fgLat = CrfObjective.getFgLat(fgLatPred, goldConfig);        
        fmLat = new FactorsModule(mid, fgLat, tmpS);
        fmLat.forward();
        
        // Run inference to compute Z(x) by summing over the latent variables w and the predicted variables y.
        //fgLatPred = factors.getOutput().getFactorGraph();
        infLatPred = infFactory.getInferencer(fgLatPred);
        infLatPred.run();
        
        // Run inference to compute Z(y,x) by summing over the latent variables w.
        infLat = infFactory.getInferencer(fgLat);
        infLat.run();
        
        // Compute the conditional log-likelihood for this example.
        
        // Inference computes Z(y,x) by summing over the latent variables w.
        double numerator = tmpS.fromLogProb(infLat.getLogPartition());
        
        // Inference computes Z(x) by summing over the latent variables w and the predicted variables y.
        double denominator = tmpS.fromLogProb(infLatPred.getLogPartition());


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
        for (int a=0; a<fgLatPred.getNumFactors(); a++) {
            // If the factor is a global factor, backprop through CLL and the factor to the model.            
            // Otherwise, backprop through CLL only to the factor.
            Factor fLatPred = fgLatPred.getFactor(a);
            Factor fLat = fgLat.getFactor(a);
            if (fLatPred instanceof GlobalFactor) {
                assert mid.getAlgebra() == RealAlgebra.getInstance();
                ((GlobalFactor) fLat).addExpectedPartials(gradient, 1.0, infLat, a);
                ((GlobalFactor) fLatPred).addExpectedPartials(gradient, -1.0, infLatPred, a);
            } else {
                // Compute the difference of the marginal distrubtions.
                VarTensor margLat = infLat.getLogMarginalsForFactorId(a);
                VarTensor margLatPred = infLatPred.getLogMarginalsForFactorId(a);
                Tensor addend = margLat.copyAndConvertAlgebra(tmpS);
                addend.elemSubtract(margLatPred.copyAndConvertAlgebra(tmpS));
                // Divide out the factor itself.
                VarTensor ft = fmLatPred.getOutput().f[a];
                addend.elemDivide(ft);

                // Multiply in the adjoint of the likelihood. TODO
                assert s.equals(RealAlgebra.getInstance());
                addend.multiply(tmpS.fromReal(yAdj.get(0)));
                
                // Add the adjoint to the lat pred module only.
                Factors factorsAdj = fmLatPred.getOutputAdj();
                VarTensor fAdj = factorsAdj.f[a];
                fAdj.elemAdd(addend);
                log.trace("margLat = {}\nmargLatPred = {}\naddend = {}\nfAdj = {}", margLat, margLatPred, addend, fAdj);
            }
        }
        // Backprop through the factors to the model. 
        fmLatPred.backward();
    }

    @Override
    public List<? extends Module<? extends MVec>> getInputs() {
        return Lists.getList(mid);
    }

}
