import java.io.IOException;

import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;

public class Letor {
	
	
	
	public static double getBM25Score(int docId, String[] stem, String fieldName)
			throws IOException {
		TermVector tv = new TermVector(docId, fieldName);
		double totalScore = 0.0;
		float avgDocLen = QryEval.READER.getSumTotalTermFreq(fieldName)
				/ (float) QryEval.READER.getDocCount(fieldName);
		int N = QryEval.READER.numDocs();
		long docLen = QryEval.s.getDocLength(fieldName, docId);
		for (int i = 0; i < stem.length; i++) {
			int idx = tv.getStemIdx(stem[i]);
			if (idx == -1)
				continue;
			int tf = tv.stemFreq(idx);
			int df = tv.stemDf(idx);
			double idf = Math.max(0, Math.log((N - df + 0.5) / (df + 0.5)));
			double tfWeight = (double) tf
					/ (tf + BM25RetrievalModel.K1
							* (1 - BM25RetrievalModel.B + BM25RetrievalModel.B
									* docLen / avgDocLen));
			double scoreOfI = idf * tfWeight;
			totalScore += scoreOfI;
		}
		return totalScore;
	}
	public static double getAuthorziationScore(int docId) throws IOException{
		double score=0.0;
		Terms terms = QryEval.READER.getTermVector(docId,"url");
		if(terms!=null){
			
		}
			score+=1.0;
		return score;
	}
	
	//0.2582 oh noooooo
	public static double getFieldCnt(int docId) throws IOException{
		double score=0.0;
		Terms terms = QryEval.READER.getTermVector(docId,"title");
		if(terms!=null)
			score+=1.0;
		terms = QryEval.READER.getTermVector(docId,"url");
		if(terms!=null)
			score+=1.0;
		terms = QryEval.READER.getTermVector(docId,"inlink");
		if(terms!=null)
			score+=1.0;
		return score;
	}
	//slightly worse 2.2590
	public static double getField(int docId, String fieldname) throws IOException{
		Terms terms = QryEval.READER.getTermVector(docId,fieldname);
		if(terms==null)
			return 0.0;
		else
			return 1.0;
	}
	//worse 2.2588
	public static double getTF(int docId, String[] stem,
			String fieldName ) throws IOException{
		TermVector tv = new TermVector(docId, fieldName);
		double totalTf = 0.0;
		for (int i = 0; i < stem.length; i++) {
			int idx = tv.getStemIdx(stem[i]);
			if (idx == -1)
				continue;
			totalTf+= tv.stemFreq(idx);
		}
		return totalTf;
	}
	public static double getVSMScore(int docId, String[] stems, TermVector docVec) throws IOException {
		double totalScore = 0.0;
		int N = QryEval.READER.numDocs();
		for (int i = 0; i < stems.length; i++) {
			int idx = docVec.getStemIdx(stems[i]);
    		int tf = 0;
    		int df = 0;
    		if (idx != -1)	{ // check whether the query term is in the document
    			tf = docVec.stemFreq(idx);
    			df = docVec.stemDf(idx);
    		}
    		else 
    			continue;
    		double idf = (Math.log(tf+0.5)+1.0)*Math.log((N-df+0.5)/(df+0.5));
    		totalScore += idf;
		}
		double sum2=0.0;
		for(int i=0;i<stems.length;i++){
			int df = 0;
			int idx = docVec.getStemIdx(stems[i]);
			if (idx != -1)	{ // check whether the query term is in the document
    			df = docVec.stemDf(idx);
    		}
			sum2+=Math.log((N-df+0.5)/(df+0.5));
		}
		double sum1=0.0;
		for(int i=0;i<docVec.positionsLength();i++){
			int index=docVec.stemAt(i);
			int tf=docVec.stemFreq(index);
			sum1+=(Math.log(tf+0.5)+1);
		}
		totalScore =totalScore/((Math.sqrt(sum1))*(Math.sqrt(sum2)));
		return totalScore;
	}
	
	//worse 2.2581
	public static double getTfIdfScore(int docId, String[] stems, TermVector docVec) throws IOException {
		double totalScore = 0.0;
		int numOfDoc = QryEval.READER.numDocs();
		for (int i = 0; i < stems.length; i++) {
			int idx = docVec.getStemIdx(stems[i]);
    		int docTf = 0;
    		int termDf = 0;
    		if (idx != -1)	{ // check whether the query term is in the document
    			docTf = docVec.stemFreq(idx);
    			termDf = docVec.stemDf(idx);
    		}
    		else continue;
    		float idf = (float)Math.log((numOfDoc - termDf + 0.5) 
    				/ (termDf + 0.5));
    		totalScore += docTf * idf;
		}
		totalScore /= stems.length;
		return totalScore;
	}
	public static double getOverlapScore(int docId, String[] stem, String fieldName) throws IOException{
		TermVector tv = new TermVector(docId, fieldName);
		int overlap=0;
		for (int i = 0; i < stem.length; i++) {
			int idx = tv.getStemIdx(stem[i]);
			if (idx == -1) {
				
			}else{
				overlap++;
			}
		}
		return (double)(overlap)/stem.length;
	}

	
	public static double getWeightedTfIdfScore(int docId, String[] stem) throws IOException{
		double totalScore=0.0;
		double bodys=0.0, urls=0.0, titles=0.0, inlinks=0.0;
		//0.6,0.1,0.2,0.1 0.2591
		double BODY_WEIGHT=0.6;
		double URL_WEIGHT=0.1;
		double TITLE_WEIGHT=0.2;
		double INLINK_WEIGHT=0.1;
		Terms terms = QryEval.READER.getTermVector(docId, "body");
		if(terms!=null){
			bodys=getTfIdfScore(docId, stem, new TermVector(docId,"body"));
		}
		terms = QryEval.READER.getTermVector(docId, "url");
		if(terms!=null){
			urls=getTfIdfScore(docId, stem, new TermVector(docId,"url"));
		}
		terms = QryEval.READER.getTermVector(docId, "title");
		if(terms!=null){
			titles=getTfIdfScore(docId, stem, new TermVector(docId,"title"));
		}
		terms = QryEval.READER.getTermVector(docId, "inlink");
		if(terms!=null){
			inlinks=getTfIdfScore(docId, stem, new TermVector(docId,"inlink"));
		}
		totalScore=bodys*BODY_WEIGHT+urls*URL_WEIGHT+titles*TITLE_WEIGHT+INLINK_WEIGHT*inlinks;
		return totalScore;
	}
	public static double getIndriScore(int docId, String[] stem,
			String fieldName) throws IOException {
		double totalScore = 1.0;
		double l = IndriRetrievalModel.Lambda;
		double m = IndriRetrievalModel.Mu;
		long C = QryEval.READER.getSumTotalTermFreq(fieldName);
		TermVector tv = new TermVector(docId, fieldName);
		int overlap=0;
		long docLen = QryEval.s.getDocLength(fieldName, docId);
		for (int i = 0; i < stem.length; i++) {
			int idx = tv.getStemIdx(stem[i]);
			int tf=0;
			long ctf=0;
			if (idx == -1) {
				//not appear in this field of the document
				Term qTerm = new Term(fieldName, stem[i]);
				ctf = QryEval.READER.totalTermFreq(qTerm);//????default ctf, tf=0
			} else {
				overlap++;
				tf = tv.stemFreq(idx);
				ctf = tv.totalStemFreq(idx);
			}
			double mle= (double)(ctf)/C;
			//correct version
			double score = (double) (1-l) * ((tf + (m * mle)) / (docLen + m)) + (double)(l) * mle;
			totalScore*=Math.pow(score, 1.0/stem.length);
		}
		if(overlap==0)
			return 0.0;
		return totalScore;
	}
	
}
