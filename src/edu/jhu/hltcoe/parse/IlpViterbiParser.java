package edu.jhu.hltcoe.parse;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import edu.jhu.hltcoe.data.DepTree;
import edu.jhu.hltcoe.data.Sentence;
import edu.jhu.hltcoe.model.Model;

public class IlpViterbiParser implements ViterbiParser {

    private static final String ZIMPL_CODE_XML = "/edu/jhu/hltcoe/parse/zimpl_dep_parse.xml";
    private Map<String,String> codeMap = new HashMap<String,String>();
    
    public IlpViterbiParser() {
        loadZimplCodeFromResource(ZIMPL_CODE_XML);
    }
    
    @Override
    public DepTree getViterbiParse(Sentence sentence, Model model) {
        // TODO Auto-generated method stub
        return null;
    }
    

    private void loadZimplCodeFromResource(String resourceName) {
        try {
            InputStream inputStream = this.getClass().getResourceAsStream(resourceName);
            if (inputStream == null) {
                throw new RuntimeException("Unable to find resource: " + resourceName);
            }
            
            DocumentBuilderFactory builderFactory =
                DocumentBuilderFactory.newInstance();
            DocumentBuilder builder;
            try {
                builder = builderFactory.newDocumentBuilder();
            } catch (ParserConfigurationException e) {
                throw new RuntimeException(e);
            }
            Document document;
            try {
                document = builder.parse(inputStream);
            } catch (SAXException e) {
                throw new RuntimeException(e);
            }
                    
            loadZimplCodeFromXml(document);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    private void loadZimplCodeFromXml(Document document) {
        NodeList codeNodes = document.getElementsByTagName("code");
        for (int i=0; i<codeNodes.getLength(); i++) {
            Node codeNode = codeNodes.item(i);
            Node idNode = codeNode.getAttributes().getNamedItem("id");
            String id = idNode.getNodeValue();
            
            NodeList codeChildren = codeNode.getChildNodes();
            for (int j=0; j<codeChildren.getLength(); j++) {
                Node codeChild = codeChildren.item(j);
                if (codeChild.getNodeType() == codeChild.CDATA_SECTION_NODE) {
                    String zimplCode = codeChild.getNodeValue();
                    
                    // Put the <id, zimplCode> pair into the codeMap
                    codeMap.put(id, zimplCode);
                }
            }
        }
    }

    public static void main(String[] args) {
        new IlpViterbiParser();
    }
}
