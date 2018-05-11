/*
 *  Copyright (c) 2018, Carnegie Mellon University.  All Rights Reserved.
 *  Version 3.3.2.
 */
import java.io.*;
import java.util.*;

import org.apache.lucene.analysis.Analyzer.TokenStreamComponents;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

/**
 *  This software illustrates the architecture for the portion of a
 *  search engine that evaluates queries.  It is a guide for class
 *  homework assignments, so it emphasizes simplicity over efficiency.
 *  It implements an unranked Boolean retrieval model, however it is
 *  easily extended to other retrieval models.  For more information,
 *  see the ReadMe.txt file.
 */
public class QryEval {

    //  --------------- Constants and variables ---------------------

    private static final String USAGE =
            "Usage:  java QryEval paramFile\n\n";

    private static final String[] TEXT_FIELDS =
            { "body", "title", "url", "inlink" };

    private static Map<String, String> parameters = null;


    //  --------------- Methods ---------------------------------------

    /**
     *  @param args The only argument is the parameter file name.
     *  @throws Exception Error accessing the Lucene index.
     */
    public static void main(String[] args) throws Exception {

        //  This is a timer that you may find useful.  It is used here to
        //  time how long the entire program takes, but you can move it
        //  around to time specific parts of your code.

        Timer timer = new Timer();
        timer.start ();

        //  Check that a parameter file is included, and that the required
        //  parameters are present.  Just store the parameters.  They get
        //  processed later during initialization of different system
        //  components.



        if (args.length < 1) {
            throw new IllegalArgumentException (USAGE);
        }

        parameters = readParameterFile (args[0]);

        //  Open the index and initialize the retrieval model.

        Idx.open (parameters.get ("indexPath"));
        RetrievalModel model = initializeRetrievalModel (parameters);

        //  Perform experiments.

        processQueryFile(parameters.get("queryFilePath"), parameters.get("trecEvalOutputPath"),
                parameters.get("trecEvalOutputLength"),model);

        //  Clean up.

        timer.stop ();
        System.out.println ("Time:  " + timer);
    }

    /**
     *  Allocate the retrieval model and initialize it using parameters
     *  from the parameter file.
     *  @return The initialized retrieval model
     *  @throws IOException Error accessing the Lucene index.
     */
    private static RetrievalModel initializeRetrievalModel (Map<String, String> parameters)
            throws IOException {

        RetrievalModel model = null;
        String modelString = parameters.get ("retrievalAlgorithm").toLowerCase();

        if (modelString.equals("unrankedboolean")) {
            model = new RetrievalModelUnrankedBoolean();
        } else if (modelString.equals("rankedboolean")) {
            model = new RetrievalModelRankedBoolean();
        } else if (modelString.equals("bm25")) {
            double k1 =  Double.parseDouble(parameters.get ("BM25:k_1"));
            double b =  Double.parseDouble(parameters.get ("BM25:b"));
            double k3 =  Double.parseDouble(parameters.get ("BM25:k_3"));
            if (k1 < 0.0 || k3 < 0.0 || b < 0.0 || b > 1.0) {
                throw new IllegalArgumentException
                        ("Invalid k1 or b or k3 " + parameters.get("retrievalAlgorithm"));
            }
            model = new RetrievalModelBM25(k1, b, k3);
        }  else if (modelString.equals("indri")) {
            double mu =  Double.parseDouble(parameters.get ("Indri:mu"));
            double lambda =  Double.parseDouble(parameters.get ("Indri:lambda"));
            if (mu < 0 || lambda > 1.0 || lambda < 0.0) {
                throw new IllegalArgumentException
                        ("Invalid mu or lambda " + parameters.get("retrievalAlgorithm"));
            }
            model = new RetrievalModelIndri(lambda,mu);
        } else {
            throw new IllegalArgumentException
                    ("Unknown retrieval model " + parameters.get("retrievalAlgorithm"));
        }

        return model;
    }

    /**
     * Print a message indicating the amount of memory used. The caller can
     * indicate whether garbage collection should be performed, which slows the
     * program but reduces memory usage.
     *
     * @param gc
     *          If true, run the garbage collector before reporting.
     */
    public static void printMemoryUsage(boolean gc) {

        Runtime runtime = Runtime.getRuntime();

        if (gc)
            runtime.gc();

        System.out.println("Memory used:  "
                + ((runtime.totalMemory() - runtime.freeMemory()) / (1024L * 1024L)) + " MB");
    }

    /**
     * Process one query.
     * @param qString A string that contains a query.
     * @param model The retrieval model determines how matching and scoring is done.
     * @return Search results
     * @throws IOException Error accessing the index
     */
    static ScoreList processQuery(String qString, RetrievalModel model)
            throws IOException {

        String defaultOp = model.defaultQrySopName ();
        qString = defaultOp + "(" + qString + ")";
        Qry q = QryParser.getQuery (qString);

        // Show the query that is evaluated

        System.out.println("    --> " + q);

        if (q != null) {

            ScoreList r = new ScoreList ();

            if (q.args.size () > 0) {   // Ignore empty queries

                q.initialize (model);

                while (q.docIteratorHasMatch (model)) {
                    int docid = q.docIteratorGetMatch ();
                    double score = ((QrySop) q).getScore (model);
                    r.add (docid, score);
                    q.docIteratorAdvancePast (docid);
                }
            }
            return r;
        } else
            return null;
    }

    /**
     *  Process the query file.
     *  @param queryFilePath
     *  @param model
     *  @throws IOException Error accessing the Lucene index.
     */
    static void processQueryFile(String queryFilePath, String outFilePath, String length, RetrievalModel model)
            throws IOException {

        BufferedReader input = null;
        BufferedWriter output = null;
        BufferedWriter expansionWriter = null;
        //PrintWriter expansionWriter = null;

        try {
            String qLine = null;

            input = new BufferedReader(new FileReader(queryFilePath));
            output = new BufferedWriter(new FileWriter(outFilePath));

            String fbExpansionQueryFile = parameters.get("fbExpansionQueryFile");

            if (fbExpansionQueryFile != null) {
                expansionWriter = new BufferedWriter(new FileWriter(fbExpansionQueryFile));
            }
            //  Each pass of the loop processes one query.

            while ((qLine = input.readLine()) != null) {
                int d = qLine.indexOf(':');

                if (d < 0) {
                    throw new IllegalArgumentException
                            ("Syntax error:  Missing ':' in query line.");
                }

                printMemoryUsage(false);

                String qid = qLine.substring(0, d);
                String query = qLine.substring(d + 1);

                System.out.println("Query " + qLine);

                ScoreList r = null;



                boolean fb = Boolean.valueOf(parameters.get("fb"));


                if (fb == true) {
                    String defaultOp = model.defaultQrySopName ();
                    query = defaultOp + "(" + query + ")";
                    Qry q = QryParser.getQuery (query);
                    RetrievalModelIndri Indri = (RetrievalModelIndri) model;
                    int q_id = Integer.parseInt(qid);
                    //QryExpansion(q, query, Indri, q_id);

                    String queryExpanded = expadedQry (q, query, Indri, q_id);



                    expansionWriter.write(String.format("%s: %s\n", qid, queryExpanded));
                    expansionWriter.flush();

                    double fbOrigWeight = Double.parseDouble(parameters.get("fbOrigWeight"));

                   // System.out.println(1 - fbOrigWeight);

                    query = String.format("#wand(%f %s %f %s )", fbOrigWeight, query, 1 - fbOrigWeight, queryExpanded);

                    //System.out.println(query);

                }

                r = processQuery(query, model);

                if (r != null) {
                    printResults(qid, r, length, output);

                    System.out.println();
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            input.close();
            output.close();
            if (expansionWriter != null) {
                expansionWriter.close();
            }
        }
    }

    /**
     * Print the query results.
     *
     * THIS IS NOT THE CORRECT OUTPUT FORMAT. YOU MUST CHANGE THIS METHOD SO
     * THAT IT OUTPUTS IN THE FORMAT SPECIFIED IN THE HOMEWORK PAGE, WHICH IS:
     *
     * QueryID Q0 DocID Rank Score RunID
     *
     * @param queryName
     *          Original query.
     * @param result
     *          A list of document ids and scores
     * @throws IOException Error accessing the Lucene index.
     */
    static void printResults(String queryName, ScoreList result, String length, BufferedWriter output) throws IOException {

        System.out.println(queryName + ":  ");
        result.sort();
        if (result.size() < 1) {
            String str = String.format("%s Q0 %s %s %s %s\n",
                    queryName, "dummyRecord","1", "0","fubar");
            System.out.println(str);
            output.write(str);
        } else {
            for (int i = 0; i < Math.min(result.size(),Integer.valueOf(length)); i++) {
                String str = String.format("%s Q0 %s %d %.18f %s\n",
                        queryName, Idx.getExternalDocid(result.getDocid(i)),i + 1,result.getDocidScore(i),"fubar");
                System.out.println(str);
                output.write(str);
            }
        }
    }


    /**
     *  Read the specified parameter file, and confirm that the required
     *  parameters are present.  The parameters are returned in a
     *  HashMap.  The caller (or its minions) are responsible for processing
     *  them.
     *  @return The parameters, in <key, value> format.
     */
    private static Map<String, String> readParameterFile (String parameterFileName)
            throws IOException {

        Map<String, String> parameters = new HashMap<String, String>();

        File parameterFile = new File (parameterFileName);

        if (! parameterFile.canRead ()) {
            throw new IllegalArgumentException
                    ("Can't read " + parameterFileName);
        }

        Scanner scan = new Scanner(parameterFile);
        String line = null;
        do {
            line = scan.nextLine();
            String[] pair = line.split ("=");
            parameters.put(pair[0].trim(), pair[1].trim());
        } while (scan.hasNext());

        scan.close();

        if (! (parameters.containsKey ("indexPath") &&
                parameters.containsKey ("queryFilePath") &&
                parameters.containsKey ("trecEvalOutputPath") &&
                parameters.containsKey ("retrievalAlgorithm"))) {
            throw new IllegalArgumentException
                    ("Required parameters were missing from the parameter file.");
        }

        return parameters;
    }






    private static String expadedQry (Qry q, String qString, RetrievalModelIndri model, int qid) throws IOException {


        ScoreList scorelist = new ScoreList();

        // Check the parameter to see if there any missing
        if (!(parameters.containsKey("fbExpansionQueryFile") && parameters.containsKey("fbTerms") && parameters.containsKey("fbMu")
                && parameters.containsKey("fbOrigWeight"))) {
            throw new IllegalArgumentException("Expansion paramters missing!");
        }

        int fbDocs = Integer.parseInt(parameters.get ("fbDocs"));
        int fbTerms = Integer.parseInt(parameters.get ("fbTerms"));
        double fbMu= Double.parseDouble(parameters.get ("fbMu"));

        // chech if there has ranking file, if there not exists initial file, we create one, else we read it.
        if (!parameters.containsKey("fbInitialRankingFile") || parameters.get("fbInitialRankingFile").equals("")) {
            try {
                scorelist = processQuery(qString, model);
                scorelist.sort();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            scorelist = readRanked(parameters.get("fbInitialRankingFile")).get(qid);
        }

        // Return the candidates with their score

        HashMap<String, Double> candidateTerms = candidates(scorelist, fbDocs, fbMu);
        HashMap<String, Double> candidateTerm = new HashMap<String, Double>();
        //ArrayList<Integer> idList = new ArrayList<>();



        //Timer timer = new Timer();
        //timer.start ();
        // Get score for each term

        //HashMap<String, ArrayList<Integer>> docList = new HashMap<>();

        for (int k = 0; k < scorelist.size() && k < fbDocs; k++ ) {
            int docid = scorelist.getDocid(k);
            TermVector t = new TermVector(docid, "body");
            double docLen = Idx.getFieldLength("body", docid);
            double docScore = scorelist.getDocidScore(k);

            for (String term : candidateTerms.keySet()) {
                double tf = 0.0;
                double score = 0.0;
                double pmle =  candidateTerms.get(term);

                int index  = t.indexOfStem(term);
                if (index > -1) {
                    tf = t.stemFreq(index);
                }

                double ptd = (tf + fbMu * pmle) / (docLen + fbMu);
                double currScore = ptd * docScore * Math.log(1.0 / pmle);
                score = score + currScore;
                if (candidateTerm.containsKey(term)) {
                    candidateTerm.put(term, candidateTerm.get(term) + score);
                } else {
                    candidateTerm.put(term, score);
                }

            }

        }

        /*for (String term : candidateTerms.keySet()) {
            double score = 0.0;
            //ArrayList<Integer> doc_list = invertedList.get(term);
            for(int k = 0; k < scorelist.size() && k < fbDocs; k++) {
                double docScore = scorelist.getDocidScore(k); // Wrong docScore
                int docid = scorelist.getDocid(k);
                double tf = 0;
                double pmle =  candidateTerms.get(term);
                TermVector t = new TermVector(docid, "body");
                double docLen = Idx.getFieldLength("body", docid);

                //double tf = t.stemFreq(k);

                //Timer timer = new Timer();
                //timer.start ();

                int index  = t.indexOfStem(term);
                if (index > -1) {
                    tf = t.stemFreq(index);
                }

                //timer.stop();
                //System.out.println ("Time6:  " + timer);

                double ptd = (tf + fbMu * pmle) / (docLen + fbMu);
                double currScore = ptd * docScore * Math.log(1.0 / pmle);
                score = score + currScore;


            }
            candidateTerm.put(term, score);
        }*/

        //timer.stop();
        //System.out.println ("Time6:  " + timer);



        //System.out.println(candidateTerm.size());
        // Get top n terms
        PriorityQueue<Map.Entry<String, Double>> top = new PriorityQueue<Map.Entry<String, Double>>(
                new Comparator<Map.Entry<String, Double>>() {
                    @Override
                    public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2) {
                        return o2.getValue().compareTo(o1.getValue());
                    }
                }
        );

        top.addAll(candidateTerm.entrySet());



        String add = "";

        for (int i = 0; i < fbTerms; i++) {
            Map.Entry<String, Double> tmp = top.poll();
            add = String.format(" %.4f %s", tmp.getValue(), tmp.getKey()) + add;
        }
        String expandedQry = "#wand(" + add + ")";


        return expandedQry;
    }

    private static HashMap<Integer, ScoreList> readRanked(String fbInitialRankingFile) throws IOException{
        HashMap<Integer, ScoreList> fbScore = new HashMap<Integer, ScoreList>();

        //Timer timer = new Timer();
        //timer.start ();

        BufferedReader br = null;

        try {
            String qLine = null;

            br = new BufferedReader(new FileReader(fbInitialRankingFile));

            //  Each pass of the loop processes one query.

            while ((qLine = br.readLine()) != null) {
                String[] doc = qLine.split(" ");
                int qid = Integer.parseInt(doc[0]);
                double score = Double.parseDouble(doc[4]);


                if (fbScore.containsKey(qid)) {
                    fbScore.get(qid).add(Idx.getInternalDocid(doc[2]), score);
                } else {
                    ScoreList s = new ScoreList();
                    s.add(Idx.getInternalDocid(doc[2]), score);
                    fbScore.put(qid, s);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        //timer.stop();
        //System.out.println ("Time3:  " + timer);

        return fbScore;
    }

    private static HashMap<String, Double> candidates(ScoreList scorelist, int fbDocs, double fbMu) throws IOException{
        HashMap<String, Double> map = new HashMap<>();

        double len = Idx.getSumOfFieldLengths("body");

        for (int i = 0; i < fbDocs && i < scorelist.size(); i++) {

            //System.out.println("docid "  + scorelist.getDocid(i));

            TermVector termVector = new TermVector(scorelist.getDocid(i),"body");

            for (int j = 1; j < termVector.stemsLength(); j++) {

                String term = termVector.stemString(j);

                if (term.contains(",") || term.contains(".")) {
                    continue;
                } else {
                    //double ctf = Idx.getTotalTermFreq("body", term);
                    double ctf = termVector.totalStemFreq(j);
                    double pmle = ctf / len;
                    if(!map.containsKey(term)) {
                        map.put(term, pmle);
                    }
                }
            }
        }
        return map;
    }
}


