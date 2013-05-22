package edu.jhu.hltcoe.parse.cky;

import java.util.regex.Pattern;

public class GrammarConstants {

    //private static final Pattern binarizedRegex = Pattern.compile("^@");
    
    public static boolean isBinarized(String symbolStr) {
        return symbolStr.charAt(0) == '@';
    }


}
