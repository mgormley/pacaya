package edu.jhu.autodiff.erma;

import edu.jhu.autodiff.MVec;
import edu.jhu.gm.model.FgModel;
import edu.jhu.prim.vector.IntDoubleVector;
import edu.jhu.util.semiring.Algebra;
import edu.jhu.util.semiring.Algebras;

public class MVecFgModel implements MVec<MVecFgModel> {

    private FgModel model;
    private IntDoubleVector params;
    private final Algebra s = Algebras.REAL_ALGEBRA;

    public MVecFgModel(FgModel model) {
        this.model = model;
        this.params = model.getParams();
    }
    
    @Override
    public int size() {
        return model.getNumParams();
    }
    
    @Override
    public double getValue(int idx) {
        return params.get(idx);
    }

    @Override
    public double setValue(int idx, double val) {
        return params.set(idx, val);
    }

    @Override
    public void elemAdd(MVecFgModel addend) {
        params.add(addend.params);
    }

    @Override
    public MVecFgModel copy() {
        return new MVecFgModel(model.getDenseCopy());
    }

    @Override
    public void fill(double val) {
        model.fill(val);
    }

    @Override
    public MVecFgModel copyAndFill(double val) {
        if (val == s.zero()) {
            return new MVecFgModel(model.getSparseZeroedCopy());
        } else {
            MVecFgModel copy = copy();
            copy.fill(val);
            return copy;
        }
    }

    @Override
    public MVecFgModel copyAndConvertAlgebra(Algebra newS) {
        if (newS.equals(this.getAlgebra())) {
            return copy();
        } else {
            throw new IllegalArgumentException("Unable to convert algebra");
        }
    }
    
    @Override
    public Algebra getAlgebra() {
        return s ;
    }

    public FgModel getModel() {
        return model;
    }

}
