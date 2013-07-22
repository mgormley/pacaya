package edu.berkeley.nlp.PCFGLA.smoothing;

import java.util.HashSet;
import java.util.Set;

import edu.jhu.data.Label;
import edu.jhu.data.Word;
import edu.jhu.util.Alphabet;


/**
 * Constructs signatures for unknown words as in the berkeley parser.
 * 
 * The method getSignature() was copied from edu.berkeley.nlp.PCFGLA.smoothing.SophisticatedLexicon.
 * 
 * @author mgormley
 * @author mmitchell
 */
public class BerkeleySignatureBuilder {

    private Alphabet<Label> lexAlphabet;

    public BerkeleySignatureBuilder(Alphabet<Label> lexAlphabet) {
        this.lexAlphabet = new Alphabet<Label>(lexAlphabet);
        this.lexAlphabet.stopGrowth();
    }

    private boolean isKnown(String lowered) {
        return lexAlphabet.lookupIndex(new Word(lowered)) != -1;
    }
    
    public Word getSignature(Word wordLabel, int loc, int unknownLevel) {
        String word = wordLabel.getLabel();
        return new Word(getSignature(word, loc, unknownLevel));
    }
   
    public Set<String> getSimpleUnkFeatures(String word, int loc, String language) {
        int unknownLevel;
        if (language.equals("en")) {
            unknownLevel = 5;
        } else {
            unknownLevel = 6;
        }
        Set<String> simpleUnkFeatures = new HashSet<String>();
        switch (unknownLevel) {
            case 6: {
                String[] reflexiveEndings = {"se", "me", "te", "le", "la", "lo"};
                String[] verbEndings = {"iéramos", "áramos", "ábamas", "íamos", "isteis",
                    "ierais", "asteis", "íais", "yendo", "uelto", "ierto", "ieron", "ieras",
                    "ieran", "arais", "abais", "ído", "ías", "ían", "éis", "áis", "iste", 
                    "imos", "iera", "iedo", "emos", "aste", "aron", "aras", "aran", "ando", 
                    "amos", "abas", "aban", "ía", "ás", "án", "ió", "ido", "cho", "ara", "ado", "aba"};
                int wlen = word.length();
                int numCaps = 0;
                boolean hasDigit = false;
                boolean hasDash = false;
                boolean hasLower = false;
                boolean hasRefl = false;
                boolean hasVerb = false;
                for (int i = 0; i < wlen; i++) {
                    char ch = word.charAt(i);
                    if (Character.isDigit(ch)) {
                        hasDigit = true;
                    } else if (ch == '-') {
                        hasDash = true;
                    } else if (Character.isLetter(ch)) {
                        if (Character.isLowerCase(ch)) {
                            hasLower = true;
                        } else if (Character.isTitleCase(ch)) {
                            hasLower = true;
                            numCaps++;
                        } else {
                            numCaps++;
                        }
                    }
                }
                char ch0 = word.charAt(0);
                String lowered = word.toLowerCase();
                if (Character.isUpperCase(ch0) || Character.isTitleCase(ch0)) {
                    if (loc == 0 && numCaps == 1) {
                        simpleUnkFeatures.add("_INITC");
                    } else {
                        simpleUnkFeatures.add("_CAPS");
                    }
                } else if (!Character.isLetter(ch0) && numCaps > 0) {
                    simpleUnkFeatures.add("_CAPS");
                } else if (hasLower) { // (Character.isLowerCase(ch0)) {
                    simpleUnkFeatures.add("_LOWER");
                }
                if (hasDigit) {
                    simpleUnkFeatures.add("_DIGIT");
                }
                if (hasDash) {
                    simpleUnkFeatures.add("_DASH");
                }
                
                // Getting into more language-specific stuff.            
                if (lowered.endsWith("s") && wlen >= 3 && !hasVerb) {
                    // here length 3, so you don't miss out on ones like 80s
                } else if (word.length() >= 5 && !hasDash
                        && !(hasDigit && numCaps > 0)) {
                    // don't do for very short words;
                    // Implement common discriminating suffixes
                    // Reflexive endings
                    String ending = lowered.length() > 2 ? lowered.substring(lowered.length() - 2) : lowered;
                    for (String reflEnding : reflexiveEndings) {
                        if (reflEnding.equals(ending)) {
                            hasRefl = true;
                            break;
                        }
                        
                    }
                    if (hasRefl) {
                        ending = lowered.length() > 4 ? lowered.substring(lowered.length() - 4,lowered.length() - 2) : lowered;
                    }
                    // Verb endings
                    for (String verbEnding : verbEndings) {
                        if (verbEnding.equals(ending)) {
                            hasVerb = true;
                            simpleUnkFeatures.add("_VERB");
                            break;
                        }
                    }
                }
                break;
            }
            // case 5 TBD.
        }        
        return simpleUnkFeatures;
    }
    
    //Overloaded version of below, to start adding "language" as an option.
    public String getSignature(String word, int loc, String language) {
        int unknownLevel;
        if (language.equals("en")) {
            unknownLevel = 5;
        } else {
            unknownLevel = 6;
        }
        return getSignature(word, loc, unknownLevel);
    }
    /**
     * This routine returns a String that is the "signature" of the class of a
     * word. For, example, it might represent whether it is a number of ends in
     * -s. The strings returned by convention match the pattern UNK-.* , which
     * is just assumed to not match any real word. Behavior depends on the
     * unknownLevel (-uwm flag) passed in to the class. The recognized numbers
     * are 1-5: 5 is fairly English-specific; 4, 3, and 2 look for various word
     * features (digits, dashes, etc.) which are only vaguely English-specific;
     * 1 uses the last two characters combined with a simple classification by
     * capitalization.
     * MM, 12.July.2013:  Added unknownLevel '6', for Spanish.
     * 
     * @param word
     *            The word to make a signature for
     * @param loc
     *            Its position in the sentence (mainly so sentence-initial
     *            capitalized words can be treated differently)
     * @return A String that is its signature (equivalence class)
     */
    public String getSignature(String word, int loc, int unknownLevel) {
        // int unknownLevel = Options.get().useUnknownWordSignatures;
        StringBuffer sb = new StringBuffer("UNK");

        if (word.length() == 0)
            return sb.toString();

        switch (unknownLevel) {

        case 6: {
            String[] reflexiveEndings = {"se", "me", "te", "le", "la", "lo"};
            String[] verbEndings = {"iéramos", "áramos", "ábamas", "íamos", "isteis",
                "ierais", "asteis", "íais", "yendo", "uelto", "ierto", "ieron", "ieras",
                "ieran", "arais", "abais", "ído", "ías", "ían", "éis", "áis", "iste", 
                "imos", "iera", "iedo", "emos", "aste", "aron", "aras", "aran", "ando", 
                "amos", "abas", "aban", "ía", "ás", "án", "ió", "ido", "cho", "ara", "ado", "aba"};
            int wlen = word.length();
            int numCaps = 0;
            boolean hasDigit = false;
            boolean hasDash = false;
            boolean hasLower = false;
            boolean hasRefl = false;
            boolean hasVerb = false;
            for (int i = 0; i < wlen; i++) {
                char ch = word.charAt(i);
                if (Character.isDigit(ch)) {
                    hasDigit = true;
                } else if (ch == '-') {
                    hasDash = true;
                } else if (Character.isLetter(ch)) {
                    if (Character.isLowerCase(ch)) {
                        hasLower = true;
                    } else if (Character.isTitleCase(ch)) {
                        hasLower = true;
                        numCaps++;
                    } else {
                        numCaps++;
                    }
                }
            }
            char ch0 = word.charAt(0);
            String lowered = word.toLowerCase();
            if (Character.isUpperCase(ch0) || Character.isTitleCase(ch0)) {
                if (loc == 0 && numCaps == 1) {
                    sb.append("-INITC");
                    if (isKnown(lowered)) {
                        sb.append("-KNOWNLC");
                    }
                } else {
                    sb.append("-CAPS");
                }
            } else if (!Character.isLetter(ch0) && numCaps > 0) {
                sb.append("-CAPS");
            } else if (hasLower) { // (Character.isLowerCase(ch0)) {
                sb.append("-LC");
            }
            if (hasDigit) {
                sb.append("-NUM");
            }
            if (hasDash) {
                sb.append("-DASH");
            }
            
            // Getting into more language-specific stuff.            
            if (lowered.endsWith("s") && wlen >= 3 && !hasVerb) {
                // here length 3, so you don't miss out on ones like 80s
                    sb.append("-s");
            } else if (word.length() >= 5 && !hasDash
                    && !(hasDigit && numCaps > 0)) {
                // don't do for very short words;
                // Implement common discriminating suffixes
                // Reflexive endings
                String ending = lowered.length() > 2 ? lowered.substring(lowered.length() - 2) : lowered;
                for (String reflEnding : reflexiveEndings) {
                    if (reflEnding.equals(ending)) {
                        hasRefl = true;
                        break;
                    }
                    
                }
                if (hasRefl) {
                    ending = lowered.length() > 4 ? lowered.substring(lowered.length() - 4,lowered.length() - 2) : lowered;
                }
                // Verb endings
                for (String verbEnding : verbEndings) {
                    if (verbEnding.equals(ending)) {
                        hasVerb = true;
                        sb.append("-VERB");
                        break;
                    }
                }
            }
            break;
        }
        
        case 5: {
            // Reformed Mar 2004 (cdm); hopefully much better now.
            // { -CAPS, -INITC ap, -LC lowercase, 0 } +
            // { -KNOWNLC, 0 } + [only for INITC]
            // { -NUM, 0 } +
            // { -DASH, 0 } +
            // { -last lowered char(s) if known discriminating suffix, 0}
            int wlen = word.length();
            int numCaps = 0;
            boolean hasDigit = false;
            boolean hasDash = false;
            boolean hasLower = false;
            for (int i = 0; i < wlen; i++) {
                char ch = word.charAt(i);
                if (Character.isDigit(ch)) {
                    hasDigit = true;
                } else if (ch == '-') {
                    hasDash = true;
                } else if (Character.isLetter(ch)) {
                    if (Character.isLowerCase(ch)) {
                        hasLower = true;
                    } else if (Character.isTitleCase(ch)) {
                        hasLower = true;
                        numCaps++;
                    } else {
                        numCaps++;
                    }
                }
            }
            char ch0 = word.charAt(0);
            String lowered = word.toLowerCase();
            if (Character.isUpperCase(ch0) || Character.isTitleCase(ch0)) {
                if (loc == 0 && numCaps == 1) {
                    sb.append("-INITC");
                    if (isKnown(lowered)) {
                        sb.append("-KNOWNLC");
                    }
                } else {
                    sb.append("-CAPS");
                }
            } else if (!Character.isLetter(ch0) && numCaps > 0) {
                sb.append("-CAPS");
            } else if (hasLower) { // (Character.isLowerCase(ch0)) {
                sb.append("-LC");
            }
            if (hasDigit) {
                sb.append("-NUM");
            }
            if (hasDash) {
                sb.append("-DASH");
            }
            if (lowered.endsWith("s") && wlen >= 3) {
                // here length 3, so you don't miss out on ones like 80s
                char ch2 = lowered.charAt(wlen - 2);
                // not -ess suffixes or greek/latin -us, -is
                if (ch2 != 's' && ch2 != 'i' && ch2 != 'u') {
                    sb.append("-s");
                }
            } else if (word.length() >= 5 && !hasDash
                    && !(hasDigit && numCaps > 0)) {
                // don't do for very short words;
                // Implement common discriminating suffixes
                /*
                 * if (Corpus.myLanguage==Corpus.GERMAN){
                 * sb.append(lowered.substring(lowered.length()-1)); }else{
                 */
                if (lowered.endsWith("ed")) {
                    sb.append("-ed");
                } else if (lowered.endsWith("ing")) {
                    sb.append("-ing");
                } else if (lowered.endsWith("ion")) {
                    sb.append("-ion");
                } else if (lowered.endsWith("er")) {
                    sb.append("-er");
                } else if (lowered.endsWith("est")) {
                    sb.append("-est");
                } else if (lowered.endsWith("ly")) {
                    sb.append("-ly");
                } else if (lowered.endsWith("ity")) {
                    sb.append("-ity");
                } else if (lowered.endsWith("y")) {
                    sb.append("-y");
                } else if (lowered.endsWith("al")) {
                    sb.append("-al");
                    // } else if (lowered.endsWith("ble")) {
                    // sb.append("-ble");
                    // } else if (lowered.endsWith("e")) {
                    // sb.append("-e");
                }
            }
            break;
        }

        case 4: {
            boolean hasDigit = false;
            boolean hasNonDigit = false;
            boolean hasLetter = false;
            boolean hasLower = false;
            boolean hasDash = false;
            boolean hasPeriod = false;
            boolean hasComma = false;
            for (int i = 0; i < word.length(); i++) {
                char ch = word.charAt(i);
                if (Character.isDigit(ch)) {
                    hasDigit = true;
                } else {
                    hasNonDigit = true;
                    if (Character.isLetter(ch)) {
                        hasLetter = true;
                        if (Character.isLowerCase(ch)
                                || Character.isTitleCase(ch)) {
                            hasLower = true;
                        }
                    } else {
                        if (ch == '-') {
                            hasDash = true;
                        } else if (ch == '.') {
                            hasPeriod = true;
                        } else if (ch == ',') {
                            hasComma = true;
                        }
                    }
                }
            }
            // 6 way on letters
            if (Character.isUpperCase(word.charAt(0))
                    || Character.isTitleCase(word.charAt(0))) {
                if (!hasLower) {
                    sb.append("-AC");
                } else if (loc == 0) {
                    sb.append("-SC");
                } else {
                    sb.append("-C");
                }
            } else if (hasLower) {
                sb.append("-L");
            } else if (hasLetter) {
                sb.append("-U");
            } else {
                // no letter
                sb.append("-S");
            }
            // 3 way on number
            if (hasDigit && !hasNonDigit) {
                sb.append("-N");
            } else if (hasDigit) {
                sb.append("-n");
            }
            // binary on period, dash, comma
            if (hasDash) {
                sb.append("-H");
            }
            if (hasPeriod) {
                sb.append("-P");
            }
            if (hasComma) {
                sb.append("-C");
            }
            if (word.length() > 3) {
                // don't do for very short words: "yes" isn't an "-es" word
                // try doing to lower for further densening and skipping digits
                char ch = word.charAt(word.length() - 1);
                if (Character.isLetter(ch)) {
                    sb.append("-");
                    sb.append(Character.toLowerCase(ch));
                }
            }
            break;
        }

        case 3: {
            // This basically works right, except note that 'S' is applied to
            // all
            // capitalized letters in first word of sentence, not just first....
            sb.append("-");
            char lastClass = '-'; // i.e., nothing
            char newClass;
            int num = 0;
            for (int i = 0; i < word.length(); i++) {
                char ch = word.charAt(i);
                if (Character.isUpperCase(ch) || Character.isTitleCase(ch)) {
                    if (loc == 0) {
                        newClass = 'S';
                    } else {
                        newClass = 'L';
                    }
                } else if (Character.isLetter(ch)) {
                    newClass = 'l';
                } else if (Character.isDigit(ch)) {
                    newClass = 'd';
                } else if (ch == '-') {
                    newClass = 'h';
                } else if (ch == '.') {
                    newClass = 'p';
                } else {
                    newClass = 's';
                }
                if (newClass != lastClass) {
                    lastClass = newClass;
                    sb.append(lastClass);
                    num = 1;
                } else {
                    if (num < 2) {
                        sb.append('+');
                    }
                    num++;
                }
            }
            if (word.length() > 3) {
                // don't do for very short words: "yes" isn't an "-es" word
                // try doing to lower for further densening and skipping digits
                char ch = Character.toLowerCase(word.charAt(word.length() - 1));
                sb.append('-');
                sb.append(ch);
            }
            break;
        }

        case 2: {
            // {-ALLC, -INIT, -UC, -LC, zero} +
            // {-DASH, zero} +
            // {-NUM, -DIG, zero} +
            // {lowerLastChar, zeroIfShort}
            boolean hasDigit = false;
            boolean hasNonDigit = false;
            boolean hasLower = false;
            for (int i = 0; i < word.length(); i++) {
                char ch = word.charAt(i);
                if (Character.isDigit(ch)) {
                    hasDigit = true;
                } else {
                    hasNonDigit = true;
                    if (Character.isLetter(ch)) {
                        if (Character.isLowerCase(ch)
                                || Character.isTitleCase(ch)) {
                            hasLower = true;
                        }
                    }
                }
            }
            if (Character.isUpperCase(word.charAt(0))
                    || Character.isTitleCase(word.charAt(0))) {
                if (!hasLower) {
                    sb.append("-ALLC");
                } else if (loc == 0) {
                    sb.append("-INIT");
                } else {
                    sb.append("-UC");
                }
            } else if (hasLower) { // if (Character.isLowerCase(word.charAt(0)))
                                    // {
                sb.append("-LC");
            }
            // no suffix = no (lowercase) letters
            if (word.indexOf('-') >= 0) {
                sb.append("-DASH");
            }
            if (hasDigit) {
                if (!hasNonDigit) {
                    sb.append("-NUM");
                } else {
                    sb.append("-DIG");
                }
            } else if (word.length() > 3) {
                // don't do for very short words: "yes" isn't an "-es" word
                // try doing to lower for further densening and skipping digits
                char ch = word.charAt(word.length() - 1);
                sb.append(Character.toLowerCase(ch));
            }
            // no suffix = short non-number, non-alphabetic
            break;
        }

        default:
            sb.append("-");
            sb.append(word.substring(Math.max(word.length() - 2, 0), word
                    .length()));
            sb.append("-");
            if (Character.isLowerCase(word.charAt(0))) {
                sb.append("LOWER");
            } else {
                if (Character.isUpperCase(word.charAt(0))) {
                    if (loc == 0) {
                        sb.append("INIT");
                    } else {
                        sb.append("UPPER");
                    }
                } else {
                    sb.append("OTHER");
                }
            }
        } // end switch (unknownLevel)
        // System.err.println("Summarized " + word + " to " + sb.toString());
        return sb.toString();
    } // end getSignature()
    
}
