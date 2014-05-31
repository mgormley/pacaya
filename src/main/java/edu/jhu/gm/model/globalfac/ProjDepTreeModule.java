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
    private List<AbstractTensorModule> topoOrder;
    private ElemDivide mTrueOut;
    private ElemDivide mFalseOut;
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
        //        // Get the incoming messages at time (t).
        //        Tensor tmTrueIn = getMsgs(parent, msgs, LinkVar.TRUE, CUR_MSG, IN_MSG, s);        
        //        Tensor tmFalseIn = getMsgs(parent, msgs, LinkVar.FALSE, CUR_MSG, IN_MSG, s);
        //        
        //        // Construct the circuit.
        //        TensorIdentity mTrueIn = new TensorIdentity(tmTrueIn);
        //        TensorIdentity mFalseIn = new TensorIdentity(tmFalseIn);

        Tensor tmTrueIn = mTrueIn.getOutput();
        Tensor tmFalseIn = mFalseIn.getOutput();
        n = tmTrueIn.getDims()[1];
        
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
        
        mTrueOut = new ElemDivide(bTrue, mTrueIn);
        mFalseOut = new ElemDivide(bFalse, mFalseIn);

        topoOrder = Lists.getList(pi, weights, parse, alphas, betas, root, edgeSums, bTrue,
                partition, neg1, negBTrue, bFalse, mTrueOut, mFalseOut);

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
        
        // Correct if input messages have negative infinity.
        checkAndFixOutMsgs(tmTrueIn, tmFalseIn, tmTrueOut, tmFalseOut, s);
        
        return getOutput();
        
//        // Set the outgoing messages at time (t+1).
//        setMsgs(parent, msgs, tmTrueOut, LinkVar.TRUE, NEW_MSG, OUT_MSG, s);
//        setMsgs(parent, msgs, tmFalseOut, LinkVar.FALSE, NEW_MSG, OUT_MSG, s);
    }

    @Override
    public void backward() {        
//        // Set adjoints on outgoing message modules at time (t+1).
//        Tensor tTrue = getMsgs(parent, msgsAdj, LinkVar.TRUE, NEW_MSG, OUT_MSG, s);
//        mTrueOut.getOutputAdj().elemAdd(tTrue);
//        Tensor tFalse = getMsgs(parent, msgsAdj, LinkVar.FALSE, NEW_MSG, OUT_MSG, s);
//        mFalseOut.getOutputAdj().elemAdd(tFalse);
        
        // Backward pass.
        List<AbstractTensorModule> rev = Lists.reverse(topoOrder);
        for (Module<Tensor> module : rev) {
            module.backward();
        }
        
//        // Increment adjoints of the incoming messages at time (t).
//        // TODO: Here we just set the outgoing messages, this shouldn't be a
//        // problem since they will never be nonzero.
//        setMsgs(parent, msgsAdj, mTrueIn.getOutputAdj(), LinkVar.TRUE, CUR_MSG, IN_MSG, s);
//        setMsgs(parent, msgsAdj, mFalseIn.getOutputAdj(), LinkVar.TRUE, CUR_MSG, IN_MSG, s);        
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
    
    private void checkAndFixOutMsgs(Tensor tmTrueIn, Tensor tmFalseIn, Tensor tmTrueOut, Tensor tmFalseOut, Algebra s) {
        // TODO: Should this in some way impact the backward pass?
        for (int c=0; c<tmTrueOut.size(); c++) {
            double inMsgTrue = tmTrueIn.getValue(c);
            double inMsgFalse = tmFalseIn.getValue(c);
            if (inMsgTrue == s.zero() || inMsgFalse == s.zero()) {
                // If the incoming message contained zeros, send back the same message.
                tmTrueOut.setValue(c, inMsgTrue);
                tmFalseOut.setValue(c, inMsgFalse);
            }
        }
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

}
