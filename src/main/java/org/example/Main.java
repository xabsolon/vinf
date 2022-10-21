package org.example;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.json.JSONObject;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

    private static String findAlternativeNamesInInfobox(String infobox) {
        Pattern pattern = Pattern.compile("\\| iny_nazov = * *(?<nazov>[^|]+)([ \\n])*\\|", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(infobox);
        if(matcher.find()) {
            String group = matcher.group("nazov").trim();
            if(!group.isEmpty())
                return group;
        }
        return null;
    }
    private static List<String> findAlternativeNamesInRevision(Element revision) {
        String textContent = revision.getElementsByTagName("text").item(0).getTextContent();
        List<String> matches = Pattern.compile("\\{\\{ *Infobox(?:[^}{]+)*+}}", Pattern.CASE_INSENSITIVE)
                .matcher(textContent)
                .results()
                .map(MatchResult::group).toList();

        List<String> alternativeNames = new ArrayList<>();
        for (String match : matches) {
            String name = findAlternativeNamesInInfobox(match);
            if(name != null) alternativeNames.add(name);
        }

        return alternativeNames;
    }
    private static PageInfo processPage(String page) throws ParserConfigurationException, IOException, SAXException {
        Element root = Utils.createXmlRoot(page);
        String pageId = root.getElementsByTagName("id").item(0).getTextContent();
        String pageTitle = root.getElementsByTagName("title").item(0).getTextContent();
        NodeList revisions = root.getElementsByTagName("revision");
        List<String> alternativeNames = findAlternativeNamesInRevision((Element) revisions.item(0));
        return new PageInfo(pageId, pageTitle, alternativeNames);
    }

    private static InputStream loadBzip2File(String fileName) {
        try {
            FileInputStream fileInputStream = new FileInputStream(fileName);
            return new BZip2CompressorInputStream(fileInputStream, true);
        } catch (FileNotFoundException e) {
            System.out.println("Failed to load file.");
            e.printStackTrace();
            System.exit(1);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        try {
            InputStream inputStream = loadBzip2File("skwiki-latest.xml.bz2");
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            JSONObject jsonObject = new JSONObject();

            StringBuilder page = new StringBuilder();
            boolean isPage = false;
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("<page>")) {
                    isPage = true;
                    page.setLength(0);
                    page.append(line);
                    continue;
                }
                else if (line.contains("</page>")) {
                    isPage = false;
                    page.append(line);
                    PageInfo pageInfo = processPage(page.toString());
                    List<String> alternativeNames = pageInfo.getAlternativeNames();
                    if(!alternativeNames.isEmpty()) {
                        JSONObject pageInfoJsonObject = new JSONObject();
                        pageInfoJsonObject.put("title", pageInfo.getTitle());
                        pageInfoJsonObject.put("alternativeNames", alternativeNames);
                        jsonObject.put(pageInfo.getId(), pageInfoJsonObject);
                    }
                    continue;
                }

                if (isPage) page.append(line);
            }
            Utils.writeJSONToFile("parsed.json", jsonObject);
        } catch (IOException | ParserConfigurationException | SAXException e) {
            throw new RuntimeException(e);
        }
        long end = System.currentTimeMillis();
        float sec = (end - start) / 1000F;
        System.out.println(sec + " seconds");
    }
}