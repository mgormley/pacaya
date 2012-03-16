package edu.jhu.hltcoe.parse.pr;

import util.CountAlphabet;
import depparsing.data.DepCorpus;
import edu.jhu.hltcoe.data.Label;

public class DepInstance {

    public final int numWords;
    public final int postags[];

    public DepInstance(int[] tags) {
        this.postags = tags;
        this.numWords = postags.length;
    }

    @Override
    public String toString() {
        return super.toString() + util.ArrayPrinting.intArrayToString(postags, null, "sentence tags")
                + "\n";
    }

    public static String[] getTagStrings(CountAlphabet<Label> tagAlphabet, int[] tagIds) {
        String[] tags = new String[tagIds.length];
        for (int i = 0; i < tags.length; i++) {
            tags[i] = tagAlphabet.index2feat.get(tagIds[i]).toString();
        }
        return tags;
    }
    
    public String toString(CountAlphabet<Label> tagAlphabet) {
        return util.ArrayPrinting.intArrayToString(postags, getTagStrings(tagAlphabet, postags), "sentence tags")
                + "\n";
    }

    public String getTagsStrings(DepCorpus c) {
        String tagsS[] = c.getTagStrings(postags);
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < tagsS.length; i++) {
            sb.append(tagsS[i] + " ");
        }
        sb.append("\n");
        return sb.toString();
    }
}
