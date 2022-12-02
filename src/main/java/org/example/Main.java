package org.example;

import org.apache.spark.SparkConf;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.functions;
import org.apache.spark.sql.types.DataTypes;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
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

    public static void main(String[] args) {

        long start = System.currentTimeMillis();

        SparkConf sparkConf = new SparkConf().setAppName("App").setMaster("local");
        SparkSession sparkSession = SparkSession.builder().config(sparkConf).getOrCreate();
        sparkSession.sparkContext().setLogLevel("ERROR");

        sparkSession.sqlContext()
                .udf()
                .register( "extractRegex", ( Row s1 ) -> {
                    String text = s1.getAs("text").toString();

                    List<String> matches = Pattern.compile("\\{\\{ *Infobox(?:[^}{]+)*+}}", Pattern.CASE_INSENSITIVE)
                            .matcher(text)
                            .results()
                            .map(MatchResult::group).toList();

                    List<String> alternativeNames = new ArrayList<>();
                    for (String match : matches) {
                        String name = findAlternativeNamesInInfobox(match, "iny_nazov");
                        if(name != null) {
                            alternativeNames.add(name);
                        }
                    }

                    return alternativeNames.toString();

                }, DataTypes.StringType );


        Dataset<Row> df = sparkSession.read()
                .option("rowTag", "page")
                .format("xml")
                .load("newpages.xml");

        df = df.withColumn("alternativeNames", functions.callUDF("extractRegex", df.col("revision")));

        df.show(100);

        df.select("title", "id", "alternativeNames")
            .write()
            .format("xml")
            .option("rootTag", "pages")
            .option("rowTag", "page")
            .save("result.xml");

        //List<PageInfo> slovakPages = parseWikiFile(jsonObject, "skwiki-latest.xml.bz2", "iny_nazov");
        //List<PageInfo> englishPages = parseWikiFile(jsonObject, "enwiki-latest-pages-articles-multistream10.xml-p4045403p5399366.bz2", "aka");

//        Index index = new Index();
//        index.buildIndex(df);
//        index.search("Nissan");

        long end = System.currentTimeMillis();
        float sec = (end - start) / 1000F;
        System.out.println(sec + " seconds");
    }
}