package edu.jhu.hltcoe.data;


public class Ptb45To17TagReducer extends AbstractTagReducer implements TagReducer {
        
    /**
     * Ported to Java from Jason Smith's reduce-tags.pl.
     */
    @Override
    public String reduceTag(String tag) {
        if ("``".equals(tag)) { return "LPUNC"; }
        else if (",".equals(tag)) { return "INPUNC"; }
        else if (":".equals(tag)) { return "INPUNC"; }
        else if (".".equals(tag)) { return "ENDPUNC"; }
        else if ("$".equals(tag)) { return "ADJ"; }
        else if ("''".equals(tag)) { return "RPUNC"; }
        else if ("#".equals(tag)) { return "ADJ"; }
        else if ("CC".equals(tag)) { return "CONJ"; }
        else if ("CD".equals(tag)) { return "ADJ"; }
        else if ("DT".equals(tag)) { return "DET"; }
        else if ("EX".equals(tag)) { return "N"; }
        else if ("FW".equals(tag)) { return "N"; }
        else if ("IN".equals(tag)) { return "PREP"; }
        else if ("JJ".equals(tag)) { return "ADJ"; }
        else if ("JJR".equals(tag)) { return "ADJ"; }
        else if ("JJS".equals(tag)) { return "ADJ"; }
        else if ("-LRB-".equals(tag)) { return "LPUNC"; }
        else if ("LS".equals(tag)) { return "INPUNC"; }
        else if ("MD".equals(tag)) { return "V"; }
        else if ("NN".equals(tag)) { return "N"; }
        else if ("NNP".equals(tag)) { return "N"; }
        else if ("NNPS".equals(tag)) { return "N"; }
        else if ("NNS".equals(tag)) { return "N"; }
        else if ("PDT".equals(tag)) { return "DET"; }
        else if ("POS".equals(tag)) { return "POS"; }
        else if ("PRP".equals(tag)) { return "N"; }
        else if ("PRP$".equals(tag)) { return "ADJ"; }
        else if ("RB".equals(tag)) { return "ADV"; }
        else if ("RBR".equals(tag)) { return "ADV"; }
        else if ("RBS".equals(tag)) { return "ADV"; }
        else if ("RP".equals(tag)) { return "PRT"; }
        else if ("-RRB-".equals(tag)) { return "RPUNC"; }
        else if ("SYM".equals(tag)) { return "INPUNC"; }
        else if ("TO".equals(tag)) { return "TO"; }
        else if ("UH".equals(tag)) { return "INPUNC"; }
        else if ("VB".equals(tag)) { return "V"; }
        else if ("VBD".equals(tag)) { return "V"; }
        else if ("VBG".equals(tag)) { return "VBG"; }
        else if ("VBN".equals(tag)) { return "VBN"; }
        else if ("VBP".equals(tag)) { return "V"; }
        else if ("VBZ".equals(tag)) { return "V"; }
        else if ("WDT".equals(tag)) { return "W"; }
        else if ("WP".equals(tag)) { return "W"; }
        else if ("WP$".equals(tag)) { return "W"; }
        else if ("WRB".equals(tag)) { return "W"; }
        else { 
            return null;
        }
    }
    
}
