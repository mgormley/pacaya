package edu.jhu.nlp.features;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.jhu.nlp.features.TemplateLanguage.EdgeProperty;
import edu.jhu.nlp.features.TemplateLanguage.FeatTemplate;
import edu.jhu.nlp.features.TemplateLanguage.FeatTemplate0;
import edu.jhu.nlp.features.TemplateLanguage.FeatTemplate1;
import edu.jhu.nlp.features.TemplateLanguage.FeatTemplate2;
import edu.jhu.nlp.features.TemplateLanguage.FeatTemplate3;
import edu.jhu.nlp.features.TemplateLanguage.FeatTemplate4;
import edu.jhu.nlp.features.TemplateLanguage.JoinTemplate;
import edu.jhu.nlp.features.TemplateLanguage.ListModifier;
import edu.jhu.nlp.features.TemplateLanguage.OtherFeat;
import edu.jhu.nlp.features.TemplateLanguage.Position;
import edu.jhu.nlp.features.TemplateLanguage.PositionList;
import edu.jhu.nlp.features.TemplateLanguage.PositionModifier;
import edu.jhu.nlp.features.TemplateLanguage.RulePiece;
import edu.jhu.nlp.features.TemplateLanguage.SymbolProperty;
import edu.jhu.nlp.features.TemplateLanguage.TokPropList;
import edu.jhu.nlp.features.TemplateLanguage.TokProperty;

/**
 * Reader for the template little language.
 * 
 * @author mgormley
 */
public class TemplateReader {

    private static final Pattern STRUCTURE_SEP_REGEX = Pattern.compile(Pattern.quote(TemplateLanguage.STRUCTURE_SEP));
    private static final Pattern TEMPLATE_SEP_REGEX = Pattern.compile("\\s*"+Pattern.quote(TemplateLanguage.TEMPLATE_SEP)+"\\s*");

    private static final Pattern comment = Pattern.compile("\\s*#.*");
        
    private List<FeatTemplate> tpls = new ArrayList<FeatTemplate>();
    
    public TemplateReader() {        
    }

    public void readFromFile(String path) throws IOException {
        read(new FileInputStream(path));
    }
    
    public void readFromResource(String resourceName) throws IOException {
        InputStream inputStream = this.getClass().getResourceAsStream(resourceName);
        if (inputStream == null) {
            throw new IOException("Unable to find resource: " + resourceName);
        }
        read(inputStream);
    }
    
    public void read(InputStream in) throws IOException {
        BufferedReader input = new BufferedReader(new InputStreamReader(in));        
        String line;
        while ((line = input.readLine()) != null) {
            readLine(line);
        }
        in.close();
    }
    
    private void readLine(String line) {
        // Remove comments
        Matcher cmatch = comment.matcher(line);
        line = cmatch.replaceAll("");
        // Trim whitespace
        line = line.trim();
        if (line.equals("")) {
            // Skip this empty line.
            return;
        }
        
        // Get each singleton feature separated by +.
        String[] templates = TEMPLATE_SEP_REGEX.split(line);
        FeatTemplate[] tplArray = new FeatTemplate[templates.length];
        for (int i=0; i<templates.length; i++) {
            tplArray[i] = getSingletonFeatTemplate(templates[i]);
        }
        
        // Add the final feature template.
        if (tplArray.length == 1) {
            tpls.add(tplArray[0]);            
        } else {
            tpls.add(new JoinTemplate(tplArray));
        }
    }

    private FeatTemplate getSingletonFeatTemplate(String t) {
        TemplateStruct s = TemplateStruct.readTree(t);
                    
        // Get all of the individual pieces by looking each up by its string name
        // in {@link TemplateLanguage#getDescByName}.
        Position pos = safeGet(s, Position.class);
        PositionList pl = safeGet(s, PositionList.class);
        TokProperty prop = safeGet(s, TokProperty.class);
        TokPropList propl = safeGet(s, TokPropList.class);
        PositionModifier mod =safeGet(s, PositionModifier.class);
        ListModifier lmod = safeGet(s, ListModifier.class);
        EdgeProperty eprop = safeGet(s, EdgeProperty.class);
        RulePiece rpiece = safeGet(s, RulePiece.class);
        SymbolProperty rprop = safeGet(s, SymbolProperty.class);
        OtherFeat other = safeGet(s, OtherFeat.class);
                
        if (pos != null && pl != null) {
            throw new IllegalStateException("Both position and position list cannot be specified: " + t);
        }
        
        // Try to create a template from the available pieces.
        FeatTemplate tpl;
        if (pos != null && prop != null) {
            mod = (mod == null) ? PositionModifier.IDENTITY : mod;  
            tpl = new FeatTemplate1(pos, mod, prop);                    
        } else if (pos != null && propl != null) {
            mod = (mod == null) ? PositionModifier.IDENTITY : mod;  
            tpl = new FeatTemplate2(pos, mod, propl);
        } else if (pl != null) {
            lmod = (lmod == null) ? ListModifier.SEQ : lmod;  
            tpl = new FeatTemplate3(pl, prop, eprop, lmod);
        } else if (rpiece != null && rprop != null) {                
            tpl = new FeatTemplate4(rpiece, rprop);
        } else if (other != null) {
            tpl = new FeatTemplate0(other);
        } else {
            throw new IllegalStateException("Invalid template: " + t);
        }
        return tpl;
    }
    
    //@SuppressWarnings("unchecked")
    private <T> T safeGet(TemplateStruct p, Class<T> class1) {
        if (class1.isInstance(p.type)) {
            return (T) p.type;
        }
        for (TemplateStruct c : p.deps) {
            T t = safeGet(c, class1);
            if (t != null) {
                return t;
            }
        }
        return null;
    }

    public List<FeatTemplate> getTemplates() {
        return tpls;
    }
    
    public static FeatTemplate lineToFeatTemplate(String line) {
        TemplateReader tr = new TemplateReader();
        tr.readLine(line);
        if (tr.getTemplates().size() > 0) {
            return tr.getTemplates().get(0);
        } else {
            return null;
        }
    }
    
}
