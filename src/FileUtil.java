import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;


public class FileUtil {
	public static void appendToFile(String str, String filename) {
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(new File(filename), true));
			writer.write(str);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				writer.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	public static void writeToFile(String str, String filename) {
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(new File(filename)));
			writer.write(str);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				writer.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	
	
	public static ArrayList<String> readQueryFile(String file) {
		if (file == null)
			System.exit(1);
		Scanner scan;
		ArrayList<String> queries = new ArrayList<String>();
		try {
			scan = new Scanner(new File(file));
			while (scan.hasNext()) {
				queries.add(scan.nextLine());
			}
			scan.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return queries;
	}
	public static void readParams(String file, HashMap<String, String> params)
			throws FileNotFoundException {
		Scanner scan = new Scanner(new File(file));
		String line = null;
		do {
			line = scan.nextLine();
			String[] pair = line.split("=");
			if(pair.length>1){
				params.put(pair[0].trim(), pair[1].trim());
			}else{
				params.put(pair[0].trim(),"");
			}
		} while (scan.hasNext());
		scan.close();
	}
	public static void checkFileParams(HashMap<String, String> params) {
		checkParams("indexPath", params);
		checkParams("queryFilePath", params);
		checkParams("trecEvalOutputPath", params);
		checkParams("retrievalAlgorithm", params);
	}
	public static boolean checkParams(String str, HashMap<String, String> params) {
		if (!params.containsKey(str)) {
			fatalError("Error: Parameters were missing.");
			return false;
		}
		return true;
	}
	static void fatalError(String message) {
		System.err.println(message);
		System.exit(1);
	}
}
