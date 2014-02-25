package edu.jhu.gm.inf;


import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import edu.jhu.gm.model.DenseFactor;
import edu.jhu.gm.model.ExplicitFactor;
import edu.jhu.gm.model.Factor;
import edu.jhu.gm.model.FactorGraph;
import edu.jhu.gm.model.FactorGraph.FgEdge;
import edu.jhu.gm.model.FactorGraph.FgNode;
import edu.jhu.gm.model.GlobalFactor;
import edu.jhu.gm.model.UnsupportedFactorTypeException;
import edu.jhu.gm.model.Var;
import edu.jhu.gm.model.VarSet;
import edu.jhu.prim.util.math.FastMath;
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
        boolean isLogDomain();
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
        
        public boolean cacheFactorBeliefs = true;
        
        public BeliefPropagationPrm() {
        }
        public FgInferencer getInferencer(FactorGraph fg) {
            return new BeliefPropagation(fg, this);
        }
        public boolean isLogDomain() {
            return logDomain;
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
    
    private DenseFactor[] factorBeliefCache;

    public BeliefPropagation(FactorGraph fg, BeliefPropagationPrm prm) {
        this.prm = prm;
        this.fg = fg;
        this.msgs = new Messages[fg.getNumEdges()];
        this.factorBeliefCache = new DenseFactor[fg.getNumFactors()];
        
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
        // Initialize factor beliefs
        for(FgNode node : fg.getNodes()) {
        	if(node.isFactor() && !(node.getFactor() instanceof GlobalFactor)) {
            	Factor f = node.getFactor();        		
        		DenseFactor fBel = new DenseFactor(f.getVars());
            	int c = f.getVars().calcNumConfigs();
            	for(int i=0; i<c; i++)
            		fBel.setValue(i, f.getUnormalizedScore(i));
            	
            	for(FgEdge v2f : node.getInEdges()) {
            		DenseFactor vBel = msgs[v2f.getId()].message;
            		if(prm.logDomain) fBel.add(vBel);
            		else fBel.prod(vBel);
            	}
            	
            	factorBeliefCache[f.getId()] = fBel;
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
    protected void createMessage(FgEdge edge, int iter) {

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
        } else {
        	// Since this is not a global factor, we send messages in the normal way, which
            // in the case of a factor to variable message requires enumerating all possible
            // variable configurations.
            DenseFactor msg = msgs[edgeId].newMessage;
            
            // Initialize the message to all ones (zeros in log-domain) since we are "multiplying".
            msg.set(prm.logDomain ? 0.0 : 1.0);
            
            if (edge.isVarToFactor()) {
                // Message from variable v* to factor f*.
                //
                // Compute the product of all messages received by v* except for the
                // one from f*.
                getProductOfMessages(edge.getParent(), msg, edge.getChild());
            } else {
                // Message from factor f* to variable v*.
                //
                // Compute the product of all messages received by f* (each
                // of which will have a different domain) with the factor f* itself.
                // Exclude the message going out to the variable, v*.
                DenseFactor prod;
            	if(prm.cacheFactorBeliefs && !(factor instanceof GlobalFactor)) {
            		// we are computing f->v, which is the product of a bunch of factor values and v->f messages
            		// we can cache this product and remove the v->f message that would have been excluded from the product
            		DenseFactor remove = msgs[edge.getOpposing().getId()].message;
            		DenseFactor from = factorBeliefCache[factor.getId()];
            		prod = new DenseFactor(from);
            		if(prm.logDomain) prod.subBP(remove);
            		else prod.divBP(remove);
            		
            		assert !prod.containsBadValues(prm.logDomain) : "prod from cached beliefs = " + prod;
            	}
            	else {	// fall back on normal way of computing messages without caching
            		prod = new DenseFactor(factor.getVars());
                	// Set the initial values of the product to those of the sending factor.
                	int numConfigs = prod.getVars().calcNumConfigs();
                	for (int c = 0; c < numConfigs; c++) {
                		prod.setValue(c, factor.getUnormalizedScore(c));
                	}
                	getProductOfMessages(edge.getParent(), prod, edge.getChild());
            	}

            	
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
            // normalize and logNormalize already check for NaN
            else assert !msg.containsBadValues(prm.logDomain) : "msg = " + msg;
            
            // Set the final message in case we created a new object.
            msgs[edgeId].newMessage = msg;
        }
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
    protected void getProductOfMessages(FgNode node, DenseFactor prod, FgNode exclNode) {
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
            assert !prod.containsBadValues(prm.logDomain) : "prod = " + prod;
        }
    }

    /** Gets the product of messages (as in getProductOfMessages()) and then normalizes. */
    protected void getProductOfMessagesNormalized(FgNode node, DenseFactor prod, FgNode exclNode) {
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
    protected void sendMessage(FgEdge edge) {
        int edgeId = edge.getId();
       
        Messages ec = msgs[edgeId];
        // Just swap the pointers to the current message and the new message, so
        // that we don't have to create a new factor object.
        DenseFactor oldMessage = ec.message;
        ec.message = ec.newMessage;
        ec.newMessage = oldMessage;
        
        // update factor beliefs
        if(prm.cacheFactorBeliefs && edge.isVarToFactor() && !(edge.getFactor() instanceof GlobalFactor)) {
        	Factor f = edge.getFactor();
        	DenseFactor update = factorBeliefCache[f.getId()];
        	if(prm.isLogDomain()) {
        		update.subBP(oldMessage);
        		update.add(ec.message);
        	}
        	else {
        		update.divBP(oldMessage);
        		update.prod(ec.message);
        	}
        }
        
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

    protected DenseFactor getMarginals(Var var, FgNode node) {
        DenseFactor prod = new DenseFactor(new VarSet(var), prm.logDomain ? 0.0 : 1.0);
        // Compute the product of all messages sent to this variable.
        getProductOfMessagesNormalized(node, prod, null);
        return prod;
    }

    protected DenseFactor getMarginals(Factor factor, FgNode node) {
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
        //           = \sum_a \sum_{x_a} - b(x_a) ln (\chi(x_a) / b(x_a))
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
            if (f instanceof ExplicitFactor) {
                int numConfigs = f.getVars().calcNumConfigs();
                DenseFactor beliefs = getMarginalsForFactorId(a);
                for (int c=0; c<numConfigs; c++) {                
                    double chi_c = ((ExplicitFactor) f).getValue(c);
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
                // TODO: we need to support GlobalFactors here. Until that is done, this computation will be incorrect for 
                // factor graphs with global factors.
                // bethe += ((GlobalFactor) f).getExpectedLogBelief();
                ignoredClasses.add(f.getClass());
            }
        }
        for (int i=0; i<fg.getVars().size(); i++) {
            Var v = fg.getVars().get(i);
            int numNeighbors = fg.getVarNode(i).getOutEdges().size();
            DenseFactor beliefs = getMarginalsForVarId(i);
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
