package com.martinwunderlich.nlp.doe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;

public class DOECorpusDocument implements java.io.Serializable {

	private static final String SEN_END = "</s>";

	private static final String SEN_START = "<s>";

	/**
	 * 
	 */
	private static final long serialVersionUID = 2869914283679554290L;

	private String fullTitle;
	
	private String shortTitle;
	private String shortShortTitle;
	private String cameronNumber;
	private String originalLocation;

	private LinkedHashMap<String, DOECorpusLine> lines = new LinkedHashMap<String, DOECorpusLine>();
	private ArrayList<String> sentences = new ArrayList<String>();
	private ArrayList<String> tokenList = new ArrayList<String>();;
	private HashSet<String> typeSet = new HashSet<String>();

	private int maxSenLength = -1;
	private int minSenLength = 1000;
	
	public DOECorpusDocument(String shortTitle, String shortShortTitle, String cameronNumber, String originalLocation) {
		this.shortTitle = shortTitle;
		this.shortShortTitle = shortShortTitle;
		this.cameronNumber = cameronNumber;
		this.originalLocation = originalLocation;
	}

	public void addLine(DOECorpusLine doeCorpusLine) {
		this.lines.put(doeCorpusLine.getLineID(), doeCorpusLine);
	}

	public int getLineCount() {
		return this.lines.size();
	}

	public void setFullTitle(String text) {
		this.fullTitle = text;
	}
	
	public String getShortTitle() {
		return shortTitle;
	}

	public void setShortTitle(String shortTitle) {
		this.shortTitle = shortTitle;
	}

	public String getShortShortTitle() {
		return shortShortTitle;
	}

	public void setShortShortTitle(String shortShortTitle) {
		this.shortShortTitle = shortShortTitle;
	}

	public String getCameronNumber() {
		return cameronNumber;
	}

	public void setCameronNumber(String cameronNumber) {
		this.cameronNumber = cameronNumber;
	}

	public String getOriginalLocation() {
		return originalLocation;
	}

	public void setOriginalLocation(String originalLocation) {
		this.originalLocation = originalLocation;
	}

	public String getFullTitle() {
		return fullTitle;
	}

	public void splitLinesIntoSentences() {
		for(DOECorpusLine line : this.lines.values()) {
			String[] sentences = line.getLine().split("\\.");
			
			for(String sen : sentences)
				this.sentences.add(SEN_START + sen + SEN_END);
		}
	}

	public ArrayList<String> tokenizeSentences() {
		for(String sen : this.sentences) {
			
			// Remove start and end markers
			sen = sen.replace(SEN_START, "").replace(SEN_END, "");;
			
			String[] tokens = sen.split(" ");
			List<String> tokenList = new ArrayList<String>(Arrays.asList(tokens));
			tokenList = normalizeTokens(tokenList);
			
			this.tokenList.addAll(tokenList);
			this.typeSet.addAll(tokenList);
			
			if(tokenList.size() > this.maxSenLength)
				this.maxSenLength = tokenList.size();
			
			if(tokenList.size() < this.minSenLength)
				this.minSenLength = tokenList.size();
		}
		
		return tokenList;
	}

	private List<String> normalizeTokens(List<String> tokens) {
		ArrayList<String> normTokens = new ArrayList<String>();
		String[] charsToRemove = {",", ".", "!", "?", ";", "<", ">", "(", ")"};
				
		for(String token : tokens) {
			for(String c : charsToRemove)
				token.replace(c, "");
			normTokens.add(token);
		}
		
		return normTokens;
	}

	public int getTokenCount() {
		return this.tokenList.size();
	}

	public int getTypeCount() {
		return this.typeSet.size();
	}

	public int getSentenceCount() {
		return this.sentences.size();
	}

	public Collection<? extends String> getTypes() {
		return this.typeSet;
	}

	public ArrayList<DOECorpusLine> getLines() {
		return (ArrayList<DOECorpusLine>) this.lines.values();
	}

	public ArrayList<String> getTokenWindow(String lineID, String targetTerm, int windowSize) {
  		DOECorpusLine line = this.lines.get(lineID);
		ArrayList<String> tokens = line.toTokenListLowerCase();
		ArrayList<String> vectorTokens = new ArrayList<String>();
		
		int pos = tokens.indexOf(targetTerm.toLowerCase());
		
		if(pos < 0) {
			System.out.println("Term not in here: " + line.getLine());
			return null;
		}
		
		int numberOfMissingTokensBefore = windowSize - pos;
		int numberOfMissingTokensAfter = windowSize - (tokens.size() - pos);
		
		if( numberOfMissingTokensBefore < 0 ) {				// enough tokens available in current line
			int startPos = pos - windowSize;
			List<String> tokensToAddBefore = tokens.subList(startPos, pos);
			vectorTokens.addAll(0, tokensToAddBefore);
		}
		else  												// need padding tokens from previous line(s)
			vectorTokens = padWithTokensFromLineBefore(windowSize, line, tokens, vectorTokens, pos, numberOfMissingTokensBefore);
		
		
		if( numberOfMissingTokensAfter < 0 ) {				// enough tokens available in current line
			int endPos = pos + windowSize + 1;
			List<String> tokensToAddAfter= tokens.subList(pos+1, endPos);
			vectorTokens.addAll(tokensToAddAfter);
		}
		else { 												// need padding tokens from following line(s)
			vectorTokens = padWithTokensFromLineAfter(windowSize, line, tokens, vectorTokens, pos, numberOfMissingTokensAfter);
		}
		
		return vectorTokens;
	}

	/**
	 * @param windowSize
	 * @param line
	 * @param tokens
	 * @param vectorTokens
	 * @param pos
	 * @param numberOfMissingTokensAfter
	 * @return
	 */
	private ArrayList<String> padWithTokensFromLineAfter(int windowSize, DOECorpusLine line, ArrayList<String> tokens, ArrayList<String> vectorTokens, int pos, int numberOfMissingTokensAfter) {
		vectorTokens.addAll(tokens.subList(pos+1,tokens.size()));
		DOECorpusLine lineAfter = line.getLineAfter();
		
		while(lineAfter != null && vectorTokens.size() < 2*windowSize) {
			ArrayList<String> tokensAfter = lineAfter.toTokenList();
			int endPos = Math.min(numberOfMissingTokensAfter + 1, tokensAfter.size());
			List<String> tokensToAddAfter = tokensAfter.subList(0, endPos);
			vectorTokens.addAll(tokensToAddAfter);
			numberOfMissingTokensAfter -= tokensToAddAfter.size();
			lineAfter = lineAfter.getLineAfter();
		}
		
		return vectorTokens;
	}

	/**
	 * @param windowSize
	 * @param line
	 * @param tokens
	 * @param vectorTokens
	 * @param pos
	 * @param numberOfMissingTokensBefore
	 * @return
	 */
	private ArrayList<String> padWithTokensFromLineBefore(int windowSize, DOECorpusLine line, ArrayList<String> tokens,ArrayList<String> vectorTokens, int pos, int numberOfMissingTokensBefore){
		vectorTokens.addAll(tokens.subList(0, pos));
		DOECorpusLine lineBefore = line.getLineBefore();
		
		while(lineBefore != null && vectorTokens.size() < windowSize) {
			ArrayList<String> tokensBefore = lineBefore.toTokenList();
			int startPos = Math.max(0, tokensBefore.size() - numberOfMissingTokensBefore);
			List<String> tokensToAddBefore = tokensBefore.subList(startPos, tokensBefore.size());
			vectorTokens.addAll(0, tokensToAddBefore);
			numberOfMissingTokensBefore -= tokensToAddBefore.size();
			lineBefore = lineBefore.getLineBefore();
		}
		
		return vectorTokens;
	}

	public int getMaxSentenceLength() {
		return this.maxSenLength;
	}

	public int getMinSentenceLength() {
		return this.minSenLength;
	}

	public int getTotalSentenceLength() {
		return this.sentences.size();
	}
}
