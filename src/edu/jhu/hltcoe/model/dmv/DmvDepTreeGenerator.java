package edu.jhu.hltcoe.model.dmv;

import java.util.Random;

import edu.jhu.hltcoe.data.DepTree;
import edu.jhu.hltcoe.data.DepTreeNode;
import edu.jhu.hltcoe.data.DepTreebank;
import edu.jhu.hltcoe.data.Label;
import edu.jhu.hltcoe.data.ProjDepTreeNode;
import edu.jhu.hltcoe.data.ProjWallDepTreeNode;
import edu.jhu.hltcoe.math.LabeledMultinomial;

public class DmvDepTreeGenerator {
    
    private DmvModel model;
    private Random random;

    public DmvDepTreeGenerator(DmvModel model, long seed) {
        this.model = new DmvModel(model.getTagAlphabet());
        this.model.copyFrom(model);
        this.model.convertLogToReal();
        random = new Random(seed);
    }
    
    public DepTreebank getTreebank(int numTrees) {
        DepTreebank treebank = new DepTreebank(model.getTagAlphabet());
        
        for (int i=0; i<numTrees; i++) {
            ProjDepTreeNode wall = new ProjWallDepTreeNode();
            recursivelyGenerate(wall);
            
            treebank.add(new DepTree(wall));
        }
        
        return treebank;
    }

    private void recursivelyGenerate(ProjDepTreeNode parent) {
        if (parent.isWall()) {
            LabeledMultinomial<Label> parameters = model.getRootWeights();
            Label childLabel = parameters.sampleFromMultinomial(random);
            parent.addChildToOutside(new ProjDepTreeNode(childLabel), "r");
        } else {
            sampleChildren(parent, "l");
            sampleChildren(parent, "r");
        }
        
        // Recurse on each child
        for (DepTreeNode child : parent.getChildren()) {
            recursivelyGenerate((ProjDepTreeNode)child);
        }
    }

    private void sampleChildren(ProjDepTreeNode parent, String lr) {
        if (random.nextDouble() > model.getStopWeight(parent.getLabel(), lr, true)) {
            // Generate adjacent
            LabeledMultinomial<Label> parameters = model.getChooseWeights(parent.getLabel(), lr);
            Label childLabel = parameters.sampleFromMultinomial(random);
            parent.addChildToOutside(new ProjDepTreeNode(childLabel), lr);
            while (random.nextDouble() > model.getStopWeight(parent.getLabel(), lr, false)) {
                // Generate non-adjacent
                childLabel = parameters.sampleFromMultinomial(random);
                parent.addChildToOutside(new ProjDepTreeNode(childLabel), lr);
            }
        }
    }
    
}
