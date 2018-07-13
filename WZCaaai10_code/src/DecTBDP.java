/**
 * 
 */

import java.util.logging.*;

import lpsolve.*;

import java.util.*;
//import java.io.*;


/**
 * @author Feng Wu
 *
 */
public class DecTBDP {
	/**
	 * @param args
	 */
	public static void main(String[] args) {
        int problemId = 0;
        int problemHorizon = 4;

        int maxTrees = 100;

		DecTBDP solver = new DecTBDP(problemHorizon, problemId);
		solver.solve(maxTrees);
	}
	
	Logger log;
	Model model;
	Heuristic heuristic;
	Policy policyAgent0, policyAgent1;
	
	static long EvaluationCount = 0;
	
	public static int MAX_STATE_EVA = 20;
	public static int SIM_TRIAL_RUN = 20;
	public static int EVA_TRIAL_RUN = 20;
	
	public final static int EVA_TYPE = 2; 
	public final static boolean MUL_THRD = true;

	/**
	 * 
	 */
	public DecTBDP(int horizon, int id) {
		log = Utils.getLogger(this);
		model = new Model(horizon, id);
		heuristic = new Heuristic(model);
	}
	
	public void reset() {
		Model.rand.setSeed(System.currentTimeMillis());
		policyAgent0.reset();
		policyAgent1.reset();
	}
	
	public void solve(int maxTrees) {
		log.info("TBDP Solver");
		
		Layer.MAX_NODES = maxTrees;
        policyAgent0 = new Policy(0, model);
		policyAgent1 = new Policy(1, model);
		Linear.init(model);
		
		if (EVA_TYPE == 1) {
			Walker.init(model);
		}
		
		double[] portfolio = {0.0, 0.45, 0.55};
		
//		portfolio[0] = 0.0;
//		portfolio[1] = 0.0;
//		portfolio[2] = 1 - portfolio[0] - portfolio[1];
	
		int runs = 20;
		
		Data.setFile(model.problemID + "-" 
				+ model.numHorizon + "-"
				+ DecTBDP.EVA_TRIAL_RUN + "-"
				+ Layer.MAX_NODES + "-"
				+ runs + ".xls");
		Data.setHeader("# ");
		Data.setFormat("%.01f\t %.01f");
		
		double count = 0.0, time = 0.0, values = 0.0;
		for (int i = 0; i < runs; ++i) {
			EvaluationCount = 0;
			
			log.info("***************** Run " + (i+1));
			double reward = solveTBDP(portfolio, 1.0);
			count += EvaluationCount;
			time += time_record;
			values += reward;
			reset();
			
			Data.append(String.format(Data.format, reward, time_record));
			Data.flush();
		}
		time /= runs;
		values /= runs;
		log.info("Average Time: " + time);
		log.info("Average Value: " + values);
		
		double total = runs * model.numHorizon * 
				EVA_TRIAL_RUN * Layer.MAX_NODES * 
				Layer.MAX_NODES * model.numStates;
		
		double percent = count / total;
		log.info("Average Count: " + count + "/" + total + "=" + percent);
		
		Data.append("# average");
		Data.append("# " + String.format(Data.format, values, time));
		Data.append("# evaluation factor: " + percent);
		Data.close();
	}
	
	private double time_record = 0.0;
	public double solveTBDP(double[] portfolio, double rate) {
		int maxTrees = Layer.MAX_NODES;
		
		MultiCore.init();
		EvalTask[] tasklist = null;
		if (MUL_THRD) {
			tasklist = new EvalTask[MultiCore.numOfCPU - 1];
			for (int i = 0; i < tasklist.length; ++i) {
				tasklist[i] = new EvalTask(i);
				MultiCore.execute(tasklist[i]);
			}
		}
		
		Timer.start();
		Belief[][] beliefList = heuristic.getTrajectories(portfolio, maxTrees, policyAgent0, policyAgent1);
		for (int h = 0; h < model.numHorizon; ++h) {
			log.info("############### Horizon: " + h);
			Timer.start();
			int topDownHeight = model.numHorizon - h - 1;
			for (int n = 0; n < maxTrees; ++n) {
                log.info("--- tree: " + n);
				Belief belief = beliefList[topDownHeight][n];
				
				int i = n, j = n;
				Node node0 = policyAgent0.layerList[h].nodeList[i];
				Node node1 = policyAgent1.layerList[h].nodeList[j];
				runLinearProgramming(h, node0, node1, belief, rate);
				node0.beliefs[j] = belief;
				node1.beliefs[i] = belief;
			}	
			Timer.end("SolveLP");
			
			if (EVA_TYPE != 2) {
				Timer.start();
				if (EVA_TYPE == 0) {
					evaluate(h, policyAgent0, policyAgent1);
				}
				if (EVA_TYPE == 1) {
					evalByWeight(h, policyAgent0, policyAgent1);
				}
				Timer.end("EvalPol");
			} 
			else if (MUL_THRD) {
				for (int i = 0; i < tasklist.length; ++i) {
					tasklist[i].update(h);
				}
			}
		}
		time_record = Timer.end("Total Time");

		double svalue = getBestValueBySim(model.numHorizon - 1, policyAgent0, policyAgent1);
		log.info("Simulation Value: " + svalue);
		
		if (MUL_THRD) {
			for (EvalTask t : tasklist) {
				t.close();
			}
			MultiCore.close();
		}
		return svalue;
	}
	
	public double getBestValue(int h, Policy pol0, Policy pol1) {
		int n0 = 0, n1 = 0;
		double value = Double.NEGATIVE_INFINITY;
		for (int q0 = 0; q0 < Layer.MAX_NODES; ++q0) {
			for (int q1 = 0; q1 < Layer.MAX_NODES; ++q1) {
				double tmpVal = 0.0;
				for (int s = 0; s < model.numStates; ++s) {
					tmpVal += model.getStartDist(s) * pol0.layerList[h].nodeList[q0].getValue(q1, s); //.stateValue[q1][s];
				}
				if (tmpVal > value) {
					n0 = q0;
					n1 = q1;
					value = tmpVal;
				}
			}
		}
		pol0.rootNode = n0;
		pol1.rootNode = n1;
		return value;
	}
	
	public double getBestValueBySim(int h, Policy pol0, Policy pol1) {
		int n0 = 0, n1 = 0;
		double value = Double.NEGATIVE_INFINITY;
		
		value = 0.0;
		int a0 = 0, a1 = 0;
		int[] obs = new int[2];
		int numTrials = 1000;
		for (int num = 0; num < numTrials; ++num) {
			int simulatedState = Belief.getInitState(model);
			n0 = pol0.rootNode; n1 = pol1.rootNode;
			for (int t = 0; t <= h; ++t) {
				int h_ = model.numHorizon - t -1;
				Node node0 = pol0.layerList[h_].nodeList[n0];
				Node node1 = pol1.layerList[h_].nodeList[n1];
				
				a0 = Model.randInt(node0.psi);
				a1 = Model.randInt(node1.psi);
				value += model.getReward(simulatedState, a0, a1);
				simulatedState = heuristic.getStateBySimRun(simulatedState, a0, a1, obs);
				n0 = Model.randInt(node0.phi[obs[0]]);
				n1 = Model.randInt(node1.phi[obs[1]]);
			}
		}
		value /= (double)numTrials;
		
		return value;
	}
	
	class EvalTask implements CoreTask {
		int ID = 0;
		int currHorizon = 0;
		boolean isRuning = false;
//		BufferedWriter tasklog = null;
		
		public EvalTask(int id) {
			ID = id;
//			try {
//				tasklog = new BufferedWriter(new FileWriter("task-" + id + ".log"));
//				print("# task log: " + id);
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
		}
		
		void close() {
//			try {
//				tasklog.flush();
//				tasklog.close();
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
		}
		
		void print(String str) {
//			try {
//				tasklog.write(str + "\n");
//				tasklog.flush();
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
		}
		
		public void update(int h) {
			if (!isRuning) {
				print("Updating: " + h);
				currHorizon = h;
				isRuning = true;
			}
		}
		
		public void doTask() {
			while (MultiCore.isRuning) {
				if (!isRuning) {
					MultiCore.sleep(1);
					continue;
				}
				print("Horizon: " + currHorizon);
				long t0 = System.currentTimeMillis();
				for (int q0 = 0; q0 < Layer.MAX_NODES; ++q0) {
					for (int q1 = 0; q1 < Layer.MAX_NODES; ++q1) {
						for (int s = 0; s < model.numStates; ++s) {
							if (!MultiCore.isRuning) return;
							evalBySim(currHorizon, q0, q1, s, false);
						}
					}
				}
				for (int h = 0; h < currHorizon-1; ++h) {
					policyAgent0.getLayer(h).clean();
					policyAgent1.getLayer(h).clean();
				}
				long t1 = System.currentTimeMillis();
				print("Time: " + (double)(t1-t0)/1000.0);
				isRuning = false;
			}
		}
	}
	
	class Elem {
		int h, n0, n1, s;
		double v;
		Elem(int h_, int n0_, int n1_, int s_, double v_) {
			h = h_; n0 = n0_; n1 = n1_; s = s_; v = v_;
		}
	}
	public void evalBySim(int h, int n0, int n1, int backupState, boolean im) {
		Policy pol0 = policyAgent0, pol1 = policyAgent1;
		int currTrials = pol0.getNode(h, n0).getStateCount(n1, backupState);
		
		if (currTrials >= DecTBDP.EVA_TRIAL_RUN) {
			return;
		}
		
		int numTrials = DecTBDP.EVA_TRIAL_RUN - currTrials;
		Stack<Elem> stack = new Stack<Elem>();
		int backupN0 = n0, backupN1 = n1;
		
		double value = 0.0;
		int a0 = 0, a1 = 0;
		int[] obs = new int[2];
		for (int num = 0; num < numTrials; ++num) {
			int simulatedState = backupState;
			n0 = backupN0; n1 = backupN1;
			stack.clear();
			value = 0.0;
			for (int t = h; t >= 0; --t) {
				Node node0 = pol0.layerList[t].nodeList[n0];
				Node node1 = pol1.layerList[t].nodeList[n1];
				
				int count = node0.getStateCount(n1, simulatedState);
				if (count >= EVA_TRIAL_RUN) {
					value = node0.getValue(n1, simulatedState);
					break;
				}
				
				a0 = Model.randInt(node0.psi);
				a1 = Model.randInt(node1.psi);
				value = model.getReward(simulatedState, a0, a1);
				stack.push(new Elem(t, n0, n1, simulatedState, value));
				
				simulatedState = heuristic.getStateBySimRun(simulatedState, a0, a1, obs);
				n0 = Model.randInt(node0.phi[obs[0]]);
				n1 = Model.randInt(node1.phi[obs[1]]);
				
				value = 0.0;
			}
			
			while (!stack.isEmpty()) {
				Elem e = stack.pop();
				value += e.v;
				Node node0 = pol0.getNode(e.h, e.n0);
				Node node1 = pol1.getNode(e.h, e.n1);
				
				int count = node0.getStateCount(e.n1, e.s);
				double tmp = (node0.getValue(e.n1, e.s)*count + value)/(count + 1);
				
				node0.setValue(e.n1, e.s, tmp);
				node0.setStateCount(e.n1, e.s, count + 1);
				node1.setValue(e.n0, e.s, tmp);
				node1.setStateCount(e.n0, e.s, count + 1);
				
				if (im) {
					EvaluationCount += 1;
				}
			}
		}
	}
	
	public void evalByWeight(final int h, Policy pol0, Policy pol1) {
		Node[] node0 = pol0.layerList[h].nodeList;
		Node[] node1 = pol1.layerList[h].nodeList;
		Node[] node0_ = null, node1_ = null;
		if (h > 0) {
			node0_ = pol0.layerList[h-1].nodeList;
			node1_ = pol1.layerList[h-1].nodeList;
		}
		
		for (int q0 = 0; q0 < node0.length; ++q0) {
			node0[q0].reset();
		}
		for (int q1 = 0; q1 < node1.length; ++q1) {
			node1[q1].reset();
		}
		
		int maxStates = DecTBDP.MAX_STATE_EVA;
		int[][][] stateList = new int[node0.length][node1.length][maxStates];
		for (int q0 = 0; q0 < node0.length; ++q0) {
			for (int q1 = 0; q1 < node1.length; ++q1) {
//				for (int s = 0; s < maxStates; ++s) {
//					stateList[q0][q1][s] = s;
//				}
				
				Belief belief = null;
				if (node0[q0].beliefs[q1] != null) {
					belief = node0[q0].beliefs[q1];
				} else if (node1[q1].beliefs[q0] != null) {
					belief = node1[q1].beliefs[q0];
				} else {
					belief = new Belief(model);
					if (node1[q1].beliefs[q1] != null) {
						belief.add(node1[q1].beliefs[q1]);
					}
					if (node0[q0].beliefs[q0] != null) {
						belief.add(node0[q0].beliefs[q0]);
					}
				}
				
				if (belief.nzList.size() > maxStates) {
					double curMax = Double.POSITIVE_INFINITY; 
					for (int n = 0; n < maxStates; ++n) {
						int state = 0;
						double tmpMax = Double.NEGATIVE_INFINITY;
						for (int s : belief.nzList) {
							double tmpVal = belief.stateProb[s];
							if (tmpVal < curMax-Belief.EPS) {
								if (tmpMax < tmpVal) {
									state = s;
									tmpMax = tmpVal;
								}
							}
						}
						curMax = tmpMax;
						stateList[q0][q1][n] = state;
					}
				} else {
					int state = -1;
					Walker.reset();
					Arrays.fill(stateList[q0][q1], -1);
					for (int n = 0; n < maxStates; ++n) {
						if (n < belief.nzList.size()) {
							state = belief.nzList.get(n);
						} else {
							state = Walker.next();
							if (state < 0) break;
						}
						
						stateList[q0][q1][n] = state;
						Walker.set(state);
					}
				}
			}
		}
		
		double tmpVal = 0.0, tmpVal_ = 0.0;
		Collection<Integer> list = null;
		Set<Integer> set = new HashSet<Integer>();
		for (int q0 = 0; q0 < node0.length; ++q0) {
			for (int q1 = 0; q1 < node1.length; ++q1) { 
				for (int k = 0; k < maxStates; ++k) {
					int s = stateList[q0][q1][k]; if (s < 0) continue;
				//for (int s = 0; s < model.numStates; ++s) {
					tmpVal = 0.0;
					for (int a0 : node0[q0].actList) {
					//for (int a0 = 0; a0 < model.numAct0; ++a0) {
						for (int a1 : node1[q1].actList) {
						//for (int a1 = 0; a1 < model.numAct1; ++a1) {
							tmpVal += node0[q0].psi[a0] * node1[q1].psi[a1] * model.getReward(s, a0, a1);
							if (h > 0) {
								for (int o0 = 0; o0 < model.numObs0; ++o0) {
									for (int o1 = 0; o1 < model.numObs1; ++o1) {
										if (model.nsList != null) {
											list = model.nsList[s][a0][a1][o0][o1];
										}
										if (list == null || list.isEmpty()) {
											continue;
										}
										for (int s_ : list) {
											tmpVal_ = model.getTrans(s, a0, a1, s_) * model.getObs(o0, o1, s, a0, a1, s_);
											for (int q0_ = 0; q0_ < node0_.length; ++q0_) {
												for (int q1_ = 0; q1_ < node1_.length; ++q1_) {
													if (node0_[q0_].mapsValue[q1_] == null || !node0_[q0_].mapsValue[q1_].containsKey(s_)) continue;
													tmpVal += tmpVal_ * node0[q0].eta[a0][o0][q0_] * node1[q1].eta[a1][o1][q1_] * node0_[q0_].getValue(q1_, s_);
												}
											}
										}
									}
								}
							}
						}
					}
					node0[q0].setValue(q1, s, tmpVal);
					node1[q1].setValue(q0, s, tmpVal);
				}
			}
		}
	}

	// TODO: order the state with the weight of each component of the
	// tested belief, and choose the top-N state and only evaluate this
	// state. If there is less state, sampling it by heuristic and get them.
	// TODO: for special problem with less coordination, it is possible to
	// only evaluate non-coordinated state of each agent and the coordinated state
	// to produce the value function of all.
	public void evaluate(final int h, Policy pol0, Policy pol1) {
		Node[] node0 = pol0.layerList[h].nodeList;
		Node[] node1 = pol1.layerList[h].nodeList;
		Node[] node0_ = null, node1_ = null;
		if (h > 0) {
			node0_ = pol0.layerList[h-1].nodeList;
			node1_ = pol1.layerList[h-1].nodeList;
		}
		
		for (int q0 = 0; q0 < node0.length; ++q0) {
			if (node0[q0].stateValue == null) {
				node0[q0].stateValue = new double[node1.length][model.numStates];
			}
		}
		for (int q1 = 0; q1 < node1.length; ++q1) {
			if (node1[q1].stateValue == null) {
				node1[q1].stateValue = new double[node0.length][model.numStates];
			}
		}
		
		double tmpVal = 0.0, tmpVal_ = 0.0;
		Collection<Integer> list = null;
		Set<Integer> set = new HashSet<Integer>();
		for (int q0 = 0; q0 < node0.length; ++q0) {
			for (int q1 = 0; q1 < node1.length; ++q1) { 
				for (int s = 0; s < model.numStates; ++s) {
					tmpVal = 0.0;
					for (int a0 : node0[q0].actList) {
					//for (int a0 = 0; a0 < model.numAct0; ++a0) {
						for (int a1 : node1[q1].actList) {
						//for (int a1 = 0; a1 < model.numAct1; ++a1) {
							tmpVal += node0[q0].psi[a0] * node1[q1].psi[a1] * model.getReward(s, a0, a1);
							if (h > 0) {
								for (int o0 = 0; o0 < model.numObs0; ++o0) {
									for (int o1 = 0; o1 < model.numObs1; ++o1) {
										if (model.nsList != null) {
											list = model.nsList[s][a0][a1][o0][o1];
										}
										if (list == null || list.isEmpty()) {
											continue;
										}
										for (int s_ : list) {
										//for (int s_ = 0; s_ < model.numStates; ++s_) {
											tmpVal_ = model.getTrans(s, a0, a1, s_) * model.getObs(o0, o1, s, a0, a1, s_);
											for (int q0_ = 0; q0_ < node0_.length; ++q0_) {
												for (int q1_ = 0; q1_ < node1_.length; ++q1_) {
													tmpVal += tmpVal_ * node0[q0].eta[a0][o0][q0_] * node1[q1].eta[a1][o1][q1_] * node0_[q0_].stateValue[q1_][s_];
												}
											}
										}
									}
								}
							}
						}
					}
					node0[q0].stateValue[q1][s] = tmpVal;
					node1[q1].stateValue[q0][s] = tmpVal;
				}
			}
		}
	}
	
	public void runLinearProgramming(int h, Node node0, Node node1, final Belief belief, double rate) {
		final int MAX_STEP = 20;
		final double LP_EPS = 1e-4;
		
//		node0.genRandNode();
//		node1.genRandNode();
		
		int ncount = 0;
		double eps = 0.0;
		do {
			eps = 0.0;
			for (int i = 0; i < 2; ++i) {
				eps += solveLinearProgramming(h, i, node0, node1, belief, rate);
			}
		} while (ncount++ < MAX_STEP && eps > LP_EPS);
	}
	
	public double solveLinearProgramming(int h, int ag, Node node0, Node node1, Belief belief, double rate) {
		assert (belief.nzList != null);
		double epsilon = Double.POSITIVE_INFINITY;
		Node node = null, node_ = null;
		if (ag == 0) {
			node = node0;
			node_ = node1;
		} else {
			node = node1;
			node_ = node0;
		}	
		
		int[] n;
		int numVars;
		double[] x, solution;
		if (h > 0) {
			n = Linear.n2;
			x = Linear.x2;			
			numVars = Linear.var2;
			solution = Linear.so2;
		} else {
			n = Linear.n1;
			x = Linear.x1;
			numVars = Linear.var1;
			solution = Linear.so1;
		}
		
		double totalVal = 0.0D;
		int index = 0;
		
		try {
			// LPSolve model
			LpSolve lp = LpSolve.makeLp(0, numVars);
			
			if (lp.getLp() != 0) 
			{
				lp.setAddRowmode(true);

				/////////////////////////////////
				// Probability constraints
				/////////////////////////////////
				Arrays.fill(x, 0.0D);
				for (int a = 0; a < node.psi.length; ++a) {
					x[a] = 1.0;
				}
				lp.addConstraintex(numVars, x, n, LpSolve.EQ, 1.0D);
				
				if (h > 0) {
					index = node.psi.length;
					for (int a = 0; a < node.eta.length; ++a) {
						for (int o = 0; o < node.eta[a].length; ++o) {
							Arrays.fill(x, 0.0D);
							for (int q = 0; q < node.eta[a][o].length; ++q) {
								x[index++] = 1.0;
							}
							x[a] = -1.0;
							lp.addConstraintex(numVars, x, n, LpSolve.EQ, 0.0D);
						}
					}
					assert (index == numVars - 1);
				}
				
//				for (int i = 0; i < numVars - 1; ++i) {
//					lp.setBounds(n[i], 0.0, 1.0);
//				}
				
				///////////////////////////////////
				// Improvement constraints
				//////////////////////////////////
				Arrays.fill(x, 0.0D);
				x[numVars - 1] = -1.0;
				totalVal = 0.0;
				index = 0;
				for (int a = 0; a < node.psi.length; ++a) {
					double param = 0.0;
					for (int a_ : node_.actList) {
					//for (int a_ = 0; a_ < node_.psi.length; ++a_) {
						int a0 = 0, a1 = 0;
						if (ag == 0) {
							a0 = a; a1 = a_;
						} else {
							a0 = a_; a1 = a;
						}
						double tmp = 0.0;
						for (int s : belief.nzList) {
						// for (int s = 0; s < model.numStates; ++s) {
							tmp += belief.stateProb[s] * model.getReward(s, a0, a1);
						}
						param += tmp * node_.psi[a_];
					}
					x[index++] = param;
					totalVal += param * node.psi[a];
				}
				assert (index == model.numAct0);
				if (h > 0) {
					Policy pol = (ag == 0) ? policyAgent1 : policyAgent0;
					int a0 = 0, a1 = 0, o0 = 0, o1 = 0;
					Collection<Integer> list = null;
					Set<Integer> set = new HashSet<Integer>();
					double param, tmp, tmp_;
					for (int a = 0; a < node.eta.length; ++a) {
						for (int o = 0; o < node.eta[a].length; ++o) {						
							for (int q = 0; q < node.eta[a][o].length; ++q) {
								param = 0.0;
								for (int s : belief.nzList) {
									tmp = 0.0;
									for (int a_ : node_.actList) {
									//for (int a_ = 0; a_ < node_.eta.length; ++a_) {
										for (int o_ = 0; o_ < node_.eta[a_].length; ++o_) {
											if (ag == 0) {
												o0 = o; a0 = a;
												o1 = o_; a1 = a_;
											} else {
												o0 = o_; a0 = a_;
												o1 = o; a1 = a;
											}
											if (model.nsList != null) {
												list = model.nsList[s][a0][a1][o0][o1];
											}
											
											if (list == null || list.isEmpty()) {
												continue;
											}
											for (int s_ : list) {
												tmp_ = model.getTrans(s, a0, a1, s_) * model.getObs(o0, o1, s, a0, a1, s_);
												for (int q_ = 0; q_ < node_.eta[a_][o_].length; ++q_) {
													if (node_.eta[a_][o_][q_] <= Node.EPS) continue;
													if (EVA_TYPE == 2) {
														if (ag == 0) {
															evalBySim(h-1, q, q_, s_, true);
														} else {
															evalBySim(h-1, q_, q, s_, true);
														}
													}
													tmp += tmp_ * node_.eta[a_][o_][q_] * pol.layerList[h-1].nodeList[q_].getValue(q, s_); //.stateValue[q][s_];
												}
											}
										}
									}
									param += tmp * belief.stateProb[s];
								}
								x[index++] = param;
								totalVal += param * node.eta[a][o][q];
							}
						}
					}
					assert (index == numVars - 1);
				}
				totalVal -= 1e-6; //
				lp.addConstraintex(numVars, x, n, LpSolve.GE, totalVal);
				
				//////////////////////////////
				// Objective function
				//////////////////////////////
				lp.setAddRowmode(false);
				Arrays.fill(x, 0.0D);
				x[numVars - 1] = 1.0D;
				lp.setObjFnex(numVars, x, n);
				lp.setMaxim();
				
				lp.setVerbose(LpSolve.IMPORTANT);
//				lp.writeLp("model.lp");
				int ret = lp.solve();
				
				if (ret == LpSolve.OPTIMAL)
				{
					/////////////////////////////
					// Update the policy
					////////////////////////////
					lp.getVariables(solution);
//					epsilon = lp.getObjective();
					epsilon = solution[numVars - 1];
					
					index = 0;
					for (int a = 0; a < node.psi.length; ++a) {
						node.psi[a] += rate * (solution[index++] - node.psi[a]); 
					}
					assert (index == model.numAct0);
					if (h > 0) {
						for (int a = 0; a < node.eta.length; ++a) {
							for (int o = 0; o < node.eta[a].length; ++o) {
								for (int q = 0; q < node.eta[a][o].length; ++q) {
									node.eta[a][o][q] = solution[index++];
								}
							}
						}
						assert (index == numVars - 1);
						
						double[][] phi_ = new double[node.phi.length][node.phi[0].length];
						for (int o = 0; o < phi_.length; ++o) {
							for (int q = 0; q < phi_[o].length; ++q) {
								phi_[o][q] = 0.0;
								for (int a = 0; a < node.psi.length; ++a) {
									phi_[o][q] += node.eta[a][o][q];
								}
							}
						}
						
						for (int a = 0; a < node.eta.length; ++a) {
							for (int o = 0; o < node.eta[a].length; ++o) {
								for (int q = 0; q < node.eta[a][o].length; ++q) {
									node.eta[a][o][q] = node.psi[a] * (node.phi[o][q] + rate * (phi_[o][q] - node.phi[o][q]));
								}
							}
						}
						node.phi = phi_;
					}
					node.update();
				}
				else
				{
//					String info;
					switch (ret)
					{
					case LpSolve.UNBOUNDED:
//						info = "unbounded";
						log.warning("LpSolve: Unbounded");
						break;
					case LpSolve.INFEASIBLE:
						epsilon = 0.0;
//						info = "infeasible";
						log.warning("LpSolve: Infeasible");
						break;
					case LpSolve.NOMEMORY:
					case LpSolve.SUBOPTIMAL:
					case LpSolve.DEGENERATE:
					case LpSolve.NUMFAILURE:
					case LpSolve.USERABORT:
					case LpSolve.TIMEOUT:		
					default:
						epsilon = 0.0;
//						info = "unknown";
						log.warning("LpSolve: Unknown");
					}
//					lp.writeLp(info+".lp");
				}
				
				lp.deleteLp();
			}
		} 
		catch (LpSolveException e) {
			e.printStackTrace();
		}
		return epsilon;
	}
	
	static class Linear {
		static int[] n1, n2;
		static int var1, var2;
		static double[] x1, x2, so1, so2;
		
		static void init(Model m) {
			var1 = m.numAct0 + 1;
			var2 = var1 + m.numAct0 * m.numObs0 * Layer.MAX_NODES;
			
			n1 = new int[var1];
			for (int i = 0; i < var1; ++i) {
				n1[i] = i + 1;
			}
			n2 = new int[var2];
			for (int i = 0; i < var2; ++i) {
				n2[i] = i + 1;
			}
			
			x1 = new double[var1];
			x2 = new double[var2];
			so1 = new double[var1];
			so2 = new double[var2];
		}
	}
	
	static class Walker {
		static int size;
		static int[] list, index;
		
		static void init(Model m) {
			list = new int[m.numStates];
			index = new int[m.numStates];
			for (int i = 0; i < m.numStates; ++i) {
				list[i] = i;
				index[i] = i;
			}
			size = list.length;
		}
		
		static void reset() {
			size = list.length;
		}
		
		static void set(int i) {
			if (index[i] < size) {
				int j = list[size-1];
				list[size-1] = list[index[i]];
				list[index[i]] = j;
				
				index[j] = index[i];
				index[i] = size-1;
				
				--size;
			}
		}
		
		static int next() {
			if (size <= 0) {
				return -1;
			} else {
				int i = Model.rand.nextInt(size);
				return list[i];
			}
		}
	}
}
