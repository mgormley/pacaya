package edu.jhu.data.conll;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A graph representing SRL annotations on a sentence.
 * 
 * @author mgormley
 * @author mmitchell
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
    public static class SrlArg extends SrlNode {
        
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
     * @author mmitchell
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

        public int getId() {
            // ... Right? =/
            return position+1;
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

    // Predicate array, indexed by position of predicate.
    private SrlPred[] predArr;    
    // Argument array, indexed by position of predicate.
    private SrlArg[] argArr;
    // Sentence length.
    private int n;
    
    public SrlGraph(int n) {
        this.n = n;
        this.preds = new ArrayList<SrlPred>();
        this.args = new ArrayList<SrlArg>();
        this.edges = new ArrayList<SrlEdge>();
        this.predArr = new SrlPred[n];
        this.argArr = new SrlArg[n];
    }
        
    public SrlGraph(CoNLL09Sentence sent) {
        this.n = sent.size();
        this.preds = new ArrayList<SrlPred>();
        this.args = new ArrayList<SrlArg>();
        this.edges = new ArrayList<SrlEdge>();
        this.predArr = new SrlPred[n];
        this.argArr = new SrlArg[n];
        
        // Create a predicate for each row marked with fillpred = Y.
        for (int i = 0; i < sent.size(); i++) {
            CoNLL09Token tok = sent.get(i);
            if (tok.isFillpred()) {
                addPred(new SrlPred(i, tok.getPred()));
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
                    addEdge(edge);
                    if (!argAdded) {
                        // Add the argument.
                        addArg(arg);
                        argAdded = true;
                    }
                }
            }
        }
    }

    /** Adds the edge. */
    public void addEdge(SrlEdge edge) {
        edges.add(edge);
        addPred(edge.getPred());
        addArg(edge.getArg());
    }

    /** Adds the pred if it hasn't already been added. */
    public void addPred(SrlPred pred) {
        if (predArr[pred.getPosition()] != null) {
            if (predArr[pred.getPosition()] != pred) {
                throw new IllegalArgumentException("Cannot add multiple preds at the same position.");
            } else {
                // Already added.
                return;
            }
        }
        preds.add(pred);
        predArr[pred.getPosition()] = pred;
    }
    
    /** Adds the arg if it hasn't already been added. */
    public void addArg(SrlArg arg) {
        if (argArr[arg.getPosition()] != null) {
            if (argArr[arg.getPosition()] != arg) {
                throw new IllegalArgumentException("Cannot add multiple args at the same position.");
            } else {
                // Already added.
                return;
            }
        }
        args.add(arg);
        argArr[arg.getPosition()] = arg;
    }

    public SrlPred getPredAt(int position) {
        return predArr[position];
    }
    
    public SrlArg getArgAt(int position) {
        return argArr[position];
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

    /**
     * Creates a new mapping of argument positions to arguments.
     */
    public Map<Integer, SrlArg> getPositionArgMap() {
        HashMap<Integer, SrlArg> map = new HashMap<Integer, SrlArg>();
        for (SrlArg arg : args) {
            map.put(arg.getPosition(), arg);
        }
        return map;        
    }

    public int getNumPreds() {
        return preds.size();
    }    
}
