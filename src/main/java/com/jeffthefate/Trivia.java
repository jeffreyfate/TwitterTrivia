package com.jeffthefate;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import twitter4j.DirectMessage;
import twitter4j.StallWarning;
import twitter4j.Status;
import twitter4j.StatusDeletionNotice;
import twitter4j.StatusUpdate;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;
import twitter4j.User;
import twitter4j.UserList;
import twitter4j.UserStreamListener;
import twitter4j.conf.Configuration;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;

/**
 * Creates a game of trivia played on the account in the given configuration.
 * 
 * @author Jeff Fate
 *
 */
public class Trivia implements UserStreamListener {
	
	private static final int WAIT_FOR_QUESTION = (5 * 60 * 1000);
	private static final int WAIT_BETWEEN_QUESTIONS = 90000;
	private static final int WAIT_FOR_ANSWER = (60 * 1000);
	private static final int WAIT_FOR_RESPONSE = (60 * 1000);
	private static final int WAIT_FOR_TIP = (3 * 60 * 1000);
	
	private static final int PRE_ROUND_TIME = (15 * 1000);
	private static final int LEADERS_EVERY = 10;
	private static final int ROUND_TWO = 15;
	private static final int BONUS_ROUND = 30;
	private static final int PLUS_SCORE = 500;
	private static final int BONUS_SCORE = 1000;

	private static int diffCount;
	private ArrayList<ArrayList<String>> nameMap;
    private HashMap<String, String> acronymMap;
    private ArrayList<String> replaceList;
    private static List<String> winners = new ArrayList<String>(0);
    private static Map<String, Integer> scoreMap =
    		new HashMap<String, Integer>();
    private static int currScore = 0;
    private static String currAnswer;
    private static int questionWait = WAIT_FOR_QUESTION;
    private static TwitterStream twitterStream = null;
    private static List<Long> currTwitterStatus = new ArrayList<Long>(0);
    
    private String templateFile;
    private String fontFile;
    private String leadersTitle;
    private int mainSize;
    private int dateSize;
    private int limit;
    private StringBuilder sb;
    
    private static Screenshot screenshot;
    
    private Configuration twitterConfig;
    
    private int totalQuestions = 0;
    private int reqQuestions;
    
    private boolean isDev;
    
    private Message message;
    
    private HashMap<String, Long> responseMap = new HashMap<String, Long>();
    private ArrayList<String> tipList;
    
    private String preTweet;
	
	public Trivia(String templateFile, String fontFile, String leadersTitle,
			int mainSize, int dateSize, int limit, Configuration twitterConfig,
			int reqQuestions, ArrayList<ArrayList<String>> nameMap,
			HashMap<String, String> acronymMap, ArrayList<String> replaceList,
			ArrayList<String> tipList, boolean isDev, String preTweet) {
		this.templateFile = templateFile;
		this.fontFile = fontFile;
		this.leadersTitle = leadersTitle;
		this.mainSize = mainSize;
		this.dateSize = dateSize;
		this.limit = limit;
		this.twitterConfig = twitterConfig;
		this.reqQuestions = reqQuestions;
		this.nameMap = nameMap;
		this.acronymMap = acronymMap;
		this.replaceList = replaceList;
		this.tipList = tipList;
		this.isDev = isDev;
		this.preTweet = preTweet;
		message = new Message();
	}
	
	private class Message {
		public Message() {
		}
	}
	
	private Timer timer = new Timer();
	
	private void setTimer(long newWait, boolean killStream) {
		timer.cancel();
		timer = new Timer();
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				finishTrivia();
				synchronized (message) {
					message.notify();
				}
			}
		}, newWait);
		if (killStream) {
			twitterStream.cleanUp();
		    twitterStream.shutdown();
		}
	}
	
	private void finishTrivia() {
		twitterStream.cleanUp();
	    twitterStream.shutdown();
	    onCorrectUser(currAnswer, winners);
		sendUpdateTweets();
	}
	
	public void startTrivia(boolean preShowTweet,
			String preShowText, int preShowTime) {
		if (preShowTweet) {
			postTweet(preShowText, null, -1);
			if (tipList.isEmpty()) {
				try {
	    			Thread.sleep(preShowTime);
	    		} catch (InterruptedException e1) {}
			}
			else {
				int preShowTips = (int) (preShowTime / WAIT_FOR_TIP) - 1;
				Collections.shuffle(tipList);
				for (int i = 0; i < preShowTips; i++) {
					try {
		    			Thread.sleep(WAIT_FOR_TIP);
		    		} catch (InterruptedException e1) {}
					postTweet(preTweet + tipList.get(i), null, -1);
				}
			}
		}
		for (int i = 0; i < reqQuestions; i++) {
			synchronized (message) {
				boolean success = false;
				do {
					success = askQuestion();
					// TODO Mark them 0 and start over again
				} while (!success);
				try {
					message.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		onFinalLeaders();
	    for (Entry<String, Integer> user : usersMap.entrySet()) {
	    	System.out.println(user.getKey() + " : " + user.getValue());
	    }
	}
	
	private void sendUpdateTweets() {
		System.out.println("checking if asking a new question");
	    if (totalQuestions < reqQuestions) {
	    	if (totalQuestions % LEADERS_EVERY == 0) {
	    		screenshot = new TriviaScreenshot(templateFile, fontFile,
	    				leadersTitle, generateLeaderboard(), mainSize,
	    				dateSize, limit);
	    		postTweet(preTweet + "Current Top 10",
	    				new File(screenshot.getOutputFilename()), -1);
	    	}
	    	if (totalQuestions == BONUS_ROUND) {
	    		postTweet("[BONUS ROUND] " + BONUS_SCORE +
	    				" pts added to the value of each question",
	    				null, -1);
	    		try {
					Thread.sleep(PRE_ROUND_TIME);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
	    	}
	    	else if (totalQuestions == ROUND_TWO) {
	    		postTweet("[Starting ROUND 2] " + PLUS_SCORE +
	    				" pts added to the value of each question",
	    				null, -1);
	    		try {
					Thread.sleep(PRE_ROUND_TIME);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
	    	}
	    }
	}
	
	private void processTweet(Status status) {
		System.out.println("CURR TIME: " + new Date(System.currentTimeMillis()).toString());
		System.out.println("TWEET TIME: " + status.getCreatedAt().toString());
		System.out.println("RAW RESPONSE: " + status.getText());
		String userName = status.getUser().getScreenName();
		String inReplyTo = status.getInReplyToScreenName();
		long convoId = status.getInReplyToStatusId();
		System.out.println("user: " + userName);
		System.out.println("inReplyTo: " + inReplyTo);
		// Check to see if the user can submit an answer
		Long userTime = responseMap.get(userName);
		// Keep track of people who respond directly to the question
		if (currTwitterStatus.contains(convoId)) {
			if (usersMap.containsKey(userName))
    			usersMap.put(userName, usersMap.get(userName)+1);
    		else
    			usersMap.put(userName, 1);
		}
		if (userTime != null && currTwitterStatus.contains(convoId) &&
				System.currentTimeMillis()-userTime < WAIT_FOR_RESPONSE) {
			System.out.println("return");
			return;
		}
		else {
			System.out.println("Adding " + userName + " : " + System.currentTimeMillis());
			responseMap.put(userName, System.currentTimeMillis());
		}
		// Massage response and answer
		String response = massageResponse(status.getText());
		System.out.println("MASSAGED RESPONSE: " + response);
		String answer = massageAnswer(currAnswer);
		System.out.println("MASSAGED ANSWER: " + answer);
		System.out.println("questionWait: " + questionWait);
		System.out.println("WAIT_FOR_ANSWER: " + WAIT_FOR_ANSWER);
		// Get the diff characters between the answer and the response
		boolean isCorrect = checkAnswer(answer, response, userName);
		if (!isCorrect)
			isCorrect = checkAnswer(answer, reCheck(answer, response),
					userName);
		if (isCorrect && questionWait > WAIT_FOR_ANSWER) {
			questionWait = WAIT_FOR_ANSWER;
			setTimer(questionWait, false);
		}
		System.out.println("questionWait: " + questionWait);
		if (winners.size() == 3)
			setTimer(WAIT_FOR_ANSWER, true);
	}
	
	private String massageResponse(String text) {
		return text.toLowerCase(Locale.getDefault()).replaceFirst(
						"(?<=^|(?<=[^a-zA-Z0-9-_\\.]))@([A-Za-z]+[A-Za-z0-9]+)",
						"").
				replaceAll("[.,'`\":;/?\\-!@#]", "").trim();
	}
	
	private String massageAnswer(String text) {
		return text.toLowerCase(Locale.getDefault()).
				replaceAll("[.,'`\":;/?\\-!@#]", "").trim();
	}
	
	public void onCorrectUser(String correctAnswer, List<String> screenNames) {
		System.out.println("onCorrectUser: " + correctAnswer);
		sb = new StringBuilder();
		sb.append(preTweet);
		sb.append("Answer: \"");
		sb.append(correctAnswer);
		sb.append("\"");
		String screenName;
		for (int i = 0; i < screenNames.size(); i++) {
			screenName = screenNames.get(i);
			if (sb.length() + 11 + screenName.length() +
					Integer.toString(scoreMap.get(screenName))
							.length() > 140)
				break;
			else
				sb.append("\n");
			sb.append("#");
			sb.append(i+1);
			sb.append(" @");
			sb.append(screenName);
			sb.append(" [");
			sb.append(scoreMap.get(screenName));
			sb.append("pts]");
		}
		postTweet(sb.toString(), null,
				currTwitterStatus != null && !currTwitterStatus.isEmpty() ?
						currTwitterStatus.get(0) : -1);
		try {
			System.out.println("sleeping");
			Thread.sleep(WAIT_BETWEEN_QUESTIONS);
			System.out.println("DONE sleeping");
		} catch (InterruptedException e) {}
	}
	
	private void onFinalLeaders() {
		Map<String, Integer> sortedMap = generateLeaderboard();
		String winner = "Final Top 10";
		int firstScore = -1;
		String firstPlayer = null;
		int playerNum = 0;
		for (Entry<String, Integer> player : sortedMap.entrySet()) {
			playerNum++;
			if (playerNum <= 1) {
				firstScore = player.getValue();
				firstPlayer = player.getKey();
			}
			else {
				if (player.getValue() == firstScore) {
					winner = "@" + firstPlayer + " and @" + player.getKey() +
							" tied with " + firstScore + "pts!";
					break;
				}
			}
			winner = "@" + firstPlayer + " with " + firstScore +
					"pts is the winner!";
			if (playerNum > 1)
				break;
		}
		screenshot = new TriviaScreenshot(templateFile, fontFile,
				leadersTitle, generateLeaderboard(), mainSize, dateSize,
				limit);
		postTweet(preTweet + winner,
				new File(screenshot.getOutputFilename()), -1);
	}
	
	private void watchTwitterStream(String answer, String questionId) {
    	System.out.println("watchTwitterStream");
    	winners.clear();
    	winners = new ArrayList<String>(0);
    	System.out.println("ANSWER: " + answer);
    	currAnswer = answer;
    	questionWait = WAIT_FOR_QUESTION;
	    twitterStream = new TwitterStreamFactory(twitterConfig)
	    		.getInstance();
	    twitterStream.addListener(this);
	    // sample() method internally creates a thread which manipulates TwitterStream and calls these adequate listener methods continuously.
	    twitterStream.user();
	    setTimer(questionWait, false);
    }
	
    private static Map<String, Integer> generateLeaderboard() {
    	ValueComparator scoreComparator = new ValueComparator(scoreMap);
        Map<String, Integer> sortedMap =
        		new TreeMap<String, Integer>(scoreComparator);
        sortedMap.putAll(scoreMap);
    	return sortedMap;
    }
    
    private static class ValueComparator implements Comparator<String> {

        Map<String, Integer> base;
        
        public ValueComparator(Map<String, Integer> base) {
            this.base = base;
        }

        // Note: this comparator imposes orderings that are inconsistent with equals.    
        public int compare(String a, String b) {
            if (base.get(a) >= base.get(b))
                return -1;
            else
                return 1;
            // returning 0 would merge keys
        }
    }
    
    public Status postTweet(String message, File file, long replyTo) {
    	System.out.println("Tweet text: " + message);
    	System.out.println("Tweet length: " + message.length());
        Twitter twitter = new TwitterFactory(twitterConfig).getInstance();
		StatusUpdate statusUpdate = new StatusUpdate(message);
		System.out.println("Status text: " + statusUpdate.getStatus());
		System.out.println("Status length: " + statusUpdate.getStatus().length());
		Status status = null;
		if (file != null)
			statusUpdate.media(file);
		statusUpdate.setInReplyToStatusId(replyTo);
    	try {
			status = twitter.updateStatus(statusUpdate);
    	} catch (TwitterException te) {
    		te.printStackTrace();
    		System.out.println("Failed to get timeline: " +
    				te.getMessage());
    	}
		return status;
    }
    
	public void onBlock(User arg0, User arg1) {}
	public void onDeletionNotice(long arg0, long arg1) {}
	public void onDirectMessage(DirectMessage arg0) {}
	public void onFavorite(User arg0, User arg1, Status arg2) {}
	public void onFollow(User arg0, User arg1) {}
	public void onFriendList(long[] arg0) {}
	public void onUnblock(User arg0, User arg1) {}
	public void onUnfavorite(User arg0, User arg1, Status arg2) {}
	public void onUserListCreation(User arg0, UserList arg1) {}
	public void onUserListDeletion(User arg0, UserList arg1) {}
	public void onUserListMemberAddition(User arg0, User arg1, UserList arg2) {}
	public void onUserListMemberDeletion(User arg0, User arg1, UserList arg2) {}
	public void onUserListSubscription(User arg0, User arg1, UserList arg2) {}
	public void onUserListUnsubscription(User arg0, User arg1, UserList arg2) {}
	public void onUserListUpdate(User arg0, UserList arg1) {}
	public void onUserProfileUpdate(User arg0) {}
	public void onDeletionNotice(StatusDeletionNotice arg0) {}
	public void onScrubGeo(long arg0, long arg1) {}
	public void onStallWarning(StallWarning arg0) {}
	public void onStatus(Status status) {
		processTweet(status);
	}
	public void onTrackLimitationNotice(int arg0) {}
	public void onException(Exception e) {e.printStackTrace();}
	
	private boolean checkAnswer(String answer, String response,
    		String screenName) {
		System.out.println("checkAnswer");
		if (answer == null || response == null)
			return false;
		for (String replace : replaceList) {
			answer = answer.replaceAll(replace, "");
			response = response.replaceAll(replace, "");
		}
    	int buffer = 4;
    	switch (answer.length()) {
    	case 15:
    	case 14:
    	case 13:
    	case 12:
    	case 11:
    	case 10:
    		buffer = 3;
    		break;
    	case 9:
    	case 8:
    	case 7:
    	case 6:
    	case 5:
    		buffer = 2;
    		break;
    	case 4:
    	case 3:
    		buffer = 1;
    		break;
    	case 2:
    	case 1:
    		buffer = 0;
    		break;
    	}
    	if (answer.matches("^[0-9]+$"))
    		buffer = 0;
    	diffCount = StringUtils.getLevenshteinDistance(response, answer,
    			buffer);
    	boolean isCorrect = false;
    	
    	switch(diffCount) {
    	case -1:
    		break;
    	default:
    		isCorrect = true;
    		if (!winners.contains(screenName) && winners.size() < 3) {
	    		Integer userScore = scoreMap.get(screenName);
	    		int pointsEarned = currScore - (winners.size() * (currScore/4));
	    		winners.add(screenName);
	    		if (userScore == null)
	    			scoreMap.put(screenName, pointsEarned);
	    		else
	    			scoreMap.put(screenName, userScore+pointsEarned);
    		}
    		break;
    	}
    	System.out.println("returning");
    	return isCorrect;
    }
	
	private String reCheck(String answer, String response) {
		Pattern p = Pattern.compile("-?\\d+");
    	Matcher m;
		// If answer is numerals, convert them to words and check again
		if (answer.matches(".*\\d.*")) {
			m = p.matcher(answer);
			while (m.find()) {
				answer = answer.replace(m.group(),
						EnglishNumberToWords.convert(
								Long.parseLong(m.group())));
			}
			return answer;
		}
		// If response is numerals, convert them to words and check again
		else if (response.matches(".*\\d.*")) {
			m = p.matcher(response);
			while (m.find()) {
				response = response.replace(m.group(),
						EnglishNumberToWords.convert(
								Long.parseLong(m.group())));
			}
			return response;
		}
		for (ArrayList<String> list : nameMap) {
			if (list.contains(answer) && list.contains(response))
				return answer;
		}
		if (acronymMap.containsKey(response))
			return acronymMap.get(response);
		return null;
	}
	
	Map<String, Integer> usersMap = new HashMap<String, Integer>();
	
	public boolean askQuestion() {
    	System.out.println("Asking question");
    	responseMap = new HashMap<String, Long>();
    	Map<String, String> question;
    	StringBuilder sb;
    	// Every 3rd question, pick one that is totally random out of
    	// New, Prioritized, Normal
    	// Other 2 questions are one that is randomly chosen from
    	// New, Prioritized
    	boolean includeNorm = true;
    	switch(((int)(3 * Math.random()))) {
    	case 0:
    		break;
    	case 1:
    	case 2:
    		includeNorm = false;
    		break;
		default:
			break;
    	}
    	int count = getQuestionCount(includeNorm);
    	if (count < 1) {
    		includeNorm = !includeNorm;
    		count = getQuestionCount(includeNorm);
    	}
    	if (count < 1)
    		return false;
    	int skip = ((int)((getQuestionCount(includeNorm)) * Math.random()));
		question = getQuestion(includeNorm, false, 1, skip);
		// TODO Distinguish between the question being too long and being
		// out of questions completely
		if (question == null || question.isEmpty())
			return false;
		if (!isDev)
			markAsTrivia(question.get("objectId"), 0);
		sb = new StringBuilder();
		sb.append(preTweet);
		sb.append("[");
		sb.append(totalQuestions+1);
		sb.append("/");
		sb.append(reqQuestions);
		sb.append("] [");
		try {
			currScore = Integer.parseInt(question.get("score"));
		} catch (NumberFormatException e) {
			currScore = 100;
		}
		if (totalQuestions+1 > BONUS_ROUND)
			currScore += BONUS_SCORE;
		else if (totalQuestions+1 > ROUND_TWO)
			currScore += PLUS_SCORE;
		sb.append(currScore);
		sb.append("pts] ");
		sb.append(question.get("question"));
		System.out.println("SCORE: " + currScore);
		ArrayList<String> tweetList = new ArrayList<String>(0);
		int index = -1;
		while (sb.length() > 140) {
			index = sb.indexOf(" ", 120);
			tweetList.add(sb.substring(0, index).trim() + " ->");
			sb.delete(0, index+1);
			sb.insert(0, "-> ");
		}
		tweetList.add(sb.toString());
		/*
		if (sb.length() > 140)
			return false;
		*/
		totalQuestions++;
		Status status;
		currTwitterStatus = new ArrayList<Long>(0);
		for (String tweet : tweetList) {
			if (currTwitterStatus.isEmpty())
				status = postTweet(tweet, null, -1);
			else {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {}
				status = postTweet(tweet, null, currTwitterStatus.get(0));
			}
			if (status != null)
				currTwitterStatus.add(status.getId());
			else
				return false;
		}
		watchTwitterStream(question.get("answer"),
				question.get("objectId"));
		return true;
    }
	
	private static Map<String, String> getQuestion(boolean includeNorm,
			boolean single, int limit, int skip) {
    	System.out.println("includeNorm: " + includeNorm);
    	System.out.println("limit: " + limit);
    	System.out.println("skip: " + skip);
    	HttpClientBuilder httpclient = HttpClientBuilder.create();
        HttpEntity entity = null;
        HttpResponse response = null;
        String responseString = null;
        String url = "https://api.parse.com/1/classes/Question?";
    	if (skip >= 0)
    		url += ("skip=" + skip);
		url += ("&limit=" + limit);
		url += "&order=updatedAt";
		if (includeNorm)
			// Choose from everything but those that are asked
			url += ("&where%3D%7B%22%24or%22%3A%5B%7B%22trivia%22%3A%7B%22%24" +
					"exists%22%3Afalse%7D%7D%2C%7B%22trivia%22%3A%7B%22%24ne" +
					"%22%3A0%7D%7D%5D%7D");
		else
			// Choose from only new and prioritized
			url += ("&where%3D%7B%22%24or%22%3A%5B%7B%22trivia%22%3A%7B%22%24" +
					"nin%22%3A%5B0%2C1%5D%7D%7D%2C%7B%22trivia%22%3A%7B%22%24" +
					"exists%22%3Afalse%7D%7D%5D%7D");

        HttpGet httpGet = new HttpGet(url);
        System.out.println(url);
        httpGet.addHeader("X-Parse-Application-Id",
        		"ImI8mt1EM3NhZNRqYZOyQpNSwlfsswW73mHsZV3R");
        httpGet.addHeader("X-Parse-REST-API-Key",
        		"1smRSlfAvbFg4AsDxat1yZ3xknHQbyhzZ4msAi5w");
        try {
            response = httpclient.build().execute(httpGet);
            if (response.getStatusLine().getStatusCode() != 200) {
                System.out.println("GET questions failed!");
                return null;
            }
            entity = response.getEntity();
            if (entity != null)
                 responseString = EntityUtils.toString(response.getEntity());  
        } catch (ClientProtocolException e1) {
            System.out.println("Failed to connect to " +
                    httpGet.getURI().toASCIIString());
            e1.printStackTrace();
        } catch (IOException e1) {
            System.out.println("Failed to get setlist from " +
                    httpGet.getURI().toASCIIString());
            e1.printStackTrace();
        }
        return getQuestionInfoFromResponse(responseString);
    }
	
	private static Map<String, String> getQuestionInfoFromResponse(
    		String responseString) {
    	JsonFactory f = new JsonFactory();
        JsonParser jp;
        Map<String, String> questionMap = new HashMap<String, String>();
        String fieldname;
        try {
            jp = f.createParser(responseString);
            jp.nextToken();
            jp.nextToken();
            fieldname = jp.getCurrentName();
            if ("results".equals(fieldname)) { // contains an object
                jp.nextToken();
                while (jp.nextToken() != null) {
                    jp.nextToken();
                    fieldname = jp.getCurrentName();
                    if ("answer".equals(fieldname)) {
                        jp.nextToken();
                        questionMap.put(fieldname, jp.getText());
                    }
                    else if ("question".equals(fieldname)) {
                    	questionMap.put(fieldname, jp.getText());
                    }
                    else if ("objectId".equals(fieldname)) {
                    	questionMap.put(fieldname, jp.getText());
                    }
                    else if ("score".equals(fieldname)) {
                    	questionMap.put(fieldname, jp.getText());
                    }
                    else if ("trivia".equals(fieldname)) {
                    	System.out.println("TRIVIA VALUE: " + jp.getText());
                    }
                }
            }
            jp.close(); // ensure resources get cleaned up timely and properly
        } catch (JsonParseException e) {
            System.out.println("Failed to parse " + responseString);
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("Failed to parse " + responseString);
            e.printStackTrace();
        }
        return questionMap;
    }
	
	private static int getQuestionCount(boolean includeNorm) {
    	HttpClientBuilder httpclient = HttpClientBuilder.create();
        HttpEntity entity = null;
        HttpResponse response = null;
        String responseString = null;
        String url = "https://api.parse.com/1/classes/Question?";
        url += "count=1&limit=0";
        if (includeNorm)
			// Choose from everything but those that are asked
			url += ("&where%3D%7B%22%24or%22%3A%5B%7B%22trivia%22%3A%7B%22%24" +
					"exists%22%3Afalse%7D%7D%2C%7B%22trivia%22%3A%7B%22%24ne" +
					"%22%3A0%7D%7D%5D%7D");
		else
			// Choose from only new and prioritized
			url += ("&where%3D%7B%22%24or%22%3A%5B%7B%22trivia%22%3A%7B%22%24" +
					"nin%22%3A%5B0%2C1%5D%7D%7D%2C%7B%22trivia%22%3A%7B%22%24" +
					"exists%22%3Afalse%7D%7D%5D%7D");
        HttpGet httpGet = new HttpGet(url);
        System.out.println(url);
        httpGet.addHeader("X-Parse-Application-Id",
        		"ImI8mt1EM3NhZNRqYZOyQpNSwlfsswW73mHsZV3R");
        httpGet.addHeader("X-Parse-REST-API-Key",
        		"1smRSlfAvbFg4AsDxat1yZ3xknHQbyhzZ4msAi5w");
        try {
            response = httpclient.build().execute(httpGet);
            if (response.getStatusLine().getStatusCode() != 200) {
                System.out.println("GET questions failed!");
                return -1;
            }
            entity = response.getEntity();
            if (entity != null)
                 responseString = EntityUtils.toString(response.getEntity());  
        } catch (ClientProtocolException e1) {
            System.out.println("Failed to connect to " +
                    httpGet.getURI().toASCIIString());
            e1.printStackTrace();
        } catch (IOException e1) {
            System.out.println("Failed to get setlist from " +
                    httpGet.getURI().toASCIIString());
            e1.printStackTrace();
        }
        System.out.println("COUNT: " + responseString);
        return getQuestionCountFromResponse(responseString);
    }
	
	private static int getQuestionCountFromResponse(String responseString) {
    	JsonFactory f = new JsonFactory();
        JsonParser jp;
        int count = -1;
        String fieldname;
        try {
            jp = f.createParser(responseString);
            jp.nextToken();
            jp.nextToken();
            fieldname = jp.getCurrentName();
            if ("results".equals(fieldname)) { // contains an object
                jp.nextToken();
                while (jp.nextToken() != null) {
                    jp.nextToken();
                    fieldname = jp.getCurrentName();
                    if ("count".equals(fieldname)) {
                        jp.nextToken();
                        count = Integer.parseInt(jp.getText().trim());
                    }
                }
            }
            jp.close(); // ensure resources get cleaned up timely and properly
        } catch (JsonParseException e) {
            System.out.println("Failed to parse " + responseString);
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("Failed to parse " + responseString);
            e.printStackTrace();
        }
        return count;
    }
	
	private static void markAsTrivia(String objectId, int triviaLevel) {
    	String setTrivia = "{\"trivia\":" + triviaLevel + "}";
    	HttpClientBuilder httpclient = HttpClientBuilder.create();
        HttpEntity entity = new StringEntity(setTrivia,
        		ContentType.APPLICATION_JSON);
        HttpResponse response = null;
        String url = "https://api.parse.com/1/classes/Question/" + objectId;
        HttpPut httpPut = new HttpPut(url);
        httpPut.setEntity(entity);
        httpPut.addHeader("X-Parse-Application-Id",
        		"ImI8mt1EM3NhZNRqYZOyQpNSwlfsswW73mHsZV3R");
        httpPut.addHeader("X-Parse-REST-API-Key",
        		"1smRSlfAvbFg4AsDxat1yZ3xknHQbyhzZ4msAi5w");
        try {
            response = httpclient.build().execute(httpPut);
            if (response.getStatusLine().getStatusCode() != 200) {
                System.out.println("PUT question " + objectId + " failed!");
                return;
            }
        } catch (ClientProtocolException e1) {
            System.out.println("Failed to connect to " +
            		httpPut.getURI().toASCIIString());
            e1.printStackTrace();
        } catch (IOException e1) {
            System.out.println("Failed to get setlist from " +
            		httpPut.getURI().toASCIIString());
            e1.printStackTrace();
        }
    }
    
	// TODO Make a special method to mark a question a specific value
    private static void markAllAsTrivia(int triviaLevel) {
    	Map<String, String> questionMap;
    	do {
    		questionMap = getQuestion(false, true, 1, 0);
    		if (!questionMap.isEmpty())
    			markAsTrivia(questionMap.get("objectId"), triviaLevel);
    	} while (!questionMap.isEmpty());
    }
	
}
