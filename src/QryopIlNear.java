/**
 *  This class implements the NEAR operator for all retrieval models.
 *  The NEAR operator stores inverted list of the near result.
 *	For example: #NEAR/2 (apple pie)
 * @author zhiyuel
 */
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

public class QryopIlNear extends QryopIl {
	private int distance;

	public QryopIlNear() {

	}
	public QryopIlNear(double weight) {
		this.weight=weight;
	}
	public QryopIlNear(int distance, double weight) {
		this.weight=weight;
		this.distance=distance;
	}
	public QryopIlNear(int distance) {
		this.distance = distance;
	}

	public QryopIlNear(Qryop... q) {
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

		// Initialization
		allocArgPtrs(r);
		QryResult result = new QryResult();
		if(!syntaxCheckArgResults(this.argPtrs)){
			if(this.argPtrs.size()==0){
				return result;
			}else if(this.argPtrs.size()==1){
				result.invertedList=this.argPtrs.get(0).invList;
				return result;
			}
		}
		String field = this.argPtrs.get(0).invList.field;
		result.invertedList.field = new String(
				this.argPtrs.get(0).invList.field);

		// get shortest df list
		ArgPtr p0 = this.argPtrs.get(0);
		ArgPtr p1 = this.argPtrs.get(1);
		result=nearTwo(p0,p1);
		String s1 = this.args.get(0).toString();
		String s2 = this.args.get(1).toString();
		int index=2;
		/*
		 * when it is more than two parameters of the near operator, should continue to do near operation.
		 * i.e. #Near/n(a, b, c) is equal to #Near/n(#Near/n(a, b), c) 
		 * */
		while(index<this.argPtrs.size()) {
			Qryop combine = new QryopIlTerm("#NEAR/" + distance + "( " + s1
					+ " " + s2 + ")", field);
			combine.allocArgPtrs(r, result.invertedList);
			combine.args.add(new QryopIlTerm("#NEAR/" + distance + "( " + s1
					+ " " + s2 + ")", field));
			ArgPtr p = combine.argPtrs.get(0);
			ArgPtr q = this.argPtrs.get(index);
			result=nearTwo(p,q);
			System.out.println();
			index++;

		}
		result.invertedList.field = field;
		freeArgPtrs();
		return result;
	}
/**
 * the near operator results of two args
 * First, find the same docid, then check the position. 
 * Both document and position should only be matched exactly once.
 * @param  p0,p1
 * @return inverted list of near operation.
 * */
	public QryResult nearTwo(ArgPtr p0, ArgPtr p1) {
		QryResult result =new QryResult();
		while (p0.nextDoc < p0.invList.df && p1.nextDoc < p1.invList.df) {

			if (p0.invList.getDocid(p0.nextDoc) > p1.invList
					.getDocid(p1.nextDoc)) {
				p1.nextDoc++;
			} else if (p0.invList.getDocid(p0.nextDoc) == p1.invList
					.getDocid(p1.nextDoc)) {
				// match doc:
				int nextDocid = p0.invList.getDocid(p0.nextDoc);
				Vector<Integer> v1 = p0.invList.postings.elementAt(p0.nextDoc).positions;
				Vector<Integer> v2 = p1.invList.postings.elementAt(p1.nextDoc).positions;
				List<Integer> positions = new ArrayList<Integer>();
				for (int i = 0, j = 0; i < v1.size() && j < v2.size();) {
					if (v1.get(i) >= v2.get(j)) {
						// p0.invList.postings.elementAt(p0.nextDoc).nextpos++;
						j++;
					} else {
						if (v2.get(j) - v1.get(i) <= this.distance) {
							positions.add(v2.get(j));
							i++;
							j++;
						} else {
							i++;
						}
					}
				}
				if (positions.size() > 0) {
					result.invertedList.appendPosting(nextDocid, positions);
				}
				p0.nextDoc++;
				p1.nextDoc++;
			} else {
				p0.nextDoc++;
			}
		}
		return result;
	}

	public Boolean syntaxCheckArgResults(List<ArgPtr> ptrs) {
		if(ptrs.size()<2){
			System.err.println("Error: Invalid argument in:  "
					+ this.toString());
			return false;
		}
		
		for (int i = 0; i < this.args.size(); i++) {

			if (!(this.args.get(i) instanceof QryopIl))
				FileUtil.fatalError("Error:  Invalid argument in "
						+ this.toString());
			else if ((i > 0)
					&& (!ptrs.get(i).invList.field
							.equals(ptrs.get(0).invList.field)))
				FileUtil.fatalError("Error:  Arguments must be in the same field:  "
						+ this.toString());
		}
		return true;
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		String result = new String();

		for (Iterator<Qryop> i = this.args.iterator(); i.hasNext();)
			result += (i.next().toString() + " ");

		return ("#NEAR/" + distance + "( " + result + ")");
	}

	public int getDistance() {
		return distance;
	}

	public void setDistance(int distance) {
		this.distance = distance;
	}

}
