import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Semaphore;

/**
 * This class scans a file for a list of URLs, scans each webpage in the URL list and searches
 * for the occurrence of a particular string. An output file is then created with the list
 * of URLs along with the result of the search (Yes, No or N/A)
 */
public class WebsiteSearcher {

    private Map<String, String> urlsVisited = new ConcurrentHashMap<>();
    private Set<String> urlsToSearch = new HashSet<>();
    private static WebsiteSearcherUtils wsUtils = new WebsiteSearcherUtils();
    private static final int permitCount = 20;

    //The semaphore will regulate the number of concurrent threads
    Semaphore sem = new Semaphore(permitCount);

    //The constructor loads the URLs to be searched into a set
    public WebsiteSearcher(String fileName){

        try{
            //Loading the urls from the input file
            urlsToSearch = wsUtils.loadUrls(fileName);
        }
        catch (FileNotFoundException fnfe){
            System.out.println("file [" + fileName + "] not found");
        }
        catch(IOException ioe){
            System.out.println("Error when reading from urls file [" + fileName + "]");
        }

        System.out.println("URLs loaded");
    }

    //Function to iterate over the set of URLs and perform a concurrent search for a string
    //The results are stored in the urlsVisited map
    public void searchSites(String key){

        //This list is used to keep track of running threads
        //This is needed to ensure the function only returns when all threads have completed
        List<Thread> threadList = new CopyOnWriteArrayList<>();

        for(String url: urlsToSearch){

            try{
                //First acquire one of the available permits from the semaphore
                sem.acquire();

                //Spawn a new thread to search the page for the given text and update the result in a map
                Thread tb = new Thread() {
                    public void run() {
                        try {
                            String containsText = scanSite(url, key);
                            urlsVisited.put(url,containsText);
                            System.out.println("URL: " + url + "\tContains text? " + containsText);
                        } finally {
                            sem.release();
                            threadList.remove(this);
                        }
                    }
                };
                threadList.add(tb);
                tb.start();
            }
            catch(InterruptedException ie){
                Thread.currentThread().interrupt();
            }
        }

        //Waiting for remaining threads to complete before exiting function
        int running = 0;
        do {
            running = 0;
            for (Thread thread : threadList) {
                if (thread.isAlive()) {
                    running++;
                }
            }
        } while (running > 0);
    }

    //This function searches for a given string in a given webpage and returns the result of the search (Yes, No, N/A)
    String scanSite(String url, String key){
        try{
            Connection connection = Jsoup.connect(url);
            //Loading some pages generates an SSL exception if their certificates are not recognized
            // Some of these can be handled by using the validateTLSCertificates(false) option
            //Document htmlDocument = connection.validateTLSCertificates(false).get();
            Document htmlDocument = connection.get();
            String bodyText = htmlDocument.body().text();
            //The search is case-sensitive by default
            //To be case insensitive, bot bodyText and the key can be changed to lower or upper case
            boolean containsText = bodyText.contains(key);
            return containsText ? "Yes" : "No";

        }
        catch(IOException ex) {
            System.out.println("IO Exception while parsing URL: " + url);
            //If the webpage is not accessible for some reason, return N/A
            return "N/A";
        }
    }

    //Function to write the results of the search from the urlsVisited map to a file
    void writeResults(String resultsFileName){

        try {
            wsUtils.writeResults(urlsVisited, resultsFileName);
        }
        catch(IOException ioe){
            System.out.println("Exception when writing to reports file: ");
            ioe.printStackTrace();
        }
    }

    public static void main(String[] args){

        WebsiteSearcher ws = new WebsiteSearcher(args[0]);
        ws.searchSites(args[1]);
        ws.writeResults("results.txt");
    }
}
