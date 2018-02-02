package local.xd.ccm;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.function.Consumer;

import org.apache.commons.logging.impl.SimpleLog;
import org.apache.http.HttpResponse;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.fluent.Request;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class GetProfileDetails {

	private static final SimpleLog LOG = new SimpleLog(GetProfileDetails.class.getSimpleName());
	static {
		LOG.setLevel(SimpleLog.LOG_LEVEL_ALL);
	}

	private static final Random rnd = new Random(System.currentTimeMillis());

	public static class ProfileDetail {
		private String name;
		private String lastSeen;
		private Boolean canWriteMessages;
		private Integer questions;
		private Integer responses;
		private List<String> lastQuestions = new ArrayList<>();

		public ProfileDetail(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getLastSeen() {
			return lastSeen;
		}

		public void setLastSeen(String lastSeen) {
			this.lastSeen = lastSeen;
		}

		public Boolean getCanWriteMessages() {
			return canWriteMessages;
		}

		public void setCanWriteMessages(Boolean canWriteMessages) {
			this.canWriteMessages = canWriteMessages;
		}

		public Integer getQuestions() {
			return questions;
		}

		public void setQuestions(Integer questions) {
			this.questions = questions;
		}

		public Integer getResponses() {
			return responses;
		}

		public void setResponses(Integer responses) {
			this.responses = responses;
		}

		public List<String> getLastQuestions() {
			return lastQuestions;
		}

		public void setLastQuestions(List<String> lastQuestions) {
			this.lastQuestions = lastQuestions;
		}

	}

	static Map<String, ProfileDetail> alldetails = new HashMap<>();

	public static void main(String[] args) {
		try {
			ObjectMapper om = new ObjectMapper();
			List<Entry<String, Integer>> profiles = loadProfileList(om);

			for (int i = 0; i < profiles.size(); ++i) {
				final String profileLink = profiles.get(i).getKey();
				LOG.info(i + "/" + profiles.size() + " : " + profileLink);
				getDetails(profileLink, om);
				sleep();
			}

			File detailsFile = new File("details.json");
			om.writeValue(detailsFile, alldetails);

		} catch (Exception ex) {
			throw new CCMException(ex);
		}

		LOG.info("Done !");
	}

	private static void sleep() {
		try {
			final long sleepTime = sleepTime();
			LOG.info("Sleeping for " + (sleepTime / 1000) + " s");
			Thread.sleep(sleepTime);
		} catch (InterruptedException e) {
			LOG.info(e);
			Thread.currentThread().interrupt();
		}
	}

	public static List<Entry<String, Integer>> loadProfileList(ObjectMapper om) throws IOException {
		File file = new File("users.json");
		Map<String, Integer> profiles = om.readValue(file, new TypeReference<HashMap<String, Integer>>() {
		});
		List<Map.Entry<String, Integer>> entries = new ArrayList<>();
		entries.addAll(profiles.entrySet());
		Collections.sort(entries, new Comparator<Map.Entry<String, Integer>>() {
			@Override
			public int compare(Entry<String, Integer> a, Entry<String, Integer> b) {
				int valCompare = -Integer.compare(a.getValue(), b.getValue());
				if (valCompare == 0) {
					return a.getKey().compareTo(b.getKey());
				}
				return valCompare;
			}
		});
		return entries;
	}

	private static void getDetails(String profileLink, ObjectMapper om) {
		try {
			String url = "http://www.commentcamarche.net" + profileLink;
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

			Elements canWriteMessages = document.select("#jColMiddleActions a.button.btBlue.btSmall.overlayW");
			Elements dates = document.select(".infosTable th, .infosTable td");
			Elements stats = document.select(".floatLeft .statsTable th, .floatLeft .statsTable td");
			Elements lastQuestions = document.select("#profileMiddle td.subject a");

			String[] profileParts = profileLink.split("/");
			final ProfileDetail details = new ProfileDetail(profileParts[profileParts.length - 1]);
			details.setCanWriteMessages(!canWriteMessages.isEmpty());
			for (int i = 0; i < dates.size(); i += 2) {
				if ("Derni\u00e8re intervention".equals(dates.get(i).html())) {
					details.setLastSeen(dates.get(i + 1).html());
					break;
				}
			}

			for (int i = 1; i <= stats.size(); i += 2) {
				Element t = stats.get(i);
				String text = t.html();
				if (!t.childNodes().isEmpty() && "a".equals(t.childNodes().get(0).nodeName())) {
					text = t.child(0).html();
				}
				if ("Questions".equals(text)) {
					details.setQuestions(Integer.valueOf(stats.get(i - 1).html()));
				} else if ("R\u00e9ponses".equals(text)) {
					details.setResponses(Integer.valueOf(stats.get(i - 1).html()));
				}
			}

			lastQuestions.forEach(new Consumer<Element>() {

				@Override
				public void accept(Element t) {
					details.getLastQuestions().add(t.html());
				}

			});

			alldetails.put(details.getName(), details);

			LOG.info(om.writeValueAsString(details));
		} catch (Exception ex) {
			LOG.error(ex);
		}

	}

	private static int sleepTime() {
		return (rnd.nextInt(5) + 10) * 1000;
	}
}
