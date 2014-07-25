package edu.jhu.autodiff;

import static org.junit.Assert.assertTrue;
import edu.jhu.autodiff.ModuleTestUtils.ModuleFn;
import edu.jhu.autodiff.tensor.ConvertAlgebra;
import edu.jhu.prim.vector.IntDoubleDenseVector;
import edu.jhu.util.collections.Lists;
import edu.jhu.util.semiring.Algebra;
import edu.jhu.util.semiring.Algebras;
import edu.jhu.util.semiring.LogSignAlgebra;
import edu.jhu.util.semiring.RealAlgebra;

public class AbstractModuleTest {
    
    /** Factory for a module which takes one modules as input. */
    public interface OneToOneFactory<X extends ModuleTensor<X>,Y extends ModuleTensor<Y>> {
        Module<Y> getModule(Module<X> m1);
    }
        
    /** Factory for a module which takes two modules as input. */
    public interface TwoToOneFactory<W extends ModuleTensor<W>, X extends ModuleTensor<X>, Y extends ModuleTensor<Y>> {
        Module<Y> getModule(Module<W> m1, Module<X> m2);
    }
    
    /** Factory for a module which takes one tensor modules as input. */
    public interface Tensor1Factory extends OneToOneFactory<Tensor,Tensor> { }
    
    /** Factory for a module which takes two tensor modules as input. */
    public interface Tensor2Factory extends TwoToOneFactory<Tensor, Tensor, Tensor> { }

    /** Evaluation of a module which takes one tensor modules as input. */
    public static void evalTensor1(Tensor t1, Tensor expT1Adj, Tensor1Factory fact, Tensor expOut, double adjFill) {
        evalTensor1(t1, expT1Adj, fact, expOut, adjFill, new RealAlgebra());
        evalTensor1(t1, expT1Adj, fact, expOut, adjFill, new LogSignAlgebra());
    }

    private static void evalTensor1(Tensor t1, Tensor expT1Adj, 
            Tensor1Factory fact, Tensor expOut, double adjFill, Algebra tmpS) {
        TensorIdentity id1 = new TensorIdentity(t1);
        ConvertAlgebra<Tensor> id1Co = new ConvertAlgebra<Tensor>(id1, tmpS);
        Module<Tensor> ea = fact.getModule(id1Co);
        ConvertAlgebra<Tensor> eaCo = new ConvertAlgebra<Tensor>(ea, t1.getAlgebra());
    
        TopoOrder<Tensor> topo = new TopoOrder<Tensor>(Lists.getList(id1), eaCo);
        
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
        ConvertAlgebra<Tensor> id1Co = new ConvertAlgebra<Tensor>(id1, tmpS);
        TensorIdentity id2 = new TensorIdentity(t2);
        ConvertAlgebra<Tensor> id2Co = new ConvertAlgebra<Tensor>(id2, tmpS);
        Module<Tensor> main = fact.getModule(id1Co, id2Co);
        ConvertAlgebra<Tensor> mainCo = new ConvertAlgebra<Tensor>(main, t1.getAlgebra());
    
        TopoOrder<Tensor> topo = new TopoOrder<Tensor>(Lists.getList(id1, id2), mainCo);
        
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

    /** Same as below, but uses one 3 dimensional input tensors. */
    public static void evalTensor1ByFiniteDiffs(Tensor1Factory fact) {
        Tensor t1 = TensorUtils.getVectorFromValues(Algebras.REAL_ALGEBRA, 2, 3, 5);
        TensorIdentity in1 = new TensorIdentity(t1);
        evalTensor1ByFiniteDiffs(fact, in1);
    }

    /**
     * Evaluates a tensor module by finite differences. This tensor module takes one tensor as
     * input, and will be tested on multiple semirings.
     */
    public static void evalTensor1ByFiniteDiffs(Tensor1Factory fact, Module<Tensor> in1) {        
        evalOneToOneByFiniteDiffs(fact, in1);
    }
    
    /**
     * Evaluates a tensor module by finite differences. This tensor module takes one tensor as
     * input, and will be tested on multiple semirings.
     * NOTE: This method is just a variant of the one above which always makes the input to the tested
     * module non-negative.
     */
    public static void evalTensor1ByFiniteDiffsAbs(Tensor1Factory fact, Module<Tensor> in1) {        
        evalOneToOneByFiniteDiffsAbs(fact, in1);
    }
    
    /**
     * Evaluates a module by finite differences. This module takes one module as
     * input, and will be tested on multiple semirings.
     */
    public static <X extends ModuleTensor<X>, Y extends ModuleTensor<Y>> void evalOneToOneByFiniteDiffs(OneToOneFactory<X,Y> fact, Module<X> in1) {        
        assert in1.getAlgebra().equals(Algebras.REAL_ALGEBRA);
        
        for (Algebra s : Lists.getList(Algebras.REAL_ALGEBRA, Algebras.LOG_SIGN_ALGEBRA)) {
            Module<X> in1Co = new ConvertAlgebra<X>(in1, s);
            Module<Y> main = fact.getModule(in1Co);
            Module<Y> mainCo = new ConvertAlgebra<Y>(main, Algebras.REAL_ALGEBRA);
            
            TopoOrder<Y> topo = new TopoOrder<Y>(Lists.getList(in1), mainCo);
            ModuleTestUtils.assertFdAndAdEqual(topo, 1e-5, 1e-8);
        }
    }
    
    /**
     * Evaluates a module by finite differences. This module takes one module as
     * input, and will be tested on multiple semirings.
     * 
     * NOTE: This method is just a variant of the one above which always makes the input to the tested
     * module non-negative.
     */
    public static <X extends ModuleTensor<X>, Y extends ModuleTensor<Y>> void evalOneToOneByFiniteDiffsAbs(OneToOneFactory<X,Y> fact, Module<X> in1) {        
        assert in1.getAlgebra().equals(Algebras.REAL_ALGEBRA);
        
        for (Algebra s : Lists.getList(Algebras.REAL_ALGEBRA, Algebras.LOG_SIGN_ALGEBRA)) {
            Module<X> in1Co = new ConvertAlgebra<X>(in1, s);
            Module<Y> main = fact.getModule(in1Co);
            Module<Y> mainCo = new ConvertAlgebra<Y>(main, Algebras.REAL_ALGEBRA);
            
            TopoOrder<Y> topo = new TopoOrder<Y>(Lists.getList(in1), mainCo);
            int numParams = ModuleFn.getInputSize(topo.getInputs());
            IntDoubleDenseVector x = ModuleTestUtils.getAbsZeroOneGaussian(numParams);
            ModuleTestUtils.assertFdAndAdEqual(topo, x, 1e-5, 1e-8);
        }
    }
    
    /** Same as below, but uses two 3 dimensional input tensors. */
    public static void evalTensor2ByFiniteDiffs(Tensor2Factory fact) {
        Tensor t1 = TensorUtils.getVectorFromValues(Algebras.REAL_ALGEBRA, 2, 3, 5);
        Tensor t2 = TensorUtils.getVectorFromValues(Algebras.REAL_ALGEBRA, 4, 6, 7);
        TensorIdentity in1 = new TensorIdentity(t1);
        TensorIdentity in2 = new TensorIdentity(t2);
        evalTensor2ByFiniteDiffs(fact, in1, in2);
    }
    
    /**
     * Evaluates a tensor module by finite differences. This tensor module takes two tensors as
     * input, and will be tested on multiple semirings.
     */
    public static void evalTensor2ByFiniteDiffs(Tensor2Factory fact, Module<Tensor> in1, Module<Tensor> in2) {        
        evalTwoToOneByFiniteDiffs(fact, in1, in2);
    }
    
    /**
     * Evaluates a tensor module by finite differences. This tensor module takes two tensors as
     * input, and will be tested on multiple semirings.
     * 
     * NOTE: This method is just a variant of the one above which always makes the input to the tested
     * module non-negative.
     */
    public static void evalTensor2ByFiniteDiffsAbs(Tensor2Factory fact, Module<Tensor> in1, Module<Tensor> in2) {        
        evalTwoToOneByFiniteDiffsAbs(fact, in1, in2);
    }
        
    /**
     * Evaluates a module by finite differences. This module takes two modules as
     * input, and will be tested on multiple semirings.
     */
    public static <W extends ModuleTensor<W>, X extends ModuleTensor<X>, Y extends ModuleTensor<Y>> void evalTwoToOneByFiniteDiffs(
            TwoToOneFactory<W, X, Y> fact, Module<W> in1, Module<X> in2) {        
        assert in1.getAlgebra().equals(Algebras.REAL_ALGEBRA);
        assert in2.getAlgebra().equals(Algebras.REAL_ALGEBRA);
        
        for (Algebra s : Lists.getList(Algebras.REAL_ALGEBRA, Algebras.LOG_SIGN_ALGEBRA)) {
            Module<W> in1Co = new ConvertAlgebra<W>(in1, s);
            Module<X> in2Co = new ConvertAlgebra<X>(in2, s);
            Module<Y> main = fact.getModule(in1Co, in2Co);
            Module<Y> mainCo = new ConvertAlgebra<Y>(main, Algebras.REAL_ALGEBRA);
            
            TopoOrder<Y> topo = new TopoOrder<Y>(Lists.getList(in1, in2), mainCo);
            ModuleTestUtils.assertFdAndAdEqual(topo, 1e-5, 1e-8);
        }
    }
    
    /**
     * Evaluates a module by finite differences. This module takes two modules as
     * input, and will be tested on multiple semirings.
     * 
     * NOTE: This method is just a variant of the one above which always makes the input to the tested
     * module non-negative.
     */
    public static <W extends ModuleTensor<W>, X extends ModuleTensor<X>, Y extends ModuleTensor<Y>> void evalTwoToOneByFiniteDiffsAbs(
            TwoToOneFactory<W, X, Y> fact, Module<W> in1, Module<X> in2) {        
        assert in1.getAlgebra().equals(Algebras.REAL_ALGEBRA);
        assert in2.getAlgebra().equals(Algebras.REAL_ALGEBRA);
        
        for (Algebra s : Lists.getList(Algebras.REAL_ALGEBRA, Algebras.LOG_SIGN_ALGEBRA)) {
            Module<W> in1Co = new ConvertAlgebra<W>(in1, s);
            Module<X> in2Co = new ConvertAlgebra<X>(in2, s);
            Module<Y> main = fact.getModule(in1Co, in2Co);
            Module<Y> mainCo = new ConvertAlgebra<Y>(main, Algebras.REAL_ALGEBRA);
            
            TopoOrder<Y> topo = new TopoOrder<Y>(Lists.getList(in1, in2), mainCo);
            int numParams = ModuleFn.getInputSize(topo.getInputs());
            IntDoubleDenseVector x = ModuleTestUtils.getAbsZeroOneGaussian(numParams);
            ModuleTestUtils.assertFdAndAdEqual(topo, x, 1e-5, 1e-8);
        }
    }

}
