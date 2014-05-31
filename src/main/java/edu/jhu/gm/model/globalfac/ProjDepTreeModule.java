package edu.jhu.gm.model.globalfac;

import java.util.List;

import org.apache.log4j.Logger;

import edu.jhu.autodiff.AbstractTensorModule;
import edu.jhu.autodiff.ElemDivide;
import edu.jhu.autodiff.ElemMultiply;
import edu.jhu.autodiff.Module;
import edu.jhu.autodiff.Prod;
import edu.jhu.autodiff.ScalarAdd;
import edu.jhu.autodiff.ScalarMultiply;
import edu.jhu.autodiff.Select;
import edu.jhu.autodiff.Tensor;
import edu.jhu.autodiff.TensorIdentity;
import edu.jhu.autodiff.erma.InsideOutsideDepParse;
import edu.jhu.parse.dep.EdgeScores;
import edu.jhu.prim.tuple.Pair;
import edu.jhu.prim.util.math.FastMath;
import edu.jhu.util.collections.Lists;
import edu.jhu.util.semiring.Algebra;
import edu.jhu.util.semiring.LogSemiring;

public class ProjDepTreeModule implements Module<Pair<Tensor, Tensor>> {

    private Module<Tensor> mTrueIn;
    private Module<Tensor> mFalseIn;
    private List<Module<Tensor>> topoOrder;
    private Module<Tensor> mTrueOut;
    private Module<Tensor> mFalseOut;
    private Algebra s;

    private static final Logger log = Logger.getLogger(ProjDepTreeFactor.class);
    
    /** The sentence length. */
    private int n;
    
    // Counters.
    private static int unsafeLogSubtracts = 0;
    private static int logSubtractCount = 0;
    private static int extremeOddsRatios = 0;
    private static int oddsRatioCount = 0;
    
    public ProjDepTreeModule(Module<Tensor> mTrueIn, Module<Tensor> mFalseIn, Algebra s) {
        this.mTrueIn = mTrueIn;
        this.mFalseIn = mFalseIn;
        this.s = s;
    }
    
    @Override
    public Pair<Tensor, Tensor> forward() {
        // Construct the circuit.
        Tensor tmTrueIn = mTrueIn.getOutput();
        Tensor tmFalseIn = mFalseIn.getOutput();
        n = tmTrueIn.getDims()[1];
        
        requireNoZeros(tmFalseIn);
        
        Prod pi = new Prod(mFalseIn);
        ElemDivide weights = new ElemDivide(mTrueIn, mFalseIn);
        
        InsideOutsideDepParse parse = new InsideOutsideDepParse(weights, s);
        Select alphas = new Select(parse, 0, InsideOutsideDepParse.ALPHA_IDX);
        Select betas = new Select(parse, 0, InsideOutsideDepParse.BETA_IDX);
        Select root = new Select(parse, 0, InsideOutsideDepParse.ROOT_IDX); // The first entry in this selection is for the root.
        
        ElemMultiply edgeSums = new ElemMultiply(alphas, betas);
        ScalarMultiply bTrue = new ScalarMultiply(edgeSums, pi, 0);
        
        ScalarMultiply partition = new ScalarMultiply(pi, root, 0);
        TensorIdentity neg1 = new TensorIdentity(Tensor.getScalarTensor(s.fromReal(-1.0)));
        ScalarMultiply negBTrue = new ScalarMultiply(bTrue, neg1, 0);
        ScalarAdd bFalse = new ScalarAdd(negBTrue, partition, 0);
        
        ElemDivide mTrueOut0 = new ElemDivide(bTrue, mTrueIn);
        ElemDivide mFalseOut0 = new ElemDivide(bFalse, mFalseIn);
        
        // If the incoming message contained zeros, send back the same message.
        Tensor inProd = tmTrueIn.copy();
        inProd.elemMultiply(tmTrueIn);
        mTrueOut = new TakeLeftIfZero(mTrueIn, mTrueOut0, inProd);
        mFalseOut = new TakeLeftIfZero(mFalseIn, mFalseOut0, inProd);
        
        topoOrder = Lists.getList(pi, weights, parse, alphas, betas, root, edgeSums, bTrue,
                partition, neg1, negBTrue, bFalse, mTrueOut0, mFalseOut0, mTrueOut, mFalseOut);

        // Forward pass.
        for (Module<Tensor> module : topoOrder) {
            module.forward();
            if (module == partition) {
                // Correct if partition function is too small.
                checkAndFixPartition(bTrue, partition); // TODO: semiring
            } else if (module == weights && s instanceof LogSemiring) {
                // Check odds ratios for potential floating point precision errors.
                checkLogOddsRatios(EdgeScores.tensorToEdgeScores(weights.getOutput()));
            }
        }

        Tensor tmTrueOut = mTrueOut.getOutput();
        Tensor tmFalseOut = mFalseOut.getOutput();       
        assert !tmTrueOut.containsNaN() && !tmFalseOut.containsNaN();
        
        return getOutput();
    }

    @Override
    public void backward() {
        // Backward pass.
        List<Module<Tensor>> rev = Lists.reverse(topoOrder);
        for (Module<Tensor> module : rev) {
            assert !module.getOutputAdj().containsNaN();
            module.backward();
            for (Object in : module.getInputs()) {
                Module<Tensor> inn = (Module<Tensor>) in;
                assert !inn.getOutputAdj().containsNaN();
            }
        } 
    }

    @Override
    public Pair<Tensor, Tensor> getOutput() {
        return new Pair<Tensor,Tensor>(mTrueOut.getOutput(), mFalseOut.getOutput());
    }

    @Override
    public Pair<Tensor, Tensor> getOutputAdj() {
        return new Pair<Tensor,Tensor>(mTrueOut.getOutputAdj(), mFalseOut.getOutputAdj());        
    }

    @Override
    public void zeroOutputAdj() {
        mTrueOut.zeroOutputAdj();
        mFalseOut.zeroOutputAdj();
    }

    @Override
    public List<? extends Object> getInputs() {
        return Lists.getList(mTrueIn, mFalseIn);
    }

    private void checkAndFixPartition(Module<Tensor> bTrue, Module<Tensor> module) {
        // Correct for the case where the partition function is smaller
        // than some of the beliefs.
        double max = bTrue.getOutput().getMax();
        if (max > module.getOutput().getValue(0)) {
            module.getOutput().setValue(0, max);
            unsafeLogSubtracts++;
        }
        logSubtractCount++;
    }
    
    private void checkLogOddsRatios(EdgeScores es) {       
        // Keep track of the minimum and maximum odds ratios, in order to detect
        // possible numerical precision issues.        
        double minOddsRatio = Double.POSITIVE_INFINITY;
        double maxOddsRatio = Double.NEGATIVE_INFINITY;

        for (int p = -1; p < n; p++) {
            for (int c = 0; c < n; c++) {
                double oddsRatio = es.getScore(p, c);
                // Check min/max.
                if (oddsRatio < minOddsRatio && oddsRatio != Double.NEGATIVE_INFINITY) {
                    // Don't count *negative* infinities when logging extreme odds ratios.
                    minOddsRatio = oddsRatio;
                }
                if (oddsRatio > maxOddsRatio) {
                    maxOddsRatio = oddsRatio;
                }
            }
        }

        // Check whether the max/min odds ratios (if added) would result in a
        // floating point error.
        oddsRatioCount++;
        if (FastMath.logSubtractExact(FastMath.logAdd(maxOddsRatio, minOddsRatio), maxOddsRatio) == Double.NEGATIVE_INFINITY) {
            extremeOddsRatios++;            
            log.debug(String.format("maxOddsRatio=%.20g minOddsRatio=%.20g", maxOddsRatio, minOddsRatio));
            log.debug(String.format("Proportion extreme odds ratios:  %f (%d / %d)", (double) extremeOddsRatios/ oddsRatioCount, extremeOddsRatios, oddsRatioCount));
            // We log the proportion of unsafe log-subtracts here only as a convenient way of highlighting the two floating point errors together.
            log.debug(String.format("Proportion unsafe log subtracts:  %f (%d / %d)", (double) unsafeLogSubtracts / logSubtractCount, unsafeLogSubtracts, logSubtractCount));
        }
    }

    private void requireNoZeros(Tensor tmFalseIn) {
        for (int i=0; i<n; i++) {
            for (int j=0; j<n; j++) {
                if ( tmFalseIn.get(i,j) == 0.0) {
                    throw new IllegalStateException("Hard constraints turning ON an edge are not supported.");
                }
            }
        }
    }
    
    /**
     * Element-wise operation which takes the left side if the mark is zero, and the right side
     * otherwise.
     * 
     * @author mgormley
     */
    private static class TakeLeftIfZero extends AbstractTensorModule implements Module<Tensor> {
        
        Module<Tensor> leftIn; 
        Module<Tensor> rightIn; 
        Tensor mark;
        
        public TakeLeftIfZero(Module<Tensor> left, Module<Tensor> right, Tensor mark) {
            this.leftIn = left;
            this.rightIn = right;
            this.mark = mark;
        }

        @Override
        public Tensor forward() {
            Tensor left = leftIn.getOutput();
            Tensor right = rightIn.getOutput();
            Tensor.checkEqualSize(left, right);
            y = left.copyAndFill(0.0);
            for (int c=0; c<y.size(); c++) {
                Tensor t = (mark.getValue(c) == 0.0) ? left : right;
                y.setValue(c, t.getValue(c));
            }
            return y;
        }

        @Override
        public void backward() {
            Tensor leftAdj = leftIn.getOutputAdj();
            Tensor rightAdj = rightIn.getOutputAdj();
            for (int c=0; c<yAdj.size(); c++) {
                Tensor t = (mark.getValue(c) == 0.0) ? leftAdj : rightAdj;
                t.addValue(c, yAdj.getValue(c));
            }
        }

        @Override
        public List<? extends Object> getInputs() {
            return Lists.getList(leftIn, rightIn);
        }
        
    }

}
