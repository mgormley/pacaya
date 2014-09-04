package edu.jhu.data;

import java.util.ArrayList;
import java.util.List;

/**
 * Representation of mentions of relations, events, or semantic roles.<br>
 * <br>
 * See also {@link RelationMention}.
 * 
 * @author mgormley
 */
public class RelationMentions {

    private List<RelationMention> ments;

    public RelationMentions() {
        ments = new ArrayList<>();
    }

    /** Deep copy constructor. */
    public RelationMentions(RelationMentions other) {
        this.ments = new ArrayList<>(other.ments.size());
        for (RelationMention sm : other.ments) {
            this.ments.add(new RelationMention(sm));
        }
    }

    public void intern() {
        for (RelationMention sm : ments) {
            sm.intern();
        }
    }

    public void add(RelationMention sm) {
        ments.add(sm);
    }

    @Override
    public String toString() {
        return "Situations [ments=" + ments + "]";
    }

    public String toString(List<String> words) {
        StringBuilder sb = new StringBuilder();
        sb.append("Situations [");
        for (int i = 0; i < ments.size(); i++) {
            if (i != 0) {
                sb.append(" ");
            }
            sb.append(ments.get(i).toString(words));
        }
        sb.append("]");
        return sb.toString();
    }

}
