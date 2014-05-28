package edu.jhu.autodiff;

import java.util.List;

import edu.jhu.util.collections.Lists;

/**
 * Sum of all the entries in the tensor.
 * @author mgormley
 */
public class Select extends AbstractTensorModule implements Module<Tensor> {

    private Module<Tensor> modIn;
    private int dim; // In comments below, d.
    private int idx; // In comments below, k.
    
    public Select(Module<Tensor> modIn, int dim, int idx) {
        this.modIn = modIn;
        this.dim = dim;
        this.idx = idx;
    }
    
    /** Foward pass: y[i] = x[j], where j = (i1, i2, ..., i(d-1), k, i(d+1), ..., i(n)) */
    @Override
    public Tensor forward() {
        Tensor x = modIn.getOutput();
        int[] yDims = getYDimsFromXDims(x.getDims());
        y = new Tensor(yDims);
        DimIter yIter = new DimIter(y.getDims());
        while (yIter.hasNext()) {
            int[] yIdx = yIter.next();
            int[] xIdx = getXIdxFromYIdx(yIdx);
            y.set(yIdx, x.get(xIdx));
        }
        return y;
    }

    /** Backward pass: dG/dx_i = dG/dy dy/dx_i = dG/dy */
    @Override
    public void backward() {
        Tensor xAdj = modIn.getOutputAdj();
        DimIter yIter = new DimIter(yAdj.getDims());
        while (yIter.hasNext()) {
            int[] yIdx = yIter.next();
            int[] xIdx = getXIdxFromYIdx(yIdx);
            xAdj.add(xIdx, yAdj.get(yIdx));
        }
    }

    int[] getYDimsFromXDims(int[] xDims) {
        int[] yDims = new int[xDims.length-1];
        for (int i=0; i<yDims.length; i++) {
            if (i < dim) {
                yDims[i] = xDims[i];
            } else {
                yDims[i] = xDims[i+1];
            }
        }
        return yDims;
    }

    int[] getXIdxFromYIdx(int[] yIdx) {
        int[] xIdx = new int[yIdx.length+1];
        for (int i=0; i<xIdx.length; i++) {
            if (i < dim) {
                xIdx[i] = yIdx[i];
            } else if (i == dim) {
                xIdx[dim] = idx;        
            } else {
                xIdx[i] = yIdx[i-1];
            }
        }
        return xIdx;
    }

    @Override
    public List<Module<Tensor>> getInputs() {
        return Lists.getList(modIn);
    }

}
