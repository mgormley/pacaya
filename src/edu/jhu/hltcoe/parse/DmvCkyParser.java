package edu.jhu.hltcoe.parse;


import edu.jhu.hltcoe.data.DepTree;
import edu.jhu.hltcoe.data.DepTreebank;
import edu.jhu.hltcoe.data.Sentence;
import edu.jhu.hltcoe.data.SentenceCollection;
import edu.jhu.hltcoe.gridsearch.dmv.DmvObjective;
import edu.jhu.hltcoe.model.Model;
import edu.jhu.hltcoe.model.dmv.DmvModel;
import depparsing.extended.CKYParser;
import depparsing.extended.DepInstance;
import depparsing.extended.DepSentenceDist;
import edu.jhu.hltcoe.train.DmvTrainCorpus;
import edu.jhu.hltcoe.util.Pair;

public class DmvCkyParser implements ViterbiParser {

    private double parseWeight;
    private DmvObjective dmvObj;
    private DmvTrainCorpus corpus;

    @Override
    public double getLastParseWeight() {
        return parseWeight;
    }
    
    public DepTreebank getViterbiParse(DmvTrainCorpus corpus, Model genericModel) {
        // Lazily construct the objective.
        if (dmvObj == null || this.corpus != corpus) {
            this.dmvObj = new DmvObjective(corpus);
            this.corpus = corpus;
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
        DmvModel model = (DmvModel) genericModel;
        DepTreebank treebank = new DepTreebank(model.getTagAlphabet());

        parseWeight = 0.0;
        for (Sentence sentence : sentences) {
            Pair<DepTree, Double> pair = parse(sentence, model);
            DepTree tree = pair.get1();
            parseWeight += pair.get2();
            
            treebank.add(tree);
        }
        return treebank;
    }

    public Pair<DepTree, Double> parse(Sentence sentence, DmvModel depProbMatrix) {
        assert(sentence.getAlphabet() == depProbMatrix.getTagAlphabet());
        DepSentenceDist sd = new DepSentenceDist(sentence, depProbMatrix);

        Pair<DepTree, Double> pair = parse(sentence, sd);
        return pair;
    }

    public Pair<DepTree, Double> parse(Sentence sentence, DepSentenceDist sd) {
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
