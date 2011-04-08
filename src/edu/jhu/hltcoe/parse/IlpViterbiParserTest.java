package edu.jhu.hltcoe.parse;

import org.junit.Test;

import edu.jhu.hltcoe.data.DepTree;
import edu.jhu.hltcoe.data.Sentence;
import edu.jhu.hltcoe.data.SentenceCollection;
import edu.jhu.hltcoe.data.Word;
import edu.jhu.hltcoe.model.DmvModelFactory;
import edu.jhu.hltcoe.model.Model;
import edu.jhu.hltcoe.model.ModelFactory;
import edu.jhu.hltcoe.model.DmvModelFactory.RandomWeightGenerator;


public class IlpViterbiParserTest {

    @Test
    public void testSimple() {
        SentenceCollection sentences = new SentenceCollection();
        sentences.add(getSentenceFromString("the cat ate the hat with the mouse"));
        ModelFactory modelFactory = new DmvModelFactory(new RandomWeightGenerator());
        Model model = modelFactory.getInstance(sentences);
        // projective parsing
        IlpViterbiParser parser = new IlpViterbiParser(true);
        for (Sentence sentence : sentences) {
            DepTree depTree = parser.getViterbiParse(sentence, model);
            System.out.println(depTree);
        }
        // non-projective parsing
        parser = new IlpViterbiParser(false);
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

