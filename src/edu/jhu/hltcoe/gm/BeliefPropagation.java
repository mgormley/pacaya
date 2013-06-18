package edu.jhu.hltcoe.gm;


import edu.jhu.hltcoe.gm.BipartiteGraph.Edge;
import edu.jhu.hltcoe.gm.BipartiteGraph.Node;
import edu.jhu.hltcoe.gm.FactorGraph.Factor;
import edu.jhu.hltcoe.gm.FactorGraph.Var;
import edu.jhu.hltcoe.gm.FactorGraph.VarSet;
import edu.jhu.hltcoe.util.Timer;

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
     * A container class for messages and properties of an edge in a factor graph.
     * 
     * @author mgormley
     *
     */
    private static class EdgeContents {
        public Factor message;
        public Factor newMessage;
        public EdgeContents(Edge edge) {
            Node n1 = edge.getFirst();
            Node n2 = edge.getSecond();
            Factor factor;
            Var var;
            if (n1 instanceof Factor) {
                factor = (Factor) n1;
                var = (Var) n2;
            } else {
                factor = (Factor) n2;
                var = (Var) n1;
            }
            message = new Factor(new VarSet(var));
            newMessage = new Factor(new VarSet(var));
        }
    }
    
    private final BeliefPropagationPrm prm;
    private final FactorGraph fg;
    /** A container of messages each edge in the factor graph. */
    private final EdgeContents[] edges;

    public BeliefPropagation(BeliefPropagationPrm prm) {
        this.prm = prm;
        this.fg = prm.fg;
        this.edges = new EdgeContents[fg.getNumEdges()];
        for (int i=0; i<edges.length; i++) {
            edges[i] = new EdgeContents(fg.getEdge(i));
            // TODO: consider alternate initializations.
            edges[i].message.set(1.0);
            edges[i].newMessage.set(1.0);
        }
    }
    
    @Override
    public void run() {
        Timer timer = new Timer();
        timer.start();
        for (int iter=0; iter < prm.maxIterations; iter++) {
            if (timer.totSec() > prm.timeoutSeconds) {
                break;
            }
            if (prm.updateOrder == BpUpdateOrder.SEQUENTIAL) {
                for (Edge edge : prm.schedule.getOrder()) {
                    createMessage(edge);
                    sendMessage(edge);
                }
            } else if (prm.updateOrder == BpUpdateOrder.PARALLEL) {
                for (Edge edge : fg.getEdges()) {
                    createMessage(edge);
                }
                for (Edge edge : fg.getEdges()) {
                    sendMessage(edge);
                }
            } else {
                throw new RuntimeException("Unsupported update order: " + prm.updateOrder);
            }
        }
        timer.stop();
    }
    
    private void createMessage(Edge edge) {
        Node sender = edge.getFirst();
        Node receiver = edge.getSecond();
        
        int edgeId = 0; // TODO: 
        edges[edgeId].newMessage.set(0);
    }

    private void sendMessage(int edgeId) {
        //EdgeContent ec = edges[edgeId];
    }

    @Override
    public Factor getMarginals(Var var) {
        return getMarginals(new VarSet(var));
    }

    @Override
    public Factor getMarginals(VarSet varSet) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Factor getMarginals(Factor factor) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Factor getMarginalsForVarId(int varId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Factor getMarginalsForFactorId(int factorId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public double getLogPartition(FgExample ex, FgModel model) {
        // TODO Auto-generated method stub
        return 0;
    }
    
}
