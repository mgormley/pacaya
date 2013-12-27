package edu.jhu.featurize;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
import edu.jhu.featurize.TemplateLanguage.FeatTemplate1;
import edu.jhu.featurize.TemplateLanguage.FeatTemplate2;
import edu.jhu.featurize.TemplateLanguage.FeatTemplate3;
import edu.jhu.featurize.TemplateLanguage.FeatTemplate4;
import edu.jhu.featurize.TemplateLanguage.ListModifier;
import edu.jhu.featurize.TemplateLanguage.OtherFeat;
import edu.jhu.featurize.TemplateLanguage.Position;
import edu.jhu.featurize.TemplateLanguage.PositionList;
import edu.jhu.featurize.TemplateLanguage.PositionModifier;
import edu.jhu.featurize.TemplateLanguage.TokPropList;
import edu.jhu.featurize.TemplateLanguage.TokProperty;

/**
 * Reader for the template little language.
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
        
        
        String[] templates = TEMPLATE_SEP_REGEX.split(line);
        for (String t : templates) {
            String[] structures = STRUCTURE_SEP_REGEX.split(t);
            List<Description> descs = new ArrayList<Description>();
            for (String s : structures) {
                Description desc = TemplateLanguage.getDescByName(s);
                if (desc == null) {
                    throw new IllegalStateException("Unknown name: " + s);
                }
                descs.add(desc);
            }
                        
            Position pos = safeGet(descs, Position.class);
            PositionList pl = safeGet(descs, PositionList.class);
            TokProperty prop = safeGet(descs, TokProperty.class);
            TokPropList propl = safeGet(descs, TokPropList.class);
            PositionModifier mod =safeGet(descs, PositionModifier.class);
            ListModifier lmod = safeGet(descs, ListModifier.class);
            EdgeProperty eprop = safeGet(descs, EdgeProperty.class);
            OtherFeat other = safeGet(descs, OtherFeat.class);
            
            if (pos != null && pl != null) {
                throw new IllegalStateException("Both position and position list cannot be specified: " + line);
            }
            
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
            } else if (other != null) {
                tpl = new FeatTemplate4(other);
            } else {
                throw new IllegalStateException("Invalid template: " + t);
            }
                    
            tpls.add(tpl);
        }
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
    
}
