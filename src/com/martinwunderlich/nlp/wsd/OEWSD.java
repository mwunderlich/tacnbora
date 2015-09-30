package com.martinwunderlich.nlp.wsd;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;

import com.martinwunderlich.common.datetime.DateTimeUtil;
import com.martinwunderlich.common.listutils.ListAndMapUtil;
import com.martinwunderlich.common.ssl.SSLUtil;
import com.martinwunderlich.nlp.classification.ClassificationResult;
import com.martinwunderlich.nlp.classification.MalletClassificationResult;
import com.martinwunderlich.nlp.classification.MostFrequentClassAssignmentTrainer;
import com.martinwunderlich.nlp.classification.RandomAssignmentTrainer;
import com.martinwunderlich.nlp.doe.DOECorpus;
import com.martinwunderlich.nlp.doe.DOECorpusLine;
import com.martinwunderlich.nlp.doe.DOECorpusReader;
import com.martinwunderlich.nlp.doe.DOEDict;
import com.martinwunderlich.nlp.doe.DOEReader;

import cc.mallet.classify.Classifier;
import cc.mallet.classify.ClassifierTrainer;
import cc.mallet.classify.MaxEntTrainer;
import cc.mallet.classify.NaiveBayesTrainer;
import cc.mallet.classify.Trial;
import cc.mallet.pipe.CharSequence2TokenSequence;
import cc.mallet.pipe.FeatureSequence2FeatureVector;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.PrintInputAndTarget;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.pipe.Target2Label;
import cc.mallet.pipe.TokenSequence2FeatureSequence;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.Label;

public class OEWSD {

	// Directories - TODO: externalize to config file
	private final static String	APP_DIR					=	OEWSD.class.getProtectionDomain().getCodeSource().getLocation().toString();
	private final static String DATA_ROOT_DIR			=	APP_DIR + "../data/";
	private final static String DOE_SOURCE_DIR 			=	DATA_ROOT_DIR + "lexicographic/DOE/";
	private final static String DOE_SAVE_DIR			=	DOE_SOURCE_DIR + "save/";
	private final static String DOE_CORPUS_SOURCE_DIR 	=	DATA_ROOT_DIR + "corpora/DOE_corpus/html/";
	private final static String DOE_CORPUS_SAVE_DIR		=	DOE_CORPUS_SOURCE_DIR + "save/";
    
    // Command Strings for CMD line interface
    private static final String DATA_CMD_STR 					= "data";
    private static final String PARSE_DOE_CMD_STR 				= "parsedoe";
    private static final String LOAD_DOE_CMD_STR 				= "loaddoe";
    private static final String PARSE_DOECORPUS_CMD_STR 		= "parsedoe_corpus";
	private static final String LOAD_DOECORPUS_CMD_STR 			= "loaddoe_corpus";
	private static final String SEARCH_DOECORPUS_CMD_STR 		= "search";
	private static final String COLLOCATIONS_DOECORPUS_CMD_STR 	= "conc";
	private static final String FINDCOUNT_CORPUS_CMD_STR 		= "findcount";
	private static final String PRINT_TYPES_CMD_STR		 		= "types";
	private static final String CONVERT2MALLET_CMD_STR		 	= "convert2mallet"; // Parameters: target term, annotated source file; output file; text window size x (window will be +/-around the target term
	private static final String TRAIN_MALLET_CLASSIFIER_CMD_STR= "train"; // Parameters: target term, annotated source file, training algo, output file, text window size
	
	// Global variables for corpus and dictionary
	private static DOECorpus corpus = null;
	private static DOEDict dictionary;
	
    
	public static void main(String[] args) {
		if( args.length == 0)
			printUsage();
		
		init();
		
		System.out.println("Started OEWSD at " + new Date());
		System.out.println("Running in APP_DIR: " + APP_DIR);
		
		String cmd = args[0];
		
		switch(cmd) {
			case DATA_CMD_STR: execDataCmd(args);
						break;
			case SEARCH_DOECORPUS_CMD_STR : execSearchDoeCorpus(args);
						break;
			case FINDCOUNT_CORPUS_CMD_STR : execFindOccurrencesDoeCorpus(args);
						break;
			case COLLOCATIONS_DOECORPUS_CMD_STR : execFindConcordancesDoeCorpus(args);
						break;
			case PRINT_TYPES_CMD_STR: execPrintTypesDoeCorpus(args);
						break;
			case CONVERT2MALLET_CMD_STR: execConvertToMalletFormat(args);
						break;
			case TRAIN_MALLET_CLASSIFIER_CMD_STR: execTrainMalletClassifier(args);
						break;
			default: System.out.println("UNKNOWN COMMAND: " + args[0]);
					printUsage();
					break;
		}
	}
	
	private static void execConvertToMalletFormat(String[] args) {
		if(args.length != 5)
			throw new IllegalArgumentException("There should be 5 arguments, but I found only " + args.length);
		
		// Load corpus first
		execParseDoeCorpus(false /* doSave */);
		
		String targetTerm = args[1];
		String sourceFile = args[2];
		String targetFile = args[3];
		int termWindowSize = Integer.parseInt(args[4]);
		
		System.out.println("Reading concordance file for target term '" + targetTerm + "'...");
		System.out.println("File path: " + sourceFile);
		
		BufferedReader in = null;
		String malletFileString = "";
		try {
			in = new BufferedReader(new FileReader(sourceFile));

			String str;
			while ((str = in.readLine()) != null) {
			    String[] lineArray = str.split(";");
			    String docID = lineArray[0].replace("Doc ID: ", "").trim();
			    String lineID = lineArray[1].replace("Line ID: ", "").trim();
			    String instanceID = (docID + "_" + lineID).replaceAll(" ", "_");
			    String senseID = lineArray[2].replace("DOE sense ID: ", "").trim();
			    String text = lineArray[3];
			    
			    String data = corpus.getWindowTokensAsSpaceSeparatedString(targetTerm, docID, lineID, termWindowSize);
			    if( data != null) {
				    String malletInstanceLine = instanceID + " " + senseID + " " + data + "\n";
				    malletFileString += malletInstanceLine;
			    }
			}
			
			in.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.out.println("Writing result to Mallet file...");
		System.out.println("File path: " + targetFile);
		PrintWriter out;
		try {
			out = new PrintWriter(targetFile);
			out.println(malletFileString);
			out.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Writing result to Mallet file...DONE");
	}
	
	public static void execTrainMalletClassifier(String[] args) {
		// Parameters: target term, annotated source file, training algo, output file for classifier, output file base for results, text window size
		if(args.length != 8)
			throw new IllegalArgumentException("There should be 8 arguments, but I found " + args.length);
		
		// Load corpus first
		execParseDoeCorpus(false /* doSave */);
		
		// Parse cmd line params
		String targetTerm = args[1];
		String sourceFile = args[2];
		String[] trainingAlgos = args[3].split(",");
		String outputFileClassifier = args[4];
		String outputFileResults = args[5];
		String[] termWindowSizes = args[6].split(",");
		boolean useCollocationalVector = "coll".equals(args[7]) ? true : false;
		
		System.out.println("Reading concordance file for target term '" + targetTerm + "'...");
		System.out.println("File path: " + sourceFile);
		
		// Build the preprocessing pipe
		Pipe pipe = buildPipe();
		
		System.out.println("Running classification for training algorithm(s) " + Arrays.toString(trainingAlgos) + " and term window size(s) " + Arrays.toString(termWindowSizes) + "...");
		List<ClassificationResult> results = new ArrayList<ClassificationResult>();
		
		for(String trainingAlgo : trainingAlgos)
			for(String termWindowSizeStr : termWindowSizes) {
				int termWindowSize = Integer.parseInt(termWindowSizeStr);
				results.addAll( runTrainingAndClassification(targetTerm, sourceFile, trainingAlgo, outputFileClassifier, outputFileResults, termWindowSize, pipe, useCollocationalVector) );
			}

        // Save results
        saveResultsToFile(outputFileResults, results);
        
		System.out.println("Running classification...DONE");
	}

	/**
	 * @param targetTerm
	 * @param sourceFile
	 * @param trainingAlgo
	 * @param outputFileClassifier
	 * @param outputFileResults
	 * @param termWindowSize
	 * @param pipe
	 * @return
	 */
	private static List<ClassificationResult> runTrainingAndClassification(String targetTerm,
			String sourceFile, String trainingAlgo,
			String outputFileClassifier, String outputFileResults,
			int termWindowSize, Pipe pipe, boolean useCollocationalVector) {
		// Read in concordance file and create list of Mallet training instances
		//TODO: Remove duplication of code (see execConvertToMalletFormat(...))
		String vectorType = useCollocationalVector ? "coll" : "bow";
		
		InstanceList instanceList = readConcordanceFileToInstanceList(targetTerm, sourceFile, termWindowSize, pipe, useCollocationalVector);
		
		// Creating splits for training and testing
		double[] proportions = {0.9, 0.1};
		InstanceList[] splitLists = instanceList.split(proportions);
		InstanceList trainingList = splitLists[0];
		InstanceList testList = splitLists[1];
		
		// Train the classifier
		ClassifierTrainer classifierTrainer = getClassifierTrainerForAlgorithm(trainingAlgo);
		
		Classifier classifier = classifierTrainer.train(trainingList);
		if( classifier.getLabelAlphabet() != null ) { // TODO: Make sure this is not null in RandomClassifier
			System.out.println("Labels:\n" + classifier.getLabelAlphabet());
			System.out.println("Size of data alphabet (= type count of training list): " + classifier.getAlphabet().size());
		}
		
		// Run tests and get results
		Trial trial = new Trial(classifier, testList);
        List<ClassificationResult> results = new ArrayList<ClassificationResult>();
        
        for(int i = 0; i < classifier.getLabelAlphabet().size(); i++) {
        	Label label = classifier.getLabelAlphabet().lookupLabel(i);
        	ClassificationResult result = new MalletClassificationResult(trainingAlgo, targetTerm, vectorType, label.toString(), termWindowSize, trial, sourceFile);
        	results.add(result);
        	
        	System.out.println(result.toString());
        }
        
        // Save classifier
        saveClassifierToFile(outputFileClassifier, classifier, trainingAlgo, termWindowSize);
        
        return results;
	}

	/**
	 * @param trainingAlgo
	 * @return
	 */
	private static ClassifierTrainer getClassifierTrainerForAlgorithm(
			String trainingAlgo) {
		ClassifierTrainer classifierTrainer = null;
		switch(trainingAlgo) {
			case "nb"		: classifierTrainer = new NaiveBayesTrainer();
				break;
			case "maxent"	: classifierTrainer = new MaxEntTrainer();
				break;
			case "rnd"		: classifierTrainer = new RandomAssignmentTrainer();
				break;
			case "mostfreq"	: classifierTrainer = new MostFrequentClassAssignmentTrainer();
				break;
			default		 	: classifierTrainer = new NaiveBayesTrainer();
				break;
		}
		return classifierTrainer;
	}

	/**
	 * @param outputFileClassifier
	 * @param classifier
	 */
	private static void saveClassifierToFile(String outputFileClassifier, Classifier classifier, String algo, int windowSize) {
		ObjectOutputStream oos;
		
		if( outputFileClassifier.contains("@@DATETIME@@") ) {
    		String dateTime = DateTimeUtil.getDateTimeString();
    		outputFileClassifier = outputFileClassifier.replace("@@DATETIME@@", dateTime);
    	}
		
		if( outputFileClassifier.contains("@@ALGO@@") ) {
    		String dateTime = DateTimeUtil.getDateTimeString();
    		outputFileClassifier = outputFileClassifier.replace("@@ALGO@@", algo);
    	}
		
		if( outputFileClassifier.contains("@@WINDOW@@") ) {
			String windowSizeStr = Integer.toString(windowSize);
    		String dateTime = DateTimeUtil.getDateTimeString();
    		outputFileClassifier = outputFileClassifier.replace("@@WINDOW@@", windowSizeStr);
    	}
    	
		try {
			oos = new ObjectOutputStream(new FileOutputStream (outputFileClassifier));
	        oos.writeObject (classifier);
	        oos.close();
		} catch (IOException e) {
			System.out.println("ERROR while trying to save classifier to file " + outputFileClassifier);
			e.printStackTrace();
		}
		System.out.println("Classifier written successfully to file " + outputFileClassifier);
	}

	/**
	 * @param outputFileResults
	 * @param results
	 */
	private static void saveResultsToFile(String outputFileResults,
			List<ClassificationResult> results) {
		try {
        	String csvOutput = "";
        	csvOutput += MalletClassificationResult.getCSVHeaderString() + "\n";
        	
        	for(ClassificationResult result : results)
        		csvOutput += ((MalletClassificationResult)result).toCSVLine() + "\n";
        
        	if( outputFileResults.contains("@@DATETIME@@") ) {
        		String dateTime = DateTimeUtil.getDateTimeString();
        		outputFileResults = outputFileResults.replace("@@DATETIME@@", dateTime);
        	}
        	
        	File resultsFile = new File(outputFileResults);
        	FileUtils.writeStringToFile(resultsFile, csvOutput);
		} catch (IOException e) {
			System.out.println("ERROR while trying to write results to CSV file " + outputFileResults);
			e.printStackTrace();
		}
        System.out.println("Results written successfully to file " + outputFileResults);
	}

	/**
	 * @param targetTerm
	 * @param sourceFile
	 * @param termWindowSize
	 * @param pipe
	 */
	private static InstanceList readConcordanceFileToInstanceList(String targetTerm, String sourceFile, int termWindowSize, Pipe pipe, boolean useCollocationalVector) {
		InstanceList instanceList = new InstanceList(pipe);
		BufferedReader in = null;
		try {
			in = new BufferedReader(new FileReader(sourceFile));
			int incomplete = 0;
			
			String str;
			while ((str = in.readLine()) != null) {
			    String[] lineArray = str.split(";");
			    
			    if(lineArray.length != 4) {
			    	System.out.println("WARNING: Skipping possibly invalid CSV line " + str + " in file " + sourceFile) ;
			    	continue;
			    }
			    
			    String docID = lineArray[0].replace("Doc ID: ", "").trim();
			    String lineID = lineArray[1].replace("Line ID: ", "").trim();
			    String instanceID = (docID + "_" + lineID).replaceAll(" ", "_");
			    String senseID = lineArray[2].replace("DOE sense ID: ", "").trim();
			    String text = lineArray[3];
			    
			    if(targetTerm.equals("faeder"))
			    	targetTerm = "fæder";
			    
			    ArrayList<String> data = corpus.getWindowTokens(targetTerm, docID, lineID, termWindowSize);
			    
			    if( data.size() != 2*termWindowSize ) {
			    	incomplete++;
			    	System.out.println("WARNING: Incomplete token list " + incomplete + " found " + data);
			    }
			    	
			    
			    if( useCollocationalVector ) {
			    	System.out.println("Converting data to collocational vector: \n\t" + data);
			    	int i = termWindowSize * (-1);
			    	int index = i + termWindowSize;
			    	
			    	while(i <= termWindowSize && index < data.size()) {
			    		if(i != 0) {
			    			data.set(index, data.get(index) + "_" + i); // skip position of target term
			    			index++;
			    		}
			    		
			    		i++;
			    	}
			    	System.out.println("Converting data to collocational vector...DONE\n\t" + data);
			    }
			    
			    String dataStr = data.toString().replace(", ", " ").replace("[","").replace("]","").replace(".","");
			    Instance trainingInstance = new Instance(dataStr, senseID, instanceID, text);

			    instanceList.addThruPipe(trainingInstance);
			}
			in.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			if(in != null)
				try {
					in.close();
				} catch (IOException e1) {
				}
		}
		
		return instanceList;
	}

	/**
	 * @return
	 */
	private static Pipe buildPipe() {
		// Build pipe
		ArrayList pipeList = new ArrayList();
        // Tokenize raw strings
		Pattern tokenPattern = Pattern.compile("[\\p{L}\\p{N}_]+");
        pipeList.add(new CharSequence2TokenSequence(tokenPattern));

        // Normalize all tokens to all lowercase
        //pipeList.add(new TokenSequenceLowercase());

        // Remove stopwords from a standard English stoplist.
        //  options: [case sensitive] [mark deletions]
        //pipeList.add(new TokenSequenceRemoveStopwords(false, false));

        // Convert strings to integers by looking them up in an alphabet.
        pipeList.add(new TokenSequence2FeatureSequence());

        // Do the same thing for the "target" field:
        pipeList.add(new Target2Label());

        // Now convert the sequence of features to a sparse vector,
        //  mapping feature IDs to counts.
        pipeList.add(new FeatureSequence2FeatureVector());
		
        // Add some output
        pipeList.add(new PrintInputAndTarget());
        
        Pipe pipe = new SerialPipes(pipeList);
		
        return pipe;
	}

	private static void execPrintTypesDoeCorpus(String[] args) {
		boolean ascending = true;
		
		// Ascending or descending?
		if( args.length > 1)
			if("asc".equalsIgnoreCase(args[1]))
				ascending = true;
			else if("desc".equalsIgnoreCase(args[1]))
				ascending = false;
			else
				throw new IllegalArgumentException("Second parameter must be either 'asc' or 'desc'");
		
		// Save to file?
		String path = "";
		List<String> lines = null;
		boolean writeToFile = false;
		if( args.length > 2 ) {
			path = args[2];
			lines = new ArrayList<>();
			writeToFile = true;
		}
		
		// Load corpus first
		execParseDoeCorpus(false /* doSave */);
		
		HashMap<String, Integer> types = corpus.getVocabulary();
		Map<String, Integer> sortedTypes = ListAndMapUtil.sortByComparator(types, ascending);
		
		for(String key : sortedTypes.keySet()) {
			String line = key + ";" + sortedTypes.get(key);
			System.out.println(line);
			
			if(writeToFile)
				lines.add(line);
		}
		
		// Save to file
		if(writeToFile)
			try {
				FileUtils.writeLines(new File(path), lines);
			} catch (IOException e) {
				System.out.println("Error while trying to write to file " + path);
				e.printStackTrace();
			}
	}

	private static void execFindConcordancesDoeCorpus(String[] args) {
		int limit = 1000000;
		if( args.length > 2 )
			limit = 100;
		
		String searchString = args[1];

		execParseDoeCorpus(false /* doSave */);
		
		ArrayList<DOECorpusLine> allLines = new ArrayList<DOECorpusLine>();
		HashMap<String, ArrayList<DOECorpusLine>> matches = corpus.getConcordances(searchString);
		for(ArrayList<DOECorpusLine> list : matches.values())
			allLines.addAll(list);
		
		if(allLines.size() > limit)
			allLines = getRandomSelectionFromList(allLines, limit);
		
		System.out.println("Concordances for search term " + searchString + " with limit " + limit);
		for(DOECorpusLine line : allLines) {
			System.out.println("Doc ID: " + line.getShortTitle() + "; Line ID: " + line.getLineID() + "; " + line.getLine());
		}
	}

	private static ArrayList<DOECorpusLine> getRandomSelectionFromList(ArrayList<DOECorpusLine> list, int limit) {
		Set<DOECorpusLine> randomSet = new HashSet<DOECorpusLine>();
		Random random = new Random();
		
		while(randomSet.size() < limit) {
			DOECorpusLine randomLine = list.get( random.nextInt(list.size()) );
			randomSet.add(randomLine);
		}
		
		return new ArrayList<DOECorpusLine>(randomSet);
	}

	private static void execFindOccurrencesDoeCorpus(String[] args) {
		int minCount = Integer.parseInt(args[1]);
		int maxCount = Integer.parseInt(args[2]);
		int minLen   = Integer.parseInt(args[3]);
		
		// Load corpus first
		execParseDoeCorpus(false /* doSave */);
		
		corpus.findTypesWithMinMaxOccurrenceAndMinLength(minCount, maxCount, minLen);
	}

	private static void init() {
		SSLUtil.installAllTrustingTrustManager();
	}

	private static void execSearchDoeCorpus(String[] args) {
		String searchString = args[1];
		boolean interactive = false;
		
		// Load corpus first
		execParseDoeCorpus(false /* doSave */);
		
		// Check mode
		if(args.length > 2)
			if(args[2].equals("i"))
				interactive = true;
		
		// Search for string
		HashMap<String, ArrayList<DOECorpusLine>>  matches = corpus.getConcordances(searchString);
		printSearchResults(searchString, matches);
		
		if(interactive) {
			do {
				System.out.println("Enter the sear term here (or QUIT to exit):");
				Scanner scanIn = new Scanner(System.in);
			    searchString = scanIn.nextLine();
			    scanIn.close();
			    
			    if(!("QUIT".equals(searchString))) {
			    	matches = corpus.getConcordances(searchString);
					printSearchResults(searchString, matches);
			    }
		    } while(!("QUIT".equals(searchString)));
		}
	}

	/**
	 * @param searchString
	 * @param matches
	 */
	private static void printSearchResults(String searchString, HashMap<String, ArrayList<DOECorpusLine>> matches) {
		// Print results
		System.out.println("Found the search string " + searchString + " in the following " + matches.size() + " docs and lines: ");
		for(String key : matches.keySet()) {
			System.out.println("Doc ID: " + key);
			int i = 0;
			for(DOECorpusLine line : matches.get(key)) {
				System.out.println("\t" + i + ") "+ line.getLineID() + " - " + line.getLine().replace(searchString, "@@" + searchString + "@@"));
				i++;
			}
		}
	}

	private static void execDataCmd(String[] args) {
		boolean hasParams = false;
		boolean doSave = false;
		ArrayList<String> params = null;
		
		System.out.println("Called data cmd with " + args);
		
		String subCmd = args[1];
		if(args.length > 2) {
			hasParams = true;
			params = new ArrayList<String>( Arrays.asList(args) );
			params.remove(0); // get rid of first two cmds
			params.remove(0);
			
			if(params.contains("s"))
				doSave = true;
				
		}
		
		switch(subCmd) {
			case PARSE_DOE_CMD_STR : execParseDoe(doSave);
							break;
			case PARSE_DOECORPUS_CMD_STR : execParseDoeCorpus(doSave);
							break;
			case LOAD_DOECORPUS_CMD_STR : execLoadDoeCorpus(params);
							break;
		}
	}

	private static void execLoadDoeCorpus(ArrayList<String> params) {
		DOECorpus corpus = DOECorpus.loadFromFile(DOE_CORPUS_SAVE_DIR + "doe.corpus");
	}

	private static void execParseDoeCorpus(boolean doSave) {
		if(corpus != null) {
			System.out.println("Using pre-loaded corpus.");
			return;
		}
		
		DOECorpusReader corpusReader = new DOECorpusReader(DOE_CORPUS_SOURCE_DIR, DOE_CORPUS_SAVE_DIR, doSave);
		corpus = corpusReader.loadFromHTML();

		if(doSave)
			corpus.saveToFile(DOE_CORPUS_SAVE_DIR + "doe.corpus");
	}

	private static void execParseDoe(boolean doSave) {
		DOEReader doer = new DOEReader(DOE_SOURCE_DIR, DOE_SAVE_DIR, doSave);
		dictionary = doer.parseHTML();
		if(doSave)
			dictionary.saveToFile(DOE_SAVE_DIR + "doe.dict");
	}

	private static void printUsage(){
		System.out.println("The OEWSD command line interface supports the following parameters, whereby the first parameter will be the command and the remaining parameters will be parameters to this command:\n");
		System.out.println("\t" + DATA_CMD_STR + " " + PARSE_DOE_CMD_STR + " s - reads the DOE data from the data directory and builds the internal structure; if the s flag is given, the structure will be persisted as Java serialized objects to the output path given in the preferences.");
		System.out.println("\t" + DATA_CMD_STR + " " + LOAD_DOE_CMD_STR + " - Load the DOE data from the data directory.");
	    System.out.println("\t" + DATA_CMD_STR + " " + PARSE_DOECORPUS_CMD_STR + " - Parse the DOE corpus into an internal representation.");
		System.out.println("\t" + DATA_CMD_STR + " " +  LOAD_DOECORPUS_CMD_STR + " - Load the DOE corpus from the given directory.");
		System.out.println("\t" +  SEARCH_DOECORPUS_CMD_STR + " - Search for a given target term in the corpus text. The flag i can be used to switch to interactive mode, whereby the corpus is loaded only once and the user can run several searches through the command line interfacen.");
		System.out.println("\t" + COLLOCATIONS_DOECORPUS_CMD_STR + " - Find concordance matches for a given term.");
		System.out.println("\t" + FINDCOUNT_CORPUS_CMD_STR + " - Count the number of occurences of the given term. This command takes three parameters: minimum count, maximum count and minium length of the terms.");
		System.out.println("\t" + PRINT_TYPES_CMD_STR + " - Outputs all types in the corpus.");
		System.out.println("\t" + CONVERT2MALLET_CMD_STR + " - Converts an annotated source file to the Mallet format required for cmd line usage. Parameters: target term, annotated source file; output file; text window size x (window will be +/-around the target term");
		System.out.println("\t" + TRAIN_MALLET_CLASSIFIER_CMD_STR + " - Trains a classifier with the given algorithm and outputs the results. Paramters are (in order):");
		System.out.println("\t\t - The targetTerm to be classified.");
		System.out.println("\t\t - The sourceFile with the training data.");
		System.out.println("\t\t - The abbreviation of the trainingAlgo(s), i.e. one of rnd, mostfreq, nb, maxent. This can be a comma-separated list to train several classifiers at one, e.g. \"nb,maxent\"");
		System.out.println("\t\t - The outputFileClassifier where the trained classifier(s) will be stored.");
		System.out.println("\t\t - The outputFileResults where the classification results will be stored in CSV format.");
		System.out.println("\t\t - The termWindowSize(s) to be used in training. This can be a comma-separated list to train for several parameters values at once");
		System.out.println("\t\t - A boolean flag to specify whether a collocational vector should be used (\"coll\") or a bag-of-word model (\"bow\")");
		
		System.out.println("");
		System.exit(0);
	}
}
