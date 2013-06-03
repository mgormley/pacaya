package edu.jhu.hltcoe.model.dmv;

import edu.jhu.hltcoe.util.Alphabet;
import edu.jhu.hltcoe.data.Label;
import edu.jhu.hltcoe.data.TaggedWord;
import edu.jhu.hltcoe.model.dmv.DmvModel.Lr;

public class SimpleStaticDmvModel {

    public static final TaggedWord TW_A = new TaggedWord("a", "A");
    public static final TaggedWord TW_B = new TaggedWord("b", "B");

    public static DmvModel getTwoPosTagInstance() {
        DmvModelFactory modelFactory = new UniformDmvModelFactory();
        Alphabet<Label> alphabet = new Alphabet<Label>();
        alphabet.lookupIndex(TW_A);
        alphabet.lookupIndex(TW_B);
        DmvModel dmvModel = (DmvModel) modelFactory.getInstance(alphabet);

        dmvModel.fill(0.0);
        dmvModel.putRootWeight(TW_A, 0.5);
        dmvModel.putRootWeight(TW_B, 0.5);
        dmvModel.putChildWeight(TW_A, Lr.LEFT, TW_A, 0.5);
        dmvModel.putChildWeight(TW_A, Lr.LEFT, TW_B, 0.5);
        dmvModel.putChildWeight(TW_A, Lr.RIGHT, TW_A, 1.0); // dummy param
        dmvModel.putChildWeight(TW_B, Lr.LEFT, TW_A, 0.5);
        dmvModel.putChildWeight(TW_B, Lr.LEFT, TW_B, 0.5);
        dmvModel.putChildWeight(TW_B, Lr.RIGHT, TW_B, 1.0);
        
        dmvModel.fillStopProbs(1.0);
        dmvModel.putStopProb(TW_A, Lr.LEFT, true, 0.6);
        dmvModel.putStopProb(TW_A, Lr.RIGHT, true, 0.6); 
        dmvModel.putStopProb(TW_B, Lr.LEFT, true, 0.6); 
        dmvModel.putStopProb(TW_B, Lr.RIGHT, true, 0.6);
        
        dmvModel.convertRealToLog();
        dmvModel.assertLogNormalized(1e-13);
        return dmvModel;
    }

    public static final TaggedWord noun = new TaggedWord("noun", "NOUN");
    public static final TaggedWord adj = new TaggedWord("adj", "ADJ");
    public static final TaggedWord verb = new TaggedWord("verb", "VERB");
    
    public static DmvModel getThreePosTagInstance() {
        DmvModelFactory modelFactory = new UniformDmvModelFactory();
        Alphabet<Label> alphabet = new Alphabet<Label>();
        alphabet.lookupIndex(noun);
        alphabet.lookupIndex(adj);
        alphabet.lookupIndex(verb);
        DmvModel dmvModel = (DmvModel) modelFactory.getInstance(alphabet);

        dmvModel.fill(0.0);
        dmvModel.putRootWeight(verb, 1.0);
        dmvModel.putChildWeight(noun, Lr.LEFT, adj, 1.0);
        dmvModel.putChildWeight(noun, Lr.RIGHT, verb, 1.0);
        dmvModel.putChildWeight(adj, Lr.LEFT, adj, 1.0);
        dmvModel.putChildWeight(adj, Lr.RIGHT, adj, 1.0);
        dmvModel.putChildWeight(verb, Lr.LEFT, noun, 1.0);
        dmvModel.putChildWeight(verb, Lr.RIGHT, noun, 1.0);
        
        dmvModel.fillStopProbs(1.0);
        dmvModel.putStopProb(noun, Lr.LEFT, true, 0.6);
        dmvModel.putStopProb(noun, Lr.RIGHT, true, 0.9); // Allow elephant saw cat that saw mouse
        dmvModel.putStopProb(adj, Lr.LEFT, true, 0.8);
        dmvModel.putStopProb(verb, Lr.LEFT, true, 0.0); // Always have a subject and object
        dmvModel.putStopProb(verb, Lr.RIGHT, true, 0.0);
        
        dmvModel.convertRealToLog();
        dmvModel.assertLogNormalized(1e-13);
        return dmvModel;
    }
    
    public static DmvModel getAltThreePosTagInstance() {
        DmvModelFactory modelFactory = new UniformDmvModelFactory();
        Alphabet<Label> alphabet = new Alphabet<Label>();
        alphabet.lookupIndex(noun);
        alphabet.lookupIndex(adj);
        alphabet.lookupIndex(verb);
        DmvModel dmvModel = (DmvModel) modelFactory.getInstance(alphabet);

        dmvModel.fill(0.0);
        dmvModel.putRootWeight(verb, 1.0);
        dmvModel.putChildWeight(noun, Lr.LEFT, adj, 1.0);
        dmvModel.putChildWeight(noun, Lr.RIGHT, verb, 1.0);
        dmvModel.putChildWeight(adj, Lr.LEFT, adj, 1.0);
        dmvModel.putChildWeight(adj, Lr.RIGHT, adj, 1.0);
        dmvModel.putChildWeight(verb, Lr.LEFT, noun, 1.0);
        dmvModel.putChildWeight(verb, Lr.RIGHT, noun, 1.0);
        
        dmvModel.fillStopProbs(1.0);
        dmvModel.putStopProb(noun, Lr.LEFT, true, 0.7);
        dmvModel.putStopProb(noun, Lr.RIGHT, true, 0.95); // Allow elephant saw cat that saw mouse
        dmvModel.putStopProb(adj, Lr.LEFT, true, 0.8);
        dmvModel.putStopProb(adj, Lr.RIGHT, true, 1.0);
        dmvModel.putStopProb(verb, Lr.LEFT, true, 0.3); // Always have a subject and object
        dmvModel.putStopProb(verb, Lr.RIGHT, true, 0.4);
        
        dmvModel.convertRealToLog();
        dmvModel.assertLogNormalized(1e-13);
        return dmvModel;
    }
    

    public static DmvModel getFixedStopRandChild() {
        DmvModelFactory modelFactory = new RandomDmvModelFactory(1.0);
        Alphabet<Label> alphabet = new Alphabet<Label>();
        alphabet.lookupIndex(noun);
        alphabet.lookupIndex(adj);
        alphabet.lookupIndex(verb);
        DmvModel dmvModel = (DmvModel) modelFactory.getInstance(alphabet);
        dmvModel.convertLogToReal();
        
        dmvModel.fillStopProbs(0.9);
        
        dmvModel.convertRealToLog();
        dmvModel.assertLogNormalized(1e-13);
        return dmvModel;
    }
    
}
