/**
 *  Copyright (c) 2018, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.*;
import java.util.Vector;

/**
 *  The AND operator for all retrieval models.
 */
public class QrySopWsum extends QrySop {

    /**
     *  Indicates whether the query has a match.
     *  @param r The retrieval model that determines what is a match
     *  @return True if the query matches, otherwise false.
     */
    public boolean docIteratorHasMatch (RetrievalModel r) {
        if (r instanceof  RetrievalModelIndri) {
            return this.docIteratorHasMatchMin(r);
        }
        throw new IllegalArgumentException
                (r.getClass().getName() + " doesn't support the WSUM operator.");
    }

    public double getDefaultScore(RetrievalModel r, int doc_id) throws IOException {
        double score = 0.0;
        double weight_sum = 0.0;
        for (int i = 0; i < this.args.size(); i++) {
            weight_sum += this.weight.get(i);
        }
        for (int i = 0; i < this.args.size(); i++) {
            Qry list = this.args.get(i);
            score = score + ((this.weight.get(i) / weight_sum )) *  ((QrySop)list).getDefaultScore(r, doc_id);
        }
        return score;
    }

    /**
     *  Get a score for the document that docIteratorHasMatch matched.
     *  @param r The retrieval model that determines how scores are calculated.
     *  @return The document score.
     *  @throws IOException Error accessing the Lucene index
     */
    public double getScore (RetrievalModel r) throws IOException {

        if (r instanceof RetrievalModelIndri) {
            return this.getScoreWsum (r);
        }  else {
            throw new IllegalArgumentException
                    (r.getClass().getName() + " doesn't support the And operator.");
        }
    }

    /**
     *  getScore for the UnrankedBoolean retrieval model.
     *  @param r The retrieval model that determines how scores are calculated.
     *  @return The document score.
     *  @throws IOException Error accessing the Lucene index
     */
    private double getScoreWsum (RetrievalModel r) throws IOException {
        if (!this.docIteratorHasMatchCache()) {
            return 0.0;
        }
        double score = 0.0;
        double weight_sum = 0.0;
        int docid = this.docIteratorGetMatch();
        for (int i = 0; i < this.args.size(); i++) {
            weight_sum += this.weight.get(i);
        }
        for (int i = 0; i < this.args.size(); i++) {
            Qry list = this.args.get(i);
            if (list.docIteratorHasMatchCache() && list.docIteratorGetMatch() == this.docIteratorGetMatch()) {
                score = score +  (this.weight.get(i) / weight_sum ) * ((QrySop)list).getScore(r);
                //System.out.println("term  " + list);
                //System.out.println("weight  " + this.weight.get(i));

            } else {
                score = score + ((this.weight.get(i) / weight_sum )) *  ((QrySop)list).getDefaultScore(r, docid);
            }
        }
        return score;
    }
}
