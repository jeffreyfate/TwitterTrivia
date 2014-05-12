package com.jeffthefate;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

//import twitter4j.DirectMessage;
//import twitter4j.RateLimitStatusEvent;
//import twitter4j.RateLimitStatusListener;
//import twitter4j.StallWarning;
import twitter4j.Status;
//import twitter4j.StatusDeletionNotice;
import twitter4j.StatusUpdate;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
//import twitter4j.TwitterStream;
//import twitter4j.TwitterStreamFactory;
//import twitter4j.User;
//import twitter4j.UserList;
//import twitter4j.UserStreamListener;
import twitter4j.conf.Configuration;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

/**
 * Creates a game of trivia on Twitter, played on the account in the given
 * configuration.
 * 
 * @author Jeff Fate
 * 
 */
public class Trivia /*implements UserStreamListener*/ {

	private static final int WAIT_FOR_QUESTION = (5 * 60 * 1000);
	private static final int WAIT_BETWEEN_QUESTIONS = 90000;
	private static final int WAIT_FOR_ANSWER = (60 * 1000);
	private static final int WAIT_FOR_LIGHTNING = (20 * 1000);
	private static final int WAIT_FOR_RESPONSE = (60 * 1000);
	private static final int WAIT_FOR_TIP = (3 * 60 * 1000);

	private static final int PRE_ROUND_TIME = (15 * 1000);
	private static final int LEADERS_EVERY = 10;
	private static final int PLUS_SCORE = 500;
	private static final int BONUS_SCORE = 1000;

	private static int diffCount;
	private ArrayList<ArrayList<String>> nameMap;
	private HashMap<String, String> acronymMap;
	private ArrayList<String> replaceList;
	private static List<String> winners = new ArrayList<String>(0);
	private static Map<String, Integer> scoreMap = new HashMap<String, Integer>();
	private static int currScore = 0;
	private static String currAnswer;
	//private TwitterStream twitterStream = null;
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

	private int totalQuestions;
	private int questionCount;
	private int bonusCount;

	private boolean isDev;

	private Message message;

	private HashMap<String, Long> responseMap = new HashMap<String, Long>();
	private ArrayList<String> tipList;

	private String preTweet;

	private int lightningCount;
	private int lightningRound;
	private int roundTwo;
	private int bonusRound;
	private boolean isLightning = false;
	private boolean isBonus = false;
	
	private boolean inTrivia = false;

	private Timer timer = new Timer();

	Map<String, Integer> usersMap = new HashMap<String, Integer>();

	static {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		System.setProperty("currentDate", dateFormat.format(new Date()));
	}

	private static Logger logger = Logger.getLogger(Trivia.class);

	public Trivia(String templateFile, String fontFile, String leadersTitle,
			int mainSize, int dateSize, int limit, Configuration twitterConfig,
			int questionCount, int bonusCount,
			ArrayList<ArrayList<String>> nameMap,
			HashMap<String, String> acronymMap, ArrayList<String> replaceList,
			ArrayList<String> tipList, boolean isDev, String preTweet,
			int lightningCount) {
		this.templateFile = templateFile;
		this.fontFile = fontFile;
		this.leadersTitle = leadersTitle;
		this.mainSize = mainSize;
		this.dateSize = dateSize;
		this.limit = limit;
		this.twitterConfig = twitterConfig;
		this.questionCount = questionCount;
		this.bonusCount = bonusCount;
		this.nameMap = nameMap;
		this.acronymMap = acronymMap;
		this.replaceList = replaceList;
		this.tipList = tipList;
		this.isDev = isDev;
		this.preTweet = preTweet;
		this.lightningCount = lightningCount;
		message = new Message();
	}

	private class Message {
		public Message() {
		}
	}

	public void setQuestionCount(int questionCount) {
		logger.info("Setting question count: " + questionCount);
		this.questionCount = questionCount;
	}
	
	public int getQuestionCount() {
		return questionCount;
	}

	public void setBonusCount(int bonusCount) {
		logger.info("Setting bonus count: " + bonusCount);
		this.bonusCount = bonusCount;
	}
	
	public int getBonusCount() {
		return bonusCount;
	}

	public void setLightningCount(int lightningCount) {
		logger.info("Setting lightning count: " + lightningCount);
		this.lightningCount = lightningCount;
	}
	
	public int getLightningCount() {
		return lightningCount;
	}

	public void startTrivia(boolean preShowTweet, String preShowText,
			int preShowTime) {
		logger.info("Starting trivia...");
		currScore = 0;
		isLightning = false;
		isBonus = false;
		logger.info("Setting inTrivia to false");
		inTrivia = false;
		usersMap.clear();
		scoreMap.clear();
		winners.clear();
		/*
		twitterStream = new TwitterStreamFactory(twitterConfig).getInstance();
		twitterStream.addRateLimitStatusListener(new RateLimitStatusListener() {
			public void onRateLimitReached(RateLimitStatusEvent event) {
				logger.error("Rate limit reached!");
				logger.error("Limit: " + event.getRateLimitStatus().getLimit());
				logger.error("Remaining: " +
						event.getRateLimitStatus().getRemaining());
				logger.error("Reset time: " +
						event.getRateLimitStatus().getResetTimeInSeconds());
				logger.error("Seconds until reset: " +
						event.getRateLimitStatus().getSecondsUntilReset());
			}

			public void onRateLimitStatus(RateLimitStatusEvent event) {
				logger.warn("Rate limit event!");
				logger.warn("Limit: " + event.getRateLimitStatus().getLimit());
				logger.warn("Remaining: " +
						event.getRateLimitStatus().getRemaining());
			}
		});
		*/
		totalQuestions = 0;
		if (preShowTweet) {
			logger.info("Sending preshow tweet");
			postTweet(preShowText, null, -1);
			if (tipList.isEmpty()) {
				try {
					Thread.sleep(preShowTime);
				} catch (InterruptedException e1) {
				}
			} else {
				logger.info("Sending preshow tips");
				int preShowTips = (int) (preShowTime / WAIT_FOR_TIP) - 1;
				Collections.shuffle(tipList);
				for (int i = 0; i < preShowTips; i++) {
					try {
						Thread.sleep(WAIT_FOR_TIP);
					} catch (InterruptedException e1) {
					}
					postTweet(preTweet + tipList.get(i), null, -1);
				}
			}
		}
		bonusRound = questionCount - bonusCount;
		int startRange = (questionCount - bonusCount) / 3;
		int endRange = startRange * 2;
		lightningRound = (int) (Math.random() * (endRange - startRange))
				+ startRange;
		logger.info("Lightning start question: " + lightningRound);
		roundTwo = lightningRound + lightningCount;
		logger.info("Round 2 start question: " + roundTwo);
		boolean success = false;
		for (int i = 0; i < questionCount; i++) {
			logger.info("QUESTION " + (i + 1));
			synchronized (message) {
				do {
					success = askQuestion();
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
			logger.info(user.getKey() + " : " + user.getValue());
		}
		stopListening();
	}

	private void setTimer(final long newWait, boolean killStream) {
		logger.info("Setting timer: " + newWait + " : " + killStream);
		timer.cancel();
		timer.purge();
		timer = new Timer();
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				logger.info("Timer ran " + newWait + "ms");
				finishTrivia();
				synchronized (message) {
					message.notify();
				}
			}
		}, newWait);
		if (killStream) {
			stopListening();
		}
	}

	private void stopListening() {
		logger.info("Stop listening to twitter stream");
		inTrivia = false;
		/*
		twitterStream.removeListener(this);
		twitterStream.shutdown();
		*/
	}

	private void finishTrivia() {
		logger.info("Finishing question");
		stopListening();
		onCorrectUser(currAnswer, winners);
		sendUpdateTweets();
		isLightning = totalQuestions >= lightningRound
				&& totalQuestions < roundTwo;
		logger.info("In lightning round: " + isLightning);
	}

	private void sendUpdateTweets() {
		logger.info("Sending update tweets if applicable");
		if (totalQuestions < questionCount) {
			if (totalQuestions % LEADERS_EVERY == 0) {
				screenshot = new TriviaScreenshot(templateFile, fontFile,
						leadersTitle, generateLeaderboard(), mainSize,
						dateSize, limit);
				postTweet(preTweet + "Current Top 10",
						new File(screenshot.getOutputFilename()), -1);
			}
			if (totalQuestions == (questionCount - bonusCount)) {
				postTweet("[BONUS ROUND] " + BONUS_SCORE + "pts added to the"
						+ " value of each question - Points awarded to first "
						+ "correct answer ONLY", null, -1);
				try {
					Thread.sleep(PRE_ROUND_TIME);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			} else if (totalQuestions == lightningRound && lightningCount > 0) {
				postTweet("[LIGHTNING ROUND] Points awarded to first correct "
						+ "answer only", null, -1);
				try {
					Thread.sleep(PRE_ROUND_TIME);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			} else if (totalQuestions == roundTwo) {
				postTweet("[Starting ROUND 2] " + PLUS_SCORE
						+ "pts added to the value of each question", null, -1);
				try {
					Thread.sleep(PRE_ROUND_TIME);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}
		}
	}

	public void processTweet(Status status) {
		logger.info("inTrivia: " + inTrivia);
		if (!inTrivia) {
			return;
		}
		logger.info("Tweet: " + status.getCreatedAt().toString() + " : "
				+ status.getText());
		String userName = status.getUser().getScreenName();
		String inReplyTo = status.getInReplyToScreenName();
		logger.info("From: " + userName);
		logger.info("Reply to: " + inReplyTo);
		long convoId = status.getInReplyToStatusId();
		// Check to see if the user can submit an answer
		Long userTime = responseMap.get(userName);
		// Keep track of people who respond directly to the question
		if (currTwitterStatus.contains(convoId)) {
			if (usersMap.containsKey(userName))
				usersMap.put(userName, usersMap.get(userName) + 1);
			else
				usersMap.put(userName, 1);
		}
		if (userTime != null && currTwitterStatus.contains(convoId)) {
			long waitPassed = System.currentTimeMillis() - userTime;
			if (waitPassed < WAIT_FOR_RESPONSE) {
				logger.info(userName + " already answered this question");
				logger.info(userName + " needs to wait another "
						+ ((WAIT_FOR_RESPONSE - waitPassed) / 1000) + "seconds");
				return;
			}
		} else {
			logger.info(userName + " added to jail");
			responseMap.put(userName, System.currentTimeMillis());
		}
		// Massage response and answer
		String response = massageResponse(status.getText());
		logger.info(userName + " massaged response: " + response);
		String answer = massageAnswer(currAnswer);
		logger.info("Massaged answer: " + answer);
		// Get the diff characters between the answer and the response
		boolean isCorrect = checkAnswer(answer, response, userName);
		if (!isCorrect) {
			logger.info("Not correct, rechecking");
			isCorrect = checkAnswer(answer, reCheck(answer, response), userName);
		}
		if ((isLightning && winners.size() == 1)
				|| (isBonus && winners.size() == 1) || winners.size() == 3) {
			logger.info("At least one correct answer");
			if (isLightning) {
				logger.info("Lightning round - moving to next question");
				finishTrivia();
				synchronized (message) {
					message.notify();
				}
			} else {
				logger.info("Bonus and 1 correct OR 3 correct - next question");
				setTimer(WAIT_FOR_ANSWER, true);
			}
			return;
		}
		if (isCorrect) {
			logger.info("CORRECT!");
			setTimer(WAIT_FOR_ANSWER, false);
		}
	}

	private String massageResponse(String text) {
		return text
				.toLowerCase(Locale.getDefault())
				.replaceFirst(
						"(?<=^|(?<=[^a-zA-Z0-9-_\\.]))@([A-Za-z]+[A-Za-z0-9]+)",
						"").replaceAll("[.,'`\":;/?\\-!@#]", "").trim();
	}

	private String massageAnswer(String text) {
		return text.toLowerCase(Locale.getDefault())
				.replaceAll("[.,'`\":;/?\\-!@#]", "").trim();
	}

	public void onCorrectUser(String correctAnswer, List<String> screenNames) {
		logger.info("Correct answer: " + correctAnswer);
		logger.info("Winners: " + screenNames);
		sb = new StringBuilder();
		sb.append(preTweet);
		sb.append("Answer: \"");
		sb.append(correctAnswer);
		sb.append("\"");
		String screenName;
		for (int i = 0; i < screenNames.size(); i++) {
			screenName = screenNames.get(i);
			if (sb.length() + 11 + screenName.length()
					+ Integer.toString(scoreMap.get(screenName)).length() > 140)
				break;
			else
				sb.append("\n");
			sb.append("#");
			sb.append(i + 1);
			sb.append(" @");
			sb.append(screenName);
			sb.append(" [");
			sb.append(scoreMap.get(screenName));
			sb.append("pts]");
		}
		postTweet(sb.toString(), null, currTwitterStatus != null
				&& !currTwitterStatus.isEmpty() ? currTwitterStatus.get(0) : -1);
		try {
			if (isLightning) {
				logger.info("Waiting " + WAIT_FOR_LIGHTNING
						+ " before next question");
				Thread.sleep(WAIT_FOR_LIGHTNING);
			} else {
				logger.info("Waiting " + WAIT_BETWEEN_QUESTIONS
						+ " before next question");
				Thread.sleep(WAIT_BETWEEN_QUESTIONS);
			}
		} catch (InterruptedException e) {
		}
	}

	private void onFinalLeaders() {
		logger.info("Posting final leaderboard");
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
			} else {
				if (player.getValue() == firstScore) {
					winner = "@" + firstPlayer + " and @" + player.getKey()
							+ " tied with " + firstScore + "pts!";
					break;
				}
			}
			winner = "@" + firstPlayer + " with " + firstScore
					+ "pts is the winner!";
			if (playerNum > 1)
				break;
		}
		screenshot = new TriviaScreenshot(templateFile, fontFile, leadersTitle,
				sortedMap, mainSize, dateSize, limit);
		postTweet(preTweet + winner, new File(screenshot.getOutputFilename()),
				-1);
	}

	private void watchTwitterStream(String answer) {
		logger.info("Watching twitter stream for " + answer);
		winners.clear();
		winners = new ArrayList<String>(0);
		currAnswer = answer;
		/*
		twitterStream.addListener(this);
		twitterStream.user();
		*/
		setTimer(WAIT_FOR_QUESTION, false);
		logger.info("Setting inTrivia to true");
		inTrivia = true;
	}

	private static Map<String, Integer> generateLeaderboard() {
		logger.info("Creating leaderboard");
		ValueComparator scoreComparator = new ValueComparator(scoreMap);
		Map<String, Integer> sortedMap = new TreeMap<String, Integer>(
				scoreComparator);
		sortedMap.putAll(scoreMap);
		return sortedMap;
	}

	private static class ValueComparator implements Comparator<String> {
		Map<String, Integer> base;

		public ValueComparator(Map<String, Integer> base) {
			this.base = base;
		}

		// Note: this comparator imposes orderings that are inconsistent with
		// equals.
		public int compare(String a, String b) {
			if (base.get(a) >= base.get(b))
				return -1;
			else
				return 1;
			// returning 0 would merge keys
		}
	}

	public Status postTweet(String message, File file, long replyTo) {
		logger.info("Post tweet (length: " + message.length() + "):");
		logger.info(message);
		Twitter twitter = new TwitterFactory(twitterConfig).getInstance();
		StatusUpdate statusUpdate = new StatusUpdate(message);
		logger.info("Status (length: " + statusUpdate.getStatus().length()
				+ "):");
		logger.info(statusUpdate.getStatus());
		Status status = null;
		if (file != null) {
			logger.info("Adding screenshot");
			statusUpdate.media(file);
		}
		statusUpdate.setInReplyToStatusId(replyTo);
		try {
			status = twitter.updateStatus(statusUpdate);
		} catch (TwitterException e) {
			logger.error("Failed to get timeline: " + e.getMessage());
			e.printStackTrace();
		}
		return status;
	}
	/*
	public void onBlock(User arg0, User arg1) {
	}

	public void onDeletionNotice(long arg0, long arg1) {
	}

	public void onDirectMessage(DirectMessage arg0) {
	}

	public void onFavorite(User arg0, User arg1, Status arg2) {
	}

	public void onFollow(User arg0, User arg1) {
	}

	public void onFriendList(long[] arg0) {
	}

	public void onUnblock(User arg0, User arg1) {
	}

	public void onUnfavorite(User arg0, User arg1, Status arg2) {
	}
	
	public void onUnfollow(User arg0, User arg1) {	
	}

	public void onUserListCreation(User arg0, UserList arg1) {
	}

	public void onUserListDeletion(User arg0, UserList arg1) {
	}

	public void onUserListMemberAddition(User arg0, User arg1, UserList arg2) {
	}

	public void onUserListMemberDeletion(User arg0, User arg1, UserList arg2) {
	}

	public void onUserListSubscription(User arg0, User arg1, UserList arg2) {
	}

	public void onUserListUnsubscription(User arg0, User arg1, UserList arg2) {
	}

	public void onUserListUpdate(User arg0, UserList arg1) {
	}

	public void onUserProfileUpdate(User arg0) {
	}

	public void onDeletionNotice(StatusDeletionNotice arg0) {
	}

	public void onScrubGeo(long arg0, long arg1) {
	}

	public void onStallWarning(StallWarning arg0) {
	}

	public void onStatus(Status status) {
		processTweet(status);
	}

	public void onTrackLimitationNotice(int arg0) {
	}

	public void onException(Exception e) {
		e.printStackTrace();
	}
	*/
	private boolean checkAnswer(String answer, String response,
			String screenName) {
		logger.info("Checking answer (" + answer + ") and response ("
				+ response + ")");
		if (answer == null || response == null)
			return false;
		logger.info("Filtering out pronouns");
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
		if (answer.matches("^[0-9]+$")) {
			buffer = 0;
		}
		logger.info("Match buffer: " + buffer);
		diffCount = StringUtils
				.getLevenshteinDistance(response, answer, buffer);
		boolean isCorrect = false;
		logger.info("Difference count: " + diffCount);
		switch (diffCount) {
		case -1:
			logger.info("Didn't match closely enough");
			break;
		default:
			isCorrect = true;
			if ((isLightning && winners.isEmpty())
					|| (isBonus && winners.isEmpty())
					|| (!winners.contains(screenName) && winners.size() < 3)) {
				Integer userScore = scoreMap.get(screenName);
				int pointsEarned = currScore
						- (winners.size() * (currScore / 4));
				winners.add(screenName);
				if (userScore == null)
					scoreMap.put(screenName, pointsEarned);
				else
					scoreMap.put(screenName, userScore + pointsEarned);
				logger.info("Adding " + screenName + " to winners");
			}
			break;
		}
		return isCorrect;
	}

	private String reCheck(String answer, String response) {
		logger.info("Running recheck on response (" + answer + ":" + response
				+ ")");
		Pattern p = Pattern.compile("-?\\d+");
		Matcher m;
		// If answer is numerals, convert them to words and check again
		if (answer.matches(".*\\d.*")) {
			logger.info("Answer " + answer + " matches as numeral");
			m = p.matcher(answer);
			while (m.find()) {
				answer = answer
						.replace(m.group(), EnglishNumberToWords.convert(Long
								.parseLong(m.group())));
			}
			logger.info("Translated answer: " + answer);
			return answer;
		}
		// If response is numerals, convert them to words and check again
		else if (response.matches(".*\\d.*")) {
			logger.info("Response " + response + " matches as numeral");
			m = p.matcher(response);
			while (m.find()) {
				response = response
						.replace(m.group(), EnglishNumberToWords.convert(Long
								.parseLong(m.group())));
			}
			logger.info("Translated response: " + response);
			return response;
		}
		logger.info("Looking through name map for matching answer");
		for (ArrayList<String> list : nameMap) {
			if (list.contains(answer) && list.contains(response)) {
				logger.info("Found match for " + response + " and " + answer);
				return answer;
			}
		}
		logger.info("Looking through acronym map for matching answer");
		if (acronymMap.containsKey(response)) {
			return acronymMap.get(response);
		}
		return null;
	}

	public boolean askQuestion() {
		logger.info("Asking question");
		responseMap.clear();
		responseMap = new HashMap<String, Long>();
		Map<String, String> question;
		StringBuilder sb;
		// Every 3rd question, pick one that is totally random out of
		// New, Prioritized, Normal
		// Other 2 questions are one that is randomly chosen from
		// New, Prioritized
		boolean prioritize = true;
		switch (((int) (3 * Math.random()))) {
		case 0:
			prioritize = false;
			break;
		default:
			break;
		}
		int count = getQuestionCount(prioritize, isLightning);
		logger.info("Question count: " + count);
		switch (count) {
		case -1:
			// Failure in getting questions
			logger.warn("Failure getting question count - need to retry");
			return false;
		case 0:
			// Should get at least one back, otherwise there are none left and
			// all 1s should be marked as 0s
			logger.warn("No questions left!");
			if (!prioritize && !isDev) {
				markAllAsTrivia(1);
				return false;
			} else {
				return false;
			}
		}
		int skip = ((int) (count * Math.random()));
		List<Map<String, String>> questionList = getQuestion(prioritize,
				isLightning, false, 1, skip);
		if (!questionList.isEmpty()) {
			question = questionList.get(0);
		} else {
			return false;
		}
		if (question == null || question.isEmpty()) {
			return false;
		}
		if (!isDev) {
			markAsTrivia(question.get("objectId"), 0);
		}
		sb = new StringBuilder();
		sb.append(preTweet);
		sb.append("[");
		sb.append(totalQuestions + 1);
		sb.append("/");
		sb.append(questionCount);
		sb.append("] [");
		String score = question.get("score");
		try {
			currScore = Integer.parseInt(score);
		} catch (NumberFormatException e) {
			logger.warn("Bad score format for question "
					+ question.get("objectId") + ": " + question.get("score"));
			if (score == null) {
				currScore = 1000;
			} else {
				currScore = 400;
			}
		}
		logger.info("Current question score: " + currScore);
		if (totalQuestions + 1 > (questionCount - bonusCount)) {
			logger.info("Adding a " + BONUS_SCORE + " bonus to question score");
			currScore += BONUS_SCORE;
		} else if (totalQuestions + 1 > roundTwo) {
			logger.info("Adding a " + PLUS_SCORE + " bonus to question score");
			currScore += PLUS_SCORE;
		}
		sb.append(currScore);
		sb.append("pts] ");
		sb.append(question.get("question"));
		logger.info("Total question score: " + currScore);
		ArrayList<String> tweetList = new ArrayList<String>(0);
		int index = -1;
		while (sb.length() > 140) {
			index = sb.indexOf(" ", 120);
			tweetList.add(sb.substring(0, index).trim() + " ->");
			sb.delete(0, index + 1);
			sb.insert(0, "-> ");
		}
		tweetList.add(sb.toString());
		totalQuestions++;
		if (totalQuestions > bonusRound) {
			isBonus = true;
		}
		Status status;
		currTwitterStatus = new ArrayList<Long>(0);
		for (String tweet : tweetList) {
			if (currTwitterStatus.isEmpty()) {
				status = postTweet(tweet, null, -1);
			} else {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
				}
				status = postTweet(tweet, null, currTwitterStatus.get(0));
			}
			if (status != null) {
				currTwitterStatus.add(status.getId());
			}
			else {
				return false;
			}
		}
		watchTwitterStream(question.get("answer"));
		return true;
	}

	private static List<Map<String, String>> getQuestion(boolean prioritize,
			boolean lightning, boolean reset, int limit, int skip) {
		logger.info("Getting question (prioritize: " + prioritize
				+ ", lightning: " + lightning + ", reset: " + reset
				+ ", limit: " + limit + ", skip: " + skip);
		HttpClientBuilder httpclient = HttpClientBuilder.create();
		HttpEntity entity = null;
		HttpResponse response = null;
		String responseString = null;
		String url = "https://api.parse.com/1/classes/Question?";
		if (skip >= 0) {
			url += ("skip=" + skip);
		}
		url += ("&limit=" + limit);
		url += "&order=updatedAt";
		if (lightning) {
			// Get questions only from Lyrics and Scramble categories
			logger.info("Fetching lyrics or scramble question");
			url += ("&where%3D%7B%22category%22%3A%7B%22%24in%22%3A%5B%22"
					+ "Lyrics%22%2C%22Scramble%22%5D%7D%7D");
		} else if (reset) {
			logger.info("Fetching asked question");
			url += "where%3D%7B%22trivia%22%3A0%7D";
		} else {
			if (!prioritize) {
				// Choose from everything but those that are asked
				logger.info("Fetching any unasked question");
				url += ("&where%3D%7B%22%24or%22%3A%5B%7B%22trivia%22%3A%7B%22%24"
						+ "exists%22%3Afalse%7D%7D%2C%7B%22trivia%22%3A%7B%22%24ne"
						+ "%22%3A0%7D%7D%5D%7D");
			} else {
				// Choose from only new and prioritized
				logger.info("Fetching new or prioritized question");
				url += ("&where%3D%7B%22%24or%22%3A%5B%7B%22trivia%22%3A%7B%22%24"
						+ "nin%22%3A%5B0%2C1%5D%7D%7D%2C%7B%22trivia%22%3A%7B%22%24"
						+ "exists%22%3Afalse%7D%7D%5D%7D");
			}
		}
		HttpGet httpGet = new HttpGet(url);
		logger.info("Fetching questions via: ");
		logger.info(url);
		httpGet.addHeader("X-Parse-Application-Id",
				"ImI8mt1EM3NhZNRqYZOyQpNSwlfsswW73mHsZV3R");
		httpGet.addHeader("X-Parse-REST-API-Key",
				"1smRSlfAvbFg4AsDxat1yZ3xknHQbyhzZ4msAi5w");
		try {
			response = httpclient.build().execute(httpGet);
		} catch (IOException e) {
			logger.error("Failed to connect to "
					+ httpGet.getURI().toASCIIString());
			e.printStackTrace();
		}
		if (response.getStatusLine().getStatusCode() != 200) {
			logger.error("Fetch question count response NOT 200!");
		}
		entity = response.getEntity();
		if (entity != null) {
			try {
				responseString = EntityUtils.toString(response.getEntity());
			} catch (Exception e) {
				logger.error("Failed to parse entity from "
						+ httpGet.getURI().toASCIIString());
				e.printStackTrace();
			}
		}
		return getQuestionInfoFromResponse(responseString);
	}

	private static List<Map<String, String>> getQuestionInfoFromResponse(
			String responseString) {
		List<Map<String, String>> mapList = new ArrayList<Map<String, String>>(
				0);
		if (responseString == null) {
			return mapList;
		}
		JsonFactory f = new JsonFactory();
		JsonParser jp;
		Map<String, String> questionMap = new HashMap<String, String>();
		String fieldname;
		try {
			jp = f.createParser(responseString);
			jp.nextToken(); // START_OBJECT
			while (jp.nextToken() != JsonToken.END_OBJECT) {
				fieldname = jp.getCurrentName(); // "results"
				jp.nextToken(); // START_ARRAY
				if ("results".equals(fieldname)) { // contains an object
					while (jp.nextToken() != JsonToken.END_ARRAY) { // START_OBJECT
						while (jp.nextToken() != JsonToken.END_OBJECT) { // key
							fieldname = jp.getCurrentName();
							jp.nextToken(); // value
							if ("answer".equals(fieldname)) {
								questionMap.put(fieldname, jp.getText());
							} else if ("question".equals(fieldname)) {
								questionMap.put(fieldname, jp.getText());
							} else if ("objectId".equals(fieldname)) {
								questionMap.put(fieldname, jp.getText());
							} else if ("score".equals(fieldname)) {
								questionMap.put(fieldname, jp.getText());
							} else if ("trivia".equals(fieldname)) {
								logger.info("TRIVIA VALUE: " + jp.getText());
							}
						}
						mapList.add(questionMap);
						questionMap = new HashMap<String, String>();
					}
				}
			}
			jp.close(); // ensure resources get cleaned up timely and properly
		} catch (Exception e) {
			logger.error("Failed to parse " + responseString);
			e.printStackTrace();
		}
		return mapList;
	}

	private static int getQuestionCount(boolean prioritize, boolean lightning) {
		HttpClientBuilder httpclient = HttpClientBuilder.create();
		HttpEntity entity = null;
		HttpResponse response = null;
		String responseString = null;
		String url = "https://api.parse.com/1/classes/Question?";
		url += "count=1&limit=0";
		if (lightning) {
			// Get questions only from Lyrics and Scramble categories
			logger.info("Fetching lyrics or scramble count");
			url += ("&where%3D%7B%22category%22%3A%7B%22%24in%22%3A%5B%22"
					+ "Lyrics%22%2C%22Scramble%22%5D%7D%7D");
		} else {
			if (!prioritize) {
				// Choose from everything but those that are asked
				logger.info("Fetching any unasked count");
				url += ("&where%3D%7B%22%24or%22%3A%5B%7B%22trivia%22%3A%7B%22%24"
						+ "exists%22%3Afalse%7D%7D%2C%7B%22trivia%22%3A%7B%22%24ne"
						+ "%22%3A0%7D%7D%5D%7D");
			} else {
				// Choose from only new and prioritized
				logger.info("Fetching new or prioritized count");
				url += ("&where%3D%7B%22%24or%22%3A%5B%7B%22trivia%22%3A%7B%22%24"
						+ "nin%22%3A%5B0%2C1%5D%7D%7D%2C%7B%22trivia%22%3A%7B%22%24"
						+ "exists%22%3Afalse%7D%7D%5D%7D");
			}
		}
		HttpGet httpGet = new HttpGet(url);
		logger.info("Fetching questions via: ");
		logger.info(url);
		httpGet.addHeader("X-Parse-Application-Id",
				"ImI8mt1EM3NhZNRqYZOyQpNSwlfsswW73mHsZV3R");
		httpGet.addHeader("X-Parse-REST-API-Key",
				"1smRSlfAvbFg4AsDxat1yZ3xknHQbyhzZ4msAi5w");
		try {
			response = httpclient.build().execute(httpGet);
		} catch (IOException e) {
			logger.error("Failed to connect to "
					+ httpGet.getURI().toASCIIString());
			e.printStackTrace();
			return -1;
		}
		if (response.getStatusLine().getStatusCode() != 200) {
			logger.error("Fetch question count response NOT 200!");
			return -1;
		}
		entity = response.getEntity();
		if (entity != null) {
			try {
				responseString = EntityUtils.toString(response.getEntity());
			} catch (Exception e) {
				logger.error("Failed to parse entity from "
						+ httpGet.getURI().toASCIIString());
				e.printStackTrace();
				return -1;
			}
		} else {
			return -1;
		}
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
		} catch (Exception e) {
			logger.error("Failed to parse " + responseString);
			e.printStackTrace();
		}
		return count;
	}

	private static void markAsTrivia(String objectId, int triviaLevel) {
		logger.info("Marking question " + objectId + " to " + triviaLevel);
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
				logger.error("Edit question " + objectId + " response NOT 200!");
				return;
			}
		} catch (Exception e) {
			logger.error("Failed to connect to "
					+ httpPut.getURI().toASCIIString());
			e.printStackTrace();
		}
	}

	private static void markAllAsTrivia(int triviaLevel) {
		logger.info("Setting all questions to " + triviaLevel);
		List<Map<String, String>> questionList;
		do {
			questionList = getQuestion(false, false, true, 1000, 0);
			if (!questionList.isEmpty()) {
				for (Map<String, String> questionMap : questionList) {
					markAsTrivia(questionMap.get("objectId"), triviaLevel);
				}
			}
		} while (!questionList.isEmpty());
	}

}
