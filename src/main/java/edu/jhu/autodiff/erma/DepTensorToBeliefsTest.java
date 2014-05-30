package edu.jhu.autodiff.erma;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import edu.jhu.autodiff.ModuleTestUtils;
import edu.jhu.autodiff.Tensor;
import edu.jhu.autodiff.TensorIdentity;
import edu.jhu.gm.model.globalfac.ProjDepTreeFactor.LinkVar;

public class DepTensorToBeliefsTest {

    @Test
    public void test() {
        Tensor t1 = new Tensor(2,2);
        t1.setValuesOnly(ModuleTestUtils.getVector(.2, .3, .5, .7));
        TensorIdentity id1 = new TensorIdentity(t1);

        BeliefsIdentity inf = DepTensorFromBeliefsTest.getBeliefsModule();
        DepTensorToBeliefs s = new DepTensorToBeliefs(id1, inf);
        
        Beliefs out = s.forward();

        assertEquals(null, out.varBeliefs[0]);
        assertEquals(null, out.varBeliefs[1]);

        assertEquals(.3, out.varBeliefs[2].getValue(0), 1e-13);
        assertEquals(.7, out.varBeliefs[2].getValue(1), 1e-13);
        assertEquals(.5, out.varBeliefs[3].getValue(0), 1e-13);
        assertEquals(.5, out.varBeliefs[3].getValue(1), 1e-13);
               
        
        Beliefs outAdj = s.getOutputAdj();
        outAdj.fill(2.2);
        s.backward();
        Tensor inAdj = id1.getOutputAdj();
        assertEquals(0.0, inAdj.get(0,0), 1e-13); // -1, 0
        assertEquals(0.0, inAdj.get(0,1), 1e-13); //  0, 1
        assertEquals(2.2, inAdj.get(1,0), 1e-13); //  1, 0
        assertEquals(2.2, inAdj.get(1,1), 1e-13); // -1, 1        
    }

}
