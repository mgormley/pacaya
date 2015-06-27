package edu.jhu.pacaya.autodiff.erma;

import static org.junit.Assert.*;

import org.junit.Test;

import edu.jhu.pacaya.gm.model.Var;
import edu.jhu.pacaya.gm.model.VarSet;
import edu.jhu.pacaya.gm.model.VarTensor;
import edu.jhu.pacaya.gm.model.Var.VarType;
import edu.jhu.pacaya.util.semiring.Algebra;
import edu.jhu.pacaya.util.semiring.RealAlgebra;

public class FactorsTest {

    @Test
    public void testSize() {
        Algebra s = RealAlgebra.getInstance();
        VarTensor[] fsArr = new VarTensor[2];
        fsArr[0] = new VarTensor(s, new VarSet(new Var(VarType.PREDICTED, 2, "v1", null)));
        fsArr[1] = new VarTensor(s, new VarSet(new Var(VarType.PREDICTED, 3, "v2", null)));
        Factors fs = new Factors(fsArr);
        assertEquals(5, fs.size());
    }

}
