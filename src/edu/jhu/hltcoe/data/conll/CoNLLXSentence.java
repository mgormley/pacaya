package edu.jhu.hltcoe.data.conll;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import edu.jhu.hltcoe.data.Label;
import edu.jhu.hltcoe.data.Sentence;
import edu.jhu.hltcoe.data.TaggedWord;

/**
 * One sentence from a CoNNL-X formatted file.
 */
public class CoNLLXSentence implements Iterable<CoNLLXToken> {

    private ArrayList<CoNLLXToken> tokens;
    
    public CoNLLXSentence(ArrayList<String> sentLines) {
        tokens = new ArrayList<CoNLLXToken>();
        for (String line : sentLines) {
            tokens.add(new CoNLLXToken(line));
        }
    }

    public CoNLLXSentence(Sentence sent, int[] heads) {
        tokens = new ArrayList<CoNLLXToken>();
        for (int i=0; i<sent.size(); i++) {
            Label label = sent.get(i);
            TaggedWord tw = (TaggedWord) label;
            tokens.add(new CoNLLXToken(i+1, tw.getWord(), tw.getWord(), tw.getTag(), tw.getTag(), null, heads[i], "NO_LABEL", null, null));
        }
    }

    public static CoNLLXSentence readCoNLLXSentence(BufferedReader reader) throws IOException {
        // The current token.
        String line;
        // The tokens for one sentence.
        ArrayList<String> tokens = new ArrayList<String>();
        
        while ((line = reader.readLine()) != null) {
            if (line.equals("")) {
                // End of sentence marker.
                return new CoNLLXSentence(tokens);
            } else {
                // Regular token.
                tokens.add(line);
            }
        }
        // Could not read a full sentence.
        return null;
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

    /**
     * Returns the head value for each token. The wall has index 0.
     * @return
     */
    public int[] getHeads() {
        int[] heads = new int[size()];
        for (int i=0; i<heads.length; i++) {
            heads[i] = tokens.get(i).getHead();
        }
        return heads;
    }
    
    /**
     * Returns my internal reprensentation of the parent index for each token. The wall has index -1.
     * @return
     */
    public int[] getParents() {
        int[] parents = new int[size()];
        for (int i=0; i<parents.length; i++) {
            parents[i] = tokens.get(i).getHead() - 1;
        }
        return parents;
    }
}