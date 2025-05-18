package mye003.searchenginenews.services;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import mye003.searchenginenews.dto.SearchResult;

import org.apache.lucene.queryparser.classic.*;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class SearchService {
    private final Analyzer analyzer = new StandardAnalyzer();
    private final Directory indexDir;
    private final IndexSearcher searcher;
    private static final int MAX_HISTORY = 20;
    
    private static class SearchHistoryEntry {
        String query;
        List<Document> topDocuments;

        SearchHistoryEntry(String query, List<Document> topDocuments) {
            this.query = query;
            this.topDocuments = topDocuments;
        }
    }

    //Kathe antikeimeno einai ths morfhs (query, 5 top apotelesmata apo thn anazhthsh)
    private final Deque<SearchHistoryEntry> history = new LinkedList<>(); 
    
    
    public SearchService(String indexPath) throws IOException {
        this.indexDir = FSDirectory.open(Paths.get(indexPath)); //Kratame to path tou index
        this.searcher = new IndexSearcher(DirectoryReader.open(indexDir)); //Anoigoume ton searcher sto index
    }

    
    //Methodos poy kanei search kai epistrefei mia lista me ta apotelesmata tou search
    public List<SearchResult> search(String queryText, String field, int maxHits) throws ParseException, IOException {
    	
    	QueryParser parser = new QueryParser(field, analyzer); //Dhmioyrgia parser
        Query baseQuery = parser.parse(queryText); //Metatroph tou text tou xrhsth se query object

        //TFIDF
        BooleanQuery.Builder finalQueryBuilder = new BooleanQuery.Builder(); //Ftiaxnoume ena syntheto query
        finalQueryBuilder.add(baseQuery, BooleanClause.Occur.MUST); //Prosthetoume to query sto syntheto me AND(MUST)
        
        Map<String, Float> boostedTerms = extractBoostTermsFromHistory(field); //Set me {entry:value(score)}
        
        for (Map.Entry<String, Float> entry : boostedTerms.entrySet()) { //Gia kathe oro (entry)
            Term term = new Term(field, entry.getKey());
            BoostQuery boostedQuery = new BoostQuery(new TermQuery(term), entry.getValue()); //To value einai to score
            finalQueryBuilder.add(boostedQuery, BooleanClause.Occur.SHOULD); //Prosthetoume to query me OR(should)
        }

        Query finalQuery = finalQueryBuilder.build(); //Exei to base query me MUST kai tous boosted orous me should
        //Kanoume anazhthsh me to query panw sto index
        TopDocs docs = searcher.search(finalQuery, maxHits); //Ta apotelesmata einai sorted kata score

        List<SearchResult> results = new ArrayList<>();
        List<Document> topDocsToStore = new ArrayList<>(); //Gia ta 5 prwta documents
        
        //Apothikeuoume ta docs sthn lista
        for (int i = 0; i < Math.min(5, docs.scoreDocs.length); i++) {
            Document doc = searcher.storedFields().document(docs.scoreDocs[i].doc);
            topDocsToStore.add(doc);
        }

        //Prosthikh sto istoriko
        if (!topDocsToStore.isEmpty()) {
            if (history.size() >= MAX_HISTORY) history.removeFirst();
            history.addLast(new SearchHistoryEntry(queryText, topDocsToStore));
        }
        
        //DEBUGGING
        System.out.println("[History] New search: \"" + queryText + "\"");
        System.out.println("[History] History size: " + history.size());

        //Epistrofh apotelesmatwn gia kathe document pou brethike
        for (ScoreDoc sd : docs.scoreDocs) {
        	Document doc = searcher.storedFields().document(sd.doc);
            results.add(mapDocument(doc, sd.score));
        }

        return results;
    }

    
    //DTO gia thn search result
    private SearchResult mapDocument(Document doc, float score) {
        return new SearchResult(
                doc.get("second_headline"),
                doc.get("url"),
                doc.get("description"),
                doc.get("date_published"),
                score
        );
    }
    
    private Map<String, Float> extractBoostTermsFromHistory(String field) throws IOException {
       
    	Map<String, Integer> termFreq = new HashMap<>();
        Map<String, Float> tfidfScores = new HashMap<>();

        for (SearchHistoryEntry entry : history) { //Diatrexoume to history (gia kathe search)
            for (Document doc : entry.topDocuments) { //Diatrexoume kathe document (gia kathe document tou search)
                String content = doc.get(field);
                if (content == null) continue;

                String[] terms = content.toLowerCase().split("\\W+"); //Afairoume ton "thoryvo" apo to keimeno
                for (String term : terms) {
                    if (term.length() < 3) continue; //Filtro mikrwn lexewn
                    termFreq.put(term, termFreq.getOrDefault(term, 0) + 1);
                }
            }
        }
        
        //Ypologizoume to TF-IDF gia kathe lexh
        int totalDocs = searcher.getIndexReader().numDocs();
        for (Map.Entry<String, Integer> entry : termFreq.entrySet()) {
            String term = entry.getKey();
            int tf = entry.getValue();

            Term indexedTerm = new Term(field, term);
            int df = searcher.getIndexReader().docFreq(indexedTerm); //DF gia ton typo
            if (df == 0) continue;

            float idf = (float) Math.log((double) (totalDocs + 1) / (df + 1)); //Typos IDF
            float tfidf = tf * idf * 0.3f; //Typos TF-IDF me varos wste na ephreazei thn thmh pio omala
            if (tfidf > 1.0f) {
                tfidfScores.put(term, tfidf);
            }
        }
        
        //DEBUGGING
        System.out.println("[Boosted Terms]");
        for (Map.Entry<String, Float> e : tfidfScores.entrySet()) {
            System.out.println(" - " + e.getKey() + ": " + e.getValue());
        }
        
        
        //Epistrefontai oi 10 pio boosted lekseeis se hashmap
        return tfidfScores.entrySet().stream()
                .sorted(Map.Entry.<String, Float>comparingByValue().reversed())
                .limit(10)
                .collect(HashMap::new, (m, e) -> m.put(e.getKey(), e.getValue()), HashMap::putAll);
    }



}
