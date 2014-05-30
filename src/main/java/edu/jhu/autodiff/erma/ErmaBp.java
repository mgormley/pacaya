package edu.jhu.autodiff.erma;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import edu.jhu.autodiff.Module;
import edu.jhu.gm.inf.BeliefPropagation.BpScheduleType;
import edu.jhu.gm.inf.BeliefPropagation.BpUpdateOrder;
import edu.jhu.gm.inf.BeliefPropagation.FgInferencerFactory;
import edu.jhu.gm.inf.BeliefPropagation.Messages;
import edu.jhu.gm.inf.BfsBpSchedule;
import edu.jhu.gm.inf.BpSchedule;
import edu.jhu.gm.inf.BruteForceInferencer;
import edu.jhu.gm.inf.FgInferencer;
import edu.jhu.gm.inf.RandomBpSchedule;
import edu.jhu.gm.model.Factor;
import edu.jhu.gm.model.FactorGraph;
import edu.jhu.gm.model.FactorGraph.FgEdge;
import edu.jhu.gm.model.FactorGraph.FgNode;
import edu.jhu.gm.model.Var;
import edu.jhu.gm.model.VarSet;
import edu.jhu.gm.model.VarTensor;
import edu.jhu.gm.model.globalfac.GlobalFactor;
import edu.jhu.prim.list.DoubleArrayList;
import edu.jhu.prim.util.math.FastMath;
import edu.jhu.util.collections.Lists;
import edu.jhu.util.semiring.Algebra;
import edu.jhu.util.semiring.RealAlgebra;

/**
 * Loopy belief propagation inference algorithm with support for empirical risk
 * minimization under approximations (ERMA) (Stoyanov, Ropson, & Eisner, 2011)
 * 
 * @author mgormley
 */
public class ErmaBp implements Module<Beliefs>, FgInferencer {
    
    private static final Logger log = Logger.getLogger(ErmaBp.class);
    private static final FgEdge END_OF_EDGE_CREATION = null;
    
    public static class ErmaBpPrm implements FgInferencerFactory {
        public BpScheduleType schedule = BpScheduleType.TREE_LIKE;
        public int maxIterations = 100;
        public BpUpdateOrder updateOrder = BpUpdateOrder.PARALLEL;
        //public final FactorGraph fg;
        public boolean logDomain = true;
        /** Whether to normalize the messages after sending. */
        public boolean normalizeMessages = true;
        /** The maximum message residual for convergence testing. */
        public double convergenceThreshold = 0;
        
        public ErmaBpPrm() {
        }
        public FgInferencer getInferencer(FactorGraph fg) {
            return new ErmaBp(fg, this);
        }
        public boolean isLogDomain() {
            return logDomain;
        }
    }
    
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
        
    private final ErmaBpPrm prm;
    private final FactorGraph fg;
    private BpSchedule sched;
    // Messages for each edge in the factor graph. Indexed by edge id. 
    private Messages[] msgs;
    // The number of messages that have converged.
    private int numConverged = 0;
    // The variable and factor beliefs - the output of a forward() call.
    VarTensor[] varBeliefs; // Indexed by variable id.
    VarTensor[] facBeliefs; // Indexed by factor id.
    
    // The tape, which records each message passed in the forward() call.
    private Tape tape;
    // The tape for the normalization of the variable and factor beliefs.
    double[] varBeliefsUnSum; // Indexed by variable id.
    double[] facBeliefsUnSum; // Indexed by factor id.

    // Adjoints of the messages for each edge in the factor graph. Indexed by edge id. 
    private Messages[] msgsAdj;
    // The adjoints for the potential tables (i.e. factors). Indexed by factor id. The output of a backward call.
    private VarTensor[] potentialsAdj;
    
    Algebra s = new RealAlgebra();     // TODO:
    private Beliefs b;
    private Beliefs bAdj;

    public ErmaBp(FactorGraph fg, ErmaBpPrm prm) {
        this.prm = prm;
        this.fg = fg;
        
        if (prm.updateOrder == BpUpdateOrder.SEQUENTIAL) {
            if (prm.schedule == BpScheduleType.TREE_LIKE) {
                sched = new BfsBpSchedule(fg);
            } else if (prm.schedule == BpScheduleType.RANDOM) {
                sched = new RandomBpSchedule(fg);
            } else {
                throw new RuntimeException("Unknown schedule type: " + prm.schedule);
            }
        }
    }
    
    /**
     * For debugging. Remove later.
     */
    public Messages[] getMessages() {
    	return msgs;
    }
    
    /** @inheritDoc */
    @Override
    public void run() {
        forward();
    }
    
    @Override
    public Beliefs forward() {
        // Initialization.
        tape = new Tape();
        this.msgs = new Messages[fg.getNumEdges()];        
        for (int i=0; i<msgs.length; i++) {
            // TODO: consider alternate initializations.
            msgs[i] = new Messages(fg.getEdge(i), prm.logDomain, prm.normalizeMessages);
        }
        // Reset the global factors.
        for (Factor factor : fg.getFactors()) {
            if (factor instanceof GlobalFactor) {
                ((GlobalFactor)factor).reset();
            }
        }
        
        // Message passing.
        List<FgEdge> order = (prm.updateOrder == BpUpdateOrder.SEQUENTIAL) ?
            order = sched.getOrder() : null;
        for (int iter=0; iter < prm.maxIterations; iter++) {
            if (prm.updateOrder == BpUpdateOrder.SEQUENTIAL) {
                if (prm.schedule == BpScheduleType.RANDOM) {
                    order = sched.getOrder();
                }
                for (FgEdge edge : order) {
                    forwardCreateMessage(edge, iter);
                    forwardSendMessage(edge);
                    if (isConverged()) {
                        // Stop on convergence: Break out of inner loop.
                        break;
                    }
                }
            } else if (prm.updateOrder == BpUpdateOrder.PARALLEL) {
                for (FgEdge edge : fg.getEdges()) {
                    forwardCreateMessage(edge, iter);
                }
                // Mark the end of the message creation on the tape with a special tape entry.
                tape.add(END_OF_EDGE_CREATION, null, 0, false);
                for (FgEdge edge : fg.getEdges()) {
                    forwardSendMessage(edge);
                }
            } else {
                throw new RuntimeException("Unsupported update order: " + prm.updateOrder);
            }
            if (isConverged()) {
                // Stop on convergence.
                log.trace("Stopping on convergence. Iterations = " + (iter+1));
                break;
            }
        }
        
        forwardVarAndFacBeliefs();
        b = new Beliefs(varBeliefs, facBeliefs);
        return b;
    }

    private void forwardCreateMessage(FgEdge edge, int iter) {
        if (!edge.isVarToFactor() && (edge.getFactor() instanceof GlobalFactor)) {
            boolean created = forwardGlobalFactorToVar(edge, iter);
            if (created) {
                // Add all the outgoing messages from the global factor to the tape.
                normalizeAndAddToTape(edge, true); // only mark the first edge as "created"
                for (FgEdge e2 : edge.getParent().getOutEdges()) {
                    if (e2 != edge) {
                        // Include each created edge so that we can reverse normalization.
                        normalizeAndAddToTape(e2, false);
                    }
                }
            }
        } else {
            if (edge.isVarToFactor()) {
                forwardVarToFactor(edge);
            } else {
                forwardFactorToVar(edge);
            }
            normalizeAndAddToTape(edge, true);
        }
    }

    private void normalizeAndAddToTape(FgEdge edge, boolean created) {
        double msgSum = 0;
        if (prm.normalizeMessages) {
            msgSum = forwardNormalize(edge);
        }
        // The tape stores the old message, the normalization constant of the new message, and the edge.
        VarTensor oldMsg = new VarTensor(msgs[edge.getId()].message);
        tape.add(edge, oldMsg, msgSum, created);
    }

    public boolean isConverged() {
        return numConverged == msgs.length;
    }
    
    private void forwardVarToFactor(FgEdge edge) {
        // Since this is not a global factor, we send messages in the normal way, which
        // in the case of a factor to variable message requires enumerating all possible
        // variable configurations.
        VarTensor msg = msgs[edge.getId()].newMessage;
        
        // Initialize the message to all ones (zeros in log-domain) since we are "multiplying".
        msg.fill(prm.logDomain ? 0.0 : 1.0);
        
        // Message from variable v* to factor f*.
        //
        // Compute the product of all messages received by v* except for the
        // one from f*.
        getProductOfMessages(edge.getParent(), msg, edge.getChild());

        // Set the final message in case we created a new object.
        msgs[edge.getId()].newMessage = msg;
    }

    private void forwardFactorToVar(FgEdge edge) {
        Var var = edge.getVar();
        Factor factor = edge.getFactor();
        // Since this is not a global factor, we send messages in the normal way, which
        // in the case of a factor to variable message requires enumerating all possible
        // variable configurations.
        VarTensor msg = msgs[edge.getId()].newMessage;
        
        // Initialize the message to all ones (zeros in log-domain) since we are "multiplying".
        msg.fill(prm.logDomain ? 0.0 : 1.0);
        
        // Message from factor f* to variable v*.
        //
        // Compute the product of all messages received by f* (each
        // of which will have a different domain) with the factor f* itself.
        // Exclude the message going out to the variable, v*.
        VarTensor prod = new VarTensor(factor.getVars());
        // Set the initial values of the product to those of the sending factor.
        int numConfigs = prod.getVars().calcNumConfigs();
        for (int c = 0; c < numConfigs; c++) {
            prod.setValue(c, factor.getUnormalizedScore(c));
        }
        getProductOfMessages(edge.getParent(), prod, edge.getChild());
        
        // Marginalize over all the assignments to variables for f*, except
        // for v*.
        if (prm.logDomain) { 
            msg = prod.getLogMarginal(new VarSet(var), false);
        } else {
            msg = prod.getMarginal(new VarSet(var), false);
        }

        // Set the final message in case we created a new object.
        msgs[edge.getId()].newMessage = msg;
    }

    private boolean forwardGlobalFactorToVar(FgEdge edge, int iter) {
        log.trace("Creating messages for global factor.");
        // Since this is a global factor, we pass the incoming messages to it, 
        // and efficiently marginalize over the variables. The current setup is
        // create all the messages from this factor to its variables, but only 
        // once per iteration.
        GlobalFactor globalFac = (GlobalFactor) edge.getFactor();
        return globalFac.createMessages(edge.getParent(), msgs, prm.logDomain, false, iter);
    }

    private double forwardNormalize(FgEdge edge) {
        VarTensor msg = msgs[edge.getId()].newMessage;
        assert (msg.getVars().equals(new VarSet(edge.getVar())));
        double sum = Double.NaN;
        if (prm.normalizeMessages) {
            if (prm.logDomain) { 
                sum = msg.logNormalize();
            } else {
                sum = msg.normalize();
            }
        } else {
            // normalize and logNormalize already check for NaN
            assert !msg.containsBadValues(prm.logDomain) : "msg = " + msg;        
        }
        msgs[edge.getId()].newMessage = msg;
        return sum;
    }

    private void forwardVarAndFacBeliefs() {
        // Cache the variable beliefs and their normalizing constants.
        varBeliefs = new VarTensor[fg.getNumVars()];
        varBeliefsUnSum = new double[fg.getNumVars()];
        for (int v=0; v<varBeliefs.length; v++) {
            VarTensor b = calcVarBeliefs(fg.getVar(v));
            varBeliefsUnSum[v] = (prm.logDomain) ? b.logNormalize() : b.normalize();
            varBeliefs[v] = b;
        }
        // Cache the factor beliefs and their normalizing constants.
        facBeliefs = new VarTensor[fg.getNumFactors()];
        facBeliefsUnSum = new double[fg.getNumFactors()];
        for (int a=0; a<facBeliefs.length; a++) {
            Factor fac = fg.getFactor(a);
            if (!(fac instanceof GlobalFactor)) {
                VarTensor b = calcFactorBeliefs(fg.getFactor(a));
                facBeliefsUnSum[a] = (prm.logDomain) ? b.logNormalize() : b.normalize();
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

        // Compute the adjoints of the normalized messages.
        this.msgsAdj = new Messages[fg.getNumEdges()];
        for (int i=0; i<msgs.length; i++) {
            FgEdge edge = fg.getEdge(i);
            int varId = edge.getVar().getId();
            int facId = edge.getFactor().getId();
            msgsAdj[i] = new Messages(edge, prm.logDomain, prm.normalizeMessages);
            msgsAdj[i].message.multiply(s.zero());
            // Instead of setting newMessage to null, we just zero it and then
            // swap these back and forth during backwardSendMessage.
            msgsAdj[i].newMessage.multiply(s.zero());
            if (!edge.isVarToFactor()) {
                // Backward pass for variable beliefs.
                initFactorToVarAdj(i, varBeliefsAdj, varId, facId);                
            } else if (!(fg.getFactor(facId) instanceof GlobalFactor)) {
                // Backward pass for factor beliefs. Part 1.
                initVarToFactorAdj(i, facBeliefsAdj, varId, facId, edge);
            }
        }
        this.potentialsAdj = new VarTensor[fg.getNumFactors()];
        for (int a=0; a<fg.getNumFactors(); a++) {
            if (!(fg.getFactor(a) instanceof GlobalFactor)) {
                // Backward pass for factor beliefs. Part 2.
                initPotentialsAdj(a, facBeliefsAdj);
            }
        }
        
        // Reset the global factors.
        for (Factor factor : fg.getFactors()) {
            if (factor instanceof GlobalFactor) {
                ((GlobalFactor)factor).reset();
            }
        }
        
        // Compute the message adjoints by running BP in reverse.
        if (prm.updateOrder == BpUpdateOrder.SEQUENTIAL) {
            // Process each tape entry in reverse order.
            for (int t = tape.size() - 1; t >= 0; t--) {
                backwardSendMessage(t);
                backwardNormalize(t);
                backwardCreateMessage(t);            
            }
        } else if (prm.updateOrder == BpUpdateOrder.PARALLEL) {
            int t = tape.size() - 1;
            while (t >= 0) {
                // Send the messages backwards from each tape entry until an
                // END_OF_EDGE_CREATION marker is reached.
                int tTop = t;
                for (; t >= 0; t--) {                
                    if (tape.edges.get(t) == END_OF_EDGE_CREATION) {
                        break;
                    }
                    backwardSendMessage(t);
                    backwardNormalize(t);
                }
                // Create the adjoints of the messages that were just
                // "sent backwards" above.
                t = tTop;
                for (; t >= 0; t--) {
                    if (tape.edges.get(t) == END_OF_EDGE_CREATION) {
                        t--;
                        break;
                    }
                    backwardCreateMessage(t);
                }
            }
        } else {
            throw new RuntimeException("Unsupported update order: " + prm.updateOrder);
        }
    }

    private void backwardSendMessage(int t) {
        // Dequeue from tape.
        FgEdge edge = tape.edges.get(t);
        VarTensor oldMsg = tape.msgs.get(t);
        int i = edge.getId();
        
        // Send messages and adjoints in reverse.
        msgs[i].newMessage = msgs[i].message;       // The message at time (t+1)
        msgs[i].message = oldMsg;                   // The message at time (t)
        // Swap the adjoint messages and zero the one for time (t).
        VarTensor tmp = msgsAdj[i].newMessage;
        tmp.multiply(0);
        msgsAdj[i].newMessage = msgsAdj[i].message; // The adjoint at time (t+1)
        msgsAdj[i].message = tmp;                   // The adjoint at time (t)
    }

    private void backwardNormalize(int t) {
        if (prm.normalizeMessages) {
            // Dequeue from tape.
            FgEdge edge = tape.edges.get(t);
            double msgSum = tape.msgSums.get(t);
            int i = edge.getId();
            // Convert the adjoint of the message to the adjoint of the unnormalized message.
            unnormalizeAdjInPlace(msgs[i].newMessage, msgsAdj[i].newMessage, msgSum);
        }
    }
    
    /**
     * Creates the adjoint of the unnormalized message for the edge at time t
     * and stores it in msgsAdj[i].message.
     */
    private void backwardCreateMessage(int t) {
        // Dequeue from tape.
        FgEdge edge = tape.edges.get(t);
        boolean created = tape.createFlags.get(t);            
        int i = edge.getId();
        
        if (!edge.isVarToFactor() && (edge.getFactor() instanceof GlobalFactor)) {
            // TODO: The schedule should be over edge sets, not individual edges, so we don't need the "created" flag.
            GlobalFactor factor = (GlobalFactor) edge.getFactor();
            if (created) {
                factor.backwardCreateMessages(edge.getParent(), msgs, msgsAdj, s);
            }
        } else {            
            if (edge.isVarToFactor()) {
                backwardVarToFactor(edge, i);
            } else {
                backwardFactorToVar(edge, i);
            }
        }        
    }

    private void unnormalizeAdjInPlace(VarTensor dist, VarTensor distAdj, double unormSum) {
        // TODO: use a semiring
        VarTensor unormAdj = distAdj;
        double dotProd = dist.dotProduct(distAdj);       
        unormAdj.add(- dotProd);
        unormAdj.multiply(1.0 / unormSum);
    }

    private void initVarToFactorAdj(int i, VarTensor[] facBeliefsAdj, int varId, int facId, FgEdge edge) {
        Factor fac = fg.getFactor(facId);
        VarTensor prod = new VarTensor(facBeliefsAdj[facId]);
        prod.prod(BruteForceInferencer.safeGetDenseFactor(fac));
        getProductOfMessages(fg.getFactorNode(facId), prod, fg.getVarNode(varId));
        if (prm.logDomain) { 
            msgsAdj[i].message = prod.getLogMarginal(new VarSet(edge.getVar()), false);
        } else {
            msgsAdj[i].message = prod.getMarginal(new VarSet(edge.getVar()), false);
        }
    }

    private void initFactorToVarAdj(int i, VarTensor[] varBeliefsAdj, int varId, int facId) {
        msgsAdj[i].message = new VarTensor(varBeliefsAdj[varId]);
        getProductOfMessages(fg.getVarNode(varId), msgsAdj[i].message, fg.getFactorNode(facId));
    }

    private void initPotentialsAdj(int a, VarTensor[] facBeliefsAdj) {
        potentialsAdj[a] = new VarTensor(facBeliefsAdj[a]);
        getProductOfMessages(fg.getFactorNode(a), potentialsAdj[a], null);
    }

    private void backwardVarToFactor(FgEdge edgeIA, int i) {
        // Increment the adjoint for each factor to variable message.
        for (FgEdge edgeBI : fg.getVarNode(edgeIA.getVar().getId()).getInEdges()) {
            if (edgeBI != edgeIA.getOpposing()) {
                VarTensor prod = new VarTensor(msgsAdj[i].newMessage);
                // Get the product with all the incoming messages into the variable, excluding the factor from edge and edge2.
                getProductOfMessages(edgeIA.getParent(), prod, edgeIA.getChild(), edgeBI.getParent());
                msgsAdj[edgeBI.getId()].message.add(prod); // TODO: semiring
                // TODO: Above we could alternatively divide out the edgeBI contribution to a cached product.
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
            potentialsAdj[facId].add(prod); // TODO: semiring
        }
        
        // Increment the adjoint for each variable to factor message.
        for (FgEdge edgeJA : fg.getFactorNode(facId).getInEdges()) {
            if (edgeJA != edgeAI.getOpposing()) {
                VarTensor prod = new VarTensor(BruteForceInferencer.safeGetDenseFactor(factor));
                getProductOfMessages(edgeAI.getParent(), prod, edgeAI.getChild(), edgeJA.getParent());
                prod.prod(msgsAdj[i].newMessage); // TODO: semiring
                VarSet varJ = msgsAdj[edgeJA.getId()].message.getVars();
                msgsAdj[edgeJA.getId()].message.add(prod.getMarginal(varJ, false)); // TODO: semiring
                // TODO: Above we could alternatively divide out the edgeBI contribution to a cached product.
            }
        }
    }
    
    public VarTensor[] getPotentialsAdj() {
        return potentialsAdj;
    }
    
    public VarTensor getPotentialsAdj(int factorId) {
        return potentialsAdj[factorId];
    }
    
    public void clear() {
        Arrays.fill(msgs, null);
        tape = null;
    }
    
    protected void getProductOfMessages(FgNode node, VarTensor prod, FgNode exclNode) {
        getProductOfMessages(node, prod, exclNode, null);
    }
    
    /**
     * Computes the product of all messages being sent to a node, optionally
     * excluding messages sent from another node.
     * 
     * Upon completion, prod will contain the product of all incoming messages
     * to node, except for the message from exclNode if specified, times the
     * factor given in prod.
     * 
     * @param node
     *            The node to which all the messages are being sent.
     * @param prod
     *            An input factor with which the product will (destructively) be
     *            taken.
     * @param exclNode
     *            If null, the product of all messages will be taken. If
     *            non-null, any message sent from exclNode to node will be
     *            excluded from the product.
     */
    protected void getProductOfMessages(FgNode node, VarTensor prod, FgNode exclNode1, FgNode exclNode2) {
        for (FgEdge nbEdge : node.getInEdges()) {
            if (nbEdge.getParent() == exclNode1 || nbEdge.getParent() == exclNode2) {
                // Don't include the receiving variable.
                continue;
            }
            // Get message from neighbor to factor.
            VarTensor nbMsg = msgs[nbEdge.getId()].message;
            
            // The neighbor messages have a different domain, but addition
            // should still be well defined.
            if (prm.logDomain) {
                prod.add(nbMsg);
            } else {
                prod.prod(nbMsg);
            }
        }
    }

    /** Gets the product of messages (as in getProductOfMessages()) and then normalizes. */
    protected void getProductOfMessagesNormalized(FgNode node, VarTensor prod, FgNode exclNode) {
        getProductOfMessages(node, prod, exclNode);
        if (prm.logDomain) { 
            prod.logNormalize();
        } else {
            prod.normalize();
        }
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
        ec.residual = getResidual(ec.message, ec.newMessage);
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
        assert !ec.message.containsBadValues(prm.logDomain) : "ec.message = " + ec.message;
        
        if (log.isTraceEnabled()) {
            log.trace("Message sent: " + ec.message);
        }
    }

    /**
     * Gets the residual for a new message, as the maximum error over all
     * assignments.
     * 
     * Following the definition of Sutton & McCallum (2007), we compute the
     * residual as the infinity norm of the difference of the log of the message
     * vectors.
     */
    private double getResidual(VarTensor message, VarTensor newMessage) {
        VarTensor logRatio = new VarTensor(newMessage);
        if (prm.logDomain) {
            logRatio.subBP(message);
        } else {
            logRatio.divBP(message);
            logRatio.convertRealToLog();
        }
        return logRatio.getInfNorm();
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
            if (prm.logDomain) {
                b.logNormalize();
            } else {
                b.normalize();
            }
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
        VarTensor prod = new VarTensor(new VarSet(var), prm.logDomain ? 0.0 : 1.0);
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
        
        VarTensor prod = new VarTensor(BruteForceInferencer.safeGetDenseFactor(factor));
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
            double partition = prm.logDomain ? 0.0 : 1.0;
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
                
                double nodePartition = getPartitionFunctionAtVarNode(node);
                if (prm.logDomain) {
                    partition += nodePartition;
                } else {
                    partition *= nodePartition;   
                }
            }
            assert !Double.isNaN(partition);
            return partition;
        }
        
        if (!prm.logDomain) {
            return Math.exp(-getBetheFreeEnergy());
        } else {
            return - getBetheFreeEnergy();
        }
    }
    
    /**
     * Computes the Bethe free energy of the factor graph. For acyclic graphs,
     * this is equal to -log(Z) where Z is the exact partition function. For 
     * loopy graphs it can be used as an approximation.
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
        
        double semiringZero = prm.logDomain ? Double.NEGATIVE_INFINITY : 0.0;
        
        double bethe = 0.0;
        Set<Class<?>> ignoredClasses = new HashSet<Class<?>>();
        for (int a=0; a<fg.getFactors().size(); a++) {
            Factor f = fg.getFactors().get(a);
            if (!(f instanceof GlobalFactor)) {
                int numConfigs = f.getVars().calcNumConfigs();
                VarTensor beliefs = getFactorBeliefs(a);
                for (int c=0; c<numConfigs; c++) {                
                    double chi_c = f.getUnormalizedScore(c);
                    // Since we want multiplication by 0 to always give 0 (not the case for Double.POSITIVE_INFINITY or Double.NaN.
                    if (beliefs.getValue(c) != semiringZero) { 
                        if (prm.logDomain) {
                            bethe += FastMath.exp(beliefs.getValue(c)) * (beliefs.getValue(c) - chi_c);
                        } else {
                            bethe += beliefs.getValue(c) * FastMath.log(beliefs.getValue(c) / chi_c);
                        }
                    }
                }
            } else {
                bethe += ((GlobalFactor) f).getExpectedLogBelief(fg.getFactorNode(a), msgs, prm.logDomain);
            }
        }
        for (int i=0; i<fg.getVars().size(); i++) {
            Var v = fg.getVars().get(i);
            int numNeighbors = fg.getVarNode(i).getOutEdges().size();
            VarTensor beliefs = getVarBeliefs(i);
            double sum = 0.0;
            for (int c=0; c<v.getNumStates(); c++) {
                if (beliefs.getValue(c) != semiringZero) { 
                    if (prm.logDomain) {
                        sum += FastMath.exp(beliefs.getValue(c)) * beliefs.getValue(c);
                    } else {
                        sum += beliefs.getValue(c) * FastMath.log(beliefs.getValue(c));
                    }
                }
            }
            bethe -= (numNeighbors - 1) * sum;
        }
        
        for (Class<?> clazz : ignoredClasses) {
            log.warn("Bethe free energy value is INVALID. Returning NaN instead. Ignoring factor for Bethe free energy computation: " + clazz);
            return Double.NaN;
        }
        
        assert !Double.isNaN(bethe);        
        return bethe;
    }

    /**
     * FOR TESTING ONLY.
     * Gets the partition function for the connected component containing the given node.
     */
    // TODO: This should be package private or protected. It is exposed for testing only.
    public double getPartitionFunctionAtVarNode(FgNode node) {
        // We just return the normalizing constant for the marginals of any variable.
        if (!node.isVar()) {
            throw new IllegalArgumentException("Node must be a variable node.");
        }
        Var var = node.getVar();
        VarTensor prod = new VarTensor(new VarSet(var), prm.logDomain ? 0.0 : 1.0);
        // Compute the product of all messages sent to this node.
        getProductOfMessages(node, prod, null);
        if (prm.logDomain) {
            return prod.getLogSum();
        } else {
            return prod.getSum();
        }
    }
    
    /* ------------------------- FgInferencer Methods -------------------- */
    
    /** @inheritDoc
     */
    @Override
    public VarTensor getMarginals(Var var) {
        VarTensor marg = getVarBeliefs(var);
        if (prm.logDomain) {
            marg = new VarTensor(marg); // Keep the cached beliefs intact.
            marg.convertLogToReal();
        }
        return marg;
    }
    
    /** @inheritDoc
     */
    @Override
    public VarTensor getMarginals(Factor factor) {
        VarTensor marg = getFactorBeliefs(factor);
        if (prm.logDomain) {
            marg = new VarTensor(marg); // Keep the cached beliefs intact.
            marg.convertLogToReal();
        }
        return marg;
    }
        
    /** @inheritDoc */
    @Override
    public VarTensor getMarginalsForVarId(int varId) {
        return getMarginals(fg.getVar(varId));
    }

    /** @inheritDoc */
    @Override
    public VarTensor getMarginalsForFactorId(int factorId) {
        return getMarginals(fg.getFactor(factorId));
    }

    /** @inheritDoc */
    @Override
    public double getPartition() {
        double pb = getPartitionBelief();
        if (prm.logDomain) {
            pb = FastMath.exp(pb);
        }
        return pb; 
    }    

    /** @inheritDoc
     */
    @Override
    public VarTensor getLogMarginals(Var var) {
        VarTensor marg = getVarBeliefs(var);
        if (!prm.logDomain) {
            marg = new VarTensor(marg); // Keep the cached beliefs intact.
            marg.convertRealToLog();
        }
        return marg;
    }
    
    /** @inheritDoc
     */
    @Override
    public VarTensor getLogMarginals(Factor factor) {
        VarTensor marg = getFactorBeliefs(factor);
        if (!prm.logDomain) {
            marg = new VarTensor(marg); // Keep the cached beliefs intact.
            marg.convertRealToLog();
        }
        return marg;
    }
        
    /** @inheritDoc */
    @Override
    public VarTensor getLogMarginalsForVarId(int varId) {
        return getLogMarginals(fg.getVar(varId));
    }

    /** @inheritDoc */
    @Override
    public VarTensor getLogMarginalsForFactorId(int factorId) {
        return getLogMarginals(fg.getFactor(factorId));
    }

    /** @inheritDoc */
    @Override
    public double getLogPartition() {
        double pb = getPartitionBelief();
        if (!prm.logDomain) {
            pb = FastMath.log(pb);
        }
        return pb; 
    }
        
    @Override
    public boolean isLogDomain() {
        return prm.logDomain;
    }

    public FactorGraph getFactorGraph() {
        return fg;
    }

    @Override
    public Beliefs getOutput() {
        return b;
    }

    @Override
    public Beliefs getOutputAdj() {
        if (bAdj == null) {
            bAdj = b.copyAndFill(0.0);
        }
        return bAdj;
    }

    @Override
    public void zeroOutputAdj() {
        if (bAdj != null) { bAdj.fill(0.0); }
    }

    @Override
    public List<Module<Beliefs>> getInputs() {
        throw new RuntimeException("Not implemented");
        //return Lists.getList();
    }
    
}
