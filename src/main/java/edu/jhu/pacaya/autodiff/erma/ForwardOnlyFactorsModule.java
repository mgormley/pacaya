package edu.jhu.pacaya.autodiff.erma;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.jhu.pacaya.autodiff.AbstractModule;
import edu.jhu.pacaya.autodiff.MVec;
import edu.jhu.pacaya.autodiff.Module;
import edu.jhu.pacaya.gm.inf.BruteForceInferencer;
import edu.jhu.pacaya.gm.model.Factor;
import edu.jhu.pacaya.gm.model.FactorGraph;
import edu.jhu.pacaya.gm.model.VarTensor;
import edu.jhu.pacaya.gm.model.globalfac.GlobalFactor;
import edu.jhu.pacaya.util.collections.QLists;
import edu.jhu.pacaya.util.semiring.Algebra;

/**
 * Module for creating factors by querying only getLogUnormalizedScore().
 * 
 * @author mgormley
 */
class ForwardOnlyFactorsModule extends AbstractModule<Factors> implements Module<Factors> {

    private static final Logger log = LoggerFactory.getLogger(ForwardOnlyFactorsModule.class);
    
    private Module<MVecFgModel> modIn;
    private FactorGraph fg;
    private List<Module<?>> facMods;
    
    public ForwardOnlyFactorsModule(Module<MVecFgModel> modIn, FactorGraph fg, Algebra s) {
        super(s);
        this.modIn = modIn;
        this.fg = fg;
    }
    
    @Override
    public Factors forward() {
        // Get modules that create the factors.
        facMods = new ArrayList<>();
        for (int a = 0; a < fg.getNumFactors(); a++) {
            Factor f = fg.getFactor(a);
            if (f instanceof GlobalFactor) {
                List<? extends Module<? extends MVec>> inputs = new ArrayList<>();
                facMods.add(new ParamFreeGlobalFactorModule(s, (GlobalFactor)f, inputs));
            } else {
                facMods.add(new ParamFreeFactorModule(s, f));
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
        return y;
    }
    
    @Override
    public Factors getOutputAdj() {
        if (yAdj == null) {
            yAdj = new Factors(y.s, facMods);
            yAdj.f = new VarTensor[y.f.length];
            for (int a = 0; a < y.f.length; a++) {
                Module<?> fm = facMods.get(a);
                if (fg.getFactor(a) instanceof GlobalFactor) {
                    yAdj.f[a] = null;
                } else {
                    // Call getOutputAdj() only on regular factors.
                    Object o = fm.getOutputAdj();
                    if (o instanceof VarTensor) {
                        yAdj.f[a] = (VarTensor) o;
                        assert !yAdj.f[a].containsBadValues();
                    } else {
                        throw new RuntimeException("Unexpected type returned by factor module: " + o.getClass());
                    }
                }
            }
        }
        return yAdj;
    }

    @Override
    public void backward() {
        throw new IllegalStateException("Operation not supported");
    }

    @Override
    public List<Module<MVecFgModel>> getInputs() {
        return QLists.getList();
    }
    
    private static class FactorToVarTensorModule extends AbstractModule<VarTensor> implements Module<VarTensor> {
        
        private Factor f;
                
        public FactorToVarTensorModule(Algebra s, Factor f) {
            super(s);
            this.f = f;
        }

        @Override
        public VarTensor forward() {
            y = BruteForceInferencer.safeNewVarTensor(s, f);
            return y;
        }

        @Override
        public void backward() {
            throw new IllegalStateException("Operation not supported");
        }

        @Override
        public List<? extends Module<? extends MVec>> getInputs() {
            return QLists.getList();
        }
        
    }

}
