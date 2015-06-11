package edu.jhu.pacaya.autodiff;

import static org.junit.Assert.assertTrue;

import java.util.List;

import edu.jhu.pacaya.autodiff.ModuleTestUtils.ModuleFn;
import edu.jhu.pacaya.autodiff.tensor.ConvertAlgebra;
import edu.jhu.pacaya.util.collections.Lists;
import edu.jhu.pacaya.util.semiring.Algebra;
import edu.jhu.pacaya.util.semiring.LogSignAlgebra;
import edu.jhu.pacaya.util.semiring.RealAlgebra;
import edu.jhu.pacaya.util.semiring.SplitAlgebra;
import edu.jhu.prim.vector.IntDoubleVector;

public class AbstractModuleTest {

    public interface VectorFactory {
        IntDoubleVector getVector(int numParams);
    }
    
    public static class StandardNormalVectorFactory implements VectorFactory {
        public IntDoubleVector getVector(int numParams) {
            return ModuleTestUtils.getZeroOneGaussian(numParams);
        }
    }

    public static class AbsStandardNormalVectorFactory implements VectorFactory {
        public IntDoubleVector getVector(int numParams) {
            return ModuleTestUtils.getAbsZeroOneGaussian(numParams);
        }
    }
    
    /** Factory for a module which takes one modules as input. */
    public interface OneToOneFactory<X extends MVec,Y extends MVec> {
        Module<Y> getModule(Module<X> m1);
    }
        
    /** Factory for a module which takes two modules as input. */
    public interface TwoToOneFactory<W extends MVec, X extends MVec, Y extends MVec> {
        Module<Y> getModule(Module<W> m1, Module<X> m2);
    }
    
    /** Factory for a module which takes one tensor modules as input. */
    public interface Tensor1Factory extends OneToOneFactory<Tensor,Tensor> { }
    
    /** Factory for a module which takes two tensor modules as input. */
    public interface Tensor2Factory extends TwoToOneFactory<Tensor, Tensor, Tensor> { }

    private static final List<Algebra> test2Algebras = Lists.getList(RealAlgebra.REAL_ALGEBRA, LogSignAlgebra.LOG_SIGN_ALGEBRA);
    private static final List<Algebra> test3Algebras = Lists.getList(RealAlgebra.REAL_ALGEBRA, SplitAlgebra.SPLIT_ALGEBRA, LogSignAlgebra.LOG_SIGN_ALGEBRA);
    
    /** Evaluation of a module which takes one tensor modules as input. */
    public static void evalTensor1(Tensor t1, Tensor expT1Adj, Tensor1Factory fact, Tensor expOut, double adjFill) {
        evalTensor1(t1, expT1Adj, fact, expOut, adjFill, RealAlgebra.REAL_ALGEBRA);
        evalTensor1(t1, expT1Adj, fact, expOut, adjFill, LogSignAlgebra.LOG_SIGN_ALGEBRA);
    }

    private static void evalTensor1(Tensor t1, Tensor expT1Adj, 
            Tensor1Factory fact, Tensor expOut, double adjFill, Algebra tmpS) {
        Identity<Tensor> id1 = new Identity<Tensor>(t1);
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
        evalTensor2OneAlgebra(t1, expT1Adj, t2, expT2Adj, fact, expOut, adjFill, RealAlgebra.REAL_ALGEBRA);
        evalTensor2OneAlgebra(t1, expT1Adj, t2, expT2Adj, fact, expOut, adjFill, LogSignAlgebra.LOG_SIGN_ALGEBRA);
    }

    /** Evaluation of a module which takes two tensor modules as input.
     * This version takes the value of the adjoint to be filled. 
     */
    public static void evalTensor2OneAlgebra(Tensor t1, Tensor expT1Adj, Tensor t2, Tensor expT2Adj,
            Tensor2Factory fact, Tensor expOut, double adjFill, Algebra tmpS) {
        Tensor adj = expOut.copy();
        adj.fill(adjFill);
        evalTensor2OneAlgebra(t1, expT1Adj, t2, expT2Adj, fact, expOut, adj, tmpS);
    }
    
    /** Evaluation of a module which takes two tensor modules as input.
     * This version takes the full adjoint. 
     */
    public static void evalTensor2OneAlgebra(Tensor t1, Tensor expT1Adj, Tensor t2, Tensor expT2Adj,
            Tensor2Factory fact, Tensor expOut, Tensor adj, Algebra tmpS) {
        Tensor.checkSameAlgebra(t1, t2);
        
        Identity<Tensor> id1 = new Identity<Tensor>(t1);
        ConvertAlgebra<Tensor> id1Co = new ConvertAlgebra<Tensor>(id1, tmpS);
        Identity<Tensor> id2 = new Identity<Tensor>(t2);
        ConvertAlgebra<Tensor> id2Co = new ConvertAlgebra<Tensor>(id2, tmpS);
        Module<Tensor> main = fact.getModule(id1Co, id2Co);
        ConvertAlgebra<Tensor> mainCo = new ConvertAlgebra<Tensor>(main, t1.getAlgebra());
    
        TopoOrder<Tensor> topo = new TopoOrder<Tensor>(Lists.getList(id1, id2), mainCo);
        
        Tensor out = topo.forward();
        assertTensorEqual(expOut, out, 1e-13);
        assertTrue(out == topo.getOutput());
    
        // Set the adjoint.
        adj = adj.copyAndConvertAlgebra(topo.getOutputAdj().getAlgebra());
        topo.getOutputAdj().set(adj);
        topo.backward();
        assertTensorEqual(expT1Adj, id1.getOutputAdj(), 1e-13);
        assertTensorEqual(expT2Adj, id2.getOutputAdj(), 1e-13);
    }

    private static void assertTensorEqual(Tensor expOut, Tensor out, double d) {
        assertTrue("Expected: " + expOut + " but was: " + out, expOut.equals(out, d));
    }

    /** Calls {@link #evalTensor1ByFiniteDiffs(Tensor1Factory, Module)} with one 3 dimensional input tensors. */
    public static void evalTensor1ByFiniteDiffs(Tensor1Factory fact) {
        Tensor t1 = TensorUtils.getVectorFromValues(RealAlgebra.REAL_ALGEBRA, 2, 3, 5);
        Identity<Tensor> in1 = new Identity<Tensor>(t1);
        evalTensor1ByFiniteDiffs(fact, in1);
    }

    /**
     * Calls {@link #evalOneToOneByFiniteDiffsAbs(OneToOneFactory, Module, VectorFactory)} with 0,1 Gaussian vector factory.
     */
    public static void evalTensor1ByFiniteDiffs(Tensor1Factory fact, Module<Tensor> in1) {        
        evalOneToOneByFiniteDiffs(fact, in1);
    }
    
    /**
     * Calls {@link #evalOneToOneByFiniteDiffsAbs(OneToOneFactory, Module, VectorFactory)} with positive side of 0,1 Gaussian vector factory.
     */
    public static void evalTensor1ByFiniteDiffsAbs(Tensor1Factory fact, Module<Tensor> in1) {        
        evalOneToOneByFiniteDiffsAbs(fact, in1);
    }
    
    /**
     * Calls {@link #evalOneToOneByFiniteDiffsAbs(OneToOneFactory, Module, VectorFactory)} with 0,1 Gaussian vector factory.
     */
    public static <X extends MVec, Y extends MVec> void evalOneToOneByFiniteDiffs(OneToOneFactory<X,Y> fact, Module<X> in1) {        
        evalOneToOneByFiniteDiffsAbs(fact, in1, new StandardNormalVectorFactory());
    }
    
    /**
     * Calls {@link #evalOneToOneByFiniteDiffsAbs(OneToOneFactory, Module, VectorFactory)} with positive side of 0,1 Gaussian vector factory.
     */
    public static <X extends MVec, Y extends MVec> void evalOneToOneByFiniteDiffsAbs(OneToOneFactory<X,Y> fact, Module<X> in1) {        
        evalOneToOneByFiniteDiffsAbs(fact, in1, new AbsStandardNormalVectorFactory());
    }
    
    /**
     * Evaluates a module by finite differences. This module takes one module as
     * input, and will be tested on multiple semirings.
     */
    private static <X extends MVec, Y extends MVec> void evalOneToOneByFiniteDiffsAbs(OneToOneFactory<X,Y> fact, Module<X> in1, VectorFactory vec) {        
        assert in1.getAlgebra().equals(RealAlgebra.REAL_ALGEBRA);
        
        for (Algebra s : test2Algebras) {
            Module<X> in1Co = new ConvertAlgebra<X>(in1, s);
            Module<Y> main = fact.getModule(in1Co);
            Module<Y> mainCo = new ConvertAlgebra<Y>(main, RealAlgebra.REAL_ALGEBRA);
            
            TopoOrder<Y> topo = new TopoOrder<Y>(Lists.getList(in1), mainCo);
            IntDoubleVector x = vec.getVector(ModuleFn.getOutputSize(topo.getInputs()));
            double delta = s.equals(SplitAlgebra.SPLIT_ALGEBRA) ? 1e-2 : 1e-7;
            ModuleTestUtils.assertGradientCorrectByFd(topo, x, 1e-5, delta);
        }
    }
    
    /**
     * Calls {@link #evalTensor2ByFiniteDiffs(Tensor2Factory, Module, Module, VectorFactory)} with two 3 dimensional input tensors.
     */
    public static void evalTensor2ByFiniteDiffs(Tensor2Factory fact) {
        Tensor t1 = TensorUtils.getVectorFromValues(RealAlgebra.REAL_ALGEBRA, 2, 3, 5);
        Tensor t2 = TensorUtils.getVectorFromValues(RealAlgebra.REAL_ALGEBRA, 4, 6, 7);
        Identity<Tensor> in1 = new Identity<Tensor>(t1);
        Identity<Tensor> in2 = new Identity<Tensor>(t2);
        evalTensor2ByFiniteDiffs(fact, in1, in2);
    }
        
    /**
     * Calls {@link #evalTwoToOneByFiniteDiffs(TwoToOneFactory, Module, Module, VectorFactory)} with 0,1 Gaussian vector factory.
     */
    public static void evalTensor2ByFiniteDiffs(Tensor2Factory fact, Module<Tensor> in1, Module<Tensor> in2) {        
        evalTwoToOneByFiniteDiffs(fact, in1, in2);
    }
    
    /**
     * Calls {@link #evalTwoToOneByFiniteDiffs(TwoToOneFactory, Module, Module, VectorFactory)} with positive side of 0,1 Gaussian vector factory.
     */
    public static void evalTensor2ByFiniteDiffsAbs(Tensor2Factory fact, Module<Tensor> in1, Module<Tensor> in2) {        
        evalTwoToOneByFiniteDiffsAbs(fact, in1, in2);
    }
        
    /**
     * Calls {@link #evalTwoToOneByFiniteDiffs(TwoToOneFactory, Module, Module, VectorFactory)} with 0,1 Gaussian vector factory.
     */
    public static <W extends MVec, X extends MVec, Y extends MVec> void evalTwoToOneByFiniteDiffs(
            TwoToOneFactory<W, X, Y> fact, Module<W> in1, Module<X> in2) {        
        evalTwoToOneByFiniteDiffs(fact, in1, in2, new StandardNormalVectorFactory());
    }
    
    /**
     * Calls {@link #evalTwoToOneByFiniteDiffs(TwoToOneFactory, Module, Module, VectorFactory)} with positive side of 0,1 Gaussian vector factory.
     */
    public static <W extends MVec, X extends MVec, Y extends MVec> void evalTwoToOneByFiniteDiffsAbs(
            TwoToOneFactory<W, X, Y> fact, Module<W> in1, Module<X> in2) {        
        evalTwoToOneByFiniteDiffs(fact, in1, in2, new AbsStandardNormalVectorFactory());
    }

    /**
     * Evaluates a module by finite differences. This module takes two modules as
     * input, and will be tested on multiple semirings.
     */
    public static <W extends MVec, X extends MVec, Y extends MVec> void evalTwoToOneByFiniteDiffs(
            TwoToOneFactory<W, X, Y> fact, Module<W> in1, Module<X> in2, VectorFactory vec) {        
        assert in1.getAlgebra().equals(RealAlgebra.REAL_ALGEBRA);
        assert in2.getAlgebra().equals(RealAlgebra.REAL_ALGEBRA);
        
        for (Algebra s : test2Algebras) {
            Module<W> in1Co = new ConvertAlgebra<W>(in1, s);
            Module<X> in2Co = new ConvertAlgebra<X>(in2, s);
            Module<Y> main = fact.getModule(in1Co, in2Co);
            Module<Y> mainCo = new ConvertAlgebra<Y>(main, RealAlgebra.REAL_ALGEBRA);
            
            TopoOrder<Y> topo = new TopoOrder<Y>(Lists.getList(in1, in2), mainCo);
            IntDoubleVector x = vec.getVector(ModuleFn.getOutputSize(topo.getInputs()));
            double delta = s.equals(SplitAlgebra.SPLIT_ALGEBRA) ? 1e-2 : 1e-8;
            ModuleTestUtils.assertGradientCorrectByFd(topo, x, 1e-5, delta);
        }
    }
    
    /** Tests that two modules (instantiated by factories) yield equal adjoints. */
    public static <X extends MVec, Y extends MVec> void checkOneToOneEqualAdjoints(OneToOneFactory<X, Y> fact1,
            OneToOneFactory<X, Y> fact2, Module<X> in1) {
        checkOneToOneEqualAdjointsAbs(fact1, fact2, in1, new StandardNormalVectorFactory());
    }

    /** Tests that two modules (instantiated by factories) yield equal adjoints. */
    public static <X extends MVec, Y extends MVec> void checkOneToOneEqualAdjointsAbs(OneToOneFactory<X, Y> fact1,
            OneToOneFactory<X, Y> fact2, Module<X> in1) {
        checkOneToOneEqualAdjointsAbs(fact1, fact2, in1, new AbsStandardNormalVectorFactory());
    }
    
    /** Tests that two modules (instantiated by factories) yield equal adjoints. */
    public static <X extends MVec, Y extends MVec> void checkOneToOneEqualAdjointsAbs(OneToOneFactory<X, Y> fact1,
            OneToOneFactory<X, Y> fact2, Module<X> in1, VectorFactory vec) {
        assert in1.getAlgebra().equals(RealAlgebra.REAL_ALGEBRA);
                
        for (Algebra s : test3Algebras) {
            System.out.println("Testing on Algebra: " + s);
            @SuppressWarnings("unchecked")
            Module<Y>[] topos = new Module[2];
            int i=0;
            for (OneToOneFactory<X,Y> fact : Lists.getList(fact1, fact2)) {
                Module<X> in1Co = new ConvertAlgebra<X>(in1, s);
                Module<Y> main = fact.getModule(in1Co);
                Module<Y> mainCo = new ConvertAlgebra<Y>(main, RealAlgebra.REAL_ALGEBRA);
                
                TopoOrder<Y> topo = new TopoOrder<Y>(Lists.getList(in1), mainCo);
                topos[i++] = topo;
            }
            IntDoubleVector x = vec.getVector(ModuleFn.getOutputSize(topos[0].getInputs()));
            double delta = s.equals(SplitAlgebra.SPLIT_ALGEBRA) ? 1e-2 : 1e-8;
            ModuleTestUtils.assertGradientEquals(topos[0], topos[1], x, 1e-5, delta);
        }
    }
    
}
