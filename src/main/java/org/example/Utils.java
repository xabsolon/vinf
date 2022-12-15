package org.example;

import org.apache.spark.SparkConf;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.api.java.UDF1;
import org.apache.spark.sql.types.DataTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {

    static String findAlternativeNamesInInfobox(String infobox, String fieldName) {
        Pattern pattern = Pattern.compile("\\| " + fieldName + " = * *(?<nazov>[^|]+)([ \\n])*\\|", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(infobox);
        if (matcher.find()) {
            String group = matcher.group("nazov").trim();
            if (!group.isEmpty())
                return group;
        }
        return null;
    }

    static SparkSession initSparkSession() {
        SparkConf sparkConf = new SparkConf().setAppName("App").setMaster("local");
        SparkSession sparkSession = SparkSession.builder().config(sparkConf).getOrCreate();
        sparkSession.sparkContext().setLogLevel("ERROR");

        UDF1<Row, Object> extractRegexSlovak = (Row s1) -> {
            String text = s1.getAs("text").toString();

            List<String> matches = Pattern.compile("\\{\\{ *Infobox(?:[^}{]+)*+}}", Pattern.CASE_INSENSITIVE)
                    .matcher(text)
                    .results()
                    .map(MatchResult::group).toList();

            List<String> alternativeNames = new ArrayList<>();
            for (String match : matches) {
                String name = findAlternativeNamesInInfobox(match, "iny_nazov");
                if (name != null) {
                    alternativeNames.add(name);
                }
            }

            return alternativeNames.toString();

        };

        UDF1<Row, Object> extractRegexEnglish = (Row s1) -> {
            String text = s1.getAs("text").toString();

            List<String> matches = Pattern.compile("\\{\\{ *Infobox(?:[^}{]+)*+}}", Pattern.CASE_INSENSITIVE)
                    .matcher(text)
                    .results()
                    .map(MatchResult::group).toList();

            List<String> alternativeNames = new ArrayList<>();
            for (String match : matches) {
                String name = findAlternativeNamesInInfobox(match, "aka");
                if (name != null) {
                    alternativeNames.add(name);
                }
            }

            return alternativeNames.toString();

        };

        sparkSession.sqlContext()
                .udf()
                .register("extractRegexSlovak", extractRegexSlovak, DataTypes.StringType);

        sparkSession.sqlContext()
                .udf()
                .register("extractRegexEnglish", extractRegexEnglish, DataTypes.StringType);

        return sparkSession;
    }
}
