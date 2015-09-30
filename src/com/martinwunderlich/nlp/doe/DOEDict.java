package com.martinwunderlich.nlp.doe;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;

import org.apache.commons.io.FileUtils;

public class DOEDict implements java.io.Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 8129143778939995053L;
	HashMap<String, DOEEntryList> entryMap = new HashMap<String, DOEEntryList>();
	
	public void addEntryList(String letter, DOEEntryList entryList) {
		this.entryMap.put(letter, entryList);
	}

	public void saveToFile(String fileName) {
		try
	      {
			if(fileName.startsWith("file:"))
				fileName = fileName.replace("file:", "");
			 File file = new File(fileName);
			 System.out.println("Serializing dictionary to file " + fileName);
			 FileOutputStream fileOut = FileUtils.openOutputStream(file);
	         ObjectOutputStream out = new ObjectOutputStream(fileOut);
	         out.writeObject(this);
	         out.close();
	         fileOut.close();
	         System.out.printf("Serialized dictionary has been saved in " + fileName);
	      } catch(IOException i) {
	          i.printStackTrace();
	      }
	}
	
	public static DOEDict loadFromFile(String fileName) {
		DOEDict dictionary = null;
		if(fileName.startsWith("file:"))
			fileName = fileName.replace("file:", "");
		System.out.printf("De-serializing dictionary from file " + fileName);
	    try{
			 FileInputStream fileIn = new FileInputStream(fileName);
			 ObjectInputStream in = new ObjectInputStream(fileIn);
			 dictionary = (DOEDict) in.readObject();
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
	    
	    System.out.println("De-serializing dictionary from file...DONE");
	    
		return dictionary;
	}
}
