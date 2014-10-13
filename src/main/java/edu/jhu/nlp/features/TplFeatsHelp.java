package edu.jhu.nlp.features;

import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.junit.Test;

import edu.jhu.nlp.features.TemplateLanguage.FeatTemplate;

public class TplFeatsHelp {

    @Test
    public void testGetAll() throws Exception {
        List<FeatTemplate> tpls;
        
        tpls = TemplateSets.getAllUnigramFeatureTemplates();
        int numUnigrams = tpls.size();
        System.out.println("Number of unigram templates: " + numUnigrams);  
        
        BufferedWriter w = Files.newBufferedWriter(Paths.get("/Users/mgormley/research/pacaya/tmp/v2.txt"), StandardCharsets.UTF_8);
        int i=0;
        for (FeatTemplate ft : tpls) {
            if (i<10) {
                System.out.println(ft);
            }
            w.write(ft.getName());
            w.write("\n");
            i++;
        }
        w.close();
    }
    
}
