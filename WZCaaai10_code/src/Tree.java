import java.util.*;
import java.util.concurrent.*;

public class Tree {
	public static int MAX_LEAFS = 3;
	
	int startTree;
	Leaf[][] subTrees;

	public Tree(int horizon, int numObs, int numAct) {
		startTree = 0;
		subTrees = new Leaf[horizon][MAX_LEAFS];
		for (int h = 0; h < horizon; ++h) {
			for (int i = 0; i < MAX_LEAFS; ++i) {
				subTrees[h][i] = new Leaf(numObs, i, h);
				subTrees[h][i].genRandLeaf(numAct);
			}
		}
	}

	public final Leaf getLeaf(int h, int n) {
		Leaf leaf = subTrees[h][n];
		return leaf;
	}
	
	public final int getAct(int h, int n) {
		int a = subTrees[h][n].rootAct;
		return a;
	}
	
	public final int getNextLeaf(int h, int n, int o) {
		int n_ = Model.randInt(subTrees[h][n].subProb[o]);
		return n_;
	}
	
	final void reset() {
		for (int i = 0; i < subTrees.length; ++i) {
			for (int j = 0; j < subTrees[i].length; ++j) {
				subTrees[i][j].reset();
			}
		}
	}
}

class Leaf {
	int id;
	int height;
	
	int rootAct;
	double[][] subProb;
	Map<Integer, VTab<Double>> valTab;
	
	Leaf(int numObs, int i, int h) {
		id = i;
		height = h;
		rootAct = 0;
		valTab = new ConcurrentHashMap<Integer, VTab<Double>>();
		subProb = new double[numObs][Tree.MAX_LEAFS];
	}
	
	final void reset() {
		valTab.clear();
		genRandLeaf();
	}
	
	final void setValue(int n, int s, double v) {
		int key = n + s * Tree.MAX_LEAFS;
		if (valTab.containsKey(key)) {
			VTab<Double> tab = valTab.get(key);
			tab.value = (tab.value*tab.count + v) / (tab.count + 1);
			tab.count++;
		} else {
			VTab<Double> tab = new VTab<Double>();
			tab.value = v;
			tab.count = 1;
			valTab.put(key, tab);
		}
	}
	
	final double getValue(int n, int s) {
		int key = n + s * Tree.MAX_LEAFS;
		if (valTab.containsKey(key)) {
			return valTab.get(key).value;
		} else {
			return 0.0;
		}
	}
	
	final int getCount(int n, int s) {
		int key = n + s * Tree.MAX_LEAFS;
		if (valTab.containsKey(key)) {
			return valTab.get(key).count;
		} else {
			return 0;
		}
	}
	
	void genRandLeaf(int numAct) {
		genRandLeaf();
		rootAct = Model.rand.nextInt(numAct);
	}
	
	void genRandLeaf() {
		Random rand = Model.rand;
		for (int o = 0; o < subProb.length; ++o) {
			double norm = 0.0;
			for (int i = 0; i < subProb[o].length; ++i) {
				subProb[o][i] = rand.nextDouble();
				norm += subProb[o][i];
			}
			for (int i = 0; i < subProb[o].length; ++i) {
				subProb[o][i] /= norm;
			}
		}		
	}
}

class VTab<T> {
	int count;
	T value;
	
	VTab() {
		count = 0;
	}
	
	VTab(T v) {
		count = 0;
		value = v;
	}
	
	VTab(int c, T v) {
		count = c;
		value = v;
	}
}

class Traj {
	int time;
	int state;
	double value;
	int node0, node1;
	
	Traj() {	
	}
	
	Traj(int t, int s, int n0, int n1, double v) {
		time = t;
		state = s; value = v;
		node0 = n0; node1 = n1;
	}
}
