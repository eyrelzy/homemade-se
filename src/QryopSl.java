/**
 *  All query operators that return score lists are subclasses of the
 *  QryopSl class.  This class has two main purposes.  First, it
 *  allows query operators to easily recognize any nested query
 *  operator that returns a score list (e.g., #AND (a #OR (b c)).
 *  Second, it is a place to store data structures and methods that are
 *  common to all query operators that return score lists.
 *  
 *  Copyright (c) 2015, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.IOException;

public abstract class QryopSl extends Qryop {

	private double w;

	/**
	 * Use the specified retrieval model to evaluate the query arguments. Define
	 * and return ArgPtrs that the query operator can use.
	 * 
	 * @param r
	 *            A retrieval model that controls how the operator behaves.
	 * @return void
	 * @throws IOException
	 */
	public void allocArgPtrs(RetrievalModel r) throws IOException {

		for (int i = 0; i < this.args.size(); i++) {

			// If this argument doesn't return ScoreLists, wrap it
			// in a #SCORE operator.

			/* eyre: convert invertedlist into scorelist */

			if (!QryopSl.class.isInstance(this.args.get(i))) {
				// #SCORE
				w = this.args.get(i).getWeight();
				this.args.set(i, new QryopSlScore(this.args.get(i)));
				this.args.get(i).setWeight(w);
			}
			ArgPtr ptri = new ArgPtr();
			ptri.invList = null;
			ptri.scoreList = this.args.get(i).evaluate(r).docScores;
			ptri.nextDoc = 0;
			this.argPtrs.add(ptri);
		}
	}

	/*
	 * Calculate the default score for the specified document if it does not
	 * match the query operator. This score is 0 for many retrieval models, but
	 * not all retrieval models.
	 * 
	 * @param r A retrieval model that controls how the operator behaves.
	 * 
	 * @param docid The internal id of the document that needs a default score.
	 * 
	 * @return The default score.
	 */
	public abstract double getDefaultScore(RetrievalModel r, long docid)
			throws IOException;

	public abstract double getDefaultScore(RetrievalModel r, long docid,
			DocLengthStore dls) throws IOException;

}
