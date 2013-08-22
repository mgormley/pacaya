package edu.jhu.gm;


import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;

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
    
    private static final Logger log = Logger.getLogger(BeliefPropagation.class);

    public interface FgInferencerFactory {
        FgInferencer getInferencer(FactorGraph fg);
    }
    
    public static class BeliefPropagationPrm implements FgInferencerFactory {
        public BpScheduleType schedule = BpScheduleType.TREE_LIKE;
        public int maxIterations = 100;
        public double timeoutSeconds = Double.POSITIVE_INFINITY;
        public BpUpdateOrder updateOrder = BpUpdateOrder.PARALLEL;
        //public final FactorGraph fg;
        public boolean logDomain = true;
        /** Whether to normalize the messages after sending. */
        public boolean normalizeMessages = true;
        public BeliefPropagationPrm() {
        }
        public FgInferencer getInferencer(FactorGraph fg) {
            return new BeliefPropagation(fg, this);
        }
    }
    
    public enum BpScheduleType {
        /** Send messages from a root to the leaves and back. */
        TREE_LIKE,
        /** Send messages in a random order. */
        RANDOM
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
    public static class Messages {
        
        /** The current message. */
        public DenseFactor message;
        /** The pending messge. */
        public DenseFactor newMessage;
        
        /** Constructs a message container, initializing the messages to the uniform distribution. */
        public Messages(FgEdge edge, BeliefPropagationPrm prm) {
            // Initialize messages to the (possibly unormalized) uniform
            // distribution in case we want to run parallel BP.
            double initialValue = prm.logDomain ? 0.0 : 1.0;
            // Every message to/from a variable will be a factor whose domain is
            // that variable only.
            Var var = edge.getVar();
            message = new DenseFactor(new VarSet(var), initialValue);
            newMessage = new DenseFactor(new VarSet(var), initialValue);
            
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
    private BpSchedule sched;
    private List<FgEdge> order;
    
    public BeliefPropagation(FactorGraph fg, BeliefPropagationPrm prm) {
        this.prm = prm;
        this.fg = fg;
        this.msgs = new Messages[fg.getNumEdges()];

        if (prm.updateOrder == BpUpdateOrder.SEQUENTIAL) {
            // Cache the order if this is a sequential update.
            if (prm.schedule == BpScheduleType.TREE_LIKE) {
                sched = new BfsBpSchedule(fg);
            } else if (prm.schedule == BpScheduleType.RANDOM) {
                sched = new RandomBpSchedule(fg);
            } else {
                throw new RuntimeException("Unknown schedule type: " + prm.schedule);
            }
            order = sched.getOrder();
        }
        

//        // TODO: REMOVE THIS!!!!!! Only using this to establish memory usage.
//        // Initialization.
//        for (int i=0; i<msgs.length; i++) {
//            // TODO: consider alternate initializations.
//            msgs[i] = new Messages(fg.getEdge(i));
//        }
    }
    
    /** @inheritDoc */
    @Override
    public void run() {
        Timer timer = new Timer();
        timer.start();
        
        // Initialization.
        for (int i=0; i<msgs.length; i++) {
            // TODO: consider alternate initializations.
            msgs[i] = new Messages(fg.getEdge(i), prm);
        }
        // Reset the global factors.
        for (Factor factor : fg.getFactors()) {
            if (factor instanceof GlobalFactor) {
                ((GlobalFactor)factor).reset();
            }
        }
        
        // Message passing.
        for (int iter=0; iter < prm.maxIterations; iter++) {
            if (timer.totSec() > prm.timeoutSeconds) {
                break;
            }
            if (prm.updateOrder == BpUpdateOrder.SEQUENTIAL) {
                for (FgEdge edge : order) {
                    createMessage(edge, iter);
                    sendMessage(edge);
                }
            } else if (prm.updateOrder == BpUpdateOrder.PARALLEL) {
                for (FgEdge edge : fg.getEdges()) {
                    createMessage(edge, iter);
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
        
    public void clear() {
        Arrays.fill(msgs, null);
    }
    
    /**
     * Creates a message and stores it in the "pending message" slot for this edge.
     * @param edge The directed edge for which the message should be created.
     * @param iter The iteration number.
     */
    private void createMessage(FgEdge edge, int iter) {
        int edgeId = edge.getId();
        Var var = edge.getVar();
        Factor factor = edge.getFactor();
        
        if (!edge.isVarToFactor() && factor instanceof GlobalFactor) {
            log.trace("Creating messages for global factor.");
            // Since this is a global factor, we pass the incoming messages to it, 
            // and efficiently marginalize over the variables. The current setup is
            // create all the messages from this factor to its variables, but only 
            // once per iteration.
            GlobalFactor globalFac = (GlobalFactor) factor;
            globalFac.createMessages(edge.getParent(), msgs, prm.logDomain, iter);
            // The messages have been set, so just return.
            return;
        } else if (!edge.isVarToFactor() && !(factor instanceof ExplicitFactor)) {
            throw new UnsupportedFactorTypeException(factor);
        }

        
        DenseFactor msg = msgs[edgeId].newMessage;
        
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
            DenseFactor prod = new DenseFactor(factor.getVars());
            // Set the initial values of the product to those of the sending factor.
            prod.set((DenseFactor) factor);
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
    private void getProductOfMessages(FgNode node, DenseFactor prod, FgNode exclNode) {
        for (FgEdge nbEdge : node.getInEdges()) {
            if (nbEdge.getParent() == exclNode) {
                // Don't include the receiving variable.
                continue;
            }
            // Get message from neighbor to factor.
            DenseFactor nbMsg = msgs[nbEdge.getId()].message;
            
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
    private void getProductOfMessagesNormalized(FgNode node, DenseFactor prod, FgNode exclNode) {
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
        DenseFactor oldMessage = ec.message;
        ec.message = ec.newMessage;
        ec.newMessage = oldMessage;
        
        if (log.isTraceEnabled()) {
            log.trace("Message sent: " + ec.message);
        }
    }

    /** @inheritDoc */
    @Override
    public DenseFactor getMarginals(VarSet varSet) {
        // We could implement this method, but it will be slow since we'll have
        // to find a factor that contains all of these variables.
        // For now we just throw an exception.
        throw new RuntimeException("not implemented");
    }

    private DenseFactor getMarginals(Var var, FgNode node) {
        DenseFactor prod = new DenseFactor(new VarSet(var), prm.logDomain ? 0.0 : 1.0);
        // Compute the product of all messages sent to this variable.
        getProductOfMessagesNormalized(node, prod, null);
        return prod;
    }

    private DenseFactor getMarginals(Factor factor, FgNode node) {
        if (!(factor instanceof ExplicitFactor)) {
            throw new UnsupportedFactorTypeException(factor, "Getting marginals of a global factor is not supported."
                    + " This would require exponential space to store the resulting factor.");
        }
        
        DenseFactor prod = new DenseFactor((DenseFactor)factor);
        // Compute the product of all messages sent to this factor.
        getProductOfMessagesNormalized(node, prod, null);
        return prod;
    }
    
    /** @inheritDoc
     * Note this method is slow compared to querying by id, and requires an extra hashmap lookup.  
     */
    @Override
    public DenseFactor getMarginals(Var var) {
        FgNode node = fg.getNode(var);
        return getMarginals(var, node);
    }
    
    /** @inheritDoc
     * Note this method is slow compared to querying by id, and requires an extra hashmap lookup.  
     */
    @Override
    public DenseFactor getMarginals(Factor factor) {
        FgNode node = fg.getNode(factor);
        return getMarginals(factor, node);
    }
        
    /** @inheritDoc */
    @Override
    public DenseFactor getMarginalsForVarId(int varId) {
        FgNode node = fg.getVarNode(varId);
        return getMarginals(node.getVar(), node);
    }

    /** @inheritDoc */
    @Override
    public DenseFactor getMarginalsForFactorId(int factorId) {
        FgNode node = fg.getFactorNode(factorId);
        return getMarginals(node.getFactor(), node);
    }

    /** @inheritDoc */
    @Override
    public double getPartition() {
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
        return partition;
    }

    /**
     * Gets the partition function for the connected component containing the given node.
     * 
     * Package private FOR TESTING ONLY.
     */
    double getPartitionFunctionAtVarNode(FgNode node) {
        // We just return the normalizing constant for the marginals of any variable.
        if (!node.isVar()) {
            throw new IllegalArgumentException("Node must be a variable node.");
        }
        Var var = node.getVar();
        DenseFactor prod = new DenseFactor(new VarSet(var), prm.logDomain ? 0.0 : 1.0);
        // Compute the product of all messages sent to this node.
        getProductOfMessages(node, prod, null);
        if (prm.logDomain) {
            return prod.getLogSum();
        } else {
            return prod.getSum();
        }
    }

    @Override
    public boolean isLogDomain() {
        return prm.logDomain;
    }
    
}
