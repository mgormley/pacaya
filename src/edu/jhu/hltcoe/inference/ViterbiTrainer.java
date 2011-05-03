package edu.jhu.hltcoe.inference;

import org.apache.log4j.Logger;
import org.jboss.dna.common.statistic.Stopwatch;

import edu.jhu.hltcoe.data.DepTree;
import edu.jhu.hltcoe.data.DepTreebank;
import edu.jhu.hltcoe.data.Sentence;
import edu.jhu.hltcoe.data.SentenceCollection;
import edu.jhu.hltcoe.model.Model;
import edu.jhu.hltcoe.model.ModelFactory;
import edu.jhu.hltcoe.parse.IlpViterbiParser;
import edu.jhu.hltcoe.parse.ViterbiParser;

public class ViterbiTrainer implements Trainer {

    private static Logger log = Logger.getLogger(ViterbiTrainer.class);
    private ViterbiParser parser;
    private EMTrainer<DepTreebank> emTrainer;
    
    public ViterbiTrainer(ViterbiParser parser, MStep<DepTreebank> mStep, ModelFactory modelFactory, int iterations) {
        this.parser = parser;
        emTrainer = new EMTrainer<DepTreebank>(new ViterbiEStep(), mStep, modelFactory, iterations);
    }
    
    @Override
    public void train(SentenceCollection sentences) {
        emTrainer.train(sentences);
    }

    private class ViterbiEStep implements EStep<DepTreebank> {

        @Override
        public DepTreebank getCounts(SentenceCollection sentences, Model model) {
            Stopwatch stopwatch = new Stopwatch();
            DepTreebank treebank = new DepTreebank();
            for (Sentence sentence: sentences) {
                stopwatch.start();
                DepTree tree = parser.getViterbiParse(sentence, model);
                stopwatch.stop();
                treebank.add(tree);
                log.debug(String.format("Avg parse time: %.3f Num sents: %d", 
                        stopwatch.getAverageDuration().getDurationInMilliseconds(), 
                        stopwatch.getCount()));
            }
            return treebank;
        }
        
    }
    
}
