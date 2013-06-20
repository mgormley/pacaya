package edu.jhu.hltcoe.gm;


import edu.jhu.hltcoe.gm.BipartiteGraph.Edge;
import edu.jhu.hltcoe.gm.BipartiteGraph.Node;
import edu.jhu.hltcoe.gm.FactorGraph.FgEdge;
import edu.jhu.hltcoe.util.Timer;
import edu.jhu.hltcoe.util.math.Vectors;

/**
 * Loopy belief propagation inference algorithm.
 * 
 * @author mgormley
 *
 */
public class BeliefPropagation implements FgInferencer {

    public static class BeliefPropagationPrm {
        public BpSchedule schedule;
        public int maxIterations;
        public int timeoutSeconds;
        public BpUpdateOrder updateOrder;
        public final FactorGraph fg;
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
    private static class Messages {
        
        /** The current message. */
        public Factor message;
        /** The pending messge. */
        public Factor newMessage;
        
        public Messages(FgEdge edge) {
            Var var = edge.getVar();
            message = new Factor(new VarSet(var));
            newMessage = new Factor(new VarSet(var));
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
        for (int i=0; i<msgs.length; i++) {
            msgs[i] = new Messages(fg.getEdge(i));
            // TODO: consider alternate initializations.
            msgs[i].message.set(1.0);
            msgs[i].newMessage.set(1.0);
        }
    }
    
    /** @inheritDoc */
    @Override
    public void run() {
        Timer timer = new Timer();
        timer.start();
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
        
        // We are working in the log-domain so initialize to 0.0, since we're "multiplying".
        msg.set(0.0);
        
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
            prod.set(factor);
            getProductOfMessages(edge.getParent(), prod, edge.getChild());

            // Marginalize over all the assignments to variables for f*, except
            // for v*.
            msg = prod.getMarginal(new VarSet(var), false);
        }
        
        msg.logNormalize();      
        
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
    private void getProductOfMessages(Node node, Factor prod, Node exclNode) {
        for (FgEdge nbEdge : fg.getEdges(node)) {
            if (nbEdge.getParent() == exclNode) {
                // Don't include the receiving variable.
                continue;
            }
            // Get message from neighbor to factor.
            Factor nbMsg = msgs[nbEdge.getId()].message;
            
            // The neighbor messages have a different domain, but addition
            // should still be well defined.
            prod.add(nbMsg);
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
    public Factor getMarginals(Var var) {
        Factor prod = new Factor(new VarSet(var));
        // Compute the product of all messages sent to this variable.
        getProductOfMessages(fg.getNode(var), prod, null);
        // Normalize the product.
        prod.logNormalize();
        return prod;
    }

    /** @inheritDoc */
    @Override
    public Factor getMarginals(VarSet varSet) {
        // We could implement this method, but it will be slow since we'll have
        // to find a factor that contains all of these variables.
        // For now we just throw an exception.
        throw new RuntimeException("not implemented");
    }
    
    /** @inheritDoc */
    @Override
    public Factor getMarginals(Factor factor) {
        Factor prod = new Factor(factor.getVars());
        // Compute the product of all messages sent to this variable.
        getProductOfMessages(fg.getNode(factor), prod, null);
        // Normalize the product.
        prod.logNormalize();
        return prod;
    }
    
    /** @inheritDoc */
    @Override
    public Factor getMarginalsForVarId(int varId) {
        return getMarginals(fg.getVar(varId));
    }

    /** @inheritDoc */
    @Override
    public Factor getMarginalsForFactorId(int factorId) {
        return getMarginals(fg.getFactor(factorId));
    }

    /** @inheritDoc */
    @Override
    public double getLogPartition(FgExample ex, FgModel model) {
        // We just return the normalizing constant for the marginals of any variable.
        Factor v0Marginal = getMarginalsForVarId(0);
        return v0Marginal.getLogSum();
    }
    
}
