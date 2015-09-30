package com.martinwunderlich.nlp.doe;

import java.util.ArrayList;
import java.util.Arrays;

public class DOECorpusLine implements java.io.Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5712095630797726424L;
	private String lineID = "";
	private String line = "";
	private String shortTitle = "";
	private DOECorpusLine lineBefore = null;
	private DOECorpusLine lineAfter = null;

	public DOECorpusLine(String lineID, String line, String shortTitle) {
		this.lineID = lineID;
		this.line = line;
		this.shortTitle = shortTitle;
	}

	public String getLineID() {
		return lineID;
	}

	public void setLineID(String lineID) {
		this.lineID = lineID;
	}

	public String getLine() {
		return line;
	}

	public void setLine(String line) {
		this.line = line;
	}

	public boolean containsIgnoreCase(String searchString) {
		if( this.line.toLowerCase().contains(searchString.toLowerCase()) )
			return true;
		else
			return false;
	}

	public String getShortTitle() {
		return this.shortTitle;
	}

	public boolean startsWithIgnoreCase(String string) {
		if(string.length() > this.line.length())
			return false;
		
		if(this.line.substring(0, string.length()-1).toLowerCase().equals(string.toLowerCase()))
			return true;
		else
			return false;
	}

	public ArrayList<String> toTokenList() {
		String[] tokens = this.line.split(" ");
		
		return new ArrayList<String>( Arrays.asList( tokens ) );
	}
	
	public ArrayList<String> toTokenListLowerCase() {
		ArrayList<String> resultTokens = new ArrayList<String>();
		String[] tokens = this.line.split(" ");
		
		for(String token : tokens) {
			if(token.endsWith("."))
				token = token.substring(0, token.length()-1); // TODO: Make sure to standard token normalizer here
		
			resultTokens.add(token.toLowerCase());
		}
		
		return resultTokens;
	}

	public DOECorpusLine getLineBefore() {
		return lineBefore;
	}
	
	public void setLineBefore(DOECorpusLine lineBefore) {
		this.lineBefore = lineBefore;
	}

	public DOECorpusLine getLineAfter() {
		return lineAfter;
	}

	public void setLineAfter(DOECorpusLine lineAfter) {
		this.lineAfter = lineAfter;
	}
	
	@Override
	public String toString() {
		return this.line;
	}
}
