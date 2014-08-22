package edu.jhu.data;

import java.util.List;

/**
 * Representation of mentions of relations, events, or semantic roles.<br>
 * <br> 
 * A situation mention consists of:
 * <ul>
 *  <li>Label of the situation type.</li>
 *  <li>An optional text span corresponding to the trigger.</li>
 *  <li>A list of arguments, each of which has a text span and a label.</li>
 * </ul>
 * 
 * @author mgormley
 */
public class Situations {

    private String type;
    // Optional trigger span for this situation.
    private Span trigger;
    private List<LabeledSpan> args;
    
    public Situations(String type, Span trigger, List<LabeledSpan> args) {
        super();
        this.type = type;
        this.trigger = trigger;
        this.args = args;
    }
    
    
    
}
