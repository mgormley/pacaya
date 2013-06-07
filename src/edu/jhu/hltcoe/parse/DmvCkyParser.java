package edu.jhu.hltcoe.parse;


import org.apache.log4j.Logger;

import edu.jhu.hltcoe.data.DepTree;
import edu.jhu.hltcoe.data.DepTreebank;
import edu.jhu.hltcoe.data.Sentence;
import edu.jhu.hltcoe.data.SentenceCollection;
import edu.jhu.hltcoe.gridsearch.dmv.DmvObjective;
import edu.jhu.hltcoe.gridsearch.dmv.DmvObjective.DmvObjectivePrm;
import edu.jhu.hltcoe.gridsearch.dmv.IndexedDmvModel;
import edu.jhu.hltcoe.model.Model;
import edu.jhu.hltcoe.model.dmv.DmvModel;
import edu.jhu.hltcoe.parse.cky.DmvCkyPcfgParser;
import edu.jhu.hltcoe.train.DmvTrainCorpus;
import edu.jhu.hltcoe.util.Pair;
import edu.jhu.hltcoe.util.Timer;

public class DmvCkyParser implements DepParser {

    private static final Logger log = Logger.getLogger(DmvCkyParser.class);

    private double parseWeight;
    private DmvObjective dmvObj;
    private SentenceCollection sents;
    private DmvObjectivePrm objPrm;
    private Timer timer;

    private DmvCkyPcfgParser parser;
    
    public DmvCkyParser() {
        this(new DmvObjectivePrm());
    }
    
    public DmvCkyParser(DmvObjectivePrm objPrm) {
        this.objPrm = objPrm;
        this.timer = new Timer();
    }

    @Override
    public double getLastParseWeight() {
        return parseWeight;
    }
    
    public DepTreebank getViterbiParse(DmvTrainCorpus corpus, Model genericModel) {
        // Lazily construct the objective.
        if (dmvObj == null || this.sents != corpus.getSentences()) {
            this.dmvObj = new DmvObjective(objPrm, new IndexedDmvModel(corpus));
            this.sents = corpus.getSentences();
        }
        DmvModel model = (DmvModel) genericModel;
        DepTreebank treebank = new DepTreebank(model.getTagAlphabet());

        parseWeight = 0.0;

        for (int s = 0; s < corpus.size(); s++) {
            if (corpus.isLabeled(s)) {
                treebank.add(corpus.getTree(s));
            } else {
                Pair<DepTree, Double> pair = parse(corpus.getSentence(s), model);
                treebank.add(pair.get1());
            }
        }
        parseWeight = dmvObj.computeTrueObjective((DmvModel)model, treebank);
        return treebank;
    }

    @Override
    public DepTreebank getViterbiParse(SentenceCollection sentences, Model genericModel) {
        DmvTrainCorpus corpus = new DmvTrainCorpus(sentences);
        return getViterbiParse(corpus, genericModel);
    }

    public Pair<DepTree, Double> parse(Sentence sentence, DmvModel dmv) {
        timer.start();
        if (parser == null) {
            // Lazily contruct the parser, which does some handy caching of the model.
            parser = new DmvCkyPcfgParser();
        }
        Pair<DepTree,Double> pair = parser.parse(sentence, dmv);
        timer.stop();
        log.debug("Average seconds per sentence: " + timer.avgSec());
        return pair;
    }
    
}
