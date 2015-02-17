package edu.jhu.autodiff;

/**
 * The returned module is "mutable" in that prior to a forward / backward call, it expects a
 * setOutput() and setOutputAdj() respectively to be called.
 * 
 * @author mgormley
 * @param <T>
 */
public interface MutableModule<T extends MVec> extends Module<T> {

    void setOutput(T y);

    void setOutputAdj(T yAdj);

}
