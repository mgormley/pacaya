package edu.jhu.gm.inf;

import edu.jhu.gm.model.Var;
import edu.jhu.gm.model.VarSet;
import edu.jhu.gm.model.VarTensor;
import edu.jhu.gm.model.FactorGraph.FgEdge;

/**
 * A container class for messages and properties of an edge in a factor
 * graph.
 * 
 * @author mgormley
 * 
 */
public class Messages {
    
    /** The current message. */
    public VarTensor message;
    /** The pending messge. */
    public VarTensor newMessage;
    /** The residual between the previous message and the current message. */
    public double residual = Double.POSITIVE_INFINITY;
    
    /** Constructs a message container, initializing the messages to the uniform distribution. */
    public Messages(FgEdge edge, boolean logDomain, boolean normalizeMessages) {
        // Initialize messages to the (possibly unnormalized) uniform
        // distribution in case we want to run parallel BP.
        double initialValue = logDomain ? 0.0 : 1.0;
        // Every message to/from a variable will be a factor whose domain is
        // that variable only.
        Var var = edge.getVar();
        VarSet vars = new VarSet(var); // TODO: Can we create only one of these per variable?
        message = new VarTensor(vars, initialValue);
        newMessage = new VarTensor(vars, initialValue);
        
        if (normalizeMessages) {
            // Normalize the initial messages.
            if (logDomain) {
                message.logNormalize();
                newMessage.logNormalize();
            } else {
                message.normalize();
                newMessage.normalize();
            }
        }
    }

    @Override
    public String toString() {
        return "Messages [message=" + message + ", newMessage=" + newMessage + ", residual=" + residual + "]";
    }    
    
}