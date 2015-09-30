package com.martinwunderlich.nlp.doe;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class DOECorpusReader {

	private static final String CAMERON_NUMBER = "Cameron number: ";
	private static final String SHORT_SHORT_TITLE = "Short Short Title: ";
	private String sourceDir;
	private String saveDir;
	private boolean doSave;

	private String[] ignoreLinesStartingWith = {"January 2000", "Dictionary of Old English", "We ask that you not copy", "Cited by", "Citation is by page "};
	private String[] ignoreLinesEndingWith = {"citations"};
	
	public DOECorpusReader(String doeCorpusSourceDir, String doeCorpusSaveDir, boolean doSave) {
		this.sourceDir = doeCorpusSourceDir;
		this.saveDir = doeCorpusSaveDir;
		this.doSave = doSave;
	}

	/**
	 * Loads the corpus files and parses them into a Document+Sentence structure.
	 * There should be one HTML file in the DOE corpus format per doc unter the source directory.
	 * @return
	 */
	public DOECorpus loadFromHTML() {
		File[] files = getFileList();
		
		System.out.println("Found " + files.length + " documents in " + this.sourceDir);
		System.out.println("Parsing their contents...");
		int totalLineCount = 0;
		
		DOECorpus corpus = new DOECorpus();
		
		for(File file : files) {
			try {
				Document htmlDoc = Jsoup.parse(file, null);
				String originalLocation = htmlDoc.location();
				String shortTitle = htmlDoc.title();
				String shortShortTitle = "";
				String cameronNumber = "";
				
				DOECorpusDocument corpusDoc = new DOECorpusDocument(shortTitle, shortShortTitle, cameronNumber, originalLocation);
						
				Elements paragraphElements = htmlDoc.select("p");
				DOECorpusLine lineBefore = null;
				
				for(Element p : paragraphElements) {
					String text = p.text();
					
					if(ignoreLine(text))
						continue;
					
					else if(text.startsWith(SHORT_SHORT_TITLE))
						shortShortTitle = text.replace(SHORT_SHORT_TITLE, "");
					
					else if(text.startsWith(CAMERON_NUMBER))
						cameronNumber = text.replace(CAMERON_NUMBER, "");
					
					else if(text.startsWith("[")) {
						String lineID = "";
						String line = "";
						Pattern linePattern = Pattern.compile("\\[([0-9]+\\s\\([0-9a-zA-Z\\.,\\s\\(\\)]+\\))\\]\\s(.*)");
						Matcher m = linePattern.matcher(text);
						if (m.matches()) {
						  lineID = m.group(1).trim();
						  line   = m.group(2).trim();
						  
						  DOECorpusLine newLine = new DOECorpusLine(lineID, line, shortTitle);
						  newLine.setLineBefore(lineBefore);
						  if(lineBefore != null)
							  lineBefore.setLineAfter(newLine);
						  corpusDoc.addLine(newLine);
						  lineBefore = newLine;
						}
					}
					else if(!text.isEmpty())	// This should be the full title
						corpusDoc.setFullTitle(text);
				}
				
				totalLineCount += corpusDoc.getLineCount();
				corpus.addDocument(corpusDoc);
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		System.out.println("Parsing their contents...DONE");
		System.out.println("Total number of lines: " + totalLineCount);
		
		System.out.println("Splitting lines into sentences...");
		corpus.splitIntoSentences();
		System.out.println("Splitting lines into sentences...DONE");
		
		System.out.println("Tokenizing sentences...");
		corpus.tokenize();
		System.out.println("Tokenizing sentences...DONE");
		System.out.println("Total number of tokens: " + corpus.getTokenCount());
		System.out.println("Total number of types: " + corpus.getTypeCount());
		System.out.println("Total number of sentences: " + corpus.getSentenceCount());
		System.out.println("Average sentence length: " + corpus.getAvgSenLength());
		System.out.println("Minimum sentence length: " + corpus.getMinSenLength());
		System.out.println("Maximum sentence length: " + corpus.getMaxSenLength());
		
		return corpus;
	}

	private boolean ignoreLine(String text) {
		for(String str : this.ignoreLinesStartingWith)
			if(text.startsWith(str))
				return true;
		
		for(String str : this.ignoreLinesEndingWith)
			if(text.endsWith(str))
				return true;
		
		return false;
	}

	/**
	 * @return
	 */
	private File[] getFileList() {
		File dir = new File(this.sourceDir.replace("file:", ""));
		File [] files = dir.listFiles(new FilenameFilter() {
		    @Override
		    public boolean accept(File dir, String name) {
		        return (name.endsWith(".htm") || name.endsWith(".html")) && ! name.startsWith("toc-");
		    }
		});
		return files;
	}
}
