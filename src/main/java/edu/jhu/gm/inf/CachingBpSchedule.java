package edu.jhu.gm.inf;

import java.util.ArrayList;
import java.util.List;

import edu.jhu.gm.inf.BeliefPropagation.BpScheduleType;
import edu.jhu.gm.inf.BeliefPropagation.BpUpdateOrder;
import edu.jhu.gm.model.FactorGraph;
import edu.jhu.gm.model.FactorGraph.FgEdge;
import edu.jhu.gm.model.FactorGraph.FgNode;
import edu.jhu.gm.model.globalfac.GlobalFactor;
import edu.jhu.util.collections.Lists;

/**
 * Wraps a MessagePassingSchedule to create an iteration-specific schedule such that: at iteration
 * -1, we send all the constant messages, then we never send them again.
 * 
 * @author mgormley
 */
public class CachingBpSchedule {

    private MpSchedule sched;
    private List<Object> order;
    private boolean canReuseOrder;

    public CachingBpSchedule(MpSchedule sched, BpUpdateOrder updateOrder, BpScheduleType schedule) {
        this.sched = sched;
        this.order = null;
        this.canReuseOrder = !(updateOrder == BpUpdateOrder.SEQUENTIAL || schedule == BpScheduleType.RANDOM);
    }

    public List<Object> getOrder(int iter) {
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
    @SuppressWarnings("unchecked")
    private static List<Object> filterConstantMsgs(List<Object> order) {
        ArrayList<Object> filt = new ArrayList<Object>();
        for (Object item : order) {
            if (item instanceof List) {
                List<Object> items = filterConstantMsgs((List<Object>) item);
                if (items.size() > 0) {
                    filt.add(items);
                }
            } else if (item instanceof FgEdge) {
                // If the parent node is not a leaf.
                if (!isConstantMsg((FgEdge) item)) {
                    filt.add(item);
                }
            } else if (item instanceof GlobalFactor) {
                filt.add(item);
            } else {
                throw new RuntimeException("Invalid type in order: " + item.getClass());
            }
        }
        return filt;
    }

    /** Filters edges not from a leaf node. */
    @SuppressWarnings("unchecked")
    private static List<Object> filterNonConstantMsgs(List<Object> order) {
        ArrayList<Object> filt = new ArrayList<Object>();
        for (Object item : order) {
            if (item instanceof List) {
                List<Object> items = filterNonConstantMsgs((List<Object>) item);
                if (items.size() > 0) {
                    filt.add(items);
                }
            } else if (item instanceof FgEdge) {
                // If the parent node is not a leaf.
                if (isConstantMsg((FgEdge) item)) {
                    filt.add(item);
                }
            } else if (item instanceof GlobalFactor) {
                // Filter.
            } else {
                throw new RuntimeException("Invalid type in order: " + item.getClass());
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
    
    @SuppressWarnings("rawtypes")
    public static List<FgEdge> toEdgeList(FactorGraph fg, Object item) {
        ArrayList<FgEdge> edges = new ArrayList<FgEdge>();
        if (item instanceof List) {
            for (Object elem : (List) item) {
                addEdgesOfElem(elem, fg, edges);
            }
        } else {
            addEdgesOfElem(item, fg, edges);
        }
        return edges;
    }

    private static void addEdgesOfElem(Object elem, FactorGraph fg, ArrayList<FgEdge> edges) {
        if (elem instanceof FgEdge) {
            edges.add((FgEdge) elem);
        } else if (elem instanceof GlobalFactor) {
            edges.addAll(fg.getNode((GlobalFactor) elem).getOutEdges());
        } else {
            throw new RuntimeException("Unsupported type in schedule: " + elem.getClass());
        }
    }
    
    public static List<?> toFactorEdgeList(Object item) {
        if (item instanceof List) {
            return (List<?>) item;
        } else {
            return Lists.getList(item);
        }
    }


}
