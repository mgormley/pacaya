package edu.jhu.hltcoe.parse;

import java.util.Map.Entry;

import util.Alphabet;
import depparsing.globals.Constants;
import edu.jhu.hltcoe.data.DepTree;
import edu.jhu.hltcoe.data.DepTreebank;
import edu.jhu.hltcoe.data.Label;
import edu.jhu.hltcoe.data.Sentence;
import edu.jhu.hltcoe.data.SentenceCollection;
import edu.jhu.hltcoe.data.WallDepTreeNode;
import edu.jhu.hltcoe.math.LabeledMultinomial;
import edu.jhu.hltcoe.model.Model;
import edu.jhu.hltcoe.model.dmv.DmvModel;
import edu.jhu.hltcoe.parse.pr.CKYParser;
import edu.jhu.hltcoe.parse.pr.DepInstance;
import edu.jhu.hltcoe.parse.pr.DepProbMatrix;
import edu.jhu.hltcoe.parse.pr.DepSentenceDist;
import edu.jhu.hltcoe.util.Pair;
import edu.jhu.hltcoe.util.Triple;
import edu.jhu.hltcoe.util.Utilities;

public class DmvCkyParser implements ViterbiParser {

    private double parseWeight;

    @Override
    public double getLastParseWeight() {
        return parseWeight;
    }

    @Override
    public DepTreebank getViterbiParse(SentenceCollection sentences, Model genericModel) {
        DmvModel model = (DmvModel)genericModel;

        DepProbMatrix depProbMatrix = modelToDepProbMatrix(model, sentences.getLabelAlphabet());
        DepTreebank treebank = new DepTreebank();
        
        parseWeight = 0.0;
        for (Sentence sentence : sentences) {
            int[] tags = sentence.getLabelIds();
            DepInstance depInstance = new DepInstance(tags);
            DepSentenceDist sd = new DepSentenceDist(depInstance, depProbMatrix.nontermMap);
            sd.cacheModel(depProbMatrix);

            int numWords = sd.depInst.postags.length;
            int[] parents = new int[numWords];

            parseWeight += CKYParser.parseSentence(sd, parents);

            // Must decrement parents array by one
            for (int i=0; i<parents.length; i++) {
                parents[i]--;
            }
            
            DepTree tree = new DepTree(sentence, parents, true);
            treebank.add(tree);
        }
        return treebank;
    }
    
    /**
     * TODO: This is an expensive conversion, and DmvModel should be rewritten so that it's just
     * some double arrays. 
     */
    private DepProbMatrix modelToDepProbMatrix(DmvModel model, Alphabet<Label> tagAlphabet) {
        DepProbMatrix depProbMatrix = new DepProbMatrix(tagAlphabet, 2, 1);
        depProbMatrix.fill(Double.NEGATIVE_INFINITY);
        for (Entry<Pair<Label, String>, LabeledMultinomial<Label>> entry : model.getChooseWeights().entrySet()) {
            Pair<Label, String> key = entry.getKey();
            Label parent = key.get1();
            String lr = key.get2();
            LabeledMultinomial<Label> mult = entry.getValue();
            for (Entry<Label, Double> cEntry : mult.entrySet()) {
                Label child = cEntry.getKey();
                double prob = cEntry.getValue();
                
                // We use logForIlp so that our solutions are analogous to IlpViterbiParser
                double logProb = Utilities.logForIlp(prob);
                
                if (child.equals(WallDepTreeNode.WALL_LABEL)) {
                    // Skip these
                    continue;
                } else if (parent.equals(WallDepTreeNode.WALL_LABEL)) {
                    int cid = tagAlphabet.lookupObject(child);
                    depProbMatrix.root[cid] = logProb;
                } else {
                    int pid = tagAlphabet.lookupObject(parent);
                    int dir = lr.equals("l") ? Constants.LEFT : Constants.RIGHT;
                    int cid = tagAlphabet.lookupObject(child);
                    int v = 0;  
                    depProbMatrix.child[cid][pid][dir][v] = logProb;
                }
            }
        }
        
        for (Entry<Triple<Label, String, Boolean>, Double> entry : model.getStopWeights().entrySet()) {
            Triple<Label, String, Boolean> key = entry.getKey();
            double stopProb = entry.getValue();
            Label parent = key.get1();
            String lr = key.get2();
            boolean adjacent = key.get3();
            

            int pid = tagAlphabet.lookupObject(parent);
            int dir = lr.equals("l") ? Constants.LEFT : Constants.RIGHT;
            // Note this is backwards from how adjacency is encoded for the ILPs
            int kids = adjacent ? 0 : 1;
            
            // We use logForIlp so that our solutions are analogous to IlpViterbiParser
            double stopLogProb = Utilities.logForIlp(stopProb);
            double contLogProb = Utilities.logForIlp(1.0 - stopProb);
            
            if (!parent.equals(WallDepTreeNode.WALL_LABEL)) {
                depProbMatrix.decision[pid][dir][kids][Constants.END] = stopLogProb;
                depProbMatrix.decision[pid][dir][kids][Constants.CONT] = contLogProb;
            }
        }
        
        return depProbMatrix;
    }
    
}
