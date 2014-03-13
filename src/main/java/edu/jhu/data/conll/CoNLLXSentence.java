package edu.jhu.data.conll;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import edu.jhu.data.Sentence;
import edu.jhu.data.simple.SimpleAnnoSentence;

/**
 * One sentence from a CoNLL-X formatted file.
 */
public class CoNLLXSentence implements Iterable<CoNLLXToken> {

    private ArrayList<CoNLLXToken> tokens;

    public CoNLLXSentence(List<CoNLLXToken> tokens) {
        this.tokens = new ArrayList<CoNLLXToken>(tokens);
    }
    
    public CoNLLXSentence(ArrayList<String> sentLines) {
        tokens = new ArrayList<CoNLLXToken>();
        for (String line : sentLines) {
            tokens.add(new CoNLLXToken(line));
        }
    }

    @Deprecated
    public CoNLLXSentence(Sentence sent, int[] heads) {
        tokens = new ArrayList<CoNLLXToken>();
        for (int i=0; i<sent.size(); i++) {
            String label = sent.get(i);
            // TODO: Here we just add the label as the tag and word.
            tokens.add(new CoNLLXToken(i+1, label, label, label, label, null, heads[i], "NO_LABEL", -1, null));
        }
    }

    /** Deep copy constructor. */
    public CoNLLXSentence(CoNLLXSentence sent) {
        tokens = new ArrayList<CoNLLXToken>(sent.tokens.size());
        for (CoNLLXToken tok : sent) {
            tokens.add(new CoNLLXToken(tok));
        }
    }    
    
    public CoNLLXToken get(int i) {
        return tokens.get(i);
    }
    
    public int size() {
        return tokens.size();
    }
    
    @Override
    public Iterator<CoNLLXToken> iterator() {
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

    /**
     * Converts internal representation back to
     * CoNLL09 format.
     */
    public void setHeadsFromParents(int[] parents) {
        for (int i=0; i<parents.length; i++) {
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
    
    public List<String> getCposTags() {
        List<String> pposTags = new ArrayList<String>(size());
        for (int i=0; i<size(); i++) {
            pposTags.add(tokens.get(i).getCposTag());            
        }
        return pposTags;
    }
    
    public List<String> getPosTags() {
        List<String> posTags = new ArrayList<String>(size());
        for (int i=0; i<size(); i++) {
            posTags.add(tokens.get(i).getPosTag());            
        }
        return posTags;
    }
    
    public ArrayList<List<String>> getFeats() {
        ArrayList<List<String>> feats = new ArrayList<List<String>>(size());
        for (int i=0; i<size(); i++) {
            feats.add(tokens.get(i).getFeats());            
        }
        return feats;
    }
        
    public List<String> getDeprels() {
        List<String> deprels = new ArrayList<String>();
        for (int i=0; i<size(); i++) {
            deprels.add(tokens.get(i).getDepRel());            
        }
        return deprels;
    }

    public List<String> getPdeprels() {
        List<String> pdeprels =  new ArrayList<String>();
        for (int i=0; i<size(); i++) {
            pdeprels.add(tokens.get(i).getPDepRel());            
        }
        return pdeprels;
    }
    
    // -------------------------------------- //

    public void intern() {
        for (CoNLLXToken tok : this) {
            tok.intern();
        }
    }
    
    public SimpleAnnoSentence toSimpleAnnoSentence(boolean usePhead) {
        return toSimpleAnnoSentence(this, usePhead);
    }
    
    public static SimpleAnnoSentence toSimpleAnnoSentence(CoNLLXSentence cos, boolean usePhead) {
        SimpleAnnoSentence s = new SimpleAnnoSentence();
        s.setSourceSent(cos);
        s.setWords(cos.getWords());
        s.setLemmas(cos.getLemmas());
        s.setFeats(cos.getFeats());

        s.setCposTags(cos.getCposTags());
        s.setPosTags(cos.getPosTags());
        
        if (usePhead) {
            s.setParents(cos.getParentsFromPhead());
            s.setDeprels(cos.getPdeprels());
        } else {            
            s.setParents(cos.getParentsFromHead());
            s.setDeprels(cos.getDeprels());
        }
        
        return s;
    }

    /**
     * Creates a new CoNLLXSentence with all columns set for each field.
     */
    public static CoNLLXSentence fromSimpleAnnoSentence(SimpleAnnoSentence sent) {
        // Get the tokens for this sentence.
        List<CoNLLXToken> toks = new ArrayList<CoNLLXToken>();
        for (int i = 0; i < sent.size(); i++) {
            CoNLLXToken tok = new CoNLLXToken();
            tok.setId(i+1);
            tok.setForm(sent.getWord(i));            
            tok.setLemma(sent.getLemma(i));
            tok.setCpostag(sent.getCposTag(i));
            tok.setPostag(sent.getPosTag(i));
            // TODO: Check for nulls here!!!
            if (sent.getFeats() != null) { tok.setFeats(sent.getFeats(i)); }
            tok.setHead(sent.getParent(i) + 1);
            tok.setPhead(sent.getParent(i) + 1);
            tok.setDeprel(sent.getDeprel(i));
            tok.setPdeprel(sent.getDeprel(i));
            
            toks.add(tok);
        }
        // Create the new sentence.
        CoNLLXSentence updatedSentence = new CoNLLXSentence(toks);
        return updatedSentence;
    }
    
}