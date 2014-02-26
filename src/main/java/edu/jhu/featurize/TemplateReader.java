package edu.jhu.featurize;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.jhu.featurize.TemplateLanguage.Description;
import edu.jhu.featurize.TemplateLanguage.EdgeProperty;
import edu.jhu.featurize.TemplateLanguage.FeatTemplate;
import edu.jhu.featurize.TemplateLanguage.FeatTemplate0;
import edu.jhu.featurize.TemplateLanguage.FeatTemplate1;
import edu.jhu.featurize.TemplateLanguage.FeatTemplate2;
import edu.jhu.featurize.TemplateLanguage.FeatTemplate3;
import edu.jhu.featurize.TemplateLanguage.FeatTemplate4;
import edu.jhu.featurize.TemplateLanguage.JoinTemplate;
import edu.jhu.featurize.TemplateLanguage.ListModifier;
import edu.jhu.featurize.TemplateLanguage.OtherFeat;
import edu.jhu.featurize.TemplateLanguage.Position;
import edu.jhu.featurize.TemplateLanguage.PositionList;
import edu.jhu.featurize.TemplateLanguage.PositionModifier;
import edu.jhu.featurize.TemplateLanguage.RulePiece;
import edu.jhu.featurize.TemplateLanguage.SymbolProperty;
import edu.jhu.featurize.TemplateLanguage.TokPropList;
import edu.jhu.featurize.TemplateLanguage.TokProperty;

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
        List<Description> descs = new ArrayList<Description>();
        Description desc = TemplateLanguage.getDescByName(t);
        if (desc != null) {
            descs.add(desc);
        } else {
            String[] structures = STRUCTURE_SEP_REGEX.split(t);
            for (String s : structures) {
                desc = TemplateLanguage.getDescByName(s);
                if (desc == null) {
                    throw new IllegalStateException("Unknown name: " + s);
                }
                descs.add(desc);
            }
        }
                    
        // Get all of the individual pieces by looking each up by its string name
        // in {@link TemplateLanguage#getDescByName}.
        Position pos = safeGet(descs, Position.class);
        PositionList pl = safeGet(descs, PositionList.class);
        TokProperty prop = safeGet(descs, TokProperty.class);
        TokPropList propl = safeGet(descs, TokPropList.class);
        PositionModifier mod =safeGet(descs, PositionModifier.class);
        ListModifier lmod = safeGet(descs, ListModifier.class);
        EdgeProperty eprop = safeGet(descs, EdgeProperty.class);
        RulePiece rpiece = safeGet(descs, RulePiece.class);
        SymbolProperty rprop = safeGet(descs, SymbolProperty.class);
        OtherFeat other = safeGet(descs, OtherFeat.class);
                
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
    
    @SuppressWarnings("unchecked")
    private <T> T safeGet(List<Description> descs, Class<T> class1) {
        for (Description d : descs) {
            if (class1.isInstance(d.getObj())) {
                return (T) d.getObj();
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
