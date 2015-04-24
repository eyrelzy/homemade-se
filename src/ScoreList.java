/**
 *  This class implements the document score list data structure
 *  and provides methods for accessing and manipulating them.
 *
 *  Copyright (c) 2015, Carnegie Mellon University.  All Rights Reserved.
 */

import java.util.*;

public class ScoreList {

  //  A little utilty class to create a <docid, score> object.

  protected class ScoreListEntry {
    private int docid;
    private double score;

    private ScoreListEntry(int docid, double score) {
      this.docid = docid;
      this.score = score;
    }
    public double getScore() {
        return score;
    }

    public int getDocid() {
        return docid;
    }
  }

  List<ScoreListEntry> scores = new ArrayList<ScoreListEntry>();

  /**
   *  Append a document score to a score list.
   *  @param docid An internal document id.
   *  @param score The document's score.
   *  @return void
   */
  public void add(int docid, double score) {
    scores.add(new ScoreListEntry(docid, score));
  }

  /**
   *  Get the n'th document id.
   *  @param n The index of the requested document.
   *  @return The internal document id.
   */
  public int getDocid(int n) {
    return this.scores.get(n).docid;
  }

  /**
   *  Get the score of the n'th document.
   *  @param n The index of the requested document score.
   *  @return The document's score.
   */
  public double getDocidScore(int n) {
    return this.scores.get(n).score;
  }
  
  public void setDocid(int n, int docid){
	  this.scores.get(n).docid=docid;
  }
  public void setDocidScore(int n, double score){
	  this.scores.get(n).score=score;
  }
  /**
   * 
   * sort first by scores and second by docid: return 1 means to swap
   * @param hm hashmap of internal id and external doc name
   * 
   **/
  public void sortByScoresAndDocID(final HashMap<Integer, String> hm) {
      Collections.sort(scores, new Comparator<ScoreListEntry>() {
	       public int compare(ScoreListEntry s1, ScoreListEntry s2) {
	              if (s1.getScore() < s2.getScore()) {
	                  return 1;
	              } else if (s1.getScore() == s2.getScore()) {
	                  String id1=null, id2=null;
	                  id1 = hm.get(s1.getDocid());//external id
	                  id2 = hm.get(s2.getDocid());
	                  if (id1!=null && id2!=null) {
	                      int ret = id2.compareTo(id1);
	                      if (ret > 0) 
	                    	  return 0;
	                      else if (ret < 0) 
	                    	  return 1;
	                  }
	              }
	              return 0;
	          }
	      }
      );
  }

}
