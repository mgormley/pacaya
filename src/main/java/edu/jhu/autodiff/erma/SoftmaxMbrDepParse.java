package edu.jhu.autodiff.erma;

import java.util.List;

import org.apache.log4j.Logger;

import edu.jhu.autodiff.Module;
import edu.jhu.autodiff.Tensor;
import edu.jhu.autodiff.TensorIdentity;
import edu.jhu.autodiff.TopoOrder;
import edu.jhu.autodiff.tensor.ConvertAlgebra;
import edu.jhu.autodiff.tensor.ElemMultiply;
import edu.jhu.autodiff.tensor.Exp;
import edu.jhu.autodiff.tensor.ScalarDivide;
import edu.jhu.autodiff.tensor.Select;
import edu.jhu.util.collections.Lists;
import edu.jhu.util.semiring.Algebra;
import edu.jhu.util.semiring.LogSignAlgebra;
import edu.jhu.util.semiring.RealAlgebra;

/** 
 * Performs softmax MBR decoding for dependency parsing.
 * 
 * 1. Compute edge weights w_e = exp(p_{\theta}(y_e=1|x) / T)
 * 2. Run inside-outside on w_e to get q_{\theta}^{1/T}(e)
 * 
 * The input to this module is expected to be a tensor containing the edge weights for dependency
 * parsing. The tensor is expected to be an nxn matrix, capable of being converted to EdgeScores
 * internally by EdgeScores.tensorToEdgeScores().
 * 
 * @author mgormley
 */
public class SoftmaxMbrDepParse extends TopoOrder<Tensor> implements Module<Tensor> {

    private static final Logger log = Logger.getLogger(SoftmaxMbrDepParse.class);
    
    /**
     * Constructor with a default internal semiring.
     * @param margIn The input marginals.
     * @param temperature The temperature (assumed to be in the REAL semiring).
     */
    public SoftmaxMbrDepParse(Module<Tensor> margIn, double temperature) {
        this(margIn, temperature, new LogSignAlgebra());
    }
    
    /**
     * Constructor.
     * @param margIn The input marginals.
     * @param temperature The temperature (assumed to be in the REAL semiring).
     * @param tmpS The semiring used only internally.
     */
    public SoftmaxMbrDepParse(Module<Tensor> margIn, double temperature, Algebra tmpS) {
        super(Lists.getList(margIn), build(margIn, temperature, tmpS));
    }
    
    private static Module<Tensor> build(Module<Tensor> pIn, double temperature, Algebra tmpS) {
        Algebra outS = pIn.getAlgebra();

        // Internally we use a different algebra (tmpS) to avoid numerical precision problems.
        ConvertAlgebra<Tensor> pIn1 = new ConvertAlgebra<Tensor>(pIn, tmpS);
        
        TensorIdentity ti = new TensorIdentity(Tensor.getScalarTensor(tmpS, tmpS.fromReal(temperature)));
        ScalarDivide divide = new ScalarDivide(pIn1, ti, 0);
        Exp exp = new Exp(divide);
        InsideOutsideDepParse io = new InsideOutsideDepParse(exp);
        
        // Compute marginals
        Select alphas = new Select(io, 0, InsideOutsideDepParse.ALPHA_IDX);
        Select betas = new Select(io, 0, InsideOutsideDepParse.BETA_IDX);
        Select root = new Select(io, 0, InsideOutsideDepParse.ROOT_IDX); // The first entry in this selection is for the root.
        ElemMultiply edgeSums = new ElemMultiply(alphas, betas);
        ScalarDivide marg = new ScalarDivide(edgeSums, root, 0);
        
        ConvertAlgebra<Tensor> conv = new ConvertAlgebra<Tensor>(marg, outS);

        return conv;
    }

    public void report() {
        for (Module<? extends Object> mm : this.getTopoOrder()) {
            Module<Tensor> m = (Module<Tensor>) mm;
            System.out.println("Module: " + m.getClass());
            System.out.println("Algebra: " + m.getAlgebra().getClass());
            System.out.println("Output (reals): " + m.getOutput().copyAndConvertAlgebra(new RealAlgebra()));
            System.out.println("OutputAdj (reals): " + m.getOutputAdj().copyAndConvertAlgebra(new RealAlgebra()));
            System.out.println("");
        }
    }
    
}
