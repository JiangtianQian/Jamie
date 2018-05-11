import java.io.*;
import java.util.*;

public class RetrievalModelDiversity {
    private Map<String, String> parameters;

    // Store initial ranking of query
    // <Docid, Scorelist>
    private Map<String, ScoreList> initialQry;
    // <query id, <Doc id, scorelist>>
    private Map<String, HashMap<String, ScoreList>> initialQryIntentRanking;
    // <Query id, intent id list>
    private Map<String, ArrayList<String>> initialQryIntentList;

    // intents list reading from the intent file <intent id, intent content list>
    Map<String, ArrayList<String>> intentsContentList;


    public void diversity(Map<String, String> parameters, RetrievalModel model) throws Exception {
        // Initial the required parameters
        this.parameters = parameters;

        String filename = this.parameters.get("queryFilePath");
        String qLine = null;
        BufferedReader input = null;
        input = new BufferedReader(new FileReader(filename));

        String outputPath = parameters.get("trecEvalOutputPath");

        BufferedWriter output = new BufferedWriter(new FileWriter(outputPath));

        // check whether the initial ranking files and intents have been provided
        if (this.parameters.containsKey("diversity:initialRankingFile")) {
            if (this.parameters.get("diversity:initialRankingFile").length() != 0) {
                readQueryAndItent();

                ScoreList qryScore;
                HashMap<String, ScoreList> intentScore = new HashMap<>();

                while ((qLine = input.readLine()) != null) {
                    int d = qLine.indexOf(':');

                    if (d < 0) {
                        throw new IllegalArgumentException
                                ("Syntax error:  Missing ':' in query line.");
                    }


                    String qid = qLine.substring(0, d);

                    System.out.println("Query " + qLine);

                    qryScore = this.initialQry.get(qid);

                    intentScore = this.initialQryIntentRanking.get(qid);

                    calculate(qryScore, intentScore, qid, output);
                }
            }
        } else {
            readIntents();
            ScoreList qryScore;

            while ((qLine = input.readLine()) != null) {
                HashMap<String, ScoreList> intentScores = new HashMap<>();
                int d = qLine.indexOf(':');

                if (d < 0) {
                    throw new IllegalArgumentException
                            ("Syntax error:  Missing ':' in query line.");
                }

                String qid = qLine.substring(0, d);
                String query = qLine.substring(d + 1);

                System.out.println("Query " + qLine);


                qryScore = QryEval.processQuery(query, model);
                qryScore.sort();/////////////////////////////////////


                //read intent qi from the diversity:intentsFile file;
                ArrayList<String> intentList = this.initialQryIntentList.get(qid);
                ArrayList<String> contents = this.intentsContentList.get(qid);
                // <Intent id, scorelist>

                for (int i = 0; i < intentList.size(); i++) {
                    String intent_id = intentList.get(i);
                    String content = contents.get(i);
                    //use query qi to retrieve documents;
                    ScoreList score = QryEval.processQuery(content, model);
                    score.sort();
                    intentScores.put(intent_id, score);
                }

                calculate(qryScore, intentScores, qid, output);
            }
        }

        output.close();
    }

    /**
     * read relevance-based document rankings for query q from the
     * diversity:initialRankingFile file;
     * Store the read in doc in Hashmap, the key is query id and
     * the value is the score list.
     */
    public void readQueryAndItent() throws Exception {
        this.initialQry = new HashMap<>();
        this.initialQryIntentList = new HashMap<>();
        this.initialQryIntentRanking = new HashMap<>();

        String fileName = parameters.get("diversity:initialRankingFile");
        BufferedReader br = new BufferedReader(new FileReader(fileName));

        String previousId = "-1";
        String line;
        ScoreList r = new ScoreList();

        while ((line = br.readLine()) != null) {
            String[] sub = line.split(" ");
            String qid = sub[0].trim();
            int docid = Idx.getInternalDocid(sub[2].trim());

            // Next query
            if (!qid.equals(previousId)) {
                previousId = qid;
                r = new ScoreList();

                // query intent
                if (sub[0].indexOf('.') != -1) {
                    int index = sub[0].indexOf('.');
                    String intent_q = sub[0].substring(0, index);
                    String intent_id = sub[0].substring(index + 1);

                    if (!this.initialQryIntentList.containsKey(intent_q)) {
                        ArrayList<String> list = new ArrayList<>();
                        list.add(intent_id);
                        HashMap<String, ScoreList> scorelist = new HashMap<String, ScoreList>();
                        scorelist.put(intent_id, r);
                        this.initialQryIntentList.put(intent_q, list);
                        this.initialQryIntentRanking.put(intent_q, scorelist);
                    } else {
                        this.initialQryIntentList.get(intent_q).add(intent_id);
                        this.initialQryIntentRanking.get(intent_q).put(intent_id, r);
                    }
                } else {
                    this.initialQry.put(previousId, r);
                }
            }
            r.add(docid, Double.parseDouble(sub[4].trim()));
        }
        br.close();
    }

    /**
     * read intent qi from the diversity:intentsFile file;
     * Build intents content list and intent id list for each query
     */
    public void readIntents() throws Exception {
        this.initialQryIntentList = new HashMap<>();
        this.intentsContentList = new HashMap<>();

        String fileName = this.parameters.get("diversity:intentsFile");
        BufferedReader br = new BufferedReader(new FileReader(fileName));

        String line = null;
        while ((line = br.readLine()) != null) {
            String[] sub = line.split(":");
            int index = sub[0].indexOf('.');
            String q_id = sub[0].trim().substring(0, index);
            String intent_id = sub[0].substring(index + 1);
            if (!this.intentsContentList.containsKey(q_id)) {
                this.intentsContentList.put(q_id, new ArrayList<>());
            }
            this.intentsContentList.get(q_id).add(sub[1].trim());

            if (!this.initialQryIntentList.containsKey(q_id)) {
                this.initialQryIntentList.put(q_id, new ArrayList<>());
            }
            this.initialQryIntentList.get(q_id).add(intent_id);
        }
        br.close();
    }


    public void calculate(ScoreList queryScore, HashMap<String, ScoreList> intentScore, String qid,
                          BufferedWriter output) throws Exception {
        ArrayList<double[]> scores = scaling(queryScore, intentScore);

        // Stoer docid and rank for reference
        HashMap<Integer, Integer> docIdMap = new HashMap<>();
        // Get doc id at rank i
        for (int j = 0; j < queryScore.size(); j++) {
            docIdMap.put(j, queryScore.getDocid(j));
        }

        ScoreList res = null;
        String algorithm = this.parameters.get("diversity:algorithm");
        if (algorithm.toLowerCase().equals("xquad")) {
            res = xQuad(scores, docIdMap);
        }

        if (algorithm.toLowerCase().equals("pm2")) {
            res = PM2(scores, docIdMap);
        }

        if (res != null) {
            res.sort();
            String outputLength = "100";
            if (parameters.containsKey("trecEvalOutputLength"))
                outputLength = parameters.get("trecEvalOutputLength");
            else if (parameters.containsKey("diversity:maxResultRankingsLength"))
                outputLength = parameters.get("diversity:maxResultRankingsLength");


            QryEval.printResults(qid, res, outputLength, output);
            //output.flush();
        }
    }

    public ArrayList<double[]> scaling(ScoreList qryScore,
                                       HashMap<String, ScoreList> intentScore) throws Exception {

        boolean scale = false;
        int numOfIntent = intentScore.size();
        //System.out.println(numOfIntent);

        ArrayList<double[]> tmp_score = new ArrayList<>();
        HashMap<Integer, Integer> rank = new HashMap<>();
        int inputRankingLen = Integer.parseInt(parameters.get("diversity:maxInputRankingsLength"));
        int length1 = Math.min(inputRankingLen, qryScore.size());

        for (int j = 0; j < length1; j++) {
            double[] arr = new double[numOfIntent + 1];
            double score = qryScore.getDocidScore(j);

            arr[0] = score;
            tmp_score.add(arr);
            if (score > 1.0) {
                scale = true;
            }
            rank.put(qryScore.getDocid(j), j);
        }
        for (String key : intentScore.keySet()) {
            ScoreList r = intentScore.get(key);
            int length_r = Math.min(length1, r.size());
            for (int k = 0; k < length_r; k++) {
                int docid = r.getDocid(k);
                if (rank.containsKey(docid)) {
                    int index = rank.get(docid);
                    double score2 = r.getDocidScore(k);
                    tmp_score.get(index)[Integer.parseInt(key)] = score2;
                    if (score2 > 1.0) {
                        scale = true;
                    }
                }
            }
        }
        if (scale == true) {
            double[] sum = new double[numOfIntent + 1];
            double max = -1;
            for (int m = 0; m < tmp_score.size(); m++) {
                double[] arr_tmp = tmp_score.get(m);
                for (int n = 0; n < numOfIntent + 1; n++) {
                    sum[n] += arr_tmp[n];
                    if (sum[n] > max) {
                        max = sum[n];
                    }
                }
            }

            for (double d : sum) {
                if (d > max) {
                    max = d;
                }
            }


            for (int p = 0; p < tmp_score.size(); p++) {
                for (int q = 0; q < numOfIntent + 1; q++) {
                    tmp_score.get(p)[q] = tmp_score.get(p)[q] / max;
                }
            }
        }
        return tmp_score;
    }

    public ScoreList xQuad(ArrayList<double[]> scores,
                           HashMap<Integer, Integer> docIdMap) throws Exception {
        ScoreList r = new ScoreList();
        int qsize = scores.get(0).length - 1;
        double intentWeight = 1.0 / qsize;

        int length = Integer.parseInt(parameters.get("diversity:maxResultRankingLength"));
        HashSet<Integer> initialRankingSet = new HashSet<>();
        for (int i = 0; i < scores.size(); i++) {
            initialRankingSet.add(i);
        }

        ArrayList<Double> currentCoverage = new ArrayList<>();


        int prev_maxScoreDoc = -1;

        while (r.size() < length && r.size() < initialRankingSet.size()) {

            int curr_maxScoreDoc = -1;
            double maxScore = -1;


            int size = currentCoverage.size();
            for (int i = 0; i <= qsize; i++) {
                if (size == 0) {
                    currentCoverage.add(1.0);
                } else {
                    if (i != 0) {
                        currentCoverage.set(i, currentCoverage.get(i) * (1.0 - scores.get(prev_maxScoreDoc)[i]));
                    }

                }
            }

            for (int rankDocId : initialRankingSet) {
                double[] qryScore = scores.get(rankDocId);
                double intentScore = 0;

                for (int j = 1; j <= qsize; j++) {
                    intentScore += qryScore[j] * currentCoverage.get(j);
                }
                intentScore *= intentWeight;

                double lambda = Double.parseDouble(parameters.get("diversity:lambda"));

                double score = (1 - lambda) * qryScore[0] + lambda * intentScore;

                if (maxScore < score) {
                    maxScore = score;
                    curr_maxScoreDoc = rankDocId;
                    prev_maxScoreDoc = rankDocId;
                }
            }

            initialRankingSet.remove(curr_maxScoreDoc);
            r.add(docIdMap.get(curr_maxScoreDoc), maxScore);
        }
        return r;
    }

    // PM2 method
    public ScoreList PM2(ArrayList<double[]> scores,
                         HashMap<Integer, Integer> docIdMap) throws Exception{
        ScoreList r = new ScoreList();
        int qsize = scores.get(0).length - 1;
        HashSet<Integer> initialRankingSet = new HashSet<>();
        /////////////////////////////////////////////////////////
        for(int i = 0; i < scores.size(); i++) {
            initialRankingSet.add(i);
        }

        double intentWeight = 1.0 / qsize;

        // initial votes
        int length = Integer.parseInt(parameters.get("diversity:maxResultRankingLength"));
        int maxOutputSize = Math.min(length, initialRankingSet.size());
        double vi = intentWeight * maxOutputSize;

        // initial slots
        double[] si = new double[qsize + 1];

        while(r.size() < maxOutputSize){
            double maxScore = -1;
            int maxScoreDoc = -1;

            // Priority of each intent now
            double[] qti = new double[qsize + 1];
            double iStarScore = -1;
            // The intent to cover now
            int iStar = -1;

            for (int i = 1; i <= qsize; i++) {
                double qt = vi / (2 * si[i] + 1);
                qti[i] = qt;
                if(iStarScore < qt){
                    iStarScore = qt;
                    iStar = i;
                }
            }

            // Tell if all the score are zero
            boolean flag = true;

            // PM2 score dStar
            for(int rank: initialRankingSet) {
                double[] qryScore = scores.get(rank);


                // Covers qi for target
                double coverIntent = iStarScore * qryScore[iStar];

                //Covers other itents
                double coverOtherIntents = 0.0;

                for(int j = 1; j <= qsize; j++) {
                    if (j != iStar){
                        coverOtherIntents += qryScore[j] * qti[j];

                    }
                }
                double lambda = Double.parseDouble(parameters.get("diversity:lambda"));
                double score =  lambda * coverIntent + (1 - lambda) * coverOtherIntents;
                if (score != 0) {
                    flag = false;
                }
                if(maxScore < score){
                    maxScore = score;
                    maxScoreDoc = rank;
                }
            }

            // Sometimes when PM2 is constructing a ranking, it reaches a point where all
            // of the intent scores for all of the remaining candidate documents are zero.
            // This causes all of the diversified document scores to be zero.
            // How should PM2 handle this case?

            //Complete the ranking using the remaining candidate documents in relevance order.
            // Assign them scores that maintain the ranking order
            // (i.e., diversified document scores > relevance-only document scores).

            //How do you assign the score of d3 and d4 so that they are in relevance order,
            // while at the same time they don't rank higher than d1 and d2?

            // That is the max score is 0. Times it with 0.99 to make it smaller than the documents we
            // want to append behind;

            if (flag) {
                int length1 = Integer.parseInt(parameters.get("diversity:maxResultRankingLength"));
                for (int mm = 0; mm < length1 && mm < scores.size(); mm++) {
                    if(initialRankingSet.contains(mm)) {
                        double score = r.getDocidScore(r.size() - 1) * 0.0000001;
                        r.add(docIdMap.get(mm), score);
                        initialRankingSet.remove(mm);
                    }
                }
                break;
            } else {
                r.add(docIdMap.get(maxScoreDoc), maxScore);
                initialRankingSet.remove(maxScoreDoc);
                // update coverage of each intent
                double[] qryScore = scores.get(maxScoreDoc);
                double sum = 0.0;
                for(int i = 1; i <= qsize; i++) {
                    sum += qryScore[i];
                }

                for(int i = 1; i <= qsize; i++) {
                    si[i] += qryScore[i] / sum;
                }
            }
        }
        return r;
    }
}
