import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

public class QryopSlSum extends QryopSl {
	public QryopSlSum(){
		
	}
	public QryopSlSum(double weight){this.weight=weight;}
	@Override
	public double getDefaultScore(RetrievalModel r, long docid)
			throws IOException {
		// TODO Auto-generated method stub
		if (r instanceof BM25RetrievalModel)
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
		if (r instanceof BM25RetrievalModel)
			return (evaluateBM25(r));
		return null;
	}

	private QryResult evaluateBM25(RetrievalModel r) throws IOException {
		// TODO Auto-generated method stub
		allocArgPtrs(r);
		QryResult result = new QryResult();
		HashMap<Integer, Double> hm = new HashMap<Integer, Double>();
		for (int j = 0; j < this.argPtrs.size(); j++) {
			ArgPtr ptrj = this.argPtrs.get(j);
			for (; ptrj.nextDoc < ptrj.scoreList.scores.size(); ptrj.nextDoc++) {
				int curid = ptrj.scoreList.getDocid(ptrj.nextDoc);
				double score = ptrj.scoreList.getDocidScore(ptrj.nextDoc);//one term
				if (!hm.containsKey(curid))
					hm.put(curid, score);
				else {
					hm.put(curid, hm.get(curid) + score);
				}
			}
		}
		Iterator<Entry<Integer, Double>> it = hm.entrySet().iterator();
		while(it.hasNext()){
			Entry<Integer, Double> en=it.next();
			result.docScores.add(en.getKey(), en.getValue());
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

		return ("#SUM( " + result + ")");
	}

	@Override
	public double getDefaultScore(RetrievalModel r, long docid,
			DocLengthStore dls) throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

}
