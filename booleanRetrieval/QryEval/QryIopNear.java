/**
 *  Copyright (c) 2018, Carnegie Mellon University.  All Rights Reserved.
 */
import java.io.*;
import java.util.*;

/**
 *  The Near operator for all retrieval models.
 */
public class QryIopNear extends QryIop {
    private int operatorDistance;
    public QryIopNear(int operatorDistance) {
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
            Vector<Integer> location_left = query.docIteratorGetMatchPosting().positions;

            for (Qry term : this.args) {
                //System.out.println("a");
                // The temporary list to store the intermediate location results
                Vector<Integer> tmp = new Vector<Integer>();

                if (term.docIteratorGetMatch() != docId) {
                    break;
                }

                // The second term's location list
                Vector<Integer> location_right = ((QryIop) term).docIteratorGetMatchPosting().positions;

                // Initialize parameter for the recursive location finding
                int size_left = location_left.size();
                int size_right = location_right.size();
                int count_left = 0;
                int count_right = 0;

                while (true) {
                    //Take care of the index out of boundary problem
                    if (count_right >= size_right || count_left >= size_left) {
                        break;
                    }
                    // When the left location is lager then right, the right one advance
                    // because we need right location larger than left location
                    if (location_left.get(count_left) > location_right.get(count_right)) {
                        if (count_right >= size_right - 1) {
                            break;
                        } else {
                            count_right++;
                        }
                    }
                    // When the left and right difference is smaller than specific distance, add right location
                    // to final results
                    else if (location_right.get(count_right) - location_left.get(count_left) <= this.operatorDistance) {
                        tmp.add(location_right.get(count_right));
                        if (count_left >= size_left - 1 || count_right >= size_right - 1) {
                            break;
                        }
                        // After find the suitable pair, advance left and right location, for we want each
                        // pair be find just once
                        count_left++;
                        count_right++;
                    }
                    // When left is small than right and there is no suitable pair at this time,
                    // advance left
                    else {
                        if (count_left >= size_left - 1) {
                            break;
                        }
                        count_left++;
                    }
                }
                // If there is now match from the left to right, we can break here
                // to avoid meaningless calculation.
                location_left = tmp;
                if (location_left.size() == 0) {
                    break;
                }
            }
            Collections.sort(location_left);
            if (!location_left.isEmpty()) {
                this.invertedList.appendPosting(docId, location_left);
            }
            this.args.get(0).docIteratorAdvancePast(docId);
        }
    }
}
