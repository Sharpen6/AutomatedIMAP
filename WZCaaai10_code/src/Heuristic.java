import java.util.*;

public class Heuristic {
	Model model;
	
	double[][] mdpValues;
	int[][] mdpActs0, mdpActs1;
	Belief[] mdpBeliefs, randBeliefs;

	public Heuristic(Model m) {
		model = m;
		
		System.out.print("Compute MDP ... ");
		computeMDPValues();
		System.out.println("ok!");
	}
	
	void reset() {
		mdpBeliefs = getBeliefByMDPActions(mdpActs0, mdpActs1);
		randBeliefs = getBeliefByRandPolicy();
	}
	
	double getMMDPValue() {
		double sum = 0.0;
		Belief b = Belief.getInitBelief(model);
		for (int s : b.nzList) {
			sum += b.stateProb[s] * mdpValues[s][model.numHorizon-1];
		}
		return sum;
	}
	
	double[][][][] getMDPQ() {
		System.out.print("Compute QMDP ... ");
		double[][][][] qVal = new double[model.numHorizon][model.numStates][model.numAct0][model.numAct1];
		Collection<Integer> list = null;
		Set<Integer> set = new HashSet<Integer>();
		for (int h = 0; h < model.numHorizon; ++h) {
			for (int s = 0; s < model.numStates; ++s) {
				for (int a0 = 0; a0 < model.numAct0; ++a0) {		
					for (int a1 = 0; a1 < model.numAct1; ++a1) {
						qVal[h][s][a0][a1] = model.getReward(s, a0, a1);
						if (h > 0) {
							for (int s_ = 0; s_ < model.numStates; ++s_) {
								qVal[h][s][a0][a1] += model.getTrans(s, a0, a1, s_) * mdpValues[s][h-1];
							}
						}
					}
				}
			}
		}
		System.out.println("ok!");
		return qVal;
	}
	
	double calcGreedyAction(Belief b, int h, int[] act) {
		Set<Integer> set = new HashSet<Integer>();
		double maxVal = Double.NEGATIVE_INFINITY;
		for (int a0 = 0; a0 < model.numAct0; ++a0) {
			for (int a1 = 0; a1 < model.numAct1; ++a1) {
				double tmpVal = 0.0;
				for (int s : b.nzList) {
					tmpVal += b.stateProb[s] * model.getReward(s, a0, a1);
				}
				if (tmpVal > maxVal) {
					maxVal = tmpVal;
					act[0] = a0;
					act[1] = a1;
				}
			}
		}
		return maxVal;
	}
	
	double getGreedyValue() {
		double value = 0.0;
		int[] act = new int[2];
		int[] obs = new int[2];
		
		int numTrials = 20;
		int a0, a1, a0_, a1_;
		for (int num = 0; num < numTrials; ++num) {
			Belief b0 = Belief.getInitBelief(model);
			Belief b1 = Belief.getInitBelief(model);
			int simulatedState = Belief.getInitState(model);
			for (int t = 0; t < model.numHorizon; ++t) {
//				a0_ = Model.rand.nextInt(model.numAct0);
//				a1_ = Model.rand.nextInt(model.numAct1);				
				int h = model.numHorizon - t -1;
				
				calcGreedyAction(b0, h, act);
				a0 = act[0];
				a1_ = act[1];
				
				calcGreedyAction(b1, h, act);
				a0_ = act[0];
				a1 = act[1];
	
				value += model.getReward(simulatedState, a0, a1);
				simulatedState = getStateBySimRun(simulatedState, a0, a1, obs);
				
				b0 = b0.SimUpdate(a0, a1_, 20, model);
				b1 = b1.SimUpdate(a0_, a1, 20, model);
			}
		}
		value /= (double)numTrials;
		
		return value;
	}
	
	final int[] kmaxSelect(double[] prob, int k) {
		int[] arr = new int[k];
		double curMax = Double.POSITIVE_INFINITY;
		for (int i = 0; i < k; ++i) {
			int n = 0;
			double tmpMax = Double.NEGATIVE_INFINITY;
			for (int j = 0; j < prob.length; ++j) {
				if (prob[j] < curMax-1e-6) {
					if (tmpMax < prob[j]) {
						n = j;
						tmpMax = prob[j];
					}
				}
			}
			curMax = tmpMax;
			arr[i] = n;
		}
		return arr;
	}
	
	final double minmax(double a, double x, double b) {
		if (b < a) {
			throw new Error();
		}
		if (x > b) {
			return b;
		}
		if (x < a) {
			return a;
		}
		return x;
	}
	
	Belief[][] getSamples(double[] portfolio, int maxTrees, final Tree pol0, final Tree pol1) {
		int type = 0;
		int n0 = 0, n1 = 0;
		int a0 = 0, a1 = 0;
		int[] obs = new int[2];
		int simulatedState = 0, nextState = 0;
		Belief[][] beliefs = new Belief[model.numHorizon][maxTrees];
		
		for (int num = 0; num < maxTrees; ++num) {
			beliefs[0][num] = Belief.getInitBelief(model);
			if (portfolio[0] * maxTrees > num) {
				type = 0;
				n0 = num; n1 = num;
			} else if ((portfolio[0] + portfolio[1]) * maxTrees > num) {
				type = 1;
			} else if ((portfolio[0] + portfolio[1] + portfolio[2]) * maxTrees > num) {
				type = 2;
			}
			
			int samplesPerRun = 20;
			for (int t = 0; t < samplesPerRun; ++t) {
				if (pol0 != null)
					n0 = pol0.startTree;
				if (pol1 != null)
					n1 = pol1.startTree;
				simulatedState = Belief.getInitState(model);
				for (int h = 0; h < model.numHorizon; ++h) {
					int k = model.numHorizon - h - 1;
					if (type == 0) {
						a0 = pol0.getLeaf(k, n0).rootAct;
						a1 = pol1.getLeaf(k, n1).rootAct;
					} else if (type == 1) {
						a0 = mdpActs0[simulatedState][k];
						a1 = mdpActs1[simulatedState][k];
					} else if (type == 2) {	
						a0 = Model.rand.nextInt(model.numAct0);
						a1 = Model.rand.nextInt(model.numAct1);					
					}
					
					nextState = getStateBySimRun(simulatedState, a0, a1, obs);
					if (h != model.numHorizon-1) {
						if (beliefs[h+1][num] == null) {
							beliefs[h+1][num] = new Belief(model);
						}
						beliefs[h+1][num].stateProb[nextState] += 1.0;
					}
					
					if (type == 0) {
						n0 = Model.randInt(pol0.getLeaf(k, n0).subProb[obs[0]]);
						n1 = Model.randInt(pol1.getLeaf(k, n1).subProb[obs[1]]);
					}
					
					simulatedState = nextState;
				}
			}
			for (int h = 1; h < model.numHorizon; ++h) {
				for (int s = 0; s < model.numStates; ++s) {
					beliefs[h][num].stateProb[s] /= samplesPerRun;
					if (beliefs[h][num].stateProb[s] > Belief.EPS) {
						beliefs[h][num].nzList.add(s);
					}
				}
			}
		}
		
		return beliefs;
	}
	
	Belief[][] getTrajectories(double[] portfolio, int maxTrees, final Policy pol0, final Policy pol1) {
		int type = 0;
		int n0 = 0, n1 = 0;
		int a0 = 0, a1 = 0;
		int[] obs = new int[2];
		int simulatedState = 0, nextState = 0;
		Belief[][] beliefs = new Belief[model.numHorizon][maxTrees];
		
		int depth = model.numHorizon;
		for (int num = 0; num < maxTrees; ++num) {
			beliefs[0][num] = Belief.getInitBelief(model);
			if (portfolio[0] * maxTrees > num) {
				type = 0;
				n0 = num; n1 = num;
//				depth = Model.rand.nextInt(model.numHorizon);
			} else if ((portfolio[0] + portfolio[1]) * maxTrees > num) {
				type = 1;
			} else if ((portfolio[0] + portfolio[1] + portfolio[2]) * maxTrees > num) {
				type = 2;
			}
			
			int samplesPerRun = DecTBDP.SIM_TRIAL_RUN;
			for (int t = 0; t < samplesPerRun; ++t) {
				n0 = pol0.rootNode;
				n1 = pol1.rootNode;
				simulatedState = Belief.getInitState(model);
				for (int h = 0; h < model.numHorizon; ++h) {
					int k = model.numHorizon - h - 1;
					if (type == 0) {
						if (h < depth) {
							a0 = Model.randInt(pol0.getNode(k, n0).psi);
							a1 = Model.randInt(pol1.getNode(k, n1).psi);
						} else {
							a0 = Model.rand.nextInt(model.numAct0);
							a1 = Model.rand.nextInt(model.numAct1);	
						}
					} else if (type == 1) {
						a0 = mdpActs0[simulatedState][k];
						a1 = mdpActs1[simulatedState][k];
					} else if (type == 2) {	
						a0 = Model.rand.nextInt(model.numAct0);
						a1 = Model.rand.nextInt(model.numAct1);					
					}
					
					nextState = getStateBySimRun(simulatedState, a0, a1, obs);
					if (h != model.numHorizon-1) {
						if (beliefs[h+1][num] == null) {
							beliefs[h+1][num] = new Belief(model);
						}
						beliefs[h+1][num].stateProb[nextState] += 1.0;
					}
					
					if (type == 0 && h < depth) {
						n0 = Model.randInt(pol0.getNode(k, n0).phi[obs[0]]);
						n1 = Model.randInt(pol1.getNode(k, n1).phi[obs[1]]);
					}
					
					simulatedState = nextState;
				}
			}
			for (int h = 1; h < model.numHorizon; ++h) {
				for (int s = 0; s < model.numStates; ++s) {
					beliefs[h][num].stateProb[s] /= samplesPerRun;
					if (beliefs[h][num].stateProb[s] > Belief.EPS) {
						beliefs[h][num].nzList.add(s);
					}
				}
			}
		}
		
		return beliefs;
	}
	
	Belief getBelief(int topDownHeight, double[] portfolio, int num, int maxTrees) {
		if (topDownHeight == 0) {
			return Belief.getInitBelief(model);
		}
		else if (portfolio[0] * maxTrees > num) {
			// random policy heuristic
			if (num == 0) {
				return randBeliefs[topDownHeight];
			} else { 
				return getBeliefByRandSimRun(topDownHeight, num);
			}
		}else if ((portfolio[0] + portfolio[1]) * maxTrees > num) {
			// the MDP heuristic
			if (portfolio[0] * maxTrees > num - 1) {
				return mdpBeliefs[topDownHeight];
			} else {
				return getBeliefByMDPSimRun(topDownHeight, num, mdpActs0, mdpActs1);
			}
		}
		throw new Error("");
	}

	void computeMDPValues() {
		mdpValues = new double[model.numStates][model.numHorizon];
		mdpActs0 = new int[model.numStates][model.numHorizon];
		mdpActs1 = new int[model.numStates][model.numHorizon];
		
		Set<Integer> set = new HashSet<Integer>();
		for (int h = 0; h < model.numHorizon; ++h) {
			for (int s = 0; s < model.numStates; ++s) {
				double maxVal = Double.NEGATIVE_INFINITY;
				for (int a0 = 0; a0 < model.numAct0; ++a0) {
					for (int a1 = 0; a1 < model.numAct1; ++a1) {
						double tmpVal = model.getReward(s, a0, a1);
						if (h > 0) {
							double tmpTrans = 0.0;
                            List<Integer> list = model.tsList[s][a0][a1];
                            for (int s_ : list) {
							//for (int s_ = 0; s_ < model.numStates; ++s_) {
								tmpTrans = model.getTrans(s, a0, a1, s_);
								if (tmpTrans >= 1e-6) {
									tmpVal += tmpTrans * mdpValues[s_][h-1];
								}
							}
						}
						if (tmpVal > maxVal) {
							maxVal = tmpVal;
							mdpActs0[s][h] = a0;
							mdpActs1[s][h] = a1;
						}
					}
				}
				mdpValues[s][h] = maxVal;
			}
		}
	}
	
	Belief[] getBeliefByMDPActions(int[][] acts0, int[][] acts1) {
		Belief[] beliefs = new Belief[model.numHorizon];
		beliefs[0] = Belief.getInitBelief(model);
		
		for (int h = 1; h < model.numHorizon; ++h) {
			beliefs[h] = new Belief(model);
			for (int s1 = 0; s1 < model.numStates; ++s1) {
				double stateProb = 0.0;
				for (int s0 = 0; s0 < model.numStates; ++s0) {
					int a0 = acts0[s0][model.numHorizon - h];
					int a1 = acts1[s0][model.numHorizon - h];
					stateProb += model.getTrans(s0, a0, a1, s1) * beliefs[h - 1].stateProb[s0];
				}
				if (stateProb <= Belief.EPS) {
					stateProb = 0.0;
				} else {
					beliefs[h].nzList.add(s1);
				}
				beliefs[h].stateProb[s1] = stateProb;
			}
		}
		return beliefs;
	}
	
	Belief getBeliefByMDPSimRun(int topDownHeight, int num, int[][] acts0, int[][] acts1) {
		Belief belief = new Belief(model);
		int samplesPerRun = (num % 10) + 1;
		for (int i = 0; i < samplesPerRun; i++) {
			int simulatedState = Belief.getInitState(model);
			for (int h = 0; h < topDownHeight; ++h) {
				int a0 = acts0[simulatedState][model.numHorizon - h - 1];
				int a1 = acts1[simulatedState][model.numHorizon - h - 1];
				
				double acc = 0.0;
				double thr = Model.rand.nextDouble();
				int nextState = -1;
				for (int s = 0; s < model.numStates; ++s) {
					acc += model.getTrans(simulatedState, a0, a1, s);
					if (acc >= thr) {
						nextState = s;
						break;
					}
				}
				assert (nextState != -1);
				simulatedState = nextState;
			}
			belief.stateProb[simulatedState]++;
		}
		
		for (int s = 0; s < model.numStates; ++s) {
			belief.stateProb[s] /= samplesPerRun;
			if (belief.stateProb[s] > Belief.EPS) {
				belief.nzList.add(s);
			}
		}
		return belief;
	}
	
	Belief[] getBeliefByRandPolicy() {
		Belief[] beliefs = new Belief[model.numHorizon];
		beliefs[0] = Belief.getInitBelief(model);
		
		double numActx2 = model.numAct0 * model.numAct1;
		for (int h = 1; h < model.numHorizon; ++h) {
			beliefs[h] = new Belief(model);
			for (int s1 = 0; s1 < model.numStates; ++s1) {
				double stateProb = 0.0;
				for (int a0 = 0; a0 < model.numAct0; ++a0)
					for (int a1 = 0; a1 < model.numAct1; ++a1){
							for (int s0 = 0; s0 < model.numStates; ++s0) 
								stateProb += (1.0 /  numActx2) * beliefs[h - 1].stateProb[s0] * model.getTrans(s0, a0, a1, s1);
					}
				if (stateProb <= Belief.EPS) {
					stateProb = 0.0;
				} else {
					beliefs[h].nzList.add(s1);
				}
				beliefs[h].stateProb[s1] = stateProb;
			}
		}
		return beliefs;
	}
	
	Belief getBeliefByRandSimRun(int topDownHeight, int num) {
		Belief belief = new Belief(model);
		int samplesPerRun = (num % 10) + 1;
		for (int i = 0; i < samplesPerRun; i++) {
			int simulatedState = Belief.getInitState(model);
			for (int h = 0; h < topDownHeight; ++h) {
				int a0 = Model.rand.nextInt(model.numAct0);
				int a1 = Model.rand.nextInt(model.numAct1);
				
				double acc = 0.0;
				double thr = Model.rand.nextDouble();
				int nextState = -1;
				for (int s = 0; s < model.numStates; ++s) {
					acc += model.getTrans(simulatedState, a0, a1, s);
					if (acc >= thr) {
						nextState = s;
						break;
					}
				}
				assert (nextState != -1);
				simulatedState = nextState;
			}
			belief.stateProb[simulatedState]++;
		}
		
		for (int s = 0; s < model.numStates; ++s) {
			belief.stateProb[s] /= samplesPerRun;
			if (belief.stateProb[s] > Belief.EPS) {
				belief.nzList.add(s);
			}
		}
		return belief;
	}
	
	int getStateBySimRun(int state, int a0, int a1, int[] obs) {
		double acc = 0.0, thr = 0.0;
		
		int state_ = -1;
		acc = 0.0;
		thr = Model.rand.nextDouble();
		
		Collection<Integer> list = null;
		Set<Integer> set = new HashSet<Integer>();
		if (model.tsList != null) {
			list = model.tsList[state][a0][a1];
		} else {
			for (int s = 0; s < model.numStates; ++s) {
				acc += model.getTrans(state, a0, a1, s);
				if (acc >= thr) {
					state_ = s;
					break;
				}
			}
		}
		if (list != null) {
			for (int s : list) {
				acc += model.getTrans(state, a0, a1, s);
				if (acc >= thr) {
					state_ = s;
					break;
				}
			}
		}
		
		int obs0 = -1, obs1 = -1;
		acc = 0.0;
		thr = Model.rand.nextDouble();
		obs_loop: {
			for (int o0 = 0; o0 < model.numObs0; ++o0)
				for (int o1 = 0; o1 < model.numObs1; ++o1) {
					acc += model.getObs(o0, o1, state, a0, a1, state_);
					if (acc >= thr) {
						obs0 = o0;
						obs1 = o1;
						break obs_loop;
					}
				}
		}
		obs[0] = obs0; obs[1] = obs1;
		
		return state_;
	}
	
	Belief getBeliefByMDPExplore(int topDownHeight, final int[][] acts0, final int[][] acts1) {
		Belief belief = Belief.getInitBelief(model);
		int state = Belief.getInitState(model);
		int[] obs = new int[2];
		
		for (int t = 0; t < topDownHeight; ++t) {
			int h = model.numHorizon - t - 1;
			
			int a0 = acts0[state][h];
			int a1 = acts1[state][h];
			int s_ = getStateBySimRun(state, a0, a1, obs);
			
			state = s_;
			belief = belief.BayesUpdate(a0, a1, obs[0], obs[1], model);
		}
		
		return belief;
	}
	
	int getStateByPolicy(Policy pol0, Policy pol1, int topDownHeight) {
		int state = Belief.getInitState(model);
		int[] obs = new int[2];
		int q0 = 0, q1 = 0;

		for (int t = 0; t < topDownHeight; ++t) {
			int h = model.numHorizon - t - 1;
			int a0 = Model.randInt(pol0.getNode(h, q0).psi);
			int a1 = Model.randInt(pol1.getNode(h, q1).psi);
				
			state = getStateBySimRun(state, a0, a1, obs);
			q0 = Model.randInt(pol0.getNode(h, q0).phi[obs[0]]);
			q1 = Model.randInt(pol1.getNode(h, q1).phi[obs[1]]);
		}
		
		return state;
	}
	
	Belief getBeliefByPolicySimRun(Policy pol0, Policy pol1, int trials, int topDownHeight) {
		Belief b = new Belief(model);
		
		b.weight = 1.0;
		b.nzList.clear();
		
		for (int i = 0; i < trials; ++i) {
			int state = getStateByPolicy(pol0, pol1, topDownHeight);
			b.stateProb[state]++;
		}
		
		for (int s = 0; s < model.numStates; ++s) {
			b.stateProb[s] /= trials;
			if (b.stateProb[s] > Belief.EPS) {
				b.nzList.add(s);
			}
		}
		
		return b;
	}
	
	double getLookheadValue(int h, final Belief b, int[] acts) {
		double value = Double.NEGATIVE_INFINITY;
		for (int a0 = 0; a0 < model.numAct0; ++a0) {
			for (int a1 = 0; a1 < model.numAct1; ++a1) {
				double tmp = 0.0;
				for (int s : b.nzList) {
					double tmp_ = model.getReward(s, a0, a1);
					if (h > 0) {
						for (int s_ = 0; s_ < model.numStates; ++s_) {
							tmp_ += model.getTrans(s, a0, a1, s_) * mdpValues[s_][h - 1];
						}
					}
					tmp += tmp_ * b.stateProb[s];
				}
				if (tmp > value) {
					value = tmp;
					acts[0] = a0;
					acts[1] = a1;
				}
			}
		}
		return value;
	}
	
	double selectBestPolicy(int h, final Belief b, final Policy pol0, final Policy pol1, int[] nodes) {
		double value = Double.NEGATIVE_INFINITY;
		Node[] n0 = pol0.layerList[h].nodeList;
		Node[] n1 = pol1.layerList[h].nodeList;
		for (int q0 = 0; q0 < n0.length; ++q0) {
			for (int q1 = 0; q1 < n1.length; ++q1) {
				double tmp = 0.0;
				for (int s : b.nzList) {
					tmp += b.stateProb[s] * n0[q0].getValue(q1, s); //.stateValue[q1][s];
				}
				if (tmp > value) {
					value = tmp;
					nodes[0] = q0;
					nodes[1] = q1;
				}
			}
		}
		return value;
	}
	
	ArrayList<JointHistory> getHistBySimRun(Policy pol0, Policy pol1, int type) {
		ArrayList<JointHistory> histList = new ArrayList<JointHistory>();
		
		Belief belief = Belief.getInitBelief(model);
		int state = Belief.getInitState(model);
		int[] obs = new int[2];
		int q0 = 0, q1 = 0;
		
		for (int t = 0; t < model.numHorizon; ++t) {
			int h = model.numHorizon - t - 1;
			
			histList.add(new JointHistory(belief, q0, q1, h));
			
			int a0 = 0, a1 = 0;
			
			switch (type) {
			case 0:
				a0 = this.mdpActs0[state][h];
				a1 = this.mdpActs1[state][h];
				break;
			case 1:
				a0 = Model.randInt(pol0.getNode(h, q0).psi);
				a1 = Model.randInt(pol1.getNode(h, q1).psi);
				break;
			default:
				a0 = Model.rand.nextInt(model.numAct0);
				a1 = Model.rand.nextInt(model.numAct1);
				break;
			}
			
			state = getStateBySimRun(state, a0, a1, obs);
			q0 = Model.randInt(pol0.getNode(h, q0).phi[obs[0]]);
			q1 = Model.randInt(pol1.getNode(h, q1).phi[obs[1]]);
		
//			belief = belief.TransUpdate(a0, a1, model);
			belief = belief.SimUpdate(a0, a1, 10, model);
//			belief = belief.BayesUpdate(a0, a1, obs[0], obs[1], model);
		}
		
		return histList;
	}
}

class JointHistory {
	int height;
	Belief belief;
	int node0, node1;
	
	JointHistory(Belief b, int n0, int n1, int h) {
		belief = b;
		node0 = n0;
		node1 = n1;
		height = h;
	}
}

class Belief {
	final static double EPS = 1e-6;
	
	double weight;
	double[] stateProb;
	ArrayList<Integer> nzList;
	
	Belief() {
		weight = 1.0;
		stateProb = null;
		nzList = null;
	}
	
	Belief(Model m) {
		weight = 1.0;
		stateProb = new double[m.numStates];
		nzList = new ArrayList<Integer>();
	}
	
	Belief(Belief b) {
		weight = b.weight;
		stateProb = new double[b.stateProb.length];
		System.arraycopy(b.stateProb, 0, stateProb, 0, stateProb.length);
		nzList = new ArrayList<Integer>();
		nzList.addAll(b.nzList);
	}
	
	void add(Belief b) {
		for (int s : b.nzList) {
			stateProb[s] += b.stateProb[s];
		}
		norm();
	}
	
	void norm() {
		double sum = 0.0;
		for (int s = 0; s < stateProb.length; ++s) {
			sum += stateProb[s];
		}
		nzList.clear();
		for (int s = 0; s < stateProb.length; ++s) {
			stateProb[s] /= sum;
			if (stateProb[s] > Belief.EPS) {
				nzList.add(s);
			}
		}
	}
	
	boolean equalTo(Belief b, int r) {
		for (int s = 0; s < stateProb.length; ++s) {
			double n = Math.round(stateProb[s] * r) / (double)r;
			double n_ = Math.round(b.stateProb[s] * r) / (double)r;
			if (n != n_) {
				return false;
			}
		}
		return true;
	}
	
	Belief SimUpdate(int a0, int a1, int samplesPerRun, Model m) {
		Belief b = new Belief(m);
		
		b.weight = 1.0;
		b.nzList.clear();
		
		for (int i = 0; i < samplesPerRun; i++) {
			int simulatedState = Model.randInt(stateProb);
			double acc = 0.0;
			double thr = Model.rand.nextDouble();
			int nextState = -1;
			for (int s = 0; s < m.numStates; ++s) {
				acc += m.getTrans(simulatedState, a0, a1, s);
				if (acc >= thr) {
					nextState = s;
					break;
				}
			}
			assert (nextState != -1);
			simulatedState = nextState;

			b.stateProb[simulatedState]++;
		}
		
		for (int s = 0; s < m.numStates; ++s) {
			b.stateProb[s] /= samplesPerRun;
			if (b.stateProb[s] > Belief.EPS) {
				b.nzList.add(s);
			}
		}
		
		return b;
	}
	
	Belief TransUpdate(int a0, int a1, Model m) {
		Belief b = new Belief(m);
		
		b.weight = 1.0;
		b.nzList.clear();
		for (int s_ = 0; s_ < m.numStates; ++s_) {
			b.stateProb[s_] = 0.0;
			if (nzList == null || nzList.isEmpty()) {
				for (int s = 0; s < m.numStates; ++s) {
					b.stateProb[s_] += m.getTrans(s, a0, a1, s_) * stateProb[s];
				}
			} else {
				for (int s : nzList) {
					b.stateProb[s_] += m.getTrans(s, a0, a1, s_) * stateProb[s];
				}
			}
			if (b.stateProb[s_] > Belief.EPS) {
				b.nzList.add(s_);
			}
		}
		
		return b;
	}
	
	Belief BayesUpdate(int a0, int a1, int o0, int o1, Model m) {
		Belief b = new Belief(m);	
		
		double norm = 0.0;
		for (int s_ = 0; s_ < m.numStates; ++s_) {
			b.stateProb[s_] = 0.0;
			if (nzList == null || nzList.isEmpty()) {
				for (int s = 0; s < m.numStates; ++s) {
					b.stateProb[s_] += m.getTrans(s, a0, a1, s_) * m.getObs(o0, o1, s, a0, a1, s_) * stateProb[s];
				}
			} else {
				for (int s : nzList) {
					b.stateProb[s_] += m.getTrans(s, a0, a1, s_) * m.getObs(o0, o1, s, a0, a1, s_) * stateProb[s];
				}
			}
			norm += b.stateProb[s_];
		}
		
		if (Double.compare(norm, 0.0) == 0) {
			return null;
		}
		
		b.nzList.clear();
		b.weight = weight * norm;
		for (int s_ = 0; s_ < m.numStates; ++s_) {
			b.stateProb[s_] /= norm;
			if (b.stateProb[s_] > Belief.EPS) {
				b.nzList.add(s_);
			}
		}
		
		return b;
	}
	
	static Belief getInitBelief(Model m) {
		Belief belief = new Belief(m);
		for (int s = 0; s < m.numStates; ++s) {
			belief.stateProb[s] = m.getStartDist(s);
			if (belief.stateProb[s] > Belief.EPS) {
				belief.nzList.add(s);
			}
		}
		return belief;
	}
	
	static int getInitState(Model m) {
		int state = -1;
		double acc = 0.0;
		double thr = Model.rand.nextDouble();
		for (int s = 0; s < m.numStates; ++s) {
			acc += m.getStartDist(s);
			if (acc >= thr) {
				state = s;
				break;
			}
		}
		return state;
	}
}
