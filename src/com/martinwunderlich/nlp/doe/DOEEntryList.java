package com.martinwunderlich.nlp.doe;

import java.util.ArrayList;

public class DOEEntryList implements java.io.Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4909397346773953711L;

	private String letter = "";

	private ArrayList<DOEEntry> entryList = new ArrayList<DOEEntry>();
	
	public DOEEntryList(String letter) {
		this.letter  = letter;
	}

	public void addEntry(DOEEntry doeEntry) {
		this.entryList.add(doeEntry);
	}
	
	public int size() {
		return this.entryList.size();
	}
	
	public String getLetter() {
		return letter;
	}
}
