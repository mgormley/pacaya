package edu.jhu.hltcoe.parse;

import java.util.regex.Pattern;

public class ZimplXmlCodeContainerReader extends XmlCodeContainerReader {

    private static Pattern priorityPattern = Pattern.compile("priority \\d+");
    private IlpFormulation formulation;;
    
    public ZimplXmlCodeContainerReader(IlpFormulation formulation) {
        this.formulation = formulation;
    }

    /**
     * Gets a code snippet with the specified id
     */
    @Override
    public String getCodeSnippet(Object id) {
        String codeSnippet;
        if (id instanceof IlpFormulation) {
            codeSnippet = super.getCodeSnippet(id.toString());
        } else {
            codeSnippet = super.getCodeSnippet(id);
        }
        
        // Convert to the LP Relaxation automatically
        if (formulation.isLpRelaxation()) {
            codeSnippet = codeSnippet.replace(" binary", " real >= 0 <= 1");
            codeSnippet = codeSnippet.replace(" integer", " real");
            codeSnippet = priorityPattern.matcher(codeSnippet).replaceAll("");
        }
        
        return codeSnippet;
    }
    
}
