package org.example;

import org.apache.commons.io.FileUtils;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.functions;
import org.json.JSONArray;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

import static org.example.Utils.initSparkSession;

public class Main {
    private static final String SLOVAK_DATASET = "skwiki-latest.xml.bz2";
    private static final String ENGLISH_DATASET = "enwiki-latest-pages-articles-multistream10.xml-p4045403p5399366.bz2";
    private static Index index = null;
    private static Path output = Path.of("parsed.json");

    private static Dataset<Row> parseDataset(SparkSession sparkSession, String datasetFilePath, String udfName) {
        Dataset<Row> df = sparkSession.read()
                .option("rowTag", "page")
                .format("xml")
                .load(datasetFilePath);

        df = df.withColumn("alternativeNames", functions.callUDF(udfName, df.col("revision")));

        return df;
    }

    private static void parseDatasets() {

        SparkSession sparkSession = initSparkSession();

        Dataset<Row> slovakDataset = parseDataset(sparkSession, SLOVAK_DATASET, "extractRegexSlovak");
        Dataset<Row> englishDataset = parseDataset(sparkSession, ENGLISH_DATASET, "extractRegexEnglish");

        Dataset<Row> df = slovakDataset.union(englishDataset);

        df = df.filter(df.col("alternativeNames").notEqual("[]"));

        System.out.println(df.count());

        String path = "result.xml";
        try {
            FileUtils.deleteDirectory(new File(path));
        } catch (IOException e) {
            System.out.println("Failed to delete directory");
        }

        String json = df.select("title", "id", "alternativeNames").toJSON().collectAsList().toString();

        try {
            Files.writeString(output, json);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        loadIndex();
    }

    private static void loadIndex() {
        String parsedContent;
        try {
            parsedContent = Files.readString(output);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        JSONArray array = new JSONArray(parsedContent);

        index = new Index();
        index.buildIndex(array);
    }

    public static void main(String[] args) {
        long start = System.currentTimeMillis();

        do {
            if(index == null && Files.exists(output)) {
                loadIndex();
                System.out.println("Index loaded. You can start searching.");
            }

            Scanner scan = new Scanner(System.in);
            System.out.println("1 - Process dataset");
            System.out.println("2 - Find alternative names for wikipedia page");
            System.out.println("3 - Find wikipedia page by alternative name");
            System.out.print("Enter a choice: ");

            int num = scan.nextInt();

            boolean shouldBreak = false;
            switch (num) {
                case 1:
                    System.out.println("Processing slovak and english wikipedia dataset...");
                    parseDatasets();
                    break;
                case 2:
                    if(index == null) {
                        System.out.println("Dataset is not processed. Run command 1 - Process datase first.");
                        break;
                    }
                    System.out.println("Enter search phrase:");
                    String query = scan.next();
                    index.search(query);
                    break;
                case 3:
                    if(index == null) {
                        System.out.println("Dataset is not processed. Run command 1 - Process datase first.");
                        break;
                    }
                    System.out.println("Enter search phrase:");
                    String query1 = scan.next();
                    index.searchByAlternativeNames(query1);
                    break;
                default:
                    System.out.println("Exiting...");
                    shouldBreak = true;
                    break;
            }
            if (shouldBreak) break;
        } while (true);

        long end = System.currentTimeMillis();
        float sec = (end - start) / 1000F;
        System.out.println(sec + " seconds");
    }
}