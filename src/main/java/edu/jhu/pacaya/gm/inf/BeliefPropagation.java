package edu.jhu.pacaya.gm.inf;


import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.jhu.pacaya.autodiff.Identity;
import edu.jhu.pacaya.autodiff.MVecArray;
import edu.jhu.pacaya.autodiff.Module;
import edu.jhu.pacaya.autodiff.MutableModule;
import edu.jhu.pacaya.autodiff.Tensor;
import edu.jhu.pacaya.gm.model.Factor;
import edu.jhu.pacaya.gm.model.FactorGraph;
import edu.jhu.pacaya.gm.model.Factors;
import edu.jhu.pacaya.gm.model.ForwardOnlyFactorsModule;
import edu.jhu.pacaya.gm.model.Var;
import edu.jhu.pacaya.gm.model.VarSet;
import edu.jhu.pacaya.gm.model.VarTensor;
import edu.jhu.pacaya.gm.model.globalfac.AutodiffGlobalFactor;
import edu.jhu.pacaya.gm.model.globalfac.GlobalFactor;
import edu.jhu.pacaya.gm.util.BipartiteGraph;
import edu.jhu.pacaya.util.Prm;
import edu.jhu.pacaya.util.collections.QLists;
import edu.jhu.pacaya.util.files.QFiles;
import edu.jhu.pacaya.util.semiring.Algebra;
import edu.jhu.pacaya.util.semiring.LogSemiring;
import edu.jhu.prim.list.DoubleArrayList;
import edu.jhu.prim.list.IntArrayList;

/**
 * Loopy belief propagation inference algorithm with support for empirical risk
 * minimization under approximations (ERMA) (Stoyanov, Ropson, & Eisner, 2011)
 * 
 * @author mgormley
 */
public class BeliefPropagation extends AbstractFgInferencer implements Module<Beliefs>, FgInferencer {
    
    private static final Logger log = LoggerFactory.getLogger(BeliefPropagation.class);
    
    public static class BeliefPropagationPrm extends Prm implements FgInferencerFactory, BeliefsModuleFactory {
        private static final long serialVersionUID = 1L;        
        public BpScheduleType schedule = BpScheduleType.TREE_LIKE;
        public int maxIterations = 100;
        public BpUpdateOrder updateOrder = BpUpdateOrder.PARALLEL;
        public Algebra s = LogSemiring.getInstance();
        /** Whether to normalize the messages after sending. */
        public boolean normalizeMessages = true;
        /** The maximum message residual for convergence testing. */
        public double convergenceThreshold = 0;
        /** 
         * Whether to keep a tape of messages to allow for a backwards pass.
         * If this class is used only as a generic inference method, setting to 
         * false can save memory. 
         */
        public boolean keepTape = true;
        /** Directory for dumping of beliefs at each iteration (debugging only). */
        public Path dumpDir = null;
        /** Minimum number of neighbors for a variable to compute messages by dividing out from a cached belief. */
        public int minVarNbsForCache = Integer.MAX_VALUE; // TODO: Use this.
        /** Minimum number of neighbors for a factor   to compute messages by dividing out from a cached belief. */
        public int minFacNbsForCache = Integer.MAX_VALUE; // TODO: This might still be buggy.
        
        public BeliefPropagationPrm() {
        }
        
        @Override
        public FgInferencer getInferencer(FactorGraph fg) {
            return new BeliefPropagation(fg, this);
        }

        @Override
        public Module<Beliefs> getBeliefsModule(Module<Factors> fm, FactorGraph fg) {
            return new BeliefPropagation(fg, this, fm);
        }
        
        @Override
        public Algebra getAlgebra() {
            return s;
        }
        
    }
    
    public enum BpScheduleType {
        /** Send messages from a root to the leaves and back. */
        TREE_LIKE,
        /** Send messages in a random order. */
        RANDOM,
        /**
         * FOR TESTING ONLY: Schedule with only edges, so that no global factor dynamic programming
         * algorithms are ever called.
         */
        NO_GLOBAL_FACTORS,
    }
    
    public enum BpUpdateOrder {
        /** Send each message in sequence according to the schedule. */ 
        SEQUENTIAL,
        /** Create all messages first. Then send them all at the same time. */
        PARALLEL
    };

    /**
     * The tape entries for recording the forward computation of belief propagation. Each entry on
     * the tape consists of several parts: an item in the schedule representing which messages were
     * sent, the normalized messages, and the normalizing constants of the pre-normalized messages.
     * Optionally, we also include the modules for a global factor.
     * 
     * @author mgormley
     */
    private static class TapeEntry {
        public Object item;
        public List<VarTensor> msgs;
        public DoubleArrayList msgSums;
        public MutableModule<MVecArray<VarTensor>> modIn = null;
        public MutableModule<MVecArray<VarTensor>> modOut = null;
        
        public TapeEntry(Object item, List<Integer> edges) {
            this.item = item;
            this.msgs = new ArrayList<VarTensor>(edges.size());
            this.msgSums = new DoubleArrayList(edges.size());
        }
        
    }
    
    private final BeliefPropagationPrm prm;
    private final Algebra s;
    private final FactorGraph fg;   
    private final BipartiteGraph<Var, Factor> bg;
    private final CachingBpSchedule sched;
    // Messages for each edge in the factor graph. Indexed by edge id.
    private VarTensor[] msgs;
    private VarTensor[] newMsgs;
    // The message residuals. Indexed by edge id.
    private double[] residuals;
    // The number of messages that have converged.
    private int numConverged;
    // The variable and factor beliefs - the output of a forward() call.
    VarTensor[] varBeliefs; // Indexed by variable id.
    VarTensor[] facBeliefs; // Indexed by factor id.
    
    // The tape, which records each message passed in the forward() call.
    private List<TapeEntry> tape;
    // The tape for the normalization of the variable and factor beliefs.
    double[] varBeliefsUnSum; // Indexed by variable id.
    double[] facBeliefsUnSum; // Indexed by factor id.

    // Adjoints of the messages for each edge in the factor graph. Indexed by edge id. 
    private VarTensor[] msgsAdj;
    private VarTensor[] newMsgsAdj;
    // The adjoints for the potential tables (i.e. factors). Indexed by factor id. The output of a backward call.
    private VarTensor[] potentialsAdj;
    
    private Beliefs b;
    private Beliefs bAdj;
    private final Module<Factors> fm;

    private static AtomicInteger oscillationCount = new AtomicInteger(0);
    private static AtomicInteger sendCount = new AtomicInteger(0);
    
    public BeliefPropagation(FactorGraph fg, BeliefPropagationPrm prm) {
        this(fg, prm, getFactorsModule(fg, prm));
    }

    private static Module<Factors> getFactorsModule(FactorGraph fg, BeliefPropagationPrm prm) {
        ForwardOnlyFactorsModule fm = new ForwardOnlyFactorsModule(null, fg, prm.getAlgebra());
        fm.forward();
        return fm;
    }
    
    public BeliefPropagation(final FactorGraph fg, BeliefPropagationPrm prm, Module<Factors> fm) {
        if (prm.getAlgebra() != null && !prm.getAlgebra().equals(fm.getAlgebra())) {
            // TODO: We shouldn't even specify the algebra in prm.
            log.warn("Ignoring Algebra in ErmaBpPrm since the input module dictates the algebra: "
                    + prm.getAlgebra() + " " + fm.getAlgebra());
        }
        this.fg = fg;
        this.bg = fg.getBipgraph();
        this.s = fm.getAlgebra();
        this.prm = prm;
        this.fm = fm;
        
        MpSchedule sch;
        if (prm.updateOrder == BpUpdateOrder.SEQUENTIAL) {
            if (prm.schedule == BpScheduleType.TREE_LIKE) {
                sch = new BfsMpSchedule(fg);
            } else if (prm.schedule == BpScheduleType.RANDOM) {
                sch = new RandomMpSchedule(fg);
            } else if (prm.schedule == BpScheduleType.NO_GLOBAL_FACTORS) {
                sch = new NoGlobalFactorsMpSchedule(fg);
            } else {
                throw new RuntimeException("Unknown schedule type: " + prm.schedule);
            }
        } else {
            sch = new ParallelMpSchedule(fg);
        }        
        sched = new CachingBpSchedule(sch, prm.updateOrder, prm.schedule);
    }

    /** @inheritDoc */
    @Override
    public void run() {
        forward();
    }

    /* ---------------------------- BEGIN: Forward Pass Methods --------------------- */

    @Override
    public Beliefs forward() {
        // Initialization.
        initForward();
        
        // Message passing.
        loops:
        for (int iter=-1; iter < prm.maxIterations; iter++) {
            List<Object> order = sched.getOrder(iter, fg);
            for (Object item : order) {
                List<Integer> edges = CachingBpSchedule.toEdgeList(fg, item);
                List<?> elems = CachingBpSchedule.toFactorEdgeList(item);
                TapeEntry te = prm.keepTape ? new TapeEntry(item, edges) : null;
                for (Object elem : elems) {
                    if (elem instanceof Integer) {
                        forwardCreateMessage((Integer) elem);
                    } else if (elem instanceof AutodiffGlobalFactor) {
                        forwardGlobalFacToVar((AutodiffGlobalFactor) elem, te);
                    } else {
                        throw new RuntimeException("Unsupported type in schedule: " + elem.getClass());
                    }
                }
                for (Integer edge : edges) {
                    normalizeAndAddToTape(edge, te);
                }
                for (Integer edge : edges) {
                    forwardSendMessage(edge, iter);
                }
                if (prm.keepTape) { tape.add(te); }
                if (isConverged()) {
                    // Stop on convergence: Break out of inner and outer loop.
                    log.trace("Stopping on convergence. Iterations = {}", (iter+1));
                    break loops;
                }
            }
            maybeWriteAllBeliefs(iter);
        }
        
        log.trace("Oscillation rate: {}", ((double) oscillationCount.get() / sendCount.get()));
        
        forwardVarAndFacBeliefs();
        b = new Beliefs(varBeliefs, facBeliefs);
        return b;
    }

    public boolean isConverged() {
        return numConverged == msgs.length;
    }

    private void initForward() {
        // Set the number of converged messages to zero.
        numConverged = 0;
        // Initialize the tape.
        tape = new ArrayList<TapeEntry>();
        // Initialize Messages.
        this.msgs = new VarTensor[bg.getNumEdges()];  
        this.newMsgs = new VarTensor[bg.getNumEdges()];  
        for (int v=0; v<fg.getNumVars(); v++) {
            Var var = fg.getVar(v);
            VarSet vars = new VarSet(var);
            for (int nb=0; nb<bg.numNbsT1(v); nb++) {
                // Var to Factor edge.
                int e = bg.edgeT1(v, nb);
                msgs[e] = new VarTensor(s, vars, s.one());
                newMsgs[e] = new VarTensor(s, vars, s.one());
                // Factor to Var edge.
                e = bg.opposingT1(v, nb);
                msgs[e] = new VarTensor(s, vars, s.one());
                newMsgs[e] = new VarTensor(s, vars, s.one());                
            }
        }
        // Initialize residuals.
        this.residuals = new double[bg.getNumEdges()];
        Arrays.fill(residuals, Double.POSITIVE_INFINITY);
        // Cache the variable beliefs.
        varBeliefs = new VarTensor[fg.getNumVars()];
        if (prm.minVarNbsForCache < Integer.MAX_VALUE) {
            for (int v=0; v<varBeliefs.length; v++) {
                varBeliefs[v] = calcVarBeliefs(fg.getVar(v));
            }
        }
        // Cache the factor beliefs.
        facBeliefs = new VarTensor[fg.getNumFactors()];
        if (prm.minFacNbsForCache < Integer.MAX_VALUE) {
            for (int a=0; a<facBeliefs.length; a++) {
                Factor fac = fg.getFactor(a);
                if (!(fac instanceof GlobalFactor)) {
                    // Note that this is used to cache the product of messages. It is not a factor belief.
                    //facBeliefs[a] = new VarTensor(s, fac.getVars(), s.one());
                    //FgNode node = fg.getFactorNode(fac.getId());
                    //getProductOfMessages(node, facBeliefs[a], null, null);
                    facBeliefs[a] = calcFactorBeliefs(fac);
                }
            }
        }
        // Initialize the normalizing constants. These are used when computing the final beliefs.
        varBeliefsUnSum = new double[fg.getNumVars()];
        facBeliefsUnSum = new double[fg.getNumFactors()];
        // Clear the adjoints of the messages and potentials.
        msgsAdj = null;
        potentialsAdj = null;
    }

    private void forwardCreateMessage(int edge) {
        if (!bg.isT1T2(edge) && (bg.t2E(edge) instanceof GlobalFactor)) {
            log.warn("ONLY FOR TESTING: Creating a single message from a global factor: " + edge);
        }
        if (bg.isT1T2(edge)) {
            forwardVarToFactor(edge);
        } else {
            forwardFactorToVar(edge);
        }
    }

    private void forwardVarToFactor(int edge) {
        // Since this is not a global factor, we send messages in the normal way, which
        // in the case of a factor to variable message requires enumerating all possible
        // variable configurations.
        VarTensor msg = newMsgs[edge];
        
        // Initialize the message to all ones (zeros in log-domain) since we are "multiplying".
        msg.fill(s.one());
        
        // Message from variable v* to factor f*.
        //
        // Compute the product of all messages received by v* except for the
        // one from f*.
        getCavityProductAtVar(bg.parentE(edge), msg, bg.iterE(edge));
    }

    private void forwardFactorToVar(int edge) {
        Var var = bg.t1E(edge);
        Factor factor = bg.t2E(edge);
        // Since this is not a global factor, we send messages in the normal way, which
        // in the case of a factor to variable message requires enumerating all possible
        // variable configurations.
        
        // Message from factor f* to variable v*.
        //
        // Set the initial values of the product to those of the sending factor.
        VarTensor prod = safeNewVarTensor(factor);
        // Compute the product of all messages received by f* (each
        // of which will have a different domain) with the factor f* itself.
        // Exclude the message going out to the variable, v*.
        getCavityProductAtFactor(bg.parentE(edge), prod, bg.iterE(edge));
        
        // Marginalize over all the assignments to variables for f*, except
        // for v*.
        VarTensor msg = prod.getMarginal(new VarSet(var), false);
        assert !msg.containsBadValues() : "msg = " + msg;
        
        // Set the final message in case we created a new object.
        newMsgs[edge].setValuesOnly(msg);
    }

    private void forwardGlobalFacToVar(AutodiffGlobalFactor globalFac, TapeEntry te) {
        if (globalFac.getVars().size() == 0) { return; }
        log.trace("Creating messages for global factor.");
        // Since this is a global factor, we pass the incoming messages to it, 
        // and efficiently marginalize over the variables.
        int f = globalFac.getId();
        VarTensor[] inMsgs = getMsgs(f, msgs, IN_MSG);
        VarTensor[] outMsgs = getMsgs(f, newMsgs, OUT_MSG);
        Identity<MVecArray<VarTensor>> modIn = new Identity<MVecArray<VarTensor>>(new MVecArray<VarTensor>(inMsgs));
        Module<?> fmIn = fm.getOutput().getFactorModule(globalFac.getId());
        MutableModule<MVecArray<VarTensor>> modOut = globalFac.getCreateMessagesModule(modIn, fmIn);
        modOut.setOutput(new MVecArray<VarTensor>(outMsgs));
        modOut.forward();
        if (prm.keepTape) {
            assert te.modIn == null;
            assert te.modOut == null;
            te.modIn = modIn;
            te.modOut = modOut;
        }
    }

    private void normalizeAndAddToTape(int edge, TapeEntry te) {
        double msgSum = 0;
        if (prm.normalizeMessages) {
            msgSum = forwardNormalize(edge);
        }
        if (prm.keepTape) {
            // The tape stores the old message, the normalization constant of the new message, and the edge.
            te.msgs.add(new VarTensor(msgs[edge]));
            te.msgSums.add(msgSum);
        }
    }

    private double forwardNormalize(int edge) {
        VarTensor msg = newMsgs[edge];
        double sum = msg.normalize();
        return sum;
    }

    private void forwardVarAndFacBeliefs() {
        // Cache the variable beliefs and their normalizing constants.
        for (int v=0; v<varBeliefs.length; v++) {
            VarTensor b = calcVarBeliefs(fg.getVar(v));
            varBeliefsUnSum[v] = b.normalize();
            varBeliefs[v] = b;
        }
        // Cache the factor beliefs and their normalizing constants.
        for (int a=0; a<facBeliefs.length; a++) {
            Factor fac = fg.getFactor(a);
            if (!(fac instanceof GlobalFactor)) {
                VarTensor b = calcFactorBeliefs(fg.getFactor(a));
                facBeliefsUnSum[a] = b.normalize();
                facBeliefs[a] = b;
            }
        }
    }

    /**
     * Sends the message that is currently "pending" for this edge. This just
     * copies the message in the "pending slot" to the "message slot" for this
     * edge.
     * 
     * @param edge The edge over which the message should be sent.
     * @param iter The current iteration.
     */
    private void forwardSendMessage(int edge, int iter) {      
        // Update the residual
        double oldResidual = residuals[edge];
        residuals[edge] = smartResidual(msgs[edge], newMsgs[edge], edge);
        if (oldResidual > prm.convergenceThreshold && residuals[edge] <= prm.convergenceThreshold) {
            // This message has (newly) converged.
            numConverged ++;
        }
        if (oldResidual <= prm.convergenceThreshold && residuals[edge] > prm.convergenceThreshold) {
            // This message was marked as converged, but is no longer converged.
            numConverged--;
        }
        
        // Check for oscillation. Did the argmax change?
        if (log.isTraceEnabled() && iter > 0) {
            if (msgs[edge].getArgmaxConfigId() != newMsgs[edge].getArgmaxConfigId()) {    
                oscillationCount.incrementAndGet();
            }
            sendCount.incrementAndGet();
            log.trace("Residual: {} {}", fg.edgeToString(edge), residuals[edge]);
        }

        // Update the cached belief.
        int child = bg.childE(edge);
        if (!bg.isT1T2(edge) && bg.numNbsT1(child) >= prm.minVarNbsForCache) {
            varBeliefs[child].elemDivBP(msgs[edge]);
            varBeliefs[child].elemMultiply(newMsgs[edge]);            
        } else if (bg.isT1T2(edge) && bg.numNbsT2(child) >= prm.minFacNbsForCache){
            Factor f = bg.t2E(edge);
            if (! (f instanceof GlobalFactor)) {
                facBeliefs[child].divBP(msgs[edge]);
                facBeliefs[child].prod(newMsgs[edge]);
            }
        }
        
        // Send message: Just swap the pointers to the current message and the new message, so
        // that we don't have to create a new factor object.
        VarTensor oldMessage = msgs[edge];
        msgs[edge] = newMsgs[edge];
        newMsgs[edge] = oldMessage;
        assert !msgs[edge].containsBadValues() : "msgs[edge] = " + msgs[edge];
                
        if (log.isTraceEnabled()) {
            log.trace("Message sent: {} {}", fg.edgeToString(edge), msgs[edge]);
        }
    }

    /** Returns the "converged" residual for constant messages, and the actual residual otherwise. */
    private double smartResidual(VarTensor message, VarTensor newMessage, int edge) {
        // This is intentionally NOT the semiring zero.
        return CachingBpSchedule.isConstantMsg(edge, fg) ? 0.0 : getResidual(message, newMessage);
    }

    /**
     * Gets the residual for a new message, as the maximum error over all assignments.
     * 
     * Following the definition of Sutton & McCallum (2007), we compute the residual as the infinity
     * norm of the difference of the log of the message vectors.
     * 
     * Note: the returned value is NOT in the semiring / abstract algebra. It is the actual value
     * described above.
     */
    private double getResidual(VarTensor t1, VarTensor t2) {
        assert s == t1.getAlgebra() && s == t2.getAlgebra();
        Tensor.checkEqualSize(t1, t2);
        Tensor.checkSameAlgebra(t1, t2);
        double residual = Double.NEGATIVE_INFINITY;
        for (int c=0; c<t1.size(); c++) {
            double abs = Math.abs(s.toLogProb(t1.get(c)) - s.toLogProb(t2.get(c)));
            if (abs > residual) {
                residual = abs;
            }
        }
        return residual;
    }
    
    /* ---------------------------- END: Forward Pass Methods --------------------- */
    /* ---------------------------- BEGIN: Backward Pass Methods --------------------- */

    public void backward() {
        VarTensor[] varBeliefsAdj = bAdj.varBeliefs;
        VarTensor[] facBeliefsAdj = bAdj.facBeliefs;
        
        // Initialize the adjoints.
    
        // We are given the adjoints of the normalized beleifs. Compute
        // the adjoints of the unnormalized beliefs and store them in the original
        // adjoint arrays.
        for (int v=0; v<varBeliefsAdj.length; v++) {
            unnormalizeAdjInPlace(varBeliefs[v], varBeliefsAdj[v], varBeliefsUnSum[v]);
        }
        for (int a=0; a<facBeliefsAdj.length; a++) {
            if (facBeliefs[a] != null) {
                unnormalizeAdjInPlace(facBeliefs[a], facBeliefsAdj[a], facBeliefsUnSum[a]);
            }
        }
    
        // Initialize the message and potential adjoints by running the variable / factor belief computation in reverse.
        backwardVarFacBeliefs(varBeliefsAdj, facBeliefsAdj);
        
        // Process each tape entry in reverse order.
        for (int t = tape.size() - 1; t >= 0; t--) {
            // Dequeue from tape.
            TapeEntry te = tape.get(t);
            List<Integer> edges = CachingBpSchedule.toEdgeList(fg, te.item);
            List<?> elems = CachingBpSchedule.toFactorEdgeList(te.item);
            
            for (int j = edges.size() - 1; j >= 0; j--) {
                backwardSendMessage(edges.get(j), te.msgs.get(j));
            }
            for (int j = edges.size() - 1; j >= 0; j--) {
                backwardNormalize(edges.get(j), te.msgSums.get(j));
            }
            for (int j = elems.size() - 1; j >= 0; j--) {
                Object elem = elems.get(j);
                if (elem instanceof Integer) {
                    backwardCreateMessage((Integer) elem);
                } else if (elem instanceof AutodiffGlobalFactor) {
                    backwardGlobalFactorToVar((AutodiffGlobalFactor) elem, te);
                } else {
                    throw new RuntimeException("Unsupported type in schedule: " + elem.getClass());
                }
            }
        }
    }

    private void initVarToFactorAdj(int edge, VarTensor[] facBeliefsAdj) {
        int facId = bg.childE(edge);
        Factor fac = fg.getFactor(facId);
        VarTensor prod = safeNewVarTensor(fac);
        prod.prod(facBeliefsAdj[facId]);
        getCavityProductAtFactor(facId, prod, bg.dualE(edge));
        VarTensor msgAdj = prod.getMarginal(msgs[edge].getVars(), false);
        msgsAdj[edge].setValuesOnly(msgAdj);
        logTraceMsgUpdate("initVarToFactorAdj", msgsAdj[edge], edge);
    }

    private void initFactorToVarAdj(int edge, VarTensor[] varBeliefsAdj) {
        int varId = bg.childE(edge);
        msgsAdj[edge] = new VarTensor(varBeliefsAdj[varId]);
        getCavityProductAtVar(varId, msgsAdj[edge], bg.dualE(edge));
        logTraceMsgUpdate("initFactorToVarAdj", msgsAdj[edge], edge);
    }

    private void initPotentialsAdj(int a, VarTensor[] facBeliefsAdj) {
        VarTensor tmp = new VarTensor(facBeliefsAdj[a]);
        getProductAtFactor(a, tmp);
        potentialsAdj[a].add(tmp);
        logTraceMsgUpdate("initPotentialsAdj", potentialsAdj[a], -1);
        assert !potentialsAdj[a].containsNaN() : "potentialsAdj[a] = " + potentialsAdj[a];
    }

    /**
     * Creates the adjoint of the unnormalized message for the edge at time t
     * and stores it in msgsAdj[i].
     */
    private void backwardCreateMessage(int edge) {    
        if (!bg.isT1T2(edge) && (bg.t2E(edge) instanceof GlobalFactor)) {
            log.warn("ONLY FOR TESTING: Creating a single message from a global factor: " + edge);
        }
        if (bg.isT1T2(edge)) {
            backwardVarToFactor(edge);
        } else {
            backwardFactorToVar(edge);
        }
        assert !msgsAdj[edge].containsNaN() : "msgsAdj[i] = " + msgsAdj[edge] + "\n" + "edge: " + fg.edgeToString(edge);
    }

    private void backwardVarToFactor(int edgeIA) {
        int i = bg.parentE(edgeIA);
        int aNb = bg.iterE(edgeIA);
        for (int bNb = 0; bNb < bg.numNbsT1(i); bNb++) {
            if (bNb != aNb) {
                VarTensor prod = new VarTensor(newMsgsAdj[edgeIA]);
                // Get the product with all the incoming messages into the variable, excluding the factor from a and b.
                getCavityProductAtVar(i, prod, aNb, bNb);
                int edgeBI = bg.opposingT1(i, bNb);
                msgsAdj[edgeBI].add(prod);
                // TODO: Above we could alternatively divide out the edgeBI contribution to a cached product.
                logTraceMsgUpdate("backwardVarToFactor", msgsAdj[edgeBI], edgeBI);
            }
        }
    }

    private void backwardFactorToVar(int edgeAI) {        
        Factor factor = bg.t2E(edgeAI);
        int a = bg.parentE(edgeAI);
        int iNb = bg.iterE(edgeAI);
        
        // Increment the adjoint for the potentials.
        {
            if (potentialsAdj[a] != null) {
                VarTensor prod = new VarTensor(newMsgsAdj[edgeAI]);
                getCavityProductAtFactor(a, prod, iNb);
                // Skip this step when testing global factors.
                potentialsAdj[a].add(prod);
                logTraceMsgUpdate("backwardFactorToVar", potentialsAdj[a], -1);
            }
        }
        
        // Increment the adjoint for each variable to factor message.
        for (int jNb = 0; jNb < bg.numNbsT2(a); jNb++) {
            if (jNb != iNb) {
                VarTensor prod = safeNewVarTensor(factor);
                getCavityProductAtFactor(a, prod, iNb, jNb);
                prod.prod(newMsgsAdj[edgeAI]);
                int edgeJA = bg.opposingT2(a, jNb);
                VarSet varJ = msgsAdj[edgeJA].getVars();
                msgsAdj[edgeJA].add(prod.getMarginal(varJ, false));
                // TODO: Above we could alternatively divide out the edgeBI contribution to a cached product.
                logTraceMsgUpdate("backwardFactorToVar", msgsAdj[edgeJA], edgeJA);
            }
        }
    }

    private void backwardGlobalFactorToVar(AutodiffGlobalFactor globalFac, TapeEntry te) {
        if (globalFac.getVars().size() == 0) { return; }
        int f = globalFac.getId();
        VarTensor[] inMsgs = getMsgs(f, msgs, IN_MSG);
        VarTensor[] inMsgsAdj = getMsgs(f, msgsAdj, IN_MSG);
        VarTensor[] outMsgsAdj = getMsgs(f, newMsgsAdj, OUT_MSG);
        te.modIn.setOutput(new MVecArray<VarTensor>(inMsgs));
        te.modIn.setOutputAdj(new MVecArray<VarTensor>(inMsgsAdj));
        te.modOut.setOutputAdj(new MVecArray<VarTensor>(outMsgsAdj));
        te.modOut.backward();
        // Replaced: globalFac.backwardCreateMessages(inMsgs, outMsgsAdj, inMsgsAdj);
    }

    private void backwardNormalize(int edge, double msgSum) {
        if (prm.normalizeMessages) {
            // Convert the adjoint of the message to the adjoint of the unnormalized message.
            unnormalizeAdjInPlace(newMsgs[edge], newMsgsAdj[edge], msgSum);
        }
    }

    private void backwardVarFacBeliefs(VarTensor[] varBeliefsAdj, VarTensor[] facBeliefsAdj) {
        // Compute the adjoints of the normalized messages.
        this.msgsAdj = new VarTensor[fg.getNumEdges()];
        this.newMsgsAdj = new VarTensor[fg.getNumEdges()];
        for (int v=0; v<fg.getNumVars(); v++) {
            Var var = fg.getVar(v);
            VarSet vars = new VarSet(var);
            for (int nb=0; nb<bg.numNbsT1(v); nb++) {
                // Instead of setting newMessage to null, we just zero it and then
                // swap these back and forth during backwardSendMessage.
                //
                // Var to Factor edge.
                int edge = bg.edgeT1(v, nb);
                msgsAdj[edge] = new VarTensor(s, vars, s.zero());
                newMsgsAdj[edge] = new VarTensor(s, vars, s.zero());
                if (!(bg.t2E(edge) instanceof GlobalFactor)) {
                    // Backward pass for factor beliefs. Part 1.
                    initVarToFactorAdj(edge, facBeliefsAdj);
                }
                // Factor to Var edge.
                edge = bg.opposingT1(v, nb);
                msgsAdj[edge] = new VarTensor(s, vars, s.zero());
                newMsgsAdj[edge] = new VarTensor(s, vars, s.zero());    
                // Backward pass for variable beliefs.
                initFactorToVarAdj(edge, varBeliefsAdj);  
                assert !msgsAdj[edge].containsNaN() : "msgsAdj[i] = " + msgsAdj[edge] + "\n" + "edge: " + edge;
            }
        }
        // Initialize the adjoints of the potentials.
        this.potentialsAdj = fm.getOutputAdj().f;
        
        for (int a=0; a<fg.getNumFactors(); a++) {
            if (!(fg.getFactor(a) instanceof GlobalFactor)) {
                // Backward pass for factor beliefs. Part 2.
                initPotentialsAdj(a, facBeliefsAdj);
            }
        }
    }

    private void backwardSendMessage(int edge, VarTensor oldMsg) {
        // Update the cached belief.
        int child = bg.childE(edge);
        if (!bg.isT1T2(edge) && bg.numNbsT1(child) >= prm.minVarNbsForCache) {
            varBeliefs[child].elemDivBP(msgs[edge]);
            varBeliefs[child].elemMultiply(oldMsg);
        } else if (bg.isT1T2(edge) && bg.numNbsT2(child) >= prm.minFacNbsForCache){
            Factor f = bg.t2E(edge);
            if (! (f instanceof GlobalFactor)) {
                facBeliefs[child].divBP(msgs[edge]);
                facBeliefs[child].prod(oldMsg);
            }
        }
        
        // Send messages and adjoints in reverse.
        newMsgs[edge] = msgs[edge];       // The message at time (t+1)
        msgs[edge] = oldMsg;                   // The message at time (t)
        // Swap the adjoint messages and zero the one for time (t).
        VarTensor tmp = newMsgsAdj[edge];
        tmp.multiply(s.zero());
        newMsgsAdj[edge] = msgsAdj[edge]; // The adjoint at time (t+1)
        msgsAdj[edge] = tmp;                   // The adjoint at time (t)
        
        logTraceMsgUpdate("backwardSendMessage", newMsgsAdj[edge], edge);
    }

    private void unnormalizeAdjInPlace(VarTensor dist, VarTensor distAdj, double unormSum) {
        if (unormSum == s.zero()) {
            throw new IllegalArgumentException("Unable to unnormalize when sum is 0.0\n"+dist+"\n"+distAdj+"\n"+unormSum);
        }
        VarTensor unormAdj = distAdj;
        double dotProd = dist.getDotProduct(distAdj);       
        unormAdj.subtract(dotProd);
        unormAdj.divide(unormSum);
        dist.multiply(unormSum);
        logTraceMsgUpdate("unnormalizeAdjInPlace", distAdj, -1);
    }
    
    /* ---------------------------- END: Backward Pass Methods --------------------- */

    // Constants for getMsgs().
    private static final boolean NEW_MSG = true;
    private static final boolean CUR_MSG = false;
    private static final boolean IN_MSG = true;
    private static final boolean OUT_MSG = false;
    
    /**
     * Gets messages from the Messages[].
     * 
     * @param f The factor's index.
     * @param msgs The input messages.
     * @param isNew Whether to get messages in .newMessage or .message.
     * @param isIn Whether to get incoming or outgoing messages.
     * @return The output messages.
     */
    private VarTensor[] getMsgs(int f, VarTensor[] msgs, boolean isIn) {
        int numNbs = bg.numNbsT2(f);
        VarTensor[] arr = new VarTensor[numNbs];
        for (int nb=0; nb<numNbs; nb++) {
            int edge = isIn ? bg.opposingT2(f, nb) : bg.edgeT2(f, nb);
            arr[nb] = msgs[edge];
        }
        return arr;
    }
    
    private VarTensor safeNewVarTensor(Factor factor) {
        if (factor instanceof GlobalFactor) {
            // This case is only ever used in testing.
            log.warn("For testing only.");
            return BruteForceInferencer.safeNewVarTensor(s, factor);
        } else {
            return new VarTensor(fm.getOutput().f[factor.getId()]);
        }
    }

    private void logTraceMsgUpdate(String name, VarTensor msg, int edge) {
        if (log.isTraceEnabled()) {
            if (edge != -1) {
                log.trace(name+" "+fg.edgeToString(edge)+"\n"+msg);
            } else {
                log.trace(name+"\n"+msg);
            }
        }
        assert !msg.containsNaN() : "msg = " + msg + "\n" + "edge: " + fg.edgeToString(edge);
    }

    private void getProductAtFactor(int f, VarTensor prod) {
        getCavityProductAtFactor(f, prod, -1, -1);
    }
    
    private void getCavityProductAtFactor(int f, VarTensor prod, int excl) {
        getCavityProductAtFactor(f, prod, excl, -1);
    }
    
    private void getCavityProductAtFactor(int f, VarTensor prod, int excl1, int excl2) {
        if (bg.numNbsT2(f) >= prm.minFacNbsForCache) {
            // Compute message by dividing out from cached belief.
            prod.prod(facBeliefs[f]);
            // TODO: Change the usage of this method so that it returns the product of the messages
            // AND the factor.
            prod.divBP(fm.getOutput().f[f]);
            if (excl1 != -1) {
                int e_v1_f = bg.opposingT2(f, excl1);
                prod.divBP(msgs[e_v1_f]);
            }
            if (excl2 != -1) {
                int e_v2_f = bg.opposingT2(f, excl2);
                prod.divBP(msgs[e_v2_f]);
            }
        } else {
            // Standard message computation.
            calcProductAtFactor(f, prod, excl1, excl2);
        }
    }

    private void calcProductAtFactor(int f, VarTensor prod, int excl1, int excl2) {
        for (int nb=0; nb<bg.numNbsT2(f); nb++) {
            if (nb == excl1 || nb == excl2) {
                // Don't include messages to these neighbors.
                continue;
            }
            // Get message from neighbor to this node.
            VarTensor nbMsg = msgs[bg.opposingT2(f, nb)];
            // Since the node is a variable, this is an element-wise product. 
            prod.prod(nbMsg);
        }
    }
    
    private void getCavityProductAtVar(int v, VarTensor prod, int exclNode) {
        getCavityProductAtVar(v, prod, exclNode, -1);
    }
    
    private void getCavityProductAtVar(int v, VarTensor prod, int excl1, int excl2) {
        int numNbs = bg.numNbsT1(v);
        if (numNbs >= prm.minVarNbsForCache) {
            // Compute message by dividing out from cached belief.
            prod.elemMultiply(varBeliefs[v]);
            if (excl1 != -1) {
                int e_f1_v = bg.opposingT1(v, excl1);
                prod.elemDivBP(msgs[e_f1_v]);
            }
            if (excl2 != -1) {
                int e_f2_v = bg.opposingT1(v, excl2);
                prod.elemDivBP(msgs[e_f2_v]);
            }
        } else {
            // Standard message computation.
            calcProductAtVar(v, prod, excl1, excl2);
        }
    }

    // TODO: Fix up this comment
    /**
     * Computes the product of all messages being sent to a node, optionally excluding messages sent
     * from another node or two.
     * 
     * Upon completion, prod will be multiplied by the product of all incoming messages to node,
     * except for the message from exclNode1 / exclNode2 if specified.
     * 
     * @param node The node to which all the messages are being sent.
     * @param prod An input / output tensor with which the product will (destructively) be taken.
     * @param exclNode1 If non-null, any message sent from exclNode1 to node will be excluded from
     *            the product.
     * @param exclNode2 If non-null, any message sent from exclNode2 to node will be excluded from
     *            the product.
     */
    private void calcProductAtVar(int v, VarTensor prod, int excl1, int excl2) {
        for (int nb=0; nb<bg.numNbsT1(v); nb++) {
            if (nb == excl1 || nb == excl2) {
                // Don't include messages to these neighbors.
                continue;
            }
            // Get message from neighbor to this node.
            VarTensor nbMsg = msgs[bg.opposingT1(v, nb)];
            // Since the node is a variable, this is an element-wise product. 
            prod.elemMultiply(nbMsg);
        }
    }
        
    VarTensor getVarBeliefs(int varId) {
        return varBeliefs[varId];
    }
    
    VarTensor getFactorBeliefs(int facId) {
        if (facBeliefs[facId] == null) {
            // Beliefs for global factors are not cached.
            Factor factor = fg.getFactor(facId);
            assert factor instanceof GlobalFactor;
            VarTensor b = calcFactorBeliefs(factor);
            b.normalize();
            return b;
        }
        return facBeliefs[facId];
    }

    protected VarTensor getVarBeliefs(Var var) {
        return getVarBeliefs(var.getId());
    }

    protected VarTensor getFactorBeliefs(Factor factor) {
        return getFactorBeliefs(factor.getId());
    }
    
    /** Gets the unnormalized variable beleifs. */
    private VarTensor calcVarBeliefs(Var var) {
        // Compute the product of all messages sent to this variable.
        VarTensor prod = new VarTensor(s, new VarSet(var), s.one());
        calcProductAtVar(var.getId(), prod, -1, -1);
        return prod;
    }

    /** Gets the unnormalized factor beleifs. */
    private VarTensor calcFactorBeliefs(Factor factor) {
        if (factor instanceof GlobalFactor) {
            log.warn("Getting marginals of a global factor is not supported."
                    + " This will require exponential space to store the resulting factor."
                    + " This should only be used for testing.");
        }        
        // Compute the product of all messages sent to this factor.
        VarTensor prod = safeNewVarTensor(factor);
        calcProductAtFactor(factor.getId(), prod, -1, -1);
        return prod;
    }
    
    public double getPartitionBelief() {
        // TODO: This method almost always overflows when s is the REAL semiring.

        if (prm.updateOrder == BpUpdateOrder.SEQUENTIAL && prm.schedule == BpScheduleType.TREE_LIKE
                && prm.normalizeMessages == false && bg.isAcyclic()) {
            // Special case which only works on non-loopy graphs with the two pass schedule and 
            // no renormalization of messages.
            // 
            // The factor graph's overall partition function is the product of the
            // partition functions for each connected component. 
            double partition = s.one();
            IntArrayList ccs = bg.getConnectedComponentsT2();
            for (int i=0; i<ccs.size(); i++) {
                int t2 = ccs.get(i);
                if (bg.numNbsT2(t2) > 0) {
                    // Get a variable node in this connected component.
                    int v = bg.childT2(t2, 0);
                    double nodePartition = varBeliefsUnSum[v];
                    partition = s.times(partition, nodePartition);
                }
            }
            assert !s.isNaN(partition);
            return partition;
        }
        
        return s.fromLogProb(- getBetheFreeEnergy());
    }
    
    /**
     * Computes the Bethe free energy of the factor graph. For acyclic graphs,
     * this is equal to -log(Z) where Z is the exact partition function. For 
     * loopy graphs it can be used as an approximation.
     * 
     * NOTE: The result of this call is always in the real semiring.
     */
    double getBetheFreeEnergy() {
        // 
        // G_{Bethe} = \sum_a \sum_{x_a} - b(x_a) ln \chi(x_a)
        //              + \sum_a \sum_{x_a} b(x_a) ln b(x_a)
        //              + \sum_i (n_i - 1) \sum_{x_i} b(x_i) ln b(x_i)
        //           = \sum_a \sum_{x_a} b(x_a) ln (b(x_a) / \chi(x_a))
        //              + \sum_i (n_i - 1) \sum_{x_i} b(x_i) ln b(x_i)
        //
        //     where n_i is the number of neighbors of the variable x_i,
        //     b(x_a) and b(x_i) are normalized distributions and x_a is 
        //     the set of variables participating in factor a. 
        //
                
        double bethe = 0.0;
        for (int a=0; a<fg.getFactors().size(); a++) {
            Factor f = fg.getFactors().get(a);
            if (!(f instanceof GlobalFactor)) {
                int numConfigs = f.getVars().calcNumConfigs();
                VarTensor beliefs = getFactorBeliefs(a);
                for (int c=0; c<numConfigs; c++) {                
                    // Since we want multiplication by 0 to always give 0 (not the case for Double.POSITIVE_INFINITY or Double.NaN.
                    double b_c = beliefs.getValue(c);
                    if (b_c != s.zero()) {
                        double r_b_c = s.toReal(b_c);
                        double log_b_c = s.toLogProb(b_c);
                        double log_chi_c = f.getLogUnormalizedScore(c);
                        bethe += r_b_c * (log_b_c - log_chi_c);
                    }
                }
            } else {
                VarTensor[] inMsgs = getMsgs(f.getId(), msgs, IN_MSG);
                bethe += ((GlobalFactor) f).getExpectedLogBelief(inMsgs);
            }
        }
        for (int v=0; v<fg.getVars().size(); v++) {
            Var var = fg.getVars().get(v);
            int numNeighbors = bg.numNbsT1(v);
            VarTensor beliefs = getVarBeliefs(v);
            double sum = 0.0;
            for (int c=0; c<var.getNumStates(); c++) {
                double b_c = beliefs.getValue(c);
                if (b_c != s.zero()) {
                    double r_b_c = s.toReal(b_c);
                    double log_b_c = s.toLogProb(b_c);
                    sum += r_b_c * log_b_c;
                }
            }
            bethe -= (numNeighbors - 1) * sum;
        }
        
        assert !Double.isNaN(bethe);        
        return bethe;
    }

    private void maybeWriteAllBeliefs(int iter) {
        if (prm.dumpDir != null) {
            forwardVarAndFacBeliefs();
            try {
                BufferedWriter writer = QFiles.createTempFileBufferedWriter("bpdump", prm.dumpDir.toFile());
                writer.write("Iteration: " + iter + "\n");
                writer.write("Messages:\n");
                for (int e=0; e<bg.getNumEdges(); e++) {
                    writer.write(fg.edgeToString(e) + "\n");
                    writer.write("message: ");
                    writer.write(AbstractFgInferencer.ensureRealSemiring(msgs[e]) + "\n");
                    writer.write("newMessage: ");
                    writer.write(AbstractFgInferencer.ensureRealSemiring(newMsgs[e]) + "\n");
                }
                writer.write("Var marginals:\n");
                for (Var v : fg.getVars()) {
                    writer.write(calcVarBeliefs(v) + "\n");
                }
                writer.write("Factor marginals:\n");
                for (Factor f : fg.getFactors()) {
                    if (! (f instanceof GlobalFactor)) {
                        writer.write(calcFactorBeliefs(f) + "\n");
                    }
                }
                writer.write("Partition: " + this.getPartition());
                writer.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
    
    public VarTensor[] getPotentialsAdj() {
        return potentialsAdj;
    }

    public VarTensor getPotentialsAdj(int factorId) {
        return potentialsAdj[factorId];
    }

    @Override
    public Beliefs getOutput() {
        return b;
    }

    @Override
    public Beliefs getOutputAdj() {
        if (bAdj == null) {            
            bAdj = b.copyAndFill(s.zero());
        }
        return bAdj;
    }

    @Override
    public void zeroOutputAdj() {
        if (bAdj != null) { bAdj.fill(s.zero()); }
    }

    @Override
    public List<Module<Factors>> getInputs() {
        return QLists.getList(fm);
    }
    
    public FactorGraph getFactorGraph() {
        return fg;
    }
    
    public Algebra getAlgebra() {
        return s;
    }
    
    /** For testing only. */
    public VarTensor[] getMessages() {
        return msgs;
    }
    
    /** For testing only. */
    public VarTensor[] getNewMessages() {
        return newMsgs;
    }
    
    /** For testing only. */
    public VarTensor[] getMessagesAdj() {
        return msgsAdj;
    }
    
    /** For testing only. */
    public VarTensor[] getNewMessagesAdj() {
        return newMsgsAdj;
    }
    
}
