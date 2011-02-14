package edu.jhu.hltcoe.parse;

import java.util.Map;

import edu.jhu.hltcoe.data.DepTree;
import edu.jhu.hltcoe.data.Sentence;
import edu.jhu.hltcoe.ilp.GurobiIlpSolver;
import edu.jhu.hltcoe.ilp.IlpSolver;
import edu.jhu.hltcoe.model.Model;

public class IlpViterbiParser implements ViterbiParser {

    private static final String ZIMPL_CODE_XML = "/edu/jhu/hltcoe/parse/zimpl_dep_parse.xml";
    private Map<String,String> codeMap;
    
    public IlpViterbiParser() {
        XmlCodeContainerReader reader = new XmlCodeContainerReader();
        reader.loadZimplCodeFromResource(ZIMPL_CODE_XML);
        codeMap = reader.getCodeMap();
    }
    
    @Override
    public DepTree getViterbiParse(Sentence sentence, Model model) {
        // Create workspace
        
        // Encode sentence
        
        // Encode model
        
        // Create .zpl file
        String zimplFile = null;
        
        // Run zimpl
        // Run ILP solver
        IlpSolver ilpSolver = new GurobiIlpSolver(zimplFile);
        ilpSolver.solve();
        Map<String,String> result = ilpSolver.getResult();
        
        // Decode parses
        DepTree tree = null;
        
        return tree;
    }
    
    public static void main(String[] args) {
        new IlpViterbiParser();
    }
}
