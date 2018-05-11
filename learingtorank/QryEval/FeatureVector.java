import com.sun.javafx.iio.ios.IosDescriptor;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.*;


public class FeatureVector {
    // Empty feature vector
    public HashMap<Integer, Double> featurevector;
    public HashSet<Integer> featureDisable;
    public RetrievalModelLeToR r;
    double k1,b,k3,mu,lambda;
    public Map<String, String> parameter;



    //Map<query_id, Map<external_id, relevance_score>>
    public HashMap<String, HashMap<String, String>> rel;


    public FeatureVector(RetrievalModelLeToR r) {
        featurevector = new HashMap<>();
        featureDisable = new HashSet<>();

        String featureDisables = r.parameters.get("letor:featureDisable");
        this.k1 = Double.parseDouble(r.parameters.get("BM25:k_1"));
        this.b = Double.parseDouble(r.parameters.get("BM25:b"));
        this.k3 = Double.parseDouble(r.parameters.get("BM25:k_3"));
        this.mu = Double.parseDouble(r.parameters.get("Indri:mu"));
        this.lambda = Double.parseDouble(r.parameters.get("Indri:lambda"));
        this.parameter = r.parameters;

        if(parameter.containsKey("letor:featureDisable")){
            for(String s : featureDisables.split(",")) {
                featureDisable.add(Integer.parseInt(s));
            }
        }
    }

    public HashMap<Integer, Double> getFeaturevector(String[] terms, String extenalId) throws Exception {

        try {
            int docid = Idx.getInternalDocid(extenalId);
            if (docid != -1) {
                featurevector = new HashMap<>();

                // f1
                if (!featureDisable.contains(1)) {
                    try {
                        int spamScore = Integer.parseInt(Idx.getAttribute("spamScore", docid));
                        featurevector.put(1, spamScore * 1.0);
                    } catch (Exception e) {
                        featurevector.put(1, -1.0);
                    }

                }

                // f2
                if (!featureDisable.contains(2)) {
                    try{
                        String rawUrl = Idx.getAttribute("rawUrl", docid);
                        int count = rawUrl.replaceAll("[^/]", "").length() - 2;
                        featurevector.put(2, count * 1.0);
                    } catch (Exception e) {
                        featurevector.put(2, -1.0);
                    }

                }

                // f3
                if (!featureDisable.contains(3)) {
                    try {
                        String rawUrl = Idx.getAttribute("rawUrl", docid);
                        int index = rawUrl.indexOf("wikipedia.org");
                        if (index != -1) {
                            featurevector.put(3, 1.0);
                        } else {
                            featurevector.put(3, 0.0);
                        }
                    } catch (Exception e) {
                        featurevector.put(3, -1.0);
                    }

                }

                // f4
                if (!featureDisable.contains(4)) {
                    try {
                        float prScore = Float.parseFloat(Idx.getAttribute("PageRank", docid));

                        featurevector.put(4, prScore * 1.0);
                    }catch (Exception e) {
                        featurevector.put(4, -1.0);
                    }

                }

                // f5
                if (!featureDisable.contains(5)) {
                    try {
                        featurevector.put(5, featureBM25(terms, docid, "body"));
                    } catch (Exception e) {
                        featurevector.put(5, -1.0);
                    }
                }

                // f6
                if (!featureDisable.contains(6)) {
                    try {
                        featurevector.put(6, featureIndri(terms, docid, "body"));
                    }catch (Exception e) {
                        featurevector.put(6, -1.0);
                    }

                }

                // f7
                if (!featureDisable.contains(7)) {
                    try {
                        featurevector.put(7, featureOverlap(terms, docid, "body"));
                    }catch (Exception e) {
                        featurevector.put(7, -1.0);
                    }
                }

                // f8
                if (!featureDisable.contains(8)) {
                    try {
                        featurevector.put(8, featureBM25(terms, docid, "title"));
                    } catch (Exception e) {
                        featurevector.put(8, -1.0);
                    }
                }

                // f9
                if (!featureDisable.contains(9)) {
                    try {
                        featurevector.put(9, featureIndri(terms, docid, "title"));
                    }catch (Exception e) {
                        featurevector.put(9, -1.0);
                    }
                }

                // f10
                if (!featureDisable.contains(10)) {
                    try {
                        featurevector.put(10, featureOverlap(terms, docid, "title"));
                    } catch (Exception e) {
                        featurevector.put(10, -1.0);
                    }
                }

                // f11
                if (!featureDisable.contains(11)) {
                    try {
                        featurevector.put(11, featureBM25(terms, docid, "url"));
                    } catch (Exception e) {
                        featurevector.put(11, -1.0);
                    }
                }

                // f12
                if (!featureDisable.contains(12)) {
                    try {
                        featurevector.put(12, featureIndri(terms, docid, "url"));
                    }catch (Exception e) {
                        featurevector.put(12, -1.0);
                    }
                }

                // f13
                if (!featureDisable.contains(13)) {
                    try {
                        featurevector.put(13, featureOverlap(terms, docid, "url"));
                    } catch (Exception e) {
                        featurevector.put(13, -1.0);
                    }
                }

                // f14
                if (!featureDisable.contains(14)) {
                    try {
                        featurevector.put(14, featureBM25(terms, docid, "inlink"));
                    } catch (Exception e) {
                        featurevector.put(14, -1.0);
                    }
                }

                // f15
                if (!featureDisable.contains(15)) {
                    //System.out.println("=======");
                    try {
                        featurevector.put(15, featureIndri(terms, docid, "inlink"));
                    } catch (Exception e) {
                        featurevector.put(15, -1.0);
                    }
                }

                // f16
                if (!featureDisable.contains(16)) {
                    try {
                        featurevector.put(16, featureOverlap(terms, docid, "inlink"));
                    } catch (Exception e) {
                        featurevector.put(16, -1.0);
                    }

                }

                // f17 overlap of (terms in query) and
                // (terms in document body field / (length of whole field * size of query))
                if(!featureDisable.contains(17)) {
                    try {
                        featurevector.put(17, featureBodyOverlap(terms, docid, "body"));
                    } catch (Exception e) {
                        featurevector.put(17, -1.0);
                    }
                }

                /*if (!featureDisable.contains(17)) {
                    try {
                        int titleLen = Idx.getFieldLength("title", docid);
                        //System.out.println(titleLen);

                        featurevector.put(17, titleLen * 1.0);
                    } catch (Exception e) {
                        featurevector.put(17, -1.0);
                    }

                }*/


                // f18 authority top-level domains
                if (!featureDisable.contains(18)) {
                    try {
                        String rawUrl = Idx.getAttribute("rawUrl", docid);
                        String[] domain = new String[]{".net", ".gov", ".edu", ".mil", ".org", ".com", ".int"};
                        boolean flag = false;

                        for (String s : domain) {
                            if (rawUrl.indexOf(s) != -1) {
                                flag = true;
                            }
                        }

                        if (flag == true) {
                            featurevector.put(18, 1.0);
                        } else {
                            featurevector.put(18, 0.0);
                        }
                    } catch (Exception e) {
                        featurevector.put(18, -1.0);
                    }
                }

            }

        } catch (Exception e){

        }

        return featurevector;
    }

    public Double featureBodyOverlap(String[] queryTerms, int docid, String field) throws IOException {

        TermVector term = new TermVector(docid, field);
        int count = 0;

        for (String t : queryTerms) {
            int index  = term.indexOfStem(t);
            if (index > -1) {
                count++;
            }
        }

        long divid = Idx.getSumOfFieldLengths(field) * queryTerms.length;
        return count * 1.0 / queryTerms.length * 1.0 / (divid);
    }

    public Double featureBM25(String[] queryTerms, int docid, String field) throws IOException {
        double score = 0.0;

            double k1 = this.k1;
            double b =  this.b;
            double k3 = this.k3;
            // tokenize the query before calling featureBM25
            //featureBM25 (queryStems, docid, field):
            TermVector term = new TermVector(docid, field);
            //for each stem in <docid, field>
            //if stem is a queryStem
            //score += BM25 term score for stem

            double df;
            double tf;
            double docLen = Idx.getFieldLength(field, docid);
            long docNum = Idx.getNumDocs();
            double avgLen = Idx.getSumOfFieldLengths(field) /(double) Idx.getDocCount(field);
            double user_weight = (k3 + 1) * 1 / (k3 + 1);

            if (term.positionsLength()!= 0 && term.stemsLength()!= 0) {
                for (String t : queryTerms) {
                    int index  = term.indexOfStem(t);
                    if (index != -1) {
                        df = term.stemDf(index);
                        tf = term.stemFreq(index);
                        double RSJ = Math.max(0,Math.log((docNum - df + 0.5) / (df + 0.5)));
                        double tf_weight = tf / (tf + k1 * ((1 - b) + b * docLen / avgLen));
                        score += RSJ * tf_weight * user_weight; //

                        //System.out.println(score);
                    }
                }
            }



        return score;
    }

    public Double featureIndri(String[] queryTerms, int docid, String field) throws IOException {
        double score = 1.0;
        double mu = this.mu;
        double lambda = this.lambda;
        double corpusLen = (double)Idx.getSumOfFieldLengths(field);
        double len_doc = Idx.getFieldLength(field, docid);

        TermVector term = new TermVector(docid, field);

        boolean count = false;

        if (term.stemsLength() != 0 && term.positionsLength() != 0 ) {
            for (String t : queryTerms) {
                int index = term.indexOfStem(t);
                double p_mle = Idx.getTotalTermFreq(field, t) / corpusLen;
                double tf = 0.0;
                if (index != -1) {
                    tf = (double)term.stemFreq(index);
                    //System.out.println(tf);
                    count = true;
                }
                score *= (1.0 - lambda) * (tf + mu * p_mle) / (len_doc + mu) + lambda * p_mle;
            }

            if (count) {
                return Math.pow(score, 1.0 / (double)queryTerms.length);
            } else {
                return 0.0;
            }
        }
        return 0.0;
    }

    public Double featureOverlap(String[] queryTerms, int docid, String field) throws IOException {

        TermVector term = new TermVector(docid, field);
        double count = 0.0;
        if (term.stemsLength() != 0 && term.positionsLength() != 0 ) {
            for (String t : queryTerms) {
                int index = term.indexOfStem(t);
                if (index != -1) {
                    count = count + 1.0;
                }
            }
        }
        return count / (double)queryTerms.length;
    }


    // Read query relevant file
    public void relevanceFile(String file, boolean flag) throws IOException {

        this.rel = new HashMap<>();
        BufferedReader input = null;
        try {
            input = new BufferedReader(new FileReader(file));
            String line;
            while ((line = input.readLine()) != null) {
                String[] ids = line.split(" ");
                String qid = ids[0].trim();
                String externalId = ids[2].trim();

                String relScore;
                if (flag == true) {
                    relScore = ids[4].trim();
                } else {
                    relScore = ids[3].trim();
                }
                rel.putIfAbsent(qid, new HashMap<>());
                rel.get(qid).put(externalId, relScore);
                //System.out.println(relScore);////////////////////////////////
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            input.close();
        }

    }

    //Read and query file and Calculate feature vectors for training documents;
    // Normalize the feature vector
    public void calculateFeature(String trainfile, String outputFile) throws IOException{
        BufferedReader input = null;
        BufferedWriter writer = null;
        // Speed up
        // <ExternalId, qid, score>
        HashMap<String, HashMap<Integer, Double>> queryFeatureVector = new HashMap<>();

        try {
            String line;
            input = new BufferedReader(new FileReader(trainfile));
            while ((line = input.readLine()) != null) {
                int idx = line.indexOf(':');

                String qid = line.substring(0, idx);
                String query = line.substring(idx + 1);
                // Tokenize before call BM25
                String[] terms = QryParser.tokenizeString(query);

                // compute query feature vector for each term
                HashMap<String, String> relevantDoc = this.rel.get(qid);

                //System.out.println(relevantDoc.size());

                for (String key : relevantDoc.keySet()){
                    HashMap<Integer, Double> tmpFeature = getFeaturevector(terms, key);

                    queryFeatureVector.put(key, tmpFeature);
                }

                // Normalize the feature values for query q to [0..1]
                HashMap<Integer,Double> minMap = new HashMap<>();
                HashMap<Integer,Double> maxMap = new HashMap<>();
                //writer = new BufferedWriter(new FileWriter(
                //  new File(this.parameter.get("letor:trainingFeatureVectorsFile"))));

                writer = new BufferedWriter(new FileWriter((new File(outputFile))));
                //System.out.println("=============================");
                //System.out.println(writer);



                int featureNumber = 18;
                for (int i = 1; i <= featureNumber; i++) {
                    if (!this.featureDisable.contains(i)) {
                        maxMap.put(i, Double.MIN_VALUE);
                        minMap.put(i, Double.MAX_VALUE);
                    }
                }


                /*for (String key : queryFeatureVector.keySet()) {
                    HashMap<Integer, Double> tmp = queryFeatureVector.get(key);
                    for (int i = 1; i <= featureNumber; i++) {
                        System.out.println(tmp.get(i));
                    }
                    System.out.println("========");
                }*/

                Set<String> col = queryFeatureVector.keySet();
                List<String> keyset = new ArrayList<String>(col);
                Collections.sort(keyset);

                for (String key : queryFeatureVector.keySet()) {
                    HashMap<Integer, Double> tmp = queryFeatureVector.get(key);
                    if (tmp != null) {
                        for (int i = 1; i <= featureNumber; i++) {
                            if ((featureDisable == null || !this.featureDisable.contains(i)) && tmp.get(i) != -1) {

                                //System.out.println(tmp.size());

                                double value = tmp.get(i);

                               // System.out.println(value);
                                maxMap.put(i, Math.max(maxMap.get(i), value));


                                minMap.put(i, Math.min(minMap.get(i), value));

                            }
                        }
                    }
                }

               /*for (int i = 1; i <= featureNumber; i++) {
                    System.out.println("==============");
                    System.out.println(maxMap.get(i));

                    System.out.println(minMap.get(i));
                    System.out.println("==============");
                }*/

                for (int m = 0; m < keyset.size(); m++) {
                    String key = keyset.get(m);

                    HashMap<Integer, Double> tmp = queryFeatureVector.get(key);

                    if (tmp == null) {
                        continue;
                    }


                    String relScore = this.rel.get(qid).get(key);
                    String output = String.format("%s qid:%s ", relScore, qid);

                    if (relScore == null) {
                        continue;
                    }


                    for (int i = 1; i <= featureNumber; i++) {

                        if (!this.featureDisable.contains(i)) {
                            double value = tmp.get(i);
                            double maxValue = maxMap.get(i);
                            double minValue = minMap.get(i);

                            if((value != -1)) {
                                if (minValue == maxValue) {
                                    output += String.format("%d:%.16f ", i, 0.0);
                                } else {
                                    output += String.format("%d:%.16f ", i, (value - minValue) / (maxValue - minValue));

                                    //System.out.println(output);
                                }
                            }


                        }
                    }
                    output += String.format("# %s", key);



                    writer.write(output);
                    writer.newLine();

                }


            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            input.close();
            writer.close();
        }
    }

    // Call SVMrank to train a retrieval model;
    public void trainSVM() throws Exception{
        String svmC = this.parameter.get("letor:svmRankParamC");
        String execPath = this.parameter.get("letor:svmRankLearnPath");
        String modelOutputFile = this.parameter.get("letor:svmRankModelFile");
        String qrelsFeatureOutputFile = this.parameter.get("letor:trainingFeatureVectorsFile");

        //System.out.println("Enter trainSVM");

        // SVM software

        // runs svm_rank_learn from within Java to train the model
        // execPath is the location of the svm_rank_learn utility,
        // which is specified by letor:svmRankLearnPath in the parameter file.
        // FEAT_GEN.c is the value of the letor:c parameter.
        Process cmdProc = Runtime.getRuntime().exec(
                new String[] { execPath, "-c", String.valueOf(svmC), qrelsFeatureOutputFile, modelOutputFile });

        // The stdout/stderr consuming code MUST be included.
        // It prevents the OS from running out of output buffer space and stalling.
        // consume stdout and print it out for debugging purposes
        /*BufferedReader stdoutReader = new BufferedReader(
                new InputStreamReader(cmdProc.getInputStream()));
        String line;
        while ((line = stdoutReader.readLine()) != null) {
            System.out.println(line);
        }
        // consume stderr and print it for debugging purposes
        BufferedReader stderrReader = new BufferedReader(
                new InputStreamReader(cmdProc.getErrorStream()));
        while ((line = stderrReader.readLine()) != null) {
            System.out.println(line);
        }
        */
        // get the return value from the executable. 0 means success, non-zero
        // indicates a problem

        int retValue = cmdProc.waitFor();
        if (retValue != 0) {
            throw new Exception("SVM Rank crashed.");
        }

    }

    // Read test queries from an input file;
    // Use BM25 to get inital rankings for test queries;
    //  Calculate feature vectors for the top 100 ranked documents (for each query);
    // Write the feature vectors to a file;
    // Call SVMrank to calculate scores for test documents;
    public void readTest() throws Exception{

        RetrievalModelBM25 BM25 = new RetrievalModelBM25(this.k1, this.b,this.k3);
        QryEval.processQueryFile(this.parameter.get("queryFilePath"),
                this.parameter.get("trecEvalOutputPath"),"100", BM25);
        relevanceFile(this.parameter.get("trecEvalOutputPath"), true);

        calculateFeature(this.parameter.get("queryFilePath"), this.parameter.get("letor:testingFeatureVectorsFile"));
        testSVM();
    }


    public void testSVM() throws Exception{
        String classifyPath = this.parameter.get("letor:svmRankClassifyPath");
        String execPath = this.parameter.get("letor:testingFeatureVectorsFile");
        String modelOutputFile = this.parameter.get("letor:svmRankModelFile");
        String testDocumentScore = this.parameter.get("letor:testingDocumentScores");

        // SVM software

        // runs svm_rank_learn from within Java to train the model
        // execPath is the location of the svm_rank_learn utility,
        // which is specified by letor:svmRankLearnPath in the parameter file.
        // FEAT_GEN.c is the value of the letor:c parameter.
        Process cmdProc = Runtime.getRuntime().exec(
                new String[] {classifyPath, execPath, modelOutputFile, testDocumentScore });
        // The stdout/stderr consuming code MUST be included.
        // It prevents the OS from running out of output buffer space and stalling.
        // consume stdout and print it out for debugging purposes

        /* BufferedReader stdoutReader = new BufferedReader(
                new InputStreamReader(cmdProc.getInputStream()));
        String line;
       while ((line = stdoutReader.readLine()) != null) {
            System.out.println(line);
        }
        // consume stderr and print it for debugging purposes
        BufferedReader stderrReader = new BufferedReader(
                new InputStreamReader(cmdProc.getErrorStream()));
        while ((line = stderrReader.readLine()) != null) {
            System.out.println(line);
        }*/

        // get the return value from the executable. 0 means success, non-zero
        // indicates a problem
        int retValue = cmdProc.waitFor();
        if (retValue != 0) {
            throw new Exception("SVM Rank crashed.");
        }

    }

    /**
     * re-rank test data
     * call svmrank to produce scores for the test data
     * read in the svmrank scores and re-rank the initial
     * ranking based on the scores output re-ranked result into trec_eval format
     * Read the scores produced by SVMrank; and
     * Write the final ranking in trec_eval input format.
     */

    public void reRank() throws Exception {
        String outFilePath = this.parameter.get("trecEvalOutputPath");
        BufferedReader testScore = null;
        BufferedReader inputFile = null;
        ScoreList list = new ScoreList();
        Boolean dummy = true;
        BufferedWriter output = null;

        try {
            testScore = new BufferedReader(
                    new FileReader(new File(parameter.get("letor:testingDocumentScores"))));
            inputFile = new BufferedReader(
                    new FileReader(new File(parameter.get("letor:testingFeatureVectorsFile"))));

            String lineScore = null;
            String lineFile = null;
            String curr = "";

            while(((lineFile = inputFile.readLine()) != null) && ((lineScore = testScore.readLine()) != null)) {
                String[] tmp = lineFile.split(" ");
                String sub = tmp[1];
                String qid = sub.substring(sub.indexOf(":") + 1);
                String extenalId = tmp[tmp.length - 1].trim();
                int docid = Idx.getInternalDocid(extenalId);
                double score = Double.parseDouble(lineScore.trim());

                if(!curr.equals(qid)) {
                    list.sort();
                    if(dummy){
                        dummy = false;
                    } else {
                        output = new BufferedWriter(new FileWriter(outFilePath));
                        QryEval.printResults(curr, list, "100", output);
                        output.flush();
                    }
                    curr = qid;
                    list = new ScoreList();
                }
                list.add(docid, score);
            }
            list.sort();
            QryEval.printResults(curr, list, "100", output);
            //output.close();

        } catch (Exception e) {

        } finally {
            testScore.close();
            inputFile.close();
        }
    }


    public void leToR() throws Exception{
        // Train
        String file = this.parameter.get("letor:trainingQrelsFile");
        relevanceFile(file, false);

        String outputfile = this.parameter.get("letor:trainingFeatureVectorsFile");

        String trainfile = this.parameter.get("letor:trainingQueryFile");
        calculateFeature(trainfile, outputfile);
        trainSVM();

        // Test
        readTest();
        testSVM();

        // reRank()
        reRank();
    }
}
