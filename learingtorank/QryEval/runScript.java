import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.*;
import java.util.*;

public class runScript {
    public static void main(String args[]) throws IOException{

        //double i = 0.0;
        //for (int i = 1500; i < 7500; i = i  + 1000) {


            BufferedWriter br = null;
            br = new BufferedWriter(new FileWriter("/Users/kaka/Documents/CMU/searchengine/hw2/QryEval/sample.parm"));

            String fbOrigWeight = "0.5";

            //String fbTerms = ""  + i;
            String fbTerms = "30";

            String fbDocs = "10";
            String name = "E5_10_";
            //String retrievalAlgorithm = "Indri";
        String retrievalAlgorithm = "RankedBoolean";

           String mu = "500";
            //String mu = "" + i;
            String lambda = "0.7";


            String fb = "false";
            //String fbDocs = "10";

            String filename = name + fbOrigWeight + fbTerms+".teIn";



            String fbMu = "0";
            //String fbOrigWeight = "0.5";
            String out = name + fbOrigWeight +fbTerms+ ".qryOut";
            String initial = "fbInitialRankingFile=/Users/kaka/Documents/CMU/searchengine/hw2/QryEval/Indri-Bow.teIn";
            // String initial = "";

            br.write("indexPath=/Users/kaka/Documents/CMU/searchengine/hw2/index" + "\n");
            br.write("queryFilePath=/Users/kaka/Documents/CMU/searchengine/hw2/QryEval/query.txt" + "\n");
            br.write("trecEvalOutputLength=100" + "\n");
            br.write("retrievalAlgorithm="+retrievalAlgorithm + "\n");
            br.write("trecEvalOutputPath=/Users/kaka/Documents/CMU/searchengine/hw2/QryEval/"+ filename+ "\n");

            if (retrievalAlgorithm.equals("Indri")) {
                br.write("Indri:mu="+ mu + "\n");
                br.write("Indri:lambda="+ lambda + "\n");
            }

            if (fb.equals("true")) {
                br.write("fb="+ fb + "\n");
                br.write("fbDocs=" + fbDocs + "\n");
                br.write("fbTerms=" + fbTerms + "\n");
                br.write("fbMu=" + fbMu + "\n");
                br.write("fbOrigWeight=" + fbOrigWeight + "\n");
                br.write("fbExpansionQueryFile=/Users/kaka/Documents/CMU/searchengine/hw2/QryEval/" + out + "\n");
            }


            br.write("fbInitialRankingFile=/Users/kaka/Documents/CMU/searchengine/hw2/QryEval/Indri-Bow.teIn" + "\n");


            br.close();

            String[] input = new String[1];
            input[0] = "/Users/kaka/Documents/CMU/searchengine/hw2/QryEval/sample.parm";
            try {
                QryEval.main(input);
            } catch (Exception e) {
                e.printStackTrace();
            }

        //}




        //Process p = Runtime.getRuntime().exec("python test.py");



    }
}