package edu.jhu.nlp.words;

import java.util.ArrayList;

import edu.jhu.nlp.AbstractParallelAnnotator;
import edu.jhu.nlp.Annotator;
import edu.jhu.nlp.data.simple.AnnoSentence;
import edu.jhu.nlp.data.simple.AnnoSentenceCollection;
import edu.jhu.util.collections.Lists;

/**
 * Adds the 5-character prefix of each word to the sentence. These prefixes are used as features in
 * McDonald et al. (2006) for dependency parsing.
 * 
 * @author mgormley
 */
public class PrefixAnnotator extends AbstractParallelAnnotator implements Annotator {
    
    public static void addPrefixes(AnnoSentenceCollection sents) {
        (new PrefixAnnotator()).annotate(sents);
    }

    public void annotate(AnnoSentence sent) {
        addPrefixes(sent);
    }
    
    public static void addPrefixes(AnnoSentence sent) {
        ArrayList<String> prefixes = new ArrayList<>();
        for (String word : sent.getWords()) {
            if (word.length() > 5) {
                prefixes.add(word.substring(0, Math.min(word.length(), 5)));
            } else {
                prefixes.add(word);
            }
        }
        Lists.intern(prefixes);
        sent.setPrefixes(prefixes);
    }

}
