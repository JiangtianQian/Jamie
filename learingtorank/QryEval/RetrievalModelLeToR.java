/**
 *  Copyright (c) 2018, Carnegie Mellon University.  All Rights Reserved.
 */
import java.util.Map;
/**
 *  An object that stores parameters for the unranked Boolean
 *  retrieval model (there are none) and indicates to the query
 *  operators how the query should be evaluated.
 */
public class RetrievalModelLeToR extends RetrievalModel {

    /*public double k1, b , k3, mu, lambda;
    String featureDisable;*/

    Map<String, String> parameters;


    public String defaultQrySopName () {
        return null;
    }

    public RetrievalModelLeToR(Map<String, String> parameters) {
        this.parameters = parameters;
    }

}
