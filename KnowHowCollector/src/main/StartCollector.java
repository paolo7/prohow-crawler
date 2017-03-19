package main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import edu.uci.ics.crawler4j.crawler.CrawlConfig;
import edu.uci.ics.crawler4j.crawler.CrawlController;
import edu.uci.ics.crawler4j.fetcher.PageFetcher;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtConfig;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtServer;
import edu.uci.ics.crawler4j.url.WebURL;

public class StartCollector {

	//TODO: multithread different instructions
	public static void main(String[] args) {
		// Iterate over all the collecting instructions.
		// They must be propriety files in a folder named "instructions"
		Logger.log("INITIATION: Iterating over all the instruction files");
		File dir = new File("instructions");
		  File[] directoryListing = dir.listFiles();
		  if (directoryListing != null) {
		    for (File child : directoryListing) {
		    	try {
					if (followInstructions(child)) Logger.log("COMPLETED: instructions "+child);
					else Logger.log("ABORTED: instructions "+child);
				} catch (IOException e) {
					Logger.log("ERROR: problem reading file "+child);
					e.printStackTrace();
				}
		    }
		  } else {
			Logger.log("INITIATION ABORTED: no instruction file found");
		    // Handle the case where dir is not really a directory.
		    // Checking dir.isDirectory() above would not be sufficient
		    // to avoid race conditions with another process that deletes
		    // directories.
		  }
	}
	
	private static boolean followInstructions(File f) throws IOException{
		Logger.log("Start: reading file "+f.getName());
		FileInputStream fileInput = new FileInputStream(f);
		Properties properties = new Properties();
		properties.load(fileInput);
		fileInput.close();
		// Read all the properties
		String filename = f.getName().substring(0, f.getName().indexOf('.'));
		properties.setProperty("filename", filename);
		if(!properties.containsKey("seeds")){
			Logger.log("ERROR: Aborting instructions "+f+" because there are no seeds");
			return false;
		}
		String seedsString = properties.getProperty("seeds");
		// parse the space separated string
		String[] seeds = seedsString.split("\\s+");
		
		// Summarize the properties
		Logger.log("Properties read:");
		Logger.log(" - Filename for results:");
		Logger.log("   . "+properties.getProperty("filename"));
		Logger.log(" - Seeds: "+seeds.length);
		//for(String s: seeds) Logger.log("   . "+s);
		
		// Apply the properties:
		String crawlStorageFolder = "crawl/"+filename;
		int numberOfCrawlers = 1;
		CrawlConfig config = new CrawlConfig();
		config.setCrawlStorageFolder(crawlStorageFolder);
		config.setPolitenessDelay(Integer.valueOf(properties.getProperty("PolitenessDelay","1000")));
		config.setMaxDepthOfCrawling(Integer.valueOf(properties.getProperty("MaxDepthOfCrawling","-1")));
		config.setMaxPagesToFetch(Integer.valueOf(properties.getProperty("MaxNumPagesToFetch","-1")));
		config.setResumableCrawling(Boolean.valueOf(properties.getProperty("ResumableCrawling","true")));
		//config.setUserAgentString("");
		boolean crawlImages = Boolean.valueOf(properties.getProperty("CrawlImages","false"));
		if(crawlImages){
			config.setIncludeBinaryContentInCrawling(true);
		}
		/*
		 * Instantiate the controller for this crawl.
		 */
		PageFetcher pageFetcher = new PageFetcher(config);
		RobotstxtConfig robotstxtConfig = new RobotstxtConfig();
		RobotstxtServer robotstxtServer = new RobotstxtServer(robotstxtConfig, pageFetcher)	 {
			public boolean allows(WebURL arg0){return true;}
		};
		CrawlController controller;
		try {
			controller = new MyCrawlerController(config, pageFetcher, robotstxtServer, properties);
			for(String s : seeds) controller.addSeed(s);
			
			File additional_seeds = new File("seeds.txt");
			if(additional_seeds.exists()){
				int additional_seeds_number = 0;
				try (BufferedReader br = new BufferedReader(new FileReader(additional_seeds))) {
				    String line;
				    while ((line = br.readLine()) != null) {
				       controller.addSeed(line);
				       additional_seeds_number++;
				    }
				}
				Logger.log("External seed file seeds.txt contained: "+additional_seeds_number);
			} else {
				Logger.log("No external seed file seeds.txt was found.");
			}
			
			Logger.log("START: The crawling is starting");
			controller.startNonBlocking(KnowHowCrawlerImplementation.class, numberOfCrawlers);
			Logger.log("END: The crawling is finished");
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		// Finished the instructions
		return true;
	}

}
