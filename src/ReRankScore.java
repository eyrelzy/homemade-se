import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


public class ReRankScore {
	public String exDocid;
	public double score;
	public List<ReRankScore> scores=new ArrayList<ReRankScore>();
	public void sortByScoresAndDocID() {
	      Collections.sort(scores, new Comparator<ReRankScore>() {
		       public int compare(ReRankScore s1, ReRankScore s2) {
		              if (s1.score < s2.score) {
		                  return 1;
		              } else if (s1.score == s2.score) {
		                  String id1=null, id2=null;
		                  id1 = s1.exDocid;//external id
		                  id2 = s2.exDocid;
		                  if (id1!=null && id2!=null) {
		                      int ret = id2.compareTo(id1);
		                      if (ret < 0) 
		                    	  return 1;
		                      else if (ret > 0) 
		                    	  return 0;
		                  }
		              }
		              return 0;
		          }
		      }
	      );
	  }
}
