package edu.jhu.nlp.features;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import edu.jhu.nlp.features.TemplateLanguage.Description;

/** 
 * Tree representing a feature template.
 * @author mgormley
 */
public class TemplateStruct {
    
    public String name;
    public String full;
    public Enum<?> type;
    public List<TemplateStruct> deps = new ArrayList<>();
    
    private static Pattern whitespace = Pattern.compile("\\s");
    
    private TemplateStruct() {
        // private constructor.
    }
    
    public static TemplateStruct readTree(String t) {
        t = whitespace.matcher(t).replaceAll("");
        TemplateStruct s = new TemplateStruct();
        int j = readTree(t, 0, s);
        if (j != t.length()) {
            throw new RuntimeException("Did not consume entire tree: " + j + " " + t);
        }
        return s;
    }
    
    private static int readTree(String t, int start, TemplateStruct s) {
        int i = start;
        int j = i;
        
        // Read the name of the current node.
        if (t.charAt(i) == '(') {
            throw new RuntimeException("Node without a name: " + t);
        }
        while (j < t.length() && t.charAt(j) != '(' && t.charAt(j) != ')' && t.charAt(j) != ',') {
            j++;
        }
        s.name = t.substring(i, j);
        
        if (j == t.length() || t.charAt(j) == ',') {
            // Tree ended without a closing bracket
        } else if(t.charAt(j) == ')') {
            // This node has no arguments.
        } else if (t.charAt(j) == '(') {
            // Read the arguments.
            // Consume the paren.
            j++;
            while (true) {
                TemplateStruct a = new TemplateStruct();
                j = readTree(t, j, a);
                s.deps.add(a);
                if (t.charAt(j) == ')') {
                    // Consume the paren, and return the end of this subtree.
                    j++;
                    break;
                }
                if (t.charAt(j) == ',') {
                    // Consume the argument separator and continue reading arguments.
                    j++;
                }
            }
        } else {
            throw new RuntimeException("Never reached");
        }
        s.full = t.substring(i, j);
        
        updateWithDescription(s);
        
        return j;
    }

    private static void updateWithDescription(TemplateStruct s) {
        // Try to get a description for the full subtree.
        Description d = TemplateLanguage.getDescByName(s.full);
        if (d != null) {
            // There is a description for the full name. So discard the children.
            s.deps.clear();
        } else {
            // Otherwise, get a description for just the name (the more common case).
            d = TemplateLanguage.getDescByName(s.name);
        }
        if (d != null) {
            // Set the type from the description.
            s.type = d.getObj();
        }
    }
    
}
