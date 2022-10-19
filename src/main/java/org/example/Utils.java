package org.example;

import org.json.JSONObject;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Utils {
    public static Element createXmlRoot(String xml) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        ByteArrayInputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
        return builder.parse(input).getDocumentElement();
    }

    public static void writeJSONToFile(String name, JSONObject jsonObject) {
        try {
            Files.writeString(Paths.get(name), jsonObject.toString(4));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
