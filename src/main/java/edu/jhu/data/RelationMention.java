package edu.jhu.data;

import java.util.ArrayList;
import java.util.List;

import edu.jhu.prim.tuple.Pair;

/**
 * A relation mention consisting of:
 * <ul>
 * <li>Label of the relation type.</li>
 * <li>An optional text span corresponding to the trigger.</li>
 * <li>A list of arguments, each of which has a text span and a label.</li>
 * </ul>
 * 
 * @author mgormley
 */
public class RelationMention {

    private String type;
    private String subType;
    private List<Pair<String, NerMention>> args;
    // Optional trigger span for this situation.
    private Span trigger;

    public RelationMention(String type, String subType, List<Pair<String, NerMention>> args, Span trigger) {
        super();
        this.type = type;
        this.subType = subType;
        this.args = args;
        this.trigger = trigger;
    }

    public RelationMention(RelationMention other) {
        this.type = other.type;
        this.subType = other.subType;
        if (args != null) {
            this.args = new ArrayList<>(other.args.size());
            for (Pair<String, NerMention> pair : other.args) {
                this.args.add(new Pair<String, NerMention>(pair.get1(), new NerMention(pair.get2())));
            }
        }
        this.trigger = other.trigger;
    }

    public void intern() {
        if (type != null) {
            type = type.intern();
        }
        if (subType != null) {
            subType = subType.intern();
        }
        for (int i = 0; i < args.size(); i++) {
            Pair<String, NerMention> pair = args.get(i);
            pair.get2().intern();
            pair = new Pair<>(pair.get1().intern(), pair.get2());
            args.set(i, pair);
        }
    }

    @Override
    public String toString() {
        return "SituationMent [type=" + type + ", subType=" + subType + ", args=" + args + ", trigger=" + trigger + "]";
    }

    public String toString(List<String> words) {
        StringBuilder argsStr = new StringBuilder();
        argsStr.append("[");
        for (int i = 0; i < args.size(); i++) {
            if (i != 0) {
                argsStr.append(", ");
            }
            Pair<String, NerMention> p = args.get(i);
            argsStr.append(p.get1());
            argsStr.append("=");
            argsStr.append(p.get2().getSpan().getString(words, " "));
        }
        argsStr.append("]");
        return "SituationMent [type=" + type + ", subType=" + subType + ", args=" + argsStr + ", trigger="
                + trigger.getString(words, " ") + "]";
    }

}