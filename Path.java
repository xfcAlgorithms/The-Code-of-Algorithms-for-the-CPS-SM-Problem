package function;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.TreeSet;

public class Path {
	double estInfCov;
	double curInfCov;
	double upInfCov;
	int cost;
	LinkedList<Integer> link;
	TreeSet<String> keywordsSet;
	HashMap<Integer, Double> cate_cover;
	HashMap<Integer, Integer> locID_numVis;
	public Path(double estInfCov, double curInfCov, int cost){
		this.estInfCov = estInfCov;
		this.curInfCov = curInfCov;
		this.cost = cost;
		this.link = new LinkedList<Integer> ();
		//this.cate_cover = new HashMap<Integer, Double> ();
	}
	public Path(int cost){
		this.cost = cost;
		this.link = new LinkedList<Integer> ();
	}
	

}
