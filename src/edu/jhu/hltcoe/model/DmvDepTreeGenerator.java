package edu.jhu.hltcoe.model;

import edu.jhu.hltcoe.data.DepTree;
import edu.jhu.hltcoe.data.DepTreeNode;
import edu.jhu.hltcoe.data.DepTreebank;
import edu.jhu.hltcoe.data.Label;
import edu.jhu.hltcoe.data.ProjDepTreeNode;
import edu.jhu.hltcoe.data.WallDepTreeNode;
import edu.jhu.hltcoe.math.LabeledMultinomial;
import edu.jhu.hltcoe.util.Prng;

public class DmvDepTreeGenerator {
    
    private DmvModel model;

    public DmvDepTreeGenerator(DmvModel model) {
        this.model = model;
    }
    
    public DepTreebank getTreebank(int numTrees) {
        DepTreebank treebank = new DepTreebank();
        
        for (int i=0; i<numTrees; i++) {
            ProjDepTreeNode wall = new ProjDepTreeNode(WallDepTreeNode.WALL_LABEL);
            recursivelyGenerate(wall);
            
            treebank.add(new DepTree(wall));
        }
        
        return treebank;
    }

    private void recursivelyGenerate(ProjDepTreeNode parent) {
        sampleChildren(parent, "l");
        sampleChildren(parent, "r");
        
        // Recurse on each child
        for (DepTreeNode child : parent.getChildren()) {
            recursivelyGenerate((ProjDepTreeNode)child);
        }
    }

    private void sampleChildren(ProjDepTreeNode parent, String lr) {
        if (Prng.random.nextDouble() > model.getStopWeight(parent.getLabel(), lr, true)) {
            // Generate adjacent
            LabeledMultinomial<Label> parameters = model.getChooseWeights(parent.getLabel(), lr);
            Label childLabel = parameters.sampleFromMultinomial();
            parent.addChildToOutside(new ProjDepTreeNode(childLabel), lr);
            while (Prng.random.nextDouble() > model.getStopWeight(parent.getLabel(), lr, false)) {
                // Generate non-adjacent
                childLabel = parameters.sampleFromMultinomial();
                parent.addChildToOutside(new ProjDepTreeNode(childLabel), lr);
            }
        }
    }
    
}
