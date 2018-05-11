/**
 *  Copyright (c) 2018, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.*;

/**
 *  The AND operator for all retrieval models.
 */
public class QrySopAnd extends QrySop {

    /**
     *  Indicates whether the query has a match.
     *  @param r The retrieval model that determines what is a match
     *  @return True if the query matches, otherwise false.
     */
    public boolean docIteratorHasMatch (RetrievalModel r) {
        return this.docIteratorHasMatchAll(r);
    }

    /**
     *  Get a score for the document that docIteratorHasMatch matched.
     *  @param r The retrieval model that determines how scores are calculated.
     *  @return The document score.
     *  @throws IOException Error accessing the Lucene index
     */
    public double getScore (RetrievalModel r) throws IOException {

        if (r instanceof RetrievalModelUnrankedBoolean) {
            return this.getScoreUnrankedBoolean (r);
        } else if (r instanceof RetrievalModelRankedBoolean) {
            return this.getScoreRankedBoolean (r);
        } else {
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
    private double getScoreUnrankedBoolean (RetrievalModel r) throws IOException {
        /*if (!this.docIteratorHasMatchCache()) {
            return 0.0;
        } else {
            return 1.0;
        }*/
        for (int i = 0; i < this.args.size();i++){
            Qry list =this.args.get(i);
            if (!list.docIteratorHasMatchCache() || list.docIteratorGetMatch() != this.docIteratorGetMatch()) {
                return 0.0;
            }
        }
        return 1.0;
    }

    /**
     *  getScore for the UnrankedBoolean retrieval model.
     *  @param r The retrieval model that determines how scores are calculated.
     *  @return The document score.
     *  @throws IOException Error accessing the Lucene index
     */
    private double getScoreRankedBoolean (RetrievalModel r) throws IOException {
        if (!this.docIteratorHasMatchCache()) {
            return 0.0;
        }
        double minScore = Double.MAX_VALUE;
        for (int i = 0; i < this.args.size(); i++) {
            Qry list = this.args.get(i);
            //System.out.println(list);
            if (list.docIteratorHasMatchCache() && list.docIteratorGetMatch() == this.docIteratorGetMatch()) {
                if (((QrySop)list).getScore(r) < minScore) {
                    minScore = ((QrySop)list).getScore(r);
                }
            }
        }
        return minScore;
    }
}
