package edu.jhu.parse.cky;

import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import edu.jhu.util.Alphabet;

public class GrammarConstants {
    
    private static final Logger log = Logger.getLogger(GrammarConstants.class);

    private static final String NULL_ELEMENT_TAG = "-NONE-";
    
    public static boolean isBinarized(String symbolStr) {
        return symbolStr.charAt(0) == '@';
    }
    
    public static String getBinarizedTag(String tag) {
        return "@" + tag;
    }

    // Hard-coded to PTB null element.
    public static String getNullElementTag() {
        return NULL_ELEMENT_TAG;
    }

    // Hard-coded to PTB function tags.
    /**
     * This regular expression matches tries to match one or more functional tags, with an
     * optional trace following, at the end of a string.
     */
    private static final Pattern functionTag = Pattern.compile("(-[A-Z]+)+(?=([-=][\\d]+)?$)");
    public static String removeFunctionTag(String tag) {
        return functionTag.matcher(tag).replaceAll("");
    }
    
    private static final Pattern trace = Pattern.compile("[-=][\\d]+$");
    public static String removeTagTrace(String tag) {
        return trace.matcher(tag).replaceAll("");
    }

    // Hard-coded to Berkeley grammar refinement format.
    private static final Pattern refine = Pattern.compile("_\\d+$");
    public static String removeTagRefinements(String tag) {
        return refine.matcher(tag).replaceAll("");
    }

    // Hard-coded to Berkeley OOV signatures.
    public static String getSignature(String word, int loc, Alphabet<String> lexAlphabet) {
        log.warn("Berkeley signature builder not supported in public release");
        return word;
    }
    
}
