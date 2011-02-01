package edu.jhu.hltcoe.inference;

import java.util.List;

import edu.jhu.hltcoe.data.SentenceCollection;
import edu.jhu.hltcoe.model.Model;
import edu.jhu.hltcoe.parse.ViterbiParser;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.trees.Tree;

public class ViterbiTrainer implements Trainer {

    private ViterbiParser parser;
    private Model model;
    private int iterations;
    
    public ViterbiTrainer(ViterbiParser parser, Model model, int iterations) {
        this.parser = parser;
        this.model = model;
        this.iterations = iterations;
    }
    
    @Override
    public void train(SentenceCollection sentences) {
        for (int i=0; i<iterations; i++) {
            //Treebank treebank = 
        }
    }

    @Override
    public Tree getBestParse() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean parse(List<? extends HasWord> sentence) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean parse(List<? extends HasWord> sentence, String goal) {
        // TODO Auto-generated method stub
        return false;
    }

}
