package edu.jhu.hltcoe.data;

public class VerbTreeFilter implements TreeFilter {

    @Override
    public boolean accept(DepTree tree) {
        boolean hasVB = false;
        boolean hasMD = false;
        for (DepTreeNode node : tree) {
            if (node.getLabel() instanceof TaggedWord) {
                TaggedWord tw = (TaggedWord) node.getLabel();
                
                if (tw.getTag() == "VBP" || tw.getTag() == "VBD" || tw.getTag() == "VBZ") {
                    return true;
                } else if (tw.getTag() == "VB") {
                    hasVB = true;
                } else if (tw.getTag() == "MD") {
                    hasMD = true;
                }
                
                if (hasMD && hasVB) {
                    return true;
                }
            } else {
                // Always accept trees if we don't have tags
                return true;
            }
        }
        return false;
    }

}
