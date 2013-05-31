package edu.jhu.hltcoe.parse;


import depparsing.extended.CKYParser;
import depparsing.extended.DepSentenceDist;
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

public class DmvCkyParser implements ViterbiParser {

    private double parseWeight;
    private DmvObjective dmvObj;
    private SentenceCollection sents;
    private DmvObjectivePrm objPrm;

    public DmvCkyParser() {
        this(new DmvObjectivePrm());
    }
    
    public DmvCkyParser(DmvObjectivePrm objPrm) {
        this.objPrm = objPrm;
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
        return parse1(sentence, dmv);
    }
    
    /**
     * My CKY parser.
     */
    public Pair<DepTree, Double> parse1(Sentence sentence, DmvModel dmv) {
        DmvCkyPcfgParser parser = new DmvCkyPcfgParser();
        return parser.parse(sentence, dmv);
    }
    
    /**
     * The depparsing package's CKYParser.
     */
    public Pair<DepTree, Double> parse2(Sentence sentence, DmvModel depProbMatrix) {
        assert(sentence.getAlphabet() == depProbMatrix.getTagAlphabet());
        DepSentenceDist sd = new DepSentenceDist(sentence, depProbMatrix);

        Pair<DepTree, Double> pair = parse(sentence, sd);
        return pair;
    }

    private Pair<DepTree, Double> parse(Sentence sentence, DepSentenceDist sd) {
        int numWords = sd.depInst.postags.length;
        int[] parents = new int[numWords];

        double parseWeight = CKYParser.parseSentence(sd, parents);

        // Must decrement parents array by one
        for (int i = 0; i < parents.length; i++) {
            parents[i]--;
        }
        DepTree tree = new DepTree(sentence, parents, true);

        return new Pair<DepTree, Double>(tree, parseWeight);
    }

}
