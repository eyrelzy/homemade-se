import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;


public class QryopSlWAndIndri extends QryopSl {
	public QryopSlWAndIndri() {
	}

	public QryopSlWAndIndri(double weight) {
		this.weight = weight;
	}

	public QryopSlWAndIndri(Qryop... q) {
		for (int i = 0; i < q.length; i++)
			this.args.add(q[i]);
	}
	@Override
	public void add(Qryop q) throws IOException {
		// TODO Auto-generated method stub
		this.args.add(q);
	}
	@Override
	public double getDefaultScore(RetrievalModel r, long docid)
			throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getDefaultScore(RetrievalModel r, long docid,
			DocLengthStore dls) throws IOException {
		// TODO Auto-generated method stub
		if(r instanceof IndriRetrievalModel){
			int size = this.args.size();
			double wsum = 0.0;
			for(int j=0;j<size;j++){
				wsum+=this.args.get(j).getWeight();
			}
//	        double exp = (double) 1 / size;
	        
	        double finalDefaultScore = 1;
	        for (int index = 0; index < size; index++) {
	        	double defaultScore = ((QryopSl) this.args.get(index)).getDefaultScore(r, docid,dls);
	        	double jw=this.args.get(index).getWeight();
            	double exp=jw/wsum;
	            finalDefaultScore *= Math.pow(defaultScore, exp);
	        }
//	        System.out.println("#"+finalDefaultScore);
	        return finalDefaultScore;
		}
		return 0;
	}
	@Override
	public QryResult evaluate(RetrievalModel r) throws IOException {
		// TODO Auto-generated method stub
		allocArgPtrs(r);
		QryResult result = new QryResult();
		
		int size = this.argPtrs.size();
		double wsum = 0.0;
		for(int j=0;j<size;j++){
			wsum+=this.args.get(j).getWeight();
		}
		HashMap<Integer, float[]> hm = new HashMap<Integer, float[]>();
		for (int j = 0; j < this.argPtrs.size(); j++) {
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
            	double jw=this.args.get(j).getWeight();
            	double exp=jw/wsum;
            	if(values[j]==0.0){
//            		System.out.println(this.args.get(j).getClass());
            		double defaultScore = ((QryopSl) this.args.get(j)).getDefaultScore(r, currentDocID, dls);
//            		System.out.println(currentDocID+"|"+defaultScore);
            		finalscore*=Math.pow(defaultScore,exp);
            	}else{
            		finalscore*=Math.pow(values[j],exp);
//            		System.out.println(currentDocID+"|"+values[j]);
            	}
            }
            result.docScores.add(currentDocID, finalscore);
            
        }
		freeArgPtrs();
		
		return result;
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		String result = new String();

		for (int i = 0; i < this.args.size(); i++)
			result += this.args.get(i).getWeight()+"|"+this.args.get(i).toString() + " ";

		return ("#WAND( " + result + ")");
	}

}
