/**
 *  All query operators that return inverted lists are subclasses of
 *  the QryopIl class.  This class has two main purposes.  First, it
 *  allows query operators to easily recognize any nested query
 *  operator that returns an inverted list (e.g., #AND (a #NEAR/1 (b c)).
 *  Second, it is a place to store data structures and methods that are
 *  common to all query operators that return inverted lists.
 *  
 *  Copyright (c) 2015, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.*;


public abstract class QryopIl extends Qryop {

  /**
   *  Use the specified retrieval model to evaluate the query arguments.
   *  Define and return ArgPtrs pointers that the query operator can use.
   *  @param r A retrieval model that controls how the operator behaves.
   *  @return void
   *  @throws IOException
   */
  public void allocArgPtrs (RetrievalModel r) throws IOException {
	  
	
    for (int i=0; i<this.args.size(); i++) {
      ArgPtr ptri = new ArgPtr ();
      ptri.invList = this.args.get(i).evaluate(r).invertedList;
      ptri.scoreList = null;
      ptri.nextDoc = 0;
	
      this.argPtrs.add (ptri);
    }
  }
  public void allocArgPtrs (RetrievalModel r, InvList inv)  {

	      ArgPtr ptri = new ArgPtr ();
	      ptri.invList = inv;
	      ptri.scoreList = null;
	      ptri.nextDoc = 0;
		
	      this.argPtrs.add (ptri);
	  }

}
