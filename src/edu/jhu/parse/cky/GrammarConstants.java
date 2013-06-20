package edu.jhu.parse.cky;

import java.util.regex.Pattern;

import edu.berkeley.nlp.PCFGLA.smoothing.BerkeleySignatureBuilder;
import edu.jhu.data.Label;
import edu.jhu.data.Tag;
import edu.jhu.data.Word;
import edu.jhu.util.Alphabet;

public class GrammarConstants {
    
    private static final Tag NULL_ELEMENT_TAG = new Tag("-NONE-");
    private static int unknownLevel = 5;
    
    public static boolean isBinarized(String symbolStr) {
        return symbolStr.charAt(0) == '@';
    }
    
    public static Tag getBinarizedTag(Tag tag) {
        return new Tag("@" + tag.getTag());
    }

    // Hard-coded to PTB null element.
    public static Tag getNullElement() {
        return NULL_ELEMENT_TAG;
    }

    // Hard-coded to PTB function tags.
    /**
     * This regular expression matches tries to match one or more functional tags, with an
     * optional trace following, at the end of a string.
     */
    private static final Pattern functionTag = Pattern.compile("(-[A-Z]+)+(?=([-=][\\d]+)?$)");
    public static Tag removeFunctionTag(Tag tag) {
        String pStr = tag.getLabel();
        pStr = functionTag.matcher(pStr).replaceAll("");
        return new Tag(pStr);
    }
    
    private static final Pattern trace = Pattern.compile("[-=][\\d]+$");
    public static Tag removeTrace(Tag tag) {
        String pStr = tag.getLabel();
        pStr = trace.matcher(pStr).replaceAll("");
        return new Tag(pStr);
    }

    // Hard-coded to Berkeley grammar refinement format.
    private static final Pattern refine = Pattern.compile("_\\d+$");
    public static Tag removeRefinements(Tag tag) {
        String pStr = tag.getLabel();
        pStr = refine.matcher(pStr).replaceAll("");
        return new Tag(pStr);
    }

    // Hard-coded to Berkeley OOV signatures.
    public static Word getSignature(Word word, int loc, Alphabet<Label> lexAlphabet) {
        BerkeleySignatureBuilder bsb = new BerkeleySignatureBuilder(lexAlphabet);
        Word signature = bsb.getSignature(word, loc, unknownLevel);
        return signature;
    }
    
}
