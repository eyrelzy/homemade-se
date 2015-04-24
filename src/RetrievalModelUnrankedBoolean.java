/**
 *  The unranked Boolean retrieval model has no parameters.
 *
 *  Copyright (c) 2015, Carnegie Mellon University.  All Rights Reserved.
 */

public class RetrievalModelUnrankedBoolean extends RetrievalModel {

  /**
   * Set a retrieval model parameter.
   * @param parameterName
   * @param parametervalue
   * @return Always false because this retrieval model has no parameters.
   */
  public boolean setParameter (String parameterName, double value) {
    System.err.println ("Error: Unknown parameter name for retrieval model " +
			"UnrankedBoolean: " +
			parameterName);
    return false;
  }

  /**
   * Set a retrieval model parameter.
   * @param parameterName
   * @param parametervalue
   * @return Always false because this retrieval model has no parameters.
   */
  public boolean setParameter (String parameterName, String value) {
    System.err.println ("Error: Unknown parameter name for retrieval model " +
			"UnrankedBoolean: " +
			parameterName);
    return false;
  }

@Override
public double getParameterDouble(String parameterName) {
	// TODO Auto-generated method stub
	return 0;
}

@Override
public String getParameterString(String parameterName) {
	// TODO Auto-generated method stub
	return null;
}

}
