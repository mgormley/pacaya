package edu.jhu.nlp.data;

public class VerbTreeFilter implements TreeFilter {

    @Override
    public boolean accept(DepTree tree) {
        boolean hasVB = false;
        boolean hasMD = false;
        for (DepTreeNode node : tree) {
            String tw = node.getLabel();
                
            if (tw == "VBP" || tw == "VBD" || tw == "VBZ") {
                return true;
            } else if (tw == "VB") {
                hasVB = true;
            } else if (tw == "MD") {
                hasMD = true;
            }
            
            if (hasMD && hasVB) {
                return true;
            }
        }
        return false;
    }

}
