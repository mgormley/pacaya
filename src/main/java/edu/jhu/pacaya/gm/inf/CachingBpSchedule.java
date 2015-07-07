package edu.jhu.pacaya.gm.inf;

import java.util.ArrayList;
import java.util.List;

import edu.jhu.pacaya.autodiff.erma.ErmaBp.BpScheduleType;
import edu.jhu.pacaya.autodiff.erma.ErmaBp.BpUpdateOrder;
import edu.jhu.pacaya.gm.model.Factor;
import edu.jhu.pacaya.gm.model.FactorGraph;
import edu.jhu.pacaya.gm.model.Var;
import edu.jhu.pacaya.gm.model.globalfac.GlobalFactor;
import edu.jhu.pacaya.gm.util.BipartiteGraph;
import edu.jhu.pacaya.util.collections.QLists;

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

    public List<Object> getOrder(int iter, FactorGraph fg) {
        if (canReuseOrder && iter >= 1) {
            // Just re-use the same order.
            return order;
        }
        // Get the initial order for the edges.
        order = sched.getOrder();
        if (iter == -1) {
            // Keep only the messages from the leaves for iteration -1. Then never send these again.
            order = filterNonConstantMsgs(order, fg);
        } else {
            // Filter out the messages from the leaves.
            order = filterConstantMsgs(order, fg);
        }
        return order;
    }

    /** Filters edges from a leaf node. */
    @SuppressWarnings("unchecked")
    private static List<Object> filterConstantMsgs(List<Object> order, FactorGraph fg) {
        ArrayList<Object> filt = new ArrayList<Object>();
        for (Object item : order) {
            if (item instanceof List) {
                List<Object> items = filterConstantMsgs((List<Object>) item, fg);
                if (items.size() > 0) {
                    filt.add(items);
                }
            } else if (item instanceof Integer) {
                // If the parent node is not a leaf.
                if (!isConstantMsg((Integer) item, fg)) {
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
    private static List<Object> filterNonConstantMsgs(List<Object> order, FactorGraph fg) {
        ArrayList<Object> filt = new ArrayList<Object>();
        for (Object item : order) {
            if (item instanceof List) {
                List<Object> items = filterNonConstantMsgs((List<Object>) item, fg);
                if (items.size() > 0) {
                    filt.add(items);
                }
            } else if (item instanceof Integer) {
                // If the parent node is not a leaf.
                if (isConstantMsg((Integer) item, fg)) {
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
    public static boolean isConstantMsg(int edge, FactorGraph fg) {
        BipartiteGraph<Var,Factor> bg = fg.getBipgraph();
        int numNbs = bg.isT1T2(edge) ? bg.numNbsT1(bg.parentE(edge)) : bg.numNbsT2(bg.parentE(edge)); 
        return numNbs == 1;
    }
    
    @SuppressWarnings("rawtypes")
    public static List<Integer> toEdgeList(FactorGraph fg, Object item) {
        ArrayList<Integer> edges = new ArrayList<>();
        if (item instanceof List) {
            for (Object elem : (List) item) {
                addEdgesOfElem(elem, fg, edges);
            }
        } else {
            addEdgesOfElem(item, fg, edges);
        }
        return edges;
    }

    private static void addEdgesOfElem(Object elem, FactorGraph fg, ArrayList<Integer> edges) {
        if (elem instanceof Integer) {
            edges.add((Integer) elem);
        } else if (elem instanceof GlobalFactor) {
            // Add all the outgoing edges from the global factor.
            BipartiteGraph<Var,Factor> bg = fg.getBipgraph();
            int a = ((GlobalFactor) elem).getId();
            for (int nb=0; nb<bg.numNbsT2(a); nb++) {
                edges.add(bg.edgeT2(a, nb));
            }
        } else {
            throw new RuntimeException("Unsupported type in schedule: " + elem.getClass());
        }
    }
    
    public static List<?> toFactorEdgeList(Object item) {
        if (item instanceof List) {
            return (List<?>) item;
        } else {
            return QLists.getList(item);
        }
    }


}
