package edu.jhu.data.conll;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import edu.jhu.data.conll.SrlGraph.SrlArg;
import edu.jhu.data.conll.SrlGraph.SrlEdge;
import edu.jhu.data.conll.SrlGraph.SrlPred;
import edu.jhu.data.simple.SimpleAnnoSentence;
import edu.jhu.data.simple.SimpleAnnoSentenceCollection;

/**
 * One sentence from a CoNLL-2009 formatted file.
 * @author mmitchell
 */
public class CoNLL08Sentence implements Iterable<CoNLL08Token> {

    public static final Pattern dash = Pattern.compile("-");

    private static final Logger log = Logger.getLogger(CoNLL08Sentence.class);
    
    private ArrayList<CoNLL08Token> tokens;
    
    public CoNLL08Sentence(List<CoNLL08Token> tokens) {
        this.tokens = new ArrayList<CoNLL08Token>(tokens);
    }

    /** Deep copy constructor. */
    public CoNLL08Sentence(CoNLL08Sentence sent) {
        this.tokens = new ArrayList<CoNLL08Token>(sent.tokens.size());
        for (CoNLL08Token tok : sent) {
            this.tokens.add(new CoNLL08Token(tok));
        }
    }

    public static CoNLL08Sentence getInstanceFromTokenStrings(ArrayList<String> sentLines) {
        List<CoNLL08Token> tokens = new ArrayList<CoNLL08Token>();
        for (String line : sentLines) {
            tokens.add(new CoNLL08Token(line));
        }
        return new CoNLL08Sentence(tokens);
    }
    
    public CoNLL08Token get(int i) {
        return tokens.get(i);
    }

    public int size() {
        return tokens.size();
    }

    @Override
    public Iterator<CoNLL08Token> iterator() {
        return tokens.iterator();
    }
    
    /*
     * ------------------ Slow, but convenient accessors. ---------------------
     * Most of these getters take O(n) time to construct a list of strings. The
     * preferred way to access this class is by looping over the tokens, these
     * provide a convenient alternative.
     */
    
    /**
     * Returns my internal representation of the parent index for each token.
     * The wall has index -1.
     */
    public int[] getParentsFromHead() {       
        int[] parents = new int[size()];
        for (int i = 0; i < parents.length; i++) {
            parents[i] = tokens.get(i).getHead() - 1;
        } 
        return parents;
    }
    
    /**
     * Converts internal representation back to
     * CoNLL08 format.
     */
    public void setHeadsFromParents(int[] parents) {
        for (int i = 0; i < parents.length; i++) {
            tokens.get(i).setHead(parents[i] + 1);
        }
    }


    public List<String> getWords() {
        List<String> words = new ArrayList<String>(size());
        for (int i=0; i<size(); i++) {
            words.add(tokens.get(i).getForm());            
        }
        return words;
    }
    
    public List<String> getLemmas() {
        List<String> lemmas = new ArrayList<String>(size());
        for (int i=0; i<size(); i++) {
            lemmas.add(tokens.get(i).getLemma());            
        }
        return lemmas;
    }
    
    public List<String> getGposTags() {
        List<String> posTags = new ArrayList<String>(size());
        for (int i=0; i<size(); i++) {
            posTags.add(tokens.get(i).getGpos());            
        }
        return posTags;
    }
    
    public List<String> getPposTags() {
        List<String> pposTags = new ArrayList<String>(size());
        for (int i=0; i<size(); i++) {
            pposTags.add(tokens.get(i).getPpos());            
        }
        return pposTags;
    }
    
    public List<String> getSplitWords() {
        List<String> words = new ArrayList<String>(size());
        for (int i=0; i<size(); i++) {
            words.add(tokens.get(i).getSplitForm());            
        }
        return words;
    }
    
    public List<String> getSplitLemmas() {
        List<String> lemmas = new ArrayList<String>(size());
        for (int i=0; i<size(); i++) {
            lemmas.add(tokens.get(i).getSplitLemma());            
        }
        return lemmas;
    }
    
    public List<String> getSplitPposTags() {
        List<String> pposTags = new ArrayList<String>(size());
        for (int i=0; i<size(); i++) {
            pposTags.add(tokens.get(i).getSplitPpos());            
        }
        return pposTags;
    }
        
    public List<String> getDeprels() {
        List<String> deprels = new ArrayList<String>();
        for (int i=0; i<size(); i++) {
            deprels.add(tokens.get(i).getDeprel());            
        }
        return deprels;
    }

    public SrlGraph getSrlGraph() {
        return new SrlGraph(this);
    }

    public void setPredApredFromSrlGraph(SrlGraph srlGraph, boolean warnMismatchedPreds) {
        setColsFromSrlGraph(srlGraph, warnMismatchedPreds, false);
    }
    
    public void setColsFromSrlGraph(SrlGraph srlGraph, boolean warnMismatchedPreds, boolean setFillPred) {
        int numPreds = srlGraph.getNumPreds();
        // Set the FILLPRED and PRED column.
        for (int i=0; i<size(); i++) {
            CoNLL08Token tok = tokens.get(i);
            SrlPred pred = srlGraph.getPredAt(i);
            if (pred == null) {
                tok.setPred(null);
            } else {
                tok.setPred(pred.getLabel());
            }
        }
        
        // Set the APRED columns.
        for (int i=0; i<size(); i++) {
            CoNLL08Token tok = tokens.get(i);
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
    
    // --------------- Data munging --------------- //
    
    public void normalizeRoleNames() {
        for (CoNLL08Token tok : tokens) {
            ArrayList<String> apreds = new ArrayList<String>();
            for (String apred : tok.getApreds()) {
                if ("_".equals(apred)) {
                    apreds.add(apred);
                } else { 
                    apreds.add(normalizeRoleName(apred));
                }
            }
            tok.setApreds(apreds);
        }
    }
    
    private static String normalizeRoleName(String role) {
        String[] splitRole = dash.split(role);
        return splitRole[0].toLowerCase();
    }
    
    // -------------------------------------- //

    public void intern() {
        for (CoNLL08Token tok : this) {
            tok.intern();
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
        CoNLL08Sentence other = (CoNLL08Sentence) obj;
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
            CoNLL08Writer writer = new CoNLL08Writer(new OutputStreamWriter(baos));
            writer.write(this);
            writer.close();
            return baos.toString("UTF-8");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public SimpleAnnoSentence toSimpleAnnoSentence(boolean useGoldSyntax, boolean useSplitForms) {
        return toSimpleAnnoSentence(this, useGoldSyntax, useSplitForms);
    }
    
    public static SimpleAnnoSentence toSimpleAnnoSentence(CoNLL08Sentence cos, boolean useGoldSyntax, boolean useSplitForms) {
        SimpleAnnoSentence s = new SimpleAnnoSentence();
        s.setSourceSent(cos);
        s.setSrlGraph(cos.getSrlGraph());

        if (useSplitForms) {
            s.setWords(cos.getSplitWords());
            s.setLemmas(cos.getSplitLemmas());
        } else {
            s.setWords(cos.getWords());
            s.setLemmas(cos.getLemmas());
        }

        if (useGoldSyntax) {
            if (useSplitForms) {
                s.setPosTags(cos.getSplitPposTags());
            } else {
                s.setPosTags(cos.getGposTags());
            }
            s.setParents(cos.getParentsFromHead());
            s.setDeprels(cos.getDeprels());
        } else {
            if (useSplitForms) {
                s.setPosTags(cos.getSplitPposTags());
            } else {
                s.setPosTags(cos.getPposTags());
            }
        }
        return s;
    }

    /**
     * Creates a new CoNLL08Sentence with both columns set for each field
     * (i.e. LEMMA and SPLIT_LEMMA are both set from the values on the
     * SimpleAnnoSentence).
     */
    public static CoNLL08Sentence fromSimpleAnnoSentence(SimpleAnnoSentence sent) {
        // Get the tokens for this sentence.
        List<CoNLL08Token> toks = new ArrayList<CoNLL08Token>();
        for (int i = 0; i < sent.size(); i++) {
            CoNLL08Token tok = new CoNLL08Token();
            tok.setId(i+1);
            tok.setForm(sent.getWord(i));
            tok.setLemma(sent.getLemma(i));
            tok.setGpos(sent.getPosTag(i));
            tok.setPpos(sent.getPosTag(i));
            tok.setSplitForm(sent.getWord(i));
            tok.setSplitLemma(sent.getLemma(i));
            tok.setSplitPpos(sent.getPosTag(i));
            tok.setHead(sent.getParent(i) + 1);
            tok.setDeprel(sent.getDeprel(i));            
            toks.add(tok);
        }
        
        // Create the new sentence.
        CoNLL08Sentence updatedSentence = new CoNLL08Sentence(toks);
        
        // Update SRL columns from the SRL graph.
        updatedSentence.setColsFromSrlGraph(sent.getSrlGraph(), false, true);
        
        return updatedSentence;
    }
    
    public static SimpleAnnoSentenceCollection toSimpleAnno(Iterable<CoNLL08Sentence> conllSents, boolean useGoldSyntax, boolean useSplitForms) {
        SimpleAnnoSentenceCollection sents = new SimpleAnnoSentenceCollection();
        for (CoNLL08Sentence sent : conllSents) {
            sents.add(sent.toSimpleAnnoSentence(useGoldSyntax, useSplitForms));
        }
        return sents;
    }

}
