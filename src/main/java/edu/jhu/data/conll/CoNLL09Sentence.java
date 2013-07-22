package edu.jhu.data.conll;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import edu.jhu.data.Lemma;
import edu.jhu.data.Tag;
import edu.jhu.data.Word;
import edu.jhu.data.conll.SrlGraph.SrlArg;
import edu.jhu.data.conll.SrlGraph.SrlEdge;
import edu.jhu.data.conll.SrlGraph.SrlPred;

/**
 * One sentence from a CoNLL-2009 formatted file.
 */
public class CoNLL09Sentence implements Iterable<CoNLL09Token> {

    private ArrayList<CoNLL09Token> tokens;
    
    public CoNLL09Sentence(List<CoNLL09Token> tokens) {
        this.tokens = new ArrayList<CoNLL09Token>(tokens);
    }

//    public CoNLL09Sentence(Sentence sent, int[] heads) {
//        tokens = new ArrayList<CoNLL09Token>();
//        for (int i=0; i<sent.size(); i++) {
//            Label label = sent.get(i);
//            TaggedWord tw = (TaggedWord) label;
//            tokens.add(new CoNLL09Token(i+1, tw.getWord(), tw.getWord(), tw.getTag(), tw.getTag(), null, heads[i], "NO_LABEL", null, null));
//        }
//    }

    /** Deep copy constructor. */
    public CoNLL09Sentence(CoNLL09Sentence sent) {
        tokens = new ArrayList<CoNLL09Token>(sent.tokens.size());
        for (CoNLL09Token tok : sent) {
            tokens.add(new CoNLL09Token(tok));
        }
    }

    public static CoNLL09Sentence getInstanceFromTokenStrings(ArrayList<String> sentLines) {
        List<CoNLL09Token> tokens = new ArrayList<CoNLL09Token>();
        for (String line : sentLines) {
            tokens.add(new CoNLL09Token(line));
        }
        return new CoNLL09Sentence(tokens);
    }
    
    public CoNLL09Token get(int i) {
        return tokens.get(i);
    }

    public int size() {
        return tokens.size();
    }

    @Override
    public Iterator<CoNLL09Token> iterator() {
        return tokens.iterator();
    }

    /**
     * Returns the head value for each token. The wall has index 0.
     * 
     * @return
     */
    public int[] getHeads() {
        int[] heads = new int[size()];
        for (int i = 0; i < heads.length; i++) {
            heads[i] = tokens.get(i).getHead();
        }
        return heads;
    }

    /**
     * Returns my internal reprensentation of the parent index for each token.
     * The wall has index -1.
     * 
     * @return
     */
    public int[] getParents() {
        int[] parents = new int[size()];
        for (int i = 0; i < parents.length; i++) {
            parents[i] = tokens.get(i).getHead() - 1;
        }
        return parents;
    }

    public List<Word> getWords() {
        ArrayList<Word> words = new ArrayList<Word>(size());
        for (int i=0; i<size(); i++) {
            words.add(new Word(tokens.get(i).getForm()));            
        }
        return words;
    }
    
    public List<Lemma> getLemmas() {
        ArrayList<Lemma> words = new ArrayList<Lemma>(size());
        for (int i=0; i<size(); i++) {
            words.add(new Lemma(tokens.get(i).getLemma()));            
        }
        return words;
    }
    
    public List<Tag> getPosTags() {
        ArrayList<Tag> words = new ArrayList<Tag>(size());
        for (int i=0; i<size(); i++) {
            words.add(new Tag(tokens.get(i).getPos()));            
        }
        return words;
    }
    
    public List<Tag> getPredictedPosTags() {
        ArrayList<Tag> words = new ArrayList<Tag>(size());
        for (int i=0; i<size(); i++) {
            words.add(new Tag(tokens.get(i).getPpos()));            
        }
        return words;
    }
    
    public void setPheadsFromParents(int[] parents) {
        for (int i = 0; i < parents.length; i++) {
            tokens.get(i).setPhead(parents[i] + 1);
        }
    }

    public SrlGraph getSrlGraph() {
        return new SrlGraph(this);
    }

    public void setColsFromSrlGraph(SrlGraph srlGraph) {
        int numPreds = srlGraph.getNumPreds();
        // Set the FILLPRED and PRED column.
        for (int i=0; i<size(); i++) {
            CoNLL09Token tok = tokens.get(i);
            SrlPred pred = srlGraph.getPredAt(i);
            if (pred == null) {
                tok.setPred(null);
                tok.setFillpred(false);   
            } else {
                tok.setPred(pred.getLabel());
                tok.setFillpred(true);
            }
        }
        // Set the APRED columns.
        for (int i=0; i<size(); i++) {
            CoNLL09Token tok = tokens.get(i);
            SrlArg arg = srlGraph.getArgAt(i);
            ArrayList<String> apreds = new ArrayList<String>();
            for (int curPred=0; curPred<numPreds; curPred++) {
                apreds.add("_");
            }
            if (arg != null) {
                int curPred = 0;
                for (int j=0; j<size(); j++) {
                    SrlPred pred = srlGraph.getPredAt(j);
                    if (pred != null) {
                        for (SrlEdge edge : arg.getEdges()) {
                            if (edge.getPred() == pred) {
                                apreds.set(curPred, edge.getLabel());
                            }
                        }
                        curPred++;
                    }
                }
            }
            tok.setApreds(apreds);
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
        CoNLL09Sentence other = (CoNLL09Sentence) obj;
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
            CoNLL09Writer writer = new CoNLL09Writer(new OutputStreamWriter(baos));
            writer.write(this);
            writer.close();
            return baos.toString("UTF-8");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
