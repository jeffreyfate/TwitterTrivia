package com.jeffthefate;

import junit.framework.TestCase;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;

// TODO Create REAL unit tests

public class TriviaTest extends TestCase {
	
	private static final String DEV_KEY = "BXx60ptC4JAMBQLQ965H3g";
	private static final String DEV_SECRET = "0ivTqB1HKqQ6t7HQhIl0tTUNk8uRnv1nhDqyFXBw";
	private static final String DEV_ACCESS_TOKEN = "1265342035-6mYSoxlw8NuZSdWX0AS6cpIu3We2CbCev6rbKUQ";
	private static final String DEV_ACCESS_SECRET = "XqxxE4qLUK3wJ4LHlIbcSP1m6G4spZVmCDdu5RLuU";

    private Trivia trivia;

    public void setUp() throws Exception {
        super.setUp();
        setupAnswerMap();
        trivia = new Trivia(
                new File("src/test/resources/setlist.jpg").getAbsolutePath(),
                new File("src/test/resources/roboto.ttf").getAbsolutePath(),
                "Top Scores", 60, 30, 10, 200, 40, setupTweet(), 0, 0,
                nameMap, acronymMap, replaceList, tipList, true,
                "Game starts on @dmbtrivia2 in 15 minutes", 0,
                "/home/TEMP/scores",
                "6pJz1oVHAwZ7tfOuvHfQCRz6AVKZzg1itFVfzx2q",
                "uNZMDvDSahtRxZVRwpUVwzAG9JdLzx4cbYnhYPi7");
    }

    public void testMassageResponse() {
        String massaged = trivia.massageResponse("#I'll Back You Up||");
        assertEquals("Massaged responses aren't equal!", "ill back you up",
                massaged);
    }
	/**
	 * checkAnswer correctly ignores the incoming strings
	 */
	public void testCheckAnswer() {
		setupAnswerMap();
		/*
		Trivia trivia = new Trivia("", "", "", 0, 0, 0,
				setupTweet(), 0, nameMap, acronymMap,
				true, "@dmbtrivia", this);
		try {
			Method method1 = Trivia.class.getDeclaredMethod("checkAnswer",
					String.class, String.class, String.class);
			method1.setAccessible(true);
			Method method2 = Trivia.class.getDeclaredMethod("reCheck",
					String.class, String.class);
			method2.setAccessible(true);
			Boolean output = (Boolean) method1.invoke(trivia, "crash",
					"crash into me", "@jeffthefate");
			if (!output)
				output = (Boolean) method1.invoke(trivia, "crash",
						(String) method2.invoke(trivia, "crash", "crash into me"), "@jeffthefate");
			System.out.println("result: " + output);
			trivia.resetCheckedAcronymMap();
			trivia.resetCheckedNameMap();
			MapIterator it = nameMap.mapIterator();
			Object key;
			Object value;
			while (it.hasNext()) {
				key = it.next();
				value = it.getValue();
				output = (Boolean) method.invoke(trivia, key,
						value, "@jeffthefate");
				System.out.println("result: " + output);
				trivia.resetCheckedAcronymMap();
				trivia.resetCheckedNameMap();
				output = (Boolean) method.invoke(trivia, value,
						key, "@jeffthefate");
				System.out.println("result: " + output);
				trivia.resetCheckedAcronymMap();
				trivia.resetCheckedNameMap();
			}
			it = acronymMap.mapIterator();
			while (it.hasNext()) {
				key = it.next();
				value = it.getValue();
				output = (Boolean) method.invoke(trivia, value,
						key, "@jeffthefate");
				System.out.println("result: " + output);
				trivia.resetCheckedAcronymMap();
				trivia.resetCheckedNameMap();
				output = (Boolean) method.invoke(trivia, key,
						value, "@jeffthefate");
				System.out.println("result: " + output);
				trivia.resetCheckedAcronymMap();
				trivia.resetCheckedNameMap();
			}
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
	}
	
	private ArrayList<ArrayList<String>> nameMap = new ArrayList<ArrayList<String>>(0);
	private HashMap<String, String> acronymMap = new HashMap<String, String>();
	private ArrayList<String> replaceList = new ArrayList<String>(0);
	private ArrayList<String> tipList = new ArrayList<String>(0);
	
    private void setupAnswerMap() {
		ArrayList<String> tempList = new ArrayList<String>(0);
		tempList.add("dave");
		tempList.add("dave matthews");
		nameMap.add(tempList);
		tempList = new ArrayList<String>(0);
		tempList.add("boyd");
		tempList.add("boyd tinsley");
		nameMap.add(tempList);
		tempList = new ArrayList<String>(0);
		tempList.add("stefan");
		tempList.add("stefan lessard");
		nameMap.add(tempList);
		tempList = new ArrayList<String>(0);
		tempList.add("carter");
		tempList.add("carter beauford");
		nameMap.add(tempList);
		tempList = new ArrayList<String>(0);
		tempList.add("leroi");
		tempList.add("leroi moore");
		tempList.add("roi");
		nameMap.add(tempList);
		tempList = new ArrayList<String>(0);
		tempList.add("butch");
		tempList.add("butch taylor");
		nameMap.add(tempList);
		tempList = new ArrayList<String>(0);
		tempList.add("tim");
		tempList.add("tim reynolds");
		nameMap.add(tempList);
		tempList = new ArrayList<String>(0);
		tempList.add("jeff");
		tempList.add("jeff coffin");
		tempList.add("coffin");
		nameMap.add(tempList);
		tempList = new ArrayList<String>(0);
		tempList.add("rashawn");
		tempList.add("rashawn ross");
		nameMap.add(tempList);
		tempList = new ArrayList<String>(0);
		tempList.add("lillywhite");
		tempList.add("steve lillywhite");
		nameMap.add(tempList);
		tempList = new ArrayList<String>(0);
		tempList.add("lawlor");
		tempList.add("joe lawlor");
		nameMap.add(tempList);
		tempList = new ArrayList<String>(0);
		tempList.add("fenton");
		tempList.add("fenton williams");
		nameMap.add(tempList);
		tempList = new ArrayList<String>(0);
		tempList.add("peter");
		tempList.add("peter griesar");
		nameMap.add(tempList);
		tempList = new ArrayList<String>(0);
		tempList.add("sax");
		tempList.add("saxophone");
		nameMap.add(tempList);
		tempList = new ArrayList<String>(0);
		tempList.add("alpine");
		tempList.add("alpine valley");
		nameMap.add(tempList);
		tempList = new ArrayList<String>(0);
		tempList.add("big whiskey and the groogrux king");
		tempList.add("big whiskey");
		tempList.add("big whiskey & the groogrux king");
		tempList.add("bwggk");
		nameMap.add(tempList);
		tempList = new ArrayList<String>(0);
		tempList.add("stay");
		tempList.add("stay (wasting time)");
		nameMap.add(tempList);
		tempList = new ArrayList<String>(0);
		tempList.add("you and me");
		tempList.add("you & me");
		nameMap.add(tempList);
		tempList = new ArrayList<String>(0);
		tempList.add("rhyme and reason");
		tempList.add("rhyme & reason");
		nameMap.add(tempList);
		tempList = new ArrayList<String>(0);
		tempList.add("funny the way it is");
		tempList.add("ftwii");
		tempList.add("funny");
		nameMap.add(tempList);
		tempList = new ArrayList<String>(0);
		tempList.add("sweet up & down");
		tempList.add("sweet up and down");
		nameMap.add(tempList);
		tempList = new ArrayList<String>(0);
		tempList.add("billies");
		tempList.add("tripping billies");
		nameMap.add(tempList);
		tempList = new ArrayList<String>(0);
		tempList.add("bela fleck and the flecktones");
		tempList.add("bela fleck & the flecktones");
		tempList.add("the flecktones");
		nameMap.add(tempList);
		tempList = new ArrayList<String>(0);
		tempList.add("any noise");
		tempList.add("any noise/anti-noise");
		tempList.add("any noise anti-noise");
		tempList.add("any noise anti noise");
		tempList.add("any noise antinoise");
		tempList.add("anynoise antinoise");
		nameMap.add(tempList);
		acronymMap.put("btcs", "before these crowded streets");
		acronymMap.put("uttad", "under the table and dreaming");
		acronymMap.put("watchtower", "all along the watchtower");
		acronymMap.put("hunger", "hunger for the great light");
		acronymMap.put("crash", "crash into me");
		acronymMap.put("nancies", "dancing nancies");
		acronymMap.put("msg", "madison square garden");
		acronymMap.put("wpb", "west palm beach");
		acronymMap.put("ddtw", "dont drink the water");
		replaceList.add("the ");
		replaceList.add("his ");
		replaceList.add("her ");
	}
	
	private static Configuration setupTweet() {
    	ConfigurationBuilder cb = new ConfigurationBuilder();
		cb.setDebugEnabled(true)
		  .setOAuthConsumerKey(DEV_KEY)
		  .setOAuthConsumerSecret(DEV_SECRET)
		  .setOAuthAccessToken(DEV_ACCESS_TOKEN)
		  .setOAuthAccessTokenSecret(DEV_ACCESS_SECRET);
		return cb.build();
    }

    private TreeMap<String, Integer> createUserMap() {
        TreeMap<String, Integer> sortedMap = new TreeMap<String, Integer>();
        for (int i = 1; i <= 10; i++) {
            sortedMap.put("user" + i, 43);
        }
        return sortedMap;
    }
}
