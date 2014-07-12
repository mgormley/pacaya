package edu.jhu.autodiff;

import static org.junit.Assert.assertTrue;
import edu.jhu.util.semiring.Algebra;
import edu.jhu.util.semiring.LogSignAlgebra;
import edu.jhu.util.semiring.RealAlgebra;

public class AbstractModuleTest {

    /** Factory for a module which takes one tensor modules as input. */
    public interface Tensor1Factory {
        Module<Tensor> getModule(Module<Tensor> m1);
    }
    
    /** Factory for a module which takes two tensor modules as input. */
    public interface Tensor2Factory {
        Module<Tensor> getModule(Module<Tensor> m1, Module<Tensor> m2);
    }

    /** Evaluation of a module which takes one tensor modules as input. */
    public static void evalTensor1(Tensor t1, Tensor expT1Adj, Tensor1Factory fact, Tensor expOut, double adjFill) {
        evalTensor1(t1, expT1Adj, fact, expOut, adjFill, new RealAlgebra());
        evalTensor1(t1, expT1Adj, fact, expOut, adjFill, new LogSignAlgebra());
    }

    private static void evalTensor1(Tensor t1, Tensor expT1Adj, 
            Tensor1Factory fact, Tensor expOut, double adjFill, Algebra tmpS) {
        TensorIdentity id1 = new TensorIdentity(t1);
        ConvertAlgebra id1Co = new ConvertAlgebra(id1, tmpS);
        Module<Tensor> ea = fact.getModule(id1Co);
        ConvertAlgebra eaCo = new ConvertAlgebra(ea, t1.getAlgebra());
    
        TopoOrder topo = new TopoOrder();
        topo.add(id1); 
        topo.add(id1Co);
        topo.add(ea);
        topo.add(eaCo);
        
        Tensor out = topo.forward();
        assertTensorEqual(expOut, out, 1e-10);
        assertTrue(out == topo.getOutput());
    
        // Set the adjoint of the sum to be 1.
        topo.getOutputAdj().fill(adjFill);
        topo.backward();
        assertTensorEqual(expT1Adj, id1.getOutputAdj(), 1e-10);
    }

    /** Evaluation of a module which takes two tensor modules as input. */
    public static void evalTensor2(Tensor t1, Tensor expT1Adj, Tensor t2, Tensor expT2Adj, Tensor2Factory fact, Tensor expOut, double adjFill) {
        evalTensor2(t1, expT1Adj, t2, expT2Adj, fact, expOut, adjFill, new RealAlgebra());
        evalTensor2(t1, expT1Adj, t2, expT2Adj, fact, expOut, adjFill, new LogSignAlgebra());
    }

    private static void evalTensor2(Tensor t1, Tensor expT1Adj, Tensor t2, Tensor expT2Adj,
            Tensor2Factory fact, Tensor expOut, double adjFill, Algebra tmpS) {
        Tensor.checkSameAlgebra(t1, t2);
        
        TensorIdentity id1 = new TensorIdentity(t1);
        ConvertAlgebra id1Co = new ConvertAlgebra(id1, tmpS);
        TensorIdentity id2 = new TensorIdentity(t2);
        ConvertAlgebra id2Co = new ConvertAlgebra(id2, tmpS);
        Module<Tensor> ea = fact.getModule(id1Co, id2Co);
        ConvertAlgebra eaCo = new ConvertAlgebra(ea, t1.getAlgebra());
    
        TopoOrder topo = new TopoOrder();
        topo.add(id1); 
        topo.add(id1Co);
        topo.add(id2); 
        topo.add(id2Co);
        topo.add(ea);
        topo.add(eaCo);
        
        Tensor out = topo.forward();
        assertTensorEqual(expOut, out, 1e-13);
        assertTrue(out == topo.getOutput());
    
        // Set the adjoint of the sum to be 1.
        topo.getOutputAdj().fill(adjFill);
        topo.backward();
        assertTensorEqual(expT1Adj, id1.getOutputAdj(), 1e-13);
        assertTensorEqual(expT2Adj, id2.getOutputAdj(), 1e-13);
    }

    private static void assertTensorEqual(Tensor expOut, Tensor out, double d) {
        assertTrue("Expected: " + expOut + " but was: " + out, expOut.equals(out, d));
    }

}
