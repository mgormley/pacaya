package edu.jhu.hltcoe.data;

import edu.stanford.nlp.ling.Label;
import edu.stanford.nlp.ling.CoreAnnotations.IndexAnnotation;
import edu.stanford.nlp.trees.Dependency;
import edu.stanford.nlp.trees.DependencyFactory;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.ErasureUtils;
import edu.stanford.nlp.util.XMLUtils;

/**
 * Modified from Christopher Manning's original version. Used by DepTree.
 * 
 * An individual dependency between a head and a dependent. The head and
 * dependent are represented as a Label. For example, these can be a Word or a
 * WordTag. If one wishes the dependencies to preserve positions in a sentence,
 * then each can be a LabeledConstituent.
 */
public class UnnamedTreeDependency implements Dependency<Tree, Tree, Object> {

    //@SuppressWarnings( { "NonSerializableFieldInSerializableClass" })
    private Tree regent;
    //@SuppressWarnings( { "NonSerializableFieldInSerializableClass" })
    private Tree dependent;

    @Override
    public int hashCode() {
        return regent.hashCode() ^ dependent.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof UnnamedTreeDependency) {
            UnnamedTreeDependency d = (UnnamedTreeDependency) o;
            return governor().equals(d.governor()) && dependent().equals(d.dependent());
        }
        return false;
    }

    public boolean equalsIgnoreName(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof Dependency<?, ?, ?>) {
            Dependency<Label, Label, Object> d = ErasureUtils.<Dependency<Label, Label, Object>> uncheckedCast(o);
            return governor().equals(d.governor()) && dependent().equals(d.dependent());
        }
        return false;
    }

    @Override
    public String toString() {
        return regent + " --> " + dependent;
    }

    private static String getIndexStrOrEmpty(Label lab) {
        String ans = "";
        if (lab instanceof CoreMap) {
            CoreMap aml = (CoreMap) lab;
            int idx = aml.get(IndexAnnotation.class);
            if (idx >= 0) {
                ans = " idx=\"" + idx + "\"";
            }
        }
        return ans;
    }

    /**
     * Provide different printing options via a String keyword. The recognized
     * options are currently "xml", and "predicate". Otherwise the default
     * toString() is used.
     */
    public String toString(String format) {
        if ("xml".equals(format)) {
            String govIdxStr = getIndexStrOrEmpty(governor());
            String depIdxStr = getIndexStrOrEmpty(dependent());
            return "  <dep>\n    <governor" + govIdxStr + ">" + XMLUtils.escapeXML(governor().value())
                    + "</governor>\n    <dependent" + depIdxStr + ">" + XMLUtils.escapeXML(dependent().value())
                    + "</dependent>\n  </dep>";
        } else if ("predicate".equals(format)) {
            return "dep(" + governor() + "," + dependent() + ")";
        } else {
            return toString();
        }
    }

    public UnnamedTreeDependency(Tree regent, Tree dependent) {
        if (regent == null || dependent == null) {
            throw new IllegalArgumentException("governor or dependent cannot be null");
        }
        this.regent = regent;
        this.dependent = dependent;
    }

    public Tree governor() {
        return regent;
    }

    public Tree dependent() {
        return dependent;
    }

    public Object name() {
        return null;
    }

    public DependencyFactory dependencyFactory() {
        throw new RuntimeException("not implemented");
    }
    
    private static final long serialVersionUID = 5;


}
