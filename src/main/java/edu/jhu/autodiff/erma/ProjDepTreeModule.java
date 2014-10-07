package edu.jhu.autodiff.erma;

import java.util.List;

import org.apache.log4j.Logger;

import edu.jhu.autodiff.AbstractModule;
import edu.jhu.autodiff.Module;
import edu.jhu.autodiff.Tensor;
import edu.jhu.autodiff.tensor.Combine;
import edu.jhu.autodiff.tensor.ConvertAlgebra;
import edu.jhu.autodiff.tensor.ElemDivide;
import edu.jhu.autodiff.tensor.ElemMultiply;
import edu.jhu.autodiff.tensor.ElemSubtract;
import edu.jhu.autodiff.tensor.Prod;
import edu.jhu.autodiff.tensor.ScalarFill;
import edu.jhu.autodiff.tensor.ScalarMultiply;
import edu.jhu.autodiff.tensor.Select;
import edu.jhu.parse.dep.EdgeScores;
import edu.jhu.util.collections.Lists;
import edu.jhu.util.semiring.Algebra;
import edu.jhu.util.semiring.LogSemiring;
import edu.jhu.util.semiring.LogSignAlgebra;

/**
 * Takes incoming messages about a set of binary variables for a dependency tree and computes the
 * outgoing messages that are sent from a projective dependency parsing factor following (Smith &
 * Eisner, 2008).
 * 
 * @author mgormley
 */
public class ProjDepTreeModule implements Module<Tensor> {

    private Module<Tensor> mTrueIn;
    private Module<Tensor> mFalseIn;
    private List<Module<Tensor>> topoOrder;
    private Module<Tensor> comb;
    private Algebra outS;
    // For internal use only.
    private Algebra tmpS;

    private static final Logger log = Logger.getLogger(ProjDepTreeModule.class);
    
    /** The sentence length. */
    private int n;
    
    // Counters.
    private static int unsafeLogSubtracts = 0;
    private static int logSubtractCount = 0;
    private static int extremeOddsRatios = 0;
    private static int oddsRatioCount = 0;
    
    public ProjDepTreeModule(Module<Tensor> mTrueIn, Module<Tensor> mFalseIn) {
        this(mTrueIn, mFalseIn, new LogSignAlgebra());
    }
    
    public ProjDepTreeModule(Module<Tensor> mTrueIn, Module<Tensor> mFalseIn, Algebra tmpS) {
        AbstractModule.checkEqualAlgebras(mTrueIn, mFalseIn);
        this.mTrueIn = mTrueIn;
        this.mFalseIn = mFalseIn;
        this.outS = mTrueIn.getAlgebra();
        this.tmpS = tmpS;
    }
    
    @Override
    public Tensor forward() {
        // Construct the circuit.
        {
            // Initialize using the input tensors.
            AbstractModule.checkEqualAlgebras(mTrueIn, mFalseIn);
            Tensor tmTrueIn = mTrueIn.getOutput();
            Tensor tmFalseIn = mFalseIn.getOutput();
            n = tmTrueIn.getDims()[1];
            requireNoZeros(tmFalseIn);
        }
        
        // Internally we use a different algebra to avoid numerical precision problems.
        ConvertAlgebra<Tensor> mTrueIn1 = new ConvertAlgebra<Tensor>(mTrueIn, tmpS);
        ConvertAlgebra<Tensor> mFalseIn1 = new ConvertAlgebra<Tensor>(mFalseIn, tmpS);
        
        Prod pi = new Prod(mFalseIn1);
        ElemDivide weights = new ElemDivide(mTrueIn1, mFalseIn1);

        // Compute the dependency tree marginals, summing over all projective
        // spanning trees via the inside-outside algorithm.
        InsideOutsideDepParse parse = new InsideOutsideDepParse(weights);
        Select alphas = new Select(parse, 0, InsideOutsideDepParse.ALPHA_IDX);
        Select betas = new Select(parse, 0, InsideOutsideDepParse.BETA_IDX);
        Select root = new Select(parse, 0, InsideOutsideDepParse.ROOT_IDX); // The first entry in this selection is for the root.
        
        ElemMultiply edgeSums = new ElemMultiply(alphas, betas);
        ScalarMultiply bTrue = new ScalarMultiply(edgeSums, pi, 0);
        
        // partition = pi * \sum_{y \in Trees} \prod_{edge \in y} weight(edge) 
        ScalarMultiply partition = new ScalarMultiply(pi, root, 0);
        ScalarFill partitionMat = new ScalarFill(bTrue, partition, 0);
        
        // The beliefs are computed as follows.
        // beliefTrue = pi * FastMath.exp(chart.getLogSumOfPotentials(link.getParent(), link.getChild()));
        // beliefFalse = partition - beliefTrue;
        // 
        // Then the outgoing messages are computed as:
        // outMsgTrue = beliefTrue / inMsgTrue
        // outMsgFalse = beliefFalse / inMsgFalse
        ElemSubtract bFalse = new ElemSubtract(partitionMat, bTrue);
        
        ElemDivide mTrueOut0 = new ElemDivide(bTrue, mTrueIn1);
        ElemDivide mFalseOut0 = new ElemDivide(bFalse, mFalseIn1);

        // If the incoming message contained zeros, send back the same message.
        ElemMultiply inProd = new ElemMultiply(mTrueIn1, mFalseIn1);
        TakeLeftIfZero mTrueOut1 = new TakeLeftIfZero(mTrueIn1, mTrueOut0, inProd);
        TakeLeftIfZero mFalseOut1 = new TakeLeftIfZero(mFalseIn1, mFalseOut0, inProd);

        ConvertAlgebra<Tensor> mTrueOut = new ConvertAlgebra<Tensor>(mTrueOut1, outS);
        ConvertAlgebra<Tensor> mFalseOut = new ConvertAlgebra<Tensor>(mFalseOut1, outS);        
        comb = new Combine(mFalseOut, mTrueOut);
        
        topoOrder = Lists.getList(mTrueIn1, mFalseIn1, pi, weights, parse, alphas, betas, root, edgeSums, bTrue,
                partition, partitionMat, bFalse, mTrueOut0, mFalseOut0, inProd, mTrueOut1, mFalseOut1, mTrueOut, mFalseOut, comb);

        // Forward pass.
        for (Module<Tensor> module : topoOrder) {
            module.forward();
            if (module == partition) {
                // Correct if partition function is too small.
                checkAndFixPartition(bTrue, partition); // TODO: semiring
            } else if (module == weights && outS instanceof LogSemiring) {
                // Check odds ratios for potential floating point precision errors.
                checkLogOddsRatios(EdgeScores.tensorToEdgeScores(weights.getOutput()), tmpS);
                
                // TODO: When a possible floating point error is detected, we should try to fix that
                // outgoing message. This was implemented in the pre-ERMA version, but was removed because
                // the frequency of the errors was rather small.
                //
                // The solution: Divide out the skipped edge ahead of time. This is equivalent to
                // setting the odds ratio to 1.0. Then when sending back the message, send the
                // belief (instead of the belief / inMsg) for that edge.
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
    public Tensor getOutput() {
        return comb.getOutput();
    }

    @Override
    public Tensor getOutputAdj() {
        return comb.getOutputAdj();
    }

    @Override
    public void zeroOutputAdj() {
        for (Module<?> m : topoOrder) {
            m.zeroOutputAdj();
        }
    }

    @Override
    public List<Module<Tensor>> getInputs() {
        return Lists.getList(mTrueIn, mFalseIn);
    }

    private void checkAndFixPartition(Module<Tensor> bTrue, Module<Tensor> module) {
        AbstractModule.checkEqualAlgebras(bTrue, module);
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
        double maxOddsRatio = s.minValue();

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
    
    @Override
    public Algebra getAlgebra() {
        return outS;
    }

}
