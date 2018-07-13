import java.util.*;
import java.util.concurrent.*;

public class Policy {
	int rootNode;
	Layer[] layerList;

	public Policy(int agent, Model m) {
		layerList = new Layer[m.numHorizon];
		for (int i = 0; i < layerList.length; ++i) {
			layerList[i] = new Layer(agent, m);
		}
		rootNode = Model.rand.nextInt(Layer.MAX_NODES);
	}
	
	public Policy(int agent, Model m, CorrDevice corr) {
		layerList = new Layer[m.numHorizon];
		for (int i = 0; i < layerList.length; ++i) {
			layerList[i] = new Layer(agent, m, corr);
		}
	}
	
	public void print() {
		System.out.println("**");
		for (int i = 0; i < layerList.length; ++i) {
			layerList[i].print();
		}
	}
	
	public void reset() {
		for (int i = 0; i < layerList.length; ++i) {
			layerList[i].reset();
		}
	}
	
	public final Layer[] getLayers() {
		return layerList;
	}
	
	public final Layer getLayer(int h) {
		return layerList[h];
	}
	
	public final Node[] getNodes(int h) {
		return layerList[h].nodeList;
	}
	
	public final Node getNode(int h, int q) {
		return layerList[h].nodeList[q];
	}

}

class Layer {
	public static int MAX_NODES = 3; // 10

	Node[] nodeList;
	
	CorrDevice corrDevice;
	CorrNode[] corrNodeList;
	
	public Layer(int agent, Model m) {
		nodeList = new Node[MAX_NODES];
		for (int i = 0; i < nodeList.length; ++i) {
			nodeList[i] = new Node(agent, m);
			nodeList[i].genRandNode();
		}
	}
	
	public Layer(int agent, Model m, CorrDevice corr) {
		corrDevice = corr;
		
		corrNodeList = new CorrNode[MAX_NODES];
		for (int i = 0; i < corrNodeList.length; ++i) {
			corrNodeList[i] = new CorrNode(agent, m, corr);
			corrNodeList[i].genRandNode();
		}
	}
	
	public void print() {
		System.out.println("--");
		for (int i = 0; i < nodeList.length; ++i) {
			nodeList[i].print();
		}
	}
	
	public void reset() {
		for (int i = 0; i < nodeList.length; ++i) {
			nodeList[i].reset();
		}
	}
	
	public void clean() {
		for (int i = 0; i < nodeList.length; ++i) {
			nodeList[i].clean();
		}
	}
}

class Node {
	final static double EPS = 1e-9;
	
	int agentID;
	Model model;
	
	double[] psi;
	double[][] phi;
	double[][][] eta;
	double[][] stateValue;
	
	Belief[] beliefs;
	Map<Integer, Double>[] mapsValue;
	Map<Integer, Integer>[] stateCount;
	
//	Node shadow;
	ArrayList<Integer> actList;
	
	@SuppressWarnings("unchecked")
	public Node(int agent, Model m) {
		assert (agent == 0 || agent == 1);
		if (agent == 0) {
			psi = new double[m.numAct0];
			phi = new double[m.numObs0][Layer.MAX_NODES];
			eta = new double[m.numAct0][m.numObs0][Layer.MAX_NODES];
		} else {
			psi = new double[m.numAct1];
			phi = new double[m.numObs0][Layer.MAX_NODES];
			eta = new double[m.numAct1][m.numObs1][Layer.MAX_NODES];
		}
		model = m;
		agentID = agent;
		actList = new ArrayList<Integer>();
		
		stateValue = null;
		beliefs = new Belief[Layer.MAX_NODES];
		
		mapsValue = new Map[Layer.MAX_NODES];
		for (int i = 0; i < mapsValue.length; ++i) {
			if (DecTBDP.MUL_THRD) {
				mapsValue[i] = new ConcurrentHashMap<Integer, Double>();
			} else {
				mapsValue[i] = new HashMap<Integer, Double>();
			}
		}
		stateCount = new Map[Layer.MAX_NODES];
		for (int i = 0; i < stateCount.length; ++i) {
			if (DecTBDP.MUL_THRD) {
				stateCount[i] = new ConcurrentHashMap<Integer, Integer>();
			} else {
				stateCount[i] = new HashMap<Integer, Integer>();
			}
		}
	}
	
	public void print() {
		for (int i = 0; i < psi.length; ++i) {
			System.out.print(psi[i] + " ");
		}
		System.out.println();
	}
	
	public final void clean() {
		stateValue = null;
		Arrays.fill(beliefs, null);
		
		for (int i = 0; i < mapsValue.length; ++i) {
			if (DecTBDP.MUL_THRD) {
				synchronized (mapsValue[i]){
					mapsValue[i].clear();
				}
			} else {
				mapsValue[i].clear();
			}
		}
		for (int i = 0; i < stateCount.length; ++i) {
			if (DecTBDP.MUL_THRD) {
				synchronized (stateCount[i]){
					stateCount[i].clear();
				}
			} else {
				stateCount[i].clear();
			}
		}
	}
	
	public final void reset() {
		clean();
		genRandNode();
	}
	
	public final int getStateCount(int q, int s) {
		if (DecTBDP.MUL_THRD) {
			synchronized (stateCount[q]) {
				if (stateCount[q].containsKey(s)) {
					return stateCount[q].get(s);
				}
			}
		} else {
			if (stateCount[q].containsKey(s)) {
				return stateCount[q].get(s);
			}
		}
		return 0;
	}
	
	public final void setStateCount(int q, int s, int count) {
		if (DecTBDP.MUL_THRD) {
			synchronized(stateCount[q]) {
				stateCount[q].put(s, count);
			}
		} else {
			stateCount[q].put(s, count);
		}
	}
	
	public final void setValue(int q, int s, double value) {
		if (DecTBDP.MUL_THRD) {
			synchronized(mapsValue[q]) {
				mapsValue[q].put(s, value);
			}
		} else {
			mapsValue[q].put(s, value);
		}
	}
	
	public final double getValue(int q, int s) {
		if (stateValue != null) {
			return stateValue[q][s];
		}
		
		if (DecTBDP.MUL_THRD) {
			synchronized (mapsValue[q]) {
				if (mapsValue[q].containsKey(s)) {
					return mapsValue[q].get(s);
				}
			}
		} else {
			if (mapsValue[q].containsKey(s)) {
				return mapsValue[q].get(s);
			}
		}
		return 0.0;
	}
	
	public final boolean hasValue() {
		if (stateValue != null || mapsValue[0] != null) {
			return true;
		}
		return false;
	}
	
	public final boolean hasValue(int q, int s) {
		if (stateValue != null) {
			return true;
		}
		if (mapsValue[q] != null && mapsValue[q].containsKey(s)) {
			return true;
		}
		return false;
	}	
	
//	public static void copy(Node src, Node dest) {
//		dest.agentID = src.agentID;
//		System.arraycopy(src.psi, 0, dest.psi, 0, src.psi.length);
//		for (int o = 0; o < src.phi.length; ++o) {
//			System.arraycopy(src.phi[o], 0, dest.phi[o], 0, src.phi[o].length);
//		}
//		for (int a = 0; a < src.eta.length; ++a) {
//			for (int o = 0; o < src.eta[a].length; ++o) {
//				System.arraycopy(src.eta[a][o], 0, dest.eta[a][o], 0, src.eta[a][o].length);
//			}
//		}
//		
//		dest.actList.clear();
//		for (int i : src.actList) {
//			dest.actList.add(i);
//		}
//	}
	
	public void update() {
		actList.clear();
		for (int a = 0; a < psi.length; ++a) {
			if (psi[a] > Node.EPS) {
				actList.add(a);
			}
		}
		
//		for (int o = 0; o < phi.length; ++o) {
//			for (int q = 0; q < phi[o].length; ++q) {
//				phi[o][q] = 0.0;
//				for (int a = 0; a < psi.length; ++a) {
//					phi[o][q] += eta[a][o][q];
//				}
//			}
//		}
	}
	
	public void genRandNode() {
		Random r = Model.rand;
		double sum = 0.0;
		
		for (int a = 0; a < psi.length; ++a) {
			psi[a] = r.nextDouble();
			sum += psi[a];
		}
		actList.clear();
		for (int a = 0; a < psi.length; ++a) {
			psi[a] /= sum;
			if (psi[a] > Node.EPS) {
				actList.add(a);
			}
		}
		
		for (int o = 0; o < phi.length; ++o) {
			sum = 0.0;
			for (int q = 0; q < phi[o].length; ++q) {
				phi[o][q] = r.nextDouble();
				sum += phi[o][q];
			}
			for (int q = 0; q < phi[o].length; ++q) {
				phi[o][q] /= sum;
			}
		}
		
		for (int a = 0; a < eta.length; ++a) {
			for (int o = 0; o < eta[a].length; ++o) {
				for (int q = 0; q < eta[a][o].length; ++q) {
					eta[a][o][q] = psi[a] * phi[o][q];
				}
			}
		}
	}
}

class CorrDevice{
	public final static int MAX_CORR = 2;
	
	double[][][] trans;
	
	CorrDevice(int horizon) {
		trans = new double[horizon][MAX_CORR][MAX_CORR];
	}
	
	public void genRandDevice() {
		Random r = Model.rand;
		
		for (int h = 0; h < trans.length; ++h) {
			for (int i = 0; i < trans[h].length; ++i) {
				double sum = 0.0;
				for (int j = 0; j < trans[h][i].length; ++j) {
					trans[h][i][j] = r.nextDouble();
					sum += trans[h][i][j];
				}
				for (int j = 0; j < trans[h][i].length; ++j) {
					trans[h][i][j] /= sum;
				}
			}
		}
	}
}

class CorrNode {
	double[][] psi;
	double[][][] phi;
	double[][][][] eta;
	double[][][] stateValue;
	
	CorrNode(int agent, Model m, CorrDevice corr) {
		int ncorr = CorrDevice.MAX_CORR;
		if (agent == 0) {
			psi = new double[ncorr][m.numAct0];
			phi = new double[ncorr][m.numObs0][Layer.MAX_NODES];
			eta = new double[ncorr][m.numAct0][m.numObs0][Layer.MAX_NODES];
		} else {
			psi = new double[ncorr][m.numAct1];
			phi = new double[ncorr][m.numObs1][Layer.MAX_NODES];
			eta = new double[ncorr][m.numAct1][m.numObs1][Layer.MAX_NODES];
		}
		stateValue = null;
	}
	
	public void genRandNode() {
		Random r = Model.rand;
		
		for (int c = 0; c < psi.length; ++c) {
			double sum = 0.0;
			for (int a = 0; a < psi[c].length; ++a) {
				psi[c][a] = r.nextDouble();
				sum += psi[c][a];
			}
			for (int a = 0; a < psi[c].length; ++a) {
				psi[c][a] /= sum;
				if (psi[c][a] > Node.EPS) {
				}
			}

			for (int o = 0; o < phi[c].length; ++o) {
				sum = 0.0;
				for (int q = 0; q < phi[c][o].length; ++q) {
					phi[c][o][q] = r.nextDouble();
					sum += phi[c][o][q];
				}
				for (int q = 0; q < phi[c][o].length; ++q) {
					phi[c][o][q] /= sum;
				}
			}

			for (int a = 0; a < eta[c].length; ++a) {
				for (int o = 0; o < eta[c][a].length; ++o) {
					for (int q = 0; q < eta[c][a][o].length; ++q) {
						eta[c][a][o][q] = psi[c][a] * phi[c][o][q];
					}
				}
			}
		}
	}
}
