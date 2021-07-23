package function;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Comparator;
import java.util.Queue;
import java.util.PriorityQueue;
import java.util.TreeSet;

public class MostDivPath {
	double Epsilon = 0.2;
	int maxNumLab = 10;
	int maxSubTrees = 5000000;
	int expTimes = 50;
	int startNodeID = 369;
	int endNodeID = 2;
	int budget = 21000;
	double gama = 0.4;
	int binBudget = (int)(gama*budget);
	int numNodes = 1890816;//1890816,264347
	
    String sourcePath = "D:\\CPS\\ca\\";
	String inGPath = sourcePath + "USA-road-d.CAL.gr";
	String inKeyPath = sourcePath + "CAKey.txt";
	String inSDIndexPath = sourcePath + "CA.index";
	PrintWriter outCW = new PrintWriter(new File(sourcePath + "21randomCWDomCSVB.txt"));//
	PrintWriter outDe = new PrintWriter(new File(sourcePath + "21outputDetailsDomCSVB.txt"));//
	PrintWriter outOv = new PrintWriter(new File(sourcePath + "21outputOverallDomCSVB.txt"));//
	String CWSEPath = sourcePath + "21randomCWDomRan2.txt";
    HashMap<Integer, ArrayList<String>> locID_keyword = new HashMap<Integer, ArrayList<String>>();//store the number of categories in one poi
    ShortestDistanceQuery sdq;
	
    public class Link {
    	public LinkedList<Node> link;
    	
    	public Link(){
    		this.link = new LinkedList<Node> ();
    	}
    }
    public class Node {
    	public int nodeID;
    	public int cost;
    	public Node(int nodeID, int cost){
    		this.nodeID = nodeID;
    		this.cost = cost;
    	}

    }
    Link[] graph; //original graph
	Link[] graphRe; //reverse graph
    
   //old label
  	class Label{
  		LinkedList<Integer> path;
  		double cost;
  		double eff;
  		TreeSet<String> keywordsSet;
  		double infoCov;
  		boolean isDom;
  		HashMap<Integer, Integer> locID_numVis;
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
  			
  			locID_numVis = new HashMap<Integer, Integer>();
  		}
  	}
  	class NodeLabelArray{
		ArrayList<Label> labelArr; 
		boolean isSorted;
		NodeLabelArray(){
			this.isSorted = false;
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
	}
	class NodeLazyLabelArray{
		ArrayList<LazyLabel> labelArr; 
	}
    
	public void init_basicBigG() throws Exception{
		graph = new Link[numNodes];
		graphRe = new Link[numNodes];
		//initialize graph
		for(int i=0; i<numNodes; i++){
			graph[i] = new Link();
			graphRe[i] = new Link();
			//node_degree[i] = 0;
		}
		
		BufferedReader inBigG = new BufferedReader(new FileReader(inGPath));//input of the graph
		//complete matrix with inputed data
		 String sG;
		//read and store the data
		while ((sG = inBigG.readLine()) != null) {
			int startNode;
			int endNode;
			int cost;

			String[] slG = sG.split(" ");
			if(slG.length == 4) {
				startNode = Integer.valueOf(slG[1]);
				endNode = Integer.valueOf(slG[2]);
				double costTemp = Double.valueOf(slG[3]);
				cost = (int)costTemp;
				Node node = new Node(endNode, cost);
				graph[startNode].link.add(node);
				Node nodeRe = new Node(startNode, cost);
				graphRe[endNode].link.add(nodeRe);
			}
		}
		
		inBigG.close();
	}
	
	public void init_loc_keywordBigGCA() throws Exception{
		//initialize poi_cate_coverage
		BufferedReader inKeyInfo = new BufferedReader(new FileReader(inKeyPath));//input of the information for computing IC
		String s;
		while((s = inKeyInfo.readLine()) != null){
			String[] sl = s.split("\t");
			if(sl.length == 2){
				int locID = Integer.valueOf(sl[0]);
				String keywords = sl[1];
				
				if(keywords != null){
					String[] keywordsArray = keywords.split(",");
					ArrayList<String> keywordsList = new ArrayList<String>();
					for(int i=0; i<keywordsArray.length; i++){
						keywordsList.add(keywordsArray[i]);
					}
					if(!locID_keyword.containsKey(locID)){
						locID_keyword.put(locID, keywordsList);
					}
				}
			}
		}
		inKeyInfo.close();
	}
	
	public void init_SDIndex() throws Exception{
  		sdq = new ShortestDistanceQuery();
  		sdq.ReadIndex(inSDIndexPath);
  	}
	
	public MostDivPath() throws Exception{
		init_basicBigG();
		init_loc_keywordBigGCA();
		init_SDIndex();
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
	double comShoDisIndex(int thisStaNodeID, int thisEndNodeID){
		double shoDis = Double.MAX_VALUE;
		shoDis = sdq.distanceQuery(thisStaNodeID, thisEndNodeID);
		
		return shoDis;
	}
	void insertKeywords(TreeSet<String> keywordsSet, Integer locID){
		if(locID_keyword.containsKey(locID)){
			ArrayList<String> keywordsList = locID_keyword.get(locID);
			int listSize = keywordsList.size();
			for(int i=0; i<listSize; i++){
				keywordsSet.add(keywordsList.get(i));
			}
		}
	}
	public void copyAToB(LinkedList<Integer> A, LinkedList<Integer> B){
		B.clear();
		for(int nodeID:A){
			B.add(nodeID);
		}
	}
	void copyAKeySetB(TreeSet<String> A, TreeSet<String> B){
		for(String key:A){
			B.add(key);
		}
		
	}

	public double comNumKeyCovJoin(TreeSet<String> keywordsSet1, TreeSet<String> keywordsSet2){
		double infCov = 0.0;
		for(String key:keywordsSet1){
			if(keywordsSet2.contains(key)){
				infCov++;
			}
		}
		return infCov;
	}
	public void copyTwoLazyPaths(LinkedList<LazyPathNode> A, LinkedList<LazyPathNode> B){
		B.clear();
		for(LazyPathNode node:A){
			B.add(node);
		}
	}
	boolean wheNewPathIsDomin(double cost, double infoCov, TreeSet<String> keywordsSet, NodeLabelArray nodeLableArray){
		boolean flag = false;
		ArrayList<Label> arrLab = nodeLableArray.labelArr;
		int numLab = arrLab.size();
		if(infoCov==0 && numLab>0)
			return true;
		
		for(int i=0; i<numLab; i++){
			Label aLabel = arrLab.get(i);
			//compute the f(p_1 \join p_2)
			double joinInf = comNumKeyCovJoin(keywordsSet, aLabel.keywordsSet);
			double diff = infoCov - joinInf;
			if(diff/aLabel.infoCov <= Epsilon){
				flag = true;
				break;
			}
		}
		
		return flag;
	}
	boolean wheNewPathIsDominEff(double cost, double infoCov, TreeSet<String> keywordsSet, NodeLabelArray nodeLableArray){
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
			double joinInf = comNumKeyCovJoin(keywordsSet, aLabel.keywordsSet);
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
	//check the dominant relation between the new label and existing labels, 0-no dom, 1-potential dom, 2-dom
	int wheNewPathIsPotDomPreIndex(LazyLabel newLabel, NodeLazyLabelArray nodeLableArray, LazyLabel labelMaxInf){
		int flag = 0;
		//double cost = newLabel.cost;
		double infoCov = newLabel.infoCov;
		TreeSet<String> keywordsSet = newLabel.keywordsSet;
		ArrayList<LazyLabel> arrLab = nodeLableArray.labelArr;
		int numLab = arrLab.size();
		if(infoCov==0 && numLab>0)//deal with infoCov=0
			return 2;
		//compare the new label with the label with maximum inf
		if(labelMaxInf != null){
			double joinInfMax = comNumKeyCovJoin(keywordsSet, labelMaxInf.keywordsSet);
			double diffMax = infoCov - joinInfMax;
			if(diffMax/labelMaxInf.infoCov <= Epsilon){
				return 2;
			}
		}
		for(int i=0; i<numLab; i++){
			LazyLabel aLabel = arrLab.get(i);
			//compute the f(p_1 \join p_2)
			double joinInf = comNumKeyCovJoin(keywordsSet, aLabel.keywordsSet);
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
	boolean wheNewPathIsDominPreCheTwoLabArrOP(LazyLabel newLabel, NodeLazyLabelArray nodeLableArray, NodeLazyLabelArray newNodeLableArray, LazyLabel maxEstInfLabel){
		double cost = newLabel.cost;
		double infoCov = newLabel.infoCov;
		TreeSet<String> keywordsSet = newLabel.keywordsSet;
		boolean flag = false;
		//check maxEstInfLabel
		if(maxEstInfLabel != null){
			double joinInfMax = comNumKeyCovJoin(keywordsSet, maxEstInfLabel.keywordsSet);
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
				double joinInf = comNumKeyCovJoin(keywordsSet, aLabel.keywordsSet);
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
				double joinInf = comNumKeyCovJoin(keywordsSet, aLabel.keywordsSet);
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
	boolean wheNewPathIsDomPreCurInf(double cost, double infoCov, TreeSet<String> keywordsSet, LazyLabel labelMaxInf){
		boolean flag = false;
		//compare the new label with the label with maximum inf
		if(labelMaxInf != null){
			if(infoCov!=0){
				double joinInfMax = comNumKeyCovJoin(keywordsSet, labelMaxInf.keywordsSet);
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
	//check the dominant relation between the new label and existing labels, 0-no dom, 1-potential dom, 2-dom
	boolean wheNewPathIsDomPre(double cost, double infoCov, TreeSet<String> keywordsSet, LazyLabel labelEstMaxInf, LazyLabel labelMaxInf){
		boolean flag = false;
		//compare the new label with the label with maximum inf
		if(labelMaxInf != null){
			double joinInfMax = comNumKeyCovJoin(keywordsSet, labelMaxInf.keywordsSet);
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
			double joinInfMax = comNumKeyCovJoin(keywordsSet, labelEstMaxInf.keywordsSet);
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
	public static void quickSortDecOS(double[] array, int left, int right, int [] index) {
	     if(left > right) {
	         return;
	     }
	     double base = array[left];
	     int baseIndex = index[left];
	     int i = left, j = right;
	     while(i != j) {
	         while(array[j] <= base && i < j) {
	             j--;
	        }
	 
	         while(array[i] >= base && i < j) {
	             i++;
	         }

	         if(i < j) {
	             double tmp = array[i];
	             array[i] = array[j];
	             array[j] = tmp;
	             int tmpInd = index[i];
	             index[i] = index[j];
	             index[j] = tmpInd;
	         }
	     }

	     array[left] = array[i];
	     array[i] = base;
	     index[left] = index[i];
	     index[i] = baseIndex;
	 
	     quickSortDecOS(array, left, i - 1, index);
	     quickSortDecOS(array, i + 1, right, index);
	}
	void SortLabArrDecOS(NodeLabelArray labelArrRe) {
		ArrayList<Label> arrLab = labelArrRe.labelArr;
		int numLab = arrLab.size();
		double [] osArr = new double [numLab];
		int [] index = new int [numLab];
		for(int i=0; i<numLab; i++) {
			osArr[i] = arrLab.get(i).infoCov;
			index[i] = i;
		}
		quickSortDecOS(osArr, 0, numLab-1, index);
		ArrayList<Label> newArrLab = new ArrayList<Label>();
		for(int i=0; i<numLab; i++) {
			newArrLab.add(arrLab.get(index[i]));
		}
		labelArrRe.labelArr = newArrLab;
		labelArrRe.isSorted = true;
		arrLab.clear();
	}
	double comMerPath(TreeSet<String> keySet1, TreeSet<String> keySet2) {
		double inf = 0.0;
		for(String key:keySet2) {
			if(!keySet1.contains(key))
				inf = inf + 1.0;
		}
		inf = inf + keySet1.size();
		
		return inf;
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
	}
	void mergeTwoKeySet(TreeSet<String> newKeySet, TreeSet<String> keySet1, TreeSet<String> keySet2) {
		for(String key:keySet1) {
			newKeySet.add(key);
		}
		for(String key:keySet2) {
			newKeySet.add(key);
		}
	}
	Label MergeLabWithParLabSortUpdBestLab(Label aLabel, NodeLabelArray labelArrRe, Label curBestLable) {
		if(labelArrRe == null)
			return null;
		ArrayList<Label> arrLab = labelArrRe.labelArr;
		int numLab= arrLab.size();
		if(numLab == 0)
			return null;
		
		//check whether the arrLab is sorted, if not, sort it firstly
		if(labelArrRe.isSorted == false) {
			SortLabArrDecOS(labelArrRe);
		}
		arrLab = labelArrRe.labelArr;
		
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
			if(curLab.path.getLast() == curNodeID) {
				curLab.path.removeLast();
			}
			//compute the influence of the merged label
			double curInf = comMerPath(aLabel.keywordsSet, curLab.keywordsSet);
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
		TreeSet<String> newKeywordsSet = new TreeSet<String>();
		mergeTwoKeySet(newKeywordsSet, aLabel.keywordsSet, bestParLab.keywordsSet);
		Label newLabel = new Label(newPath, aLabel.cost+bestParLab.cost, newKeywordsSet.size(), newKeywordsSet);
		//System.out.println("newKeywordsSet.size() is: " + newKeywordsSet.size());
		//update curBestLable
		curBestLable = newLabel;
		
		return curBestLable;
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
	//check the dominant relation between the new label and existing labels, 0-no dom, 1-potential dom, 2-dom
	boolean wheNewPathIsDomPre(double cost, double infoCov, TreeSet<String> keywordsSet, Label labelMaxInf){
		boolean flag = false;
		//compare the new label with the label with maximum inf
		if(labelMaxInf != null){
			double joinInfMax = comNumKeyCovJoin(keywordsSet, labelMaxInf.keywordsSet);
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
		//System.out.println("test staNodeID is: " + newPath.getFirst().nodeID);
		//System.out.println("test EndNodeID is: " + newPath.getLast().nodeID);
	}
	void MergeLabWithParLab(LazyLabel aLabel, NodeLabelArray labelArrRe, NodeLazyLabelArray labelArrEndNode) {
		if(labelArrRe == null)
			return;
		ArrayList<Label> arrLab = labelArrRe.labelArr;
		int numLab= arrLab.size();
		if(numLab == 0)
			return;
		double bestInf = -1.0;
		int bestLabIndex = -1;
		//scan all labels in labelArrRe for finding the best merged label
		//System.out.println("numLab is: " + numLab);
		for(int i=0; i<numLab; i++) {
			Label curLab = arrLab.get(i);
			if(curLab.cost + aLabel.cost > budget)
				continue;                    
			//estimate the influence of the merged label
			double estInf = aLabel.infoCov + curLab.infoCov;
			if(estInf <= bestInf)
				break;
			int curNodeID = aLabel.lazyPath.getLast().nodeID;
			//if curLab.path has curNodeID, prune curNodeID from it
			if(curLab.path.getLast() == curNodeID) {
				curLab.path.removeLast();
			}
			//compute the influence of the merged label
			double curInf = comMerPath(aLabel.keywordsSet, curLab.keywordsSet);
			if(curInf > bestInf) {//update the best label
				bestInf = curInf;
				bestLabIndex = i;	
			}
		}
		//System.out.println("numNonSimPath is: " + numNonSimPath);
		//merge two paths for getting the best merged path and then update labelArrEndNode
		if(bestLabIndex == -1)
			return;
		Label bestParLab = arrLab.get(bestLabIndex);
		//generate a label for current path
		LinkedList<LazyPathNode> newPath = new LinkedList<LazyPathNode> ();
		copyTwoLazyPaths(aLabel.lazyPath, newPath);
		mergeTwoPaths(newPath, bestParLab.path);
		TreeSet<String> newKeywordsSet = new TreeSet<String>();
		mergeTwoKeySet(newKeywordsSet, aLabel.keywordsSet, bestParLab.keywordsSet);
		LazyLabel newLabel = new LazyLabel(newPath, aLabel.cost+bestParLab.cost, newKeywordsSet.size(), newKeywordsSet, Double.MAX_VALUE, Double.MAX_VALUE);
		labelArrEndNode.labelArr.add(newLabel);
		
		return;
	}
	LazyLabel MergeLabWithParLabUpdBestLab(LazyLabel aLabel, NodeLabelArray labelArrRe, LazyLabel curBestLable) {
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
			if(curLab.path.getLast() == curNodeID) {
				curLab.path.removeLast();
			}
			//compute the influence of the merged label
			double curInf = comMerPath(aLabel.keywordsSet, curLab.keywordsSet);
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
		TreeSet<String> newKeywordsSet = new TreeSet<String>();
		mergeTwoKeySet(newKeywordsSet, aLabel.keywordsSet, bestParLab.keywordsSet);
		LazyLabel newLabel = new LazyLabel(newPath, aLabel.cost+bestParLab.cost, newKeywordsSet.size(), newKeywordsSet, Double.MAX_VALUE, Double.MAX_VALUE);
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
		TreeSet<String> keywordsSet = new TreeSet<String>();
		insertKeywords(keywordsSet, startNodeID);
		double curInfoCov = keywordsSet.size();
		LinkedList<Integer> newPath = new LinkedList<Integer> ();
		newPath.add(startNodeID);
		Label aLabel = new Label(newPath, curCost, curInfoCov, keywordsSet);
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
				outDe.println("Run_time > 600000!!!");
				outDe.flush();
				
				return 0;
			}
			aLabel = PQLabel.poll();
			LinkedList<Integer> prePath = aLabel.path;
			int curEndNode = prePath.getLast();
			if(curEndNode == endNodeID){
				if(aLabel.infoCov > curBestLable.infoCov || curBestLable.infoCov==0){//update the best path
					curBestLable = aLabel;
				}
			}
			else{
				//check whether the new path is dominated by existing paths ending with curEndNode
				boolean newPathIsDom = wheNewPathIsDomin(aLabel.cost, aLabel.infoCov, aLabel.keywordsSet, nodeLabArr[curEndNode]);
				if(newPathIsDom == true){//prune the label
					continue;
				}
				else{//store the label into nodeLabArr[curEndNode] and extend the aLabel.path
					//store the label
					nodeLabArr[curEndNode].labelArr.add(aLabel);
					//extend the aLabel.path
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
								TreeSet<String> newKeywordsSet = new TreeSet<String>();
								copyAKeySetB(aLabel.keywordsSet, newKeywordsSet);
								insertKeywords(newKeywordsSet, node.nodeID);
								curInfoCov = newKeywordsSet.size();
								//generate a label for current path
								Label newLabel = new Label(newPath, curCost, curInfoCov, newKeywordsSet);
								PQLabel.add(newLabel);
							}
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
		TreeSet<String> keywordsSet = new TreeSet<String>();
		insertKeywords(keywordsSet, startNodeID);
		double curInfoCov = keywordsSet.size();
		double estInfCov = curInfoCov;//comEstInfCovSubInd(osTabArr[startNodeID], startNodeID, budget, 0.0, null, endNode_subIndex, staNode_subIndex);
		//System.out.println("estInfCov is: " + estInfCov);
		LinkedList<LazyPathNode> newPath = new LinkedList<LazyPathNode> ();
		LazyPathNode aPathNode = new LazyPathNode(startNodeID, 0);
		newPath.add(aPathNode);
		LazyLabel aLabel = new LazyLabel(newPath, curCost, curInfoCov, keywordsSet, estInfCov, Double.MAX_VALUE);;
		PQLabel.add(aLabel);
		aPathNode.souLabel = aLabel;
		//initialize an array to store the path ending with nodes
		NodeLazyLabelArray [] nodeLabzyLabArr = new NodeLazyLabelArray [numNodes];
		for(int i=0; i<numNodes; i++){
			nodeLabzyLabArr[i] = new NodeLazyLabelArray();
			nodeLabzyLabArr[i].labelArr = new ArrayList<LazyLabel>();
		}
		LazyLabel curBestLable = null;
		long begin_time2 = System.currentTimeMillis();
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
			int newPathIsPotDom = wheNewPathIsPotDomPreIndex(aLabel, nodeLabzyLabArr[curEndNode], maxInfLabel);
			if(curEndNode == endNodeID) {
				insSorLabelDesInf(nodeLabzyLabArr[curEndNode].labelArr, aLabel);
				if(curBestLable == null || curBestLable.infoCov < aLabel.infoCov)
					curBestLable = aLabel;
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
						/*if(endNodeID == 2838){
							System.out.println(node.nodeID + " -> " + endNodeID + " curCost is: " + curCost);
						}*/
						if(curCost <= budget){
							TreeSet<String> newKeywordsSet = new TreeSet<String>();
							copyAKeySetB(aLabel.keywordsSet, newKeywordsSet);
							insertKeywords(newKeywordsSet, node.nodeID);
							curInfoCov = newKeywordsSet.size();
							if(node.nodeID != endNodeID){
								maxInfLabel = null;
								if(nodeLabzyLabArr[node.nodeID].labelArr.size() > 0)
									maxInfLabel = nodeLabzyLabArr[node.nodeID].labelArr.get(0);
								boolean preDomFlag = wheNewPathIsDomPreCurInf(curCost, curInfoCov, newKeywordsSet, maxInfLabel);
								if(preDomFlag == true)
									continue;
							}
							long sta_time2 = System.currentTimeMillis();
							estInfCov = curInfoCov/curCost*budget;
							double uppInfCov = Double.MAX_VALUE;
							newPath = new LinkedList<LazyPathNode> ();
							copyTwoLazyPaths(prePath, newPath);
							aPathNode = new LazyPathNode(node.nodeID, 0);
							newPath.add(aPathNode);//get current path.link

							LazyLabel newLabel = new LazyLabel(newPath, curCost, curInfoCov, newKeywordsSet, estInfCov, uppInfCov);
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
		//end_time2 = System.currentTimeMillis();
		//System.out.println("Update the estimated infoCov for all labels! The runTime is: " + (end_time2 - begin_time2));
		//begin_time2 = System.currentTimeMillis();
		//System.out.println("Update the estimated infoCov for all labels!");
		System.out.println("PotPQLabel.size() is: " + PotPQLabel.size());
		//check the labels in PotPQLabel and extend the non-dom labels
		NodeLazyLabelArray [] nodeLabzyLabArrRes = new NodeLazyLabelArray [numNodes];
		for(int i=0; i<numNodes; i++){
			nodeLabzyLabArrRes[i] = new NodeLazyLabelArray();
			nodeLabzyLabArrRes[i].labelArr = new ArrayList<LazyLabel>();
		}
		
		while(PotPQLabel.size()>0){
			long cur_time = System.currentTimeMillis();
			long run_time = cur_time - begin_time;
			if(PotPQLabel.size() > maxSubTrees){
				System.out.println("PotPQLabel.size() > maxSubTrees!!!");
				return -1.0;
			}
			if(run_time > 100000){
				System.out.println("Run_time>100000!!!");
				outDe.println("Run_time>100000!!!");
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
			boolean newPathIsDom = wheNewPathIsDominPreCheTwoLabArrOP(aLabel, nodeLabzyLabArr[curEndNode], nodeLabzyLabArrRes[curEndNode], maxEstInfLab);
			if(newPathIsDom == true){//prune the label
				continue;
			}
			else{//store the label into nodeLabArr[curEndNode] and extend the aLabel.path
				//insert the label into nodeLabArr and update maxInfLabel[curEndNode]
				//nodeLabzyLabArrRes[curEndNode].labelArr.add(aLabel);
				insSorLabelDesInf(nodeLabzyLabArrRes[curEndNode].labelArr, aLabel);
				//update maxInfLabel
				/*if(maxInfLabel[curEndNode]!=null){
					if(maxInfLabel[curEndNode].infoCov < aLabel.infoCov)
						maxInfLabel[curEndNode] = aLabel;
				}
				else
					maxInfLabel[curEndNode] = aLabel;
					*/
				//extend the aLabel.path
				for(Node node : graph[curEndNode].link){
					if(lowToEndShoDis[node.nodeID] == -1){
						long begin_timeTemp2 = System.currentTimeMillis();
						lowToEndShoDis[node.nodeID] = comShoDisIndex(node.nodeID, endNodeID);
						ostime = ostime + (System.currentTimeMillis()- begin_timeTemp2);
					}
					//if(!WheLazyPathConNode(prePath, node.nodeID) && node.cost!=Integer.MAX_VALUE &&  lowToEndShoDis[node.nodeID]!=Integer.MAX_VALUE && (aLabel.cost + node.cost + lowToEndShoDis[node.nodeID]) <= budget){//just consider the nonexistent nodes
					if(node.cost!=Integer.MAX_VALUE &&  lowToEndShoDis[node.nodeID]!=Integer.MAX_VALUE && (aLabel.cost + node.cost + lowToEndShoDis[node.nodeID]) <= budget){//just consider the nonexistent nodes
						curCost = aLabel.cost + node.cost;
						if(curCost <= budget){
							TreeSet<String> newKeywordsSet = new TreeSet<String>();
							copyAKeySetB(aLabel.keywordsSet, newKeywordsSet);
							insertKeywords(newKeywordsSet, node.nodeID);
							curInfoCov = newKeywordsSet.size();
							if(node.nodeID == endNodeID && (curBestLable.infoCov < aLabel.infoCov)) {
								estInfCov = Double.MAX_VALUE;
								double uppInfCov = Double.MAX_VALUE;
								//generate a label for current path
								newPath = new LinkedList<LazyPathNode> ();
								copyTwoLazyPaths(prePath, newPath);
								aPathNode = new LazyPathNode(node.nodeID, 0);
								newPath.add(aPathNode);//get current path.link
								LazyLabel newLabel = new LazyLabel(newPath, curCost, curInfoCov, newKeywordsSet, estInfCov, uppInfCov);
								curBestLable = newLabel;
							}
							
							LazyLabel theMaxInfLabel = null;
							if(nodeLabzyLabArrRes[node.nodeID].labelArr.size()>0)
								theMaxInfLabel = nodeLabzyLabArrRes[node.nodeID].labelArr.get(0);
							//update maxEstInfLabel[curEndNode] and maxEstInfLabVisIndex[i]
							maxEstInfLab = getMaxEstInfLab(aLabel.cost, node.nodeID, nodeLabzyLabArr[node.nodeID].labelArr);
							boolean preDomFlag = wheNewPathIsDomPre(curCost, curInfoCov, newKeywordsSet, maxEstInfLab, theMaxInfLabel);
							if(preDomFlag == true)
								continue;
							estInfCov = Double.MAX_VALUE;
									//comEstInfCov(osTabArr[node.nodeID], node.nodeID, budget-curCost, curInfoCov);
							double uppInfCov = Double.MAX_VALUE;
							//if(osTabArr[node.nodeID].indNodeList.size() != 0 )
								//uppInfCov = estInfCov + curInfoCov;
							//if(uppInfCov <= maxOSfindedSou)
								//continue;
							//generate a label for current path
							newPath = new LinkedList<LazyPathNode> ();
							copyTwoLazyPaths(prePath, newPath);
							aPathNode = new LazyPathNode(node.nodeID, 0);
							newPath.add(aPathNode);//get current path.link
							LazyLabel newLabel = new LazyLabel(newPath, curCost, curInfoCov, newKeywordsSet, estInfCov, uppInfCov);
							PotPQLabel.add(newLabel);
						}
					}
				}
			}
		}
		//end_time2 = System.currentTimeMillis();
		//System.out.println("Check the labels in PotPQLabel and extend the non-dom labels! The runTime is: " + (end_time2 - begin_time2));
		//System.out.println("Check the labels in PotPQLabel and extend the non-dom labels!");
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
		TreeSet<String> keywordsSet = new TreeSet<String>();
		insertKeywords(keywordsSet, endNodeID);
		double curInfoCov = keywordsSet.size();
		LinkedList<Integer> newPathRe = new LinkedList<Integer> ();
		newPathRe.add(endNodeID);
		Label aLabelRe = new Label(newPathRe, 0, curInfoCov, keywordsSet);
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
			boolean newPathIsDom = wheNewPathIsDomin(aLabelRe.cost, aLabelRe.infoCov, aLabelRe.keywordsSet, nodeLabArrRe[curEndNode]);
			if(newPathIsDom == true){//prune the label
				continue;
			}
			else{//store the label into nodeLabArr[curEndNode] and extend the aLabel.path
				//store the label
				nodeLabArrRe[curEndNode].labelArr.add(aLabelRe);
				//insSorLabelDesInfCos(nodeLabArrRe[curEndNode].labelArr, aLabelRe);
				//testNodeID = curEndNode;
				//System.out.println("nodeLabArrRe[curEndNode].labelArr.size() is: " + nodeLabArrRe[curEndNode].labelArr.size());
				//extend the aLabel.path
				for(Node node : graphRe[curEndNode].link){
					double disToStaNode = comShoDisIndex(startNodeID, node.nodeID);
					if(node.cost!=Integer.MAX_VALUE && disToStaNode!= Double.MAX_VALUE && (aLabelRe.cost + node.cost + disToStaNode <= budget)){//just consider the nonexistent nodes
						curCost = aLabelRe.cost + node.cost;
						if(curCost <= binBudget && node.nodeID != startNodeID){
							TreeSet<String> newKeywordsSet = new TreeSet<String>();
							copyAKeySetB(aLabelRe.keywordsSet, newKeywordsSet);
							insertKeywords(newKeywordsSet, node.nodeID);
							curInfoCov = newKeywordsSet.size();
							/*if(nodeLabArrRe[node.nodeID].labelArr.size() > 0) {
								if(wheNewPathIsDomPre(curCost, curInfoCov, newKeywordsSet, nodeLabArrRe[node.nodeID].labelArr.get(0)) == true)
									continue;
							}*/
							newPathRe = new LinkedList<Integer> ();
							copyAToB(prePath, newPathRe);
							newPathRe.add(node.nodeID);//get current path.link
							//generate a label for current path
							Label newLabel = new Label(newPathRe, curCost, curInfoCov, newKeywordsSet);
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
		keywordsSet = new TreeSet<String>();
		insertKeywords(keywordsSet, startNodeID);
		curInfoCov = keywordsSet.size();
		LinkedList<Integer> newPath = new LinkedList<Integer> ();
		newPath.add(startNodeID);
		Label aLabel = new Label(newPath, curCost, curInfoCov, keywordsSet);
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
			boolean newPathIsDom = wheNewPathIsDomin(aLabel.cost, aLabel.infoCov, aLabel.keywordsSet, nodeLabArr[curEndNode]);
			if(newPathIsDom == true){//prune the label
				continue;
			}
			else{//store the label into nodeLabArr[curEndNode] and extend the aLabel.path
				//store the label
				nodeLabArr[curEndNode].labelArr.add(aLabel);
				//extend the aLabel.path
				
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
							TreeSet<String> newKeywordsSet = new TreeSet<String>();
							copyAKeySetB(aLabel.keywordsSet, newKeywordsSet);
							insertKeywords(newKeywordsSet, node.nodeID);
							curInfoCov = newKeywordsSet.size();
							//generate a label for current path
							Label newLabel = new Label(newPath, curCost, curInfoCov, newKeywordsSet);
							if(node.nodeID != endNodeID) {
								if(aLabel.cost >= budget - binBudget) {
									Label newBestLab = MergeLabWithParLabSortUpdBestLab(newLabel, nodeLabArrRe[node.nodeID], curBestLable);
									if(newBestLab != null) {
										curBestLable = newBestLab;
									}
									//numNonMatch++;
								}
								else
									PQLabel.add(newLabel);
							}
							else {
								if(newLabel.infoCov > curBestLable.infoCov || curBestLable.infoCov==0){//update the best path
									curBestLable = newLabel;
								}
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
		TreeSet<String> keywordsSet = new TreeSet<String>();
		insertKeywords(keywordsSet, startNodeID);
		double curInfoCov = keywordsSet.size();
		LinkedList<Integer> newPath = new LinkedList<Integer> ();
		newPath.add(startNodeID);
		Label aLabel = new Label(newPath, curCost, curInfoCov, keywordsSet);
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
			else{
				//check whether the new path is dominated by existing paths ending with curEndNode
				boolean newPathIsDom = false;
				if(nodeLabArr[curEndNode].labelArr.size() < maxNumLab)
					newPathIsDom = wheNewPathIsDominEff(aLabel.cost, aLabel.infoCov, aLabel.keywordsSet, nodeLabArr[curEndNode]);
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
								newPath = new LinkedList<Integer> ();
								copyAToB(prePath, newPath);
								newPath.add(node.nodeID);//get current path.link
								TreeSet<String> newKeywordsSet = new TreeSet<String>();
								copyAKeySetB(aLabel.keywordsSet, newKeywordsSet);
								insertKeywords(newKeywordsSet, node.nodeID);
								curInfoCov = newKeywordsSet.size();
								//generate a label for current path
								Label newLabel = new Label(newPath, curCost, curInfoCov, newKeywordsSet);
								PQLabel.add(newLabel);
							}
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
		TreeSet<String> keywordsSet = new TreeSet<String>();
		insertKeywords(keywordsSet, endNodeID);
		double curInfoCov = keywordsSet.size();
		LinkedList<Integer> newPathRe = new LinkedList<Integer> ();
		newPathRe.add(endNodeID);
		Label aLabelRe = new Label(newPathRe, 0, curInfoCov, keywordsSet);
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
			boolean newPathIsDom = wheNewPathIsDomin(aLabelRe.cost, aLabelRe.infoCov, aLabelRe.keywordsSet, nodeLabArrRe[curEndNode]);
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
						if(curCost <= binBudget && node.nodeID != startNodeID){
							TreeSet<String> newKeywordsSet = new TreeSet<String>();
							copyAKeySetB(aLabelRe.keywordsSet, newKeywordsSet);
							insertKeywords(newKeywordsSet, node.nodeID);
							curInfoCov = newKeywordsSet.size();
							if(nodeLabArrRe[node.nodeID].labelArr.size() > 0) {
								if(wheNewPathIsDomPre(curCost, curInfoCov, newKeywordsSet, nodeLabArrRe[node.nodeID].labelArr.get(0)) == true)
									continue;
							}
							newPathRe = new LinkedList<Integer> ();
							copyAToB(prePath, newPathRe);
							newPathRe.add(node.nodeID);//get current path.link
							//generate a label for current path
							Label newLabel = new Label(newPathRe, curCost, curInfoCov, newKeywordsSet);
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
		curCost = 0;
		keywordsSet = new TreeSet<String>();
		insertKeywords(keywordsSet, startNodeID);
		curInfoCov = keywordsSet.size();
		double estInfCov = curInfoCov;//comEstInfCovSubInd(osTabArr[startNodeID], startNodeID, budget, 0.0, null, endNode_subIndex, staNode_subIndex);
		//System.out.println("estInfCov is: " + estInfCov);
		LinkedList<LazyPathNode> newPath = new LinkedList<LazyPathNode> ();
		LazyPathNode aPathNode = new LazyPathNode(startNodeID, 0);
		newPath.add(aPathNode);
		LazyLabel aLabel = new LazyLabel(newPath, curCost, curInfoCov, keywordsSet, estInfCov, Double.MAX_VALUE);;
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
			int newPathIsPotDom = wheNewPathIsPotDomPreIndex(aLabel, nodeLabzyLabArr[curEndNode], maxInfLabel);
			//System.out.println(curEndNode + " -> " + endNodeID + " newPathIsPotDom is: " + newPathIsPotDom);
			if(newPathIsPotDom == 2)//dom, prune this label
				continue;
			if(newPathIsPotDom == 1)//potential dom, insert this label into PotPQLabel
				PotPQLabel.add(aLabel);
			else{
				//insert the label into nodeLabzyLabArr[curEndNode].labelArr in descending order of OS value
				insSorLabelDesInf(nodeLabzyLabArr[curEndNode].labelArr, aLabel);
				
				//extend the aLabel.path
				for(Node node : graph[curEndNode].link){
					if(lowToEndShoDis[node.nodeID] == -1){
						long begin_timeTemp2 = System.currentTimeMillis();
						lowToEndShoDis[node.nodeID] = comShoDisIndex(node.nodeID, endNodeID);
						ostime = ostime + (System.currentTimeMillis()- begin_timeTemp2);
						//System.out.println(node.nodeID + " -> " + endNodeID + " is: " + lowToEndShoDis[node.nodeID]);
					}
					/*if(endNodeID == 2838){
						System.out.println(node.nodeID + " -> " + endNodeID + " is: " + lowToEndShoDis[node.nodeID]);
					}*/
					if(node.cost!=Integer.MAX_VALUE && lowToEndShoDis[node.nodeID]!=Double.MAX_VALUE && (aLabel.cost + node.cost + lowToEndShoDis[node.nodeID]) <= budget){//just consider the nonexistent nodes
						curCost = aLabel.cost + node.cost;
						/*if(endNodeID == 2838){
							System.out.println(node.nodeID + " -> " + endNodeID + " curCost is: " + curCost);
						}*/
						if(curCost <= budget){
							TreeSet<String> newKeywordsSet = new TreeSet<String>();
							copyAKeySetB(aLabel.keywordsSet, newKeywordsSet);
							insertKeywords(newKeywordsSet, node.nodeID);
							curInfoCov = newKeywordsSet.size();
							if(node.nodeID != endNodeID){
								maxInfLabel = null;
								if(nodeLabzyLabArr[node.nodeID].labelArr.size() > 0)
									maxInfLabel = nodeLabzyLabArr[node.nodeID].labelArr.get(0);
								boolean preDomFlag = wheNewPathIsDomPreCurInf(curCost, curInfoCov, newKeywordsSet, maxInfLabel);
								if(preDomFlag == true)
									continue;
								long sta_time2 = System.currentTimeMillis();
								ArrayList<Label> nodeLabArrCur = nodeLabArrRe[node.nodeID].labelArr;
								if(nodeLabArrCur.size() == 0)
									estInfCov = curInfoCov/curCost*budget;
								else
									estInfCov = (curInfoCov + nodeLabArrCur.get(0).infoCov) / (double)(curCost + nodeLabArrCur.get(0).cost) * budget;
								//estInfCov = comEstInfCovSubInd(osTabArr[node.nodeID], node.nodeID, budget-curCost, aLabel.infoCov, aLabel, endNode_subIndex, staNode_subIndex);
								//outDe.println("estInfCov is: " + estInfCov);
								//outDe.flush();
								//if(budget-curCost<30)
									//System.out.println("node.nodeID is: " + node.nodeID);
								//outDe.println("estInfCov is: " + estInfCov + " estInfCov2 is: " + estInfCov2 + " budget-curCost is: " + (budget-curCost) + " curCost: " + curCost);
								//outDe.flush();
								//ostime = ostime + (System.currentTimeMillis()-sta_time2);
								//System.out.println(node.nodeID + " estInfCov is: " + estInfCov);
								double uppInfCov = Double.MAX_VALUE;
								//if(osTabArr[node.nodeID].indNodeList.size() != 0 )
									//uppInfCov = estInfCov + curInfoCov;
								//if(uppInfCov <= maxOSfindedSou)
									//continue;
								//generate a label for current path
								newPath = new LinkedList<LazyPathNode> ();
								copyTwoLazyPaths(prePath, newPath);
								aPathNode = new LazyPathNode(node.nodeID, 0);
								newPath.add(aPathNode);//get current path.link
	
								LazyLabel newLabel = new LazyLabel(newPath, curCost, curInfoCov, newKeywordsSet, estInfCov, uppInfCov);
								aPathNode.souLabel = newLabel;
								if(newLabel.cost >= budget - binBudget) {
									MergeLabWithParLab(newLabel, nodeLabArrRe[node.nodeID], nodeLabzyLabArr[endNodeID]);
								}
								else
									PQLabel.add(newLabel);
								
							}
							else{
								double uppInfCov = Double.MAX_VALUE;
								//generate a label for current path
								newPath = new LinkedList<LazyPathNode> ();
								copyTwoLazyPaths(prePath, newPath);
								aPathNode = new LazyPathNode(node.nodeID, 0);
								newPath.add(aPathNode);//get current path.link
								LazyLabel newLabel = new LazyLabel(newPath, curCost, curInfoCov, newKeywordsSet, estInfCov, uppInfCov);
								nodeLabzyLabArr[node.nodeID].labelArr.add(newLabel);
								aPathNode.souLabel = newLabel;
							}
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
		//end_time2 = System.currentTimeMillis();
		//System.out.println("Update the estimated infoCov for all labels! The runTime is: " + (end_time2 - begin_time2));
		//begin_time2 = System.currentTimeMillis();
		//System.out.println("Update the estimated infoCov for all labels!");
		System.out.println("PotPQLabel.size() is: " + PotPQLabel.size());
		//check the labels in PotPQLabel and extend the non-dom labels
		NodeLazyLabelArray [] nodeLabzyLabArrRes = new NodeLazyLabelArray [numNodes];
		for(int i=0; i<numNodes; i++){
			nodeLabzyLabArrRes[i] = new NodeLazyLabelArray();
			nodeLabzyLabArrRes[i].labelArr = new ArrayList<LazyLabel>();
		}
		endNodelabelArr = nodeLabzyLabArr[endNodeID].labelArr;
		numLabel = endNodelabelArr.size();
		LazyLabel curBestLable = null;
		for(int i=0; i<numLabel; i++){
			aLabel = endNodelabelArr.get(i);
			if(curBestLable != null){
				if(curBestLable.infoCov < aLabel.infoCov)
					curBestLable = aLabel;
			}
			else
				curBestLable = aLabel;
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
			boolean newPathIsDom = wheNewPathIsDominPreCheTwoLabArrOP(aLabel, nodeLabzyLabArr[curEndNode], nodeLabzyLabArrRes[curEndNode], maxEstInfLab);
			if(newPathIsDom == true){//prune the label
				continue;
			}
			else{
				//store the label into nodeLabArr[curEndNode] and extend the aLabel.path
				//insert the label into nodeLabArr and update maxInfLabel[curEndNode]
				//nodeLabzyLabArrRes[curEndNode].labelArr.add(aLabel);
				insSorLabelDesInf(nodeLabzyLabArrRes[curEndNode].labelArr, aLabel);
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
							TreeSet<String> newKeywordsSet = new TreeSet<String>();
							copyAKeySetB(aLabel.keywordsSet, newKeywordsSet);
							insertKeywords(newKeywordsSet, node.nodeID);
							curInfoCov = newKeywordsSet.size();
							if(node.nodeID != endNodeID){
								LazyLabel theMaxInfLabel = null;
								if(nodeLabzyLabArrRes[node.nodeID].labelArr.size()>0)
									theMaxInfLabel = nodeLabzyLabArrRes[node.nodeID].labelArr.get(0);
								maxEstInfLab = getMaxEstInfLab(curCost, node.nodeID, nodeLabzyLabArr[node.nodeID].labelArr);
								boolean preDomFlag = wheNewPathIsDomPre(curCost, curInfoCov, newKeywordsSet, maxEstInfLab, theMaxInfLabel);
								if(preDomFlag == true)
									continue;
								estInfCov = Double.MAX_VALUE;
								double uppInfCov = Double.MAX_VALUE;
								//generate a label for current path
								newPath = new LinkedList<LazyPathNode> ();
								copyTwoLazyPaths(prePath, newPath);
								aPathNode = new LazyPathNode(node.nodeID, 0);
								newPath.add(aPathNode);//get current path.link
								LazyLabel newLabel = new LazyLabel(newPath, curCost, curInfoCov, newKeywordsSet, estInfCov, uppInfCov);
								if(newLabel.cost >= budget - binBudget) {
									LazyLabel newBestLab = MergeLabWithParLabUpdBestLab(newLabel, nodeLabArrRe[node.nodeID], curBestLable);
									if(newBestLab != null) {
										curBestLable = newBestLab;
									}
								}
								else
									PotPQLabel.add(newLabel);
							}
							else{
								boolean updateBestLab = false;
								if(curBestLable != null){
									if(curInfoCov > curBestLable.infoCov){
										updateBestLab = true;
									}
								}
								else{
									updateBestLab = true;
								}
								if(updateBestLab == true){
									estInfCov = Double.MAX_VALUE;
									double uppInfCov = Double.MAX_VALUE;
									//generate a label for current path
									newPath = new LinkedList<LazyPathNode> ();
									copyTwoLazyPaths(prePath, newPath);
									aPathNode = new LazyPathNode(node.nodeID, 0);
									newPath.add(aPathNode);//get current path.link
									LazyLabel newLabel = new LazyLabel(newPath, curCost, curInfoCov, newKeywordsSet, estInfCov, uppInfCov);
									curBestLable = newLabel;
								}
							}
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
				
				double curScore = runAppAlgDomSDIndexNS();
				//double curScore = runAppAlgDomSDIndexBiNS();
				//double curScore = runAppAlgDomLUSDIndexOP();
				//double curScore = runAppAlgDomLUSDIndexBiOP();
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
		MostDivPath mdp = new MostDivPath();
		mdp.runExperimentsVarB();
		System.out.println("end!");
	}
}
