import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

public class QryopSlAndIndri extends QryopSl {

	
	public QryopSlAndIndri(){}
	public QryopSlAndIndri(double weight){this.weight=weight;}
	public QryopSlAndIndri(Qryop... q) {
		for (int i = 0; i < q.length; i++)
			this.args.add(q[i]);
	}

	@Override
	public void add(Qryop q) throws IOException {
		// TODO Auto-generated method stub
		this.args.add(q);
	}

	@Override
	public QryResult evaluate(RetrievalModel r) throws IOException {
		// TODO Auto-generated method stub
		allocArgPtrs(r);
//		System.out.println(this.args.size());
		QryResult result = new QryResult();
		// like or
		// get smallest id
		// if cur_id=id,add score, else add default score
		int size = this.argPtrs.size();
		float exp=(float)1/(float)size;
		HashMap<Integer, float[]> hm = new HashMap<Integer, float[]>();
		for (int j = 0; j < size; j++) {
			ArgPtr ptrj = this.argPtrs.get(j);
			for (; ptrj.nextDoc < ptrj.scoreList.scores.size(); ptrj.nextDoc++) {
				int docid=ptrj.scoreList.getDocid(ptrj.nextDoc);
				float score = (float)ptrj.scoreList.getDocidScore(ptrj.nextDoc);
				if(!hm.containsKey(docid)){
					float[] doubleValues = new float[size];
                    doubleValues[j] = score;//the jth args
					hm.put(docid, doubleValues);
				}else{
					float[] doubleValues=hm.get(docid);
					doubleValues[j] = score;
                    hm.put(docid, doubleValues);
				}
			}
		}
		Iterator<Entry<Integer, float[]>> iterator = hm.entrySet().iterator();
		DocLengthStore dls = new DocLengthStore(QryEval.READER);
        while (iterator.hasNext()) {
            Entry<Integer, float[]> currentPair = iterator.next();
            float[] values=currentPair.getValue();
            Integer currentDocID = currentPair.getKey();
            float finalscore=1;
            for(int j=0;j<size;j++){
            	if(values[j]==0.0){
            		double defaultScore = ((QryopSl) this.args.get(j)).getDefaultScore(r, currentDocID, dls);
            		finalscore*=Math.pow(defaultScore,exp);
//            		 System.err.println(currentDocID+"@"+defaultScore);
            	}else{
            		finalscore*=Math.pow(values[j],exp);
            		
            	}
            }
//            System.err.println(currentDocID+"@"+finalscore);
            result.docScores.add(currentDocID, finalscore);
        }
		freeArgPtrs();
		return result;
	}
	@Override
	public double getDefaultScore(RetrievalModel r, long docid)
			throws IOException {
		if (r instanceof RetrievalModelUnrankedBoolean)
			return 0;
		return 0;
		// TODO Auto-generated method stub
	}
	@Override
	public double getDefaultScore(RetrievalModel r, long docid,
			DocLengthStore dls) throws IOException {
		// TODO Auto-generated method stub
		if(r instanceof IndriRetrievalModel){
			int size = this.args.size();
	        double exp = (double) 1 / size;
	        double finalDefaultScore = 1;
	        for (int index = 0; index < size; index++) {
	        	double defaultScore = ((QryopSl) this.args.get(index)).getDefaultScore(r, docid,dls);
	            finalDefaultScore *= Math.pow(defaultScore, exp);
	        }
	        return finalDefaultScore;
		}
		return 0;
	}
	@Override
	public String toString() {
		// TODO Auto-generated method stub
		String result = new String();

		for (int i = 0; i < this.args.size(); i++)
			result += this.args.get(i).toString() + " ";

		return ("#ANDRIAND( " + result + ")");
	}

	
}
