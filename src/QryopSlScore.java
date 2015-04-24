/**
 *  This class implements the SCORE operator for all retrieval models.
 *  The single argument to a score operator is a query operator that
 *  produces an inverted list.  The SCORE operator uses this
 *  information to produce a score list that contains document ids and
 *  scores.
 *
 *  Copyright (c) 2015, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.*;
import java.util.*;

public class QryopSlScore extends QryopSl {
	private String field;
	private int ctf;
	private double corpusLen;
	
	public String getField() {
		return field;
	}

	public void setField(String field) {
		this.field = field;
	}

	public int getCtf() {
		return ctf;
	}

	public void setCtf(int ctf) {
		this.ctf = ctf;
	}

	public double getCorpusLen() {
		return corpusLen;
	}

	public void setCorpusLen(double corpusLen) {
		this.corpusLen = corpusLen;
	}

	/**
	 * Construct a new SCORE operator. The SCORE operator accepts just one
	 * argument.
	 * 
	 * @param q
	 *            The query operator argument.
	 * @return @link{QryopSlScore}
	 */
	public QryopSlScore(Qryop q) {
		this.args.add(q);
	}

	/**
	 * Construct a new SCORE operator. Allow a SCORE operator to be created with
	 * no arguments. This simplifies the design of some query parsing
	 * architectures.
	 * 
	 * @return @link{QryopSlScore}
	 */
	public QryopSlScore() {
	}
	public QryopSlScore(double w) {
		this.weight=w;
	}

	/**
	 * Appends an argument to the list of query operator arguments. This
	 * simplifies the design of some query parsing architectures.
	 * 
	 * @param q
	 *            The query argument to append.
	 */
	public void add(Qryop a) {
		this.args.add(a);
	}

	/**
	 * Evaluate the query operator.
	 * 
	 * @param r
	 *            A retrieval model that controls how the operator behaves.
	 * @return The result of evaluating the query.
	 * @throws IOException
	 */
	public QryResult evaluate(RetrievalModel r) throws IOException {
		return evaluateBoolean(r);
	}

	/**
	 * Evaluate the query operator for boolean retrieval models.
	 * 
	 * @param r
	 *            A retrieval model that controls how the operator behaves.
	 * @return The result of evaluating the query.
	 * @throws IOException
	 */
	public QryResult evaluateBoolean(RetrievalModel r) throws IOException {
		// Evaluate the query argument.
		QryResult result = args.get(0).evaluate(r);

		DocLengthStore dls = new DocLengthStore(QryEval.READER);
		double BM25k1 = 0, BM25b = 0, BM25k3 = 0, Indrimu = 0, Indrilambda = 0;
		if (r instanceof BM25RetrievalModel) {
			BM25k1 = r.getParameterDouble("BM25:k_1");
			BM25b = r.getParameterDouble("BM25:b");
			BM25k3 = r.getParameterDouble("BM25:k_3");
		} else if (r instanceof IndriRetrievalModel) {
			Indrimu = r.getParameterDouble("Indri:mu");
			Indrilambda = r.getParameterDouble("Indri:lambda");
			setCtf(result.invertedList.ctf);
            setField(result.invertedList.field);
            setCorpusLen(QryEval.READER.getSumTotalTermFreq(getField()));
		}

		// Each pass of the loop computes a score for one document. Note:
		// If the evaluate operation above returned a score list (which is
		// very possible), this loop gets skipped.
		for (int i = 0; i < result.invertedList.df; i++) {
			// DIFFERENT RETRIEVAL MODELS IMPLEMENT THIS DIFFERENTLY.
			// Unranked Boolean. All matching documents get a score of 1.0.
			if (r instanceof RetrievalModelUnrankedBoolean)
				result.docScores.add(result.invertedList.postings.get(i).docid,
						(float) 1.0);
			else if (r instanceof RetrievalModelRankedBoolean) {
				int tf = result.invertedList.postings.get(i).tf;
				result.docScores.add(result.invertedList.postings.get(i).docid,
						tf);
			} else if (r instanceof BM25RetrievalModel) {
				double BM25Score = computeBM25(result, i, dls, BM25k1, BM25b,
						BM25k3);
				result.docScores.add(result.invertedList.postings.get(i).docid,
						BM25Score);
			} else if (r instanceof IndriRetrievalModel) {
				double IndriScore = computeIndriScore(result, i, dls, Indrimu, Indrilambda);
                result.docScores.add(result.invertedList.postings.get(i).docid,
                		IndriScore);
			}
		}
		// The SCORE operator should not return a populated inverted list.
		// If there is one, replace it with an empty inverted list.
		if (result.invertedList.df > 0)
			result.invertedList = new InvList();
		return result;
	}
	private double computeIndriScore(QryResult result, int i,
			DocLengthStore dls, double indrimu, double indrilambda) throws IOException {
		// TODO Auto-generated method stub
		double tf = result.invertedList.getTf(i);
		int ctf = getCtf();
		double corpusLen = getCorpusLen();
		double mle = ctf / corpusLen;
		double docLength = dls.getDocLength(getField(),result.invertedList.getDocid(i));
		double ret=(1-indrilambda)*(tf+indrimu*mle)/(docLength+indrimu)+(indrilambda)*mle;//lambda
		return ret;
	}

	private double computeBM25(QryResult result, int i, DocLengthStore dls,
			double bM25k1, double bM25b, double bM25k3) throws IOException {
		// TODO Auto-generated method stub

		double N = QryEval.READER.numDocs();
		double df=result.invertedList.df;
		double doclen = dls.getDocLength(result.invertedList.field,
				result.invertedList.postings.get(i).docid);
		double averageDocLength = (double) QryEval.READER
				.getSumTotalTermFreq(result.invertedList.field)
				/ (double) QryEval.READER
						.getDocCount(result.invertedList.field);
		double tfd=result.invertedList.postings.elementAt(i).tf;
		double qtf=1;
		double idf =Math.max(0, Math.log((N-df+0.5)/(df+0.5)));
		double termWeight = tfd/(tfd+bM25k1*((1-bM25b)+bM25b*(doclen/averageDocLength)));
		double queryWeight=(bM25k3+1)*qtf/(bM25k3+qtf);
		return idf*termWeight*queryWeight;
	}

	/*
	 * Calculate the default score for a document that does not match the query
	 * argument. This score is 0 for many retrieval models, but not all
	 * retrieval models.
	 * 
	 * @param r A retrieval model that controls how the operator behaves.
	 * 
	 * @param docid The internal id of the document that needs a default score.
	 * 
	 * @return The default score.
	 */
	@Override
	public double getDefaultScore(RetrievalModel r, long docid)
			throws IOException {
		if (r instanceof RetrievalModelUnrankedBoolean)
			return 0;
		return 0;
	}
	@Override
	public double getDefaultScore (RetrievalModel r, long docid, DocLengthStore dls) throws IOException{
		if(r instanceof IndriRetrievalModel){
			double Indrimu = r.getParameterDouble("Indri:mu");
			double Indrilambda = r.getParameterDouble("Indri:lambda");
			double tf = 0;
			double mle =getCtf()/ getCorpusLen();
			double docLength = dls.getDocLength(getField(),(int)docid);
			double ret=(float) ((1-Indrilambda)*(tf+Indrimu*mle)/(docLength+Indrimu)+(Indrilambda)*mle);
			return ret;
		}
		return 0;
	}


	/**
	 * Return a string version of this query operator.
	 * 
	 * @return The string version of this query operator.
	 */
	public String toString() {

		String result = new String();

		for (Iterator<Qryop> i = this.args.iterator(); i.hasNext();)
			result += (i.next().toString() + " ");

		return ("#SCORE( " + result + ")");
	}

}
