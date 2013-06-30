package edu.jhu.data.conll;

import java.io.File;
import java.io.IOException;

import edu.jhu.data.BrownClusterTagger;
import edu.jhu.data.Label;
import edu.jhu.util.Alphabet;
import edu.jhu.util.cli.Opt;

public class CoNLL09BrownTagger {

    @Opt(name="train", hasArg=true, required=true, description="CoNLL 09 input file or directory.")
    public static String train = null;
    @Opt(name="trainOut", hasArg=true, required=true, description="CoNLL 09 output output file.")
    public static File trainOut = null;
    @Opt(name="brownClusters", hasArg=true, required=true, description="Brown clusters file.")
    public static File brownClusters = null;
    @Opt(name="maxTagLength", hasArg=true, description="Maximum length for brown cluster tag.")
    public static int maxTagLength = Integer.MAX_VALUE;
    
    public static void main(String[] args) throws IOException {
        Alphabet<Label> alphabet = new Alphabet<Label>();
        BrownClusterTagger tagger = new BrownClusterTagger(alphabet, maxTagLength);
        tagger.read(brownClusters);
        CoNLL09FileReader reader = new CoNLL09FileReader(new File(train));
        CoNLL09Writer writer = new CoNLL09Writer(trainOut);
        for (CoNLL09Sentence sent : reader) {
            for (CoNLL09Token tok : sent) {
                String word = tok.getForm();
                String cluster = tagger.getCluster(word);
                tok.setPos(cluster);
                tok.setPpos(cluster);
            }
            writer.write(sent);
        }
        reader.close();
        writer.close();
    }
}
