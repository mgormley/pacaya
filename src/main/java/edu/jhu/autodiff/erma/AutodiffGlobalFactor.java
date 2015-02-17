package edu.jhu.autodiff.erma;

import edu.jhu.autodiff.MVecArray;
import edu.jhu.autodiff.Module;
import edu.jhu.autodiff.MutableModule;
import edu.jhu.autodiff.Scalar;
import edu.jhu.gm.model.VarTensor;
import edu.jhu.gm.model.globalfac.GlobalFactor;

/**
 * A differentiable global factor.
 * 
 * Global factors break the mold of a Module in two ways. First, we never want
 * to instantiate the implicit VarTensor that represents the factor output of a
 * forward pass (i.e. Module.forward()). Second, we should never assume access
 * to the adjoint accumulator (i.e. Module.getOutputAdj()) for use by its backward
 * pass (i.e. Module.backward()). Third, all of these implicit representations of the 
 * GlobalFactor and its adjoints are accessible to ErmaBp by calls to GlobalFactor.createMessages()
 * and GlobalFactor.backwardCreateMessages().
 * 
 * @author mgormley
 */
public interface AutodiffGlobalFactor extends AutodiffFactor, GlobalFactor {
    
    /**
     * Module which computes and sets the messages from this global factor to each of its variables.
     * 
     * The returned module is "mutable" in that prior to a forward / backward call, it expects a
     * setOutput() and setOutputAdj() respectively to be called.
     * 
     * @param modIn The incoming messages to this factor.
     * @param fm The factor module created by this.{@link #getFactorModule(Module, edu.jhu.util.semiring.Algebra)}.
     */
    MutableModule<MVecArray<VarTensor>> getCreateMessagesModule(Module<MVecArray<VarTensor>> modIn, Module<?> fm);
    
    /**
     * Module which gets the expected log beliefs for this factor. We include factor's potential function in the
     * expectation since for most constraint factors \chi(x_a) \in \{0,1\}.
     * <p>
     * E[ln(b(x_a) / \chi(x_a)) ] = \sum_{x_a} b(x_a) ln (b(x_a) / \chi(x_a))
     * <p>
     * Note: The value should be returned as a real, though the messages may be in a different
     * semiring.
     * 
     * @param modIn The incoming messages to this factor.
     * @param fm The factor module created by this.{@link #getFactorModule(Module, edu.jhu.util.semiring.Algebra)}.
     * @return The expected log belief.
     */
    Module<Scalar> getExpectedLogBeliefModule(Module<MVecArray<VarTensor>> modIn, Module<?> fm);
    
}
