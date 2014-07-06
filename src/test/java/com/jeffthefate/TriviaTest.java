package com.jeffthefate;

import com.jeffthefate.utils.CredentialUtil;
import com.jeffthefate.utils.GameUtil;
import com.jeffthefate.utils.Parse;
import com.jeffthefate.utils.json.Question;
import junit.framework.TestCase;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TriviaTest extends TestCase {

    private Trivia trivia;
    private GameUtil gameUtil;

    public void setUp() throws Exception {
        super.setUp();
        CredentialUtil credentialUtil = CredentialUtil.instance();
        gameUtil = GameUtil.instance();
        Parse parse = credentialUtil.getCredentialedParse(true,
                "D:\\parseCreds");
        trivia = new Trivia(
                new File("src/test/resources/setlist.jpg").getAbsolutePath(),
                new File("src/test/resources/roboto.ttf").getAbsolutePath(),
                "Top Scores", 45, 22, 10, 200, 100,
                credentialUtil.getCredentialedTwitter(parse, true), 0, 0,
                gameUtil.setupAnswerList(), gameUtil.createAcronymMap(),
                gameUtil.createReplaceList(), gameUtil.createTipList(),
                true, "Game starts on @dmbtrivia2 in 15 minutes", 0,
                "/home/TEMP/scores", parse, "D:\\triviaScores.ser");
    }

    public void testMassageResponse() {
        String massaged = trivia.massageResponse("#I'll Back You Up||");
        assertEquals("Massaged responses aren't equal!", "ill back you up",
                massaged);
    }

    public void testMassageAnswer() {
        String massaged = trivia.massageAnswer("Don't Drink the Water");
        assertEquals("Massaged responses aren't equal!", "dont drink the water",
                massaged);
        massaged = trivia.massageAnswer("#41");
        assertEquals("Massaged responses aren't equal!", "41", massaged);
    }

    public void testGenerateLeaderboard() {
        HashMap<Object, Object> scoreMap = new HashMap<>();
        scoreMap.put("testUser01", 50000000);
        scoreMap.put("testUser02", 10);
        scoreMap.put("testUser03", -1);
        scoreMap.put("testUser04", 54321);
        scoreMap.put("testUser05", 12390984);
        scoreMap.put("testUser06", 7654567);
        scoreMap.put("testUser07", 7876545);
        scoreMap.put("testUser08", 60000000);
        scoreMap.put("testUser09", 9493);
        scoreMap.put("testUser10", 23095);
        scoreMap.put("testUser11", 9593);
        scoreMap.put("testUser12", 12341);
        scoreMap.put("testUser13", 5096705);
        scoreMap.put("testUser14", 5848);
        scoreMap.put("testUser15", 1);
        trivia.setScoreMap(scoreMap);
        Map<Object, Object> leaderboard = trivia.generateLeaderboard();
        Integer last = 999999999;
        for (Map.Entry<Object, Object> leader : leaderboard.entrySet()) {
            assertTrue("Sort not correct!", last > (Integer) leader.getValue());
            last = (Integer) leader.getValue();
        }
    }
	/**
	 * checkAnswer correctly ignores the incoming strings
	 */
	public void testCheckAnswer() {
        assertTrue("Response should match answer!", trivia.checkAnswer("41",
                "41"));
        assertTrue("Response should match answer!", trivia.checkAnswer(
                "dont drink teh water", "dont drink the water"));
        assertTrue("Response should match answer!", trivia.checkAnswer(
                "anne mathews", "anne matthews"));
        assertFalse("Null response or answer should not match!",
                trivia.checkAnswer(null, "test"));
        assertFalse("Null response or answer should not match!",
                trivia.checkAnswer("test", null));
	}

    public void testUpdateWinnersLightning() {
        trivia.setCurrScore(44);
        trivia.setLightning(true);
        trivia.setWinners(new ArrayList<String>(0));
        trivia.updateWinners("jeffthefate");
        List<String> expected = new ArrayList<>(0);
        expected.add("jeffthefate");
        assertEquals("Winners lists are inconsistent!", expected,
                trivia.getWinners());
        assertEquals("Score is incorrect!", 44,
                ((Integer) trivia.getScoreMap().get("jeffthefate")).intValue());
    }

    public void testUpdateWinnersBonus() {
        trivia.setCurrScore(44);
        trivia.setBonus(true);
        trivia.setWinners(new ArrayList<String>(0));
        trivia.updateWinners("jeffthefate");
        List<String> expected = new ArrayList<>(0);
        expected.add("jeffthefate");
        assertEquals("Winners lists are inconsistent!", expected,
                trivia.getWinners());
        assertEquals("Score is incorrect!", 44,
                ((Integer) trivia.getScoreMap().get("jeffthefate")).intValue());
    }

    public void testUpdateWinners() {
        trivia.setCurrScore(44);
        trivia.setWinners(new ArrayList<String>(0));
        trivia.updateWinners("jeffthefate");
        assertEquals("Winners should have one user!", 1,
                trivia.getWinners().size());
        assertEquals("Score is incorrect!", 44,
                ((Integer) trivia.getScoreMap().get("jeffthefate")).intValue());
        trivia.updateWinners("jeffthefate");
        assertEquals("Winners should have one user!", 1,
                trivia.getWinners().size());
        trivia.updateWinners("copperpot5");
        assertEquals("Winners should have two users!", 2,
                trivia.getWinners().size());
        trivia.updateWinners("dmbtrivia");
        assertEquals("Winners should have three users!", 3,
                trivia.getWinners().size());
        trivia.updateWinners("dmbtrivia2");
        assertEquals("Winners should have three users!", 3,
                trivia.getWinners().size());
        trivia.getScoreMap().put("jeffthefate", 400);
        trivia.getWinners().clear();
        trivia.setCurrScore(500);
        trivia.updateWinners("jeffthefate");
        assertEquals("Score is incorrect!", 900,
                ((Integer) trivia.getScoreMap().get("jeffthefate")).intValue());
    }

    public void testReCheck() {
        String massaged = trivia.reCheck("tripping billies", "billies");
        assertEquals("Answer and response don't match!", "tripping billies",
                massaged);
        massaged = trivia.reCheck("10", "ten");
        assertEquals("Answer and response don't match!", "ten",
                massaged);
        massaged = trivia.reCheck("dont drink the water", "ddtw");
        assertEquals("Answer and response don't match!", "dont drink the water",
                massaged);
        massaged = trivia.reCheck("dont drink the water", "spoon");
        assertNull("Shouldn't have matched!", massaged);
    }

    public void testGetQuestions() {
        List<Question> questions = trivia.getQuestions(true, true, true, 1,
                1, 1);
        assertEquals("Not expected number of questions!", 1, questions.size());
        questions = trivia.getQuestions(false, true, true, 1, 1, 1);
        assertEquals("Not expected number of questions!", 1, questions.size());
        questions = trivia.getQuestions(false, false, true, 1, 1, 1);
        assertEquals("Not expected number of questions!", 0, questions.size());
        questions = trivia.getQuestions(false, false, false, 1, 1, 1);
        assertEquals("Not expected number of questions!", 0, questions.size());
        questions = trivia.getQuestions(true, false, true, 1, 1, 1);
        assertEquals("Not expected number of questions!", 0, questions.size());
        questions = trivia.getQuestions(true, false, false, 1, 1, 1);
        assertEquals("Not expected number of questions!", 0, questions.size());
        questions = trivia.getQuestions(true, true, false, 1, 1, 1);
        assertEquals("Not expected number of questions!", 1, questions.size());
        questions = trivia.getQuestions(false, true, false, 1, 1, 1);
        assertEquals("Not expected number of questions!", 1, questions.size());
    }

}
