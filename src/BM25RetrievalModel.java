import java.util.HashMap;


public class BM25RetrievalModel extends RetrievalModel {
	
	public static double K1;
	public static double B;
	public static double K3;

	
	HashMap<String, Double> params=new HashMap<String, Double>();
	@Override
	public boolean setParameter(String parameterName, double value) {
		// TODO Auto-generated method stub
		params.put(parameterName, value);
		return true;
	}

	@Override
	public boolean setParameter(String parameterName, String value) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public double getParameterDouble(String parameterName) {
		// TODO Auto-generated method stub
		Double value = params.get(parameterName);
        if (null != value) {
            return value;
        }
		return 0;
	}

	@Override
	public String getParameterString(String parameterName) {
		// TODO Auto-generated method stub
		return null;
	}

}
