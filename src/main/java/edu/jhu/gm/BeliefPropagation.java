package edu.jhu.gm;


import edu.jhu.gm.FactorGraph.FgEdge;
import edu.jhu.gm.FactorGraph.FgNode;
import edu.jhu.util.Timer;

/**
 * Loopy belief propagation inference algorithm.
 * 
 * @author mgormley
 *
 */
public class BeliefPropagation implements FgInferencer {

    public static class BeliefPropagationPrm {
        public BpSchedule schedule = null;
        public int maxIterations = 100;
        public double timeoutSeconds = Double.POSITIVE_INFINITY;
        public BpUpdateOrder updateOrder = BpUpdateOrder.PARALLEL;
        public final FactorGraph fg;
        public boolean logDomain = true;
        /** Whether to normalize the messages after sending. */
        public boolean normalizeMessages = true;
        public BeliefPropagationPrm(FactorGraph fg) {
            this.fg = fg;
        }
    }
    
    public enum BpUpdateOrder {
        /** Send each message in sequence according to the schedule. */ 
        SEQUENTIAL,
        /** Create all messages first. Then send them all at the same time. */
        PARALLEL
    };
    
    /**
     * A container class for messages and properties of an edge in a factor
     * graph.
     * 
     * @author mgormley
     * 
     */
    private class Messages {
        
        /** The current message. */
        public Factor message;
        /** The pending messge. */
        public Factor newMessage;
        
        /** Constructs a message container, initializing the messages to the uniform distribution. */
        public Messages(FgEdge edge) {
            // Initialize messages to the (possibly unormalized) uniform
            // distribution in case we want to run parallel BP.
            double initialValue = prm.logDomain ? 0.0 : 1.0;
            // Every message to/from a variable will be a factor whose domain is
            // that variable only.
            Var var = edge.getVar();
            message = new Factor(new VarSet(var), initialValue);
            newMessage = new Factor(new VarSet(var), initialValue);
            
            if (prm.normalizeMessages) {
                // Normalize the initial messages.
                if (prm.logDomain) {
                    message.logNormalize();
                    newMessage.logNormalize();
                } else {
                    message.normalize();
                    newMessage.normalize();
                }
            }
        }
        
    }
    
    private final BeliefPropagationPrm prm;
    private final FactorGraph fg;
    /** A container of messages each edge in the factor graph. Indexed by edge id. */
    private final Messages[] msgs;

    public BeliefPropagation(BeliefPropagationPrm prm) {
        this.prm = prm;
        this.fg = prm.fg;
        this.msgs = new Messages[fg.getNumEdges()];
    }
    
    /** @inheritDoc */
    @Override
    public void run() {
        Timer timer = new Timer();
        timer.start();
        
        // Initialization.
        for (int i=0; i<msgs.length; i++) {
            // TODO: consider alternate initializations.
            msgs[i] = new Messages(fg.getEdge(i));
        }
        
        // Message passing.
        for (int iter=0; iter < prm.maxIterations; iter++) {
            if (timer.totSec() > prm.timeoutSeconds) {
                break;
            }
            if (prm.updateOrder == BpUpdateOrder.SEQUENTIAL) {
                for (FgEdge edge : prm.schedule.getOrder()) {
                    createMessage(edge);
                    sendMessage(edge);
                }
            } else if (prm.updateOrder == BpUpdateOrder.PARALLEL) {
                for (FgEdge edge : fg.getEdges()) {
                    createMessage(edge);
                }
                for (FgEdge edge : fg.getEdges()) {
                    sendMessage(edge);
                }
            } else {
                throw new RuntimeException("Unsupported update order: " + prm.updateOrder);
            }
        }
        timer.stop();
    }
    
    /**
     * Creates a message and stores it in the "pending message" slot for this edge.
     * @param edge The directed edge for which the message should be created.
     */
    private void createMessage(FgEdge edge) {
        int edgeId = edge.getId();
        Var var = edge.getVar();
        Factor factor = edge.getFactor();
        
        Factor msg = msgs[edgeId].newMessage;
        
        // Initialize the message to all ones (zeros in log-domain) since we are "multiplying".
        msg.set(prm.logDomain ? 0.0 : 1.0);
        
        if (edge.isVarToFactor()) {
            // Message from variable v* to factor f*.
            //
            // Compute the product of all messages recieved by v* except for the
            // one from f*.
            getProductOfMessages(edge.getParent(), msg, edge.getChild());
        } else {
            // Message from factor f* to variable v*.
            //
            // Compute the product of all messages received by f* (each
            // of which will have a different domain) with the factor f* itself.
            // Exclude the message going out to the variable, v*.

            // TODO: we could cache this prod factor in the EdgeContent for this
            // edge if creating it is slow.
            Factor prod = new Factor(factor.getVars());
            // Set the initial values of the product to those of the sending factor.
            prod.set(factor);
            getProductOfMessages(edge.getParent(), prod, edge.getChild());

            // Marginalize over all the assignments to variables for f*, except
            // for v*.
            if (prm.logDomain) { 
                msg = prod.getLogMarginal(new VarSet(var), false);
            } else {
                msg = prod.getMarginal(new VarSet(var), false);
            }
        }
        
        assert (msg.getVars().equals(new VarSet(var)));
        
        if (prm.normalizeMessages) {
            if (prm.logDomain) { 
                msg.logNormalize();
            } else {
                msg.normalize();
            }
        }
        
        // Set the final message in case we created a new object.
        msgs[edgeId].newMessage = msg;
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
    private void getProductOfMessages(FgNode node, Factor prod, FgNode exclNode) {
        for (FgEdge nbEdge : node.getInEdges()) {
            if (nbEdge.getParent() == exclNode) {
                // Don't include the receiving variable.
                continue;
            }
            // Get message from neighbor to factor.
            Factor nbMsg = msgs[nbEdge.getId()].message;
            
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
    private void getProductOfMessagesNormalized(FgNode node, Factor prod, FgNode exclNode) {
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
    private void sendMessage(FgEdge edge) {
        int edgeId = edge.getId();

        Messages ec = msgs[edgeId];
        // Just swap the pointers to the current message and the new message, so
        // that we don't have to create a new factor object.
        Factor oldMessage = ec.message;
        ec.message = ec.newMessage;
        ec.newMessage = oldMessage;
    }

    /** @inheritDoc */
    @Override
    public Factor getMarginals(VarSet varSet) {
        // We could implement this method, but it will be slow since we'll have
        // to find a factor that contains all of these variables.
        // For now we just throw an exception.
        throw new RuntimeException("not implemented");
    }

    private Factor getMarginals(Var var, FgNode node) {
        Factor prod = new Factor(new VarSet(var), prm.logDomain ? 0.0 : 1.0);
        // Compute the product of all messages sent to this variable.
        getProductOfMessagesNormalized(node, prod, null);
        return prod;
    }

    private Factor getMarginals(Factor factor, FgNode node) {
        Factor prod = new Factor(factor);
        // Compute the product of all messages sent to this factor.
        getProductOfMessagesNormalized(node, prod, null);
        return prod;
    }
    
    /** @inheritDoc
     * Note this method is slow compared to querying by id, and requires an extra hashmap lookup.  
     */
    @Override
    public Factor getMarginals(Var var) {
        FgNode node = fg.getNode(var);
        return getMarginals(var, node);
    }
    
    /** @inheritDoc
     * Note this method is slow compared to querying by id, and requires an extra hashmap lookup.  
     */
    @Override
    public Factor getMarginals(Factor factor) {
        FgNode node = fg.getNode(factor);
        return getMarginals(factor, node);
    }
        
    /** @inheritDoc */
    @Override
    public Factor getMarginalsForVarId(int varId) {
        FgNode node = fg.getVarNode(varId);
        return getMarginals(node.getVar(), node);
    }

    /** @inheritDoc */
    @Override
    public Factor getMarginalsForFactorId(int factorId) {
        FgNode node = fg.getFactorNode(factorId);
        return getMarginals(node.getFactor(), node);
    }

    /** @inheritDoc */
    @Override
    public double getPartition() {
        // We just return the normalizing constant for the marginals of any variable.
        Var var = fg.getVar(0);
        Factor prod = new Factor(new VarSet(var), prm.logDomain ? 0.0 : 1.0);
        // Compute the product of all messages sent to this variable.
        getProductOfMessages(fg.getNode(var), prod, null);
        if (prm.logDomain) {
            return prod.getLogSum();
        } else {
            return prod.getSum();
        }
    }
    
}
