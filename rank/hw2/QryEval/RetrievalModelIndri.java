/**
 *  Copyright (c) 2018, Carnegie Mellon University.  All Rights Reserved.
 */

/**
 *  An object that stores parameters for the unranked Boolean
 *  retrieval model (there are none) and indicates to the query
 *  operators how the query should be evaluated.
 */
public class RetrievalModelIndri extends RetrievalModel {

    public double lambda, mu;

    public String defaultQrySopName () {
        return new String ("#and");
    }

    public RetrievalModelIndri(double lambda, double mu) {
        this.lambda = lambda;
        this.mu = mu;
    }

}
