package com.martinwunderlich.nlp.doe;

import java.io.IOException;
import java.util.Map;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.martinwunderlich.common.html.HTMLtableDoc;

public class DOEEntry implements java.io.Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7466929724941880461L;
	private String id = "";
	private String url = "";
	private String entry = "";
	Map<String, String> cookies = null;
	
	private DOESenseTree senseTree = null;
	
	public DOEEntry(String id, String url, String entry, Map<String, String> cookies) {
		this.setId(id);
		this.url = url;
		this.entry = entry;
		this.cookies = cookies;
		
		buildSenseTree(url);
	}

	private void buildSenseTree(String url) {
		System.out.println("-------------------------------------------------------");
		System.out.println("Extracting senses for entry '" + this.entry + "'");
		System.out.println("-------------------------------------------------------");
		
		this.senseTree = new DOESenseTree(this);
		
		/*Connection connection = Jsoup.connect(url);
		Document doc1 = null;
		try {
			doc1 = connection.get();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
		 
		//Connection connection = Jsoup.connect("http://tapor.library.utoronto.ca/doe/dict/entries/00/@@ENTRYID@@.html".replace("@@ENTRYID@@", this.id));
		Connection connection = Jsoup.connect(url);

		for (Map.Entry<String, String> cookie : this.cookies.entrySet()) {
		    connection.cookie(cookie.getKey(), cookie.getValue());
		}

		Document doc = null;
		try {
			doc = connection.get();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		HTMLtableDoc htmlDoc = new HTMLtableDoc(url, connection);
		Elements tableElements = htmlDoc.getTables();
		Elements tableRowElements = tableElements.select(":not(thead) tr");
		
		for (int i = 0; i < tableRowElements.size(); i++) {
		   String id = "";
		   String def = "";
		   String idHierarchical = "";
		   
           Element row = tableRowElements.get(i);
           Elements rowDataElements = row.select("td");
           
           for(Element rowData : rowDataElements) {
        	   if(rowData.select("a").hasAttr("id")) {
        		   id = rowData.select("a").attr("id");
        		   idHierarchical = rowData.select("a").text();
        		   if(idHierarchical.isEmpty()) 	// needed for entries with one sense only
        			   def = rowData.text();
        	   }
        	   else
        		   def = rowData.text();
           }

    	   System.out.println("ID: " + id);
    	   System.out.println("\thierachical ID: " + idHierarchical);
    	   System.out.println("\tDef: " + def);
    	   
    	   DOEWordSense sense = new DOEWordSense(id, idHierarchical, def);
    	   this.senseTree.add(sense);
        }
	}

	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}
	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getEntry() {
		return entry;
	}

	public void setEntry(String entry) {
		this.entry = entry;
	}
	
	public int getSenseCount() {
		return this.senseTree.size();
	}
	
	public boolean isPolysemous() {
		if(this.getSenseCount() > 1)
			return true;
		else
			return false;
	}
}
