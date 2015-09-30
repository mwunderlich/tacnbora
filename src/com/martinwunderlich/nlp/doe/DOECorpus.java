package com.martinwunderlich.nlp.doe;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.io.FileUtils;

public class DOECorpus implements java.io.Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -4650533752463777081L;

	private HashMap<String, DOECorpusDocument> docs = new HashMap<String, DOECorpusDocument>();
	
	private int typeCount = -1;
	private int tokenCount = -1;
	private int sentenceCount = -1;
	
	private int maxSenLength = -1;
	private int minSenLength = 1000;
	private int totalSentenceLength = 0;
	
	HashMap<String, Integer> vocabulary = new HashMap<String, Integer>();

	
	private List<String> tokens = new ArrayList<String>();

	public void addDocument(DOECorpusDocument doc) {
		this.docs.put(doc.getShortTitle(), doc);
	}

	public void splitIntoSentences() {
		this.sentenceCount = 0;
		
		for(DOECorpusDocument doc : this.docs.values()) {
			doc.splitLinesIntoSentences();
			this.sentenceCount += doc.getSentenceCount();
		
		}
	}

	public int getSentenceCount() {
		return this.sentenceCount;
	}

	public int getMaxSenLength() {
		return this.maxSenLength;
	}

	public int getMinSenLength() {
		return this.minSenLength;
	}
	
	public double getAvgSenLength() {
		return this.totalSentenceLength / 1.0*this.sentenceCount;
	}
	
	public void tokenize() {
		this.tokenCount = 0;
		this.typeCount = 0;
		int tokens = 0;
		int types = 0;
		
		for(DOECorpusDocument doc : this.docs.values()) {
			List<String> tokenList = doc.tokenizeSentences();
			
			addToVocabulary(tokenList);
			this.tokens.addAll(tokenList);
			
			tokens = doc.getTokenCount();
			types = doc.getTypeCount();
			
			this.tokenCount += tokens;
			this.typeCount += types;
			
			if(doc.getMaxSentenceLength() > this.maxSenLength)
				this.maxSenLength = doc.getMaxSentenceLength();
			
			if( doc.getMinSentenceLength() > 0)
				if(doc.getMinSentenceLength() < this.minSenLength)
					this.minSenLength = doc.getMinSentenceLength();
			
			this.totalSentenceLength += doc.getTotalSentenceLength();
		}
	}
	
	private void addToVocabulary(List<String> tokenList) {
		for(String token : tokenList) {
			if( ! this.vocabulary.containsKey(token) ) {
				this.vocabulary.put(token, 1);
			}
			else {
				int newCount = this.vocabulary.get(token) + 1;
				this.vocabulary.put(token, newCount);
			}
		}
	}

	public void findTypesWithMinOccurrence(int c) {
		for(String type : this.vocabulary.keySet()) {
			int count = this.vocabulary.get(type);
			if(count >= c)
				System.out.println("Type " + type + " occurs " + count + " times.");
		}
	}
	
	public void findTypesWithMinMaxOccurrenceAndMinLength(int min, int max, int minLen) {
		String startLetters = "aæbcdefg";
		
		
		for(String type : this.vocabulary.keySet()) {
			int count = this.vocabulary.get(type);
			if(count >= min && count <= max && type.length() >= minLen)
				if( startLetters.contains(type.substring(0, 1).toLowerCase()) )
					System.out.println("Type " + type + " occurs " + count + " times.");
		}
	}

	public int getTokenCount() {
		return this.tokenCount;
	}
	
	public int getTypeCount() {
		return this.vocabulary.size();
	}

	public void saveToFile(String fileName) {
		try
	      {
			if(fileName.startsWith("file:"))
				fileName = fileName.replace("file:", "");
			 File file = new File(fileName);
			 System.out.println("Serializing corpus to file " + fileName);
			 FileOutputStream fileOut = FileUtils.openOutputStream(file);
	         ObjectOutputStream out = new ObjectOutputStream(fileOut);
	         out.writeObject(this);
	         out.close();
	         fileOut.close();
	         System.out.printf("Serialized corpus has been saved in " + fileName);
	      } catch(IOException i) {
	          i.printStackTrace();
	      }
	}
	
	public static DOECorpus loadFromFile(String fileName) {
		DOECorpus corpus = null;
		if(fileName.startsWith("file:"))
			fileName = fileName.replace("file:", "");
		System.out.printf("De-serializing corpus from file " + fileName);
	    try{
			 FileInputStream fileIn = new FileInputStream(fileName);
			 ObjectInputStream in = new ObjectInputStream(fileIn);
			 corpus = (DOECorpus) in.readObject();
			 in.close();
			 fileIn.close();
	    } catch(IOException i){
	         i.printStackTrace();
	         
	         return null;
	    } catch(ClassNotFoundException c) {
	         System.out.println("Corpus class not found");
	         c.printStackTrace();
	    
	         return null;
	    }
	    
	    System.out.println("De-serializing corpus from file...DONE");
	    
		return corpus;
	}

	public HashMap<String, ArrayList<DOECorpusLine>> getConcordances(String searchString) {
		HashMap<String, ArrayList<DOECorpusLine>> docsWithMatches = new HashMap<String, ArrayList<DOECorpusLine>>();
		
		for(DOECorpusDocument doc : this.docs.values()) {
			String shortTitle = doc.getShortTitle();
		
			for(DOECorpusLine line : doc.getLines())
				if(line.containsIgnoreCase(" " + searchString + " ") || line.startsWithIgnoreCase(searchString + " ") || line.containsIgnoreCase(" " + searchString + ".")) {
					if(!docsWithMatches.containsKey(shortTitle)) {
						ArrayList<DOECorpusLine> lineList = new ArrayList<DOECorpusLine>();
						lineList.add(line);
						docsWithMatches.put(shortTitle, lineList);
					}
					else {
						docsWithMatches.get(shortTitle).add(line);
					}
				}
		}
		
		return docsWithMatches;
	}

	public HashMap<String, Integer> getVocabulary() {
		return this.vocabulary;
	}
	
	public ArrayList<String> getWindowTokens(String targetTerm, String docID, String lineID, int windowSize) {
		DOECorpusDocument doc = this.docs.get(docID);
		ArrayList<String> windowTokens = doc.getTokenWindow(lineID, targetTerm, windowSize);
		
		return windowTokens;
	}
	
	public String getWindowTokensAsSpaceSeparatedString(String targetTerm, String docID, String lineID, int windowSize) {
		DOECorpusDocument doc = this.docs.get(docID);
		ArrayList<String> windowTokens = doc.getTokenWindow(lineID, targetTerm, windowSize);
		
		return toTokenWindow(windowTokens);
	}
	
	private String toTokenWindow(ArrayList<String> vectorTokens) {
		String tokenWindow = "";
		
		for(String token : vectorTokens) {
			tokenWindow += token + " ";
		}
		
		return tokenWindow.trim();
	}
}
