package com.martinwunderlich.nlp.doe;

public class DOEWordSense implements java.io.Serializable {

	private String id;
	private String idHierarchical;
	private String def;

	public DOEWordSense(String id, String idHierarchical, String def) {
		this.id = id;
		this.idHierarchical = idHierarchical;
		this.def = def;
	}

}
