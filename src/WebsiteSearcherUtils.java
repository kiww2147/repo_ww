import java.io.*;
import java.util.HashSet;
import java.util.Map;

/**
 * A utility class to deal with files. It will read an input file containing URLs and load them into a Set
 * It will also write the results of a scan to a results file
 */
public class WebsiteSearcherUtils {

    //Function scans the given file and loads urls into a HashSet
    public HashSet<String> loadUrls(String fileName) throws FileNotFoundException, IOException{

        HashSet<String> urlsList = new HashSet<String>();
        String line = null;
        String url = null;

            FileReader fileReader = new FileReader(fileName);
            BufferedReader bufferedReader = new BufferedReader(fileReader);

            //Skipping header line of the file
            bufferedReader.readLine();

            while ((line = bufferedReader.readLine()) != null) {

                //fetching URL from second entry of each line and replacing quotes
                url = line.split(",")[1].replace("\"", "");

                //Appending an http://www to each url since this is the entry which most
                //web servers in the sample seem to have for their sites
                if (!url.startsWith("http://www.")) {
                    url = "http://www." + url;
                }
                urlsList.add(url);
            }

            bufferedReader.close();

        return urlsList;
    }

    //Function to write the scan results from a map into a file
    public void writeResults(Map<String, String> urlsVisited, String resultsFileName) throws IOException{
        BufferedWriter bw = null;
        FileWriter fw = null;

        try {
            fw = new FileWriter(resultsFileName);
            bw = new BufferedWriter(fw);

            //Header line in results file
            bw.write("URL, ContainsSearchkey?\n");
            for(String url: urlsVisited.keySet()){
                bw.write(url + ", " + urlsVisited.get(url) + "\n");
            }
        }
        finally {
            if (bw != null) {
                bw.close();
            }
            if (fw != null) {
                fw.close();
            }
        }
    }
}
