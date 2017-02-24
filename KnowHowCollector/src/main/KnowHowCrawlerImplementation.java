package main;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;

import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.parser.BinaryParseData;
import edu.uci.ics.crawler4j.parser.HtmlParseData;
import edu.uci.ics.crawler4j.url.WebURL;
import edu.uci.ics.crawler4j.util.IO;

public class KnowHowCrawlerImplementation extends WebCrawler {

	private static int pagesCount = 0;
	private static int htmlCount = 0;
	private static int imageCount = 0;
	
	boolean crawlImages;
	
	private static int logPrintInverseFrequency;
	
	private Properties prop;
	
	private final Pattern FILTERS; 
	private String[] requiredPrefixes;
	private String[] blockedPrefixes;
	
	private final Pattern FILTERStoStore; 
	private String[] requiredPrefixesToStore;
	private String[] blockedPrefixesToStore;
	
	private final Pattern FILTERSimage; 
	private String[] requiredPrefixesImages;
	private String[] blockedPrefixesImages;
	private String[] requiredStringInImageURL;
	private int minImageSize;
	
	private String[] multilingualPrefixes;
	private String[] multilingualCodes;
	private String multilingualDefault;
	private SaveFiles file_saver = null;
	private Long max_file_size;
	
	private Map<String,Integer> language_statistics = new HashMap<String,Integer>();
	
	private boolean wikihowLogics;

	public KnowHowCrawlerImplementation(Properties prop) {
		//prop = ((CrawlConfigWithProperties) this.myController.getConfig()).getProperties();;
		FILTERS = Pattern.compile(prop.getProperty("FilterTypes","\\z\\A"));
		/*= Pattern.compile(".*(\\.(css|js|bmp|gif|jpe?g" + "|png|tiff?|mid|mp2|mp3|mp4"
				+ "|wav|avi|mov|mpeg|ram|m4v|pdf" + "|rm|smil|wmv|swf|wma|zip|rar|gz))$");*/
		requiredPrefixes = prop.getProperty("requiredPrefixes","").split("\\s+");
		blockedPrefixes = prop.getProperty("blockedPrefixes","").split("\\s+");
		max_file_size = Long.valueOf(prop.getProperty("max_file_size","110000000"));
		file_saver = new SaveFiles("CrawlResults1","http://w3id.org/prohowlinks#",max_file_size);
		FILTERStoStore = Pattern.compile(prop.getProperty("FilterTypesToStore","\\z\\A"));
		requiredPrefixesToStore = prop.getProperty("requiredPrefixesToStore","").split("\\s+");
		blockedPrefixesToStore = prop.getProperty("blockedPrefixesToStore","").split("\\s+");
		
		FILTERSimage = Pattern.compile(prop.getProperty("ImageTypes","\\z\\A"));
		requiredPrefixesImages = prop.getProperty("requiredPrefixesImages","").split("\\s+");
		blockedPrefixesImages = prop.getProperty("blockedPrefixesImages","").split("\\s+");
		minImageSize = Integer.valueOf(prop.getProperty("MinImageSize","-1"));
		requiredStringInImageURL = prop.getProperty("requiredStringInImageURL","").split("\\s+");
		
		multilingualPrefixes = prop.getProperty("multilingualAllowed","").split("\\s+");
		multilingualCodes = prop.getProperty("multilingualCodes","").split("\\s+");
		multilingualDefault = prop.getProperty("multilingualEquivalent","");
		
		logPrintInverseFrequency = Integer.valueOf(prop.getProperty("StatusLogInverseFrequency","100"));
		
		crawlImages = Boolean.valueOf(prop.getProperty("CrawlImages","false"));
		
		wikihowLogics = Boolean.valueOf(prop.getProperty("wikihowSpecific","false"));
		
		this.prop = prop;
		
	}
	

	/*private String neutralizeMultilingual(String multiLingualString){
		for(String s : multilingualPrefixes)
			if(multiLingualString.indexOf(s) == 0) 
				return multilingualDefault+multiLingualString.substring(s.length());
		return multiLingualString;
	}*/
	
/*	private boolean isMultilingual(String multiLingualString){
		for(String s : multilingualPrefixes)
			if(multiLingualString.indexOf(s) == 0) 
				return true;
		return false;
	}*/
	
/*	private String getMultilingualCode(String multiLingualString){
		for(int i = 0; i < multilingualPrefixes.length; i++){
			if(multiLingualString.indexOf(multilingualPrefixes[i]) == 0) 
				return multilingualCodes[i];
		}
		return "en";
	}*/
	
	
	@Override
	public boolean shouldVisit(WebURL url) {
		if(file_saver.subclass_relations.keySet().contains(url) || file_saver.subclass_relations.values().contains(url)){
			return true;
		}
		String href = url.getURL().toLowerCase(Locale.ENGLISH);
		String mhref = href;// neutralizeMultilingual(href);
		if(crawlImages && FILTERSimage.matcher(mhref).matches()) {
			for(String s : requiredPrefixesImages) 
				if (s.length() > 0 && !mhref.startsWith(s)) return false;
			for(String s : blockedPrefixesImages) 
				if (s.length() > 0 && mhref.startsWith(s)) return false;
			for(String s : requiredStringInImageURL)
				if(s.length() > 0 && mhref.indexOf(s.toLowerCase(Locale.ENGLISH)) < 0) 
					return false;
			return true;
		} else {
			if(FILTERS.matcher(mhref).matches()) return false;
			boolean found = false;
			for(String s : requiredPrefixes){
				if (s.length() > 0 && mhref.startsWith(s)) found = true;
			}
			if(!found) return false;
			for(String s : blockedPrefixes) 
				if (s.length() > 0 && mhref.startsWith(s)) return false;
			return true;
		}
	}
	
	public boolean shouldStore(String href) {
		String mhref = href;//neutralizeMultilingual(href);
		if(FILTERStoStore.matcher(mhref).matches()) return false;
		boolean found = false;
		for(String s : requiredPrefixesToStore) {
			if (s.length() > 0 && mhref.startsWith(s)) found = true;
		}
		if(!found) return false;
		for(String s : blockedPrefixesToStore) 
			if (mhref.startsWith(s)) return false;
		//WikiHow only rules
		//if(wikihowLogics){
			//if(isMultilingual(href)){
				int wikihowDomainIndex = href.indexOf("wikihow");
				int columnIndex = href.indexOf(":",wikihowDomainIndex+4);
				int dashIndex = href.indexOf("-",columnIndex);
				if(columnIndex > 0 && columnIndex > wikihowDomainIndex && (dashIndex < 0 || dashIndex > columnIndex) ) return false;
			//}
		//}
		return true;
	}


	/**
	 * This function is called when a page is fetched and ready to be processed
	 * by your program.
	 */
	@Override
	public void visit(Page page) {	
		pagesCount++;
		if(pagesCount > 0 && pagesCount % logPrintInverseFrequency == 0){
			Logger.log("\nSTATUS LOG: Extracted "+htmlCount+" HTML pages and "+imageCount+" images. (Total visited: "+pagesCount+")");
			Logger.log(get_printable_language_statistics()+"\n");
			try {
				file_saver.save_hierarchy();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		String url = page.getWebURL().getURL();
		String lowercase_url = url.toLowerCase(Locale.forLanguageTag(extract_lang(url)));
		String href = lowercase_url;
		if (lowercase_url.indexOf("?") != -1)
			href = lowercase_url.substring(0, lowercase_url.indexOf("?"));
		// Only parse HTML pages
		if (! (page.getParseData() instanceof HtmlParseData && shouldStore(href))) return;
		htmlCount++;
		HtmlParseData htmlParseData = (HtmlParseData) page.getParseData();
		Document pageDocument = Parser.parse(htmlParseData.getHtml(), url);
		//String htmlPage = pageDocument.html();
		//FileController.createFolders(new String[] {"data",prop.getProperty("filename"),"web"});
		ArticleRepresentation article = new ArticleRepresentation(href, extract_lang(href));
		HTMLparserWikiHow.parse(article, pageDocument);
		if (article.methods.size() > 0){		
			//System.out.println(article.pretty_print());
		}
		if (article.title != null && article.methods.size() > 0 && article.methods.iterator().next().steps.size() >0){			
			try {
				file_saver.save_article(article, pageDocument, href);
				if(language_statistics.containsKey(article.language_code)) 
					language_statistics.put(article.language_code, new Integer(language_statistics.get(article.language_code) +1));
				else language_statistics.put(article.language_code, new Integer(1));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				Logger.log_error("ERRROR: failed to save this article: "+href+"\nERROR LOG:"+e.getMessage()+"\n"+article.pretty_print());
				boolean success = false;
				while(!success){
					try {
						TimeUnit.MINUTES.sleep(5);
						file_saver.save_article(article, pageDocument, href);
						if(language_statistics.containsKey(article.language_code)) 
							language_statistics.put(article.language_code, new Integer(language_statistics.get(article.language_code) +1));
						else language_statistics.put(article.language_code, new Integer(1));
					} catch (IOException | InterruptedException ex) {
						Logger.log_error("ERRROR: failed to save article, trying again in 5 minutes...");
					}
					Logger.log_error("RESOLVED: article has been saved");
				}
			}
		} else {
			Logger.log_error("WARNING: something went wrong while parsing this page: "+href+"\n"+article.pretty_print());
		}
	}
	
	public String extract_lang(String href){
		// extracts language codes when webpages are in this format:
		// http://pt.wikihow.com/Aliviar-as-Dores-da-Artrite
		// https://de.wikihow.com/Arthritis-Schmerzen-loswerden
		String language_code = "en";
		if (href.length() > 11){
			if(href.indexOf("http://") == 0 && href.indexOf(".") == 9) language_code = href.substring(7, 9);
			if(href.indexOf("https://") == 0 && href.indexOf(".") == 10) language_code = href.substring(8, 10);	
			if(href.indexOf("http://www.wikihow.jp/") == 0) language_code = "jp"; 
			if(href.indexOf("http://www.wikihow.vn/") == 0) language_code = "vn";
		}
		return language_code;
	}
	
	public String get_printable_language_statistics(){
		int tot = 0;
		String s = "";
		for(String key : language_statistics.keySet()){
			tot += language_statistics.get(key);
			s = s.concat("["+key+": "+language_statistics.get(key)+"] ");
		}
		return "[ALL: "+tot+"] "+s;
	}
	/*
	public void visit_old(Page page) {	
		pagesCount++;
		
		if(pagesCount > 0 && pagesCount % logPrintInverseFrequency == 0){
			Logger.log("STATUS LOG: Extracted "+htmlCount+" HTML pages and "+imageCount+" images. (Total visited: "+pagesCount+")");
			for(String s : language_statistics.keySet()){
				Logger.log("STATUS LOG: - For language ("+s+") extracted: "+language_statistics.get(s));
				
			}
		}				String url = page.getWebURL().getURL();
				String href = url.toLowerCase(Locale.ENGLISH);
				if(wikihowLogics){
				if (page.getParseData() instanceof HtmlParseData && href.startsWith("http://www.wikihow.com/image:")){
					HtmlParseData htmlParseData = (HtmlParseData) page.getParseData();
					Document pageDocument = Parser.parse(htmlParseData.getHtml(), url);
					String htmlPage = pageDocument.html();
					

					FileController.createFolders(new String[] {"data",prop.getProperty("filename"),"licences"});
					BufferedReader in = new BufferedReader(new StringReader(htmlPage));
					BufferedWriter writer;
					String codeName = "licence"+htmlCount+".htm";
					try {
						FileController.logHTML(prop.getProperty("filename"), href, codeName);
						writer = new BufferedWriter(new FileWriter("data\\"+prop.getProperty("filename")+"\\licences\\"+codeName));
						String inputLine;
				        while ((inputLine = in.readLine()) != null){
				            try{
				                writer.write(inputLine);
				            }
				            catch(IOException e){
				            	Logger.log("ERROR: problem in saving a licence.");
				                e.printStackTrace();
				                return;
				            }
				        }
				        in.close();
				        writer.close();
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				} } else
				if (crawlImages && page.getParseData() instanceof BinaryParseData) {
					if(!FILTERSimage.matcher(href).matches()) return;
					for(String s : requiredPrefixesImages) 
						if (s.length() > 0 && !href.startsWith(s)) return;
					for(String s : blockedPrefixesImages) 
						if (s.length() > 0 && href.startsWith(s)) return;
					for(String s : requiredStringInImageURL)
						if(s.length() > 0 && href.indexOf(s.toLowerCase(Locale.ENGLISH)) < 0) 
							return;
					// CRALW AN IMAGE
					
					FileController.createFolders(new String[] {"data",prop.getProperty("filename"),"images"});
					
					
					imageCount++;
					if (minImageSize < 0 || page.getContentData().length > minImageSize) {
						try {
						
					    String extension = href.substring(url.lastIndexOf("."));
					    String codeName = "image" + imageCount + extension;

					    FileController.logPicture(prop.getProperty("filename"), href, codeName);
					    // store image
					    IO.writeBytesToFile(page.getContentData(), "data\\" + prop.getProperty("filename") + "\\images\\" + codeName);

							//System.out.println("Stored image: " + url);
						} catch (IOException e) {
							Logger.log("ERROR: problem in saving a picture.");
							e.printStackTrace();
						}
					    
					    }
				} else if (page.getParseData() instanceof HtmlParseData && shouldStore(href)) {
					htmlCount++;
					// CRAWL HTML
					//System.out.print("\n + "+page.getWebURL().getURL());
					HtmlParseData htmlParseData = (HtmlParseData) page.getParseData();
					Document pageDocument = Parser.parse(htmlParseData.getHtml(), url);
					String htmlPage = pageDocument.html();
					

					FileController.createFolders(new String[] {"data",prop.getProperty("filename"),"web"});
					BufferedReader in = new BufferedReader(new StringReader(htmlPage));
					BufferedWriter writer;
					
					String codeName = "output"+htmlCount+".htm";
					String rdfName = "output"+htmlCount+".htm";
					
					String multilingualCode = getMultilingualCode(href);
												
					//addToMultilingualCount(multilingualCode);
					
					//int folder = multilingualCount.get(multilingualCode) % 10;
					
					if(multilingualCode.length() > 0) {
						FileController.createFolders(new String[] {"data",prop.getProperty("filename"),"web",multilingualCode,"s"+folder});
						codeName = multilingualCode+"\\s"+folder+"\\"+codeName;
						rdfName = multilingualCode+"/s"+folder+"/"+rdfName;
						}
					
					
					
					try {
						FileController.logHTML(prop.getProperty("filename"), href, rdfName,multilingualCode);
						writer = new BufferedWriter(new FileWriter("data\\"+prop.getProperty("filename")+"\\web\\"+codeName));
						String inputLine;
				        while ((inputLine = in.readLine()) != null){
				            try{
				                writer.write(inputLine);
				            }
				            catch(IOException e){
				            	Logger.log("ERROR: problem in saving a page.");
				                e.printStackTrace();
				                return;
				            }
				        }
				        in.close();
				        writer.close();
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}    
				}
	}
	*/

	
}