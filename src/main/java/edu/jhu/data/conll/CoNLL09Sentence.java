package edu.jhu.data.conll;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import edu.jhu.data.concrete.SimpleAnnoSentence;
import edu.jhu.data.concrete.SimpleAnnoSentenceCollection;
import edu.jhu.data.conll.SrlGraph.SrlArg;
import edu.jhu.data.conll.SrlGraph.SrlEdge;
import edu.jhu.data.conll.SrlGraph.SrlPred;

/**
 * One sentence from a CoNLL-2009 formatted file.
 * @author mmitchell
 */
public class CoNLL09Sentence implements Iterable<CoNLL09Token> {

    public static final Pattern dash = Pattern.compile("-");

    private static final Logger log = Logger.getLogger(CoNLL09Sentence.class);
    
    private ArrayList<CoNLL09Token> tokens;
    
    public CoNLL09Sentence(List<CoNLL09Token> tokens) {
        this.tokens = new ArrayList<CoNLL09Token>(tokens);
    }

    /** Deep copy constructor. */
    public CoNLL09Sentence(CoNLL09Sentence sent) {
        this.tokens = new ArrayList<CoNLL09Token>(sent.tokens.size());
        for (CoNLL09Token tok : sent) {
            this.tokens.add(new CoNLL09Token(tok));
        }
    }


    /*public CoNLL09Sentence(SimpleAnnoSentence simpleSent) {
        // public CoNLL09Token(int id, String form, String lemma, String plemma,
        // String pos, String ppos, List<String> feat, List<String> pfeat,
        // int head, int phead, String deprel, String pdeprel,
        // boolean fillpred, String pred, List<String> apreds) 
        // tokens = new ArrayList<CoNLL09Token>();
        for (int i = 0; i< simpleSent.size(); i++) {
          String form = simpleSent.getWord(i);
          String lemma = simpleSent.getLemma(i);
          String posTag = simpleSent.getPosTag(i);
          List<String> feat = simpleSent.getFeats(i);
          int head = simpleSent.getParent(i) + 1;
          String deprel = simpleSent.getDeprel(i);  
          boolean fillpred = false;
          String pred = null;
          List<String> apreds = null;
          tokens.add(new CoNLL09Token(i+1, form, lemma, lemma, posTag, posTag, feat, feat, head, head, deprel, deprel, fillpred, pred, apreds));
      }
    }
    */

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
     * Returns my internal representation of the parent index for each token.
     * The wall has index -1.
     */
    public int[] getParentsFromPhead() {
        int[] pparents = new int[size()];
        for (int i = 0; i < pparents.length; i++) {
            pparents[i] = tokens.get(i).getPhead() - 1;
        }
        return pparents;
    }

    /**
     * Converts internal representation back to
     * CoNLL09 format.
     */
    public void setPheadsFromParents(int[] parents) {
        for (int i = 0; i < parents.length; i++) {
            tokens.get(i).setPhead(parents[i] + 1);
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

    public List<String> getPlemmas() {
        List<String> plemmas = new ArrayList<String>(size());
        for (int i=0; i<size(); i++) {
            plemmas.add(tokens.get(i).getPlemma());            
        }
        return plemmas;
    }
    
    public List<String> getPosTags() {
        List<String> posTags = new ArrayList<String>(size());
        for (int i=0; i<size(); i++) {
            posTags.add(tokens.get(i).getPos());            
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
    
    public ArrayList<List<String>> getFeats() {
        ArrayList<List<String>> feats = new ArrayList<List<String>>(size());
        for (int i=0; i<size(); i++) {
            feats.add(tokens.get(i).getFeat());            
        }
        return feats;
    }
        
    public ArrayList<List<String>> getPfeats() {
        ArrayList<List<String>> pfeats = new ArrayList<List<String>>(size());
        for (int i=0; i<size(); i++) {
            pfeats.add(tokens.get(i).getPfeat());            
        }
        return pfeats;
    }
        
    public List<String> getDeprels() {
        List<String> deprels = new ArrayList<String>();
        for (int i=0; i<size(); i++) {
            deprels.add(tokens.get(i).getDeprel());            
        }
        return deprels;
    }

    public List<String> getPdeprels() {
        List<String> pdeprels =  new ArrayList<String>();
        for (int i=0; i<size(); i++) {
            pdeprels.add(tokens.get(i).getPdeprel());            
        }
        return pdeprels;
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
            CoNLL09Token tok = tokens.get(i);
            SrlPred pred = srlGraph.getPredAt(i);
            if (pred == null) {
                tok.setPred(null);
                if (warnMismatchedPreds && tok.isFillpred()) {
                    log.warn("Not setting predicate sense on a row where FILLPRED=Y in original data.");
                }
                if (setFillPred) {                    
                    tok.setFillpred(false);
                }
            } else {
                tok.setPred(pred.getLabel());
                if (warnMismatchedPreds && !tok.isFillpred()) {
                    log.warn("Setting predicate sense on a row where FILLPRED=_ in original data.");
                }
                if (setFillPred) {
                    tok.setFillpred(true);
                }
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
    
    // --------------- Data munging --------------- //
    
    public void normalizeRoleNames() {
        for (CoNLL09Token tok : tokens) {
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
    
    public void removeDepTrees() {
        for (CoNLL09Token tok : tokens) {
            tok.setPhead(0);
            tok.setHead(0);
            tok.setDeprel("_");
            tok.setPdeprel("_");
        }    
    }
    
    public void removeDepLabels() {
        for (CoNLL09Token tok : tokens) {
            tok.setDeprel("_");
            tok.setPdeprel("_");
        }
    }
    
    // -------------------------------------- //

    public void intern() {
        for (CoNLL09Token tok : this) {
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

    public SimpleAnnoSentence toSimpleAnnoSentence(boolean useGoldSyntax) {
        return toSimpleAnnoSentence(this, useGoldSyntax);
    }
    
    public static SimpleAnnoSentence toSimpleAnnoSentence(CoNLL09Sentence cos, boolean useGoldSyntax) {
        SimpleAnnoSentence s = new SimpleAnnoSentence();
        s.setSourceSent(cos);
        s.setWords(cos.getWords());
        s.setSrlGraph(cos.getSrlGraph());
        if (useGoldSyntax) {
            s.setLemmas(cos.getLemmas());
            s.setParents(cos.getParentsFromHead());
            s.setPosTags(cos.getPosTags());
            s.setFeats(cos.getFeats());
            s.setDeprels(cos.getDeprels());
        } else {
            s.setLemmas(cos.getPlemmas());
            s.setParents(cos.getParentsFromPhead());
            s.setPosTags(cos.getPposTags());
            s.setFeats(cos.getPfeats());
            s.setDeprels(cos.getPdeprels());
        }
        return s;
    }

    public static CoNLL09Sentence fromSimpleAnnoSentence(SimpleAnnoSentence sent) {
        if(sent.getSourceSent() == null || !(sent.getSourceSent() instanceof CoNLL09Sentence)) {
            // TODO: Implement this case.
            throw new RuntimeException("The case where the source sentence is not given is not yet implemented.");
        }
        // This gets a copy of the source sentence, and so is not destructive.
        CoNLL09Sentence updatedSentence = new CoNLL09Sentence((CoNLL09Sentence) sent.getSourceSent());
        for (int i = 0; i < updatedSentence.size(); i++) {
            CoNLL09Token tok = updatedSentence.get(i);
            tok.setPlemma(sent.getLemma(i));
            tok.setPpos(sent.getPosTag(i));
            tok.setPfeat(sent.getFeats(i));
            tok.setPhead(sent.getParent(i) + 1);
            tok.setPdeprel(sent.getDeprel(i));
        }
        return updatedSentence;
    }
    
    public static SimpleAnnoSentenceCollection toSimpleAnno(Iterable<CoNLL09Sentence> conllSents, boolean useGoldSyntax) {
        SimpleAnnoSentenceCollection sents = new SimpleAnnoSentenceCollection();
        for (CoNLL09Sentence sent : conllSents) {
            sents.add(sent.toSimpleAnnoSentence(useGoldSyntax));
        }
        return sents;
    }

}
