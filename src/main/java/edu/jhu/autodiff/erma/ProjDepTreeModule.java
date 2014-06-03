package edu.jhu.autodiff.erma;

import java.util.List;

import org.apache.log4j.Logger;

import edu.jhu.autodiff.AbstractTensorModule;
import edu.jhu.autodiff.ConvertAlgebra;
import edu.jhu.autodiff.ElemDivide;
import edu.jhu.autodiff.ElemMultiply;
import edu.jhu.autodiff.ElemSubtract;
import edu.jhu.autodiff.Module;
import edu.jhu.autodiff.Prod;
import edu.jhu.autodiff.ScalarFill;
import edu.jhu.autodiff.ScalarMultiply;
import edu.jhu.autodiff.Select;
import edu.jhu.autodiff.Tensor;
import edu.jhu.autodiff.TensorIdentity;
import edu.jhu.gm.model.globalfac.ProjDepTreeFactor;
import edu.jhu.parse.dep.EdgeScores;
import edu.jhu.prim.tuple.Pair;
import edu.jhu.util.collections.Lists;
import edu.jhu.util.semiring.Algebra;
import edu.jhu.util.semiring.LogPosNegAlgebra;
import edu.jhu.util.semiring.LogSemiring;

public class ProjDepTreeModule implements Module<Pair<Tensor, Tensor>> {

    private Module<Tensor> mTrueIn;
    private Module<Tensor> mFalseIn;
    private List<Module<Tensor>> topoOrder;
    private Module<Tensor> mTrueOut;
    private Module<Tensor> mFalseOut;
    private Algebra outS;
    // For internal use only.
    private Algebra tmpS;

    private static final Logger log = Logger.getLogger(ProjDepTreeFactor.class);
    
    /** The sentence length. */
    private int n;
    
    // Counters.
    private static int unsafeLogSubtracts = 0;
    private static int logSubtractCount = 0;
    private static int extremeOddsRatios = 0;
    private static int oddsRatioCount = 0;
    
    public ProjDepTreeModule(Module<Tensor> mTrueIn, Module<Tensor> mFalseIn) {
        this(mTrueIn, mFalseIn, new LogPosNegAlgebra());
    }
    
    public ProjDepTreeModule(Module<Tensor> mTrueIn, Module<Tensor> mFalseIn, Algebra tmpS) {
        AbstractTensorModule.checkEqualAlgebras(mTrueIn, mFalseIn);
        this.mTrueIn = mTrueIn;
        this.mFalseIn = mFalseIn;
        this.outS = mTrueIn.getAlgebra();
        this.tmpS = tmpS;
    }
    
    @Override
    public Pair<Tensor, Tensor> forward() {
        // Construct the circuit.
        {
            // Initialize using the input tensors.
            AbstractTensorModule.checkEqualAlgebras(mTrueIn, mFalseIn);
            Tensor tmTrueIn = mTrueIn.getOutput();
            Tensor tmFalseIn = mFalseIn.getOutput();
            n = tmTrueIn.getDims()[1];
            requireNoZeros(tmFalseIn);
        }
        
        // Internally we use a different algebra to avoid numerical precision problems.
        ConvertAlgebra mTrueIn1 = new ConvertAlgebra(mTrueIn, tmpS);
        ConvertAlgebra mFalseIn1 = new ConvertAlgebra(mFalseIn, tmpS);
        
        Prod pi = new Prod(mFalseIn1);
        ElemDivide weights = new ElemDivide(mTrueIn1, mFalseIn1);
        
        InsideOutsideDepParse parse = new InsideOutsideDepParse(weights);
        Select alphas = new Select(parse, 0, InsideOutsideDepParse.ALPHA_IDX);
        Select betas = new Select(parse, 0, InsideOutsideDepParse.BETA_IDX);
        Select root = new Select(parse, 0, InsideOutsideDepParse.ROOT_IDX); // The first entry in this selection is for the root.
        
        ElemMultiply edgeSums = new ElemMultiply(alphas, betas);
        ScalarMultiply bTrue = new ScalarMultiply(edgeSums, pi, 0);
        
        ScalarMultiply partition = new ScalarMultiply(pi, root, 0);
        ScalarFill partitionMat = new ScalarFill(bTrue, partition, 0);
        ElemSubtract bFalse = new ElemSubtract(partitionMat, bTrue);
        
        ElemDivide mTrueOut0 = new ElemDivide(bTrue, mTrueIn1);
        ElemDivide mFalseOut0 = new ElemDivide(bFalse, mFalseIn1);

        // If the incoming message contained zeros, send back the same message.
        ElemMultiply inProd = new ElemMultiply(mTrueIn1, mFalseIn1);
        TakeLeftIfZero mTrueOut1 = new TakeLeftIfZero(mTrueIn1, mTrueOut0, inProd);
        TakeLeftIfZero mFalseOut1 = new TakeLeftIfZero(mFalseIn1, mFalseOut0, inProd);

        mTrueOut = new ConvertAlgebra(mTrueOut1, outS);
        mFalseOut = new ConvertAlgebra(mFalseOut1, outS);
        
        topoOrder = Lists.getList(mTrueIn1, mFalseIn1, pi, weights, parse, alphas, betas, root, edgeSums, bTrue,
                partition, partitionMat, bFalse, mTrueOut0, mFalseOut0, inProd, mTrueOut1, mFalseOut1, mTrueOut, mFalseOut);

        // Forward pass.
        for (Module<Tensor> module : topoOrder) {
            module.forward();
            if (module == partition) {
                // Correct if partition function is too small.
                checkAndFixPartition(bTrue, partition); // TODO: semiring
            } else if (module == weights && outS instanceof LogSemiring) {
                // Check odds ratios for potential floating point precision errors.
                checkLogOddsRatios(EdgeScores.tensorToEdgeScores(weights.getOutput()), tmpS);
            } else if (module == root && root.getOutput().getValue(0) == tmpS.zero()) {
                throw new IllegalStateException("Incoming messages disallowed all valid tree structures");
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
        AbstractTensorModule.checkEqualAlgebras(bTrue, module);
        Algebra s = bTrue.getAlgebra();
        // Correct for the case where the partition function is smaller
        // than some of the beliefs.
        double max = bTrue.getOutput().getMax();
        double partition = module.getOutput().getValue(0);
        if (!s.gte(partition, max)) {
            module.getOutput().setValue(0, max);
            unsafeLogSubtracts++;
        }
        logSubtractCount++;
    }
    
    private void checkLogOddsRatios(EdgeScores es, Algebra s) {       
        // Keep track of the minimum and maximum odds ratios, in order to detect
        // possible numerical precision issues.        
        double minOddsRatio = s.posInf();
        double maxOddsRatio = s.negInf();

        for (int p = -1; p < n; p++) {
            for (int c = 0; c < n; c++) {
                double oddsRatio = es.getScore(p, c);
                // Check min/max.
                if (s.lt(oddsRatio, minOddsRatio) && oddsRatio != s.zero()) {
                    // Don't count zeros when logging extreme odds ratios.
                    minOddsRatio = oddsRatio;
                }
                if (s.gt(oddsRatio, maxOddsRatio)) {
                    maxOddsRatio = oddsRatio;
                }
            }
        }

        // Check whether the max/min odds ratios (if added) would result in a
        // floating point error.
        oddsRatioCount++;
        if (s.minus(s.plus(maxOddsRatio, minOddsRatio), maxOddsRatio) == s.zero()) {
            extremeOddsRatios++;            
            log.debug(String.format("maxOddsRatio=%.20g minOddsRatio=%.20g", maxOddsRatio, minOddsRatio));
            log.debug(String.format("Proportion extreme odds ratios:  %f (%d / %d)", (double) extremeOddsRatios/ oddsRatioCount, extremeOddsRatios, oddsRatioCount));
            // We log the proportion of unsafe log-subtracts here only as a convenient way of highlighting the two floating point errors together.
            log.debug(String.format("Proportion unsafe log subtracts:  %f (%d / %d)", (double) unsafeLogSubtracts / logSubtractCount, unsafeLogSubtracts, logSubtractCount));
        }
    }

    private static void requireNoZeros(Tensor tmFalseIn) {
        Algebra s = tmFalseIn.getAlgebra();
        int[] dims = tmFalseIn.getDims();
        assert dims.length == 2;
        for (int i=0; i<dims[0]; i++) {
            for (int j=0; j<dims[1]; j++) {
                if ( tmFalseIn.get(i,j) == s.zero()) {
                    throw new IllegalStateException("Hard constraints turning ON an edge are not supported.");
                }
            }
        }
    }
    
    /**
     * Element-wise operation which takes the left side if the mark is zero, and the right side
     * otherwise.
     * 
     * NOTE: This module abuses the standard design since no information is back-propagated to the mark.
     * 
     * @author mgormley
     */
    private static class TakeLeftIfZero extends AbstractTensorModule implements Module<Tensor> {
        
        Module<Tensor> leftIn; 
        Module<Tensor> rightIn; 
        Module<Tensor> markIn;
        
        public TakeLeftIfZero(Module<Tensor> left, Module<Tensor> right, Module<Tensor> mark) {
            super(left.getAlgebra());
            checkEqualAlgebras(left, right, mark);
            this.leftIn = left;
            this.rightIn = right;
            this.markIn = mark;
        }

        @Override
        public Tensor forward() {
            Tensor mark = markIn.getOutput();
            Tensor left = leftIn.getOutput();
            Tensor right = rightIn.getOutput();
            Tensor.checkEqualSize(left, right);
            y = left.copyAndFill(s.zero());
            for (int c=0; c<y.size(); c++) {
                Tensor t = (mark.getValue(c) == s.zero()) ? left : right;
                y.setValue(c, t.getValue(c));
            }
            return y;
        }

        @Override
        public void backward() {
            Tensor mark = markIn.getOutput();
            Tensor leftAdj = leftIn.getOutputAdj();
            Tensor rightAdj = rightIn.getOutputAdj();
            for (int c=0; c<yAdj.size(); c++) {
                Tensor t = (mark.getValue(c) == s.zero()) ? leftAdj : rightAdj;
                t.addValue(c, yAdj.getValue(c));
            }
            // Get the mark's adjoint, but don't add to it. 
            // TODO: The mark should really be a constant tensor.
            markIn.getOutputAdj();
        }

        @Override
        public List<? extends Object> getInputs() {
            return Lists.getList(leftIn, rightIn);
        }
        
    }

    @Override
    public Algebra getAlgebra() {
        return outS;
    }

}
