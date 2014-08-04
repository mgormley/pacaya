package edu.jhu.gm.inf;

import java.util.ArrayList;
import java.util.List;

import edu.jhu.gm.inf.BeliefPropagation.BpScheduleType;
import edu.jhu.gm.inf.BeliefPropagation.BpUpdateOrder;
import edu.jhu.gm.model.FactorGraph.FgEdge;

/**
 * Wraps a MessagePassingSchedule to create an iteration-specific schedule such that: at iteration
 * -1, we send all the constant messages, then we never send them again.
 * 
 * @author mgormley
 */
public class CachingBpSchedule {

    private MpSchedule sched;
    private List<FgEdge> order;
    private boolean canReuseOrder;

    public CachingBpSchedule(MpSchedule sched, BpUpdateOrder updateOrder, BpScheduleType schedule) {
        this.sched = sched;
        this.order = null;
        this.canReuseOrder = !(updateOrder == BpUpdateOrder.SEQUENTIAL || schedule == BpScheduleType.RANDOM);
    }

    public List<FgEdge> getOrder(int iter) {
        if (canReuseOrder && iter >= 1) {
            // Just re-use the same order.
            return order;
        }
        // Get the initial order for the edges.
        order = sched.getOrder();
        if (iter == -1) {
            // Keep only the messages from the leaves for iteration -1. Then never send these again.
            order = filterNonConstantMsgs(order);
        } else {
            // Filter out the messages from the leaves.
            order = filterConstantMsgs(order);
        }
        return order;
    }

    /** Filters edges from a leaf node. */
    private static List<FgEdge> filterConstantMsgs(List<FgEdge> order) {
        ArrayList<FgEdge> filt = new ArrayList<FgEdge>();
        for (FgEdge edge : order) {
            // If the parent node is not a leaf.
            if (!isConstantMsg(edge)) {
                filt.add(edge);
            }
        }
        return filt;
    }

    /** Filters edges not from a leaf node. */
    private static List<FgEdge> filterNonConstantMsgs(List<FgEdge> order) {
        ArrayList<FgEdge> filt = new ArrayList<FgEdge>();
        for (FgEdge edge : order) {
            // If the parent node is not a leaf.
            if (isConstantMsg(edge)) {
                filt.add(edge);
            }
        }
        return filt;
    }

    /**
     * Returns true iff the edge corresponds to a message which is constant (i.e. sent from a leaf
     * node).
     */
    public static boolean isConstantMsg(FgEdge edge) {
        return edge.getParent().getOutEdges().size() == 1;
    }

}
