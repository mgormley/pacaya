package edu.jhu.hltcoe.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import edu.stanford.nlp.ling.HasTag;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.trees.CollinsHeadFinder;
import edu.stanford.nlp.trees.Dependency;
import edu.stanford.nlp.trees.HeadFinder;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.util.Filter;

public class PtbDepTree extends DepTree {

    private static String[] ptbPunctTags = {"!", "#", "$", "''", "(", ")", ",", "-LRB-", "-RRB-", ".", ":", "?", "``"};
    HashSet<String> punctTags = new HashSet<String>(Arrays.asList(ptbPunctTags));
    private Tree tree;
    
    private class PunctuationFilter implements Filter<Tree> {

        private static final long serialVersionUID = 1L;

        @Override
        public boolean accept(Tree node) {
            if (node.isPreTerminal()) {
                String tag = node.label().value();
                if (punctTags.contains(tag)) {
                    return false;
                }
            }
            return true;
        }
        
    }
    
    /**
     * Construct a dependency tree from a Stanford constituency tree.
     * 
     * @param tree
     */
    public PtbDepTree(Tree tree) {
        // Cache the original Stanford tree.
        this.tree = tree;
        
        // Remove punctuation
        tree = tree.prune(new PunctuationFilter());
        
        List<Tree> leaves = tree.getLeaves();
        for (Tree leaf : leaves) {
            if (punctTags.contains(getTag(leaf, tree))) {
                throw new IllegalStateException("There shouldn't be any leaves that are considered punctuation!");
            }
        }
        
        // Create parents array
        parents = new int[leaves.size()];
        Arrays.fill(parents, EMPTY_POSITION);

        // Percolate heads
        HeadFinder hf = new CollinsHeadFinder();
        tree.percolateHeads(hf);
        Collection<Dependency<Tree, Tree, Object>> dependencies = mapDependencies(tree, hf);

        for(Dependency<Tree, Tree, Object> dependency : dependencies) {
            Tree parent = dependency.governor();
            Tree child = dependency.dependent();
                        
            int parentIdx = indexOfInstance(leaves, parent);
            int childIdx = indexOfInstance(leaves, child);
            
            if (childIdx < 0) {
                throw new IllegalStateException("Child not found in leaves: " + child);
            } else if (parents[childIdx] != EMPTY_POSITION) {
                throw new IllegalStateException("Multiple parents for the same node: " + child);
            }
            
            parents[childIdx] = parentIdx;
        }
        for (int i=0; i<parents.length; i++) {
            if (parents[i] == EMPTY_POSITION) {
                parents[i] = WallDepTreeNode.WALL_POSITION;
            }
        }
        
        // Create nodes
        nodes.add(new WallDepTreeNode());
        int position = 0;
        for (Tree leaf : leaves) {
            // Note: it is the parent of the leaf that has the word AND the tag
            String word = getWord(leaf, tree);
            String tag = getTag(leaf, tree);
            assert(word != null || tag != null);
            nodes.add(new NonprojDepTreeNode(word, tag, position));            
            position++;
        }
        
        // Add parent/child links to DepTreeNodes
        addParentChildLinksToNodes();
        
        this.isProjective = checkIsProjective();
    }


    private String getWord(Tree leaf, Tree tree) {
        edu.stanford.nlp.ling.Label label = leaf.parent(tree).label();
        String word = null;
        if (label instanceof HasWord) {
            word = ((HasWord)label).word();
        }
        return word;
    }

    private String getTag(Tree leaf, Tree tree) {
        edu.stanford.nlp.ling.Label label = leaf.parent(tree).label();
        String tag = null;
        if (label instanceof HasTag) {
            tag = ((HasTag)label).tag();
        }
        return tag;
    }

    /**
     * Standard List.indexOf() uses the equals method for comparison.
     * This method uses == for comparison.
     */
    private <X> int indexOfInstance(List<X> list, X obj) {
        int i=0; 
        for (X x : list) {
            if ( x == obj) {
               return i; 
            }
            i++;
        }
        return -1;
    }

    private static Collection<Dependency<Tree, Tree, Object>> mapDependencies(Tree tree, HeadFinder hf) {
        if (hf == null) {
            throw new IllegalArgumentException("mapDependencies: need headfinder");
        }
        List<Dependency<Tree, Tree, Object>> deps = new ArrayList<Dependency<Tree, Tree, Object>>();
        for (Tree node : tree) {
            if (node.isLeaf() || node.children().length < 2) {
                continue;
            }
            // Label l = node.label();
            // System.err.println("doing kids of label: " + l);
            // Tree hwt = node.headPreTerminal(hf);
            Tree hwt = node.headTerminal(hf);
            // System.err.println("have hf, found head preterm: " + hwt);
            if (hwt == null) {
                throw new HeadFinderException("mapDependencies: headFinder failed!");
            }

            for (Tree child : node.children()) {
                // Label dl = child.label();
                // Tree dwt = child.headPreTerminal(hf);
                Tree dwt = child.headTerminal(hf);
                if (dwt == null) {
                    throw new HeadFinderException("mapDependencies: headFinder failed!");
                }
                // System.err.println("kid is " + dl);
                // System.err.println("transformed to " +
                // dml.toString("value{map}"));
                if (dwt != hwt) {
                    Dependency<Tree, Tree, Object> p = new UnnamedTreeDependency(hwt, dwt);
                    deps.add(p);
                }
            }
        }
        return deps;
    }

    public Tree getPtbTree() {
        return tree;
    }
    
}
