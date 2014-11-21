package edu.jhu.nlp.depparse;

import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.jhu.autodiff.erma.InsideOutsideDepParse;
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
public class Projectivizer implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(Projectivizer.class);

    private int total;
    private int correct;
    private double accuracy;
    
    public void projectivize(AnnoSentenceCollection sents) {
        total = 0;
        correct = 0;
        for (AnnoSentence sent : sents) {
            int[] oldrents = sent.getParents();
            if (oldrents != null) {
                int[] newrents = projectivize(oldrents);
                evaluate(newrents, oldrents);
                sent.setParents(newrents);
            }
        }
        accuracy = (double) correct / (double) total;
        log.info(String.format("Oracle accuracy of projectivized trees: %.4f", accuracy));
    }
    
    private void evaluate(int[] parseParents, int[] goldParents) {
        if (parseParents != null) {
            assert(parseParents.length == goldParents.length);
        }
        for (int j = 0; j < goldParents.length; j++) {
            if (parseParents != null) {
                if (goldParents[j] == parseParents[j]) {
                    correct++;
                }
            }
            total++;
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
