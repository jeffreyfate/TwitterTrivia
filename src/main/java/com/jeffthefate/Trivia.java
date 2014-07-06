package com.jeffthefate;

import com.jeffthefate.utils.EnglishNumberToWords;
import com.jeffthefate.utils.GameComparator;
import com.jeffthefate.utils.Parse;
import com.jeffthefate.utils.TwitterUtil;
import com.jeffthefate.utils.json.Count;
import com.jeffthefate.utils.json.JsonUtil;
import com.jeffthefate.utils.json.Question;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import twitter4j.Status;
import twitter4j.conf.Configuration;

import java.io.File;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Creates a game of trivia on Twitter, played on the account in the given
 * configuration.
 * 
 * @author Jeff Fate
 * 
 */
public class Trivia {

	private static final int WAIT_FOR_QUESTION = (5 * 60 * 1000);
	private static final int WAIT_BETWEEN_QUESTIONS = 90000;
	private static final int WAIT_FOR_ANSWER = (60 * 1000);
	private static final int WAIT_FOR_LIGHTNING = (20 * 1000);
	private static final int WAIT_FOR_RESPONSE = (60 * 1000);
	private static final int NUM_TIPS = 2;

	private static final int PRE_ROUND_TIME = (15 * 1000);
	private static final int LEADERS_EVERY = 10;
	private static final int PLUS_SCORE = 500;
	private static final int BONUS_SCORE = 1000;

	private ArrayList<ArrayList<String>> nameMap;
	private HashMap<String, String> acronymMap;
	private ArrayList<String> replaceList;
    private static List<String> winners = new ArrayList<>(0);
    private Map<Object, Object> scoreMap = new HashMap<>();
    private static int currScore = 0;
	private static String currAnswer;
	private static List<Long> currTwitterStatus = new ArrayList<>(0);

	private String templateFile;
	private String fontFile;
	private String leadersTitle;
	private int mainSize;
	private int dateSize;
	private int limit;
	private int topOffset;
    private int bottomOffset;
    private String triviaScreenshotFilename;

	private TriviaScreenshot screenshot;

	private Configuration twitterConfig;

	private int totalQuestions;
	private int questionCount;
	private int bonusCount;

	private boolean isDev;

	private final Message message = new Message();

	private HashMap<String, Long> responseMap = new HashMap<>();
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

	Map<String, Integer> usersMap = new HashMap<>();

	private static Logger logger = Logger.getLogger(Trivia.class);

    private Parse parse;
    private JsonUtil jsonUtil = JsonUtil.instance();
    private TwitterUtil twitterUtil = TwitterUtil.instance();

	public Trivia(String templateFile, String fontFile, String leadersTitle,
			int mainSize, int dateSize, int limit, int topOffset,
            int bottomOffset, Configuration twitterConfig, int questionCount,
            int bonusCount, ArrayList<ArrayList<String>> nameMap,
			HashMap<String, String> acronymMap, ArrayList<String> replaceList,
			ArrayList<String> tipList, boolean isDev, String preTweet,
			int lightningCount, String triviaScreenshotFilename, Parse parse) {
		this.templateFile = templateFile;
		this.fontFile = fontFile;
		this.leadersTitle = leadersTitle;
		this.mainSize = mainSize;
		this.dateSize = dateSize;
		this.limit = limit;
		this.topOffset = topOffset;
        this.bottomOffset = bottomOffset;
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
        this.triviaScreenshotFilename = triviaScreenshotFilename;
        this.parse = parse;
	}

	private class Message {
		public Message() {}
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

    public Map<Object, Object> getScoreMap() {
        return scoreMap;
    }

    public void setScoreMap(Map<Object, Object> scoreMap) {
        this.scoreMap = scoreMap;
    }

    public boolean isLightning() {
        return isLightning;
    }

    public void setLightning(boolean isLightning) {
        this.isLightning = isLightning;
    }

    public boolean isBonus() {
        return isBonus;
    }

    public void setBonus(boolean isBonus) {
        this.isBonus = isBonus;
    }

    public static List<String> getWinners() {
        return winners;
    }

    public static void setWinners(List<String> winners) {
        Trivia.winners = winners;
    }

    public static int getCurrScore() {
        return currScore;
    }

    public static void setCurrScore(int currScore) {
        Trivia.currScore = currScore;
    }

    /**
     * Begin a trivia game. Clears all information from previous games. Sends
     * tip tweets if there is a pre-show time allotted. Cycles through questions
     * then sends the final scores and checks if there are questions left for
     * the next game.
     *
     * @param preShowTweet  send a tweet to warn players a game is starting soon
     * @param preShowText   what is sent as the warning tweet
     * @param preShowTime   how long to wait before starting the game after
     *                      sending the warning tweet
     */
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

		totalQuestions = 0;
		if (preShowTweet) {
			logger.info("Sending pre-show tweet");
            twitterUtil.updateStatus(twitterConfig, preShowText, null, -1);
			if (tipList.isEmpty()) {
				try {
					Thread.sleep(preShowTime);
				} catch (InterruptedException e) {
                    logger.error("Wait for pre-show interrupted!");
                    e.printStackTrace();
				}
			} else {
				logger.info("Sending pre-show tips");
				Collections.shuffle(tipList);
				int waitForTips = preShowTime / (NUM_TIPS+1);
				for (int i = 0; i < NUM_TIPS; i++) {
					try {
						Thread.sleep(waitForTips);
					} catch (InterruptedException e) {
                        logger.error("Wait for tips interrupted!");
                        e.printStackTrace();
					}
                    twitterUtil.updateStatus(twitterConfig, preTweet +
                            tipList.get(i), null, -1);
				}
				try {
					Thread.sleep(waitForTips);
				} catch (InterruptedException e) {
                    logger.error("Wait for pre-show interrupted!");
                    e.printStackTrace();
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
		boolean success;
		for (int i = 0; i < questionCount; i++) {
			logger.info("QUESTION " + (i + 1));
			synchronized (message) {
				do {
                    try {
                        TimeUnit.SECONDS.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
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
		inTrivia = false;
		// Start resetting the questions early
        int count = getQuestionCount(false, false, 1);
		if (count <= 50 && count >= 0) {
			markAllAsTriviaInBackground();
		}
	}

    /**
     * Set timer for waiting until the question is over.
     *
     * @param newWait how long players have for the current question, in ms
     */
	private void setTimer(final long newWait) {
		logger.info("Setting timer: " + newWait);
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
	}

    /**
     * After a question is done (no more responses accepted), check for correct
     * answers, send update tweets and update the current round.
     */
	private void finishTrivia() {
		logger.info("Finishing question");
		inTrivia = false;
		onCorrectUser(currAnswer, winners);
		sendUpdateTweets();
		isLightning = totalQuestions >= lightningRound
				&& totalQuestions < roundTwo;
		logger.info("In lightning round: " + isLightning);
	}

    /**
     * Send periodic update tweets based on how many questions have been asked
     * so far and if a specific round is starting.
     */
	private void sendUpdateTweets() {
		logger.info("Sending update tweets if applicable");
		if (totalQuestions < questionCount) {
			if (totalQuestions % LEADERS_EVERY == 0) {
				screenshot = createScreenshot(generateLeaderboard());
                screenshot.createScreenshot();
                twitterUtil.updateStatus(twitterConfig, preTweet +
                                "Current Top Scores",
                        new File(screenshot.getOutputFilename()), -1);
			}
			if (totalQuestions == (questionCount - bonusCount)) {
                twitterUtil.updateStatus(twitterConfig, "[BONUS ROUND] " +
                        BONUS_SCORE + "pts added to the value of each question"
                        + " - Points awarded to first correct answer ONLY",
                        null, -1);
				try {
					Thread.sleep(PRE_ROUND_TIME);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			} else if (totalQuestions == lightningRound && lightningCount > 0) {
                twitterUtil.updateStatus(twitterConfig,
                        "[LIGHTNING ROUND] Points awarded to first correct "
                                + "answer only", null, -1);
				try {
					Thread.sleep(PRE_ROUND_TIME);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			} else if (totalQuestions == roundTwo) {
				twitterUtil.updateStatus(twitterConfig, "[Starting ROUND 2] " +
                                PLUS_SCORE + "pts added to the value of each question",
                        null, -1);
				try {
					Thread.sleep(PRE_ROUND_TIME);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}
		}
	}

    /**
     * Take the current tweet (response) and process it. Response and answer are
     * massaged, checked against each other and timers updated as necessary.
     *
     * @param status current tweet received
     */
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
		if ((answer.startsWith("19") || answer.startsWith("20")) &&
				answer.length() >= 4 &&
				(!response.startsWith("19") || !response.startsWith("20"))) {
			response = answer.substring(0, 2) + response;
		}
		else if ((response.startsWith("19") || response.startsWith("20")) &&
				response.length() >= 4 &&
				(!answer.startsWith("19") || !answer.startsWith("20"))) {
			answer = response.substring(0, 2) + answer;
		}
		boolean isCorrect = checkAnswer(answer, response);
		if (!isCorrect) {
			logger.info("Not correct, rechecking");
			isCorrect = checkAnswer(answer, reCheck(answer, response));
		}
        if (isCorrect) {
            updateWinners(userName);
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
				setTimer(WAIT_FOR_ANSWER);
                inTrivia = false;
			}
			return;
		}
		if (isCorrect) {
			logger.info("CORRECT!");
			setTimer(WAIT_FOR_ANSWER);
		}
	}

    /**
     * Massage the current response text, removing unwanted special characters
     * so it is easier to match the answer.
     *
     * @param text user response text
     * @return     massaged user response text
     */
	public String massageResponse(String text) {
		return text.toLowerCase(Locale.getDefault())
				.replaceFirst(
						"(?<=^|(?<=[^a-zA-Z0-9-_\\.]))@([A-Za-z]+[A-Za-z0-9]+)",
						"").replaceAll("[.,'`|\":;/?\\-!@#]", "").trim();
	}

    /**
     * Massage the answer text, removing unwanted special characters so it is
     * easier to match the responses.
     *
     * @param text correct answer text
     * @return     massaged correct answer text
     */
	public String massageAnswer(String text) {
		return text.toLowerCase(Locale.getDefault())
				.replaceAll("[.,'`\":;/?\\-!@#]", "").trim();
	}

    /**
     * Tweet the correct answer and the list of players with the correct answer.
     * If no correct responses, only the correct answer is tweeted.
     *
     * @param correctAnswer correct answer string
     * @param screenNames   list of players' screen names; who got it correct
     */
	public void onCorrectUser(String correctAnswer, List<String> screenNames) {
		logger.info("Correct answer: " + correctAnswer);
		logger.info("Winners: " + screenNames);
		StringBuilder sb = new StringBuilder();
		sb.append(preTweet);
		sb.append("Answer: \"");
		sb.append(correctAnswer);
		sb.append("\"");
		String screenName;
		for (int i = 0; i < screenNames.size(); i++) {
			screenName = screenNames.get(i);
			if (sb.length() + 11 + screenName.length()
					+ Integer.toString((Integer)scoreMap.get(screenName))
                        .length() > 140)
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
		twitterUtil.updateStatus(twitterConfig, sb.toString(), null,
                currTwitterStatus != null && !currTwitterStatus.isEmpty() ?
                        currTwitterStatus.get(0) : -1);
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
            logger.error("Sleeping between questions interrupted!");
            e.printStackTrace();
		}
	}

    /**
     * Create final leaderboard image and tweet it.
     */
	private void onFinalLeaders() {
		logger.info("Posting final leaderboard");
		TreeMap<Object, Object> sortedMap = generateLeaderboard();
		String winner = "Final Top 10";
		int firstScore = -1;
		String firstPlayer = null;
		int playerNum = 0;
		for (Entry<Object, Object> player : sortedMap.entrySet()) {
			playerNum++;
			if (playerNum <= 1) {
				firstScore = (Integer) player.getValue();
				firstPlayer = (String) player.getKey();
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
		screenshot = createScreenshot(sortedMap);
        screenshot.createScreenshot();
		twitterUtil.updateStatus(twitterConfig, preTweet + winner,
                new File(screenshot.getOutputFilename()), -1);
	}

    /**
     * Create a trivia scores screenshot with configuration supplied to this
     * trivia object.
     *
     * @param sortedMap leaderboard of players with correct responses
     * @return          TriviaScreenshot object created with supplied config
     */
    public TriviaScreenshot createScreenshot(
            TreeMap<Object, Object> sortedMap) {
        return new TriviaScreenshot(templateFile, fontFile, leadersTitle,
                sortedMap, mainSize, dateSize, limit, topOffset, bottomOffset,
                triviaScreenshotFilename);
    }

    /**
     * Create sorted map of user-score pairings.
     *
     * @return TreeMap with each user and their score for those who answered at
     *         least one answer correctly
     */
	public TreeMap<Object, Object> generateLeaderboard() {
		logger.info("Creating leaderboard");
		GameComparator scoreComparator = new GameComparator(scoreMap);
		TreeMap<Object, Object> sortedMap = new TreeMap<>(
				scoreComparator);
		sortedMap.putAll(scoreMap);
		return sortedMap;
	}

    /**
     * Compare answer string and response string to see if they are close
     * enough to be matching. Adds the user and score to list for record
     * keeping.
     *
     * @param answer        question's correct answer, as given
     * @param response      string submitted by user as the answer
     * @return              true if response is close enough to the answer to
     *                      be correct
     */
	public boolean checkAnswer(String answer, String response) {
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
		int diffCount = StringUtils
				.getLevenshteinDistance(response, answer, buffer);
		boolean isCorrect = false;
		logger.info("Difference count: " + diffCount);
		switch (diffCount) {
		case -1:
			logger.info("Didn't match closely enough");
			break;
		default:
			isCorrect = true;
			break;
		}
		return isCorrect;
	}

    public void updateWinners(String screenName) {
        if ((isLightning && winners.isEmpty())
                || (isBonus && winners.isEmpty())
                || (!winners.contains(screenName) && winners.size() < 3)) {
            Integer userScore = (Integer) scoreMap.get(screenName);
            int pointsEarned = currScore
                    - (winners.size() * (currScore / 4));
            winners.add(screenName);
            if (userScore == null) {
                scoreMap.put(screenName, pointsEarned);
            }
            else {
                scoreMap.put(screenName, userScore + pointsEarned);
            }
            logger.info("Adding " + screenName + " to winners");
        }
    }

    /**
     * Use number to word conversions and comparisons to equal answers to
     * convert answer and response to a more favorable string.
     *
     * @param answer    the correct answer to massage
     * @param response  the response string to massage
     * @return          either the answer or response converted to check again
     */
	public String reCheck(String answer, String response) {
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

    /**
     * Find a question to ask, ask it and wait for responses.
     *
     * @return true if everything is successful
     */
	public boolean askQuestion() {
		logger.info("Asking question");
		responseMap.clear();
		responseMap = new HashMap<>();
		Question question;
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
		int count = getQuestionCount(prioritize, isLightning, 0);
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
				markAllAsTrivia(1, true);
				return false;
			} else {
				return false;
			}
		}
		int skip = ((int) (count * Math.random()));
		List<Question> questionList = getQuestions(prioritize,
                isLightning, false, 1, skip, 0);
		if (!questionList.isEmpty()) {
			question = questionList.get(0);
		} else {
			return false;
		}
		if (question == null || question.getObjectId() == null) {
			return false;
		}
		sb = new StringBuilder();
		sb.append(preTweet);
		sb.append("[");
		sb.append(totalQuestions + 1);
		sb.append("/");
		sb.append(questionCount);
		sb.append("] [");
		currScore = question.getScore();
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
		sb.append(question.getQuestion());
		logger.info("Total question score: " + currScore);
		ArrayList<String> tweetList = new ArrayList<>(0);
		int index;
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
		currTwitterStatus = new ArrayList<>(0);
		for (String tweet : tweetList) {
			if (currTwitterStatus.isEmpty()) {
				status = twitterUtil.updateStatus(twitterConfig, tweet, null,
                        -1);
			} else {
				status = twitterUtil.updateStatus(twitterConfig, tweet, null,
                        currTwitterStatus.get(0));
			}
			if (status != null) {
				if (!isDev) {
                    parse.markAsTrivia(question.getObjectId(), 0);
				}
				currTwitterStatus.add(status.getId());
			}
			else {
				totalQuestions--;
				return false;
			}
		}
        winners.clear();
        winners = new ArrayList<>(0);
        currAnswer = question.getAnswer();
        setTimer(WAIT_FOR_QUESTION);
        inTrivia = true;
		return true;
	}

    /**
     * Get list of questions, filtered by given parameters.
     *
     * @param prioritize    only those that are new or greater trivia value
     *                      than 1
     * @param lightning     only those that are in Lyrics or Scramble
     *                      categories
     * @param reset         only already asked questions
     * @param limit         how many results to fetch
     * @param skip          how far in the results to start looking
     * @param level         if greater than 0, only fetch greater than 0 level
     * @return              list of questions given the parameters
     */
	public List<Question> getQuestions(boolean prioritize, boolean lightning,
            boolean reset, int limit, int skip, int level) {
		logger.info("Getting question (prioritize: " + prioritize
				+ ", lightning: " + lightning + ", reset: " + reset
				+ ", limit: " + limit + ", skip: " + skip);
        String query = "?skip=" + skip + "&limit=" + limit + "&order=updatedAt";
        if (lightning) {
            // Get questions only from Lyrics and Scramble categories
            logger.info("Fetching lyrics or scramble question");
            query += ("&where%3D%7B%22category%22%3A%7B%22%24in%22%3A%5B%22"
                    + "Lyrics%22%2C%22Scramble%22%5D%7D%7D");
        } else if (reset) {
            logger.info("Fetching asked question");
            query += "&where%3D%7B%22trivia%22%3A0%7D";
        } else if (level > 0) {
            logger.info("Fetching trivia greater than 0");
            query += ("&where%3D%7B%22trivia%22%3A%7B%22%24gte%22%3A" + level +
                    "%7D%7D");
        } else {
            if (!prioritize) {
                // Choose from everything but those that are asked
                logger.info("Fetching any unasked question");
                query += ("&where%3D%7B%22%24or%22%3A%5B%7B%22trivia%22%3A%7B" +
                        "%22%24exists%22%3Afalse%7D%7D%2C%7B%22trivia%22%3A" +
                        "%7B%22%24ne%22%3A0%7D%7D%5D%7D");
            } else {
                // Choose from only new and prioritized
                logger.info("Fetching new or prioritized question");
                query += ("&where%3D%7B%22%24or%22%3A%5B%7B%22trivia%22%3A%7B" +
                        "%22%24nin%22%3A%5B0%2C1%5D%7D%7D%2C%7B%22trivia%22" +
                        "%3A%7B%22%24exists%22%3Afalse%7D%7D%5D%7D");
            }
        }
        String responseString = parse.get("Question", query);
        if (responseString != null) {
            return jsonUtil.getQuestionResults(responseString)
                    .getResults();
        }
        return new ArrayList<>(0);
	}

    /**
     * Get the number of questions, filtered by given parameters.
     *
     * @param prioritize    count only questions that are brand new and
     *                      those that have a trivia value greater than one
     * @param lightning     count only questions in Lyrics and Scramble
     *                      categories
     * @param level         count only questions greater or equal to this level
     * @return              number of questions, filtered by parameters
     */
	private int getQuestionCount(boolean prioritize, boolean lightning,
			int level) {
		String query = "?count=1&limit=0";
		if (lightning) {
			// Get questions only from Lyrics and Scramble categories
			logger.info("Fetching lyrics or scramble count");
			query += ("&where%3D%7B%22category%22%3A%7B%22%24in%22%3A%5B%22"
					+ "Lyrics%22%2C%22Scramble%22%5D%7D%7D");
		} else if (level > 0) {
			logger.info("Fetching trivia greater than 0");
			query += ("&where%3D%7B%22trivia%22%3A%7B%22%24gte%22%3A" + level +
					"%7D%7D");
		} else {
			if (!prioritize) {
				// Choose from everything but those that are asked
				logger.info("Fetching any unasked count");
				query += ("&where%3D%7B%22%24or%22%3A%5B%7B%22trivia%22%3A%7B" +
                        "%22%24exists%22%3Afalse%7D%7D%2C%7B%22trivia%22%3A" +
                        "%7B%22%24ne%22%3A0%7D%7D%5D%7D");
			} else {
				// Choose from only new and prioritized
				logger.info("Fetching new or prioritized count");
				query += ("&where%3D%7B%22%24or%22%3A%5B%7B%22trivia%22%3A%7B" +
                        "%22%24nin%22%3A%5B0%2C1%5D%7D%7D%2C%7B%22trivia%22" +
                        "%3A%7B%22%24exists%22%3Afalse%7D%7D%5D%7D");
			}
		}
		String responseString = parse.get("Question", query);
        if (responseString != null) {
            Count count = jsonUtil.getCount(responseString);
            if (count != null) {
                return count.getCount();
            }
        }
        return -1;
	}

    /**
     * Mark all questions to the given level. Also, if specified, mark all
     * questions up one level.
     *
     * @param level     the new level value for questions if not updating zero
     *                  level questions
     * @param resetZero if true, update the zero level questions to the given
     *                  level value. False gets all one level questions and
     *                  increments the level values for all
     */
	private void markAllAsTrivia(int level, boolean resetZero) {
		logger.info("Setting all questions to " + level);
		List<Question> questionList;
		do {
			questionList = getQuestions(false, false, true, 1000, 0,
                    resetZero ? 0 : 1);
			if (!questionList.isEmpty()) {
				for (Question question : questionList) {
					if (!resetZero) {
                        try {
                            level = question.getTrivia() + 1;
                        } catch (NumberFormatException e) {
                            continue;
                        }
                    }
                    parse.markAsTrivia(question.getObjectId(), level);
				}
			}
		} while (!questionList.isEmpty());
	}

    /**
     * Set all level 0 questions to 1 and all other up one level. Executed
     * on a background thread.
     */
    private void markAllAsTriviaInBackground() {
        Thread markThread = new Thread() {
            public void run() {
                // Mark everything 1 and greater up one level
                markAllAsTrivia(2, false);
                // Mark all 0s to 1s
                markAllAsTrivia(1, true);
            }
        };
        markThread.start();
    }

}
