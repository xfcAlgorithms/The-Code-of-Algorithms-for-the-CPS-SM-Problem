package function;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.TreeSet;
import java.util.Map.Entry;

public class MostInfPath {
	double Epsilon = 0.2;
	int maxNumLab = 10;
	int maxSubTrees = 5000000;
	int expTimes = 50;
	int startNodeID = 369;
	int endNodeID = 2;
	int budget = 21000;
	double gama = 0.2;
	//int genBudget = 20000;
	int binBudget = (int)(gama*budget);
	int numNodes = 23532;//49945,23532

	HashMap<Integer, HashMap<Integer, Double>> poi_user_coverage = new HashMap<Integer, HashMap<Integer, Double>>();//save poi_cate_coverage
    String sourcePath = "D:\\CPS\\bri\\";
	String inGPath = sourcePath + "bri.gr";
	String inSDIndexPath = sourcePath + "bri.index";
	String inLocUserProbPath = sourcePath + "probUserVisLocIndNew.txt";
	//BufferedReader inCW = new BufferedReader(new FileReader(sourcePath + "testCW.txt"));//input of the information for computing IC
	PrintWriter outCW = new PrintWriter(new File(sourcePath + "21randomCWDomCSVB.txt"));//
	PrintWriter outDe = new PrintWriter(new File(sourcePath + "21outputDetailsCSVB.txt"));//
	PrintWriter outOv = new PrintWriter(new File(sourcePath + "21outputOverallCSVB.txt"));//
	HashMap<Integer, Integer> failedCates = new HashMap<Integer, Integer>();
	String CWSEPath = sourcePath + "21randomCWDomRan3.txt";
    ShortestDistanceQuery sdq;
   
    public class Link {
    	public LinkedList<Node> link;
    	
    	public Link(){
    		this.link = new LinkedList<Node> ();
    	}
    }
    Link[] graph; //original graph
	Link[] graphRe; //reverse graph
	
	public class Node {
		public int nodeID;
		public int cost;
		public Node(int nodeID, int cost){
			this.nodeID = nodeID;
			this.cost = cost;
		}

	}
    //new label
    class LazyPathNode{
    	int nodeID;
    	int labelID;
    	LazyLabel souLabel;
    	LazyPathNode(int nodeID, int labelID){
    		this.nodeID = nodeID;
    		this.labelID = labelID;
    		this.souLabel = null;
    	}
    	LazyPathNode(int nodeID, int labelID, LazyLabel souLabel){
    		this.nodeID = nodeID;
    		this.labelID = labelID;
    		this.souLabel = souLabel;
    	}
    }
    boolean WheLazyPathConNode(LinkedList<LazyPathNode> aLazyPath, int nodeID){
    	boolean flag = false;
    	for(LazyPathNode aLazyPathNode : aLazyPath){
    		if(aLazyPathNode.nodeID == nodeID){
    			flag = true;
    			break;
    		}
    	}
    	
    	return flag;
    }
    class LazyLabel{
		LinkedList<LazyPathNode> lazyPath;
		double cost;
		//HashMap<Integer, Double> cate_cover;//for computing information coverage
		TreeSet<String> keywordsSet;
		HashMap<Integer, Double> user_prob;
		double infoCov;
		double feaPathInfoCov;
		double estInfoCov;
		double uppInfoCov;
		boolean isDom;
		boolean isPreChe;
		int checkedLabelID;
		int preBouNodeID;
		double preBouCost;
		double preBouInf;
		LazyLabel(LinkedList<LazyPathNode> lazyPath, double cost, double infoCov, TreeSet<String> keywordsSet){
			this.lazyPath = lazyPath;
			this.cost = cost;
			this.infoCov = infoCov;
			this.keywordsSet = keywordsSet;
			//this.cate_cover = cate_cover;
			this.isDom = false;
			this.feaPathInfoCov = 0.0;
			isPreChe = false;
		}
		LazyLabel(LinkedList<LazyPathNode> lazyPath, double cost, double infoCov, TreeSet<String> keywordsSet, double estInfoCov, double uppInfoCov){
			this.lazyPath = lazyPath;
			this.cost = cost;
			this.infoCov = infoCov;
			this.keywordsSet = keywordsSet;
			//this.cate_cover = cate_cover;
			this.isDom = false;
			this.feaPathInfoCov = 0.0;
			isPreChe = false;
			this.estInfoCov = estInfoCov;
			this.uppInfoCov = uppInfoCov;
		}
		LazyLabel(LinkedList<LazyPathNode> lazyPath, double cost, double infoCov, HashMap<Integer, Double> user_prob){
			this.lazyPath = lazyPath;
			this.cost = cost;
			this.infoCov = infoCov;
			this.user_prob = user_prob;
			//this.cate_cover = cate_cover;
			this.isDom = false;
			this.feaPathInfoCov = 0.0;
			isPreChe = false;
		}
		LazyLabel(LinkedList<LazyPathNode> lazyPath, double cost, double infoCov, HashMap<Integer, Double> user_prob, double estInfoCov, double uppInfoCov){
			this.lazyPath = lazyPath;
			this.cost = cost;
			this.infoCov = infoCov;
			this.user_prob = user_prob;
			//this.cate_cover = cate_cover;
			this.isDom = false;
			this.feaPathInfoCov = 0.0;
			isPreChe = false;
			this.estInfoCov = estInfoCov;
			this.uppInfoCov = uppInfoCov;
		}
	}
	//in descending cost order
	public static Comparator<LazyLabel> ComparatorLazy = new Comparator<LazyLabel>(){
		public int compare(LazyLabel l1, LazyLabel l2) {
			if(l1.cost <= l2.cost){//if l1.cost <= l2.cost, do not change the sort
				return -1;
			}
			else{//if l2 dominates l1, change the sort
				return 1;
			}
		}
	};
	class NodeLazyLabelArray{
		ArrayList<LazyLabel> labelArr; 
	}
	//old label
	class Label{
		LinkedList<Integer> path;
		double cost;
		double eff;
		TreeSet<String> keywordsSet;
		double infoCov;
		boolean isDom;
		HashMap<Integer, Double> user_prob;
		Label(LinkedList<Integer> path, double cost, double infoCov, TreeSet<String> keywordsSet){
			this.path = path;
			this.cost = cost;
			this.infoCov = infoCov;
			this.keywordsSet = keywordsSet;
			this.isDom = false;
			if(cost!=0)
				this.eff = infoCov/cost;
			else
				this.eff = Double.MAX_VALUE;
		}
		Label(LinkedList<Integer> path, double cost, double infoCov, HashMap<Integer, Double> user_prob){
			this.path = path;
			this.cost = cost;
			this.infoCov = infoCov;
			this.user_prob = user_prob;
			this.isDom = false;
			if(cost!=0)
				this.eff = infoCov/cost;
			else
				this.eff = Double.MAX_VALUE;
		}
	}
	//in ascending cost order
	public static Comparator<Label> Comparator = new Comparator<Label>(){
		public int compare(Label l1, Label l2) {
			if(l1.cost <= l2.cost){//if l1.cost <= l2.cost, do not change the sort
				return -1;
			}
			else{//if l2 dominates l1, change the sort
				return 1;
			}
		}
	};
	//in descending eff order
		public static Comparator<Label> ComparatorEff = new Comparator<Label>(){
			public int compare(Label l1, Label l2) {
				if(l1.eff >= l2.eff){//do not change the sort
					return -1;
				}
				else{//change the sort
					return 1;
				}
			}
		};
	class NodeLabelArray{
		ArrayList<Label> labelArr; 
		boolean isSorted;
		NodeLabelArray(){
			this.isSorted = false;
		}
	}
    
    public void init_basicBigG() throws Exception{
		graph = new Link[numNodes];
		graphRe = new Link[numNodes];
		//initialize graph
		for(int i=0; i<numNodes; i++){
			graph[i] = new Link();
			graphRe[i] = new Link();
		}
		int minCost = Integer.MAX_VALUE;
		int maxCost = Integer.MIN_VALUE;
		BufferedReader inBigG = new BufferedReader(new FileReader(inGPath));//input of the graph
		//complete matrix with inputed data
		 String sG;
		//read and store the test data
		while ((sG = inBigG.readLine()) != null) {
			int startNode;
			int endNode;
			int cost;

			//divide the data from graph
			String[] slG = sG.split("\t");
			if(slG.length == 3) {
				startNode = Integer.valueOf(slG[0]);
				endNode = Integer.valueOf(slG[1]);
				double costTemp = Double.valueOf(slG[2]);
				cost = (int)costTemp;
				if(cost < minCost)
					minCost = cost;
				if(cost > maxCost)
					maxCost = cost;
				Node node = new Node(endNode, cost);
				graph[startNode].link.add(node);
				Node nodeRe = new Node(startNode, cost);
				graphRe[endNode].link.add(nodeRe);
			}
		}
		System.out.println("minCost is: " + minCost);
		System.out.println("maxCost is: " + maxCost);

		inBigG.close();
	}
    public void init_locUserProb() throws Exception{
		//initialize poi_cate_coverage
		String s;
		int node = -1;
		String cate;
		double coverage = -1.0;
		BufferedReader inLocUserProb = new BufferedReader(new FileReader(inLocUserProbPath));//input of the graph
		while((s = inLocUserProb.readLine()) != null){
			String[] sl = s.split("\t");
			node = Integer.valueOf(sl[0]);
			for(int i=1; i<sl.length; i++){
				String [] sll = sl[i].split(":");
				cate = sll[0];
				int cateID = Integer.valueOf(cate);
				coverage = Double.valueOf(sll[1]);
				//coverage = 1.0;
				if(!poi_user_coverage.containsKey(node)){
					poi_user_coverage.put(node, new HashMap<Integer, Double>());
				}
				poi_user_coverage.get(node).put(cateID, coverage);
			}
		}
		inLocUserProb.close();
		
		//compute the average number of persons in a location
		int numPoi = poi_user_coverage.size();
		double aveNum = 0;
		for(Entry<Integer, HashMap<Integer, Double>> puc: poi_user_coverage.entrySet()) {
			aveNum = aveNum + puc.getValue().size();
		}
		aveNum = aveNum / numPoi;
		System.out.println("numPoi is: " + numPoi);
		System.out.println("aveNum is: " + aveNum);
	}
    public void init_SDIndex() throws Exception{
  		sdq = new ShortestDistanceQuery();
  		sdq.ReadIndex(inSDIndexPath);
  	}
    public MostInfPath() throws Exception{
    	init_basicBigG();
		init_locUserProb();
		init_SDIndex();
	}
    public double comNumUserProbJoin(HashMap<Integer, Double> user_prob1, HashMap<Integer, Double> user_prob2){
		double numUserProbJoin = 0.0;
		for(Entry<Integer, Double> entry:user_prob2.entrySet()) {
			int userID = entry.getKey();
			double prob2 = entry.getValue();
			if(user_prob1.containsKey(userID)) {
				double prob1 = user_prob1.get(userID);
				if(prob2 <= prob1) {
					numUserProbJoin = numUserProbJoin + prob2;
				}
				else {
					numUserProbJoin = numUserProbJoin + prob1;
				}
			}
			
		}
		
		return numUserProbJoin;
	}
    public double comUserProbImp(HashMap<Integer, Double> user_prob, int newNodeID, LinkedList<Integer> path, double preNumUser){
		double newNumUser = 0.0;
		//System.out.println("newNodeID is: " + newNodeID);
		if(path.contains(newNodeID) || !poi_user_coverage.containsKey(newNodeID)) {
			return preNumUser;
		}
		//update cate_cover
		for(Entry<Integer, Double> entry:poi_user_coverage.get(newNodeID).entrySet()){
			if(user_prob.containsKey(entry.getKey())){
				int cateID = entry.getKey();
				double preProb = user_prob.get(cateID);
				double newProb = 1 - ((1-entry.getValue())*(1-preProb));
				newNumUser = newNumUser + (newProb - preProb);
				user_prob.put(cateID, newProb);
			}
			else{
				user_prob.put(entry.getKey(), entry.getValue());
				newNumUser = newNumUser + entry.getValue();
			}
		}
		newNumUser = newNumUser + preNumUser;
		
		return newNumUser;
	}
    boolean wheNewPathIsDominEff(double cost, double infoCov, HashMap<Integer, Double> user_prob, NodeLabelArray nodeLableArray){
		boolean flag = false;
		ArrayList<Label> arrLab = nodeLableArray.labelArr;
		int numLab = arrLab.size();
		if(infoCov==0 && numLab>0)
			return true;
		
		for(int i=0; i<numLab; i++){
			Label aLabel = arrLab.get(i);
			if(aLabel.cost > cost)
				break;
			//compute the f(p_1 \join p_2)
			double joinInf = comNumUserProbJoin(user_prob, aLabel.user_prob);
			double diff = infoCov - joinInf;
			if(diff/aLabel.infoCov <= Epsilon){
				flag = true;
				break;
			}
		}
		
		return flag;
	}
    public void insSorLabelAscCost(ArrayList<Label> newLabList, Label aLabel){
		//find the position for inserting the new edge
		int left = 0;
		int right = newLabList.size()-1;
		int middle;
		//System.out.println("right is: " + right);
		while(left <= right){
			//System.out.println("right is: " + right);
			//System.out.println("left is: " + right);
			middle = (left + right) / 2;  
			if(aLabel.cost == newLabList.get(middle).cost){
				left = middle;
				break;
			}
			else if(aLabel.cost > newLabList.get(middle).cost){
				left = middle + 1;
			}
			else{
				right = middle - 1;
			}
		}
		newLabList.add(left, aLabel);
	}
    public void copyCCAToCCB(HashMap<Integer, Double> CCA, HashMap<Integer, Double> CCB){
		CCB.clear();
		for(Entry<Integer, Double> entry:CCA.entrySet()){
			CCB.put(entry.getKey(), entry.getValue());
		}
	}
    double comShoDisIndex(int thisStaNodeID, int thisEndNodeID){
		double shoDis = Double.MAX_VALUE;
		shoDis = sdq.distanceQuery(thisStaNodeID, thisEndNodeID);
		
		return shoDis;
	}
    public void copyAToB(LinkedList<Integer> A, LinkedList<Integer> B){
		B.clear();
		for(int nodeID:A){
			B.add(nodeID);
		}
	}
    boolean wheNewPathIsDomin(double cost, double infoCov, HashMap<Integer, Double> user_prob, NodeLabelArray nodeLableArray){
		boolean flag = false;
		ArrayList<Label> arrLab = nodeLableArray.labelArr;
		int numLab = arrLab.size();
		if(infoCov==0 && numLab>0)
			return true;
		
		for(int i=0; i<numLab; i++){
			Label aLabel = arrLab.get(i);
			//compute the f(p_1 \join p_2)
			double joinInf = comNumUserProbJoin(user_prob, aLabel.user_prob);
			double diff = infoCov - joinInf;
			if(diff/aLabel.infoCov <= Epsilon){
				flag = true;
				break;
			}
		}
		
		return flag;
	}
    public double comUserProbImpLazy(HashMap<Integer, Double> user_prob, int newNodeID, LinkedList<LazyPathNode> lazyPath, double preNumUser){
		double newNumUser = 0.0;
		//System.out.println("newNodeID is: " + newNodeID);
		for(LazyPathNode lazyNode:lazyPath) {
			if(newNodeID == lazyNode.nodeID)
				return preNumUser;
		}
		if(!poi_user_coverage.containsKey(newNodeID)) {
			return preNumUser;
		}
		//update cate_cover
		for(Entry<Integer, Double> entry:poi_user_coverage.get(newNodeID).entrySet()){
			if(user_prob.containsKey(entry.getKey())){
				int cateID = entry.getKey();
				double preProb = user_prob.get(cateID);
				double newProb = 1 - ((1-entry.getValue())*(1-preProb));
				newNumUser = newNumUser + (newProb - preProb);
				user_prob.put(cateID, newProb);
			}
			else{
				user_prob.put(entry.getKey(), entry.getValue());
				newNumUser = newNumUser + entry.getValue();
			}
		}
		newNumUser = newNumUser + preNumUser;
		
		return newNumUser;
	}
  //check the dominant relation between the new label and existing labels, 0-no dom, 1-potential dom, 2-dom
  	int wheNewPathIsPotDomPreIndexUserProb(LazyLabel newLabel, NodeLazyLabelArray nodeLableArray, LazyLabel labelMaxInf){
  		int flag = 0;
  		//double cost = newLabel.cost;
  		double infoCov = newLabel.infoCov;
  		HashMap<Integer, Double> user_prob = newLabel.user_prob;
  		ArrayList<LazyLabel> arrLab = nodeLableArray.labelArr;
  		int numLab = arrLab.size();
  		if(infoCov==0 && numLab>0)//deal with infoCov=0
  			return 2;
  		//compare the new label with the label with maximum inf
  		if(labelMaxInf != null){
  			double joinInfMax = comNumUserProbJoin(user_prob, labelMaxInf.user_prob);
  			double diffMax = infoCov - joinInfMax;
  			if(diffMax/labelMaxInf.infoCov <= Epsilon){
  				return 2;
  			}
  		}
  		for(int i=0; i<numLab; i++){
  			LazyLabel aLabel = arrLab.get(i);
  			//compute the f(p_1 \join p_2)
  			double joinInf = comNumUserProbJoin(user_prob, aLabel.user_prob);
  			double diff = infoCov - joinInf;
  			if(diff/aLabel.infoCov <= Epsilon){
  				return 2;
  			}
  			else if(flag == 0){
  				if(diff/aLabel.estInfoCov <= Epsilon){
  					flag = 1;
  				}
  			}
  		}
  		
  		return flag;
  	}
  	public void insSorLabelDesInf(ArrayList<LazyLabel> newLabList, LazyLabel aLabel){
		//find the position for inserting the new edge
		int left = 0;
		int right = newLabList.size()-1;
		int middle;
		//System.out.println("right is: " + right);
		while(left <= right){
			//System.out.println("right is: " + right);
			//System.out.println("left is: " + right);
			middle = (left + right) / 2;  
			if(aLabel.infoCov == newLabList.get(middle).infoCov){
				left = middle;
				break;
			}
			else if(aLabel.infoCov > newLabList.get(middle).infoCov){
				right = middle - 1;
			}
			else{
				left = middle + 1;
			}
		}
		newLabList.add(left, aLabel);
	}
  	boolean wheNewPathIsDomPreCurInf(double cost, double infoCov, HashMap<Integer, Double> user_prob, LazyLabel labelMaxInf){
		boolean flag = false;
		//compare the new label with the label with maximum inf
		if(labelMaxInf != null){
			if(infoCov!=0){
				double joinInfMax = comNumUserProbJoin(user_prob, labelMaxInf.user_prob);
				double diffMax = infoCov - joinInfMax;
				double resMax = diffMax/labelMaxInf.infoCov; 
				if(resMax <= Epsilon){
					flag = true;
				}
			}
			else{
				flag = true;
			}
		}
		return flag;
	}
  	public void copyTwoLazyPaths(LinkedList<LazyPathNode> A, LinkedList<LazyPathNode> B){
		B.clear();
		for(LazyPathNode node:A){
			B.add(node);
		}
	}
  	LazyLabel getMaxEstInfLab(double cost, int curNodeID, ArrayList<LazyLabel> arrLabel) {//for the labels are stored in descending OS value order in arrLabel
		LazyLabel maxEstInfLab = null;
		int numLab = arrLabel.size();
		for(int i=0; i<numLab; i++){
			LazyLabel aLabel = arrLabel.get(i);
			if(aLabel.cost <= cost) {
				maxEstInfLab = aLabel;
				break;
			}
		}
		
		return maxEstInfLab;
	}
  //For the labels are stored in descending OS value in nodeLableArray
	boolean wheNewPathIsDominPreCheTwoLabArrOPUserProb(LazyLabel newLabel, NodeLazyLabelArray nodeLableArray, NodeLazyLabelArray newNodeLableArray, LazyLabel maxEstInfLabel){
		double cost = newLabel.cost;
		double infoCov = newLabel.infoCov;
		HashMap<Integer, Double> user_prob = newLabel.user_prob;
		boolean flag = false;
		//check maxEstInfLabel
		if(maxEstInfLabel != null){
			double joinInfMax = comNumUserProbJoin(user_prob, maxEstInfLabel.user_prob);
			double diffMax = infoCov - joinInfMax;
			double resMax = 0.0;
			if(maxEstInfLabel.feaPathInfoCov != 0)
				resMax = diffMax/maxEstInfLabel.feaPathInfoCov;
			else
				resMax = diffMax/maxEstInfLabel.infoCov;
			if(maxEstInfLabel.cost <= cost){
				if(resMax <= Epsilon){
					flag = true;
					return flag;
				}
			}
		}
		//check the newNodeLableArray
		ArrayList<LazyLabel> newArrLab = newNodeLableArray.labelArr;
		int numLab = newArrLab.size();
		for(int i=0; i<numLab; i++){
			LazyLabel aLabel = newArrLab.get(i);
			if(aLabel.cost <= cost){
				//compute the f(p_1 \join p_2)
				double joinInf = comNumUserProbJoin(user_prob, aLabel.user_prob);
				double diff = infoCov - joinInf;
				double res = diff/aLabel.infoCov;
				if(res<= Epsilon){
					flag = true;
					break;
				}
			}
			else{//aLabel.cost > cost, flag=false
				System.out.println("aLabel.cost > cost");
				//break;
			}
		}
		
		//check the old nodeLableArray
		if(flag == false){
		ArrayList<LazyLabel> arrLab = nodeLableArray.labelArr;
		numLab = arrLab.size();
		for(int i=0; i<numLab; i++){
			LazyLabel aLabel = arrLab.get(i);
			if(aLabel.cost <= cost){
				//compute the f(p_1 \join p_2)
				double joinInf = comNumUserProbJoin(user_prob, aLabel.user_prob);
				double diff = infoCov - joinInf;
				double res = 0.0;
				if(aLabel.feaPathInfoCov != 0)
					res = diff/aLabel.feaPathInfoCov;
				else
					res = diff/aLabel.infoCov;
				if(res<= Epsilon){
					flag = true;
					break;
				}
			}
			else{//aLabel.cost > cost, flag=false
				//System.out.println("aLabel.cost > cost");
				continue;
			}
		}
		}
		return flag;
	}
	//check the dominant relation between the new label and existing labels, 0-no dom, 1-potential dom, 2-dom
	boolean wheNewPathIsDomPreUserProb(double cost, double infoCov, HashMap<Integer, Double> user_prob, LazyLabel labelEstMaxInf, LazyLabel labelMaxInf){
		boolean flag = false;
		//compare the new label with the label with maximum inf
		if(labelMaxInf != null){
			double joinInfMax = comNumUserProbJoin(user_prob, labelMaxInf.user_prob);
			double diffMax = infoCov - joinInfMax;
			double resMax = 0.0;
			if(labelMaxInf.feaPathInfoCov != 0)
				resMax = diffMax/labelMaxInf.feaPathInfoCov;
			else
				resMax = diffMax/labelMaxInf.infoCov;
			if(labelMaxInf.cost <= cost){
				if(diffMax/resMax <= Epsilon){
					flag = true;
				}
			}
		}
		//compare the new label with the label with maximum est inf
		if(labelEstMaxInf != null){
			double joinInfMax = comNumUserProbJoin(user_prob, labelEstMaxInf.user_prob);
			double diffMax = infoCov - joinInfMax;
			double resMax = 0.0;
			if(labelEstMaxInf.feaPathInfoCov != 0)
				resMax = diffMax/labelEstMaxInf.feaPathInfoCov;
			else
				resMax = diffMax/labelEstMaxInf.infoCov;
			if(labelEstMaxInf.cost <= cost){
				if(diffMax/resMax <= Epsilon){
					flag = true;
				}
			}
		}
		return flag;
	}
	public void insSorLabelDesInfCos(ArrayList<Label> newLabList, Label aLabel){
		//find the position for inserting the new edge
		int left = 0;
		int right = newLabList.size()-1;
		int middle;
		//System.out.println("right is: " + right);
		while(left <= right){
			//System.out.println("right is: " + right);
			//System.out.println("left is: " + right);
			middle = (left + right) / 2;  
			if(aLabel.cost == 0){
				left = 0;
				break;
			}
			Label curLabel = newLabList.get(middle);
			if(curLabel.cost == 0)
				left = middle + 1;
			else{ 
				if(aLabel.infoCov/aLabel.cost == curLabel.infoCov/curLabel.cost){
					left = middle;
					break;
				}
				else if(aLabel.infoCov/aLabel.cost > curLabel.infoCov/curLabel.cost){
					right = middle - 1;
				}
				else{
					left = middle + 1;
				}
			}
		}
		newLabList.add(left, aLabel);
	}
	boolean wheNewPathIsDomPre(double cost, double infoCov, HashMap<Integer, Double> user_prob, Label labelMaxInf){
		boolean flag = false;
		//compare the new label with the label with maximum inf
		if(labelMaxInf != null){
			double joinInfMax = comNumUserProbJoin(user_prob, labelMaxInf.user_prob);
			double diffMax = infoCov - joinInfMax;
			if(labelMaxInf.infoCov == 0) {
				return false;
			}
			else {
				double resMax = diffMax/labelMaxInf.infoCov;
				if(labelMaxInf.cost < cost){
					if(resMax <= Epsilon){
						flag = true;
					}
				}
				else if(labelMaxInf.cost == cost){
					if(resMax < Epsilon){
						flag = true;
					}
				}
			}
		}
		
		return flag;
	}
	void comUserProb(HashMap<Integer, Double> user_prob, int newNodeID) {
		//update cate_cover
		for(Entry<Integer, Double> entry:poi_user_coverage.get(newNodeID).entrySet()){
			if(user_prob.containsKey(entry.getKey())){
				int cateID = entry.getKey();
				double preProb = user_prob.get(cateID);
				double newProb = 1 - ((1-entry.getValue())*(1-preProb));
				user_prob.put(cateID, newProb);
			}
			else{
				user_prob.put(entry.getKey(), entry.getValue());
			}
		}
		
	}
	double comMerPath(Label sLab, Label tLab) {
		double newNumUser = 0.0;
		//System.out.println("newNodeID is: " + newNodeID);
		HashMap<Integer, Double> user_prob1 = sLab.user_prob;
		HashMap<Integer, Double> temp_user_prob = new HashMap<Integer, Double>();
		LinkedList<Integer> sPath = sLab.path;
		LinkedList<Integer> tPath = tLab.path;
		LinkedList<Integer> tempPath = new LinkedList<Integer>();
		
		for(int nodeID:tPath) {
			if(sPath.contains(nodeID) || tempPath.contains(nodeID)) {
				continue;
			}
			tempPath.add(nodeID);
			comUserProb(temp_user_prob, nodeID);
		}
		
		for(Entry<Integer, Double> entry:temp_user_prob.entrySet()) {
			int userID =  entry.getKey();
			double prob2 = entry.getValue();
			if(user_prob1.containsKey(userID)){
				double preProb = user_prob1.get(userID);
				newNumUser = newNumUser + (1-preProb)*prob2;
			}
			else{
				newNumUser = newNumUser + prob2;
			}
		}
		newNumUser = newNumUser + sLab.infoCov;
		
		return newNumUser;
	}
	void mergeTwoSamPaths(LinkedList<Integer> newPath, LinkedList<Integer> parPath) {
		LinkedList<Integer> parPathTemp = new LinkedList<Integer>();
		for(int nodeID:parPath) {
			parPathTemp.add(nodeID);
		}
		//System.out.println("test EndNodeID is: " + parPathTemp.getFirst());
		int sizeParPathTemp = parPathTemp.size();
		while(sizeParPathTemp > 0) {
			int nodeID = parPathTemp.removeLast();
			//System.out.println("test nodeID is: " + nodeID);
			newPath.add(nodeID);//get current path.link
			sizeParPathTemp--;
		}
		//System.out.println("test staNodeID is: " + newPath.getFirst().nodeID);
		//System.out.println("test EndNodeID is: " + newPath.getLast().nodeID);
	}
	Label MergeLabWithParLabtUpdBestLab(Label aLabel, NodeLabelArray labelArrRe, Label curBestLable) {
		if(labelArrRe == null)
			return null;
		ArrayList<Label> arrLab = labelArrRe.labelArr;
		int numLab= arrLab.size();
		if(numLab == 0)
			return null;
		
		double staBestInf = -1.0;
		if(curBestLable != null)
			staBestInf = curBestLable.infoCov;
		int bestLabIndex = -1;
		double bestInf = -1.0;
		//System.out.println("numLab is: " + numLab);
		if(curBestLable != null) {
			bestInf = curBestLable.infoCov;
		}
		//scan all labels in labelArrRe for finding the best merged label
		for(int i=0; i<numLab; i++) {
			Label curLab = arrLab.get(i);
			if(curLab.cost + aLabel.cost > budget) {
				continue;
			}
			//estimate the influence of the merged label
			double estInf = aLabel.infoCov + curLab.infoCov;
			if(staBestInf >= estInf)
				break;
			//if curLab.path has curNodeID, prune curNodeID from it
			int curNodeID = aLabel.path.getLast();
			if(curLab.path.size()>0 && curLab.path.getLast() == curNodeID) {
				curLab.path.removeLast();
			}
			
			//compute the influence of the merged label
			double curInf = comMerPath(aLabel, curLab);
			if(curInf > bestInf) {//update the best label
				bestInf = curInf;
				bestLabIndex = i;			
			}
		}
		//System.out.println("numNonSimPath is: " + numNonSimPath);
		//System.out.println("bestInf is: " + bestInf);
		if(bestLabIndex == -1)
			return null;
		//merge two paths for getting the best merged path and then update labelArrEndNode
		Label bestParLab = arrLab.get(bestLabIndex);
		//generate a label for current path
		LinkedList<Integer> newPath = new LinkedList<Integer> ();
		copyAToB(aLabel.path, newPath);
		mergeTwoSamPaths(newPath, bestParLab.path);
		HashMap<Integer, Double> newUser_prob = new HashMap<Integer, Double>();
		//double newNumUser = comMerPathUpdUserProb(aLabel, bestParLab, newUser_prob);
		Label newLabel = new Label(newPath, aLabel.cost+bestParLab.cost, bestInf, newUser_prob);
		//System.out.println("newKeywordsSet.size() is: " + newKeywordsSet.size());
		//update curBestLable
		curBestLable = newLabel;
		
		return curBestLable;
	}
	void mergeTwoPaths(LinkedList<LazyPathNode> newPath, LinkedList<Integer> parPath) {
		LinkedList<Integer> parPathTemp = new LinkedList<Integer>();
		for(int nodeID:parPath) {
			parPathTemp.add(nodeID);
		}
		//System.out.println("test EndNodeID is: " + parPathTemp.getFirst());
		int sizeParPathTemp = parPathTemp.size();
		while(sizeParPathTemp > 0) {
			int nodeID = parPathTemp.removeLast();
			//System.out.println("test nodeID is: " + nodeID);
			LazyPathNode aPathNode = new LazyPathNode(nodeID, -1);//labID = -1 means that this node doesn't have a corresponding label
			newPath.add(aPathNode);//get current path.link
			sizeParPathTemp--;
		}
	}
	boolean containNodeLazyPath(LinkedList<LazyPathNode> path, int nodeID) {
		boolean flag = false;
		for(LazyPathNode curLazyNode:path) {
			if(curLazyNode.nodeID == nodeID) {
				flag = true;
				break;
			}
		}
		
		return flag;
	}
	double comMerPath(LazyLabel sLab, Label tLab) {
		double newNumUser = 0.0;
		//System.out.println("newNodeID is: " + newNodeID);
		HashMap<Integer, Double> user_prob1 = sLab.user_prob;
		HashMap<Integer, Double> temp_user_prob = new HashMap<Integer, Double>();
		LinkedList<LazyPathNode> sPath = sLab.lazyPath;
		LinkedList<Integer> tPath = tLab.path;
		LinkedList<Integer> tempPath = new LinkedList<Integer>();
		
		for(int nodeID:tPath) {
			if(containNodeLazyPath(sPath, nodeID) || tempPath.contains(nodeID)) {
				continue;
			}
			tempPath.add(nodeID);
			comUserProb(temp_user_prob, nodeID);
		}
		
		for(Entry<Integer, Double> entry:temp_user_prob.entrySet()) {
			int userID =  entry.getKey();
			double prob2 = entry.getValue();
			if(user_prob1.containsKey(userID)){
				double preProb = user_prob1.get(userID);
				newNumUser = newNumUser + (1-preProb)*prob2;
			}
			else{
				newNumUser = newNumUser + prob2;
			}
		}
		newNumUser = newNumUser + sLab.infoCov;
		
		return newNumUser;
	}
	LazyLabel MergeLabWithParLabUpdLabelArr(LazyLabel aLabel, NodeLabelArray labelArrRe, NodeLazyLabelArray labelArrEndNode) {
		if(labelArrRe == null)
			return null;
		ArrayList<Label> arrLab = labelArrRe.labelArr;
		int numLab= arrLab.size();
		if(numLab == 0)
			return null;
		double bestInf = -1.0;
		int bestLabIndex = -1;
		//scan all labels in labelArrRe for finding the best merged label
		for(int i=0; i<numLab; i++) {
			Label curLab = arrLab.get(i);
			if(curLab.cost + aLabel.cost > budget) {
				continue;
			}
			//estimate the influence of the merged label
			double estInf = aLabel.infoCov + curLab.infoCov;
			if(bestInf >= estInf)
				break;
			//if curLab.path has curNodeID, prune curNodeID from it
			int curNodeID = aLabel.lazyPath.getLast().nodeID;
			if(curLab.path.size()>0 && curLab.path.getLast() == curNodeID) {
				curLab.path.removeLast();
			}
			//compute the influence of the merged label
			double curInf = comMerPath(aLabel, curLab);
			if(curInf > bestInf) {//update the best label
				bestInf = curInf;
				bestLabIndex = i;
							
			}
		}
		if(bestLabIndex == -1)
			return null;
		//merge two paths for getting the best merged path and then update labelArrEndNode
		Label bestParLab = arrLab.get(bestLabIndex);
		//generate a label for current path
		LinkedList<LazyPathNode> newPath = new LinkedList<LazyPathNode> ();
		copyTwoLazyPaths(aLabel.lazyPath, newPath);
		mergeTwoPaths(newPath, bestParLab.path);
		HashMap<Integer, Double> newUser_prob = new HashMap<Integer, Double>();
		//double newNumUser = mergeTwoUserProb(newUser_prob, aLabel.user_prob, bestParLab.user_prob, aLabel.infoCov);
		LazyLabel newLabel = new LazyLabel(newPath, aLabel.cost+bestParLab.cost, bestInf, newUser_prob, Double.MAX_VALUE, Double.MAX_VALUE);
		//update curBestLable
		labelArrEndNode.labelArr.add(newLabel);
			
		return newLabel;
	}
	LazyLabel MergeLabWithParLabUpdBestLabUserProb(LazyLabel aLabel, NodeLabelArray labelArrRe, LazyLabel curBestLable) {
		if(labelArrRe == null)
			return null;
		ArrayList<Label> arrLab = labelArrRe.labelArr;
		int numLab= arrLab.size();
		if(numLab == 0)
			return null;
		double staBestInf = -1.0;
		if(curBestLable != null)
			staBestInf = curBestLable.infoCov;
		int bestLabIndex = -1;
		//scan all labels in labelArrRe for finding the best merged label
		for(int i=0; i<numLab; i++) {
			Label curLab = arrLab.get(i);
			if(curLab.cost + aLabel.cost > budget) {
				continue;
			}
			//estimate the influence of the merged label
			double estInf = aLabel.infoCov + curLab.infoCov;
			if(staBestInf >= estInf)
				break;
			//if curLab.path has curNodeID, prune curNodeID from it
			int curNodeID = aLabel.lazyPath.getLast().nodeID;
			if(curLab.path.size()>0 && curLab.path.getLast() == curNodeID) {
				curLab.path.removeLast();
			}
			//compute the influence of the merged label
			double curInf = comMerPath(aLabel, curLab);
			if(curInf > staBestInf) {//update the best label
				staBestInf = curInf;
				bestLabIndex = i;
							
			}
		}
		if(bestLabIndex == -1)
			return null;
		//merge two paths for getting the best merged path and then update labelArrEndNode
		Label bestParLab = arrLab.get(bestLabIndex);
		//generate a label for current path
		LinkedList<LazyPathNode> newPath = new LinkedList<LazyPathNode> ();
		copyTwoLazyPaths(aLabel.lazyPath, newPath);
		mergeTwoPaths(newPath, bestParLab.path);
		HashMap<Integer, Double> newUser_prob = new HashMap<Integer, Double>();
		//double newNumUser = mergeTwoUserProb(newUser_prob, aLabel.user_prob, bestParLab.user_prob, aLabel.infoCov);
		LazyLabel newLabel = new LazyLabel(newPath, aLabel.cost+bestParLab.cost, staBestInf, newUser_prob, Double.MAX_VALUE, Double.MAX_VALUE);
		//update curBestLable
		curBestLable = newLabel;
			
		return curBestLable;
	}
  	
    double runAppAlgDomSDIndexNS(){
		long begin_time = System.currentTimeMillis();
		long time_comDis = 0;
		int times_comDis = 0;
		//initialize lowToEndShoDis
		double[] lowToEndShoDis = new double [numNodes];
		for(int i=0; i<numNodes; i++){
			lowToEndShoDis[i] = -1;
		}
		//initialize 
		Queue<Label> PQLabel = new PriorityQueue<Label>(10, Comparator);//to store the partial path
		//initialize PQLabel with startNodeID
		double curCost = 0;
		HashMap<Integer, Double> user_prob = new HashMap<Integer, Double> ();
		LinkedList<Integer> newPath = new LinkedList<Integer> ();
		double curInfoCov = comUserProbImp(user_prob, startNodeID, newPath, 0);
		//System.out.println("curInfoCov is: " + curInfoCov);
		newPath.add(startNodeID);
		Label aLabel = new Label(newPath, curCost, curInfoCov, user_prob);
		PQLabel.add(aLabel);
		Label curBestLable = aLabel;
		//initialize an array to store the path ending with nodes
		NodeLabelArray [] nodeLabArr = new NodeLabelArray [numNodes];
		for(int i=0; i<numNodes; i++){
			nodeLabArr[i] = new NodeLabelArray();
			nodeLabArr[i].labelArr = new ArrayList<Label>();
		}
		while(PQLabel.size()>0){
			//System.out.println("PQLabel.size() is: " + PQLabel.size());
			long cur_time = System.currentTimeMillis();
			long run_time = cur_time - begin_time;
			if(PQLabel.size() > maxSubTrees){
				System.out.println("PQPath.size() > maxSubTrees!!!");
				return -1.0;
			}
			if(run_time > 100000){
				System.out.println("Run_time>100000!!!");
				if(curBestLable.path.size()==0){
					System.out.println("curBestLable.path.size()==0");
					System.out.println("No Solution!!!");
					//System.out.println(curBestLink.size());
					outDe.println("No Solution!!!");
					return 0.0;
				}
				else if(curBestLable.path.getLast() != endNodeID){
					System.out.println("curBestLable.path.getLast() != endNodeID");
					System.out.println("No Solution!!!");
					//System.out.println(curBestLink.size());
					outDe.println("No Solution!!!");
					return 0.0;
				}
				else{
					System.out.println("The best route is: ");
					outDe.println("The best route is: ");
					for(int nodeID: curBestLable.path){
						System.out.print(nodeID + " -> ");
						outDe.print(nodeID + " -> ");
					}
				}
				outDe.print("bestInf is: " + curBestLable.infoCov);
				outDe.println("Run_time > 300000!!!");
				outDe.flush();
				
				return 0;
			}
			aLabel = PQLabel.poll();
			LinkedList<Integer> prePath = aLabel.path;
			int curEndNode = prePath.getLast();
			//System.out.println("PQLabel.poll()!!! ");
			if(curEndNode == endNodeID){
				if(aLabel.infoCov > curBestLable.infoCov || curBestLable.infoCov==0){//update the best path
					curBestLable = aLabel;
				}
				
			}
			//check whether the new path is dominated by existing paths ending with curEndNode
			boolean newPathIsDom = wheNewPathIsDomin(aLabel.cost, aLabel.infoCov, aLabel.user_prob, nodeLabArr[curEndNode]);
			//System.out.println("wheNewPathIsDomin!!! ");
			if(newPathIsDom == true){//prune the label
				//System.out.println("newPathIsDom == true!!! ");
				continue;
			}
			
			//store the label into nodeLabArr[curEndNode] and extend the aLabel.path
			//store the label
			nodeLabArr[curEndNode].labelArr.add(aLabel);
			//extend the aLabel.path
			//System.out.println("curEndNode is: " + curEndNode);
			//System.out.println("numExt is: " + graph[curEndNode].link.size());
			for(Node node : graph[curEndNode].link){
				//System.out.println("extend!!! ");
				if(lowToEndShoDis[node.nodeID] == -1){
					long begin_timeTemp1 = System.currentTimeMillis();
					lowToEndShoDis[node.nodeID] = comShoDisIndex(node.nodeID, endNodeID);
					time_comDis = time_comDis + (System.currentTimeMillis() - begin_timeTemp1);
					times_comDis++;
					//System.out.println(node.nodeID + " -> " + endNodeID + " is: " + lowToEndShoDis[node.nodeID]);
				}
				if(node.cost!=Integer.MAX_VALUE && lowToEndShoDis[node.nodeID]!=Integer.MAX_VALUE && (aLabel.cost + node.cost + lowToEndShoDis[node.nodeID]) <= budget){//just consider the nonexistent nodes
					curCost = aLabel.cost + node.cost;
					if(curCost <= budget){
						newPath = new LinkedList<Integer> ();
						copyAToB(prePath, newPath);
						HashMap<Integer, Double> newUser_prob = new HashMap<Integer, Double> ();
						copyCCAToCCB(aLabel.user_prob, newUser_prob);
						curInfoCov = comUserProbImp(newUser_prob, node.nodeID, newPath, aLabel.infoCov);
						newPath.add(node.nodeID);//get current path.link
						//System.out.println("curInfoCov is: " + curInfoCov);
						
						//generate a label for current path
						Label newLabel = new Label(newPath, curCost, curInfoCov, newUser_prob);
						PQLabel.add(newLabel);
					}
				}
			}	
		}
		long end_time = System.currentTimeMillis();
		System.out.println("The run time is: " + (end_time - begin_time));
		outDe.println("The run time is: " + (end_time - begin_time));
		outDe.println("curBestScore is: " + curBestLable.infoCov);
		outDe.println("curShoCost is: " + curBestLable.cost);
		if(curBestLable.path.size()==0){
			System.out.println("curBestLable.path.size()==0");
			System.out.println("No Solution!!!");
			//System.out.println(curBestLink.size());
			outDe.println("No Solution!!!");
			return 0.0;
		}
		else if(curBestLable.path.getLast() != endNodeID){
			System.out.println("curBestLable.path.getLast() != endNodeID");
			System.out.println("No Solution!!!");
			//System.out.println(curBestLink.size());
			outDe.println("No Solution!!!");
			return 0.0;
		}
		else{
			System.out.println("The best route is: ");
			outDe.println("The best route is: ");
			for(int nodeID: curBestLable.path){
				System.out.print(nodeID + " -> ");
				outDe.print(nodeID + " -> ");
			}
		}
		System.out.println("end!");
		System.out.println("time_comDis is: " + time_comDis);
		System.out.println("times_comDis is: " + times_comDis);
		outDe.println("end!");
		outDe.flush();
		
		return curBestLable.infoCov;
	}
    double runAppAlgDomLUSDIndexOP(){
		long begin_time = System.currentTimeMillis();
		long ostime = 0;
		//initialize lowToEndShoDis
		double[] lowToEndShoDis = new double [numNodes];
		for(int i=0; i<numNodes; i++){
			lowToEndShoDis[i] = -1;
		}
		//System.out.println("shortest dis is: " + comLowShoDisSub(startNodeID, endNodeID, endNode_subIndex));
		Queue<LazyLabel> PQLabel = new PriorityQueue<LazyLabel>(10, ComparatorLazy);//to store the partial path
		Queue<LazyLabel> PotPQLabel = new PriorityQueue<LazyLabel>(10, ComparatorLazy);//to store the potential dominated partial path
		//initialize PQLabel with startNodeID
		double curCost = 0;
		LinkedList<LazyPathNode> newPath = new LinkedList<LazyPathNode> ();
		HashMap<Integer, Double> user_prob = new HashMap<Integer, Double> ();
		double curInfoCov = comUserProbImpLazy(user_prob, startNodeID, newPath, 0);
		double estInfCov = curInfoCov;//comEstInfCovSubInd(osTabArr[startNodeID], startNodeID, budget, 0.0, null, endNode_subIndex, staNode_subIndex);
		//System.out.println("estInfCov is: " + estInfCov);
		
		LazyPathNode aPathNode = new LazyPathNode(startNodeID, 0);
		newPath.add(aPathNode);
		LazyLabel aLabel = new LazyLabel(newPath, curCost, curInfoCov, user_prob, estInfCov, Double.MAX_VALUE);;
		PQLabel.add(aLabel);
		aPathNode.souLabel = aLabel;
		//initialize an array to store the path ending with nodes
		NodeLazyLabelArray [] nodeLabzyLabArr = new NodeLazyLabelArray [numNodes];
		for(int i=0; i<numNodes; i++){
			nodeLabzyLabArr[i] = new NodeLazyLabelArray();
			nodeLabzyLabArr[i].labelArr = new ArrayList<LazyLabel>();
		}
		long begin_time2 = System.currentTimeMillis();
		LazyLabel curBestLab = aLabel;
		//extend all non-dom labels
		while(PQLabel.size()>0){
			//System.out.println("PQLabel.size() is: " + PQLabel.size());
			long cur_time = System.currentTimeMillis();
			long run_time = cur_time - begin_time;
			if(PQLabel.size() > maxSubTrees){
				System.out.println("PQPath.size() > maxSubTrees!!!");
				return 0.0;
			}
			if(run_time > 600000){
				System.out.println("Run_time>600000!!!");
				outDe.println("Run_time>600000!!!");
				outDe.println("curInf is: " + 0);
				
				return 0.0;
			}
			aLabel = PQLabel.poll();
			LinkedList<LazyPathNode> prePath = aLabel.lazyPath;
			aPathNode = prePath.getLast();
			int curEndNode = aPathNode.nodeID;
			LazyLabel maxInfLabel = null;
			if(nodeLabzyLabArr[curEndNode].labelArr.size() > 0)
				maxInfLabel = nodeLabzyLabArr[curEndNode].labelArr.get(0);
			//check whether the new path is dominated by existing paths ending with curEndNode
			//0-no dom, 1-potential dom, 2-dom
			int newPathIsPotDom = wheNewPathIsPotDomPreIndexUserProb(aLabel, nodeLabzyLabArr[curEndNode], maxInfLabel);
			if(curEndNode == endNodeID ) {
				insSorLabelDesInf(nodeLabzyLabArr[curEndNode].labelArr, aLabel);
				if(curBestLab.infoCov < aLabel.infoCov)
					curBestLab = aLabel;
			}
			//System.out.println(curEndNode + " -> " + endNodeID + " newPathIsPotDom is: " + newPathIsPotDom);
			if(newPathIsPotDom == 2)//dom, prune this label
				continue;
			if(newPathIsPotDom == 1)//potential dom, insert this label into PotPQLabel
				PotPQLabel.add(aLabel);
			else{
				//insert the label into nodeLabzyLabArr[curEndNode].labelArr in descending order of OS value and update the label ID
				if(curEndNode != endNodeID)
					insSorLabelDesInf(nodeLabzyLabArr[curEndNode].labelArr, aLabel);
				
				//extend the aLabel.path
				for(Node node : graph[curEndNode].link){
					if(lowToEndShoDis[node.nodeID] == -1){
						long begin_timeTemp2 = System.currentTimeMillis();
						lowToEndShoDis[node.nodeID] = comShoDisIndex(node.nodeID, endNodeID);
						ostime = ostime + (System.currentTimeMillis()- begin_timeTemp2);
						//System.out.println(node.nodeID + " -> " + endNodeID + " is: " + lowToEndShoDis[node.nodeID]);
					}
					//if(!WheLazyPathConNode(prePath, node.nodeID) && node.cost!=Integer.MAX_VALUE && lowToEndShoDis[node.nodeID]!=Double.MAX_VALUE && (aLabel.cost + node.cost + lowToEndShoDis[node.nodeID]) <= budget){//just consider the nonexistent nodes
					if(node.cost!=Integer.MAX_VALUE && lowToEndShoDis[node.nodeID]!=Double.MAX_VALUE && (aLabel.cost + node.cost + lowToEndShoDis[node.nodeID]) <= budget){//just consider the nonexistent nodes
						curCost = aLabel.cost + node.cost;
						if(curCost <= budget){
							HashMap<Integer, Double> newUser_prob = new HashMap<Integer, Double> ();
							copyCCAToCCB(aLabel.user_prob, newUser_prob);
							curInfoCov = comUserProbImpLazy(newUser_prob, node.nodeID, aLabel.lazyPath, aLabel.infoCov);
							if(node.nodeID != endNodeID) {
								maxInfLabel = null;
								if(nodeLabzyLabArr[node.nodeID].labelArr.size() > 0)
									maxInfLabel = nodeLabzyLabArr[node.nodeID].labelArr.get(0);
								boolean preDomFlag = wheNewPathIsDomPreCurInf(curCost, curInfoCov, newUser_prob, maxInfLabel);
								if(preDomFlag == true)
									continue;
							}
							long sta_time2 = System.currentTimeMillis();
							estInfCov = curInfoCov/curCost*budget;
							double uppInfCov = Double.MAX_VALUE;
							//generate a label for current path
							newPath = new LinkedList<LazyPathNode> ();
							copyTwoLazyPaths(prePath, newPath);
							aPathNode = new LazyPathNode(node.nodeID, 0);
							newPath.add(aPathNode);//get current path.link

							LazyLabel newLabel = new LazyLabel(newPath, curCost, curInfoCov, newUser_prob, estInfCov, uppInfCov);
							PQLabel.add(newLabel);
							aPathNode.souLabel = newLabel;	
						}
					}
				}	
			}
		}
		System.out.println("ostime is: " + ostime);
		//long end_time2 = System.currentTimeMillis();
		//System.out.println("Finish extend all non-dom labels! The runTime is: " + (end_time2 - begin_time2));
		//begin_time2 = System.currentTimeMillis();
		//update the estimated infoCov for all labels
		NodeLazyLabelArray [] nodeLabzyLabArrRes = new NodeLazyLabelArray [numNodes];
		if(PotPQLabel.size() > 0) {
			ArrayList<LazyLabel> endNodelabelArr = nodeLabzyLabArr[endNodeID].labelArr;
			int numLabel = endNodelabelArr.size();
			for(int i=0; i<numLabel; i++){
				aLabel = endNodelabelArr.get(i);
				double feaPathInfCov = aLabel.infoCov;
				LinkedList<LazyPathNode> aPath = aLabel.lazyPath;
				for(LazyPathNode aPathNode2: aPath){
					LazyLabel aLabel2 =  aPathNode2.souLabel;
					if(aLabel2.feaPathInfoCov < feaPathInfCov)
						aLabel2.feaPathInfoCov = feaPathInfCov;
				}
			}
			System.out.println("PotPQLabel.size() is: " + PotPQLabel.size());
			for(int i=0; i<numNodes; i++){
				nodeLabzyLabArrRes[i] = new NodeLazyLabelArray();
				nodeLabzyLabArrRes[i].labelArr = new ArrayList<LazyLabel>();
			}
		}
		//check the labels in PotPQLabel and extend the non-dom labels
		while(PotPQLabel.size() > 0){
			long cur_time = System.currentTimeMillis();
			long run_time = cur_time - begin_time;
			if(PotPQLabel.size() > maxSubTrees){
				System.out.println("PotPQLabel.size() > maxSubTrees!!!");
				return -1.0;
			}
			if(run_time > 600000){
				System.out.println("Run_time>600000!!!");
				outDe.println("Run_time>600000!!!");
				outDe.println("curInf is: " + 0);
				
				return 0.0;
			}
			//System.out.println("PotPQLabel.size() is: " + PotPQLabel.size());
			aLabel = PotPQLabel.poll();
			LinkedList<LazyPathNode> prePath = aLabel.lazyPath;
			aPathNode = prePath.getLast();
			int curEndNode = aPathNode.nodeID;
			//update maxEstInfLabel[curEndNode] and maxEstInfLabVisIndex[i]
			LazyLabel maxEstInfLab = getMaxEstInfLab(aLabel.cost, curEndNode, nodeLabzyLabArr[curEndNode].labelArr);
			//check whether the new path is dominated by existing paths ending with curEndNode
			boolean newPathIsDom = wheNewPathIsDominPreCheTwoLabArrOPUserProb(aLabel, nodeLabzyLabArr[curEndNode], nodeLabzyLabArrRes[curEndNode], maxEstInfLab);
			if(newPathIsDom == true){//prune the label
				continue;
			}
			else{//store the label into nodeLabArr[curEndNode] and extend the aLabel.path
				//insert the label into nodeLabArr and update maxInfLabel[curEndNode]
				//nodeLabzyLabArrRes[curEndNode].labelArr.add(aLabel);
				insSorLabelDesInf(nodeLabzyLabArrRes[curEndNode].labelArr, aLabel);
				//update maxInfLabel
				//extend the aLabel.path
				for(Node node : graph[curEndNode].link){
					if(lowToEndShoDis[node.nodeID] == -1){
						long begin_timeTemp2 = System.currentTimeMillis();
						lowToEndShoDis[node.nodeID] = comShoDisIndex(node.nodeID, endNodeID);
						ostime = ostime + (System.currentTimeMillis()- begin_timeTemp2);
					}
					if(node.cost!=Integer.MAX_VALUE &&  lowToEndShoDis[node.nodeID]!=Integer.MAX_VALUE && (aLabel.cost + node.cost + lowToEndShoDis[node.nodeID]) <= budget){//just consider the nonexistent nodes
						curCost = aLabel.cost + node.cost;
						if(curCost <= budget){
							HashMap<Integer, Double> newUser_prob = new HashMap<Integer, Double> ();
							copyCCAToCCB(aLabel.user_prob, newUser_prob);
							curInfoCov = comUserProbImpLazy(newUser_prob, node.nodeID, aLabel.lazyPath, aLabel.infoCov);
							if(node.nodeID == endNodeID && (curBestLab.infoCov < aLabel.infoCov)) {
								double uppInfCov = Double.MAX_VALUE;
								newPath = new LinkedList<LazyPathNode> ();
								copyTwoLazyPaths(prePath, newPath);
								aPathNode = new LazyPathNode(node.nodeID, 0);
								newPath.add(aPathNode);//get current path.link
								LazyLabel newLabel = new LazyLabel(newPath, curCost, curInfoCov, newUser_prob, estInfCov, uppInfCov);
								curBestLab = newLabel;
							}

							LazyLabel theMaxInfLabel = null;
							if(nodeLabzyLabArrRes[node.nodeID].labelArr.size()>0)
								theMaxInfLabel = nodeLabzyLabArrRes[node.nodeID].labelArr.get(0);
							//update maxEstInfLabel[curEndNode] and maxEstInfLabVisIndex[i]
							maxEstInfLab = getMaxEstInfLab(aLabel.cost, node.nodeID, nodeLabzyLabArr[node.nodeID].labelArr);
							boolean preDomFlag = wheNewPathIsDomPreUserProb(curCost, curInfoCov, newUser_prob, maxEstInfLab, theMaxInfLabel);
							if(preDomFlag == true)
								continue;
							
							estInfCov = Double.MAX_VALUE;
							double uppInfCov = Double.MAX_VALUE;
							//generate a label for current path
							newPath = new LinkedList<LazyPathNode> ();
							copyTwoLazyPaths(prePath, newPath);
							aPathNode = new LazyPathNode(node.nodeID, 0);
							newPath.add(aPathNode);//get current path.link
							LazyLabel newLabel = new LazyLabel(newPath, curCost, curInfoCov, newUser_prob, estInfCov, uppInfCov);
							PotPQLabel.add(newLabel);
						}
					}
				}
			}
		}
		//get the best solution by scanning the labels in nodeLabzyLabArr[endNodeID].labelArr
		long end_time = System.currentTimeMillis();
		System.out.println("The run time is: " + (end_time - begin_time));
		outDe.println("The run time is: " + (end_time - begin_time));
		outDe.println("curBestScore is: " + curBestLab.infoCov);
		outDe.println("curShoCost is: " + curBestLab.cost);
		
		if(curBestLab == null){
			System.out.println("curBestLable.path.size()==0");
			System.out.println("No Solution!!!");
			//System.out.println(curBestLink.size());
			outDe.println("No Solution!!!");
			return 0.0;
		}
		else{
			System.out.println("The best route is: ");
			outDe.println("The best route is: ");
			for(LazyPathNode aPathNode2: curBestLab.lazyPath){
				System.out.print(aPathNode2.nodeID + " -> ");
				outDe.print(aPathNode2.nodeID + " -> ");
			}
		}
		System.out.println("end!");
		outDe.println("end!");
		outDe.flush();
		
		return curBestLab.infoCov;
	}
    double runAppAlgDomSDIndexBiNS(){
		long begin_time = System.currentTimeMillis();
		long time_comDis = 0;
		int times_comDis = 0;
		//initialize lowToEndShoDis
		double[] lowToEndShoDis = new double [numNodes];
		for(int i=0; i<numNodes; i++){
			lowToEndShoDis[i] = -1;
		}
		//step1:find all non-dominated partial path from endNode to startNode under \gama*budget
		//initialize PQLabelRe
		Queue<Label> PQLabelRe = new PriorityQueue<Label>(10, Comparator);//to store the partial path
		HashMap<Integer, Double> user_prob = new HashMap<Integer, Double> ();
		LinkedList<Integer> newPathRe = new LinkedList<Integer> ();
		double curInfoCov = comUserProbImp(user_prob, endNodeID, newPathRe, 0);
		newPathRe.add(endNodeID);
		Label aLabelRe = new Label(newPathRe, 0, curInfoCov, user_prob);
		PQLabelRe.add(aLabelRe);
		//initialize an array to store the path ending with nodes
		NodeLabelArray [] nodeLabArrRe = new NodeLabelArray [numNodes];
		double curCost = 0;
		for(int i=0; i<numNodes; i++){
			nodeLabArrRe[i] = new NodeLabelArray();
			nodeLabArrRe[i].labelArr = new ArrayList<Label>();
		}
		//int testNodeID = 0;
		while(PQLabelRe.size()>0){
			//System.out.println("PQLabel.size() is: " + PQLabel.size());
			long cur_time = System.currentTimeMillis();
			long run_time = cur_time - begin_time;
			if(PQLabelRe.size() > maxSubTrees){
				System.out.println("PQPath.size() > maxSubTrees!!!");
				return -1.0;
			}
			/*if(run_time > 100000){
				System.out.println("Run_time>100000!!!");
				return -1;
			}*/
			aLabelRe = PQLabelRe.poll();
			LinkedList<Integer> prePath = aLabelRe.path;
			int curEndNode = prePath.getLast();
			//check whether the new path is dominated by existing paths ending with curEndNode
			boolean newPathIsDom = wheNewPathIsDomin(aLabelRe.cost, aLabelRe.infoCov, aLabelRe.user_prob, nodeLabArrRe[curEndNode]);
			if(newPathIsDom == true){//prune the label
				continue;
			}
			else{//store the label into nodeLabArr[curEndNode] and extend the aLabel.path
				//store the label
				insSorLabelDesInfCos(nodeLabArrRe[curEndNode].labelArr, aLabelRe);
				//nodeLabArrRe[curEndNode].labelArr.add(aLabelRe);
				//testNodeID = curEndNode;
				//System.out.println("nodeLabArrRe[curEndNode].labelArr.size() is: " + nodeLabArrRe[curEndNode].labelArr.size());
				//extend the aLabel.path
				for(Node node : graphRe[curEndNode].link){
					double disToStaNode = comShoDisIndex(startNodeID, node.nodeID);
					if(node.cost!=Integer.MAX_VALUE && disToStaNode!= Double.MAX_VALUE && (aLabelRe.cost + node.cost + disToStaNode <= budget)){//just consider the nonexistent nodes
						curCost = aLabelRe.cost + node.cost;
						if(curCost <= binBudget){
							HashMap<Integer, Double> newUser_prob = new HashMap<Integer, Double> ();
							copyCCAToCCB(aLabelRe.user_prob, newUser_prob);
							curInfoCov = comUserProbImp(newUser_prob, node.nodeID, aLabelRe.path, aLabelRe.infoCov);
							if(nodeLabArrRe[node.nodeID].labelArr.size() > 0) {
								if(wheNewPathIsDomPre(curCost, curInfoCov, newUser_prob, nodeLabArrRe[node.nodeID].labelArr.get(0)) == true)
									continue;
							}
							newPathRe = new LinkedList<Integer> ();
							copyAToB(prePath, newPathRe);
							newPathRe.add(node.nodeID);//get current path.link
							//generate a label for current path
							Label newLabel = new Label(newPathRe, curCost, curInfoCov, newUser_prob);
							PQLabelRe.add(newLabel);
						}
					}
				}
			}	
			
		}
		//System.out.println("test is: " + nodeLabArrRe[testNodeID].labelArr.get(0).path.getFirst());
		System.out.println("binBudget is: " + binBudget);
		System.out.println("The run time of getting partial paths from t is: " + (System.currentTimeMillis()-begin_time));
		//initialize 
		Queue<Label> PQLabel = new PriorityQueue<Label>(10, Comparator);//to store the partial path
		//initialize PQLabel with startNodeID
		curCost = 0;
		user_prob = new HashMap<Integer, Double> ();
		LinkedList<Integer> newPath = new LinkedList<Integer> ();
		curInfoCov = comUserProbImp(user_prob, startNodeID, newPath, 0);
		newPath.add(startNodeID);
		Label aLabel = new Label(newPath, curCost, curInfoCov, user_prob);
		PQLabel.add(aLabel);
		Label curBestLable = aLabel;
		//initialize an array to store the path ending with nodes
		NodeLabelArray [] nodeLabArr = new NodeLabelArray [numNodes];
		for(int i=0; i<numNodes; i++){
			nodeLabArr[i] = new NodeLabelArray();
			nodeLabArr[i].labelArr = new ArrayList<Label>();
		}
		//int numNonMatch = 0;
		while(PQLabel.size()>0){
			//System.out.println("PQLabel.size() is: " + PQLabel.size());
			long cur_time = System.currentTimeMillis();
			long run_time = cur_time - begin_time;
			if(PQLabel.size() > maxSubTrees){
				System.out.println("PQPath.size() > maxSubTrees!!!");
				return -1.0;
			}
			if(run_time > 300000){
				System.out.println("Run_time>100000!!!");
				return -1;
			}
			aLabel = PQLabel.poll();
			LinkedList<Integer> prePath = aLabel.path;
			int curEndNode = prePath.getLast();
			//check whether the new path is dominated by existing paths ending with curEndNode
			boolean newPathIsDom = wheNewPathIsDomin(aLabel.cost, aLabel.infoCov, aLabel.user_prob, nodeLabArr[curEndNode]);
			if(newPathIsDom == true){//prune the label
				continue;
			}
			else{//store the label into nodeLabArr[curEndNode] and extend the aLabel.path
				//store the label
				nodeLabArr[curEndNode].labelArr.add(aLabel);
				//extend the aLabel.path
				if(aLabel.cost >= budget - binBudget) {
					Label newBestLab = MergeLabWithParLabtUpdBestLab(aLabel, nodeLabArrRe[curEndNode], curBestLable);
					if(newBestLab != null) {
						curBestLable = newBestLab;
						//System.out.println("curBestLable.infoCov is: " + curBestLable.infoCov);
					}
					//numNonMatch++;
				}
				else {
					for(Node node : graph[curEndNode].link){
						if(lowToEndShoDis[node.nodeID] == -1){
							long begin_timeTemp1 = System.currentTimeMillis();
							lowToEndShoDis[node.nodeID] = comShoDisIndex(node.nodeID, endNodeID);
							time_comDis = time_comDis + (System.currentTimeMillis() - begin_timeTemp1);
							times_comDis++;
							//System.out.println(node.nodeID + " -> " + endNodeID + " is: " + lowToEndShoDis[node.nodeID]);
						}
						if(node.cost!=Integer.MAX_VALUE && lowToEndShoDis[node.nodeID]!=Integer.MAX_VALUE && (aLabel.cost + node.cost + lowToEndShoDis[node.nodeID]) <= budget){//just consider the nonexistent nodes
							curCost = aLabel.cost + node.cost;
							if(curCost <= budget){
								newPath = new LinkedList<Integer> ();
								copyAToB(prePath, newPath);
								newPath.add(node.nodeID);//get current path.link
								HashMap<Integer, Double> newUser_prob = new HashMap<Integer, Double> ();
								copyCCAToCCB(aLabel.user_prob, newUser_prob);
								curInfoCov = comUserProbImp(newUser_prob, node.nodeID, aLabel.path, aLabel.infoCov);
								//generate a label for current path
								Label newLabel = new Label(newPath, curCost, curInfoCov, newUser_prob);
								if(node.nodeID == endNodeID) {
									if(newLabel.infoCov > curBestLable.infoCov){//update the best path
										curBestLable = newLabel;
									}
								}
								PQLabel.add(newLabel);
							}
						}	
					}
				}
			}
		}
		long end_time = System.currentTimeMillis();
		//System.out.println("numNonMatch is: " + numNonMatch);
		System.out.println("The run time is: " + (end_time - begin_time));
		outDe.println("The run time is: " + (end_time - begin_time));
		outDe.println("curBestScore is: " + curBestLable.infoCov);
		outDe.println("curShoCost is: " + curBestLable.cost);
		if(curBestLable.path.size()==0){
			System.out.println("curBestLable.path.size()==0");
			System.out.println("No Solution!!!");
			//System.out.println(curBestLink.size());
			outDe.println("No Solution!!!");
			return 0.0;
		}
		else if(curBestLable.path.getLast() != endNodeID){
			System.out.println("curBestLable.path.size() is: " + curBestLable.path.size());
			System.out.println("curBestLable.path.getLast() != endNodeID");
			System.out.println("No Solution!!!");
			//System.out.println(curBestLink.size());
			outDe.println("No Solution!!!");
			return 0.0;
		}
		else{
			System.out.println("The best route is: ");
			outDe.println("The best route is: ");
			for(int nodeID: curBestLable.path){
				System.out.print(nodeID + " -> ");
				outDe.print(nodeID + " -> ");
			}
		}
		System.out.println("end!");
		System.out.println("time_comDis is: " + time_comDis);
		System.out.println("times_comDis is: " + times_comDis);
		outDe.println("end!");
		outDe.flush();
		
		return curBestLable.infoCov;
	}
    double runAppAlgDomLUSDIndexBiOP(){
		long begin_time = System.currentTimeMillis();
		long ostime = 0;
		//initialize lowToEndShoDis
		double[] lowToEndShoDis = new double [numNodes];
		for(int i=0; i<numNodes; i++){
			lowToEndShoDis[i] = -1;
		}
		
		//step1:find all non-dominated partial path from endNode to startNode under \gama*budget
		//initialize PQLabelRe
		Queue<Label> PQLabelRe = new PriorityQueue<Label>(10, Comparator);//to store the partial path
		HashMap<Integer, Double> user_prob = new HashMap<Integer, Double> ();
		LinkedList<Integer> newPathRe = new LinkedList<Integer> ();
		double curInfoCov = comUserProbImp(user_prob, endNodeID, newPathRe, 0);
		newPathRe.add(endNodeID);
		Label aLabelRe = new Label(newPathRe, 0, curInfoCov, user_prob);
		PQLabelRe.add(aLabelRe);
		//initialize an array to store the path ending with nodes
		NodeLabelArray [] nodeLabArrRe = new NodeLabelArray [numNodes];
		double curCost = 0;
		for(int i=0; i<numNodes; i++){
			nodeLabArrRe[i] = new NodeLabelArray();
			nodeLabArrRe[i].labelArr = new ArrayList<Label>();
		}
		//int testNodeID = 0;
		while(PQLabelRe.size()>0){
			//System.out.println("PQLabel.size() is: " + PQLabel.size());
			long cur_time = System.currentTimeMillis();
			long run_time = cur_time - begin_time;
			if(PQLabelRe.size() > maxSubTrees){
				System.out.println("PQPath.size() > maxSubTrees!!!");
				return -1.0;
			}
			/*if(run_time > 100000){
				System.out.println("Run_time>100000!!!");
				return -1;
			}*/
			aLabelRe = PQLabelRe.poll();
			LinkedList<Integer> prePath = aLabelRe.path;
			int curEndNode = prePath.getLast();
			//check whether the new path is dominated by existing paths ending with curEndNode
			boolean newPathIsDom = wheNewPathIsDomin(aLabelRe.cost, aLabelRe.infoCov, aLabelRe.user_prob, nodeLabArrRe[curEndNode]);
			if(newPathIsDom == true){//prune the label
				continue;
			}
			else{//store the label into nodeLabArr[curEndNode] and extend the aLabel.path
				//store the label
				//nodeLabArrRe[curEndNode].labelArr.add(aLabelRe);
				insSorLabelDesInfCos(nodeLabArrRe[curEndNode].labelArr, aLabelRe);
				//testNodeID = curEndNode;
				//System.out.println("nodeLabArrRe[curEndNode].labelArr.size() is: " + nodeLabArrRe[curEndNode].labelArr.size());
				//extend the aLabel.path
				for(Node node : graphRe[curEndNode].link){
					double disToStaNode = comShoDisIndex(startNodeID, node.nodeID);
					if(node.cost!=Integer.MAX_VALUE && disToStaNode!= Double.MAX_VALUE && (aLabelRe.cost + node.cost + disToStaNode <= budget)){//just consider the nonexistent nodes
						curCost = aLabelRe.cost + node.cost;
						if(curCost <= binBudget){
							HashMap<Integer, Double> newUser_prob = new HashMap<Integer, Double> ();
							copyCCAToCCB(aLabelRe.user_prob, newUser_prob);
							curInfoCov = comUserProbImp(newUser_prob, node.nodeID, prePath, aLabelRe.infoCov);
							if(nodeLabArrRe[node.nodeID].labelArr.size() > 0) {
								if(wheNewPathIsDomPre(curCost, curInfoCov, newUser_prob, nodeLabArrRe[node.nodeID].labelArr.get(0)) == true)
									continue;
							}
							newPathRe = new LinkedList<Integer> ();
							copyAToB(prePath, newPathRe);
							newPathRe.add(node.nodeID);//get current path.link
							//generate a label for current path
							Label newLabel = new Label(newPathRe, curCost, curInfoCov, newUser_prob);
							PQLabelRe.add(newLabel);
						}
					}
				}
			}	
			
		}
		//System.out.println("test is: " + nodeLabArrRe[testNodeID].labelArr.get(0).path.getFirst());
		System.out.println("binBudget is: " + binBudget);
		System.out.println("The run time of getting partial paths from t is: " + (System.currentTimeMillis()-begin_time));
		Queue<LazyLabel> PQLabel = new PriorityQueue<LazyLabel>(10, ComparatorLazy);//to store the partial path
		Queue<LazyLabel> PotPQLabel = new PriorityQueue<LazyLabel>(10, ComparatorLazy);//to store the potential dominated partial path
		//initialize PQLabel with startNodeID
		LazyLabel curBestLable = null;
		curCost = 0;
		user_prob = new HashMap<Integer, Double> ();
		LinkedList<LazyPathNode> newPath = new LinkedList<LazyPathNode> ();
		curInfoCov = comUserProbImpLazy(user_prob, startNodeID, newPath, 0);
		double estInfCov = curInfoCov;//comEstInfCovSubInd(osTabArr[startNodeID], startNodeID, budget, 0.0, null, endNode_subIndex, staNode_subIndex);
		//System.out.println("estInfCov is: " + estInfCov);
		LazyPathNode aPathNode = new LazyPathNode(startNodeID, 0);
		newPath.add(aPathNode);
		LazyLabel aLabel = new LazyLabel(newPath, curCost, curInfoCov, user_prob, estInfCov, Double.MAX_VALUE);;
		PQLabel.add(aLabel);
		aPathNode.souLabel = aLabel;
		//initialize an array to store the path ending with nodes
		NodeLazyLabelArray [] nodeLabzyLabArr = new NodeLazyLabelArray [numNodes];
		for(int i=0; i<numNodes; i++){
			nodeLabzyLabArr[i] = new NodeLazyLabelArray();
			nodeLabzyLabArr[i].labelArr = new ArrayList<LazyLabel>();
			
		}
		long begin_time2 = System.currentTimeMillis();
		//extend all non-dom labels
		while(PQLabel.size()>0){
			//System.out.println("PQLabel.size() is: " + PQLabel.size());
			long cur_time = System.currentTimeMillis();
			long run_time = cur_time - begin_time;
			if(PQLabel.size() > maxSubTrees){
				System.out.println("PQPath.size() > maxSubTrees!!!");
				return -1.0;
			}
			aLabel = PQLabel.poll();
			LinkedList<LazyPathNode> prePath = aLabel.lazyPath;
			aPathNode = prePath.getLast();
			int curEndNode = aPathNode.nodeID;
			LazyLabel maxInfLabel = null;
			if(nodeLabzyLabArr[curEndNode].labelArr.size() > 0)
				maxInfLabel = nodeLabzyLabArr[curEndNode].labelArr.get(0);
			//check whether the new path is dominated by existing paths ending with curEndNode
			//0-no dom, 1-potential dom, 2-dom
			int newPathIsPotDom = wheNewPathIsPotDomPreIndexUserProb(aLabel, nodeLabzyLabArr[curEndNode], maxInfLabel);
			if(curEndNode == endNodeID) {//if node.nodeID == endNodeID, try to update curBestLable
				insSorLabelDesInf(nodeLabzyLabArr[curEndNode].labelArr, aLabel);
				if(curBestLable==null || curBestLable.infoCov < aLabel.infoCov) {
					curBestLable = aLabel;
				}
			}
			//System.out.println(curEndNode + " -> " + endNodeID + " newPathIsPotDom is: " + newPathIsPotDom);
			if(newPathIsPotDom == 2)//dom, prune this label
				continue;
			if(newPathIsPotDom == 1)//potential dom, insert this label into PotPQLabel
				PotPQLabel.add(aLabel);
			else{
				//insert the label into nodeLabzyLabArr[curEndNode].labelArr in descending order of OS value
				if(curEndNode != endNodeID)
					insSorLabelDesInf(nodeLabzyLabArr[curEndNode].labelArr, aLabel);
				
				//extend the aLabel.path
				if(aLabel.cost >= budget - binBudget)
				{
					LazyLabel newBestLab = MergeLabWithParLabUpdLabelArr(aLabel, nodeLabArrRe[curEndNode], nodeLabzyLabArr[endNodeID]);
					if(curBestLable == null)
						curBestLable = newBestLab;
					else if(newBestLab != null && curBestLable.infoCov < newBestLab.infoCov) {
						curBestLable = newBestLab;
					}
				}
				else {
					for(Node node : graph[curEndNode].link){
						if(lowToEndShoDis[node.nodeID] == -1){
							long begin_timeTemp2 = System.currentTimeMillis();
							lowToEndShoDis[node.nodeID] = comShoDisIndex(node.nodeID, endNodeID);
							ostime = ostime + (System.currentTimeMillis()- begin_timeTemp2);
							//System.out.println(node.nodeID + " -> " + endNodeID + " is: " + lowToEndShoDis[node.nodeID]);
						}
						if(node.cost!=Integer.MAX_VALUE && lowToEndShoDis[node.nodeID]!=Double.MAX_VALUE && (aLabel.cost + node.cost + lowToEndShoDis[node.nodeID]) <= budget){//just consider the nonexistent nodes
							curCost = aLabel.cost + node.cost;
							HashMap<Integer, Double> newUser_prob = new HashMap<Integer, Double> ();
							copyCCAToCCB(aLabel.user_prob, newUser_prob);
							curInfoCov = comUserProbImpLazy(newUser_prob, node.nodeID, prePath, aLabel.infoCov);	
							//extend the label
							if(node.nodeID != endNodeID) {
								maxInfLabel = null;
								if(nodeLabzyLabArr[node.nodeID].labelArr.size() > 0)
									maxInfLabel = nodeLabzyLabArr[node.nodeID].labelArr.get(0);
								boolean preDomFlag = wheNewPathIsDomPreCurInf(curCost, curInfoCov, newUser_prob, maxInfLabel);
								if(preDomFlag == true)
									continue;
							}
							long sta_time2 = System.currentTimeMillis();
							ArrayList<Label> nodeLabArrCur = nodeLabArrRe[node.nodeID].labelArr;
							if(nodeLabArrCur.size() == 0)
								estInfCov = curInfoCov/curCost*budget;
							else
								estInfCov = (curInfoCov + nodeLabArrCur.get(0).infoCov) / (double)(curCost + nodeLabArrCur.get(0).cost) * budget;
							double uppInfCov = Double.MAX_VALUE;
							//generate a label for current path
							newPath = new LinkedList<LazyPathNode> ();
							copyTwoLazyPaths(prePath, newPath);
							aPathNode = new LazyPathNode(node.nodeID, 0);
							newPath.add(aPathNode);//get current path.link
							LazyLabel newLabel = new LazyLabel(newPath, curCost, curInfoCov, newUser_prob, estInfCov, uppInfCov);
							aPathNode.souLabel = newLabel;
							PQLabel.add(newLabel);	
						}	
					}
					
				}
			}
		}
		System.out.println("ostime is: " + ostime);
		//update the estimated infoCov for all labels
		ArrayList<LazyLabel> endNodelabelArr = nodeLabzyLabArr[endNodeID].labelArr;
		int numLabel = endNodelabelArr.size();
		for(int i=0; i<numLabel; i++){
			aLabel = endNodelabelArr.get(i);
			//System.out.println("test is: " + aLabel.lazyPath.getLast().nodeID);
			double feaPathInfCov = aLabel.infoCov;
			LinkedList<LazyPathNode> aPath = aLabel.lazyPath;
			for(LazyPathNode aPathNode2: aPath){
				if(aPathNode2.souLabel != null) {
					LazyLabel aLabel2 =  aPathNode2.souLabel;
					if(aLabel2.feaPathInfoCov < feaPathInfCov)
						aLabel2.feaPathInfoCov = feaPathInfCov;
				}
			}
		}
		System.out.println("PotPQLabel.size() is: " + PotPQLabel.size());
		//check the labels in PotPQLabel and extend the non-dom labels
		NodeLazyLabelArray [] nodeLabzyLabArrRes = new NodeLazyLabelArray [numNodes];
		for(int i=0; i<numNodes; i++){
			nodeLabzyLabArrRes[i] = new NodeLazyLabelArray();
			nodeLabzyLabArrRes[i].labelArr = new ArrayList<LazyLabel>();
		}
		while(PotPQLabel.size()>0){
			//System.out.println("PotPQLabel.size() is: " + PotPQLabel.size());
			aLabel = PotPQLabel.poll();
			LinkedList<LazyPathNode> prePath = aLabel.lazyPath;
			aPathNode = prePath.getLast();
			int curEndNode = aPathNode.nodeID;
			//update maxEstInfLabel[curEndNode] and maxEstInfLabVisIndex[i]
			LazyLabel maxEstInfLab = getMaxEstInfLab(aLabel.cost, curEndNode, nodeLabzyLabArr[curEndNode].labelArr);
			//check whether the new path is dominated by existing paths ending with curEndNode
			boolean newPathIsDom = wheNewPathIsDominPreCheTwoLabArrOPUserProb(aLabel, nodeLabzyLabArr[curEndNode], nodeLabzyLabArrRes[curEndNode], maxEstInfLab);
			if(newPathIsDom == true){//prune the label
				continue;
			}
			else{
				//store the label into nodeLabArr[curEndNode] and extend the aLabel.path
				//insert the label into nodeLabArr and update maxInfLabel[curEndNode]
				//nodeLabzyLabArrRes[curEndNode].labelArr.add(aLabel);
				insSorLabelDesInf(nodeLabzyLabArrRes[curEndNode].labelArr, aLabel);
				//update maxInfLabel
				//extend the aLabel.path
				if(aLabel.cost >= budget - binBudget)
				{
					LazyLabel newBestLab = MergeLabWithParLabUpdBestLabUserProb(aLabel, nodeLabArrRe[curEndNode], curBestLable);
					if(newBestLab != null) {
						curBestLable = newBestLab;
					}
				}
				else {
					for(Node node : graph[curEndNode].link){
						if(lowToEndShoDis[node.nodeID] == -1){
							long begin_timeTemp2 = System.currentTimeMillis();
							lowToEndShoDis[node.nodeID] = comShoDisIndex(node.nodeID, endNodeID);
							ostime = ostime + (System.currentTimeMillis()- begin_timeTemp2);
						}
						if(node.cost!=Integer.MAX_VALUE &&  lowToEndShoDis[node.nodeID]!=Integer.MAX_VALUE && (aLabel.cost + node.cost + lowToEndShoDis[node.nodeID]) <= budget){//just consider the nonexistent nodes
							curCost = aLabel.cost + node.cost;
							HashMap<Integer, Double> newUser_prob = new HashMap<Integer, Double> ();
							copyCCAToCCB(aLabel.user_prob, newUser_prob);
							curInfoCov = comUserProbImpLazy(newUser_prob, node.nodeID, prePath, aLabel.infoCov);
							//if node.nodeID == endNodeID, try to update curBestLable
							if(node.nodeID == endNodeID && (curBestLable==null || curBestLable.infoCov < curInfoCov)) {
								double uppInfCov = Double.MAX_VALUE;
								newPath = new LinkedList<LazyPathNode> ();
								copyTwoLazyPaths(prePath, newPath);
								aPathNode = new LazyPathNode(node.nodeID, 0);
								newPath.add(aPathNode);//get current path.link
								LazyLabel newLabel = new LazyLabel(newPath, curCost, curInfoCov, newUser_prob, estInfCov, uppInfCov);
								curBestLable = newLabel;
							}
							//extend the label
							LazyLabel theMaxInfLabel = null;
							if(nodeLabzyLabArrRes[node.nodeID].labelArr.size()>0)
								theMaxInfLabel = nodeLabzyLabArrRes[node.nodeID].labelArr.get(0);
							maxEstInfLab = getMaxEstInfLab(curCost, node.nodeID, nodeLabzyLabArr[node.nodeID].labelArr);
							boolean preDomFlag = wheNewPathIsDomPreUserProb(curCost, curInfoCov, newUser_prob, maxEstInfLab, theMaxInfLabel);
							if(preDomFlag == true)
								continue;
							estInfCov = Double.MAX_VALUE;
							double uppInfCov = Double.MAX_VALUE;
							//generate a label for current path
							newPath = new LinkedList<LazyPathNode> ();
							copyTwoLazyPaths(prePath, newPath);
							aPathNode = new LazyPathNode(node.nodeID, 0);
							newPath.add(aPathNode);//get current path.link
							LazyLabel newLabel = new LazyLabel(newPath, curCost, curInfoCov, newUser_prob, estInfCov, uppInfCov);
							PotPQLabel.add(newLabel);
						}
					}
				}
			}
		}
		//get the best solution by scanning the labels in nodeLabzyLabArr[endNodeID].labelArr
		long end_time = System.currentTimeMillis();
		System.out.println("The run time is: " + (end_time - begin_time));
		outDe.println("The run time is: " + (end_time - begin_time));
		outDe.println("curBestScore is: " + curBestLable.infoCov);
		outDe.println("curShoCost is: " + curBestLable.cost);
		
		if(curBestLable == null){
			System.out.println("curBestLable.path.size()==0");
			System.out.println("No Solution!!!");
			//System.out.println(curBestLink.size());
			outDe.println("No Solution!!!");
			return 0.0;
		}
		else{
			System.out.println("The best route is: ");
			outDe.println("The best route is: ");
			for(LazyPathNode aPathNode2: curBestLable.lazyPath){
				System.out.print(aPathNode2.nodeID + " -> ");
				outDe.print(aPathNode2.nodeID + " -> ");
			}
		}
		System.out.println("end!");
		outDe.println("end!");
		outDe.flush();
		
		return curBestLable.infoCov;
	}
    double runAppAlgDomSLIndexConLab(){
		long begin_time = System.currentTimeMillis();
		long time_comDis = 0;
		int times_comDis = 0;
		//initialize lowToEndShoDis
		double[] lowToEndShoDis = new double [numNodes];
		for(int i=0; i<numNodes; i++){
			lowToEndShoDis[i] = -1;
		}
		//initialize 
		Queue<Label> PQLabel = new PriorityQueue<Label>(10, ComparatorEff);//to store the partial path
		//initialize PQLabel with startNodeID
		double curCost = 0;
		HashMap<Integer, Double> user_prob = new HashMap<Integer, Double> ();
		LinkedList<Integer> newPath = new LinkedList<Integer> ();
		double curInfoCov = comUserProbImp(user_prob, startNodeID, newPath, 0);
		newPath.add(startNodeID);
		Label aLabel = new Label(newPath, curCost, curInfoCov, user_prob);
		PQLabel.add(aLabel);
		Label curBestLable = aLabel;
		//initialize an array to store the path ending with nodes
		NodeLabelArray [] nodeLabArr = new NodeLabelArray [numNodes];
		//Label [] maxInfLabArr = new Label [numNodes];
		for(int i=0; i<numNodes; i++){
			nodeLabArr[i] = new NodeLabelArray();
			nodeLabArr[i].labelArr = new ArrayList<Label>();
		}
		while(PQLabel.size()>0){
			//System.out.println("PQLabel.size() is: " + PQLabel.size());
			long cur_time = System.currentTimeMillis();
			long run_time = cur_time - begin_time;
			if(PQLabel.size() > maxSubTrees){
				System.out.println("PQPath.size() > maxSubTrees!!!");
				return -1.0;
			}
			/*if(run_time > 100000){
				System.out.println("Run_time>100000!!!");
				return -1;
			}*/
			aLabel = PQLabel.poll();
			LinkedList<Integer> prePath = aLabel.path;
			int curEndNode = prePath.getLast();
			if(curEndNode == endNodeID){
				if(aLabel.infoCov > curBestLable.infoCov || curBestLable.infoCov==0){//update the best path
					curBestLable = aLabel;
				}
			}
			
			//check whether the new path is dominated by existing paths ending with curEndNode
			boolean newPathIsDom = false;
			if(nodeLabArr[curEndNode].labelArr.size() < maxNumLab)
				newPathIsDom = wheNewPathIsDominEff(aLabel.cost, aLabel.infoCov, aLabel.user_prob, nodeLabArr[curEndNode]);
			else
				newPathIsDom = true;
			if(newPathIsDom == true){//prune the label
				continue;
			}
			else{//store the label into nodeLabArr[curEndNode] and extend the aLabel.path
				//store the label
				//nodeLabArr[curEndNode].labelArr.add(aLabel);
				insSorLabelAscCost(nodeLabArr[curEndNode].labelArr, aLabel);
				//extend the aLabel.path
				for(Node node : graph[curEndNode].link){
					if(nodeLabArr[node.nodeID].labelArr.size() >= maxNumLab)
						continue;
					if(lowToEndShoDis[node.nodeID] == -1){
						long begin_timeTemp1 = System.currentTimeMillis();
						lowToEndShoDis[node.nodeID] = comShoDisIndex(node.nodeID, endNodeID);
						time_comDis = time_comDis + (System.currentTimeMillis() - begin_timeTemp1);
						times_comDis++;
						//System.out.println(node.nodeID + " -> " + endNodeID + " is: " + lowToEndShoDis[node.nodeID]);
					}
					if(node.cost!=Integer.MAX_VALUE && lowToEndShoDis[node.nodeID]!=Integer.MAX_VALUE && (aLabel.cost + node.cost + lowToEndShoDis[node.nodeID]) <= budget){//just consider the nonexistent nodes
						curCost = aLabel.cost + node.cost;
						if(curCost <= budget){
							HashMap<Integer, Double> newUser_prob = new HashMap<Integer, Double> ();
							copyCCAToCCB(aLabel.user_prob, newUser_prob);
							newPath = new LinkedList<Integer> ();
							copyAToB(prePath, newPath);
							curInfoCov = comUserProbImp(newUser_prob, node.nodeID, newPath, aLabel.infoCov);
							newPath.add(node.nodeID);//get current path.link
							//generate a label for current path
							Label newLabel = new Label(newPath, curCost, curInfoCov, newUser_prob);
							PQLabel.add(newLabel);
						}
					}
				}
			}	
			
		}
		long end_time = System.currentTimeMillis();
		System.out.println("The run time is: " + (end_time - begin_time));
		outDe.println("The run time is: " + (end_time - begin_time));
		outDe.println("curBestScore is: " + curBestLable.infoCov);
		outDe.println("curShoCost is: " + curBestLable.cost);
		if(curBestLable.path.size()==0){
			System.out.println("curBestLable.path.size()==0");
			System.out.println("No Solution!!!");
			//System.out.println(curBestLink.size());
			outDe.println("No Solution!!!");
			return 0.0;
		}
		else if(curBestLable.path.getLast() != endNodeID){
			System.out.println("curBestLable.path.getLast() != endNodeID");
			System.out.println("No Solution!!!");
			//System.out.println(curBestLink.size());
			outDe.println("No Solution!!!");
			return 0.0;
		}
		else{
			System.out.println("The best route is: ");
			outDe.println("The best route is: ");
			for(int nodeID: curBestLable.path){
				System.out.print(nodeID + " -> ");
				outDe.print(nodeID + " -> ");
			}
		}
		System.out.println("end!");
		System.out.println("time_comDis is: " + time_comDis);
		System.out.println("times_comDis is: " + times_comDis);
		outDe.println("end!");
		outDe.flush();
		
		return curBestLable.infoCov;
	}
    public void runExperimentsVarB() throws Exception{
		long runTime = 0;
		long begin_time;
		long end_time;
		double score = 0.0;
		
		for(int k=0; k<5; k++){
			if(k!=0)
				budget = budget + 1000;
			//update binBudget
			binBudget = (int)(gama*budget);
			
			runTime = 0;
			score = 0.0;
			BufferedReader inCWSE = new BufferedReader(new FileReader(CWSEPath));//input CWSE
			String s;
			
			
			for(int i=0; i<expTimes; i++){
				s = inCWSE.readLine();
				if(s == null){
					System.out.println("error, s=null");
					break;
				}
				String [] sl = s.split("\t");
				startNodeID = Integer.valueOf(sl[0]);
				endNodeID = Integer.valueOf(sl[1]);
				
				outDe.println("Round: " + i + ", startNodeID " + startNodeID + ", endNodeID " + endNodeID);
				System.out.println("Round: " + i + ", startNodeID " + startNodeID + ", endNodeID " + endNodeID);
				
				begin_time = System.currentTimeMillis();
				
				//double curScore = runAppAlgDomSDIndexNS();
				//double curScore = runAppAlgDomSDIndexBiNS();
				//double curScore = runAppAlgDomLUSDIndexOP();
				double curScore = runAppAlgDomLUSDIndexBiOP();
				//double curScore = runAppAlgDomSLIndexConLab();
				
				end_time = System.currentTimeMillis();
				long runTimeTemp = (end_time - begin_time);
				score = score + curScore;
				runTime = runTime + runTimeTemp;
				
				//one round run successfully, save the start and target points, cate_weight
				outCW.println(startNodeID + "\t" + endNodeID);
				outCW.flush();
			}
			score = score / expTimes;
			runTime = runTime / expTimes;
			//output final results
			outOv.println("The run time is: " + runTime);
			outOv.println("score is: " + score);
			outOv.flush();
			inCWSE.close();
		}
		outDe.close();
		outOv.flush();
		outOv.close();
		outCW.close();
		//inEstOpt.close();
	}
	
	public static void main(String[] args) throws Exception {
		System.out.println("start!");
		
		MostInfPath mif = new MostInfPath();
		mif.runExperimentsVarB();
		
		System.out.println("end!");
	}
	

}
