/**
 *  Copyright (c) 2018, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.*;
import java.util.*;
/**
 *  The AND operator for all retrieval models.
 */
public class QrySopWand extends QrySop {

    /**
     *  Indicates whether the query has a match.
     *  @param r The retrieval model that determines what is a match
     *  @return True if the query matches, otherwise false.
     */
    public boolean docIteratorHasMatch (RetrievalModel r) {
        if (r instanceof RetrievalModelIndri) {
            return this.docIteratorHasMatchMin(r);
        }
        return this.docIteratorHasMatchAll(r);
    }

    /**
     *  Get a score for the document that docIteratorHasMatch matched.
     *  @param r The retrieval model that determines how scores are calculated.
     *  @return The document score.
     *  @throws IOException Error accessing the Lucene index
     */
    public double getScore (RetrievalModel r) throws IOException {

        if (r instanceof RetrievalModelIndri) {
            return this.getScoreWand(r);
        } else {
            throw new IllegalArgumentException
                    (r.getClass().getName() + " doesn't support the And operator.");
        }
    }

    /**
     * getDefaultScore for implement QrySop
     * @param r
     * @param docid
     * @return
     * @throws IOException
     */
    public double getDefaultScore (RetrievalModel r, int docid) throws IOException {

        double score = 1.0;
        double weight_sum = 0.0;
        int j = 0;
        for (int i = 0; i < this.args.size(); i++) {
            weight_sum += this.weight.get(i);
        }
        for (Qry q : this.args) {
            score *= Math.pow(((QrySop)q).getDefaultScore(r, docid), this.weight.get(j) / weight_sum);
            j++;
        }
        return score;
    }

    /**
     *  getScore for the Indri retrieval model.
     *  @param r The retrieval model that determines how scores are calculated.
     *  @return The document score.
     *  @throws IOException Error accessing the Lucene index
     */
    private double getScoreWand(RetrievalModel r) throws IOException {
        double score = 1.0;
        int docid = this.docIteratorGetMatch();
        double weight_sum = 0.0;
        int j = 0;
        for (int i = 0; i < this.args.size(); i++) {
            weight_sum += this.weight.get(i);
        }

        for (Qry q : this.args) {
            if (q.docIteratorHasMatch(r) && q.docIteratorGetMatch() == docid){
                score *= Math.pow(((QrySop)q).getScore(r), this.weight.get(j) / weight_sum);
            } else {
                score *= Math.pow(((QrySop)q).getDefaultScore(r, docid), this.weight.get(j) / weight_sum) ;
            }
            j++;
        }
        //System.out.println("Score" + score);
        //System.out.println("score" + Math.pow(score, 1.0 / this.args.size()));
        return score;
    }
}
