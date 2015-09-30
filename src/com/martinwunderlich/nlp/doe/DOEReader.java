package com.martinwunderlich.nlp.doe;

import java.util.Map;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.martinwunderlich.common.html.HTMLtableDoc;
import com.martinwunderlich.common.io.FileUtil;
import com.martinwunderlich.common.ssl.SSLUtil;

public class DOEReader {

	private String sourceDir = "";
	private String saveDir = "";
	private boolean doSave = false;

	// TODO: externalize this to config file (?)
	final private String HTML_FILE_BASENAME = "Dictionary of Old English Project -- Headwords_@@LETTER@@.html";
	final private String[] letters = {"A","AE","B","C","D","E","F","G"};
	
	public DOEReader(String doeSourceDir, String doeSaveDir, boolean doSave) {
		this.sourceDir = doeSourceDir;
		this.saveDir = doeSaveDir;
		this.doSave = doSave;
	}

	public DOEDict parseHTML() {
		DOEDict dict = new DOEDict();
		
		String userParam = "user";
		String username = "martin.wunderlich@campus.lmu.de";
		String passParam = "pass";
		String password = "WundiLMU321";
		
		
		for(String letter : letters) {
			DOEEntryList entryList = new DOEEntryList(letter);
			
			// Get the absolute path for the raw HTML file
			String fileName = HTML_FILE_BASENAME.replace("@@LETTER@@", letter);
			String path = this.sourceDir + fileName;
			String absPath = FileUtil.getAbsPath(path);
		
			HTMLtableDoc htmlDoc = new HTMLtableDoc(absPath);
			Elements tableElements = htmlDoc.getTables();
			Elements tableRowElements = tableElements.select(":not(thead) tr");

	        // Loop over the table and extract entry data
	        for (int i = 1; i < tableRowElements.size(); i++) {
	           Element row = tableRowElements.get(i);
	           Elements rowData = row.select("td");
	           String entry = rowData.select("a").text();
	           String id = rowData.select("a").attr("id");
	           String url = rowData.select("a").attr("href");
	           
	           String loginUrl = "https://login.emedien.ub.uni-muenchen.de/login?url=" + url;
	           Map<String, String> cookies = SSLUtil.getLoginCookies(loginUrl, userParam, username, passParam, password);
	           
	           DOEEntry doeEntry = new DOEEntry(id, url, entry, cookies);
	           entryList.addEntry(doeEntry);
	        }
	        
	        System.out.println("Created entry list for letter " + entryList.getLetter());
	        System.out.println("with " + entryList.size() + " dictionary entries.");
	        
	        dict.addEntryList(letter, entryList);
		}
		
		return dict;
	}
}
