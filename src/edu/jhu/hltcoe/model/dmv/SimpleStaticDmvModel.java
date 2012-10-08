package edu.jhu.hltcoe.model.dmv;

import util.Alphabet;
import edu.jhu.hltcoe.data.Label;
import edu.jhu.hltcoe.data.TaggedWord;
import edu.jhu.hltcoe.data.WallDepTreeNode;

public class SimpleStaticDmvModel {

    public static final TaggedWord TW_A = new TaggedWord("a", "A");
    public static final TaggedWord TW_B = new TaggedWord("b", "B");

    public static DmvModel getTwoPosTagInstance() {
        DmvModelFactory modelFactory = new RandomDmvModelFactory(0.1);
        Alphabet<Label> alphabet = new Alphabet<Label>();
        alphabet.lookupObject(TW_A);
        alphabet.lookupObject(TW_B);
        DmvModel dmvModel = (DmvModel) modelFactory.getInstance(alphabet);

        dmvModel.fill(0.0);
        dmvModel.putRootWeight(TW_A, 0.5);
        dmvModel.putRootWeight(TW_B, 0.5);
        dmvModel.putChildWeight(TW_A, "l", TW_A, 0.5);
        dmvModel.putChildWeight(TW_A, "l", TW_B, 0.5);
        dmvModel.putChildWeight(TW_A, "r", TW_A, 1.0); // dummy param
        dmvModel.putChildWeight(TW_B, "l", TW_A, 0.5);
        dmvModel.putChildWeight(TW_B, "l", TW_B, 0.5);
        dmvModel.putChildWeight(TW_B, "r", TW_B, 1.0);
        
        dmvModel.fillStopProbs(1.0);
        dmvModel.putStopProb(TW_A, "l", true, 0.6);
        dmvModel.putStopProb(TW_A, "r", true, 0.6); 
        dmvModel.putStopProb(TW_B, "l", true, 0.6); 
        dmvModel.putStopProb(TW_B, "r", true, 0.6);
        
        dmvModel.convertRealToLog();
        dmvModel.assertNormalized(1e-13);
        return dmvModel;
    }

    public static final TaggedWord noun = new TaggedWord("Noun", "N");
    public static final TaggedWord adj = new TaggedWord("Adj", "Adj");
    public static final TaggedWord verb = new TaggedWord("Verb", "V");
    
    public static DmvModel getThreePosTagInstance() {
        DmvModelFactory modelFactory = new RandomDmvModelFactory(0.1);
        Alphabet<Label> alphabet = new Alphabet<Label>();
        alphabet.lookupObject(noun);
        alphabet.lookupObject(adj);
        alphabet.lookupObject(verb);
        DmvModel dmvModel = (DmvModel) modelFactory.getInstance(alphabet);

        dmvModel.fill(0.0);
        dmvModel.putRootWeight(verb, 1.0);
        dmvModel.putChildWeight(noun, "l", adj, 1.0);
        dmvModel.putChildWeight(noun, "r", verb, 1.0);
        dmvModel.putChildWeight(adj, "l", adj, 1.0);
        dmvModel.putChildWeight(adj, "r", adj, 1.0);
        dmvModel.putChildWeight(verb, "l", noun, 1.0);
        dmvModel.putChildWeight(verb, "r", noun, 1.0);
        
        dmvModel.fillStopProbs(1.0);
        dmvModel.putStopProb(noun, "l", true, 0.6);
        //dmvModel.putStopWeight(noun, "l", false, 0.8);
        dmvModel.putStopProb(noun, "r", true, 0.9); // Allow elephant saw cat that saw mouse
        dmvModel.putStopProb(adj, "l", true, 0.8);
        dmvModel.putStopProb(verb, "l", true, 0.0); // Always have a subject and object
        dmvModel.putStopProb(verb, "r", true, 0.0);
        
        dmvModel.convertRealToLog();
        dmvModel.assertNormalized(1e-13);
        return dmvModel;
    }
    
}
