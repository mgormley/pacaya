package edu.jhu.autodiff.erma;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.jhu.autodiff.AbstractModule;
import edu.jhu.autodiff.Module;
import edu.jhu.gm.model.Factor;
import edu.jhu.gm.model.FactorGraph;
import edu.jhu.gm.model.VarTensor;
import edu.jhu.gm.model.globalfac.GlobalFactor;
import edu.jhu.util.collections.Lists;
import edu.jhu.util.semiring.Algebra;

/**
 * Module for delegating the creation of factors.
 * 
 * @author mgormley
 */
public class FactorsModule extends AbstractModule<Factors> implements Module<Factors> {

    private static final Logger log = LoggerFactory.getLogger(FactorsModule.class);
    
    private Module<MVecFgModel> modIn;
    private FactorGraph fg;
    private List<Module<?>> facMods;
    
    public FactorsModule(Module<MVecFgModel> modIn, FactorGraph fg, Algebra s) {
        super(s);
        this.modIn = modIn;
        this.fg = fg;
    }
    
    @Override
    public Factors forward() {
        // Get modules that create the factors.
        facMods = new ArrayList<>();
        for (int a = 0; a < fg.getNumFactors(); a++) {
            Factor factor = fg.getFactor(a);
            if (factor instanceof AutodiffFactor) {
                AutodiffFactor fmf = (AutodiffFactor) factor;
                Module<?> fm = fmf.getFactorModule(modIn, s);
                facMods.add(fm);
            } else {
                throw new RuntimeException("Every factor must implement the Module interface. Do so for the class " + factor.getClass());
            }
        }
        // Forward pass and create output.
        y = new Factors(s, facMods);
        y.f = new VarTensor[fg.getNumFactors()];
        for (int a = 0; a < y.f.length; a++) {
            Module<?> fm = facMods.get(a);
            Object o = fm.forward();
            if (o instanceof VarTensor) {
                y.f[a] = (VarTensor) o;
                assert !y.f[a].containsBadValues();
            } else if (fg.getFactor(a) instanceof GlobalFactor) {
                y.f[a] = null;
            } else {
                throw new RuntimeException("Unexpected type returned by factor module: " + o.getClass());
            }
        }
        yAdj = null;
        return y;
    }

    @Override
    public Factors getOutputAdj() {
        if (yAdj == null) {
            yAdj = new Factors(y.s, facMods);
            yAdj.f = new VarTensor[y.f.length];
            for (int a = 0; a < y.f.length; a++) {
                Module<?> fm = facMods.get(a);
                Object o = fm.getOutputAdj();
                if (o instanceof VarTensor) {
                    yAdj.f[a] = (VarTensor) o;
                    assert !yAdj.f[a].containsBadValues();
                } else if (fg.getFactor(a) instanceof GlobalFactor) {
                    yAdj.f[a] = null;
                } else {
                    throw new RuntimeException("Unexpected type returned by factor module: " + o.getClass());
                }
            }
        }
        return yAdj;
    }

    @Override
    public void backward() {
        for (Module<?> fm : facMods) {
            fm.backward();
        }
    }

    @Override
    public List<Module<MVecFgModel>> getInputs() {
        // Note that ONLY the FgModel's module is considered the input. 
        // The factor creation modules are internal to this module.
        return Lists.getList(modIn);
    }

}
