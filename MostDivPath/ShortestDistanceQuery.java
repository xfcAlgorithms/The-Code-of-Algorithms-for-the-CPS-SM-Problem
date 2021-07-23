package function;
import java.io.BufferedReader;
import java.io.FileReader;

public class ShortestDistanceQuery {
	public native void ReadIndex(String filePath);
	public native double distanceQuery(int s, int t);
	
	static {
		System.loadLibrary("ShortestDistanceQuery");
	}


}
