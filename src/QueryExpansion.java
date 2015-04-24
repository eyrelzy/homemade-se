import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;




public class QueryExpansion {

	//given topdocs, compute tf, build document,<term, tf>
	public static HashMap<String, Double> computeTermScore(DocLengthStore s, HashMap<Integer, Double> topdocs, double fbmu, String field, long corpus_len,
			HashMap<String, Long> corpusFreqHashMap) throws Exception{
		// DocLengthStore s = new DocLengthStore(READER);
		// Lookup the document length of the body field of doc 0.
		// param: "field name", docid
		// System.out.println(s.getDocLength("body", 1));
		//
		// // How to use the term vector.
		
		// System.out.println(tv.stemString(10)); // get the string for the 10th
		// // stem
		// System.out.println(tv.stemDf(10)); // get its df
		// System.out.println(tv.totalStemFreq(10)); // get its ctf
		// System.out.println("==========");
		// System.err.println(tv.positionsLength()); // all stems
		// for(int i=0;i<tv.positionsLength();i++){
		// int index=tv.stemAt(i);
		// System.err.println(tv.stemString(index)+"|"+index);
		// }
 		 HashMap<String, Double> termScoreHashMap=new HashMap<String, Double>();
		 
		 for(Map.Entry<Integer, Double> en: topdocs.entrySet()){
			 int doc_Id=en.getKey();
			 double indriScore=en.getValue();
			 TermVector tv = new TermVector(doc_Id, field);// docid, field
			 long doc_len = s.getDocLength(field, doc_Id);
			 HashMap<String, Integer> termFreqHashMap=new HashMap<String, Integer>();
			 for(int i=0;i<tv.positionsLength();i++){
				 int index=tv.stemAt(i);
				 if(index==0){
//					 System.err.println(tv.stemString(index));//stop words index=0, and return string "null"
					 continue;
				 }
				 long ctf=tv.totalStemFreq(index);
				 String termStr=tv.stemString(index);
//				 System.out.println(ctf+"|"+termStr);
				 if(!termFreqHashMap.containsKey(termStr))
					 termFreqHashMap.put(termStr, 1);
				 else{
					 termFreqHashMap.put(termStr,termFreqHashMap.get(termStr)+1);
				 }
				 if(!corpusFreqHashMap.containsKey(termStr)){
					 corpusFreqHashMap.put(termStr,ctf);
				 }
				 if(!termScoreHashMap.containsKey(termStr)){
					 termScoreHashMap.put(termStr,0.0);
				 }
//				 System.err.println("unique:"+tv.stemsLength());
			 }
			 for(Map.Entry<String, Double> term: termScoreHashMap.entrySet()){
				 String termStr=term.getKey();
//				 System.err.println(termStr);
				 int tf=0;
				 if(termFreqHashMap.containsKey(termStr)){
					 tf=termFreqHashMap.get(termStr);
				 }
				 long ctf=corpusFreqHashMap.get(termStr);
				 double idf=Math.log(corpus_len*1.0/ctf);
				 double first=(double)(tf+fbmu*(ctf*1.0/corpus_len))/(doc_len+fbmu);
				 double finalScore=first*indriScore*idf;
				 termScoreHashMap.put(termStr,term.getValue()+finalScore);
			 }
//			 System.err.print("================================");
		 }
		 return termScoreHashMap;
	}
	public static <K, V extends Comparable<? super V>> SortedSet<Map.Entry<K, V>> entriesSortedByValues(Map<K, V> map) {
        SortedSet<Map.Entry<K, V>> sortedEntries = new TreeSet<Map.Entry<K, V>>(new Comparator<Map.Entry<K, V>>() {
            @Override
            public int compare(Map.Entry<K, V> e1, Map.Entry<K, V> e2) {
                return e2.getValue().compareTo(e1.getValue());
            }
        });
        sortedEntries.addAll(map.entrySet());
        return sortedEntries;
    }

	
	public static void expandQuery(SortedSet<Map.Entry<String, Double>> sorted, int fbTerms, StringBuilder expandedQuery ){
		int counter = 0;
		for (Map.Entry<String, Double> m : sorted) {
            if (counter < fbTerms) {
                String key = m.getKey();
                double score=m.getValue();
                if (!key.contains(".") && !key.contains(",")) {
//                    expandedQuery.append(" ").append(String.format("%.4f", score)).append(" ").append(key);
                    expandedQuery.append(" ").append(score).append(" ").append(key);
                    counter++;
                }
            } else {
                break;
            }
        }
	}
}
