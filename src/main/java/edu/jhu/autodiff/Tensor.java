package edu.jhu.autodiff;

import edu.jhu.gm.model.VarTensor;
import edu.jhu.gm.model.Var;
import edu.jhu.gm.model.VarSet;
import edu.jhu.gm.model.Var.VarType;

public class Tensor extends VarTensor {

    public Tensor(int... dimensions) {
        super(getFakeVarSet(dimensions));
    }

    public Tensor(Tensor input) {
        super(input);
    }

    private static VarSet getFakeVarSet(int[] dims) {
        VarSet vars = new VarSet();
        for (int i=0; i<dims.length; i++) {
            vars.add(new Var(VarType.OBSERVED, dims[i], ""+i, null));
        }
        return vars;
    }

}
