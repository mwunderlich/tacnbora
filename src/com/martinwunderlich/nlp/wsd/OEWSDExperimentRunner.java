package com.martinwunderlich.nlp.wsd;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

public class OEWSDExperimentRunner {
	// Global vars to keep track of maximum and minimum values in results table
	private static DescriptiveStatistics accuraciesMean = new DescriptiveStatistics();
	private static DescriptiveStatistics precisionsMean = new DescriptiveStatistics();
	private static DescriptiveStatistics recallsMean = new DescriptiveStatistics();
	private static DescriptiveStatistics F1sMean = new DescriptiveStatistics();
	private static DescriptiveStatistics accuraciesStdDev = new DescriptiveStatistics();
	private static DescriptiveStatistics precisionsStdDev = new DescriptiveStatistics();
	private static DescriptiveStatistics recallsStdDev = new DescriptiveStatistics();
	private static DescriptiveStatistics F1sStdDev = new DescriptiveStatistics();
	private static String[] maxValues = null;
	private static String[] minValues = null;
	
	// Command line args for calling OEWSD:
	//
	// train boc
	// /Users/martinwunderlich/git/WSD/WordSenseDisambiguation/data/corpora/DOE_corpus/trainingData/concordances_boc_annotated_reviewed_oneVsAll_A.txt
	// mostfreq,rnd,nb,me
	// /Users/martinwunderlich/git/WSD/WordSenseDisambiguation/data/mallet/classifiers/@@DATETIME@@_mostfreq+rnd+nb+me_boc_bow_Avall_1-20.classifier
	// /Users/martinwunderlich/git/WSD/WordSenseDisambiguation/data/results/@@DATETIME@@_mostfreq+rnd+nb+me_boc_bow_Avall_1-20.csv
	// 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20 nocoll
			
	public static void main(String[] args) {
		Map<String, String> resultsFiles = new HashMap<>();
		List<String[]> experimentConfigs = new ArrayList<>();
		
		//String[] targetTerms 	= { "faeder", "fultum" };
		String[] targetTerms 	= { "are", "anweald", "fultum" };
		String[] vectorTypes 	= { "bow", "coll" };
		String[] algos 			= { "rnd", "mostfreq", "nb","me" };
		
		String trainingDataFileBasePath = "/Users/martinwunderlich/git/WSD/WordSenseDisambiguation/data/corpora/DOE_corpus/trainingData/concordances_@@TERM@@_annotated_reviewed@@EXPNAME@@.txt";
		String classifierOutputFileBasePath = "/Users/martinwunderlich/git/WSD/WordSenseDisambiguation/data/mallet/classifiers/@@DATETIME@@_@@ALGO@@_@@TERM@@_@@EXPNAME@@_@@VECTORTYPE@@_1-20.classifier";
		String resultsFileBasePath = "/Users/martinwunderlich/git/WSD/WordSenseDisambiguation/data/results/2015-07-11_@@ALGO@@_@@TERM@@_@@EXPNAME@@_@@VECTORTYPE@@_1-20.csv";
		
		Map<String, String> experimentNames = new LinkedHashMap<>();
		experimentNames.put("_oneVsAll_A", "A vs. not A");
		experimentNames.put("_oneVsAll_B", "B vs. not B");
		experimentNames.put("_oneVsAll_B1", "B1 vs. not B1");
		experimentNames.put("_oneVsAll_B2", "B2 vs. not B2");
		experimentNames.put("_oneVsAll_C", "C vs. not C");
		experimentNames.put("_oneVsAll_1", "1 vs. not 1");
		experimentNames.put("_oneVsAll_2", "2 vs. not 2");
		experimentNames.put("_oneVsAll_2a", "2a vs. not 2a");
		experimentNames.put("_oneVsAll_2b", "2b vs. not 2b");
		experimentNames.put("_1_Vs_2", "1 vs. 2");
		//experimentNames.put("noC_noX", "multi");
		experimentNames.put("", "multi");
		
		/////////////////////////
		// Build configuration //
		/////////////////////////
		for(String term : targetTerms) {
			for(String algo : algos) {
				for(String expName : experimentNames.keySet()) {
					String trainingDataFile = trainingDataFileBasePath.replace("@@TERM@@", term).replace("@@EXPNAME@@", expName);
					
					if(! new File(trainingDataFile).exists()) {
						System.out.println("Skipping non-existing training file " + trainingDataFile);
						continue;
					}
					
					System.out.println("Using training file " + trainingDataFile);
					
					for(String vectorType : vectorTypes) {
						String classifierOutputFile = classifierOutputFileBasePath.replace("@@TERM@@", term).replace("@@EXPNAME@@", expName).replace("@@ALGO@@", algo).replace("@@VECTORTYPE@@", vectorType);
						String resultsFile = resultsFileBasePath.replace("@@TERM@@", term).replace("@@EXPNAME@@", expName).replace("@@ALGO@@", algo).replace("@@VECTORTYPE@@", vectorType);
						
						String[] config = new String[8];
						
						config[0] = "train";
						config[1] = term;
						config[2] = trainingDataFile;
						config[3] = algo;
						config[4] = classifierOutputFile;
						config[5] = resultsFile;
						config[6] = "1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20";
						config[7] = vectorType;
						
						experimentConfigs.add(config);
						String fileID = getFileID(term, expName, algo, vectorType);
						resultsFiles.put(fileID, resultsFile);
					}
				}
			}
		}
		
		//////////////////////
		// Generate results //
		//////////////////////
		int i = 0;
		for(String[] expConfig : experimentConfigs) {
			System.out.println("Parameter configuration " + i + ": ");
			System.out.println(Arrays.toString(expConfig));
			System.out.println("----------------------------------------------------------");
			OEWSD.execTrainMalletClassifier(expConfig);
			
			i++;
		}
		
		
		///////////////////////
		// Aggregate results //
		///////////////////////
				
		for(String term : targetTerms) {
			StringBuffer latexTable = new StringBuffer();
			List<String[]> resultsTableLines = new ArrayList<>();
			createTableHeader(latexTable);
			
			for(String algo : algos) {
				for(String expName : experimentNames.keySet()) {
					for(String vectorType : vectorTypes) {
						String fileID = getFileID(term, expName, algo, vectorType);
						
						String resultsFilePath = resultsFiles.get(fileID);
						if(resultsFilePath == null || resultsFilePath.isEmpty())
							continue;
						
						String[] resultsTableLine = getAggregateResults(algo, experimentNames.get(expName), vectorType, resultsFilePath);
						if(resultsTableLine == null)
							continue;
						
						resultsTableLines.add(resultsTableLine);
					}
				}
			}
			
			maxValues = getMaxValues();
			minValues = getMinValues();
			
			for(String[] resultsTableLine : resultsTableLines) {
				latexTable.append( toLatexLine(resultsTableLine, true /* highlightMaxMin */) );
				latexTable.append("\n");
			}
			
			createTableFooter(term, latexTable);
			
			System.out.println(latexTable.toString());
			System.out.println("DONE for term " + term);
		}
		
		
		System.out.println("DONE.");
	}

	private static String[] getMinValues() {
		String[] minVals = new String[11];
		
		minVals[0] = "";
		minVals[1] = "";
		minVals[2] = "";
		minVals[3] = doubleToString( accuraciesMean.getMin() );
		minVals[4] = doubleToString( accuraciesStdDev.getMin() );
		minVals[5] = doubleToString( precisionsMean.getMin() );
		minVals[6] = doubleToString( precisionsStdDev.getMin() );
		minVals[7] = doubleToString( recallsMean.getMin() );
		minVals[8] = doubleToString( recallsStdDev.getMin() );
		minVals[9] = doubleToString( F1sMean.getMin() );
		minVals[10] = doubleToString( F1sStdDev.getMin() );
		
		return minVals;
	}

	private static String[] getMaxValues() {
		String[] maxVals = new String[11];
		
		maxVals[0] = "";
		maxVals[1] = "";
		maxVals[2] = "";
		maxVals[3] = doubleToString( accuraciesMean.getMax() );
		maxVals[4] = doubleToString( accuraciesStdDev.getMax() );
		maxVals[5] = doubleToString( precisionsMean.getMax() );
		maxVals[6] = doubleToString( precisionsStdDev.getMax() );
		maxVals[7] = doubleToString( recallsMean.getMax() );
		maxVals[8] = doubleToString( recallsStdDev.getMax() );
		maxVals[9] = doubleToString( F1sMean.getMax() );
		maxVals[10] = doubleToString( F1sStdDev.getMax() );
		
		return maxVals;
	}

	private static void createTableFooter(String term, StringBuffer latexTable) {
		latexTable.append("\\hline\n");
		latexTable.append("\\end{tabular}\n");
		latexTable.append(String.format("\\caption{APPENDIX - detailed results for target term ``%s'' (maximum and minimum highlighted in bold and italics, respectively)}\n", term));
		latexTable.append("\\label{table:doe_target_words}\n");
		latexTable.append("\\end{table*}\n");
	}

	/**
	 * @param resultsTableHeader
	 * @param latexTable
	 */
	private static void createTableHeader(StringBuffer latexTable) {
		latexTable.append("\\begin{table*}[htb]\n");
		latexTable.append("\\scriptsize\n");
		latexTable.append("\\centering\n");
		latexTable.append("\\begin{tabular}{l l l r r r r r r r r}\n");
		latexTable.append("\\hline\n");
		latexTable.append("\\textbf{Training algorithm} & \\textbf{Classification type} & \\textbf{Vector type} & \\multicolumn{2}{c}{\\textbf{Accuracy}} & \\multicolumn{2}{c}{\\textbf{Precision}} & \\multicolumn{2}{c}{\\textbf{Recall}} & \\multicolumn{2}{c}{\\textbf{F1}} \\\\ \n");
		latexTable.append(" & & & Avg & Std Dev & Avg & Std Dev & Avg & Std Dev & Avg & Std Dev \\\\ \n");
		latexTable.append("\\hline\n");
	}

	private static String[] getAggregateResults(String algo, String name, String vectorType, String resultsFilePath) {
		String[] results = new String[11];
		
		DescriptiveStatistics accuracies = new DescriptiveStatistics();
		DescriptiveStatistics precisions = new DescriptiveStatistics();
		DescriptiveStatistics recalls = new DescriptiveStatistics();
		DescriptiveStatistics F1s = new DescriptiveStatistics();
		
		try {
			File resultsFile = new File(resultsFilePath);
			if(!resultsFile.exists())
				return null;
			
			List<String> lines = FileUtils.readLines(resultsFile, "UTF-8");
		
			for(String line : lines) {
				int s = 5;
				String[] partsOfLine = line.split(";");
				
				if(partsOfLine[0].equals("Training algorithm"))
					continue;
				
				accuracies.addValue( Double.parseDouble(partsOfLine[s]) );
				precisions.addValue( Double.parseDouble(partsOfLine[++s]) );
				recalls.addValue( Double.parseDouble(partsOfLine[++s]) );
				F1s.addValue( Double.parseDouble(partsOfLine[++s]) );
			}
			
			// Add to global values
			accuraciesMean.addValue(accuracies.getMean());
			accuraciesStdDev.addValue(accuracies.getStandardDeviation());
			precisionsMean.addValue(precisions.getMean());
			precisionsStdDev.addValue(precisions.getStandardDeviation());
			recallsMean.addValue(recalls.getMean());
			recallsStdDev.addValue(recalls.getStandardDeviation());
			F1sMean.addValue(F1s.getMean());
			F1sStdDev.addValue(F1s.getStandardDeviation());
			
			// Build list for results generation
			List<Double> values = new ArrayList<>();
			values.add(accuracies.getMean());
			values.add(accuracies.getStandardDeviation());
			values.add(precisions.getMean());
			values.add(precisions.getStandardDeviation());
			values.add(recalls.getMean());
			values.add(recalls.getStandardDeviation());
			values.add(F1s.getMean());
			values.add(F1s.getStandardDeviation());
			
			results[0] = algo;
			results[1] = name;
			results[2] = vectorType;
			
			for(int i = 0; i < values.size(); i++) {
				double value = values.get(i);
				String result = doubleToString(value);
				
				results[i+3] = result;
			}
			
		} catch (IOException e) {
			System.out.println("ERROR while trying to parse file " + resultsFilePath);
			e.printStackTrace();
		}
		
		return results;
	}

	/**
	 * @param value
	 * @return
	 */
	private static String doubleToString(double value) {
		DecimalFormat formatter = (DecimalFormat) DecimalFormat.getInstance(new Locale("en", "GB"));
		formatter.applyPattern("#.##");

		String result = formatter.format(value);
		if(result.length() != 4)
			result += "0"; 	// append trailing zero where needed
		return result;
	}
	

	private static String toLatexLine(String[] results, boolean highlight) {
		String line = "";
		boolean first = true;
		
		for(int i = 0; i < results.length; i++) {
			String str = results[i];
			
			if(highlight) {
				if(str.equals(maxValues[i]))
					str = highlightMax(str);
				else if(str.equals(minValues[i]))
					str = highlightMin(str);
			}
			
			if(first) {
				line += str;
				first = false;
			}
			else
				line += " & " + str;
		}
		
		line += " \\\\";
		
		return line;
	}

	private static String highlightMin(String str) {
		return "\\textit{" + str + "}";
	}

	private static String highlightMax(String str) {
		return "\\textbf{" + str + "}";
	}

	private static String getFileID(String term, String expName, String algo, String vectorType) {
		return algo + "_" + term + "_" + expName + "_" + vectorType;
	}

}
