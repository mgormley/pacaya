package edu.jhu.nlp.joint;

import edu.jhu.autodiff.erma.InsideOutsideDepParse;
import edu.jhu.nlp.Annotator;
import edu.jhu.nlp.data.simple.AnnoSentenceCollection;
import edu.jhu.prim.util.math.FastMath;

/**
 * TODO: Deprecate this class. This is only a hold over until we remove the dependence of
 * CommunicationsAnnotator on these options being correctly set.
 * 
 * @author mgormley
 */
public class EnsureStaticOptionsAreSet implements Annotator {        
    private static final long serialVersionUID = 1L;
    private final boolean singleRoot = InsideOutsideDepParse.singleRoot;
    private final boolean useLogAddTable = JointNlpRunner.useLogAddTable;
    @Override
    public void annotate(AnnoSentenceCollection sents) {
        InsideOutsideDepParse.singleRoot = singleRoot;
        FastMath.useLogAddTable = useLogAddTable;
    }
}