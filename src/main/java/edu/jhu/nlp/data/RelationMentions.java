package edu.jhu.nlp.data;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import edu.jhu.prim.tuple.Pair;

/**
 * Representation of mentions of relations, events, or semantic roles.<br>
 * <br>
 * See also {@link RelationMention}.
 * 
 * @author mgormley
 */
public class RelationMentions implements Iterable<RelationMention> {

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
    
    public RelationMention get(int i) {
        return ments.get(i);
    }

    @Override
    public Iterator<RelationMention> iterator() {
        return ments.iterator();
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

    public RelationMention get(NerMention ne1, NerMention ne2) {
        for (RelationMention rm : ments) {
            List<Pair<String, NerMention>> args = rm.getArgs();
            NerMention arg1 = args.get(1).get2();
			NerMention arg0 = args.get(0).get2();
			assert args.size() == 2;
			if (args.size() == 2 && 
                    ((arg1.equals(ne1) && arg0.equals(ne2)) ||
                   	 (arg0.equals(ne1) && arg1.equals(ne2))    )
                   	 ) {
                return rm;
            }
        }
        return null;
    }

	public int size() {
		return ments.size();
	}

	public List<RelationMention> getMentions() {
		return ments;
	}

}
