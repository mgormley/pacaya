package edu.jhu.nlp.depparse;

import edu.jhu.autodiff.erma.InsideOutsideDepParse;
import edu.jhu.nlp.Trainable;
import edu.jhu.nlp.data.DepTree;
import edu.jhu.nlp.data.simple.AnnoSentence;
import edu.jhu.nlp.data.simple.AnnoSentenceCollection;
import edu.jhu.parse.dep.EdgeScores;
import edu.jhu.parse.dep.ProjectiveDependencyParser;

/**
 * Projectivizes non-projective trees by running a first order parser where the edges scores are 1.0
 * if an edge is present in the gold tree and -1.0 otherwise. This follows Carerras (2007).
 * 
 * @author mgormley
 */
public class Projectivizer {
    
    public void projectivize(AnnoSentenceCollection sents) {
        for (AnnoSentence sent : sents) {
            if (sent.getParents() != null) {
                sent.setParents(projectivize(sent.getParents()));
            }
        }
    }
    
    public int[] projectivize(int[] parents) {
        //TODO: if (DepTree.checkIsProjective(parents));
        EdgeScores scores = new EdgeScores(parents.length, -1.0);
        for (int c=0; c<parents.length; c++) {
            scores.setScore(parents[c], c, 1.0);
        }
        double val;
        int[] newrents = new int[parents.length];
        if (InsideOutsideDepParse.singleRoot) {
            val = ProjectiveDependencyParser.parseSingleRoot(scores.root, scores.child, newrents);
        } else {
            val = ProjectiveDependencyParser.parseMultiRoot(scores.root, scores.child, newrents);
        }
        return newrents;
    }

}
