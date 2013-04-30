package edu.jhu.hltcoe.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import edu.jhu.hltcoe.data.conll.CoNLLXSentence;
import edu.jhu.hltcoe.util.Alphabet;
import edu.stanford.nlp.ling.HasTag;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.trees.CollinsHeadFinder;
import edu.stanford.nlp.trees.Dependency;
import edu.stanford.nlp.trees.HeadFinder;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.util.Filter;

public class DepTree implements Iterable<DepTreeNode> {

    public static final int EMPTY_POSITION = -2;
    
    private List<DepTreeNode> nodes = new ArrayList<DepTreeNode>();
    private int[] parents;
    private boolean isProjective;
    
    private static String[] ptbPunctTags = {"!", "#", "$", "''", "(", ")", ",", "-LRB-", "-RRB-", ".", ":", "?", "``"};
    HashSet<String> punctTags = new HashSet<String>(Arrays.asList(ptbPunctTags));
    
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
    public DepTree(Tree tree) {
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
        
        if (true) {
            
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
     * Construct a dependency tree from a sentence and the head of each token.
     * 
     * @param sentence The input sentence.
     * @param parents The index of the parent of each token. -1 indicates the root.
     * @param isProjective Whether the tree is projective.
     */
    public DepTree(Sentence sentence, int[] parents, boolean isProjective) {
        this.isProjective = isProjective;
        this.parents = parents;
        nodes.add(new WallDepTreeNode());
        for (int i=0; i<sentence.size(); i++) {
            Label label = sentence.get(i);
            nodes.add(new NonprojDepTreeNode(label, i));
        }
        // Add parent/child links to DepTreeNodes
        addParentChildLinksToNodes();
    }
    
    /**
     * Construct a dependency tree from a wall node and its children.
     * 
     * @param wall
     */
    @SuppressWarnings("unchecked")
    public DepTree(ProjDepTreeNode wall) {
        isProjective = true;
        nodes = (List<DepTreeNode>)wall.getInorderTraversal();
        // Set all the positions on the nodes
        int position;
        position=WallDepTreeNode.WALL_POSITION;
        for (DepTreeNode node : nodes) {
            ((ProjDepTreeNode)node).setPosition(position);
            position++;
        }
        // Set all the parent positions
        parents = new int[nodes.size()-1];
        for (int i=0; i<parents.length; i++) {
            ProjDepTreeNode parent = (ProjDepTreeNode)nodes.get(i+1).getParent();
            if (parent == null) {
                parents[i] = EMPTY_POSITION;
            } else {
                parents[i] = parent.getPosition();
            }
        }
        checkTree();
    }

    public DepTree(CoNLLXSentence sent) {
        // TODO: filter out punctuation.
        this(new Sentence(sent), sent.getParents(), false);
    }

    private DepTreeNode getNodeByPosition(int position) {
        return nodes.get(position+1);
    }
    
    private void addParentChildLinksToNodes() {
        checkTree();
        for (int i=0; i<parents.length; i++) {
            NonprojDepTreeNode child = (NonprojDepTreeNode)getNodeByPosition(i);
            NonprojDepTreeNode parent = (NonprojDepTreeNode)getNodeByPosition(parents[i]);
            child.setParent(parent);
            parent.addChild(child);
        }
    }

    private void checkTree() {
        // Check that there is exactly one node with the WALL as its parent
        int emptyCount = countChildrenOf(EMPTY_POSITION);
        if (emptyCount != 0) {
            throw new IllegalStateException("Found an empty parent cell. emptyCount=" + emptyCount);
        }
        int wallCount = countChildrenOf(WallDepTreeNode.WALL_POSITION);
        if (wallCount != 1) {
            throw new IllegalStateException("There must be exactly one node with the wall as a parent. wallCount=" + wallCount);
        }
        
        // Check that there are no cyles
        for (int i=0; i<parents.length; i++) {
            int numAncestors = 0;
            int parent = parents[i];
            while(parent != WallDepTreeNode.WALL_POSITION) {
                numAncestors += 1;
                if (numAncestors > parents.length - 1) {
                    throw new IllegalStateException("Found cycle in parents array");
                }
                parent = parents[parent];
            }
        }

        // Check for proper list lengths
        if (nodes.size()-1 != parents.length) {
            throw new IllegalStateException("Number of nodes does not equal number of parents");
        }
        
        // Check for projectivity if necessary
        if (isProjective) {
            if (!checkIsProjective()) {
                throw new IllegalStateException("Found non-projective arcs in tree");
            }
        }
    }
    
    private boolean checkIsProjective() {
        for (int i=0; i<parents.length; i++) {
            int pari = parents[i] == WallDepTreeNode.WALL_POSITION ? parents.length : parents[i];
            int minI = i < pari ? i : pari;
            int maxI = i > pari ? i : pari;
            for (int j=0; j<parents.length; j++) {
                if (j == i) {
                    continue;
                }
                if (minI < j && j < maxI) {
                    if (!(minI <= parents[j] && parents[j] <= maxI)) {
                        return false;
                    }
                } else {
                    if (!(parents[j] <= minI || parents[j] >= maxI)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private int countChildrenOf(int parent) {
        int count = 0;
        for (int i=0; i<parents.length; i++) {
            if (parents[i] == parent) {
                count++;
            }
        }
        return count;
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
    
    @Override
    public String toString() {
        return nodes.toString();
    }

    public static Collection<Dependency<Tree, Tree, Object>> mapDependencies(Tree tree, HeadFinder hf) {
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

    public List<DepTreeNode> getNodes() {
        return nodes;
    }

    public Iterator<DepTreeNode> iterator() {
        return nodes.iterator();
    }
    
    /**
     * For testing only.
     * @return
     */
    public int[] getParents() {
        return parents;
    }
    
    public int getNumTokens() {
        return parents.length;
    }

    public static class HeadFinderException extends RuntimeException {

        public HeadFinderException(String string) {
            super(string);
        }

        private static final long serialVersionUID = 1L;
        
    }
    
}
