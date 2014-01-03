package edu.jhu.data;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import edu.jhu.prim.list.IntArrayList;
import edu.jhu.prim.set.IntHashSet;
import edu.jhu.prim.tuple.Pair;
import edu.jhu.util.Alphabet;

public class DepTree implements Iterable<DepTreeNode> {

    private static final Logger log = Logger.getLogger(DepTree.class);
    public static final int EMPTY_POSITION = -2;
    
    protected List<DepTreeNode> nodes = new ArrayList<DepTreeNode>();
    protected int[] parents;
    protected boolean isProjective;
    
    protected DepTree() {
        // Only for subclasses.
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

    protected DepTreeNode getNodeByPosition(int position) {
        return nodes.get(position+1);
    }
    
    protected void addParentChildLinksToNodes() {
        checkTree();
        for (int i=0; i<parents.length; i++) {
            NonprojDepTreeNode child = (NonprojDepTreeNode)getNodeByPosition(i);
            NonprojDepTreeNode parent = (NonprojDepTreeNode)getNodeByPosition(parents[i]);
            child.setParent(parent);
            parent.addChild(child);
        }
    }

    protected void checkTree() {
        // Check that there is exactly one node with the WALL as its parent
        int emptyCount = countChildrenOf(parents, EMPTY_POSITION);
        if (emptyCount != 0) {
            throw new IllegalStateException("Found an empty parent cell. emptyCount=" + emptyCount);
        }
        int wallCount = countChildrenOf(parents, WallDepTreeNode.WALL_POSITION);
        if (wallCount != 1) {
            log.warn("There must be exactly one node with the wall as a parent. wallCount=" + wallCount);
        }
        
        // Check that there are no cyles
        if (containsCycle(parents)) {
            throw new IllegalStateException("Found cycle in parents array");
        }

        // Check for proper list lengths
        if (nodes.size()-1 != parents.length) {
            throw new IllegalStateException("Number of nodes does not equal number of parents");
        }
        
        // Check for projectivity if necessary
        if (isProjective) {
            if (!checkIsProjective(parents)) {
                throw new IllegalStateException("Found non-projective arcs in tree");
            }
        }
    }

    @Override
    public String toString() {
        return nodes.toString();
    }

    public DepTreeNode getWallNode() {
        return nodes.get(0);
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

    public Sentence getSentence(Alphabet<Label> alphabet) {
        return new Sentence(alphabet, this);
    }

    public enum Dir {
        UP, DOWN, NONE
    }
    
    /**
     * Gets the shortest dependency path between two tokens.
     * 
     * <p>
     * For the tree: x0 <-- x1 --> x2, represented by parents=[1, -1, 1] the
     * dependency path from x0 to x2 would be a list [(0, UP), (1, DOWN), (2, NONE)]
     * </p>
     * 
     * @param start The position of the start token.
     * @param end The position of the end token.
     * @param parents The parents array.
     * @return The path as a list of pairs containing the word positions and the
     *         direction of the edge, inclusive of the start and end.
     */
    public static List<Pair<Integer,Dir>> getDependencyPath(int start, int end, int[] parents) {
        int n = parents.length;
        if (start < -1 || start >= n || end < -1 || end >= n) {
            throw new IllegalArgumentException(String.format("Invalid start/end: %d/%d", start, end));
        }
        
        // Build a hash set of the ancestors of end, including end and the
        // wall node.
        IntHashSet endAncSet = new IntHashSet();
        IntArrayList endAncList = new IntArrayList();
        int curPos = end;
        while (curPos != WallDepTreeNode.WALL_POSITION) {
            endAncSet.add(curPos);
            endAncList.add(curPos);
            curPos = parents[curPos];
        }
        endAncSet.add(curPos); // Don't forget the wall node.
        endAncList.add(curPos);
        
        // Create the dependency path.
        List<Pair<Integer,Dir>> path = new ArrayList<Pair<Integer,Dir>>();
        
        // Add all the "edges" from the start up to the one pointing at the LCA.
        curPos = start;
        while (!endAncSet.contains(curPos)) {
            path.add(new Pair<Integer,Dir>(curPos, Dir.UP));
            curPos = parents[curPos];
        }

        // Least common ancestor.
        int lca = curPos;
        
        // Add all the edges from the LCA to the end position.
        int lcaIndex = endAncList.lookupIndex(lca);
        for (int i = lcaIndex; i > 0; i--) {
            path.add(new Pair<Integer,Dir>(endAncList.get(i), Dir.DOWN));
        }
        
        // TODO: Update unit tests to reflect this change.
        path.add(new Pair<Integer,Dir>(end, Dir.NONE));
        
        return path;
    }

    /**
     * Gets the siblings of the specified word.
     * @param parents The parents array.
     * @param idx The word for which to extract siblings.
     * @return The indices of the siblings.
     */
    public static ArrayList<Integer> getSiblingsOf(int[] parents, int idx) {
        int parent = parents[idx];
        ArrayList<Integer> siblings = new ArrayList<Integer>();
        for (int i=0; i<parents.length; i++) {
            if (parents[i] == parent) {
                siblings.add(i);
            }
        }
        return siblings;
    }

    /**
     * Checks if a dependency tree represented as a parents array contains a cycle.
     * 
     * @param parents
     *            A parents array where parents[i] contains the index of the
     *            parent of the word at position i, with parents[i] = -1
     *            indicating that the parent of word i is the wall node.
     * @return True if the tree specified by the parents array contains a cycle,
     *         False otherwise.
     */
    public static boolean containsCycle(int[] parents) {
        for (int i=0; i<parents.length; i++) {
            int numAncestors = 0;
            int parent = parents[i];
            while(parent != WallDepTreeNode.WALL_POSITION) {
                numAncestors += 1;
                if (numAncestors > parents.length - 1) {
                    return true;
                }
                parent = parents[parent];
            }
        }
        return false;
    }

    /**
     * Checks that a dependency tree represented as a parents array is projective.
     * 
     * @param parents
     *            A parents array where parents[i] contains the index of the
     *            parent of the word at position i, with parents[i] = -1
     *            indicating that the parent of word i is the wall node.
     * @return True if the tree specified by the parents array is projective,
     *         False otherwise.
     */
    public static boolean checkIsProjective(int[] parents) {
        for (int i=0; i<parents.length; i++) {
            int pari = (parents[i] == WallDepTreeNode.WALL_POSITION) ? parents.length : parents[i];
            int minI = i < pari ? i : pari;
            int maxI = i > pari ? i : pari;
            for (int j=0; j<parents.length; j++) {
                if (j == i) {
                    continue;
                }
                int parj = (parents[j] == WallDepTreeNode.WALL_POSITION) ? parents.length : parents[j];
                if (minI < j && j < maxI) {
                    if (!(minI <= parj && parj <= maxI)) {
                        return false;
                    }
                } else {
                    if (!(parj <= minI || parj >= maxI)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * Counts of the number of children in a dependency tree for the given
     * parent index.
     * 
     * @param parents
     *            A parents array where parents[i] contains the index of the
     *            parent of the word at position i, with parents[i] = -1
     *            indicating that the parent of word i is the wall node.
     * @param parent The parent for which the children should be counted.
     * @return The number of entries in <code>parents</code> that equal
     *         <code>parent</code>.
     */
    public static int countChildrenOf(int[] parents, int parent) {
        int count = 0;
        for (int i=0; i<parents.length; i++) {
            if (parents[i] == parent) {
                count++;
            }
        }
        return count;
    }

    /**
     * Gets the children of the specified parent.
     * @param parents A parents array.
     * @param parent The parent for which the children should be  extracted.
     * @return The indices of the children.
     */
    public static ArrayList<Integer> getChildrenOf(int[] parents, int parent) {
        ArrayList<Integer> children = new ArrayList<Integer>();
        for (int i=0; i<parents.length; i++) {
            if (parents[i] == parent) {
                children.add(i);
            }
        }
        return children;
    }

    /**
     * Checks whether idx1 is the ancestor of idx2. If idx1 is the parent of
     * idx2 this will return true, but if idx1 == idx2, it will return false.
     * 
     * @param idx1 The ancestor position.
     * @param idx2 The descendent position.
     * @param parents The parents array.
     * @return Whether idx is the ancestor of idx2.
     */
    public static boolean isAncestor(int idx1, int idx2, int[] parents) {
        int anc = parents[idx2];
        while (anc != -1) {
            if (anc == idx1) {
                return true;
            }
            anc = parents[anc];
        }
        return false;
    }
    
    
}