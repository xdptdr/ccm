package local.xd.ccm;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

import org.apache.commons.logging.impl.SimpleLog;
import org.apache.http.HttpResponse;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.fluent.Request;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.fasterxml.jackson.databind.ObjectMapper;

public class GetProfiles {

	private static final SimpleLog LOG = new SimpleLog(GetProfiles.class.getSimpleName());
	static {
		LOG.setLevel(SimpleLog.LOG_LEVEL_ALL);
	}

	private static final Random rnd = new Random(System.currentTimeMillis());

	private static final int MAX = 261;
	private static Map<String, Integer> users = new HashMap<>();

	public static void main(String[] args) {

		for (int pageNum = 1; pageNum <= MAX; ++pageNum) {
			iterate(pageNum);
			if (pageNum != MAX)
				try {
					final long sleepTime = sleepTime();
					LOG.info("Got page " + pageNum + "; Sleeping for " + sleepTime + " ms");
					Thread.sleep(sleepTime);
				} catch (InterruptedException e) {
					LOG.info(e);
					Thread.currentThread().interrupt();
				}
		}

		ObjectMapper om = new ObjectMapper();
		try {
			final File file = new File("users.json");
			FileOutputStream fos = new FileOutputStream(file);
			om.writeValue(fos, users);
			LOG.info(file.getAbsolutePath());
		} catch (Exception ex) {
			LOG.error(ex);
		}
	}

	private static int sleepTime() {
		return (rnd.nextInt(10) + 25) * 1000;
	}

	public static void iterate(int pageNum) {
		try {

			LOG.info("Getting page " + pageNum);

			String url = "http://www.commentcamarche.net/forum/java-265?page=" + pageNum;

			Document document = Request.Get(url).execute().handleResponse(new ResponseHandler<Document>() {
				public Document handleResponse(HttpResponse response) throws IOException {
					try {
						return Jsoup.parse(response.getEntity().getContent(), "UTF-8",
								"http://www.commentcamarche.net/forum/");
					} catch (Exception ex) {
						throw new CCMException(ex);
					}
				}

			});
			Elements links = document.select("a");
			Iterator<Element> it = links.iterator();
			while (it.hasNext()) {
				Element e = it.next();
				final String href = e.attr("href");
				if (href.startsWith("/profile/user")) {
					addUser(href);
				}
			}
		} catch (Exception ex) {
			LOG.error(ex);
		}
	}

	private static void addUser(String href) {
		if (!users.containsKey(href)) {
			LOG.info("New profile " + href);
			users.put(href, 0);
		} else {
			LOG.info(".");
		}

		users.put(href, users.get(href) + 1);
	}
}
