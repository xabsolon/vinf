package org.example;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

    private static String findAlternativeNamesInInfobox(String infobox, String fieldName) {
        Pattern pattern = Pattern.compile("\\| " + fieldName + " = * *(?<nazov>[^|]+)([ \\n])*\\|", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(infobox);
        if(matcher.find()) {
            String group = matcher.group("nazov").trim();
            if(!group.isEmpty())
                return group;
        }
        return null;
    }
    private static List<String> findAlternativeNamesInRevision(Element revision, String fieldName) {
        String textContent = revision.getElementsByTagName("text").item(0).getTextContent();
        List<String> matches = Pattern.compile("\\{\\{ *Infobox(?:[^}{]+)*+}}", Pattern.CASE_INSENSITIVE)
                .matcher(textContent)
                .results()
                .map(MatchResult::group).toList();

        List<String> alternativeNames = new ArrayList<>();
        for (String match : matches) {
            String name = findAlternativeNamesInInfobox(match, fieldName);
            if(name != null) {
                alternativeNames.add(name);
                System.out.println(name);
            }
        }

        return alternativeNames;
    }
    private static PageInfo processPage(String page, String fieldName) throws ParserConfigurationException, IOException, SAXException {
        Element root = Utils.createXmlRoot(page);
        String pageId = root.getElementsByTagName("id").item(0).getTextContent();
        String pageTitle = root.getElementsByTagName("title").item(0).getTextContent();
        NodeList revisions = root.getElementsByTagName("revision");
        List<String> alternativeNames = findAlternativeNamesInRevision((Element) revisions.item(0), fieldName);
        return new PageInfo(pageId, pageTitle, alternativeNames);
    }

    private static InputStream loadBzip2File(String fileName) {
        try {
            InputStream fileInputStream = new FileInputStream(fileName);
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

    private static List<PageInfo> loadSlovakPages(Path path) {
        try {
            String parsedContent = Files.readString(path);
            JSONObject object = new JSONObject(parsedContent);
            List<PageInfo> pages = new ArrayList<>();

            Iterator<String> keys = object.keys();
            while(keys.hasNext()) {
                String key = keys.next();
                JSONObject subObject = object.getJSONObject(key);
                String title = subObject.getString("title");
                JSONArray alternativeNamesJson = subObject.getJSONArray("alternativeNames");
                List<String> alternativeNames = new ArrayList<>();
                for(Object s : alternativeNamesJson) {
                    if(s instanceof String) {
                        alternativeNames.add((String)s);
                    }
                }
                pages.add(new PageInfo(key, title, alternativeNames));
            }
            return pages;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static List<PageInfo> parseWikiFile(JSONObject jsonObject, String fileName, String fieldName) {
        //Path path = Path.of("parsed.json");
        //if (Files.exists(path)){
         //   return loadSlovakPages(path);
       // }
        ArrayList<PageInfo> pageInfos = new ArrayList<>();
        try {
            InputStream inputStream = loadBzip2File(fileName);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

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
                    PageInfo pageInfo = processPage(page.toString(), fieldName);
                    List<String> alternativeNames = pageInfo.getAlternativeNames();
                    if(!alternativeNames.isEmpty()) {
                        pageInfos.add(pageInfo);
                        JSONObject pageInfoJsonObject = new JSONObject();
                        pageInfoJsonObject.put("title", pageInfo.getTitle());
                        pageInfoJsonObject.put("alternativeNames", alternativeNames);
                        jsonObject.put(pageInfo.getId(), pageInfoJsonObject);
                    }
                    continue;
                }

                if (isPage) page.append(line);
            }
        } catch (IOException | ParserConfigurationException | SAXException e) {
            throw new RuntimeException(e);
        }
        return pageInfos;
    }

    public static void main(String[] args) {
        long start = System.currentTimeMillis();

        JSONObject jsonObject = new JSONObject();
        List<PageInfo> slovakPages = parseWikiFile(jsonObject, "skwiki-latest.xml.bz2", "iny_nazov");
        List<PageInfo> englishPages = parseWikiFile(jsonObject, "enwiki-latest-pages-articles-multistream10.xml-p4045403p5399366.bz2", "aka");
        Utils.writeJSONToFile("parsed.json", jsonObject);

        List<PageInfo> pages = new ArrayList<>(slovakPages);
        pages.addAll(englishPages);


        Index index = new Index();
        index.buildIndex(pages);
        index.search("Renault");

        long end = System.currentTimeMillis();
        float sec = (end - start) / 1000F;
        System.out.println(sec + " seconds");
    }
}