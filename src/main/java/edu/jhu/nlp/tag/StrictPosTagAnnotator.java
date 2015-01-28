package edu.jhu.nlp.tag;

import java.util.ArrayList;
import java.util.Set;

import edu.jhu.nlp.AbstractParallelAnnotator;
import edu.jhu.nlp.Annotator;
import edu.jhu.nlp.Trainable;
import edu.jhu.nlp.data.simple.AnnoSentence;
import edu.jhu.nlp.data.simple.AnnoSentenceCollection;
import edu.jhu.nlp.features.TemplateLanguage.AT;
import edu.jhu.util.collections.Sets;

/**
 * Converts the POS tags or coarse POS tags to a very small enumerated (strict) POS tag set.
 * 
 * @author mgormley
 */
public class StrictPosTagAnnotator extends AbstractParallelAnnotator implements Annotator, Trainable {

    // There should be at most 8 of these "coarsest" tags.
    public enum StrictPosTag { VERB, NOUN, PUNC, CONJ, OTHER }; 
    
    private static final long serialVersionUID = 1L;
    private boolean annoTrainGold;
    
    public StrictPosTagAnnotator() {
        this(false);
    }
    
    public StrictPosTagAnnotator(boolean annoTrainGold) {
        this.annoTrainGold = annoTrainGold;
    }

    public void annotate(AnnoSentence sent) {
        addStrictPosTags(sent);
    }
    
    public static void addStrictPosTags(AnnoSentenceCollection sents) {
        (new StrictPosTagAnnotator()).annotate(sents);
    }
    
    public static void addStrictPosTags(AnnoSentence sent) {
        ArrayList<StrictPosTag> strictTags = new ArrayList<>(sent.size());
        for (int i=0; i<sent.size(); i++) {
            String pos = sent.hasAt(AT.POS) ? sent.getPosTag(i) : null; 
            String cpos = sent.hasAt(AT.CPOS) ? sent.getCposTag(i) : null;
            strictTags.add(getStrictPosTag(pos, cpos));
        }
        sent.setStrictPosTags(strictTags);
    }

    private static StrictPosTag getStrictPosTag(String pos, String cpos) {
        if (pos == null && cpos == null) {
            throw new IllegalArgumentException("Unable to add strict POS tags without the POS or coarse POS tags.");
        }
        if (cpos != null) {
            // These will match the universal POS tags from Petrov et al. (2012).
            if (cpos.equals("VERB")) {
                return StrictPosTag.VERB;
            } else if (cpos.equals("NOUN")) {
                return StrictPosTag.NOUN;
            } else if (cpos.equals(".")) {
                return StrictPosTag.PUNC;
            } else if (cpos.equals("CONJ")) {
                return StrictPosTag.CONJ;
            }
        }
        if (pos != null) {
            // This approach is taken used in Martins et al. (2013) TurboParser, which
            // took the idea from EGSTRA (http://groups.csail.mit.edu/nlp/egstra/).
            if (pos.startsWith("v") || pos.startsWith("V")) {
                return StrictPosTag.VERB;
            } else if (pos.startsWith("n") || pos.startsWith("N")) {
                return StrictPosTag.NOUN;
            } else if (pos.equals("Punc") ||
                      pos.equals("$,") ||
                      pos.equals("$.") ||
                      pos.equals("PUNC") ||
                      pos.equals("punc") ||
                      pos.equals("F") ||
                      pos.equals("IK") ||
                      pos.equals("XP") ||
                      pos.equals(",") ||
                      pos.equals(";")) {
                return StrictPosTag.PUNC;
            } else if (pos.equals("Conj") ||
                      pos.equals("KON") ||
                      pos.equals("conj") ||
                      pos.equals("Conjunction") ||
                      pos.equals("CC") ||
                      pos.equals("cc")) {
             return StrictPosTag.CONJ;
           }
        }
        return StrictPosTag.OTHER;
    } 

    @Override
    public void train(AnnoSentenceCollection trainInput, AnnoSentenceCollection trainGold,
            AnnoSentenceCollection devInput, AnnoSentenceCollection devGold) {
        if (annoTrainGold) {
            addStrictPosTags(trainGold);
        }
    }

    @Override
    public Set<AT> getAnnoTypes() {
        return Sets.getSet(AT.STRICT_POS);
    }

}
