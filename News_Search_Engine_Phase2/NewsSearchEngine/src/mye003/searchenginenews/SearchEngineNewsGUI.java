package mye003.searchenginenews;

import mye003.searchenginenews.controllers.IndexController;
import mye003.searchenginenews.controllers.SearchController;
import mye003.searchenginenews.dto.SearchResult;
import mye003.searchenginenews.services.SearchService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.regex.Pattern;

@SuppressWarnings("serial")
public class SearchEngineNewsGUI extends JFrame {

    // Statheres times
    private static final int MAX_CHARS_PER_LINE = 100;
    private static final String[] SEARCH_FIELDS = { "Keywords", "Author", "Category", "Articles Title" };

    // Oi kartes poy tha xrhsimopoihthoyn
    private final IndexController indexController;
    private SearchController searchController;

    // oi diathesimes karteles (homepage, results page)
    private final CardLayout cards = new CardLayout();
    private final JPanel cardsPane = new JPanel(cards);

    // Sxetika me to home page
    private final JTextField homeField = new JTextField(40);
    private final JButton homeSearchButton = new JButton();
    private final JComboBox<String> homeCombo = new JComboBox<>(SEARCH_FIELDS);
    private final JLabel logoLabel = new JLabel();

    // Sxetika me ta results
    private final JTextField resultsField = new JTextField();
    private final JButton resultSearchButton = new JButton();
    private final JComboBox<String> resultsCombo = new JComboBox<>(SEARCH_FIELDS);
    private final JComboBox<String> sortCombo = new JComboBox<>(new String[]{"Default", "Alphabetical"});
    private final JPanel resultsContainer = new JPanel();
    private final JScrollPane scrollPane = new JScrollPane(resultsContainer);
    private final JLabel statusLabel = new JLabel(" ");
    private final JPanel pagerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
    private int currentPage = 0;

    // Gia to initializing tou index
    private JDialog progressDlg;

    
    // O constructor kalei tis methodous gia thn dhmioyrgia tou gui kai thetei kapoiew basikes leitourgies
    public SearchEngineNewsGUI() {
        super("News Search Engine");
        indexController = new IndexController("resources/CNN_Articles_clean.csv", "resources/index");
        
        buildHomePage();
        buildResultsPage();
        
        add(cardsPane);
        
        setDefaultCloseOperation(EXIT_ON_CLOSE); // Ejodos toy programmatos otan kleinei me to X
        setMinimumSize(new Dimension(1000, 700));
        setLocationRelativeTo(null);
        setVisible(true);
        
        // Elegxei an yparxei index kai an oxi kalei thn methodo poy that to dhmiourgisei
        if (indexController.indexExists()) {
        	afterIndexReady();
        } else {
        	buildIndexAsync();
        }
    }

    
    // Dhmiourgei thn emfanish tou homepage kai bazei listeners se kapoia objects
    private void buildHomePage() {
        JPanel home = new JPanel(new GridBagLayout());
        home.setBackground(new Color(242,242,242));
        
        ImageIcon icon = new ImageIcon("resources/logo.png");
        Image scaled = icon.getImage().getScaledInstance(160, 160, Image.SCALE_SMOOTH);
        logoLabel.setIcon(new ImageIcon(scaled));
        
        
        installPlaceholder(homeField, "  Search...");
        homeField.setFont(homeField.getFont().deriveFont(16f));
        homeField.addActionListener(e -> triggerSearchFromHome()); // prosthetei listener gia na kanei anazhthsh
        
        JPanel searchBox = new JPanel();
        searchBox.setOpaque(false);
        searchBox.setLayout(new BoxLayout(searchBox, BoxLayout.X_AXIS));
        searchBox.setMaximumSize(new Dimension(500, 35));
        
        homeField.setMaximumSize(new Dimension(350, 35));
        homeCombo.setMaximumSize(new Dimension(150, 35));
        
        homeSearchButton.setText("Search"); // ή homeSearchButton.setText("Search");
        homeSearchButton.setFocusable(false);
        homeSearchButton.addActionListener(e -> triggerSearchFromHome());
        
        searchBox.add(homeField);
        searchBox.add(Box.createHorizontalStrut(5));
        searchBox.add(homeSearchButton);
        searchBox.add(Box.createHorizontalStrut(10));
        searchBox.add(homeCombo);
        
        
        JPanel box = new JPanel();
        box.setOpaque(false);
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        
        logoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        searchBox.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        box.add(logoLabel);
        box.add(Box.createVerticalStrut(30));
        box.add(searchBox);
        home.add(box);
        
        cardsPane.add(home, "home");
    }

    // dhmiourgia tou result page poy emfanizontai ta apotelesmata
    private void buildResultsPage() {
        JPanel root = new JPanel(new BorderLayout(10,10));
        root.setBorder(new EmptyBorder(10,10,10,10));
        
        resultSearchButton.setText("Search");
        resultSearchButton.setFocusable(false);
        resultSearchButton.addActionListener(e -> triggerSearchFromResults());
        
        JPanel top = new JPanel(new BorderLayout(5,5));
        top.add(new JLabel("🔍 "), BorderLayout.WEST);
        JPanel fieldAndButton = new JPanel(new BorderLayout(5,0));
        fieldAndButton.add(resultsField, BorderLayout.CENTER);
        fieldAndButton.add(resultSearchButton, BorderLayout.EAST);
        top.add(fieldAndButton, BorderLayout.CENTER);

        resultsField.addActionListener(e -> triggerSearchFromResults());
        
        JPanel east = new JPanel(new FlowLayout(FlowLayout.RIGHT,5,0));
        east.add(resultsCombo);
        east.add(sortCombo);
        
        top.add(east, BorderLayout.EAST);
        sortCombo.addActionListener(e -> renderPage(currentPage));
        
        root.add(top, BorderLayout.NORTH);
        resultsContainer.setLayout(new BoxLayout(resultsContainer, BoxLayout.Y_AXIS));
        
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        root.add(scrollPane, BorderLayout.CENTER);
        
        JPanel south = new JPanel(new BorderLayout());
        south.add(statusLabel, BorderLayout.CENTER);
        south.add(pagerPanel, BorderLayout.EAST);
        root.add(south, BorderLayout.SOUTH);
        
        cardsPane.add(root, "results");
    }

    // Jekinaei na kanei anazhthsh, pairnontas tis aparaithtes plhrofories
    private void triggerSearchFromHome() {
        String query = homeField.getText().trim(); // anakthsh keimenoy apo to pedio anazhthshs
        
        if (query.isBlank() || searchController == null)
        	return;
        
        String field = homeCombo.getSelectedItem().toString(); // anakthsh keimenou apo to dropdown (pedio)
        resultsField.setText(query);
        resultsCombo.setSelectedItem(field);
        
        String selectedField;
        if (field.equals("Keywords")) {
        	selectedField = "keywords";
        } else if (field.equals("Author")) {
        	selectedField = "author";
        } else if (field.equals("Category")) {
        	selectedField = "category";
        } else {
        	selectedField = "second_headline";
        }
        
        performSearch(query, selectedField); // klhsh methodou anazhthshs
        
        cards.show(cardsPane, "results"); // emfanhsh apotelesmaton
    }

    
    // anazhthsh apo to results page (idia logiki me to triggerSearchFromHome)
    private void triggerSearchFromResults() {
        String query = resultsField.getText().trim();
        
        if (query.isBlank())
        	return;
        
        String field = resultsCombo.getSelectedItem().toString();
        
        String selectedField;
        if (field.equals("Keywords")) {
        	selectedField = "keywords";
        } else if (field.equals("Author")) {
        	selectedField = "author";
        } else if (field.equals("Category")) {
        	selectedField = "category";
        } else {
        	selectedField = "second_headline";
        }
        
        performSearch(query, selectedField);
    }

    // klhsh methodou apo ton conroller gia anazhthsh arthron kai klhsh methodou gia emfanhsh apotelesmaton
    private void performSearch(String query, String field) {
        try {
            searchController.newSearch(query, field); // ektelesh anazhthshs
            currentPage = 0;
            renderPage(currentPage); // emfanhsh apotelesmaton
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, "Search failed:\n" + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Emfanhsh apotelesmaton sto gui
    private void renderPage(int pageNo) {
        resultsContainer.removeAll(); // afairesh prohgoymenon
        List<SearchResult> all = searchController.getCachedResults();
        List<SearchResult> list = new ArrayList<>(all);
        
        // elegxos kai emfanhsh apotelesmaton alfabitika
        if ("Alphabetical".equals(sortCombo.getSelectedItem())) {
            list.sort(Comparator.comparing(SearchResult::getTitle, String.CASE_INSENSITIVE_ORDER));
        }
        
        // ypologismos apotelesmaton ana selida
        int total = list.size();
        int perPage = searchController.getResultsPerPage();
        int pages = (int)Math.ceil(total/(double)perPage);
        
        if (pageNo >= pages) {
        	pageNo = pages-1;
        }
        
        int start = pageNo*perPage;
        int end = Math.min(start+perPage, total);
        
        // prosthikh ton gui apotelesmaton sto container me ta apotelesmata ths selidas
        for (SearchResult r : list.subList(start, end)) {
            resultsContainer.add(buildResultPanel(r));
            resultsContainer.add(Box.createVerticalStrut(8));
        }
        
        statusLabel.setText("Found " + total + " results – page " + (pageNo+1) + "/" + Math.max(pages,1));
        
        rebuildPager(pages);
        
        // emfanish ton apotelesmaton
        resultsContainer.revalidate();
        resultsContainer.repaint();
        scrollPane.getVerticalScrollBar().setValue(0);
    }

    // dhmiourgia leitourgias allaghs selidas
    private void rebuildPager(int pages) {
        pagerPanel.removeAll();
        
        if (pages<=1) {
        	pagerPanel.revalidate(); pagerPanel.repaint();
        	return;
        }
        
        // phgaine sthn arxikh
        JButton first = new JButton("<<"); first.setEnabled(currentPage>0);
        first.addActionListener(e -> { currentPage=0; renderPage(currentPage); });
        
        // prohgoumenh selida
        JButton prev = new JButton("<"); prev.setEnabled(currentPage>0);
        prev.addActionListener(e -> { currentPage--; renderPage(currentPage); });
        
        pagerPanel.add(first);
        pagerPanel.add(prev);
        
        
        // emfanish 3 prohgoumenes kai 3 epomenes selides
        int start = Math.max(0, currentPage-3);
        int end = Math.min(pages-1, currentPage+3);
        
        for (int i=start; i<=end; i++) {
            JButton b = new JButton(String.valueOf(i+1));
            b.setEnabled(i!=currentPage);
            
            int page=i;
            b.addActionListener(e -> {
            	currentPage=page;
            	renderPage(currentPage);
            });
            
            pagerPanel.add(b);
        }
        
        // epomenh selida
        JButton next = new JButton(">"); next.setEnabled(currentPage<pages-1);
        next.addActionListener(e -> { currentPage++; renderPage(currentPage); });
        
        // teleftaia selida
        JButton last = new JButton(">>"); last.setEnabled(currentPage<pages-1);
        last.addActionListener(e -> { currentPage=pages-1; renderPage(currentPage); });
        
        pagerPanel.add(next); pagerPanel.add(last);
        pagerPanel.revalidate(); pagerPanel.repaint();
    }

    // dhmiourgia panel enos apotelesmatos
    private JPanel buildResultPanel(SearchResult r) {
    	// dhmiourgia enos panel
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(new EmptyBorder(5,5,5,5)); p.setOpaque(false);
        
        // prosthikh titlou kai link sto arthro
        JLabel title = new JLabel("<html><a href='"+r.getUrl()+"'>"+r.getTitle()+"</a></html>");
        title.setFont(title.getFont().deriveFont(Font.BOLD,15f));
        title.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        title.addMouseListener(new MouseAdapter() { @Override public void mouseClicked(MouseEvent e) {
            try { Desktop.getDesktop().browse(URI.create(r.getUrl())); } catch (IOException ignored) {}
        }});
        
        // prosthhkh tou url
        JLabel url = new JLabel(r.getUrl()); url.setFont(url.getFont().deriveFont(Font.PLAIN,11f));
        url.setForeground(Color.GRAY);
        
        // prosthhkh tou description kai elegxos ton lejeon gia na ginoun bold
        String snippetHtml = highlightAndWrap(r.getSnippet(), searchController.getLastQuery(), MAX_CHARS_PER_LINE);
        JLabel snippetLbl = new JLabel("<html>"+snippetHtml+"</html>"); snippetLbl.setFont(snippetLbl.getFont().deriveFont(Font.PLAIN,13f));
        
        // prosthkh tou date published
        JLabel dateLbl = new JLabel("Date Published: " + r.getPublishedDate());
        dateLbl.setFont(url.getFont().deriveFont(Font.PLAIN,11f)); dateLbl.setForeground(Color.GRAY);
        
        p.add(title);
        p.add(url);
        p.add(snippetLbl);
        p.add(dateLbl);
        
        return p;
    }

    // allagh grammhs tou description enos arthrou
    private static String wrapTextHard(String html, int maxChars) {
        StringBuilder out = new StringBuilder(); int visible=0; boolean inTag=false;
        for (char ch: html.toCharArray()) {
            if (ch=='<') inTag=true;
            if (!inTag) {
                if (visible>=maxChars && Character.isWhitespace(ch)) { out.append("<br>"); visible=0; }
                else visible++;
            }
            out.append(ch);
            if (ch=='>') inTag=false;
        }
        return out.toString();
    }

    // bold stis lejeis poy exoun to keyword
    private String highlightAndWrap(String text, String query, int maxChars) {
        if (text==null)
        	return "";
        
        query = query.replaceAll("^\\w+:","");
        
        for (String term: query.replaceAll("[\\W]+"," ").trim().split(" ")) {
            if (term.isBlank()) continue;
            String regex = "(?i)(?<!\\p{L})("+Pattern.quote(term)+")(?!\\p{L})";
            text = text.replaceAll(regex, "<b>$1</b>");
        }
        
        return wrapTextHard(text, maxChars);
    }

    // setup tou index sto programma
    private void afterIndexReady() {
        if (progressDlg != null)
        	progressDlg.dispose();
        
        try {
            SearchService service = new SearchService("resources/index");
            searchController = new SearchController(service);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Cannot open index:\n"+ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    // Dhmiourgia neou index kai fortosh sto programma
    private void buildIndexAsync() {
        progressDlg = new JDialog(this, "Building index...", true);
        progressDlg.setLayout(new BorderLayout(10,10));
        progressDlg.add(new JLabel("Building index, please wait..."), BorderLayout.NORTH);
        
        JProgressBar bar = new JProgressBar(); bar.setIndeterminate(true);
        progressDlg.add(bar, BorderLayout.CENTER);
        progressDlg.setSize(350, 110);
        progressDlg.setLocationRelativeTo(this);
        
        SwingWorker<Void,Void> worker = new SwingWorker<>() {
            @Override protected Void doInBackground() {
                indexController.rebuildIndex(); return null;
            }
            @Override protected void done() {
                progressDlg.dispose(); afterIndexReady();
            }
        };
        
        worker.execute();
        progressDlg.setVisible(true);
    }

    // dhmiourgia placeholder sto search field se periptosh pou o xrhsths den exei kanei focus
    private void installPlaceholder(JTextField field, String placeholder) {
        field.setForeground(Color.GRAY); field.setText(placeholder);
        field.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                if (field.getText().equals(placeholder)) { field.setText(""); field.setForeground(Color.BLACK); }
            }
            @Override public void focusLost(FocusEvent e) {
                if (field.getText().isBlank()) { field.setForeground(Color.GRAY); field.setText(placeholder); }
            }
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(SearchEngineNewsGUI::new);
    }
}
