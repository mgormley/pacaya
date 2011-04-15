package edu.jhu.hltcoe.parse;

import static org.junit.Assert.*;

import org.junit.Test;

import edu.jhu.hltcoe.data.DepTree;
import edu.jhu.hltcoe.data.DepTreebank;
import edu.jhu.hltcoe.data.Sentence;
import edu.jhu.hltcoe.data.SentenceCollection;
import edu.jhu.hltcoe.data.Word;
import edu.jhu.hltcoe.model.DmvModelFactory;
import edu.jhu.hltcoe.model.Model;
import edu.jhu.hltcoe.model.ModelFactory;
import edu.jhu.hltcoe.model.DmvModelFactory.RandomWeightGenerator;
import edu.jhu.hltcoe.parse.IlpViterbiParser.IlpFormulation;

public class IlpViterbiParserTest {
    
    @Test
    public void testProjParses() {
        SentenceCollection sentences = new SentenceCollection();
        sentences.add(getSentenceFromString("the cat ate the hat with the mouse"));
        ModelFactory modelFactory = new DmvModelFactory(new RandomWeightGenerator());
        Model model = modelFactory.getInstance(sentences);
        
        // explicit projective parsing
        DepTreebank expTrees = getParses(model, sentences, IlpFormulation.EXPLICIT_PROJ);
        // DP projective parsing
        DepTreebank dpTrees = getParses(model, sentences, IlpFormulation.DP_PROJ);
        
        for (int i=0; i<dpTrees.size(); i++) {
            assertArrayEquals(expTrees.get(i).getParents(), dpTrees.get(i).getParents());
        }
    }
    
    @Test
    public void testFirstSentenceFromWsj() {
        SentenceCollection sentences = new SentenceCollection();
        //sentences.add(getSentenceFromString("NNP NNP , CD NNS JJ , MD VB DT NN IN DT JJ NN NNP CD ."));
        sentences.add(getSentenceFromString("NNP NNP , CD NNS JJ , MD VB DT NN IN DT JJ NN"));
        ModelFactory modelFactory = new DmvModelFactory(new RandomWeightGenerator());
        Model model = modelFactory.getInstance(sentences);

        // explicit projective parsing
        DepTreebank expTrees = getParses(model, sentences, IlpFormulation.EXPLICIT_PROJ);
        // DP projective parsing
        DepTreebank dpTrees = getParses(model, sentences, IlpFormulation.DP_PROJ);
        
        for (int i=0; i<dpTrees.size(); i++) {
            assertArrayEquals(expTrees.get(i).getParents(), dpTrees.get(i).getParents());
        }
    }
    
    private DepTreebank getParses(Model model, SentenceCollection sentences, IlpFormulation formulation) {
        DepTreebank trees = new DepTreebank();
        IlpViterbiParser parser = new IlpViterbiParser(formulation);
        for (Sentence sentence : sentences) {
            DepTree depTree = parser.getViterbiParse(sentence, model);
            trees.add(depTree);
            System.out.println(depTree);
        }
        return trees;
    }

    @Test
    public void testNonProj() {
        SentenceCollection sentences = new SentenceCollection();
        sentences.add(getSentenceFromString("the cat ate the hat with the mouse"));
        ModelFactory modelFactory = new DmvModelFactory(new RandomWeightGenerator());
        Model model = modelFactory.getInstance(sentences);
        // non-projective parsing
        IlpViterbiParser parser = new IlpViterbiParser(IlpFormulation.FLOW_NONPROJ);
        for (Sentence sentence : sentences) {
            DepTree depTree = parser.getViterbiParse(sentence, model);
            System.out.println(depTree);
        }
    }

    private Sentence getSentenceFromString(String string) {
        return new StringSentence(string);
    }

    private class StringSentence extends Sentence {

        public StringSentence(String string) {
            super();
            String[] splits = string.split("\\s");
            for (String tok : splits) {
                this.add(new Word(tok));
            }
        }

    }

}
