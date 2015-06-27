package edu.jhu.pacaya.gm.inf;

import edu.jhu.pacaya.gm.model.Var;
import edu.jhu.pacaya.gm.model.VarSet;
import edu.jhu.pacaya.gm.model.VarTensor;
import edu.jhu.pacaya.gm.model.FactorGraph.FgEdge;
import edu.jhu.pacaya.util.semiring.Algebra;

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
    
    /** Constructs a message container, initializing the messages to the uniform distribution. 
     * @param s TODO*/
    public Messages(Algebra s, FgEdge edge, double initialValue) {
        // Initialize messages to the (possibly unnormalized) uniform
        // distribution in case we want to run parallel BP.

        // Every message to/from a variable will be a factor whose domain is
        // that variable only.
        Var var = edge.getVar();
        VarSet vars = new VarSet(var); // TODO: Can we create only one of these per variable?
        message = new VarTensor(s, vars, initialValue);
        newMessage = new VarTensor(s, vars, initialValue);
    }

    @Override
    public String toString() {
        return "Messages [message=" + message + ", newMessage=" + newMessage + ", residual=" + residual + "]";
    }    
    
}