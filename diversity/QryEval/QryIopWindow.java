/**
 *  Copyright (c) 2018, Carnegie Mellon University.  All Rights Reserved.
 */
import java.io.*;
import java.util.*;

/**
 *  The Near operator for all retrieval models.
 */
public class QryIopWindow extends QryIop {
    private int operatorDistance;
    public QryIopWindow(int operatorDistance) {
        this.operatorDistance = operatorDistance;
    }

    /**
     *  Evaluate the query operator; the result is an internal inverted
     *  list that may be accessed via the internal iterators.
     *  @throws IOException Error accessing the Lucene index.
     */
    protected void evaluate () throws IOException {

        //  Create an empty inverted list.  If there are no query arguments,
        //  that's the final result.

        this.invertedList = new InvList(this.getField());

        if (args.size() == 0) {
            return;
        }

        // Initialize variable
        QryIop query = (QryIop) this.args.get(0);

        // Find the second argument position which fit the limits and do it
        // recursively until meet the last argument. At this time, the
        // temporary list is our result. The basic idea is # near(a b c) is
        // equals to # near(#(near a b) c).
        while (this.docIteratorHasMatchAll(null)) {
            //System.out.println(final_position);
            int docId = query.docIteratorGetMatch();

            //System.out.println("a");
            // All Ids has been processed, done.
            if (docId == Qry.INVALID_DOCID) {
                break;
            }

            // Because this is a left to right algorithm, we initialize left here
            Vector<Integer> result = new Vector<Integer>();
            // Store the all term's position index here
            int[] curr_position_index = new int[this.args.size()];


            while (true) {

                //System.out.println("a");
                // The temporary list to store the intermediate location results
                //Vector<Integer> tmp = new Vector<Integer>();

                //if (term.docIteratorGetMatch() != docId) {
                //break;
                //}

                // The second term's location list
                //Vector<Integer> location_right = ((QryIop) term).docIteratorGetMatchPosting().positions;

                // Initialize parameter for the recursive location finding
                int max = Integer.MIN_VALUE;
                int min = Integer.MAX_VALUE;
                int min_position_index = -1;
                int i = 0;
                int the_end = 0;

                for (Qry tmp : this.args) {
                    // Find the maximum and minimum index among all terms
                    Vector<Integer> position = ((QryIop)tmp).docIteratorGetMatchPosting().positions;
                    // Reach the end of some position list
                    if (curr_position_index[i] >= position.size()) {
                        the_end = 1;
                        break;
                    }

                    if (max <= position.get(curr_position_index[i])) {
                        max = position.get(curr_position_index[i]);
                    }

                    if (min >= position.get(curr_position_index[i])) {
                        min = position.get(curr_position_index[i]);
                        min_position_index = i;
                    }
                    i++;
                }

                if(the_end == 1) {
                    break;
                }

                if (max - min + 1 <= operatorDistance) {
                    result.add(max);
                    for (int j = 0; j < this.args.size(); j++){
                        curr_position_index[j]++;
                    }
                } else if (max - min + 1 > operatorDistance) {
                    curr_position_index[min_position_index]++;
                }

                // If there is now match from the left to right, we can break here
                // to avoid meaningless calculation.
                //if (result.size() == 0) {
                    //break;
                //}
            }
            Collections.sort(result);
            if (!result.isEmpty()) {
                this.invertedList.appendPosting(docId, result);
            }
            this.args.get(0).docIteratorAdvancePast(docId);
        }
    }
}
