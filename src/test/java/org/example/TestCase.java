package org.example;

import org.apache.lucene.document.Document;
import org.json.JSONArray;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.Assert.*;

public class TestCase {
    private static Index index = null;

    @Before
    public void init() {
        String parsedContent;
        try {
            parsedContent = Files.readString(Path.of("parsed.json"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        JSONArray array = new JSONArray(parsedContent);
        index = new Index();
        index.buildIndex(array);
    }

    @Test
    public void testFindRenault(){
        List<Document> documents = index.search("Renault 8");
        assertEquals(true, documents.stream().anyMatch(x -> x.getField("alternativeNames").stringValue().contains("Dacia 1100")));
    }

    @Test
    public void testFindTalet30(){
        List<Document> documents = index.search("Talet-30");
        assertEquals(true, documents.stream().anyMatch(x -> x.getField("alternativeNames").stringValue().contains("ťahač leteckej techniky")));
    }

    @Test
    public void testFindPragaV3S(){
        List<Document> documents = index.search("Praga V3S");
        assertEquals(true, documents.stream().anyMatch(x -> x.getField("alternativeNames").stringValue().contains("Vetrieska")));
    }

    @Test
    public void testFindMercedes500E(){
        List<Document> documents = index.search("Mercedes-Benz 500E");
        assertEquals(true, documents.stream().anyMatch(x -> x.getField("alternativeNames").stringValue().contains("Mercedes E 500")));
    }
}
