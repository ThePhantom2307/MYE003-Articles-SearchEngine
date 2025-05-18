package mye003.searchenginenews.controllers;

import mye003.searchenginenews.services.IndexService;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Path;


public class IndexController {

    private final String csvPath;
    private final String indexPath;
    @SuppressWarnings("unused")
	private IndexService service;

    public IndexController(String csvPath, String indexPath) {
        this.csvPath   = csvPath;
        this.indexPath = indexPath;
    }
    
    //Dhmioyrgei ena object IndexService pou ayto me th seira tou tha ftiaksei to index (helper synarthsh)
    public void buildIndex() {
        this.service = new IndexService(csvPath, indexPath);
    }

    //Dhmioyrgei to index an den yparxei
    public void ensureIndex() {
        if (!indexExists()) {
            buildIndex();
        }
    }

    //Kanei rebuild to index
    public void rebuildIndex() {
        buildIndex();
    }

    //Elegxoume an yparxei to index
    public boolean indexExists() {
        try (var dir = FSDirectory.open(Path.of(indexPath))) {
            return DirectoryReader.indexExists(dir);
        } catch (IOException e) {
            return false;
        }
    }


}
