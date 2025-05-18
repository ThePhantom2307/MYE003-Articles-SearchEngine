package mye003.searchenginenews.services;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

//Dhmioyrgei to index apo to CSV
public class IndexService {
	
	public IndexService(String csvFile, String indexPath) {

        try {
            Analyzer analyzer = new StandardAnalyzer();
            Directory indexDir = FSDirectory.open(Paths.get(indexPath));

            //Elegxoume an yparxei to index alliws to ftiaxnoume
            if (!DirectoryReader.indexExists(indexDir)) {
                IndexWriterConfig config = new IndexWriterConfig(analyzer);
                config.setOpenMode(OpenMode.CREATE);
                IndexWriter writer = new IndexWriter(indexDir, config); //O writer gia ta documents tou index
                indexCSVDocuments(csvFile, writer); //Apo CSV se document
                writer.close();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //Diavazei to CSV kai prosthetei kathe grammh ws document
    private static void indexCSVDocuments(String csvFile, IndexWriter writer) throws IOException {
        int validDocs = 0;

        try (CSVReader reader = new CSVReader(new FileReader(csvFile))) {
            String[] fields; //Kratame ta stoixeia twn fields kathe seiras
            boolean header = true; //Prwth grammh header

            while (true) {
                try {
                    fields = reader.readNext();
                } catch (CsvValidationException e) {
                    System.out.println("Skipping row due to parsing error: " + e.getMessage());
                    continue;
                }

                if (fields == null) break; //EOF

                //An eimaste sto head skip
                if (header) {
                    header = false;
                    continue;
                }
                
                //An exei ligotera fields tote skip
                if (fields.length < 11) {
                    System.out.println("Skipping row due to insufficient fields.");
                    continue;
                }
                
                //Mapping
                String indexField = fields[0].trim();
                String author = fields[1].trim();
                String datePublished = fields[2].trim();
                String category = fields[3].trim();
                String section = fields[4].trim();
                String url = fields[5].trim();
                String headline = fields[6].trim();
                String description = fields[7].trim();
                String keywords = fields[8].trim();
                String secondHeadline = fields[9].trim();
                String articleText = fields[10].trim();
                
                //Ftiaxnoume to document
                Document doc = new Document();
                doc.add(new TextField("index", indexField, Field.Store.YES));
                doc.add(new TextField("author", author, Field.Store.YES));
                doc.add(new TextField("date_published", datePublished, Field.Store.YES));
                doc.add(new TextField("category", category, Field.Store.YES));
                doc.add(new TextField("section", section, Field.Store.YES));
                doc.add(new TextField("url", url, Field.Store.YES));
                doc.add(new TextField("headline", headline, Field.Store.YES));
                doc.add(new TextField("description", description, Field.Store.YES));
                doc.add(new TextField("keywords", keywords, Field.Store.YES));
                doc.add(new TextField("second_headline", secondHeadline, Field.Store.YES));
                doc.add(new TextField("article_text", articleText, Field.Store.YES));

                writer.addDocument(doc); //Prosthetoume to document sto index mesw tou writer
                validDocs++;
            }
        }

        System.out.println("Total valid documents added: " + validDocs);
    }
}
