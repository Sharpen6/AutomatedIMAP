import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;

public class Model {
	static Random rand = new Random();
	
    int problemID;

	int numStates;
	int numHorizon;
	int numAct0, numAct1;
	int numObs0, numObs1;
	
	ProblemDataSmall data = null;
	
	ArrayList<Integer>[][][] tsList = null;
	ArrayList<Integer>[][][][][] nsList = null;

	public Model(int h, int id) {
		rand.setSeed(System.currentTimeMillis());
		numHorizon = h;
		
		data = new ProblemDataSmall(id);
		
		numStates = data.trans.length;
		
        numAct0 = data.trans[0].length;
		numAct1 = data.trans[0][0].length;

		numObs0 = data.obs.length;
		numObs1 = data.obs[0].length;
		
        System.out.println("states: " + numStates + 
                " act0: " + numAct0 + " act1: " + numAct1 + 
                " obs0: " + numObs0 + " obs1: " + numObs1);
        
		init();
	}
	
	public static int randState(Belief b) {
		double acc = 0.0, thr = rand.nextDouble();
		for (int i : b.nzList) {
			acc += b.stateProb[i];
			if (acc >= thr) {
				return i;
			}
		}
		throw new Error("");
	}
	
	public static int randInt(final double[] prob) {
		return randInt(prob, Model.rand);
	}
	
	public static int randInt(final double[] prob, Random r) {
		double acc = 0.0, thr = r.nextDouble();
		for (int i = 0; i < prob.length; ++i) {
			acc += prob[i];
			if (acc >= thr) {
				return i;
			}
		}
		throw new Error("");
	}
	
	@SuppressWarnings("unchecked")
	public void init() {
		System.out.print("init ...");
		tsList = new ArrayList[numStates][numAct0][numAct1];
		nsList = new ArrayList[numStates][numAct0][numAct1][numObs0][numObs1];
		for (int s = 0; s < numStates; ++s) {
			for (int a0 = 0; a0 < numAct0; ++a0) {
				for (int a1 = 0; a1 < numAct1; ++a1) {
					for (int s_ = 0; s_ < numStates; ++s_) {
						if (getTrans(s, a0, a1, s_) > 1e-6) {
							if (tsList[s][a0][a1] == null) {
								tsList[s][a0][a1] = new ArrayList<Integer>();
							}
							tsList[s][a0][a1].add(s_);
						}
					}
					if (tsList[s][a0][a1] == null || tsList[s][a0][a1].isEmpty()) {
						continue;
					}
                    
					for (int o0 = 0; o0 < numObs0; ++o0) {
						for (int o1 = 0; o1 < numObs1; ++o1) {
							for (int s_ : tsList[s][a0][a1]) {
								if (getObs(o0, o1, s, a0, a1, s_) > 1e-6) {
									if (nsList[s][a0][a1][o0][o1] == null) {
										nsList[s][a0][a1][o0][o1] = new ArrayList<Integer>(); 
									}
									nsList[s][a0][a1][o0][o1].add(s_);
								}
							}
						}
					}
				}
			}
		}
		System.out.println(" done!");
	}
	
	final double[] getStartDist() {
		return data.startDist;
	}
	
	final double getStartDist(int s) {
		return data.startDist[s];
	}

	final double getReward(int s, int a0, int a1) {
		return data.rew[0][s][a0][a1];
	}
	
	final double getTrans(int s, int a0, int a1, int s_) {
		return data.trans[s][a0][a1][s_];
	}
	
	final double getObs(int o0, int o1, int s, int a0, int a1, int s_) {
		return data.obs[o0][o1][s][a0][a1][s_];
	}
}
