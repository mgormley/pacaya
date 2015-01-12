package edu.jhu.nlp.features;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;

import edu.jhu.nlp.features.TemplateLanguage.FeatTemplate;

public class TemplateWriter {

    public static void write(File outFile, List<FeatTemplate> tpls) {
        try {
            Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outFile), "UTF-8"));
            for (FeatTemplate tpl : tpls) {
                writer.write(tpl.getName());
                writer.write("\n");
            }
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
