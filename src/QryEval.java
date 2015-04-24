/**
 *  QryEval illustrates the architecture for the portion of a search
 *  engine that evaluates queries.  It is a template for class
 *  homework assignments, so it emphasizes simplicity over efficiency.
 *  It implements an unranked Boolean retrieval model, however it is
 *  easily extended to other retrieval models.  For more information,
 *  see the ReadMe.txt file.
 *
 *  Copyright (c) 2015, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.SortedSet;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.Analyzer.TokenStreamComponents;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

class FV {
	String exDocId;
	int docRel;
	int qid;
	ArrayList<Double> featureVector;
	boolean[] featureFlag;

	FV(String exDocId, int docRel, int qid, ArrayList<Double> featureVector,
			boolean[] featureFlag) {
		this.exDocId = exDocId;
		this.docRel = docRel;
		this.qid = qid;
		this.featureVector = featureVector;
		this.featureFlag = featureFlag;
	}
}

public class QryEval {

	static String QUERYFILE = "";
	static String OUTPUTFILE = "";
	static String MODEL = "";

	// query expansion: pseudo relevance feedback
	static Boolean fb = false;// could be empty
	static String fbInitialRankingFile = null;//
	static int fbDocs = 0;
	static int fbTerms = 0;
	static int fbMu = 0;
	static double fbOrigWeight = 0.0;
	static String fbExpansionQueryFile = "";

	// LeToR
	public static int featureNum = 18;
	public static String trainingQueryFile;
	public static String trainingQrelsFile;
	public static String trainingFeatureVectorsFile;
	public static String pageRankFile;
	public static boolean[] featureDisable = new boolean[featureNum];
	public static String svmRankLearnPath;
	public static String svmRankClassifyPath;
	public static double svmRankParamC;
	public static String svmRankModelFile;
	public static String testingFeatureVectorsFile;
	public static String testingDocumentScores;

	static String usage = "Usage:  java "
			+ System.getProperty("sun.java.command") + " paramFile\n\n";

	public static IndexReader READER;
	public static DocLengthStore s;
	public static HashMap<String, Integer> inExhm = new HashMap<String, Integer>();
	public static HashMap<Integer, HashMap<String, Integer>> trainRel = new HashMap<Integer, HashMap<String, Integer>>();
	public static HashMap<String, Double> pageRankScore = new HashMap<String, Double>();

	public static EnglishAnalyzerConfigurable analyzer = new EnglishAnalyzerConfigurable(
			Version.LUCENE_43);
	private static ArrayList<String> queries;
	private static boolean isBM25Model = false;
	private static boolean isLETOR = false;
	private static boolean isIndriModel;
	private static boolean isWAND;
	private static boolean isWSUM;
	static {
		analyzer.setLowercase(true);
		analyzer.setStopwordRemoval(true);
		analyzer.setStemmer(EnglishAnalyzerConfigurable.StemmerType.KSTEM);
	}

	public static RetrievalModel configModel(HashMap<String, String> params)
			throws Exception {
		RetrievalModel model = null;
		if (MODEL.equals("UnrankedBoolean")) {
			model = new RetrievalModelUnrankedBoolean();
		} else if (MODEL.equals("RankedBoolean")) {
			model = new RetrievalModelRankedBoolean();
		} else if (MODEL.equals("BM25")) {
			model = new BM25RetrievalModel();
			isBM25Model = true;
			FileUtil.checkParams("BM25:k_1", params);
			FileUtil.checkParams("BM25:b", params);
			FileUtil.checkParams("BM25:k_3", params);
			model.setParameter("BM25:k_1",
					Float.parseFloat(params.get("BM25:k_1")));
			model.setParameter("BM25:k_3",
					Float.parseFloat(params.get("BM25:k_3")));
			model.setParameter("BM25:b", Float.parseFloat(params.get("BM25:b")));
		} else if (MODEL.equals("Indri")) {
			model = new IndriRetrievalModel();
			isIndriModel = true;
			model.setParameter("Indri:lambda",
					Float.parseFloat(params.get("Indri:lambda")));
			model.setParameter("Indri:mu",
					Float.parseFloat(params.get("Indri:mu")));
		} else if (MODEL.equals("letor")) {
			isLETOR = true;
			model = new BM25RetrievalModel();
			isBM25Model = true;
			trainingQueryFile = params.get("letor:trainingQueryFile");
			trainingQrelsFile = params.get("letor:trainingQrelsFile");
			trainingFeatureVectorsFile = params
					.get("letor:trainingFeatureVectorsFile");
			pageRankFile = params.get("letor:pageRankFile");
			if (params.containsKey("letor:featureDisable")) {
				if (params.get("letor:featureDisable").equals("")) {

				} else {
					String[] ds = params.get("letor:featureDisable").split(",");
					for (int i = 0; i < ds.length; i++) {
						featureDisable[Integer.parseInt(ds[i]) - 1] = true;
					}
				}
			}
			readJudgeRel();// trainRel, inExhm(external id, internal id)
			readPageRank();// pageRankScore(external id, pg score)
			svmRankLearnPath = params.get("letor:svmRankLearnPath");
			svmRankClassifyPath = params.get("letor:svmRankClassifyPath");
			svmRankParamC = Double.parseDouble(params
					.get("letor:svmRankParamC"));
			svmRankModelFile = params.get("letor:svmRankModelFile");
			testingFeatureVectorsFile = params
					.get("letor:testingFeatureVectorsFile");
			testingDocumentScores = params.get("letor:testingDocumentScores");
			model.setParameter("BM25:k_1",
					Float.parseFloat(params.get("BM25:k_1")));
			model.setParameter("BM25:k_3",
					Float.parseFloat(params.get("BM25:k_3")));
			model.setParameter("BM25:b", Float.parseFloat(params.get("BM25:b")));
			// set BM25 params
			BM25RetrievalModel.B = Float.parseFloat(params.get("BM25:b"));
			BM25RetrievalModel.K1 = Float.parseFloat(params.get("BM25:k_1"));
			BM25RetrievalModel.K3 = Float.parseFloat(params.get("BM25:k_3"));
			// set Indri params
			IndriRetrievalModel.Lambda = Float.parseFloat(params
					.get("Indri:lambda"));
			IndriRetrievalModel.Mu = Float.parseFloat(params.get("Indri:mu"));

		} else {
			FileUtil.fatalError("Error: retrieval model has not been found!");
		}
		return model;
	}

	public static RetrievalModel configParams(String[] args) throws Exception {
		HashMap<String, String> params = new HashMap<String, String>();
		FileUtil.readParams(args[0], params);
		FileUtil.checkFileParams(params);
		QUERYFILE = params.get("queryFilePath");
		OUTPUTFILE = params.get("trecEvalOutputPath");
		MODEL = params.get("retrievalAlgorithm");
		if (params.containsKey("fb")) {
			fb = Boolean.parseBoolean(params.get("fb"));
			if (fb) {
				if (params.containsKey("fbInitialRankingFile")) {
					fbInitialRankingFile = params.get("fbInitialRankingFile");
				}
				fbDocs = Integer.parseInt(params.get("fbDocs"));
				fbTerms = Integer.parseInt(params.get("fbTerms"));
				fbMu = Integer.parseInt(params.get("fbMu"));
				fbOrigWeight = Double.parseDouble(params.get("fbOrigWeight"));
				fbExpansionQueryFile = params.get("fbExpansionQueryFile");
				FileUtil.writeToFile("", fbExpansionQueryFile);
			}
		}
		// open the index
		READER = DirectoryReader.open(FSDirectory.open(new File(params
				.get("indexPath"))));
		if (READER == null) {
			FileUtil.fatalError(usage);
		}
		s = new DocLengthStore(READER);

		// config model
		RetrievalModel model = configModel(params);
		return model;
	}

	public static ArrayList<Double> createEmptyFv(int inDocId, String exDocId,
			String[] stem, boolean[] featureFlag) throws Exception {
		ArrayList<Double> fv = new ArrayList<Double>();
		for (int i = 0; i < featureNum; i++) {
			fv.add(0.0);
		}
		// int inDocId = inExhm.get(exDocId);
		Document d = READER.document(inDocId);
		String rawUrl = d.get("rawUrl");
		int fi = 0;
		// 1. get spam score
		if (!featureDisable[fi]) {
			fv.set(fi, Double.parseDouble(d.get("score")));
		}
		fi++;
		// 2.url depth for d
		if (!featureDisable[fi]) {
			int urlLen = 0;
			for (int i = 0; i < rawUrl.length(); i++) {
				if (rawUrl.charAt(i) == '/')
					urlLen++;
			}
			fv.set(fi, (double) urlLen);
		}
		fi++;
		// f3:wiki score
		if (!featureDisable[fi]) {
			String[] urlParse = rawUrl.split("/");
			for (int i = 0; i < urlParse.length; i++) {
				if (urlParse[i].contains("wikipedia.org")) {
					fv.set(fi, 1.0);
					break;
				}
			}
		}
		fi++;
		// f4: PAGE RANK SCORE
		if (!featureDisable[fi]) {
			if (pageRankScore.containsKey(exDocId)) {
				fv.set(fi, pageRankScore.get(exDocId));
			} else {
				featureFlag[fi] = true;
			}
		}
		fi++;
		// body field
		String fieldName = "body";
		fi = processFeatureComb(fieldName, inDocId, fi, featureFlag, stem, fv);
		fieldName = "title";
		fi = processFeatureComb(fieldName, inDocId, fi, featureFlag, stem, fv);
		fieldName = "url";
		fi = processFeatureComb(fieldName, inDocId, fi, featureFlag, stem, fv);
		fieldName = "inlink";
		fi = processFeatureComb(fieldName, inDocId, fi, featureFlag, stem, fv);
		// 17:
		fi=execCustomFv10(fi, inDocId, fieldName, featureFlag, stem, fv);
		// 18:
		fi=execCustomFv5(rawUrl, fi, fv);
		return fv;
	}

	public static int execCustomFv5(String rawUrl, int fi, ArrayList<Double> fv) {
		String[] urlParse = rawUrl.split("/");
//		System.err.println(rawUrl);
		for (int i = 0; i < urlParse.length; i++) {
			if (urlParse[i].contains(".org") || urlParse[i].contains(".edu")
					|| urlParse[i].contains(".gov")) {
				fv.set(fi, 1.0);
				break;
			}
		}
		return fi + 1;
	}
	public static int execCustomFv10(int fi, int inDocId, String fieldName,
			boolean[] featureFlag, String[] stem, ArrayList<Double> fv)
			throws IOException {
		double score = Letor.getWeightedTfIdfScore(inDocId, stem);
		if (!featureDisable[fi++]) {
			if (score == 0.0) {
				featureFlag[fi-1] = true;
			} else {
				fv.set(fi - 1, score);
			}
		}
		return fi;
	}

	// TF test
	public static void execCustomFv1(int fi, int inDocId, String fieldName,
			boolean[] featureFlag, String[] stem, ArrayList<Double> fv)
			throws IOException {
		if (!featureDisable[fi++]) {
			Terms terms = QryEval.READER.getTermVector(inDocId, fieldName);
			if (terms == null) {
				// field doesn't exist!
				featureFlag[fi++] = true;
			} else {
				fv.set(fi - 1, Letor.getTF(inDocId, stem, fieldName));
			}
		}
	}

	// inlink test
	public static void execCustomFv2(int fi, int inDocId, String fieldName,
			boolean[] featureFlag, String[] stem, ArrayList<Double> fv)
			throws IOException {
		if (!featureDisable[fi++]) {
			Terms terms = QryEval.READER.getTermVector(inDocId, "inlink");
			if (terms == null)
				fv.set(fi - 1, 0.0);
			else
				fv.set(fi - 1, 1.0);
		}
	}

	

	// field cnt
	public static void execCustomFv3(int fi, int inDocId, String fieldName,
			boolean[] featureFlag, String[] stem, ArrayList<Double> fv)
			throws IOException {
		if (!featureDisable[fi++]) {
			fv.set(fi - 1, Letor.getFieldCnt(inDocId));
		}
	}

	// field
	public static void execCustomFv4(int fi, int inDocId, String fieldName,
			boolean[] featureFlag, String[] stem, ArrayList<Double> fv)
			throws IOException {
		if (!featureDisable[fi++]) {
			Terms terms = QryEval.READER.getTermVector(inDocId, "body");
			if (terms == null) {
				featureFlag[fi - 1] = true;
			} else {
				TermVector docVec = new TermVector(inDocId, "body");
				fv.set(fi - 1, Letor.getTfIdfScore(inDocId, stem, docVec));
			}
		}
	}

	// body svm
	public static void execCustomFv7(int fi, int inDocId, String fieldName,
			boolean[] featureFlag, String[] stem, ArrayList<Double> fv)
			throws IOException {
		if (!featureDisable[fi++]) {
			Terms terms = QryEval.READER.getTermVector(inDocId, "body");
			if (terms == null) {
				featureFlag[fi - 1] = true;
			} else {
				TermVector docVec = new TermVector(inDocId, "body");
				fv.set(fi - 1, Letor.getVSMScore(inDocId, stem, docVec));
			}
		}
	}

	// // diveristy
	// public static void execCustomFv8(int fi, int inDocId, String fieldName,
	// boolean[] featureFlag, String[] stem, ArrayList<Double> fv)
	// throws IOException {
	// if (!featureDisable[fi++]) {
	// StringBuffer sb = new StringBuffer();
	// for (int i = 0; i < stem.length; i++) {
	// sb.append(stem[i] + " ");
	// }
	// String query = "#sum(" + sb.toString() + ")";
	// Qryop qTree = parseQuery(query);
	// System.err.println(qTree.toString());
	// RetrievalModel model = new BM25RetrievalModel();
	// model.setParameter("BM25:k_1", 1.2);
	// model.setParameter("BM25:k_3", 0);
	// model.setParameter("BM25:b", 0.75);
	// QryResult immediate_result = qTree.evaluate(model);
	// HashMap<Integer, String> inExHm = null;
	// inExHm = creatInExMap(immediate_result);
	// immediate_result.docScores.sortByScoresAndDocID(inExHm);
	// // need sort first for ranked model
	// int KTOP=3;
	// double score=0.0;
	// for (int i = 0; i < Math.min(
	// immediate_result.docScores.scores.size(), KTOP); i++) {
	// int bm25docid=immediate_result.docScores.getDocid(i);
	// score+=1.0*calDistance(inDocId, bm25docid);
	// }
	// fv.set(fi - 1,score);
	// }
	// }
	// public static int calDistance(int reldocid, int bm25docid) throws
	// IOException{
	// Terms terms = QryEval.READER.getTermVector(reldocid, "body");
	// if(terms==null){
	// System.err.println(reldocid);
	// return 0;
	// }
	// int overlap=0;
	// TermVector tv1 = new TermVector(reldocid, "body");
	// TermVector tv2 = new TermVector(bm25docid, "body");
	//
	// for(int i=0;i<tv1.positionsLength();i++){
	// int index1=tv1.stemAt(i);
	// int tf1=tv1.stemFreq(index1);
	// for(int j=0;j<tv2.positionsLength();j++){
	// int index2=tv2.stemAt(j);
	// int tf=tv2.stemFreq(index2);
	// int idx = tv1.getStemIdx(tv2.stemString(index2));
	// if (idx == -1) {
	//
	// }else{
	// overlap++;
	// }
	// }
	// }
	// return overlap;
	// }
	// field count
	public static void execCustomFv6(int fi, int inDocId, String fieldName,
			boolean[] featureFlag, String[] stem, ArrayList<Double> fv)
			throws IOException {
		if (!featureDisable[fi++]) {
			double score = 0.0;
			Terms terms = QryEval.READER.getTermVector(inDocId, "title");
			if (terms != null)
				score += 1.0;
			terms = QryEval.READER.getTermVector(inDocId, "url");
			if (terms != null)
				score += 1.0;
			terms = QryEval.READER.getTermVector(inDocId, "inlink");
			if (terms != null)
				score += 1.0;
			if (score == 0.0) {
				featureFlag[fi - 1] = true;
			} else {
				fv.set(fi - 1, score);
			}
		}
	}

	public static int processFeatureComb(String fieldName, int inDocId, int fi,
			boolean[] featureFlag, String[] stem, ArrayList<Double> fv)
			throws Exception {
		Terms terms = QryEval.READER.getTermVector(inDocId, fieldName);
		if (terms == null) {
			// field doesn't exist!
			for (int i = 0; i < 3; i++) {
				featureFlag[fi++] = true;
			}
		} else {
			// f5:bm25 score
			if (!featureDisable[fi++]) {
				fv.set(fi - 1, Letor.getBM25Score(inDocId, stem, fieldName));
			}
			// f6: indri score
			if (!featureDisable[fi++]) {
				fv.set(fi - 1, Letor.getIndriScore(inDocId, stem, fieldName));
			}
			// f7:overlap score
			if (!featureDisable[fi++]) {
				fv.set(fi - 1, Letor.getOverlapScore(inDocId, stem, fieldName));
			}
		}
		return fi;
	}

	public static void initFv(ArrayList<Double> minfv, ArrayList<Double> maxfv) {
		for (int i = 0; i < featureNum; i++) {
			minfv.add(Double.MAX_VALUE);
			maxfv.add(-Double.MAX_VALUE);
		}
	}

	public static void clearFv(ArrayList<Double> minfv, ArrayList<Double> maxfv) {
		for (int i = 0; i < featureNum; i++) {
			minfv.set(i, Double.MAX_VALUE);
			maxfv.set(i, Double.MIN_VALUE);
		}
	}

	public static void generateTrainD() throws Exception {
		ArrayList<Double> minfv = new ArrayList<Double>();
		ArrayList<Double> maxfv = new ArrayList<Double>();
		initFv(minfv, maxfv);
		FileUtil.writeToFile("", trainingFeatureVectorsFile);
		List<String> tqs = FileUtil.readQueryFile(trainingQueryFile);
		for (int i = 0; i < tqs.size(); i++) {
			List<FV> fvl = new ArrayList<FV>();
			// minfv, maxfv is corresponding to one query!!!
			String tq = tqs.get(i);
			int qid = Integer.parseInt(tq.split(":")[0]);
			String query = tq.split(":")[1];
			// use QryEval.tokenizeQuery to stop & stem the query
			String[] stemQuery = tokenizeQuery(query);
			HashMap<String, ArrayList<Double>> trainfv = new HashMap<String, ArrayList<Double>>();
			HashMap<String, boolean[]> trainflag = new HashMap<String, boolean[]>();
			Iterator<Entry<String, Integer>> it = trainRel.get(qid).entrySet()
					.iterator();
			while (it.hasNext()) {
				boolean[] featureFlag = Arrays.copyOf(featureDisable,
						featureNum);
				String exDocId = it.next().getKey();
				int inDocId = inExhm.get(exDocId);
				ArrayList<Double> featureVector = createEmptyFv(inDocId,
						exDocId, stemQuery, featureFlag);
				updateMinMaxFV(featureVector, minfv, maxfv, featureFlag);
				trainfv.put(exDocId, featureVector);
				trainflag.put(exDocId, featureFlag);
			}
			// normalize the feature values for query q to [0..1]
			it = trainRel.get(qid).entrySet().iterator();
			while (it.hasNext()) {
				Entry<String, Integer> pairs = it.next();
				String exDocId = pairs.getKey();
				int docRel = pairs.getValue();
				ArrayList<Double> featureVector = trainfv.get(exDocId);
				boolean[] featureFlag = trainflag.get(exDocId);
				normalizeFV(featureVector, minfv, maxfv, featureFlag);
				// appendFv(exDocId, docRel, qid, featureVector,
				// trainingFeatureVectorsFile);
				FV a = new FV(exDocId, docRel, qid, featureVector, featureFlag);
				fvl.add(a);
			}
			// we don't need to do so since SVM consider all of them as the same
			Collections.sort(fvl, new Comparator<FV>() {
				public int compare(FV f1, FV f2) {
					if (f1.exDocId.compareTo(f2.exDocId) > 0)
						return 1;
					else
						return 0;
				}
			});
			writeFvList(trainingFeatureVectorsFile, fvl);
			clearFv(minfv, maxfv);
		}
	}

	public static QryResult execInitalBM25(String strs, RetrievalModel model)
			throws IOException {
		Qryop qTree = null;
		String query = strs.split(":")[1];
		if (isBM25Model) {
			query = "#sum(" + query + ")";
		} else if (isIndriModel) {
			query = "#and(" + query + ")";
		} else {
			query = "#and(" + query + ")";
		}
		qTree = parseQuery(query);
		System.err.println(qTree.toString());
		return qTree.evaluate(model);
	}

	public static void retrieveInitialDocs(QryResult result, String[] stem,
			ArrayList<Double> minfv, ArrayList<Double> maxfv,
			HashMap<String, ArrayList<Double>> testfv,
			HashMap<String, boolean[]> testflag) throws Exception {

		HashMap<Integer, String> inExHm = null;
		if (result != null) {
			inExHm = creatInExMap(result);
			result.docScores.sortByScoresAndDocID(inExHm);
		}
		for (int k = 0; k < Math.min(result.docScores.scores.size(), 100); k++) {
			boolean[] featureFlag = Arrays.copyOf(featureDisable, featureNum);
			String exDocId = inExHm.get(result.docScores.getDocid(k));
			// create an empty feature vector
			ArrayList<Double> featureVector = createEmptyFv(
					result.docScores.getDocid(k), exDocId, stem, featureFlag);
			updateMinMaxFV(featureVector, minfv, maxfv, featureFlag);
			testfv.put(exDocId, featureVector);
			testflag.put(exDocId, featureFlag);
		}
	}

	public static void updateMinMaxFV(ArrayList<Double> featureVector,
			ArrayList<Double> minfv, ArrayList<Double> maxfv,
			boolean[] featureFlag) {
		for (int j = 0; j < featureVector.size(); j++) {
			if (featureFlag[j]) {
				continue;
			}
			if (featureVector.get(j) < minfv.get(j)) {
				minfv.set(j, featureVector.get(j));
			}
			if (featureVector.get(j) > maxfv.get(j)) {
				maxfv.set(j, featureVector.get(j));
			}
		}
	}

	public static void normalizeFV(ArrayList<Double> featureVector,
			ArrayList<Double> minfv, ArrayList<Double> maxfv,
			boolean[] featureFlag) {
		// put feature vector into the collection
		for (int j = 0; j < featureVector.size(); j++) {
			double norm = 0.0;
			if (featureFlag[j]) {
				continue;
			}
			if (minfv.get(j) < maxfv.get(j)) {
				norm = (featureVector.get(j) - minfv.get(j))
						/ (maxfv.get(j) - minfv.get(j));
				featureVector.set(j, norm);
			} else if (minfv.get(j) == maxfv.get(j)) {
				featureVector.set(j, 0.0);
			}
		}
	}

	public static void generateTestD(RetrievalModel model) throws Exception {
		// generate testing data for top 100 documents in initial BM25 ranking

		ArrayList<Double> minfv = new ArrayList<Double>();
		ArrayList<Double> maxfv = new ArrayList<Double>();
		initFv(minfv, maxfv);
		queries = FileUtil.readQueryFile(QUERYFILE);
		FileUtil.writeToFile("", OUTPUTFILE);
		for (int i = 0; i < queries.size(); i++) {
			HashMap<String, ArrayList<Double>> testfv = new HashMap<String, ArrayList<Double>>();
			HashMap<String, boolean[]> testflag = new HashMap<String, boolean[]>();
			FileUtil.writeToFile("", testingFeatureVectorsFile);
			ReRankScore rrs = new ReRankScore();
			int qid = Integer.parseInt(queries.get(i).split(":")[0]);

			QryResult result = execInitalBM25(queries.get(i), model);

			String[] stem = tokenizeQuery(queries.get(i).split(":")[1]);
			retrieveInitialDocs(result, stem, minfv, maxfv, testfv, testflag);
			// normalize
			Iterator<Entry<String, ArrayList<Double>>> it = testfv.entrySet()
					.iterator();
			while (it.hasNext()) {
				Entry<String, ArrayList<Double>> pairs = it.next();
				String exDocId = pairs.getKey();
				ArrayList<Double> featureVector = pairs.getValue();
				boolean[] featureFlag = testflag.get(exDocId);
				// put feature vector into the collection
				normalizeFV(featureVector, minfv, maxfv, featureFlag);
				appendFv(exDocId, 0, qid, featureVector,
						testingFeatureVectorsFile, featureFlag);
				ReRankScore rr = new ReRankScore();
				rr.exDocid = exDocId;
				rrs.scores.add(rr);
			}
			clearFv(minfv, maxfv);
			rerank(rrs);
			printReRankedResult(rrs, qid);

		}
	}

	public static void writeFvList(String filename, List<FV> fvl)
			throws IOException {
		BufferedWriter bw = new BufferedWriter(new FileWriter(
				new File(filename), true));
		for (FV fv : fvl) {
			StringBuilder sb = new StringBuilder("");
			sb.append((fv.docRel) + " qid:" + fv.qid + " ");
			for (int i = 0; i < fv.featureVector.size(); i++) {
				if (fv.featureFlag[i]) {
					continue;
				}
				sb.append(Integer.toString(i + 1) + ":"
						+ Double.toString(fv.featureVector.get(i)) + " ");
			}
			sb.append("# " + fv.exDocId + "\n");
			bw.write(sb.toString());
		}
		bw.close();
	}

	public static void appendFv(String exDocId, int docRel, int qId,
			ArrayList<Double> fv, String filename, boolean[] featureFlag)
			throws IOException {
		BufferedWriter bw = new BufferedWriter(new FileWriter(
				new File(filename), true));
		StringBuilder sb = new StringBuilder("");
		sb.append(Integer.toString(docRel) + " qid:" + qId + " ");
		for (int i = 0; i < fv.size(); i++) {
			if (featureFlag[i])
				continue;
			sb.append(Integer.toString(i + 1) + ":"
					+ Double.toString(fv.get(i)) + " ");
		}
		sb.append("# " + exDocId + "\n");
		bw.write(sb.toString());
		bw.close();
	}

	public static void train() throws Exception {
		// call svmrank to train a model
		Process cmdProc = Runtime.getRuntime().exec(
				new String[] { svmRankLearnPath, "-c",
						String.valueOf(svmRankParamC),
						trainingFeatureVectorsFile, svmRankModelFile });
		callSVM(cmdProc);
	}

	// re-rank test data
	public static void rerank(ReRankScore rrs) throws Exception {
		// call svmrank to produce scores for the test data
		Process cmdProc = Runtime.getRuntime().exec(
				new String[] { svmRankClassifyPath, testingFeatureVectorsFile,
						svmRankModelFile, testingDocumentScores });
		callSVM(cmdProc);
		// read in the svmrank scores and re-rank the initial ranking based on
		// the scores
		BufferedReader br = new BufferedReader(new FileReader(new File(
				testingDocumentScores)));
		String scoreLine = null;
		// sort the scores
		int i = 0;
		while ((scoreLine = br.readLine()) != null) {
			if (scoreLine.equals("inf")) {
				rrs.scores.get(i).score = Double.MAX_VALUE;
			} else
				rrs.scores.get(i).score = Double.parseDouble(scoreLine);
			i++;
		}
		// output re-ranked result into trec_eval format
		rrs.sortByScoresAndDocID();
	}

	public static void printReRankedResult(ReRankScore rrs, int qid) {
		List<ReRankScore> finalResult = rrs.scores;
		NumberFormat formatter = new DecimalFormat("0.000000000000");
		StringBuilder sb = new StringBuilder("");
		for (int i = 0; i < finalResult.size(); i++) {
			String docID = finalResult.get(i).exDocid;
			String score = formatter.format(finalResult.get(i).score);
			sb.append(qid + " Q0 " + docID + " " + i + " " + score
					+ " zhiyuel\n");
		}
		FileUtil.appendToFile(sb.toString(), OUTPUTFILE);
	}

	public static void callSVM(Process cmdProc) throws Exception {
		BufferedReader stdoutReader = new BufferedReader(new InputStreamReader(
				cmdProc.getInputStream()));
		String inputLine;
		while ((inputLine = stdoutReader.readLine()) != null) {
			System.out.println(inputLine);
		}
		// consume stderr and print it for debugging purposes
		BufferedReader stderrReader = new BufferedReader(new InputStreamReader(
				cmdProc.getErrorStream()));
		while ((inputLine = stderrReader.readLine()) != null) {
			System.out.println(inputLine);
		}
		// get the return value from the executable. 0 means success, non-zero
		// indicates a problem
		int retValue = cmdProc.waitFor();
		if (retValue != 0) {
			throw new Exception("SVM Rank crashed.");
		}
	}

	public static void original(RetrievalModel model) throws Exception {
		queries = FileUtil.readQueryFile(QUERYFILE);
		Qryop qTree;
		StringBuffer sb = new StringBuffer();
		// wall clock time
		long startTime = System.currentTimeMillis(); //
		for (int i = 0; i < queries.size(); i++) {
			String strs = queries.get(i);
			String QueryID = strs.split(":")[0];
			String query = strs.split(":")[1];
			if (isBM25Model) {
				query = "#sum(" + query + ")";
			} else if (isIndriModel) {
				query = "#and(" + query + ")";
			} else {
				query = "#and(" + query + ")";
			}
			qTree = parseQuery(query);
			// empty terms cause score to be zero
			removeEmptyQueryTerms(qTree, 0);
			System.err.println(qTree.toString());
			QryResult org_result = qTree.evaluate(model);
			if (!fb) {
				// no query expansion
				sb.append(printResults(1, QueryID, "Q0", "run-1", query,
						org_result));
			} else {
				StringBuilder expandedQuery = new StringBuilder("#WAND(");
				HashMap<Integer, Double> topdocs = new HashMap<Integer, Double>();
				if (fbInitialRankingFile != null) {
					// read a document ranking in the trec_eval input format
					// from this file
					topdocs = readDocumentRanking(fbInitialRankingFile, QueryID);
				} else {
					topdocs = retrieveTopDocuments(org_result);
				}
				long corpus_len = READER.getSumTotalTermFreq("body");
				HashMap<String, Long> corpusFreqHashMap = new HashMap<String, Long>();
				HashMap<String, Double> termScoreHashMap = QueryExpansion
						.computeTermScore(s, topdocs, fbMu, "body", corpus_len,
								corpusFreqHashMap);
				SortedSet<Map.Entry<String, Double>> sorted = QueryExpansion
						.entriesSortedByValues(termScoreHashMap);
				QueryExpansion.expandQuery(sorted, fbTerms, expandedQuery);
				expandedQuery.append(")");
				StringBuffer stringbuffer = new StringBuffer(QueryID);
				stringbuffer.append(": ");
				stringbuffer.append(expandedQuery.toString());
				FileUtil.appendToFile(stringbuffer.toString() + "\n",
						fbExpansionQueryFile);
				String combinedQuery = "#wand ( " + fbOrigWeight + " " + query
						+ " " + (1 - fbOrigWeight) + " " + expandedQuery + " )";
				qTree = parseQuery(combinedQuery);
				System.err.println(qTree.toString());
				removeEmptyQueryTerms(qTree, 0);
				sb.append(printResults(1, QueryID, "Q0", "run-1",
						combinedQuery, qTree.evaluate(model)));
			}
		}
		long endTime = System.currentTimeMillis();
		System.err.println("running time of test query set: "
				+ (endTime - startTime) / (double) 1000 + "s");
		FileUtil.writeToFile(sb.toString(), OUTPUTFILE);
	}

	/**
	 * @param args
	 * 
	 *            The only argument is the path to the parameter file.
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		long startTime = System.currentTimeMillis(); //
		RetrievalModel model = configParams(args);
		if (isLETOR) {
			System.out.println("GENERATING DATA....");
			generateTrainD();
			System.out.println("TRAINING MODEL....");
			train();
			System.out.println("TEST ON QUEIRES....");
			generateTestD(model);
		} else {
			original(model);
		}
		long endTime = System.currentTimeMillis();
		System.err.println("running time of test query set: "
				+ (endTime - startTime) / (double) 1000 + "s");
		printMemoryUsage(false);
	}

	public static void readPageRank() throws Exception {
		BufferedReader br = new BufferedReader(new FileReader(pageRankFile));
		String line = "";
		while ((line = br.readLine()) != null) {
			String[] pgs = line.split("\\s+");
			String exId = pgs[0];
			double score = Double.parseDouble(pgs[1]);
			pageRankScore.put(exId, score);
		}
		br.close();

	}

	public static void readJudgeRel() throws Exception {
		BufferedReader br = new BufferedReader(
				new FileReader(trainingQrelsFile));
		String line = "";
		inExhm = new HashMap<String, Integer>();
		while ((line = br.readLine()) != null) {
			String[] trainingDocs = line.split(" ");
			int queryId = Integer.parseInt(trainingDocs[0]);
			String exDocId = trainingDocs[2];
			if (!inExhm.containsKey(exDocId)) {
				inExhm.put(exDocId, getInternalDocid(exDocId));
			}
			int relScore = Integer.parseInt(trainingDocs[3]);
			if (!trainRel.containsKey(queryId)) {
				trainRel.put(queryId, new HashMap<String, Integer>());
			}
			trainRel.get(queryId).put(exDocId, relScore);
		}
		br.close();

	}

	/**
	 * queryExpansion
	 * */
	public static HashMap<Integer, Double> readDocumentRanking(String file,
			String queryID) {
		HashMap<Integer, Double> ret = new HashMap<Integer, Double>();
		if (file == null)
			System.exit(1);
		Scanner scan;
		try {
			scan = new Scanner(new File(file));
			int cnt = 0;
			while (scan.hasNext()) {
				String line = scan.nextLine();
				String[] fields = line.split("\\s+");
				int docId = -1;
				if (fields[0].trim().equals(queryID)) {
					try {
						docId = getInternalDocid(fields[2]);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					double docScore = Double.parseDouble(fields[4]);
					ret.put(docId, docScore);
					cnt++;
				}
				if (cnt == fbDocs) {
					break;
				}
			}
			scan.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return ret;
	}

	/**
	 * Get the external document id for a document specified by an internal
	 * document id. If the internal id doesn't exists, returns null.
	 * 
	 * @param iid
	 *            The internal document id of the document.
	 * @throws IOException
	 */
	static String getExternalDocid(int iid) throws IOException {
		Document d = QryEval.READER.document(iid);
		String eid = d.get("externalId");
		return eid;
	}

	/**
	 * Finds the internal document id for a document specified by its external
	 * id, e.g. clueweb09-enwp00-88-09710. If no such document exists, it throws
	 * an exception.
	 * 
	 * @param externalId
	 *            The external document id of a document.s
	 * @return An internal doc id suitable for finding document vectors etc.
	 * @throws Exception
	 */
	static int getInternalDocid(String externalId) throws Exception {
		Query q = new TermQuery(new Term("externalId", externalId));

		IndexSearcher searcher = new IndexSearcher(QryEval.READER);
		TopScoreDocCollector collector = TopScoreDocCollector.create(1, false);
		searcher.search(q, collector);
		ScoreDoc[] hits = collector.topDocs().scoreDocs;

		if (hits.length < 1) {
			throw new Exception("External id not found.");
		} else {
			return hits[0].doc;
		}
	}

	/**
	 * parseQuery converts a query string into a query tree.
	 * 
	 * @param qString
	 *            A string containing a query.
	 * @param qTree
	 *            A query tree
	 * @throws IOException
	 */
	static Qryop parseQuery(String qString) throws IOException {
		Qryop currentOp = null;
		Stack<Qryop> stack = new Stack<Qryop>();
		// Add a default query operator to an unstructured query. This
		// is a tiny bit easier if unnecessary whitespace is removed.
		qString = qString.trim();
		if (qString.charAt(0) != '#') {
			qString = "#or(" + qString + ")";
		}
		// Tokenize the query.
		StringTokenizer tokens = new StringTokenizer(qString, "\t\n\r ,()",
				true);
		String token = null;
		// Each pass of the loop processes one token. To improve
		// efficiency and clarity, the query operator on the top of the
		// stack is also stored in currentOp.
		double weight = -1.0;
		while (tokens.hasMoreTokens()) {
			token = tokens.nextToken();
			Qryop top = null;
			if (!stack.isEmpty()) {
				top = stack.peek();
				if (top instanceof QryopSlWAndIndri) {
					setWAND(true);
				} else {
					setWAND(false);
				}
				if (top instanceof QryopSlWSumIndri) {
					setWSUM(true);
				} else {
					setWSUM(false);
				}
			}

			if (token.matches("[ ,(\t\n\r]")) {
				// Ignore most delimiters.
			}

			else if (token.equalsIgnoreCase("#and")) {
				if (isIndriModel) {
					if (weight != -1.0) {
						currentOp = new QryopSlAndIndri(weight);
						weight = -1.0;
					} else
						currentOp = new QryopSlAndIndri();
				} else {
					if (weight != -1.0) {
						currentOp = new QryopSlAnd(weight);
						weight = -1.0;
					} else
						currentOp = new QryopSlAnd();
				}
				stack.push(currentOp);
			} else if (token.equalsIgnoreCase("#wand")) {
				if (weight != -1.0) {
					currentOp = new QryopSlWAndIndri(weight);
					weight = -1.0;
				} else
					currentOp = new QryopSlWAndIndri();
				stack.push(currentOp);
			} else if (token.equalsIgnoreCase("#wsum")) {
				if (weight != -1.0) {
					currentOp = new QryopSlWSumIndri(weight);
					weight = -1.0;
				} else
					currentOp = new QryopSlWSumIndri();
				stack.push(currentOp);
			} else if (token.equalsIgnoreCase("#or")) {
				if (weight != -1.0) {
					currentOp = new QryopSlOr(weight);
					weight = -1.0;
				} else
					currentOp = new QryopSlOr();
				stack.push(currentOp);
			} else if (token.equalsIgnoreCase("#syn")) {
				if (weight != -1.0) {
					currentOp = new QryopIlSyn(weight);
					weight = -1.0;
				} else
					currentOp = new QryopIlSyn();
				stack.push(currentOp);
			} else if (token.equalsIgnoreCase("#sum")) {
				if (isBM25Model) {
					currentOp = new QryopSlSum();
				}
				stack.push(currentOp);
			} else if (token.toLowerCase().startsWith("#near")) {
				// #NEAR/3 (apple pie)
				if (token.contains("/")) {
					String[] stoken = token.split("/");
					int dist = Integer.parseInt(stoken[1]);
					if (weight != -1.0) {
						currentOp = new QryopIlNear(dist, weight);
						weight = -1.0;
					} else
						currentOp = new QryopIlNear(dist);
					stack.push(currentOp);
				} else
					System.err.println("invalid operator #near");
			} else if (token.toLowerCase().startsWith("#window")) {
				// #window/3 (apple pie)
				if (token.contains("/")) {
					String[] stoken = token.split("/");
					int dist = Integer.parseInt(stoken[1]);
					if (weight != -1.0) {
						currentOp = new QryopIlWindow(dist, weight);
						weight = -1.0;
					} else
						currentOp = new QryopIlWindow(dist);
					stack.push(currentOp);
				} else
					System.err.println("invalid operator #window");

			} else if (token.startsWith(")")) { // Finish current query
				stack.pop();
				if (stack.empty())
					break;
				Qryop arg = currentOp;
				currentOp = stack.peek();
				currentOp.add(arg);
			}
			// the position of this condition is critical to nested
			else if ((isWAND || isWSUM) && (weight == -1.0)) {
				// see as weight, not terms

				if (Pattern.matches("^[0-9]*\\.?[0-9]*$", token)) {
					weight = Double.parseDouble(token);
					// System.out.println(token);
				} else {
					if (isDouble(token)) {
						weight = Double.parseDouble(token);
					}
					// System.out.println(token);
					// System.out.println(weight);
				}
			} else {
				String[] preprocessToken = null;
				if (Pattern.matches("^[0-9]*\\.?[0-9]*$", token)
						|| isDouble(token)) {
					preprocessToken = tokenizeQuery(token);
					if (preprocessToken.length > 0) {
						if (weight != -1.0) {
							currentOp.add(new QryopIlTerm(preprocessToken[0],
									weight));
						} else {
							currentOp.add(new QryopIlTerm(preprocessToken[0]));
						}
					}
				} else {
					if (token.contains(".")) {
						String[] stoken = token.split("\\.");
						preprocessToken = tokenizeQuery(stoken[0]);
						// stopwords cause empty!!!
						if (preprocessToken.length > 0) {
							if (weight != -1.0)
								currentOp.add(new QryopIlTerm(
										preprocessToken[0], stoken[1], weight));
							else
								currentOp.add(new QryopIlTerm(
										preprocessToken[0], stoken[1]));
						}
					} else {
						// @eyre trick: after tokenizing, it could be empty.
						// (ie.stopwords)
						preprocessToken = tokenizeQuery(token);
						if (preprocessToken.length > 0)
							if (weight != -1.0)
								currentOp.add(new QryopIlTerm(
										preprocessToken[0], weight));
							else
								currentOp.add(new QryopIlTerm(
										preprocessToken[0]));
					}
				}
				weight = -1.0;
			}
		}
		if (tokens.hasMoreTokens()) {
			System.err
					.println("Error:  Query syntax is incorrect.  " + qString);
			return null;
		}

		return currentOp;
	}

	public static int removeEmptyQueryTerms(Qryop tree, int location) {
		if (tree instanceof QryopIlTerm) {
			return -1;
		} else {
			if (tree.args.size() < 1) {
				return location;
			} else {
				for (int index = 0; index < tree.args.size(); index++) {
					int toRemoveLocation = removeEmptyQueryTerms(
							tree.args.get(index), index);
					if (-1 != toRemoveLocation) {
						tree.args.remove(toRemoveLocation);
						--index;
					}
				}
			}
		}
		return -1;
	}

	private static boolean isDouble(String token) {
		try {
			Double.parseDouble(token);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	/**
	 * Print a message indicating the amount of memory used. The caller can
	 * indicate whether garbage collection should be performed, which slows the
	 * program but reduces memory usage.
	 * 
	 * @param gc
	 *            If true, run the garbage collector before reporting.
	 * @return void
	 */
	public static void printMemoryUsage(boolean gc) {
		Runtime runtime = Runtime.getRuntime();
		if (gc) {
			runtime.gc();
		}
		System.out
				.println("Memory used:  "
						+ ((runtime.totalMemory() - runtime.freeMemory()) / (1024L * 1024L))
						+ " MB");
	}

	/**
	 * create hashmap for internal document id and external document id, in
	 * order to improve the program's running time
	 * 
	 * @param QryResult
	 * @return key and value pair which stands for internal id and external id.
	 * 
	 * */
	static HashMap<Integer, String> creatInExMap(QryResult result) {
		HashMap<Integer, String> hm = new HashMap<Integer, String>();
		for (ScoreList.ScoreListEntry entry : result.docScores.scores) {
			try {
				hm.put(entry.getDocid(), getExternalDocid(entry.getDocid()));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return hm;
	}

	/**
	 * Print the query results.
	 * 
	 * THIS IS NOT THE CORRECT OUTPUT FORMAT. YOU MUST CHANGE THIS METHOD SO
	 * THAT IT OUTPUTS IN THE FORMAT SPECIFIED IN THE HOMEWORK PAGE, WHICH IS:
	 * 
	 * QueryID Q0 DocID Rank Score RunID
	 * 
	 * @param queryName
	 *            Original query.
	 * @param result
	 *            Result object generated by {@link Qryop#evaluate()}.
	 * @throws IOException
	 */
	static String printResults(int modelflag, String QueryID, String querymark,
			String runid, String queryName, QryResult result)
			throws IOException {
		HashMap<Integer, String> inExHm = null;
		if (result != null) {
			inExHm = creatInExMap(result);
			result.docScores.sortByScoresAndDocID(inExHm);
		}
		/*
		 * Create the trec_eval output. Your code should write to the file
		 * specified in the parameter file, and it should write the results that
		 * you retrieved above. This code just allows the testing infrastructure
		 * to work on QryEval.
		 */
		String ret = "";
		// System.out.println(queryName + ":  ");
		if (result.docScores.scores.size() < 1) {
			// System.out.println("\tNo results.");
			// 10 Q0 dummy 1 0 run-1
			return (QueryID + "\tQ0\tdummy\t1\t0\t" + runid + "\n");
		} else {
			// need sort first for ranked model
			for (int i = 0; i < Math.min(result.docScores.scores.size(), 100); i++) {
				// System.err.println(inExHm.get(result.docScores.getDocid(i)));
				ret += QueryID + "\t" + querymark + "\t"
						+ inExHm.get(result.docScores.getDocid(i)) + "\t"
						+ (i + 1) + "\t" + result.docScores.getDocidScore(i)
						+ "\t" + runid + "\n";
			}
		}
		return ret;
	}

	static HashMap<Integer, Double> retrieveTopDocuments(QryResult result) {
		HashMap<Integer, Double> ret = new HashMap<Integer, Double>();
		HashMap<Integer, String> inExHm = null;
		if (result != null) {
			inExHm = creatInExMap(result);
			result.docScores.sortByScoresAndDocID(inExHm);
		}
		if (result.docScores.scores.size() < 1) {
			System.out.println("\tNo results.");
			return null;
		} else {
			// need sort first for ranked model
			for (int i = 0; i < Math
					.min(result.docScores.scores.size(), fbDocs); i++) {
				int docid = result.docScores.getDocid(i);
				double score = result.docScores.getDocidScore(i);
				ret.put(docid, score);
			}
		}
		return ret;
	}

	/**
	 * Given a query string, returns the terms one at a time with stopwords
	 * removed and the terms stemmed using the Krovetz stemmer.
	 * 
	 * Use this method to process raw query terms.
	 * 
	 * @param query
	 *            String containing query
	 * @return Array of query tokens
	 * @throws IOException
	 */
	static String[] tokenizeQuery(String query) throws IOException {

		TokenStreamComponents comp = analyzer.createComponents("dummy",
				new StringReader(query));
		TokenStream tokenStream = comp.getTokenStream();

		CharTermAttribute charTermAttribute = tokenStream
				.addAttribute(CharTermAttribute.class);
		tokenStream.reset();

		List<String> tokens = new ArrayList<String>();
		while (tokenStream.incrementToken()) {
			String term = charTermAttribute.toString();
			tokens.add(term);
		}
		return tokens.toArray(new String[tokens.size()]);
	}

	public static boolean isWAND() {
		return isWAND;
	}

	public static void setWAND(boolean isWAND) {
		QryEval.isWAND = isWAND;
	}

	public static boolean isWSUM() {
		return isWSUM;
	}

	public static void setWSUM(boolean isWSUM) {
		QryEval.isWSUM = isWSUM;
	}
}
