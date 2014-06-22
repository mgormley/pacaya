package edu.jhu.data.conll;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import edu.jhu.data.conll.SrlGraph.SrlArg;
import edu.jhu.data.conll.SrlGraph.SrlEdge;
import edu.jhu.data.conll.SrlGraph.SrlPred;

/**
 * One sentence from a CoNLL-2009 formatted file.
 */
public class ConllLiteSentence implements Iterable<ConllLiteToken> {

    private static Logger log = Logger.getLogger(ConllLiteSentence.class);
    
    private ArrayList<ConllLiteToken> tokens;
    
    public ConllLiteSentence(List<ConllLiteToken> tokens) {
        this.tokens = new ArrayList<ConllLiteToken>(tokens);
    }

    /** Deep copy constructor. */
    public ConllLiteSentence(ConllLiteSentence sent) {
        tokens = new ArrayList<ConllLiteToken>(sent.tokens.size());
        for (ConllLiteToken tok : sent) {
            tokens.add(new ConllLiteToken(tok));
        }
    }

    public static ConllLiteSentence getInstanceFromTokenStrings(ArrayList<String> sentLines) {
        List<ConllLiteToken> tokens = new ArrayList<ConllLiteToken>();
        for (String line : sentLines) {
            tokens.add(new ConllLiteToken(line));
        }
        return new ConllLiteSentence(tokens);
    }
    
    public ConllLiteToken get(int i) {
        return tokens.get(i);
    }

    public int size() {
        return tokens.size();
    }

    @Override
    public Iterator<ConllLiteToken> iterator() {
        return tokens.iterator();
    }

    public Map<String,Integer> getIdToPositionMap() {
        Map<String, Integer> map = new HashMap<String, Integer>();
        for (int i=0; i<this.size(); i++) {
            map.put(this.get(i).getId(), i);
        }
        return map;
    }
    
    public SrlGraph getSrlGraph() {
        Map<String, Integer> map = getIdToPositionMap();
        SrlGraph srlGraph = new SrlGraph(this.size());
        for (int i=0; i<this.size(); i++) {
            List<String[]> edges = this.get(i).getEdges();
            for (String[] e : edges) {
                SrlArg arg = srlGraph.getArgAt(i);
                if (arg == null) {
                    arg = new SrlArg(i);
                }
                int j = map.get(e[0]);
                SrlPred pred = srlGraph.getPredAt(j);
                if (pred == null) {
                    pred = new SrlPred(j, "NO.SENSE");
                }
                srlGraph.addEdge(new SrlEdge(pred, arg, e[1]));
            }
        }
        return srlGraph;
    }

    public void setEdgesFromSrlGraph(SrlGraph srlGraph) {
        Map<Integer, SrlArg> posToArgMap = srlGraph.getPositionArgMap();
        
        for (int i=0; i<this.size(); i++) {
            List<String[]> edges = new ArrayList<String[]>();

            SrlArg arg = posToArgMap.get(i);
            if (arg != null) {
                for (SrlEdge edge : arg.getEdges()) {
                    String[] e = new String[] { Integer.toString(edge.getPred().getId()), edge.getLabel()};
                    edges.add(e);
                }
            }
            this.get(i).setEdges(edges);
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((tokens == null) ? 0 : tokens.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ConllLiteSentence other = (ConllLiteSentence) obj;
        if (tokens == null) {
            if (other.tokens != null)
                return false;
        } else if (!tokens.equals(other.tokens))
            return false;
        return true;
    }    
 
    public String toString() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ConllLiteWriter writer = new ConllLiteWriter(new OutputStreamWriter(baos));
            writer.write(this);
            writer.close();
            return baos.toString("UTF-8");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
