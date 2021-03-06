package main;

import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;

import edu.uci.ics.crawler4j.crawler.Configurable;
import edu.uci.ics.crawler4j.crawler.CrawlConfig;
import edu.uci.ics.crawler4j.crawler.CrawlController;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.fetcher.PageFetcher;
import edu.uci.ics.crawler4j.frontier.DocIDServer;
import edu.uci.ics.crawler4j.frontier.Frontier;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtServer;
import edu.uci.ics.crawler4j.url.URLCanonicalizer;
import edu.uci.ics.crawler4j.url.WebURL;
import edu.uci.ics.crawler4j.util.IO;

import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

/**
 * The controller that manages a crawling session. This class creates the
 * crawler threads and monitors their progress.
 */
public class MyCrawlerController extends CrawlController {

	private static final Logger logger = Logger.getLogger(MyCrawlerController.class.getName());

	private static final List<String> random_pages =  new LinkedList<String>();
	/**
	 * The 'customData' object can be used for passing custom crawl-related
	 * configurations to different components of the crawler.
	 */
	protected Object customData;

	/**
	 * Once the crawling session finishes the controller collects the local data
	 * of the crawler threads and stores them in this List.
	 */
	protected List<Object> crawlersLocalData = new ArrayList<Object>();

	/**
	 * Is the crawling of this session finished?
	 */
	protected boolean finished;

	/**
	 * Is the crawling session set to 'shutdown'. Crawler threads monitor this
	 * flag and when it is set they will no longer process new pages.
	 */
	protected boolean shuttingDown;

	protected PageFetcher pageFetcher;
	protected RobotstxtServer robotstxtServer;
	protected Frontier frontier;
	protected DocIDServer docIdServer;
	
	protected CrawlConfig config;
	protected Properties prop;

	protected final Object waitingLock = new Object();

	public MyCrawlerController(CrawlConfig config, PageFetcher pageFetcher, RobotstxtServer robotstxtServer, Properties prop)
			throws Exception {
		super(config, pageFetcher, robotstxtServer);
		this.config = config;
		this.prop = prop;
		config.validate();
		File folder = new File(config.getCrawlStorageFolder());
		if (!folder.exists()) {
			if (!folder.mkdirs()) {
				throw new Exception("Couldn't create this folder: " + folder.getAbsolutePath());
			}
		}
		random_pages.add("http://www.wikihow.com/Special:Randomizer");
		random_pages.add("http://es.wikihow.com/Especial:Randomizer");
		random_pages.add("http://cs.wikihow.com/Speci%C3%A1ln%C3%AD:Randomizer");
		random_pages.add("http://de.wikihow.com/Spezial:Randomizer");
		random_pages.add("http://fr.wikihow.com/Sp%C3%A9cial:Randomizer");
		random_pages.add("http://hi.wikihow.com/%E0%A4%B5%E0%A4%BF%E0%A4%B6%E0%A5%87%E0%A4%B7:Randomizer");
		random_pages.add("http://id.wikihow.com/Istimewa:Randomizer");
		random_pages.add("http://it.wikihow.com/Speciale:Randomizer");
		random_pages.add("http://www.wikihow.jp/%E7%89%B9%E5%88%A5:Randomizer");
		random_pages.add("http://nl.wikihow.com/Speciaal:Randomizer");
		random_pages.add("http://pt.wikihow.com/Especial:Randomizer");
		random_pages.add("http://ru.wikihow.com/%D0%A1%D0%BB%D1%83%D0%B6%D0%B5%D0%B1%D0%BD%D0%B0%D1%8F:Randomizer");
		random_pages.add("http://ar.wikihow.com/%D8%AE%D8%A7%D8%B5:Randomizer");
		random_pages.add("http://th.wikihow.com/%E0%B8%9E%E0%B8%B4%E0%B9%80%E0%B8%A8%E0%B8%A9:Randomizer");
		random_pages.add("http://www.wikihow.vn/%C4%90%E1%BA%B7c_bi%E1%BB%87t:Randomizer");
		random_pages.add("http://ko.wikihow.com/%ED%8A%B9%EC%88%98:Randomizer");
		random_pages.add("http://zh.wikihow.com/Special:Randomizer");
		boolean resumable = config.isResumableCrawling();

		EnvironmentConfig envConfig = new EnvironmentConfig();
		envConfig.setAllowCreate(true);
		envConfig.setTransactional(resumable);
		envConfig.setLocking(resumable);

		File envHome = new File(config.getCrawlStorageFolder() + "/frontier");
		if (!envHome.exists()) {
			if (!envHome.mkdir()) {
				throw new Exception("Couldn't create this folder: " + envHome.getAbsolutePath());
			}
		}
		if (!resumable) {
			IO.deleteFolderContents(envHome);
		}

		Environment env = new Environment(envHome, envConfig);
		docIdServer = new DocIDServer(env, config);
		frontier = new Frontier(env, config, docIdServer);

		this.pageFetcher = pageFetcher;
		this.robotstxtServer = robotstxtServer;

		finished = false;
		shuttingDown = false;
	}

	/**
	 * Start the crawling session and wait for it to finish.
	 * 
	 * @param _c
	 *            the class that implements the logic for crawler threads
	 * @param numberOfCrawlers
	 *            the number of concurrent threads that will be contributing in
	 *            this crawling session.
	 */
	public <T extends WebCrawler> void start(final Class<T> _c, final int numberOfCrawlers) {
		this.start(_c, numberOfCrawlers, true);
	}

	/**
	 * Start the crawling session and return immediately.
	 * 
	 * @param _c
	 *            the class that implements the logic for crawler threads
	 * @param numberOfCrawlers
	 *            the number of concurrent threads that will be contributing in
	 *            this crawling session.
	 */
	public <T extends WebCrawler> void startNonBlocking(final Class<T> _c, final int numberOfCrawlers) {
		this.start(_c, numberOfCrawlers, false);
	}

	protected <T extends WebCrawler> void start(final Class<T> _c, final int numberOfCrawlers, boolean isBlocking) {
		try {
			finished = false;
			crawlersLocalData.clear();
			final List<Thread> threads = new ArrayList<Thread>();
			final List<T> crawlers = new ArrayList<T>();

			for (int i = 1; i <= numberOfCrawlers; i++) {
				Constructor constructor = KnowHowCrawlerImplementation.class.getConstructor(new Class[] {Properties.class});
				//c.getClass().getConstructor(new Class[] {Properties.class}).newInstance(new Object[] {prop});
				T crawler = (T) constructor.newInstance(new Object[] {prop});//_c.newInstance();
				Thread thread = new Thread(crawler, "Crawler " + i);
				crawler.setThread(thread);
				crawler.init(i, this);
				thread.start();
				crawlers.add(crawler);
				threads.add(thread);
				logger.info("Crawler " + i + " started.");
			}

			final CrawlController controller = this;

			Thread monitorThread = new Thread(new Runnable() {

				@Override
				public void run() {
					try {
						synchronized (waitingLock) {

							while (true) {
								long queueLength = frontier.getQueueLength();
								queueLength = frontier.getQueueLength();
								while (queueLength < 1) {
									System.out.println(">> "+queueLength);
									retrieve_random_pages();
									queueLength = frontier.getQueueLength();
									System.out.println(">> "+queueLength);
								}
								
								sleep(10);
								boolean someoneIsWorking = false;
								for (int i = 0; i < threads.size(); i++) {
									Thread thread = threads.get(i);
									if (!thread.isAlive()) {
										if (!shuttingDown) {
											logger.info("Thread " + i + " was dead, I'll recreate it.");
											T crawler = _c.newInstance();
											thread = new Thread(crawler, "Crawler " + (i + 1));
											threads.remove(i);
											threads.add(i, thread);
											crawler.setThread(thread);
											crawler.init(i + 1, controller);
											thread.start();
											crawlers.remove(i);
											crawlers.add(i, crawler);
										}
									} else if (crawlers.get(i).isNotWaitingForNewURLs()) {
										someoneIsWorking = true;
									}
								}
								if (!someoneIsWorking) {
									retrieve_random_pages();
									// Make sure again that none of the threads
									// are
									// alive.
									logger.info("It looks like no thread is working, waiting for 10 seconds to make sure...");
									sleep(10);

									someoneIsWorking = false;
									for (int i = 0; i < threads.size(); i++) {
										Thread thread = threads.get(i);
										if (thread.isAlive() && crawlers.get(i).isNotWaitingForNewURLs()) {
											someoneIsWorking = true;
										}
									}
									if (!someoneIsWorking) {
										
										retrieve_random_pages();
										continue;
										/*if (!shuttingDown) {
											long queueLength = frontier.getQueueLength();
											queueLength = frontier.getQueueLength();
											if (queueLength > 0) {
												continue;
											} else {
												retrieve_random_pages();
												continue;
											}
											logger.info("No thread is working and no more URLs are in queue waiting for another 10 seconds to make sure...");
											sleep(10);
											queueLength = frontier.getQueueLength();
											if (queueLength > 0) {
												continue;
											}
										}

										logger.info("All of the crawlers are stopped. Finishing the process...");
										// At this step, frontier notifies the
										// threads that were
										// waiting for new URLs and they should
										// stop
										frontier.finish();
										for (T crawler : crawlers) {
											crawler.onBeforeExit();
											crawlersLocalData.add(crawler.getMyLocalData());
										}

										logger.info("Waiting for 10 seconds before final clean up...");
										sleep(10);

										frontier.close();
										docIdServer.close();
										pageFetcher.shutDown();

										finished = true;
										waitingLock.notifyAll();

										return;*/
									}
								}
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});

			monitorThread.start();

			if (isBlocking) {
				waitUntilFinish();
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void retrieve_random_pages() {
		System.out.println("Retrieving random pages...");
		for (String r : random_pages){
			try{
				HttpURLConnection con = (HttpURLConnection)(new URL( r ).openConnection());
				con.setInstanceFollowRedirects( false );
				con.connect();
				int responseCode = con.getResponseCode();
				String location = con.getHeaderField( "Location" );
				//System.out.println( r+" ---> "+location );
				addSeed(location);
				Thread.sleep(100);
			}
			catch (InterruptedException | IOException e)  {
				
			}
		}
	}
	/**
	 * Wait until this crawling session finishes.
	 */
	public void waitUntilFinish() {
		while (!finished) {
			synchronized (waitingLock) {
				if (finished) {
					return;
				}
				try {
					waitingLock.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Once the crawling session finishes the controller collects the local data
	 * of the crawler threads and stores them in a List. This function returns
	 * the reference to this list.
	 */
	public List<Object> getCrawlersLocalData() {
		return crawlersLocalData;
	}



	/**
	 * Adds a new seed URL. A seed URL is a URL that is fetched by the crawler
	 * to extract new URLs in it and follow them for crawling.
	 * 
	 * @param pageUrl
	 *            the URL of the seed
	 */
	public void addSeed(String pageUrl) {
		addSeed(pageUrl, -1);
	}

	/**
	 * Adds a new seed URL. A seed URL is a URL that is fetched by the crawler
	 * to extract new URLs in it and follow them for crawling. You can also
	 * specify a specific document id to be assigned to this seed URL. This
	 * document id needs to be unique. Also, note that if you add three seeds
	 * with document ids 1,2, and 7. Then the next URL that is found during the
	 * crawl will get a doc id of 8. Also you need to ensure to add seeds in
	 * increasing order of document ids.
	 * 
	 * Specifying doc ids is mainly useful when you have had a previous crawl
	 * and have stored the results and want to start a new crawl with seeds
	 * which get the same document ids as the previous crawl.
	 * 
	 * @param pageUrl
	 *            the URL of the seed
	 * @param docId
	 *            the document id that you want to be assigned to this seed URL.
	 * 
	 */
	public void addSeed(String pageUrl, int docId) {
		String canonicalUrl = URLCanonicalizer.getCanonicalURL(pageUrl);
		if (canonicalUrl == null) {
			logger.error("Invalid seed URL: " + pageUrl);
			return;
		}
		if (docId < 0) {
			docId = docIdServer.getDocId(canonicalUrl);
			if (docId > 0) {
				// This URL is already seen.
				return;
			}
			docId = docIdServer.getNewDocID(canonicalUrl);
		} else {
			try {
				docIdServer.addUrlAndDocId(canonicalUrl, docId);
			} catch (Exception e) {
				logger.error("Could not add seed: " + e.getMessage());
			}
		}

		WebURL webUrl = new WebURL();
		webUrl.setURL(canonicalUrl);
		webUrl.setDocid(docId);
		webUrl.setDepth((short) 0);
		//if (!robotstxtServer.allows(webUrl)) {
			//logger.info("Robots.txt does not allow this seed: " + pageUrl);
		//} else {
			frontier.schedule(webUrl);
		//}
	}

	/**
	 * This function can called to assign a specific document id to a url. This
	 * feature is useful when you have had a previous crawl and have stored the
	 * Urls and their associated document ids and want to have a new crawl which
	 * is aware of the previously seen Urls and won't re-crawl them.
	 * 
	 * Note that if you add three seen Urls with document ids 1,2, and 7. Then
	 * the next URL that is found during the crawl will get a doc id of 8. Also
	 * you need to ensure to add seen Urls in increasing order of document ids. 
	 * 
	 * @param pageUrl
	 *            the URL of the page
	 * @param docId
	 *            the document id that you want to be assigned to this URL.
	 * 
	 */
	public void addSeenUrl(String url, int docId) {
		String canonicalUrl = URLCanonicalizer.getCanonicalURL(url);
		if (canonicalUrl == null) {
			logger.error("Invalid Url: " + url);
			return;
		}
		try {
			docIdServer.addUrlAndDocId(canonicalUrl, docId);
		} catch (Exception e) {
			logger.error("Could not add seen url: " + e.getMessage());
		}
	}

	public PageFetcher getPageFetcher() {
		return pageFetcher;
	}

	public void setPageFetcher(PageFetcher pageFetcher) {
		this.pageFetcher = pageFetcher;
	}

	public RobotstxtServer getRobotstxtServer() {
		return robotstxtServer;
	}

	public void setRobotstxtServer(RobotstxtServer robotstxtServer) {
		this.robotstxtServer = robotstxtServer;
	}

	public Frontier getFrontier() {
		return frontier;
	}

	public void setFrontier(Frontier frontier) {
		this.frontier = frontier;
	}

	public DocIDServer getDocIdServer() {
		return docIdServer;
	}

	public void setDocIdServer(DocIDServer docIdServer) {
		this.docIdServer = docIdServer;
	}

	public Object getCustomData() {
		return customData;
	}

	public void setCustomData(Object customData) {
		this.customData = customData;
	}

	public boolean isFinished() {
		return this.finished;
	}

	public boolean isShuttingDown() {
		return shuttingDown;
	}

	/**
	 * Set the current crawling session set to 'shutdown'. Crawler threads
	 * monitor the shutdown flag and when it is set to true, they will no longer
	 * process new pages.
	 */
	public void Shutdown() {
		logger.info("Shutting down...");
		this.shuttingDown = true;
		frontier.finish();
	}
}