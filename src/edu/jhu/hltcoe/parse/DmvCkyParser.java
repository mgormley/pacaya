package edu.jhu.hltcoe.parse;


import edu.jhu.hltcoe.data.DepTree;
import edu.jhu.hltcoe.data.DepTreebank;
import edu.jhu.hltcoe.data.Sentence;
import edu.jhu.hltcoe.data.SentenceCollection;
import edu.jhu.hltcoe.model.Model;
import edu.jhu.hltcoe.model.dmv.DmvModel;
import edu.jhu.hltcoe.model.dmv.DmvModelConverter;
import edu.jhu.hltcoe.parse.pr.CKYParser;
import edu.jhu.hltcoe.parse.pr.DepInstance;
import edu.jhu.hltcoe.parse.pr.DepProbMatrix;
import edu.jhu.hltcoe.parse.pr.DepSentenceDist;
import edu.jhu.hltcoe.util.Pair;

public class DmvCkyParser implements ViterbiParser {

    private double parseWeight;

    @Override
    public double getLastParseWeight() {
        return parseWeight;
    }

    @Override
    public DepTreebank getViterbiParse(SentenceCollection sentences, Model genericModel) {
        DmvModel model = (DmvModel) genericModel;

        DepProbMatrix depProbMatrix = DmvModelConverter.getDepProbMatrix(model, sentences.getLabelAlphabet());
        DepTreebank treebank = new DepTreebank();

        parseWeight = 0.0;
        for (Sentence sentence : sentences) {
            Pair<DepTree, Double> pair = parse(sentence, depProbMatrix);
            DepTree tree = pair.get1();
            parseWeight += pair.get2();
            
            treebank.add(tree);
        }
        return treebank;
    }

    public Pair<DepTree, Double> parse(Sentence sentence, DepProbMatrix depProbMatrix) {
        int[] tags = sentence.getLabelIds();
        DepInstance depInstance = new DepInstance(tags);
        DepSentenceDist sd = new DepSentenceDist(depInstance, depProbMatrix);

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
