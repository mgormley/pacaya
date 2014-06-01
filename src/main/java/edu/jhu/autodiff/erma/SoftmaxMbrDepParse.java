package edu.jhu.autodiff.erma;

import java.util.List;

import org.apache.log4j.Logger;

import edu.jhu.autodiff.ConvertAlgebra;
import edu.jhu.autodiff.Exp;
import edu.jhu.autodiff.Module;
import edu.jhu.autodiff.ScalarDivide;
import edu.jhu.autodiff.Tensor;
import edu.jhu.autodiff.TensorIdentity;
import edu.jhu.autodiff.TopoOrder;
import edu.jhu.util.collections.Lists;
import edu.jhu.util.semiring.Algebra;
import edu.jhu.util.semiring.LogPosNegAlgebra;
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
public class SoftmaxMbrDepParse implements Module<Tensor> {

    private static final Logger log = Logger.getLogger(SoftmaxMbrDepParse.class);

    private Module<Tensor> pIn;
    private double temperature;
    private TopoOrder topo;
    // The module's semiring (i.e. input and output values will be in this semiring).
    private Algebra outS;
    // The internal semiring.
    private Algebra tmpS;
    
    /**
     * Constructor with a default internal semiring.
     * @param margIn The input marginals.
     * @param temperature The temperature (assumed to be in the REAL semiring).
     */
    public SoftmaxMbrDepParse(Module<Tensor> margIn, double temperature) {
        this(margIn, temperature, new LogPosNegAlgebra());
    }
    
    /**
     * Constructor.
     * @param margIn The input marginals.
     * @param temperature The temperature (assumed to be in the REAL semiring).
     * @param tmpS The semiring used only internally.
     */
    public SoftmaxMbrDepParse(Module<Tensor> margIn, double temperature, Algebra tmpS) {
        this.pIn = margIn;
        this.temperature = temperature;
        this.outS = margIn.getAlgebra();
        this.tmpS = tmpS;
    }
    
    @Override
    public Tensor forward() {
        topo = new TopoOrder();
        
        // Internally we use a different algebra (tmpS) to avoid numerical precision problems.
        ConvertAlgebra pIn1 = new ConvertAlgebra(pIn, tmpS);
        topo.add(pIn1);
        
        TensorIdentity ti = new TensorIdentity(Tensor.getScalarTensor(tmpS, tmpS.fromReal(temperature)));
        topo.add(ti);
        ScalarDivide divide = new ScalarDivide(pIn1, ti, 0);
        topo.add(divide);
        Exp exp = new Exp(divide);
        topo.add(exp);
        InsideOutsideDepParse io = new InsideOutsideDepParse(exp);
        topo.add(io);
        
        ConvertAlgebra io1 = new ConvertAlgebra(io, outS);
        topo.add(io1);
        
        return topo.forward();
    }

    @Override
    public void backward() {
        topo.backward();
    }

    @Override
    public List<Module<Tensor>> getInputs() {
        return Lists.getList(pIn);
    }

    @Override
    public Tensor getOutput() {
        return topo.getOutput();
    }

    @Override
    public Tensor getOutputAdj() {
        return topo.getOutputAdj();
    }

    @Override
    public void zeroOutputAdj() {
        topo.zeroOutputAdj();
    }

    @Override
    public Algebra getAlgebra() {
        return outS;
    }
    
    public void report() {
        for (Module<? extends Object> mm : topo.getTopoOrder()) {
            Module<Tensor> m = (Module<Tensor>) mm;
            System.out.println("Module: " + m.getClass());
            System.out.println("Algebra: " + m.getAlgebra().getClass());
            System.out.println("Output (reals): " + m.getOutput().copyAndConvertAlgebra(new RealAlgebra()));
            //System.out.println("OutputAdj (reals): " + m.getOutputAdj().copyAndConvertAlgebra(new RealAlgebra()));
            System.out.println("");
        }
    }
    
}
