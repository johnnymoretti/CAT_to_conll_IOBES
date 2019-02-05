package eu.fbk.dh.CAT_to_Conll_IOBES;

import com.google.common.base.Joiner;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.nio.file.Paths;
import java.util.*;

/**
 * Created by giovannimoretti on 07/11/17.
 */
public class Main {
    public static void main(String[] args) {


        try {
            java.nio.file.Files.walk(Paths.get(args[0])).parallel()
                    .filter(p -> p.toString().endsWith(".xml"))
                    .forEach(filePath -> {
                        try {
                            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                            XPathFactory xPathfactory = XPathFactory.newInstance();
                            XPath xpath = xPathfactory.newXPath();

                            InputStream stream = new FileInputStream(filePath.toFile());

                            Document doc = dBuilder.parse(stream);
                            doc.getDocumentElement().normalize();

                            XPathExpression expr;
                            NodeList tokens, markables;


                            expr = xpath.compile("/Document");


                            Node doc_node = (Node) expr.evaluate(doc, XPathConstants.NODE);
                            Element elem = (Element) doc_node;

                            String fname = elem.getAttribute("doc_name");

                            System.out.println(fname);
                            if (!fname.endsWith(".txt")){
                                fname = fname+".txt";
                            }
                            expr = xpath.compile("/Document//token");
                            tokens = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);

                            expr = xpath.compile("/Document/Markables/*");
                            markables = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);

                            SortedSet<String> markableTags = new TreeSet<>();

                            for (int i = 0; i < markables.getLength(); i++) {
                                Node fileNode = markables.item(i);
                                markableTags.add(((Element) fileNode).getTagName());
                            }

                            Map<String, String> annotated_tokens = new HashMap<>();


                            for (int i = 0; i < markables.getLength(); i++) {
                                Node fileNode = markables.item(i);

                                expr = xpath.compile("./token_anchor");
                                NodeList token_anchor = (NodeList) expr.evaluate(fileNode, XPathConstants.NODESET);

                                if (token_anchor.getLength() == 1){
                                    String tagname = ((Element) fileNode).getTagName();
                                    String ref_id = ((Element) token_anchor.item(0)).getAttribute("t_id");
                                    annotated_tokens.put(tagname + ref_id, "S-" + tagname +"\n" );
                                }else{
                                    for (int anchor_id = 0; anchor_id < token_anchor.getLength(); anchor_id++) {
                                        String tagname = ((Element) fileNode).getTagName();
                                        String ref_id = ((Element) token_anchor.item(anchor_id)).getAttribute("t_id");
                                        annotated_tokens.put(tagname + ref_id, anchor_id == 0 ? "B-" + tagname : (anchor_id == (token_anchor.getLength() -1) ? "E-" + tagname +"\n" : "I-" + tagname));
                                    }
                                }



                            }
                            StringBuffer sb = new StringBuffer();
                            //   System.out.println(annotated_tokens);
                            Integer feat_num = markableTags.size();
                            int current_sentence = 0;

                            ArrayList<String> blacklist = new ArrayList<>();
                            blacklist.add("LOCATION");
                            blacklist.add("TIMEX3");


                            boolean isInside = false;

                            for (int i = 0; i < tokens.getLength(); i++) {
                                Node fileNode = tokens.item(i);
                                Element tok_elem = ((Element) fileNode);
                                Integer sentence_number = Integer.parseInt(tok_elem.getAttribute("sentence"));

                                LinkedList<String> tok = new LinkedList<>();
                                tok.add(tok_elem.getTextContent());
                                tok.add("O"); // add for custom version
                                for (String s : markableTags) {

                                    if (blacklist.contains(s)){ //add for custom
                                        tok.set(1,"O"); //add for custom
                                        continue; //add for custom
                                    } //add for custom

                                    tok.set(1, annotated_tokens.containsKey(s + tok_elem.getAttribute("t_id")) ? annotated_tokens.get(s + tok_elem.getAttribute("t_id")) : "O");
                                    if (annotated_tokens.containsKey(s + tok_elem.getAttribute("t_id"))) { // add for custom
                                        break; // add for custom
                                    } // add for custom
                                }

//                                if (current_sentence != sentence_number) {
//                                    current_sentence = sentence_number;
//                                    sb.append("\n");
//                                }


                                String prepend ="";
                                if (isInside && tok.get(1).startsWith("B-") ){
                                    prepend = "\n";
                                }


                                sb.append( prepend + Joiner.on("\t").skipNulls().join(tok) + "\n");

                                if (tok.get(1).startsWith("I-") || tok.get(1).equals("O")) {
                                    isInside = true;
                                }else{
                                    isInside = false;
                                }


                            }

                            Writer out = new BufferedWriter(new OutputStreamWriter(
                                    new FileOutputStream(args[0] + "/" + fname), "UTF-8"));
                            try {
                                out.write(sb.toString());
                            } finally {
                                out.close();
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
