import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

public class QryopIlWindow extends QryopIl {
	private int distance;

	public QryopIlWindow() {
	}
	public QryopIlWindow(double w) {
		this.weight=w;
	}

	public QryopIlWindow(int distance) {
		this.setDistance(distance);
	}
	public QryopIlWindow(int distance,double weight) {
		this.setDistance(distance);
		this.weight=weight;
	}

	public QryopIlWindow(Qryop... q) {
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
		QryResult result = new QryResult();
		if (!syntaxCheckArgResults(this.argPtrs)) {
			if (this.argPtrs.size() == 0) {
				return result;
			} else if (this.argPtrs.size() == 1) {
				result.invertedList = this.argPtrs.get(0).invList;
				return result;
			}
		}
		String field = this.argPtrs.get(0).invList.field;
		result.invertedList.field = new String(
				this.argPtrs.get(0).invList.field);

		boolean f = true;
		while (f) {
			int nextDocid = getSmallestCurrentDocid();
			boolean flag = false;

			for (int i = 0; i < this.argPtrs.size(); i++) {
				ArgPtr ptri = this.argPtrs.get(i);
				if (ptri.invList.getDocid(ptri.nextDoc) != nextDocid) {
					flag = true;
					// break;
				} else {
					ptri.nextDoc++;
				}
				if (ptri.nextDoc >= ptri.invList.postings.size()) {
					flag = true;
					f = false;
					break;
				}
			}
			// matches docid
			// if one param's document list get the last, we finished.
			if (!flag) {
				List<Integer> positions = new ArrayList<Integer>();
				boolean findPos = false;
				boolean ff = true;
				while (ff) {
					int max = Integer.MIN_VALUE, min = Integer.MAX_VALUE;
					for (int i = 0; i < this.argPtrs.size(); i++) {
						ArgPtr ptri = this.argPtrs.get(i);
						Vector<Integer> vpos = ptri.invList.postings
								.elementAt(ptri.nextDoc - 1).positions;
						int cur_index = ptri.invList.postings
								.elementAt(ptri.nextDoc - 1).nextpos;
						int cur_pos = vpos.get(cur_index);
						min = Math.min(min, cur_pos);
						max = Math.max(max, cur_pos);
					}
					int size = 1 + max - min;
					if (size > distance) {
						for (int i = 0; i < this.argPtrs.size(); i++) {
							ArgPtr ptri = this.argPtrs.get(i);
							Vector<Integer> vpos = ptri.invList.postings
									.elementAt(ptri.nextDoc - 1).positions;
							int cur_index = ptri.invList.postings
									.elementAt(ptri.nextDoc - 1).nextpos;
							int cur_pos = vpos.get(cur_index);
							if (cur_pos == min) {
								ptri.invList.postings
										.elementAt(ptri.nextDoc - 1).nextpos++;
							}
						}
					} else {
						positions.add(max);// ?what is the position of this
						for (int i = 0; i < this.argPtrs.size(); i++) {
							ArgPtr ptri = this.argPtrs.get(i);
							ptri.invList.postings.elementAt(ptri.nextDoc - 1).nextpos++;
						}
						findPos = true;
					}
					for (int i = 0; i < this.argPtrs.size(); i++) {
						ArgPtr ptri = this.argPtrs.get(i);
						Vector<Integer> vpos = ptri.invList.postings
								.elementAt(ptri.nextDoc - 1).positions;
						int cur_index = ptri.invList.postings
								.elementAt(ptri.nextDoc - 1).nextpos;

						// System.err.println(i+"|"+cur_index);

						if (cur_index >= vpos.size()) {
							ff = false;
							break;
						}
					}
				}
				if (findPos) {
					// Collections.sort (positions);
					result.invertedList.appendPosting(nextDocid, positions);
				}
			}
		}
		freeArgPtrs();
		return result;
	}

	/**
	 * Return the smallest unexamined docid from the ArgPtrs.
	 * 
	 * @return The smallest internal document id.
	 */
	public int getSmallestCurrentDocid() {

		int nextDocid = Integer.MAX_VALUE;

		for (int i = 0; i < this.argPtrs.size(); i++) {
			ArgPtr ptri = this.argPtrs.get(i);
			if (nextDocid > ptri.invList.getDocid(ptri.nextDoc))
				nextDocid = ptri.invList.getDocid(ptri.nextDoc);
		}

		return (nextDocid);
	}

	public Boolean syntaxCheckArgResults(List<ArgPtr> ptrs) {
		if (ptrs.size() < 2) {
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

		return ("#WIN/" + distance + "( " + result + ")");
	}

	public int getDistance() {
		return distance;
	}

	public void setDistance(int distance) {
		this.distance = distance;
	}

}
