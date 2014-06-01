package edu.jhu.autodiff.erma;

import java.util.List;

import edu.jhu.autodiff.AbstractTensorModule;
import edu.jhu.autodiff.ConvertAlgebra;
import edu.jhu.autodiff.Exp;
import edu.jhu.autodiff.Module;
import edu.jhu.autodiff.ScalarDivide;
import edu.jhu.autodiff.Tensor;
import edu.jhu.autodiff.TensorIdentity;
import edu.jhu.autodiff.TopoOrder;
import edu.jhu.util.collections.Lists;
import edu.jhu.util.semiring.Algebra;

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
    
    private Module<Tensor> pIn;
    private double temperature;
    private TopoOrder topo;
    private Algebra outS;
    private Algebra tmpS;
    
    public SoftmaxMbrDepParse(Module<Tensor> margIn, double temperature, Algebra tmpS) {
        this.pIn = margIn;
        this.temperature = temperature;
        this.outS = margIn.getAlgebra();
        this.tmpS = tmpS;
    }
    
    @Override
    public Tensor forward() {
        topo = new TopoOrder();
        
        // Internally we use a different algebra to avoid numerical precision problems.
        ConvertAlgebra pIn1 = new ConvertAlgebra(pIn, tmpS);
        topo.add(pIn1);
        
        TensorIdentity ti = new TensorIdentity(Tensor.getScalarTensor(tmpS, temperature));
        topo.add(ti);
        ScalarDivide divide = new ScalarDivide(pIn, ti, 0);
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
    
}
