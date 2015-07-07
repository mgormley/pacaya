package edu.jhu.pacaya.autodiff.vartensor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.jhu.pacaya.autodiff.MVec;
import edu.jhu.pacaya.gm.inf.BruteForceInferencer;
import edu.jhu.pacaya.gm.model.VarTensor;
import edu.jhu.pacaya.gm.model.globalfac.GlobalFactor;
import edu.jhu.pacaya.util.semiring.Algebra;

/**
 * This class lazily creates a {@link VarTensor} from a {@link GlobalFactor}. 
 * @author mgormley
 */
public class LazyVarTensor implements MVec {

    private static final Logger log = LoggerFactory.getLogger(LazyVarTensor.class);

    private GlobalFactor f;
    private final Algebra s;
    private VarTensor vt = null;
    private Double fillVal = null;
    
    public LazyVarTensor(GlobalFactor f, Algebra s) {
        this(f, s, null);
    }
    
    public LazyVarTensor(GlobalFactor f, Algebra s, Double fillVal) {
        this.f = f;
        this.s = s;
        this.fillVal = fillVal;
    }

    /* ---------------- Lazy Methods ------------ */
    
    @Override
    public MVec copy() {
        return new LazyVarTensor(f, s);
    }

    @Override
    public MVec copyAndConvertAlgebra(Algebra newS) {
        return new LazyVarTensor(f, newS);
    }

    @Override
    public MVec copyAndFill(double val) {
        return new LazyVarTensor(f, s, val);
    }
    
    @Override
    public Algebra getAlgebra() {
        return s;
    }

    @Override
    public void fill(double val) {
        fillVal = val;
    }
    
    /** Ensures that the VarTensor is loaded. */
    private void ensureVarTensor() {
        if (vt == null) {
            log.warn("Generating VarTensor for a GlobalFactor. This should only ever be done during testing.");
            vt = BruteForceInferencer.safeNewVarTensor(s, f);
            if (fillVal != null) {
                vt.fill(fillVal);
            }
        }
    }
    
    /* ---------------- Methods Forcing Creation of VarTensor ------------ */

    @Override
    public int size() {
        // TODO: return f.getVars().calcNumConfigs();
        ensureVarTensor();
        return vt.size();
    }

    @Override
    public double getValue(int idx) {
        ensureVarTensor();
        return vt.getValue(idx);
    }

    @Override
    public double setValue(int idx, double val) {
        ensureVarTensor();
        return vt.setValue(idx, val);
    }

    @Override
    public void elemAdd(MVec addend) {
        ensureVarTensor();
        vt.elemAdd(addend);
    }

}
