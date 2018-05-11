/**
 *  Copyright (c) 2018, Carnegie Mellon University.  All Rights Reserved.
 */

/**
 *  An object that stores parameters for the unranked Boolean
 *  retrieval model (there are none) and indicates to the query
 *  operators how the query should be evaluated.
 */
public class RetrievalModelBM25 extends RetrievalModel {

    public double k1, b , k3;


    public String defaultQrySopName () {
        return new String ("#sum");
    }

    public RetrievalModelBM25(double k1, double b, double k3) {
        this.k1 = k1;
        this.b = b;
        this.k3 = k3;
    }

}
