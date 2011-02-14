package edu.jhu.hltcoe.parse;

import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import edu.jhu.hltcoe.data.DepTree;
import edu.jhu.hltcoe.data.Sentence;
import edu.jhu.hltcoe.ilp.GurobiIlpSolver;
import edu.jhu.hltcoe.ilp.IlpSolver;
import edu.jhu.hltcoe.model.Model;

public class IlpViterbiParser implements ViterbiParser {

    private static final String ZIMPL_CODE_XML = "/edu/jhu/hltcoe/parse/zimpl_dep_parse.xml";
    private Map<String,String> codeMap;
    private final Pattern zimplVarRegex = Pattern.compile("[#$]");

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
        DepTree tree = decode(sentence, result);
        return tree;
    }
    
    private DepTree decode(Sentence sentence, Map<String, String> result) {
        int[] parents = new int[sentence.size()];
        for (Entry<String,String> entry : result.entrySet()) {
            String zimplVar = entry.getKey();
            String value = entry.getValue();
            String[] splits = zimplVarRegex.split(zimplVar);
            String varType = splits[0];
            if (varType.equals("arc")) {
                // Unused: int sentId = Integer.parseInt(splits[1]);
                int parent = Integer.parseInt(splits[2]);
                int child = Integer.parseInt(splits[3]);
                if (Integer.parseInt(value) == 1) {
                    parents[child] = parent;
                }
            }
        }
        DepTree tree = new DepTree(sentence, parents);
        return tree;
    }

    public static void main(String[] args) {
        new IlpViterbiParser();
    }
}
