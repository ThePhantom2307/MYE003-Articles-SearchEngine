package mye003.searchenginenews.controllers;

import mye003.searchenginenews.dto.SearchResult;
import mye003.searchenginenews.services.SearchService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.queryparser.classic.ParseException;


public class SearchController {

    private static final int RESULTS_PER_PAGE = 10;
    private final SearchService service;
    private List<SearchResult> cachedResults = new ArrayList<>();
    private String lastQuery;

    public SearchController(SearchService service) {
        this.service = service;
    }

    //Ektelei search mesw ths search poy ypaexei sto SearchService
    public void newSearch(String query, String field) {
        this.lastQuery = query;
        try {
            cachedResults = service.search(query, field, 4000);
        } catch (IOException | ParseException e) {
            throw new RuntimeException("Search failed: " + e.getMessage(), e);
        }
    }

    //Epistrefei ta apotelesmata poy yparxoyn sth selida pageNo
    public List<SearchResult> getPage(int pageNo) {
        int start = pageNo * RESULTS_PER_PAGE;
        int end   = Math.min(start + RESULTS_PER_PAGE, cachedResults.size());
        return cachedResults.subList(start, end);
    }
    
    //Epistrefei ta synolika apotelesmata
    public int getTotalHits()   { 
    	return cachedResults.size(); 
    
    }
    
    //Epistrefei tis synolikes selides
    public int getTotalPages()  { 
    	return (int) Math.ceil(getTotalHits() / (double) RESULTS_PER_PAGE); 
    }
    
    //Epistrefei to teleytaio query
    public String getLastQuery(){ 
    	return lastQuery; 
    }
    
    //Methodoi gia to GUI
    //Epistrefei tin lista me ta apotelesmata
    public List<SearchResult> getCachedResults() {
        return cachedResults;
    }

    //Epistrefei ta apotelesmata ana selida
    public int getResultsPerPage() {
        return RESULTS_PER_PAGE;
    }
}
