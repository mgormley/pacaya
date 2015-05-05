package edu.jhu.pacaya.autodiff.erma;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import edu.jhu.pacaya.autodiff.AbstractModuleTest;
import edu.jhu.pacaya.autodiff.Module;
import edu.jhu.pacaya.autodiff.Tensor;
import edu.jhu.pacaya.autodiff.AbstractModuleTest.OneToOneFactory;
import edu.jhu.pacaya.gm.model.Var;
import edu.jhu.pacaya.gm.model.VarConfig;
import edu.jhu.pacaya.gm.model.VarSet;
import edu.jhu.pacaya.gm.model.VarTensor;
import edu.jhu.pacaya.gm.model.Var.VarType;
import edu.jhu.pacaya.util.collections.Lists;
import edu.jhu.pacaya.util.semiring.Algebra;
import edu.jhu.pacaya.util.semiring.RealAlgebra;

public class MeanSquaredErrorTest {
    
    private VarConfig goldConfig;
    private BeliefsIdentity id1;
    private Algebra s = RealAlgebra.REAL_ALGEBRA;

    @Before
    public void setUp() {
        Var t0 = new Var(VarType.PREDICTED, 2, "t0", Lists.getList("N", "V"));
        Var t1 = new Var(VarType.PREDICTED, 2, "t1", Lists.getList("N", "V"));
        t0.setId(0);
        t1.setId(1);
        
        goldConfig = new VarConfig();
        goldConfig.put(t0, 0);
        goldConfig.put(t1, 1);

        Beliefs b = new Beliefs(s);
        b.varBeliefs = new VarTensor[2];
        b.facBeliefs = new VarTensor[0];
        b.varBeliefs[0] = new VarTensor(s, new VarSet(t0), 0.0);
        b.varBeliefs[1] = new VarTensor(s, new VarSet(t1), 0.0);
        
        b.varBeliefs[0].setValue(0, 0.3); // Gold value
        b.varBeliefs[0].setValue(1, 0.7);
        b.varBeliefs[1].setValue(0, 0.4);
        b.varBeliefs[1].setValue(1, 0.6); // Gold value
        
        id1 = new BeliefsIdentity(b);
    }
    
    @Test
    public void testSimple() {
        Algebra s = RealAlgebra.REAL_ALGEBRA;        
        MeanSquaredError mse = new MeanSquaredError(id1, goldConfig);
        
        Tensor out = mse.forward();
        assertEquals(sq(1-0.3)+sq(0-.7)+sq(0-.4)+sq(1-.6), out.getValue(0), 1e-13);
        
        mse.getOutputAdj().setValue(0, 2.2);
        mse.backward();
        Beliefs inAdj = id1.getOutputAdj();
        assertEquals(2.2 * 2 * (.3-1), inAdj.varBeliefs[0].getValue(0), 1e-13);
        assertEquals(2.2 * 2 * (.7-0), inAdj.varBeliefs[0].getValue(1), 1e-13);
        assertEquals(2.2 * 2 * (.4-0), inAdj.varBeliefs[1].getValue(0), 1e-13);
        assertEquals(2.2 * 2 * (.6-1), inAdj.varBeliefs[1].getValue(1), 1e-13);        
    }

    private double sq(double d) {
        return d*d;
    }
    
    @Test
    public void testGradByFiniteDiffsAllSemirings() {
        OneToOneFactory<Beliefs,Tensor> fact = new OneToOneFactory<Beliefs,Tensor>() {
            public Module<Tensor> getModule(Module<Beliefs> m1) {
                return new MeanSquaredError(m1, goldConfig);
            }
        };        
        AbstractModuleTest.evalOneToOneByFiniteDiffsAbs(fact, id1);
    }
    
}
