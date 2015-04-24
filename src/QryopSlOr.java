/**
 *  This class implements the OR operator for all retrieval models.
 *
 *  @author zhiyuel
 */
import java.io.IOException;

public class QryopSlOr extends QryopSl {
	public QryopSlOr(){}
	public QryopSlOr(double weight){this.weight=weight;}
	
	public QryopSlOr(Qryop... q) {
		for (int i = 0; i < q.length; i++)
			this.args.add(q[i]);
	}

	@Override
	public double getDefaultScore(RetrievalModel r, long docid)
			throws IOException {
		// TODO Auto-generated method stub
		if (r instanceof RetrievalModelUnrankedBoolean)
			return 0;
		return 0;
	}

	@Override
	public void add(Qryop q) throws IOException {
		// TODO Auto-generated method stub
		this.args.add(q);
	}

	@Override
	public QryResult evaluate(RetrievalModel r) throws IOException {
		// TODO Auto-generated method stub
		if (r instanceof RetrievalModelUnrankedBoolean)
			return (evaluateBoolean(r));
		else if(r instanceof RetrievalModelRankedBoolean)
			return  (evaluateBoolean(r));
		else if(r instanceof BM25RetrievalModel)
			return (evaluateBoolean(r));
		else if(r instanceof IndriRetrievalModel)
			return (evaluateBoolean(r));
		return null;
	}

	/**
	 * Search for the current smallest docid in order to avoid repeated docids returned from different QryResult
	 * @return the smallest docid
	 * */
	public int getSmallestCurrentDocid() {
		int nextDocid = Integer.MAX_VALUE;
		for (int i = 0; i < this.argPtrs.size(); i++) {
			ArgPtr ptri = this.argPtrs.get(i);
			//not retrieved
			if(ptri.scoreList.scores.size()==0){
				this.argPtrs.remove(i);
				i--;
			}
		}
		for (int i = 0; i < this.argPtrs.size(); i++) {
			ArgPtr ptri = this.argPtrs.get(i);
			//its result is not find
			if (nextDocid > ptri.scoreList.getDocid(ptri.nextDoc))
				nextDocid = ptri.scoreList.getDocid(ptri.nextDoc);
		}
		return (nextDocid);
	}

	/**
	 * Evaluates the query operator for rank boolean retrieval models, including any
	 * child operators and returns the result.
	 * @param param type of rank boolean retrieval models
	 * @param r
	 *            A retrieval model that controls how the operator behaves.
	 * @return The result of evaluating the query.
	 * @throws IOException
	 */
	public QryResult evaluateBoolean(RetrievalModel r) throws IOException {

		// Initialization
		allocArgPtrs(r);
		QryResult result = new QryResult();
		//one arg and its result is not find
		if(this.argPtrs.size()==1&&this.argPtrs.get(0).scoreList.scores.size()==0){
			freeArgPtrs();
			return result;
		}
		while (this.argPtrs.size() > 0) {
		      int nextDocid = getSmallestCurrentDocid ();
		      if(this.argPtrs.size()==0)//no results after removal
		    	  break;
		      double docScore=Double.MIN_VALUE;
		      for (int i=0; i<this.argPtrs.size(); i++) {
		    	  ArgPtr ptri = this.argPtrs.get(i);
		    	  if (ptri.scoreList.getDocid (ptri.nextDoc) == nextDocid) {
		    		  	docScore=Math.max(docScore,ptri.scoreList.getDocidScore(ptri.nextDoc));
		    		  	ptri.nextDoc ++;
		    	  }
		      }
		      for (int i=0; i<this.argPtrs.size(); i++) {
		    	  ArgPtr ptri = this.argPtrs.get(i);
		    	  if(ptri.scoreList.scores.size()==ptri.nextDoc){
		    		  this.argPtrs.remove(i);
		    		  i--;
		    	  }
		      }
		      result.docScores.add(nextDocid, docScore);
		}
		freeArgPtrs();
		return result;
	}
	@Override
	public String toString() {
		// TODO Auto-generated method stub
		String result = new String();

		for (int i = 0; i < this.args.size(); i++)
			result += this.args.get(i).toString() + " ";

		return ("#OR( " + result + ")");
	}

	@Override
	public double getDefaultScore(RetrievalModel r, long docid,
			DocLengthStore dls) throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

}
