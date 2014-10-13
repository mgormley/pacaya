package edu.jhu.nlp.data.conll;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import edu.jhu.nlp.data.conll.SrlGraph.SrlArg;
import edu.jhu.nlp.data.conll.SrlGraph.SrlEdge;
import edu.jhu.nlp.data.conll.SrlGraph.SrlPred;
import edu.jhu.nlp.data.simple.AnnoSentence;
import edu.jhu.nlp.data.simple.AnnoSentenceCollection;

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


    /*public CoNLL09Sentence(AnnoSentence simpleSent) {
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
        if (srlGraph == null) {
            // There are no predicates.
            for (int i=0; i<size(); i++) {
                CoNLL09Token tok = tokens.get(i);
                if (warnMismatchedPreds && tok.isFillpred()) {
                    log.warn("Not setting predicate sense on a row where FILLPRED=Y in original data.");
                }
                if (setFillPred) {
                    tok.setFillpred(false);
                }
                tok.setPred(null);
                List<String> emptyList = Collections.emptyList();
                tok.setApreds(emptyList);
            }
            return;
        }
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
    
    public void removeHeadAndPhead() {
        for (CoNLL09Token tok : tokens) {
            tok.setPhead(0);
            tok.setHead(0);
        }    
    }
    
    public void removeDeprealAndPdeprel() {
        for (CoNLL09Token tok : tokens) {
            tok.setDeprel("_");
            tok.setPdeprel("_");
        }
    }

    public void removeLemmaAndPlemma() {
        for (CoNLL09Token tok : tokens) {
            tok.setLemma("_");
            tok.setPlemma("_");
        }
    }
    
    public void removeFeatAndPfeat() {
        List<String> emptyList = Collections.emptyList();
        for (CoNLL09Token tok : tokens) {
            tok.setFeat(emptyList);
            tok.setPfeat(emptyList);
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

    public AnnoSentence toAnnoSentence(boolean useGoldSyntax) {
        return toAnnoSentence(this, useGoldSyntax);
    }
    
    public static AnnoSentence toAnnoSentence(CoNLL09Sentence cos, boolean useGoldSyntax) {
        AnnoSentence s = new AnnoSentence();
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

    /**
     * Creates a new CoNLL09Sentence with both columns set for each field
     * (i.e. PLEMMA and LEMMA are both set from the values on the
     * AnnoSentence). The reason for setting both is that the CoNLL-2009
     * evaluation script uses the "gold" columns for evaluation, but we might
     * want to utilize the predictions in some downstream task.
     */
    public static CoNLL09Sentence fromAnnoSentence(AnnoSentence sent) {
        // Get the tokens for this sentence.
        List<CoNLL09Token> toks = new ArrayList<CoNLL09Token>();
        for (int i = 0; i < sent.size(); i++) {
            CoNLL09Token tok = new CoNLL09Token();
            tok.setId(i+1);
            tok.setForm(sent.getWord(i));
            
            // Set "predicted" columns.
            if (sent.getLemmas() != null) { tok.setPlemma(sent.getLemma(i)); }
            if (sent.getPosTags() != null) { tok.setPpos(sent.getPosTag(i)); }            
            if (sent.getFeats() != null) { tok.setPfeat(sent.getFeats(i)); }
            if (sent.getParents() != null) { tok.setPhead(sent.getParent(i) + 1); }
            if (sent.getDeprels() != null) { tok.setPdeprel(sent.getDeprel(i)); }
            // Set "gold" columns.
            if (sent.getLemmas() != null) { tok.setLemma(sent.getLemma(i)); }
            if (sent.getPosTags() != null) { tok.setPos(sent.getPosTag(i)); }
            if (sent.getFeats() != null) { tok.setFeat(sent.getFeats(i)); }
            if (sent.getParents() != null) { tok.setHead(sent.getParent(i) + 1); }
            if (sent.getDeprels() != null) { tok.setDeprel(sent.getDeprel(i)); }
            
            toks.add(tok);
        }
        
        // Create the new sentence.
        CoNLL09Sentence updatedSentence = new CoNLL09Sentence(toks);
        
        // Update SRL columns from the SRL graph.
        // (This correctly handles null SRL graphs.)
        updatedSentence.setColsFromSrlGraph(sent.getSrlGraph(), false, true);
        
        return updatedSentence;
    }
    
    public static AnnoSentenceCollection toAnno(Iterable<CoNLL09Sentence> conllSents, boolean useGoldSyntax) {
        AnnoSentenceCollection sents = new AnnoSentenceCollection();
        for (CoNLL09Sentence sent : conllSents) {
            sents.add(sent.toAnnoSentence(useGoldSyntax));
        }
        return sents;
    }

}
