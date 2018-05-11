/**
 *  Copyright (c) 2018, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.*;
import java.util.*;

/**
 *  The QrySopSum operator for all retrieval models.
 */
public class QrySopSum extends QrySop {


    /**
     *  Indicates whether the query has a match.
     *  @param r The retrieval model that determines what is a match
     *  @return True if the query matches, otherwise false.
     */
    public boolean docIteratorHasMatch (RetrievalModel r) {
        return this.docIteratorHasMatchMin(r);
    }

    public double getDefaultScore(RetrievalModel r, int doc_id) throws IOException {
        return 0;
    }

    /**
     *  Get a score for the document that docIteratorHasMatch matched.
     *  @param r The retrieval model that determines how scores are calculated.
     *  @return The document score.
     *  @throws IOException Error accessing the Lucene index
     */
    public double getScore (RetrievalModel r) throws IOException {

        if (r instanceof RetrievalModelBM25) {
            return this.getScoreBM25 (r);
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
    private double getScoreBM25 (RetrievalModel r) throws IOException {
        if (!this.docIteratorHasMatchCache()) {
            return 0.0;
        }
        double score = 0;
        for (int i = 0; i < this.args.size(); i++) {
            Qry list = this.args.get(i);
            if (list.docIteratorHasMatchCache() && list.docIteratorGetMatch() == this.docIteratorGetMatch()) {
                score = score +  ((QrySop)list).getScore(r);
            }
        }
        return score;
    }
}
