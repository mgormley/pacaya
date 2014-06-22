package edu.jhu.featurize;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;

import edu.jhu.featurize.TemplateLanguage.FeatTemplate;

public class TemplateWriter {

    public static void write(File outFile, List<FeatTemplate> tpls) throws IOException {
        Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outFile), "UTF-8"));
        for (FeatTemplate tpl : tpls) {
            writer.write(tpl.getName());
            writer.write("\n");
        }
        writer.close();
    }

}
