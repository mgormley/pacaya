package edu.jhu.hltcoe.data.conll;

import java.util.ArrayList;
import java.util.List;

/**
 * A graph representing SRL annotations on a sentence.
 * 
 * @author mgormley
 *
 */
public class SrlGraph {
        
    /**
     * An SRL predicate.
     * 
     * @author mgormley
     *
     */
    public static class SrlPred extends SrlNode {
        private String label;
        
        public SrlPred(int position, String label) {
            super(position);
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        @Override
        public String toString() {
            return "SrlPred [label=" + label + ", position=" + position + "]";
        }        
    }

    /**
     * An argument to an SRL predicate.
     * 
     * @author mgormley
     *
     */
    static class SrlArg extends SrlNode {

        public SrlArg(int position) {
            super(position);
        }

        @Override
        public String toString() {
            return "SrlArg [position=" + position + "]";
        }

    }

    /** 
     * A node in an SRL Graph.
     * 
     * @author mgormley
     *
     */
    private static class SrlNode {
        protected int position;
        protected List<SrlEdge> edges;

        public SrlNode(int position) {
            this.position = position;
            this.edges = new ArrayList<SrlEdge>();
        }
        
        void add(SrlEdge edge) {
            edges.add(edge);
        }

        public int getPosition() {
            return position;
        }

        public List<SrlEdge> getEdges() {
            return edges;
        }
    }    

    /**
     * A labeled edge between an SRL predicate and argument.
     * 
     * @author mgormley
     * 
     */
    public static class SrlEdge {
        private SrlPred pred;
        private SrlArg arg;
        private String label;
        public SrlEdge(SrlPred pred, SrlArg arg, String label) {
            this.pred = pred;
            this.arg = arg;
            this.label = label;
            
            pred.add(this);
            arg.add(this);
        }
        public SrlPred getPred() {
            return pred;
        }
        public SrlArg getArg() {
            return arg;
        }
        public String getLabel() {
            return label;
        }
        @Override
        public String toString() {
            return "SrlEdge [pred=" + pred + ", arg=" + arg + ", label="
                    + label + "]";
        }        
    }
    
    private List<SrlPred> preds;
    private List<SrlArg> args;
    private List<SrlEdge> edges;
    
    public SrlGraph(CoNLL09Sentence sent) {
        this.preds = new ArrayList<SrlPred>();
        this.args = new ArrayList<SrlArg>();
        this.edges = new ArrayList<SrlEdge>();
        
        // Create a predicate for each row marked with fillpred = Y.
        for (int i = 0; i < sent.size(); i++) {
            CoNLL09Token tok = sent.get(i);
            if (tok.isFillpred()) {
                preds.add(new SrlPred(i, tok.getPred()));
            }
        }
        // For each token, check whether it is an argument. If so, add all
        // the edges between that argument and its predicates.           
        for (int j = 0; j < sent.size(); j++) {
            CoNLL09Token tok = sent.get(j);
            List<String> apreds = tok.getApreds();
            // Create an argument for this position, we will only add it if
            // it is actually annotated as an argument.
            SrlArg arg = new SrlArg(j);
            boolean argAdded = false;
            assert apreds.size() == preds.size();
            for (int i = 0; i < apreds.size(); i++) {
                String apred = apreds.get(i);
                if (apred != null && !"_".equals(apred)) {
                    SrlPred pred = preds.get(i);
                    // Add the edge.
                    SrlEdge edge = new SrlEdge(pred, arg, apred);
                    edges.add(edge);
                    if (!argAdded) {
                        // Add the argument.
                        args.add(arg);
                        argAdded = true;
                    }
                }
            }
        }
    }

    public List<SrlPred> getPreds() {
        return preds;
    }

    public List<SrlArg> getArgs() {
        return args;
    }

    public List<SrlEdge> getEdges() {
        return edges;
    }

    @Override
    public String toString() {
        return "SrlGraph [preds=" + preds + ", args=" + args + ", edges="
                + edges + "]";
    }    
    
}