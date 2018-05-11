/**
 *  Copyright (c) 2018, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.*;
import java.lang.IllegalArgumentException;

/**
 *  The SCORE operator for all retrieval models.
 */
public class QrySopScore extends QrySop {

  /**
   *  Document-independent values that should be determined just once.
   *  Some retrieval models have these, some don't.
   */

  /**
   *  Indicates whether the query has a match.
   *  @param r The retrieval model that determines what is a match
   *  @return True if the query matches, otherwise false.
   */
  public boolean docIteratorHasMatch (RetrievalModel r) {
    return this.docIteratorHasMatchFirst (r);
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
    } else if (r instanceof  RetrievalModelBM25) {
        return this.getScoreBM25(r);
    } else if (r instanceof  RetrievalModelIndri) {
        return this.getScoreIndri(r);
    } else {
      throw new IllegalArgumentException
        (r.getClass().getName() + " does not support the SCORE operator.");
    }
  }

  /**
   *  getScore for the Unranked retrieval model.
   *  @param r The retrieval model that determines how scores are calculated.
   *  @return The document score.
   *  @throws IOException Error accessing the Lucene index
   */
  public double getScoreUnrankedBoolean (RetrievalModel r) throws IOException {
    if (! this.docIteratorHasMatchCache()) {
      return 0.0;
    } else {
      return 1.0;
    }
  }
  /*
  * *
   *  getScore for the Ranked retrieval model.
   *  @param r The retrieval model that determines how scores are calculated.
   *  @return The document score.
   *  @throws IOException Error accessing the Lucene index
   */
  public double getScoreRankedBoolean (RetrievalModel r) throws IOException {
      QryIop query = this.getArg(0);
      if (query.docIteratorHasMatch(r)) {
        return query.docIteratorGetMatchPosting().tf;
      }else {
        return 0.0;
      }
  }



  /*
   * *
   *  getScore for the Ranked retrieval model.
   *  @param r The retrieval model that determines how scores are calculated.
   *  @return The document score.
   *  @throws IOException Error accessing the Lucene index
   */
  public double getScoreBM25 (RetrievalModel r) throws IOException {
    //double df = 0, tf = 0, qtf = 0, k1 = 0, b = 0, k3 = 0;
    QryIop q = this.getArg(0);
    if (q.docIteratorHasMatch(r)) {
      // idf weight
      double N = Idx.getNumDocs();
      double df = q.getDf();
      double RSJ = Math.max(0,Math.log((N - df + 0.5) / (df + 0.5)));

      // tf weight
      double tf = q.docIteratorGetMatchPosting().tf;
      String field = q.getField();
      double do_clen = Idx.getFieldLength(field, q.docIteratorGetMatch());
      double avg_len = Idx.getSumOfFieldLengths(field) /(double) Idx.getDocCount(field);
      double k1 =((RetrievalModelBM25)r).k1;
      double b = ((RetrievalModelBM25)r).b;
      double tf_weight = tf / (tf + k1 * ((1- b) + b * do_clen / avg_len));

      // user weight
      double k3 = ((RetrievalModelBM25)r).k3;
      double qtf = 1; // We don't have duplicate query term in our homework
      double user_weight = (k3 + 1) * qtf / (k3 + qtf);

      double score = RSJ * tf_weight * user_weight;
      return score;

    }
    return 0.0;
  }

    /**
     *  getScore for the Unranked retrieval model.
     *  @param r The retrieval model that determines how scores are calculated.
     *  @return The document score.
     *  @throws IOException Error accessing the Lucene index
     */
    public double getScoreIndri (RetrievalModel r) throws IOException {
        if (this.args.get(0).docIteratorHasMatch(r)) {
            QryIop q = this.getArg(0);
            int docid = q.docIteratorGetMatch();
            String field = q.getField();
            double tf = q.docIteratorGetMatchPosting().tf;
            double lambda = ((RetrievalModelIndri)r).lambda;
            double mu = ((RetrievalModelIndri)r).mu;
            double p_mle =(double)q.getCtf() / (double)Idx.getSumOfFieldLengths(field);
            double len_doc = Idx.getFieldLength(field, docid);
            //System.out.println(lambda);
            //System.out.println(1 -lambda);
            double score = (1 - lambda) * (tf + mu * p_mle) / (len_doc + mu) + lambda * p_mle;
            return score;
        }
        return 0;
    }



    /**
     *  getScore for the Unranked retrieval model.
     *  @param r The retrieval model that determines how scores are calculated.
     *  @return The document score.
     *  @throws IOException Error accessing the Lucene index
     */
    public double getDefaultScore (RetrievalModel r, int docid) throws IOException {
        //System.out.println("Enter get default score");
        double lambda = ((RetrievalModelIndri)r).lambda;
        double mu = ((RetrievalModelIndri)r).mu;
        QryIop q = this.getArg(0);
        String field = q.getField();
        double p_mle = 0.0;
        if (q.getCtf() == 0) {
            p_mle = 0.5 / (double)Idx.getSumOfFieldLengths(field);
        } else {
            p_mle = (double)q.getCtf() / (double)Idx.getSumOfFieldLengths(field);
        }

        double len_doc = Idx.getFieldLength(field, docid);
        double score = (1 - lambda) * mu * p_mle / (len_doc + mu) + lambda * p_mle;
        return score;
    }



    /**
   *  Initialize the query operator (and its arguments), including any
   *  internal iterators.  If the query operator is of type QryIop, it
   *  is fully evaluated, and the results are stored in an internal
   *  inverted list that may be accessed via the internal iterator.
   *  @param r A retrieval model that guides initialization
   *  @throws IOException Error accessing the Lucene index.
   */
  public void initialize (RetrievalModel r) throws IOException {

    Qry q = this.args.get (0);
    q.initialize (r);
  }

}
