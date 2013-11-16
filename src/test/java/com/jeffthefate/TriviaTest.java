package com.jeffthefate;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Map.Entry;

import org.apache.commons.collections.MapIterator;
import org.apache.commons.collections.bidimap.TreeBidiMap;

import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

// TODO Create REAL unit tests

/**
 * Unit test for simple App.
 */
public class TriviaTest extends TestCase {
	
	private static final String DEV_KEY = "BXx60ptC4JAMBQLQ965H3g";
	private static final String DEV_SECRET = "0ivTqB1HKqQ6t7HQhIl0tTUNk8uRnv1nhDqyFXBw";
	private static final String DEV_ACCESS_TOKEN = "1265342035-6mYSoxlw8NuZSdWX0AS6cpIu3We2CbCev6rbKUQ";
	private static final String DEV_ACCESS_SECRET = "XqxxE4qLUK3wJ4LHlIbcSP1m6G4spZVmCDdu5RLuU";
	
	/**
	 * Create the test case
	 *
	 * @param testName name of the test case
	 */
	public TriviaTest( String testName ) {
		super( testName );
	}

	/**
	 * @return the suite of tests being tested
	 */
	public static Test suite() {
		return new TestSuite( TriviaTest.class );
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
	
	private static ArrayList<ArrayList<String>> nameMap = new ArrayList<ArrayList<String>>(0);
    private static ArrayList<ArrayList<String>> acronymMap = new ArrayList<ArrayList<String>>(0);
	
    private static void setupAnswerMap() {
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
    	tempList.add("sax");
    	tempList.add("saxophone");
    	nameMap.add(tempList);
    	tempList = new ArrayList<String>(0);
    	tempList.add("alpine");
    	tempList.add("alpine valley");
    	nameMap.add(tempList);
    	/*
    	nameMap.put("dave", "dave matthews");
    	nameMap.put("boyd", "boyd tinsley");
    	nameMap.put("stefan", "stefan lessard");
    	nameMap.put("carter", "carter beauford");
    	nameMap.put("leroi moore", "leroi");
    	nameMap.put("roi", "leroi moore");
    	nameMap.put("leroi", "roi");
    	nameMap.put("butch", "butch taylor");
    	nameMap.put("tim", "tim reynolds");
    	nameMap.put("jeff", "jeff coffin");
    	nameMap.put("jeff coffin", "coffin");
    	nameMap.put("rashawn", "rashawn ross");
    	nameMap.put("lillywhite", "steve lillywhite");
    	acronymMap.put("btcs", "before these crowded streets");
    	acronymMap.put("uttad", "under the table and dreaming");
    	nameMap.put("sax", "saxophone");
    	acronymMap.put("watchtower", "all along the watchtower");
    	acronymMap.put("hunger", "hunger for the great light");
    	acronymMap.put("crash", "crash into me");
    	acronymMap.put("nancies", "dancing nancies");
    	acronymMap.put("big whiskey", "big whiskey and the groogrux king");
    	acronymMap.put("msg", "madison square garden");
    	nameMap.put("alpine", "alpine valley");
    	acronymMap.put("wpb", "west palm beach");
    	*/
    	// TODO Add pronouns
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

	public void onNextQuestion() {
		// TODO Auto-generated method stub
		
	}
}
