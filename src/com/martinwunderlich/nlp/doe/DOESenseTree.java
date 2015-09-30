package com.martinwunderlich.nlp.doe;

import java.util.ArrayList;

public class DOESenseTree implements java.io.Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3373971069655673619L;
	private DOEEntry entry;
	private ArrayList<DOEWordSense> senses = new ArrayList<DOEWordSense>();

	public DOEEntry getEntry() {
		return entry;
	}

	public DOESenseTree(DOEEntry doeEntry) {
		this.entry = doeEntry;
	}

	public void add(DOEWordSense sense) {
		this.senses.add(sense);
	}

	public int size() {
		return this.senses.size();
	}

}
