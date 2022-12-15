# Alternatívne mená (title, FB title, redirect, Disambig,….) a štatistika k nim. Document frequency, collection frequency - Wikipedia

**Predmet:** Vyhľadávanie informácií  
**Autor:** Michael Absolon  
**Cvičiaci:** Ing. Igor Stupavský

Riešenie projektu v jazyku Java.

Tento projekt sa zameriava na parsovanie alternatívnych mien automobilov z infoboxov. Vyhľadávanie umožňuje zadať názov článku z Wikipédie(Alebo konkrétnu značku vozidla) a ako výsledky sa zobrazia alternatívne mená týchto vozidiel. Príklad: Praga V3S -> Vetrieska

### Pseudokód

- Načítaj súbor skwiki-latest-pages-articles-multistream.xml.bz2 pomocou Apache Commons Compress
- Prehľadávaj súbor po riadkoch
- Keď narazíš na začiatok tagu <page>, začni ukladať riadky do bufferu
- Keď narazíš na koniec tagu </page>, uzavri buffer a odošli ho na spracovanie do funkcie processPage()
- V stránke page nájdi poslednú revíziu článku
- Pomocou regexu nájdi v textovom obsahu tejto revízie infobox
- Z infoboxu pomocou regexu extraktuj riadok "iny_nazov", ktorý obsahuje alternatívne názvy
- Vytvor súbor parsed.json a ulož doňho vyparsované dáta
- Načítaj vstup od používateľa
- Zobraz všetky záznamy, ktoré obsahujú výraz alebo sú alternatívnym menom k danému výrazu
- Zobraz document frequency a collection frequency, pre daný výraz

## Dokumentácia

###  Idea
Tento projekt sa zameriava na parsovanie alternatívnych mien z infoboxov. Vyhľadávanie umožňuje zadať názov článku z Wikipédie a ako výsledky sa zobrazia ich alternatívne mená. Vyhľadávanie funguje aj opačne, čiže zadá sa alternatívne meno a ako výsledky sa zobrazia konkrétne stránky wikipédie. Nástroj umožňuje vyhľadávať alternatívne mená v slovenských aj v anglických wikipedia datasetoch.

### Technológie
- JDK19
- Apacha Lucene - v9.4.1 - Indexovanie výsledkov parsovania, vyhľadávanie
- Apache Commons Spark - v3.3.1 - Parsovanie XML datasetov, paralelné spracovanie
- JUnit - v4.13.2 - Unit testy
- Org.json - v2.5.2 - Ukladanie medzivýsledkov do JSON súborov

### Dáta
Ako datasety boli použité XML dumpy wikipédie komprimované formátom BZIP2 zo stránky dumps.wikimedia.org.
Slovenské datasety:
- skwiki-latest-pages-articles-multistream.xml.bz2 - Obsahuje všetky slovenské wikipédia stránky a revízie stránok vo formáte XML.

Anglické datasety:
- enwiki-latest-pages-articles-multistream10.xml-p4045403p5399366.bz2 - Obsahuje časť anglických wikipédia stránok vo formáte XML.

Tieto súbormi s datasetmi musia byť počas behu programu prístupné v root zložke.

### Postup

Prvým krokom pri vývojí bolo analyzovanie wikitext formátu, ktorý používa Wikipédia na formátovanie infoboxov. Cieľom bolo vyvinúť regex, ktorý by vedel extrahovať jednotlivé atribúty z infoboxov. Po viacerých neúspechoch z dôvodu výskytu špeciálnych znakov v hodnotách jednotlivých atribútov sa mi podarilo vyvinúť regex výraz, ktorý fungoval. Druhým krokom bolo extrahovanie jednotlivých stránok z XML dumpu. To bolo najprv implementované pomocou Apache Commons Compress, ktorý vedel čítať BZIP2 súbory, avšak následne túto funkcionalitu nahradil Apache Commons Spark, ktorý vie BZIP2 súbory načítať a zároveň parsovať XML. Tretím problémom, ktorý bolo treba vyriešiť je vytváranie indexu. Index bol najprv vytvorený ako JSON súbor, avšak neskôr po lepšom pochopení Apache Spark na prednáške sa podarilo implementovať indexer spolu s vyhľadávaním TopDocs, ktorý zobrazí 10 najlepších výsledkov podľa vyhľadávanej frázy.

Diagram zobrazujúci jednotlivé fázy programu:
![alt text](https://i.imgur.com/SpWVjWi.png)

#### Indexovanie
Indexovanie je prevedené cez framework Apache Lucene a má ho na starosti
trieda Index.java. Tá dostane na vstup JSON súbor, z ktorého následne vytvorí Index.
Ukážka JSON súboru:
```
  ...,
  {
    "title": "Renault 20",
    "id": 296073,
    "alternativeNames": "[Dacia 2000<br />Renault 20<br />Renault 30]"
  },
  {
    "title": "Praga V3S",
    "id": 320315,
    "alternativeNames": "[Vetrieska]"
  },
  ...
```

#### Vyhľadávanie
Na vyhľadávanie sa používa trieda TopDocs, ktorá poukazuje na N najlepších výsledkov vyhľadávania, ktoré zodpovedajú kritériám vyhľadávania. Je to jednoduchý kontajner ukazovateľov, ktorý ukazuje na dokumenty, ktoré sú výstupom výsledku vyhľadávania.

### Použitá literatúra
- Laclavík, M. -- Šeleng, M. Vyhľadávanie informácií
- Gospodnetić, O. -- Hatcher, E. -- Mccandless, M. Lucene in Action
- https://www.tutorialspoint.com/lucene/index.htm
- https://www.tutorialspoint.com/apache_spark/index.htm

###  Overenie
Na overenie výsledkov boli použité Unit testy vo frameworku JUnit.
Boli napísané 4 Unit testy:
- Overenie vyhľadávania v slovenskom datasete. Do vyhľadávania sa vloží fráza "Talet-30" a očakáva sa, že vo výsledkoch vyhľadávania sa bude nachádzať alternatívne meno "ťahač leteckej techniky".
- Overenie vyhľadávania v slovenskom datasete. Do vyhľadávania sa vloží fráza "Praga V3S" a očakáva sa, že vo výsledkoch vyhľadávania sa bude nachádzať alternatívne meno "Vetrieska".
- Overenie vyhľadávania v anglickom datasete. Do vyhľadávania sa vloží fráza "Mercedes-Benz 500E" a očakáva sa, že vo výsledkoch vyhľadávania sa bude nachádzať alternatívne meno "Mercedes E 500".
- Overenie vyhľadávania v anglickom datasete. Do vyhľadávania sa vloží fráza "Renault 8" a očakáva sa, že vo výsledkoch vyhľadávania sa bude nachádzať alternatívne meno "Dacia 1300".

Ukážka testu pre najdenie vetriesky:
```java
    @Test
public void testFindPragaV3S(){
        List<Document> documents = index.search("Praga V3S");
        assertEquals(true, documents.stream().anyMatch(x -> x.getField("alternativeNames").stringValue().contains("Vetrieska")));
        }
```

V teste sa zavolá funkcia search, ktorá vrati pole dokumentov. A potom sa assertuje, či aspoň jeden nájdený dokument obsahuje alternatívne meno "vetrieska".

## Použivateľský manuál

Spustiteľný .jar súbor sa nachádza v zložke build.
Spustí sa príkazom:
```
java -jar vinf.jar
```

Po spustení programu sa do konzoli vypíše všetky možné príkazy:
```
Index loaded. You can start searching.
1 - Process dataset
2 - Find alternative names for wikipedia page
3 - Find wikipedia page by alternative name
Enter a choice:
```

Príkaz číslo 1 slúži na spracovanie anglického a slovenského datasetu. To môže trvať do 10 minút.
Následne je možné použiť príkazy 2, 3.

Ukážka voľbý príkazu číslo 2:

```
Index loaded. You can start searching.
1 - Process dataset
2 - Find alternative names for wikipedia page
3 - Find wikipedia page by alternative name
Enter a choice: 2
Enter search phrase: 
```

Ako vyhľadávaciu frázu zvolíme "Renault":

```
Number of results: 8

Title: Renault Kangoo
Alternative names: [Nissan Kubistar]
Nissan Kubistar

Title: Renault 21
Alternative names: [Renault 21/Medallion]

Title: Renault Clio
Alternative names: [Renault Clio Storia]
Renault Clio Storia

Title: Renault 7
Alternative names: [R7]
R7

Title: Renault 4
Alternative names: [Renault R4]
Renault R4

Title: Renault 8
Alternative names: [Renault 10<br />Dacia 1100<br />Bulgarrenault 8/10]

Title: Renault 12
Alternative names: [Dacia 1300/1310/1410]

Title: Renault 20
Alternative names: [Dacia 2000<br />Renault 20<br />Renault 30]
```

Vidíme, že pre túto frázu sa našlo 8 výsledkov.

### Záver

Podarilo sa vytvoriť zaujimavý nástroj na vyhľadávanie alternatívnych mien automobilov na Wikipédii a potom riešenie patrične overiť pomocou Unit testou. Do buducna by sa dalo pridať vyhľadávanie alternatívnych mien priamo v texte alebo z ambiguity pages.
