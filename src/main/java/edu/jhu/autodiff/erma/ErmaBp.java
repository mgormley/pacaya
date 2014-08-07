package edu.jhu.autodiff.erma;


import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import org.apache.log4j.Logger;

import edu.jhu.autodiff.Module;
import edu.jhu.autodiff.Tensor;
import edu.jhu.autodiff.erma.ErmaObjective.BeliefsModuleFactory;
import edu.jhu.gm.inf.BeliefPropagation.BpScheduleType;
import edu.jhu.gm.inf.BeliefPropagation.BpUpdateOrder;
import edu.jhu.gm.inf.BfsMpSchedule;
import edu.jhu.gm.inf.BruteForceInferencer;
import edu.jhu.gm.inf.CachingBpSchedule;
import edu.jhu.gm.inf.FgInferencer;
import edu.jhu.gm.inf.FgInferencerFactory;
import edu.jhu.gm.inf.Messages;
import edu.jhu.gm.inf.MpSchedule;
import edu.jhu.gm.inf.ParallelMpSchedule;
import edu.jhu.gm.inf.RandomMpSchedule;
import edu.jhu.gm.model.Factor;
import edu.jhu.gm.model.FactorGraph;
import edu.jhu.gm.model.FactorGraph.FgEdge;
import edu.jhu.gm.model.FactorGraph.FgNode;
import edu.jhu.gm.model.FgModel;
import edu.jhu.gm.model.Var;
import edu.jhu.gm.model.VarSet;
import edu.jhu.gm.model.VarTensor;
import edu.jhu.gm.model.globalfac.GlobalFactor;
import edu.jhu.prim.list.DoubleArrayList;
import edu.jhu.util.collections.Lists;
import edu.jhu.util.semiring.Algebra;
import edu.jhu.util.semiring.Algebras;

/**
 * Loopy belief propagation inference algorithm with support for empirical risk
 * minimization under approximations (ERMA) (Stoyanov, Ropson, & Eisner, 2011)
 * 
 * @author mgormley
 */
public class ErmaBp extends AbstractFgInferencer implements Module<Beliefs>, FgInferencer {
    
    private static final Logger log = Logger.getLogger(ErmaBp.class);
    private static final FgEdge END_OF_EDGE_CREATION = null;
    
    public static class ErmaBpPrm implements FgInferencerFactory, BeliefsModuleFactory {
        
        public BpScheduleType schedule = BpScheduleType.TREE_LIKE;
        public int maxIterations = 100;
        public BpUpdateOrder updateOrder = BpUpdateOrder.PARALLEL;
        public boolean logDomain = true;
        public Algebra s = null;
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
        
        public ErmaBpPrm() {
        }
        
        @Override
        public FgInferencer getInferencer(FactorGraph fg) {
            return new ErmaBp(fg, this);
        }

        @Override
        public Module<Beliefs> getBeliefsModule(Module<Factors> effm, FactorGraph fg) {
            return new ErmaBp(fg, this, effm);
        }
        
        @Override
        public Algebra getAlgebra() {
            if (s == null) {
                return logDomain ? Algebras.LOG_SEMIRING : Algebras.REAL_ALGEBRA;
            } else {
                return s;
            }
        }
        
    }
    
    /**
     * The tape for recording the forward computation of belief propagation. Each entry on the tape
     * consists of several parts: an edge representing which message was sent, a normalized message,
     * and the normalizing constant of the pre-normalized message.
     * 
     * @author mgormley
     */
    private static class Tape {
        public List<VarTensor> msgs = new ArrayList<VarTensor>();
        public List<FgEdge> edges = new ArrayList<FgEdge>();
        public DoubleArrayList msgSums = new DoubleArrayList();
        public BitSet createFlags = new BitSet();
        public void add(FgEdge edge, VarTensor msg, double msgSum, boolean created) {
            int t = msgs.size();
            msgs.add(msg);
            edges.add(edge);
            msgSums.add(msgSum);
            createFlags.set(t, created);
        }
        public int size() {
            return edges.size();
        }
    }
        
    private static class TapeEntry {
        public Object item;
        public List<VarTensor> msgs;
        public DoubleArrayList msgSums;
        
        public TapeEntry(Object item, List<FgEdge> edges) {
            this.item = item;
            msgs = new ArrayList<VarTensor>(edges.size());
            msgSums = new DoubleArrayList(edges.size());
        }
        
        
    }
    
    private final ErmaBpPrm prm;
    private final Algebra s;
    private final FactorGraph fg;    
    private final CachingBpSchedule sched;
    // Messages for each edge in the factor graph. Indexed by edge id. 
    private Messages[] msgs;
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
    private Messages[] msgsAdj;
    // The adjoints for the potential tables (i.e. factors). Indexed by factor id. The output of a backward call.
    private VarTensor[] potentialsAdj;
    
    private Beliefs b;
    private Beliefs bAdj;
    private Module<Factors> effm;

    public ErmaBp(FactorGraph fg, ErmaBpPrm prm) {
        this(fg, prm, getFactorsModule(fg, prm));
    }

    private static ExpFamFactorsModule getFactorsModule(FactorGraph fg, ErmaBpPrm prm) {
        ExpFamFactorsModule effm = new ExpFamFactorsModule(new FgModelIdentity(new FgModel(0)), fg, prm.getAlgebra());
        effm.forward();
        return effm;
    }
    
    public ErmaBp(final FactorGraph fg, ErmaBpPrm prm, Module<Factors> effm) {
        if (prm.getAlgebra() != null && !prm.getAlgebra().equals(effm.getAlgebra())) {
            // TODO: We shouldn't even specify the algebra in prm.
            log.warn("Ignoring Algebra in ErmaBpPrm since the input module dictates the algebra: "
                    + prm.getAlgebra() + " " + effm.getAlgebra());
        }
        this.fg = fg;
        this.s = effm.getAlgebra();
        this.prm = prm;
        this.effm = effm;
        
        MpSchedule sch;
        if (prm.updateOrder == BpUpdateOrder.SEQUENTIAL) {
            if (prm.schedule == BpScheduleType.TREE_LIKE) {
                sch = new BfsMpSchedule(fg);
            } else if (prm.schedule == BpScheduleType.RANDOM) {
                sch = new RandomMpSchedule(fg);
            } else {
                throw new RuntimeException("Unknown schedule type: " + prm.schedule);
            }
        } else {
            sch = new ParallelMpSchedule(fg);
        }        
        sched = new CachingBpSchedule(sch, prm.updateOrder, prm.schedule);
    }

    private void initForward() {
        numConverged = 0;
        varBeliefs = null;
        facBeliefs = null;
        tape = new ArrayList<TapeEntry>();
        varBeliefsUnSum = null;
        facBeliefsUnSum = null;
        msgsAdj = null;
        potentialsAdj = null;
        // Initialize Messages.
        this.msgs = new Messages[fg.getNumEdges()];
        for (int i=0; i<msgs.length; i++) {
            // TODO: consider alternate initializations. For example, we could initialize to null.
            msgs[i] = new Messages(s, fg.getEdge(i), s.one());
        }
    }
    
    /**
     * For debugging. Remove later.
     */
    public Messages[] getMessages() {
    	return msgs;
    }
    
    /** For testing only. */
    public Messages[] getMessagesAdj() {
        return msgsAdj;
    }
    
    /** @inheritDoc */
    @Override
    public void run() {
        forward();
    }
    
    @Override
    public Beliefs forward() {
        // Initialization.
        initForward();
        
        // Message passing.
        loops:
        for (int iter=-1; iter < prm.maxIterations; iter++) {
            List<Object> order = sched.getOrder(iter);
            for (Object item : order) {
                List<FgEdge> edges = CachingBpSchedule.toEdgeList(fg, item);
                List<?> elems = CachingBpSchedule.toFactorEdgeList(item);
                TapeEntry te = new TapeEntry(item, edges);
                for (Object elem : elems) {
                    if (elem instanceof FgEdge) {
                        forwardCreateMessage((FgEdge) elem);
                    } else if (elem instanceof GlobalFactor) {
                        forwardGlobalFacToVar((GlobalFactor) elem);
                    } else {
                        throw new RuntimeException("Unsupported type in schedule: " + elem.getClass());
                    }
                }
                for (FgEdge edge : edges) {
                    normalizeAndAddToTape(edge, te);
                }
                for (FgEdge edge : edges) {
                    forwardSendMessage(edge);
                }
                if (prm.keepTape) { tape.add(te); }
                if (isConverged()) {
                    // Stop on convergence: Break out of inner and outer loop.
                    log.trace("Stopping on convergence. Iterations = " + (iter+1));
                    break loops;
                }
            }
        }
        
        forwardVarAndFacBeliefs();
        b = new Beliefs(varBeliefs, facBeliefs);
        return b;
    }

    private void forwardCreateMessage(FgEdge edge) {
        if (!edge.isVarToFactor() && (edge.getFactor() instanceof GlobalFactor)) {
            throw new IllegalArgumentException("Cannot create a single message from a global factor: " + edge);
        }
        if (edge.isVarToFactor()) {
            forwardVarToFactor(edge);
        } else {
            forwardFactorToVar(edge);
        }
    }

    public boolean isConverged() {
        return numConverged == msgs.length;
    }

    private void forwardGlobalFacToVar(GlobalFactor globalFac) {
        log.trace("Creating messages for global factor.");
        // Since this is a global factor, we pass the incoming messages to it, 
        // and efficiently marginalize over the variables.
        FgNode node = fg.getNode(globalFac);
        VarTensor[] inMsgs = getMsgs(node, msgs, CUR_MSG, IN_MSG);
        VarTensor[] outMsgs = getMsgs(node, msgs, NEW_MSG, OUT_MSG);
        globalFac.createMessages(inMsgs, outMsgs);
    }    

    // Constants for getMsgs().
    private static final boolean NEW_MSG = true;
    private static final boolean CUR_MSG = false;
    private static final boolean IN_MSG = true;
    private static final boolean OUT_MSG = false;
    
    /**
     * Gets messages from the Messages[].
     * 
     * @param parent The node for this factor.
     * @param msgs The input messages.
     * @param isNew Whether to get messages in .newMessage or .message.
     * @param isIn Whether to get incoming or outgoing messages.
     * @return The output messages.
     */
    private static VarTensor[] getMsgs(FgNode parent, Messages[] msgs, boolean isNew, boolean isIn) {
        List<FgEdge> edges = (isIn) ? parent.getInEdges() : parent.getOutEdges();
        VarTensor[] arr = new VarTensor[edges.size()];
        for (int i=0; i<edges.size(); i++) {
            FgEdge edge = edges.get(i);
            arr[i] = (isNew) ? msgs[edge.getId()].newMessage : msgs[edge.getId()].message;
        }
        return arr;
    }
    
    private void forwardVarToFactor(FgEdge edge) {
        // Since this is not a global factor, we send messages in the normal way, which
        // in the case of a factor to variable message requires enumerating all possible
        // variable configurations.
        VarTensor msg = msgs[edge.getId()].newMessage;
        
        // Initialize the message to all ones (zeros in log-domain) since we are "multiplying".
        msg.fill(s.one());
        
        // Message from variable v* to factor f*.
        //
        // Compute the product of all messages received by v* except for the
        // one from f*.
        getProductOfMessages(edge.getParent(), msg, edge.getChild());
    }

    private void forwardFactorToVar(FgEdge edge) {
        Var var = edge.getVar();
        Factor factor = edge.getFactor();
        // Since this is not a global factor, we send messages in the normal way, which
        // in the case of a factor to variable message requires enumerating all possible
        // variable configurations.
        VarTensor msg = msgs[edge.getId()].newMessage;
        
        // Message from factor f* to variable v*.
        //
        // Set the initial values of the product to those of the sending factor.
        VarTensor prod = safeNewVarTensor(factor);
        // Compute the product of all messages received by f* (each
        // of which will have a different domain) with the factor f* itself.
        // Exclude the message going out to the variable, v*.
        getProductOfMessages(edge.getParent(), prod, edge.getChild());
        
        // Marginalize over all the assignments to variables for f*, except
        // for v*.
        msg = prod.getMarginal(new VarSet(var), false);
        assert !msg.containsBadValues() : "msg = " + msg;
        
        // Set the final message in case we created a new object.
        msgs[edge.getId()].newMessage = msg;
    }

    private VarTensor safeNewVarTensor(Factor factor) {
        if (factor instanceof GlobalFactor) {
            // This case is only ever used in testing.
            log.warn("For testing only.");
            return BruteForceInferencer.safeNewVarTensor(s, factor);
        } else {
            return new VarTensor(effm.getOutput().f[factor.getId()]);
        }
    }

    private void normalizeAndAddToTape(FgEdge edge, TapeEntry te) {
        double msgSum = 0;
        if (prm.normalizeMessages) {
            msgSum = forwardNormalize(edge);
        }
        if (prm.keepTape) {
            // The tape stores the old message, the normalization constant of the new message, and the edge.
            te.msgs.add(new VarTensor(msgs[edge.getId()].message));
            te.msgSums.add(msgSum);
        }
    }
    
    private double forwardNormalize(FgEdge edge) {
        VarTensor msg = msgs[edge.getId()].newMessage;
        assert (msg.getVars().size() == 1) && (msg.getVars().get(0) == edge.getVar());
        double sum = msg.normalize();
        return sum;
    }

    private void forwardVarAndFacBeliefs() {
        // Cache the variable beliefs and their normalizing constants.
        varBeliefs = new VarTensor[fg.getNumVars()];
        varBeliefsUnSum = new double[fg.getNumVars()];
        for (int v=0; v<varBeliefs.length; v++) {
            VarTensor b = calcVarBeliefs(fg.getVar(v));
            varBeliefsUnSum[v] = b.normalize();
            varBeliefs[v] = b;
        }
        // Cache the factor beliefs and their normalizing constants.
        facBeliefs = new VarTensor[fg.getNumFactors()];
        facBeliefsUnSum = new double[fg.getNumFactors()];
        for (int a=0; a<facBeliefs.length; a++) {
            Factor fac = fg.getFactor(a);
            if (!(fac instanceof GlobalFactor)) {
                VarTensor b = calcFactorBeliefs(fg.getFactor(a));
                facBeliefsUnSum[a] = b.normalize();
                facBeliefs[a] = b;
            }
        }
    }
       
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
            List<FgEdge> edges = CachingBpSchedule.toEdgeList(fg, te.item);
            List<?> elems = CachingBpSchedule.toFactorEdgeList(te.item);
            
            for (int j = edges.size() - 1; j >= 0; j--) {
                backwardSendMessage(edges.get(j), te.msgs.get(j));
            }
            for (int j = edges.size() - 1; j >= 0; j--) {
                backwardNormalize(edges.get(j), te.msgSums.get(j));
            }
            for (int j = elems.size() - 1; j >= 0; j--) {
                Object elem = elems.get(j);
                if (elem instanceof FgEdge) {
                    backwardCreateMessage((FgEdge) elem);
                } else if (elem instanceof GlobalFactor) {
                    backwardGlobalFactorToVar((GlobalFactor) elem);
                } else {
                    throw new RuntimeException("Unsupported type in schedule: " + elem.getClass());
                }
            }
        }
    }

    private void backwardVarFacBeliefs(VarTensor[] varBeliefsAdj, VarTensor[] facBeliefsAdj) {
        // Compute the adjoints of the normalized messages.
        this.msgsAdj = new Messages[fg.getNumEdges()];
        for (int i=0; i<msgs.length; i++) {
            FgEdge edge = fg.getEdge(i);
            int varId = edge.getVar().getId();
            int facId = edge.getFactor().getId();
            // Instead of setting newMessage to null, we just zero it and then
            // swap these back and forth during backwardSendMessage.
            msgsAdj[i] = new Messages(s, edge, s.zero());
            if (!edge.isVarToFactor()) {
                // Backward pass for variable beliefs.
                initFactorToVarAdj(i, varBeliefsAdj, varId, facId);                
            } else if (!(fg.getFactor(facId) instanceof GlobalFactor)) {
                // Backward pass for factor beliefs. Part 1.
                initVarToFactorAdj(i, facBeliefsAdj, varId, facId, edge);
            }
            assert !msgsAdj[i].message.containsNaN() : "msgsAdj[i].message = " + msgsAdj[i].message + "\n" + "edge: " + edge;
        }
        // Initialize the adjoints of the potentials.
        this.potentialsAdj = effm.getOutputAdj().f;
        
        for (int a=0; a<fg.getNumFactors(); a++) {
            if (!(fg.getFactor(a) instanceof GlobalFactor)) {
                // Backward pass for factor beliefs. Part 2.
                initPotentialsAdj(a, facBeliefsAdj);
            }
        }
    }

    private void backwardSendMessage(FgEdge edge, VarTensor oldMsg) {
        // Dequeue from tape.
        int i = edge.getId();
        
        // Send messages and adjoints in reverse.
        msgs[i].newMessage = msgs[i].message;       // The message at time (t+1)
        msgs[i].message = oldMsg;                   // The message at time (t)
        // Swap the adjoint messages and zero the one for time (t).
        VarTensor tmp = msgsAdj[i].newMessage;
        tmp.multiply(0);
        msgsAdj[i].newMessage = msgsAdj[i].message; // The adjoint at time (t+1)
        msgsAdj[i].message = tmp;                   // The adjoint at time (t)
        
        logTraceMsgUpdate("backwardSendMessage", msgsAdj[i].newMessage, edge);
    }

    private void backwardNormalize(FgEdge edge, double msgSum) {
        if (prm.normalizeMessages) {
            int i = edge.getId();
            // Convert the adjoint of the message to the adjoint of the unnormalized message.
            unnormalizeAdjInPlace(msgs[i].newMessage, msgsAdj[i].newMessage, msgSum);
        }
    }
    
    private void backwardGlobalFactorToVar(GlobalFactor globalFac) {
        FgNode node = fg.getNode(globalFac);
        VarTensor[] inMsgs = getMsgs(node, msgs, CUR_MSG, IN_MSG);
        VarTensor[] inMsgsAdj = getMsgs(node, msgsAdj, CUR_MSG, IN_MSG);
        VarTensor[] outMsgsAdj = getMsgs(node, msgsAdj, NEW_MSG, OUT_MSG);
        globalFac.backwardCreateMessages(inMsgs, outMsgsAdj, inMsgsAdj);
    }
    
    /**
     * Creates the adjoint of the unnormalized message for the edge at time t
     * and stores it in msgsAdj[i].message.
     */
    private void backwardCreateMessage(FgEdge edge) {        
        if (!edge.isVarToFactor() && (edge.getFactor() instanceof GlobalFactor)) {
            throw new IllegalArgumentException("Cannot create a single message from a global factor: " + edge);
        }
        int i = edge.getId();
        if (edge.isVarToFactor()) {
            backwardVarToFactor(edge, i);
        } else {
            backwardFactorToVar(edge, i);
        }
        assert !msgsAdj[i].message.containsNaN() : "msgsAdj[i].message = " + msgsAdj[i].message + "\n" + "edge: " + edge;
    }

    private void logTraceMsgUpdate(String name, VarTensor msg, FgEdge edge) {
        if (log.isTraceEnabled()) {
            if (edge != null) {
                log.trace(name+" "+edge+"\n"+msg);
            } else {
                log.trace(name+"\n"+msg);
            }
        }
        assert !msg.containsNaN() : "msg = " + msg + "\n" + "edge: " + edge;
    }

    private void unnormalizeAdjInPlace(VarTensor dist, VarTensor distAdj, double unormSum) {
        if (unormSum == s.zero()) {
            throw new IllegalArgumentException("Unable to unnormalize when sum is 0.0\n"+dist+"\n"+distAdj+"\n"+unormSum);
        }
        VarTensor unormAdj = distAdj;
        double dotProd = dist.getDotProduct(distAdj);       
        unormAdj.subtract(dotProd);
        unormAdj.divide(unormSum);
        logTraceMsgUpdate("unnormalizeAdjInPlace", distAdj, null);
    }

    private void initVarToFactorAdj(int i, VarTensor[] facBeliefsAdj, int varId, int facId, FgEdge edge) {
        Factor fac = fg.getFactor(facId);
        VarTensor prod = safeNewVarTensor(fac);
        prod.prod(facBeliefsAdj[facId]);
        getProductOfMessages(fg.getFactorNode(facId), prod, fg.getVarNode(varId));
        msgsAdj[i].message = prod.getMarginal(new VarSet(edge.getVar()), false);
        logTraceMsgUpdate("initVarToFactorAdj", msgsAdj[i].message, edge);
    }

    private void initFactorToVarAdj(int i, VarTensor[] varBeliefsAdj, int varId, int facId) {
        msgsAdj[i].message = new VarTensor(varBeliefsAdj[varId]);
        getProductOfMessages(fg.getVarNode(varId), msgsAdj[i].message, fg.getFactorNode(facId));
        logTraceMsgUpdate("initFactorToVarAdj", msgsAdj[i].message, fg.getEdge(i));
    }

    private void initPotentialsAdj(int a, VarTensor[] facBeliefsAdj) {
        VarTensor tmp = new VarTensor(facBeliefsAdj[a]);
        getProductOfMessages(fg.getFactorNode(a), tmp, null);
        potentialsAdj[a].add(tmp);
        logTraceMsgUpdate("initPotentialsAdj", potentialsAdj[a], null);
        assert !potentialsAdj[a].containsNaN() : "potentialsAdj[a] = " + potentialsAdj[a];
    }

    private void backwardVarToFactor(FgEdge edgeIA, int i) {
        // Increment the adjoint for each factor to variable message.
        for (FgEdge edgeBI : fg.getVarNode(edgeIA.getVar().getId()).getInEdges()) {
            if (edgeBI != edgeIA.getOpposing()) {
                VarTensor prod = new VarTensor(msgsAdj[i].newMessage);
                // Get the product with all the incoming messages into the variable, excluding the factor from edge and edge2.
                getProductOfMessages(edgeIA.getParent(), prod, edgeIA.getChild(), edgeBI.getParent());
                msgsAdj[edgeBI.getId()].message.add(prod);
                // TODO: Above we could alternatively divide out the edgeBI contribution to a cached product.
                logTraceMsgUpdate("backwardVarToFactor", msgsAdj[edgeBI.getId()].message, edgeBI);
            }
        }
    }

    private void backwardFactorToVar(FgEdge edgeAI, int i) {
        Factor factor = edgeAI.getFactor();
        int facId = factor.getId();
        
        // Increment the adjoint for the potentials.
        {
            VarTensor prod = new VarTensor(msgsAdj[i].newMessage);
            getProductOfMessages(edgeAI.getParent(), prod, edgeAI.getChild());
            potentialsAdj[facId].add(prod);
            logTraceMsgUpdate("backwardFactorToVar", potentialsAdj[facId], null);
        }
        
        // Increment the adjoint for each variable to factor message.
        for (FgEdge edgeJA : fg.getFactorNode(facId).getInEdges()) {
            if (edgeJA != edgeAI.getOpposing()) {
                VarTensor prod = safeNewVarTensor(factor);
                getProductOfMessages(edgeAI.getParent(), prod, edgeAI.getChild(), edgeJA.getParent());
                prod.prod(msgsAdj[i].newMessage);
                VarSet varJ = msgsAdj[edgeJA.getId()].message.getVars();
                msgsAdj[edgeJA.getId()].message.add(prod.getMarginal(varJ, false));
                // TODO: Above we could alternatively divide out the edgeBI contribution to a cached product.
                logTraceMsgUpdate("backwardFactorToVar", msgsAdj[edgeJA.getId()].message, edgeJA);
            }
        }
    }
    
    public VarTensor[] getPotentialsAdj() {
        return potentialsAdj;
    }
    
    public VarTensor getPotentialsAdj(int factorId) {
        return potentialsAdj[factorId];
    }
    
    protected void getProductOfMessages(FgNode node, VarTensor prod, FgNode exclNode) {
        getProductOfMessages(node, prod, exclNode, null);
    }
    
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
    protected void getProductOfMessages(FgNode node, VarTensor prod, FgNode exclNode1, FgNode exclNode2) {
        for (FgEdge nbEdge : node.getInEdges()) {
            if (nbEdge.getParent() == exclNode1 || nbEdge.getParent() == exclNode2) {
                // Don't include the receiving variable.
                continue;
            }
            // Get message from neighbor to factor.
            VarTensor nbMsg = msgs[nbEdge.getId()].message;
            
            // If the node is a variable, this is an element-wise product. 
            // If the node is a factor, this an an outer product.
            prod.prod(nbMsg);
        }
    }

    /** Gets the product of messages (as in getProductOfMessages()) and then normalizes. */
    protected void getProductOfMessagesNormalized(FgNode node, VarTensor prod, FgNode exclNode) {
        getProductOfMessages(node, prod, exclNode);
        prod.normalize();
    }
    
    /**
     * Sends the message that is currently "pending" for this edge. This just
     * copies the message in the "pending slot" to the "message slot" for this
     * edge.
     * 
     * @param edge The edge over which the message should be sent.
     */
    protected void forwardSendMessage(FgEdge edge) {
        int edgeId = edge.getId();
       
        Messages ec = msgs[edgeId];
        // Update the residual
        double oldResidual = ec.residual;
        ec.residual = smartResidual(ec.message, ec.newMessage, edge);
        if (oldResidual > prm.convergenceThreshold && ec.residual <= prm.convergenceThreshold) {
            // This message has (newly) converged.
            numConverged ++;
        }
        if (oldResidual <= prm.convergenceThreshold && ec.residual > prm.convergenceThreshold) {
            // This message was marked as converged, but is no longer converged.
            numConverged--;
        }
        
        // Send message: Just swap the pointers to the current message and the new message, so
        // that we don't have to create a new factor object.
        VarTensor oldMessage = ec.message;
        ec.message = ec.newMessage;
        ec.newMessage = oldMessage;
        assert !ec.message.containsBadValues() : "ec.message = " + ec.message;
        
        if (log.isTraceEnabled()) {
            log.trace("Message sent: " + ec.message);
        }
    }

    /** Returns the "converged" residual for constant messages, and the actual residual otherwise. */
    private double smartResidual(VarTensor message, VarTensor newMessage, FgEdge edge) {
        // This is intentionally NOT the semiring zero.
        return CachingBpSchedule.isConstantMsg(edge) ? 0.0 : getResidual(message, newMessage);
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
    
    protected VarTensor getVarBeliefs(int varId) {
        return varBeliefs[varId];
    }
    
    protected VarTensor getFactorBeliefs(int facId) {
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
    protected VarTensor calcVarBeliefs(Var var) {
        VarTensor prod = new VarTensor(s, new VarSet(var), s.one());
        // Compute the product of all messages sent to this variable.
        FgNode node = fg.getVarNode(var.getId());
        getProductOfMessages(node, prod, null);
        return prod;
    }

    /** Gets the unnormalized factor beleifs. */
    protected VarTensor calcFactorBeliefs(Factor factor) {
        if (factor instanceof GlobalFactor) {
            log.warn("Getting marginals of a global factor is not supported."
                    + " This will require exponential space to store the resulting factor."
                    + " This should only be used for testing.");
        }
        
        VarTensor prod = safeNewVarTensor(factor);
        // Compute the product of all messages sent to this factor.
        FgNode node = fg.getFactorNode(factor.getId());
        getProductOfMessages(node, prod, null);
        return prod;
    }
    
    public double getPartitionBelief() {
        if (prm.schedule == BpScheduleType.TREE_LIKE && prm.normalizeMessages == false) {
            // Special case which only works on non-loopy graphs with the two pass schedule and 
            // no renormalization of messages.
            // 
            // The factor graph's overall partition function is the product of the
            // partition functions for each connected component. 
            double partition = s.one();
            for (FgNode node : fg.getConnectedComponents()) {
                if (!node.isVar()) {
                    if (node.getOutEdges().size() == 0) {
                        // This is an empty factor that makes no contribution to the partition function.
                        continue;
                    } else {
                        // Get a variable node in this connected component.
                        node = node.getOutEdges().get(0).getChild();
                        assert(node.isVar());
                    }
                }
                
                double nodePartition = getPartitionBeliefAtVarNode(node);
                partition = s.times(partition, nodePartition);
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
    protected double getBetheFreeEnergy() {
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
                VarTensor[] inMsgs = getMsgs(fg.getNode(f), msgs, CUR_MSG, IN_MSG);
                bethe += ((GlobalFactor) f).getExpectedLogBelief(inMsgs);
            }
        }
        for (int i=0; i<fg.getVars().size(); i++) {
            Var v = fg.getVars().get(i);
            int numNeighbors = fg.getVarNode(i).getOutEdges().size();
            VarTensor beliefs = getVarBeliefs(i);
            double sum = 0.0;
            for (int c=0; c<v.getNumStates(); c++) {
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

    /**
     * FOR TESTING ONLY.
     * Gets the partition function for the connected component containing the given node.
     */
    // TODO: This should be package private or protected. It is exposed for testing only.
    public double getPartitionBeliefAtVarNode(FgNode node) {
        // We just return the normalizing constant for the marginals of any variable.
        if (!node.isVar()) {
            throw new IllegalArgumentException("Node must be a variable node.");
        }
        Var var = node.getVar();
        VarTensor prod = new VarTensor(s, new VarSet(var), s.one());
        // Compute the product of all messages sent to this node.
        getProductOfMessages(node, prod, null);
        return prod.getSum();
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
        if (effm == null) {
            throw new RuntimeException("No inputs until setEffm() is called.");    
        }        
        return Lists.getList(effm);
    }

    public void setEffm(ExpFamFactorsModule effm) {
        if (!s.equals(effm.getAlgebra())) {
            throw new IllegalArgumentException("Algebras must be the same for ExpFamFactorModule and this class: " + s + " " + effm.getAlgebra());
        }
        this.effm = effm;
    }
    
    public FactorGraph getFactorGraph() {
        return fg;
    }
    
    public Algebra getAlgebra() {
        return s;
    }
    
}
