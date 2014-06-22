package edu.jhu.gm.feat;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

import edu.jhu.util.Alphabet;


/**
 * Constructs signatures for unknown words. Customized for SRL and Spanish. This is currently
 * unsupported in the public release due to licensing restrictions.
 * 
 * @author mgormley
 * @author mmitchell
 */
public class SrlSignatureBuilder implements Serializable {

    private static final Logger log = Logger.getLogger(SrlSignatureBuilder.class);
    private static final long serialVersionUID = 7489745353488039306L;
    private Alphabet<String> lexAlphabet;

    public SrlSignatureBuilder(Alphabet<String> lexAlphabet) {
        this.lexAlphabet = new Alphabet<String>(lexAlphabet);
        this.lexAlphabet.stopGrowth();
    }

    private boolean isKnown(String lowered) {
        return lexAlphabet.lookupIndex(lowered) != -1;
    }
   
    public Set<String> getSimpleUnkFeatures(String word, int loc, String language) {        
        Set<String> simpleUnkFeatures = new HashSet<String>();
        simpleUnkFeatures.add(word);
        return simpleUnkFeatures;
    }
    
    //Overloaded version of below, to start adding "language" as an option.
    public String getSignature(String word, int loc, String language) {
        return getSignature(word, loc, 0);
    }

    public String getSignature(String word, int loc, int unknownLevel) {
        return word;
    }
    
}
