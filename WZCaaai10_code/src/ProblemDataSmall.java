/**
 * DEC-POMDP benchmarks written by Dan Bernstein, Chris Amato, Sven Seuken, etc.
 *
 * @author RBRlab
 *
 */
public class ProblemDataSmall {

	//trans[s][a1][a2][s']
	//rew[agent][s][a1][a2]
	public double[][][][]		trans, rew;

	//obs[o1][o2][s][a1][a2][s']
	public double[][][][][][]	obs;
	public double[]				startDist;

	//state Table for BoxPushing problem
	public static class stateTable {
		public int	agent1Position;
		public int	agent1Orientation;
		public int	agent2Position;
		public int	agent2Orientation;
	}

	public stateTable[] st;

	//observation Table for BoxPushing problem
	public static class observationTable {
		public int	observation1;
		public int	observation2;
	}

	observationTable[]	ot;

	int					north	= 0;

	int					east	= 1;

	int					south	= 2;

	int					west	= 3;

	public static void checkSumToOne(double trans[][][][], double obs[][][][][][]) {

		// check that trans probs sum to one
		double count;
		for (int s = 0; s < trans.length; s++) {
			for (int a1 = 0; a1 < trans[s].length; a1++) {
				for (int a2 = 0; a2 < trans[s][a1].length; a2++) {
					count = 0;
					for (int s_ = 0; s_ < trans[s][a1][a2].length; s_++) {
						count += trans[s][a1][a2][s_];
						if (false && trans[s][a1][a2][s_] > 0) {
							System.out.println(
									"trans[" + s + "][" + a1 + "][" + a2 + "][" + s_ + "]=" + trans[s][a1][a2][s_]);
						}
					}
					if (count > 1.01 || count < 0.99) {
						System.out.println("DONT SUM TO ONE STATE! " + count + " " + s + " " + a1 + " " + a2);
						//                  System.exit(1);
					}
				}
			}
		}

		// check that obs probs sum to one
		for (int s = 0; s < obs[0][0].length; s++) {
			for (int a1 = 0; a1 < obs[0][0][0].length; a1++) {
				for (int a2 = 0; a2 < obs[0][0][0][0].length; a2++) {
					for (int s_ = 0; s_ < obs[0][0][0][0][0].length; s_++) {
						count = 0;
						for (int o1 = 0; o1 < obs.length; o1++) {
							for (int o2 = 0; o2 < obs[0].length; o2++) {
								count += obs[o1][o2][s][a1][a2][s_];
								if (false && obs[o1][o2][s][a1][a2][s_] > 0) {
									System.out.println("obs[" + o1 + "][" + o2 + "]" + s + "][" + a1 + "][" + a2 + "]["
											+ s_ + "]=" + obs[o1][o2][s][a1][a2][s_]);
								}
							}
						}
						if (count > 1.01 || count < 0.99) {
							System.out.println("DONT SUM TO ONE OBS! " + count + " s=" + s + " a1=" + a1 + " a2=" + a2
									+ " s_=" + s_);
							//					System.exit(1);
						}
					}
				}
			}
		}
	}

	public ProblemDataSmall(int problem) {

		switch (problem) {
		case 0:
			BoxDomainSimplified boxes = new BoxDomainSimplified("D:\\Dropbox\\IMAPproject\\IMAP\\Benchmarks\\MiniBoxes\\B2\\B1.txt");
			//BoxDomainSimplified boxes = new BoxDomainSimplified("C:\\Users\\Sagi\\Dropbox\\IMAPproject\\IMAP\\Benchmarks\\MiniBoxes\\B2\\B2.txt");
			boxes.GetDecPOMDPDomain1D(this);
			break;
		
		case 9:

			//            	DEC-MDP recycling robot

			/**
			 * THIS WAS THE VERSION ACTUALLY USED IN PAPERS
			 */

			System.out.println("My DEC-MDP Recycling Robot Problem...");

			int numStates = 2 * 2;
			int numObs = 2;
			int numAct = 3;

			int numStatesEach = 2;

			//actions, 0=recharge, 1=small cans, 2=large cans

			/*The problem can be described as follows:
			 * Each robot chooses to search for the big item, search for the little item,
			 * wait or recharge.  The state is the status of the battery power for each agent
			 * (but could also include the presence of a big item... maybe there is always a small
			 * item present, but only sometimes a big one -- I want to keep the state space small,
			 * but retain a need to some cooperation)
			 * Searching for the big item increases the chance of depleting the battery, and if both
			 * robots get the big item (at the same time?) then the reward is higher
			 * Searching for the little item depletes the battery a smaller amount and never fails
			 * (get rid of Waiting?) Waiting does not deplete the battery, but does not provide a reward either (why would I ever wait?)
			 * Recharging recharges the battery, but does not provide a reward (do this instead of waiting)
			 *
			 */

			//states for each agent is 0=high, 1=low
			//so overall 0=both high, 1=

			startDist = new double[numStates];
			startDist[0] = 1.0;
			startDist[1] = 0.0;
			startDist[2] = 0.0;
			startDist[3] = 0.0;

			//actions for each agent are: searchbig, searchlittle, wait and recharge
			//states (the battery status) are high and low for each agent
			//rewards

			//trans[s][a1][a2][s_]

			//these are idependent so trans[s1,s2][a1][a2][s1,s2]=trans[s1][a1][s1]*trans[s2][a2][s2]
			double[][][] transEach = new double[numStatesEach][numAct][numStatesEach];

			//the prob of staying on high charge after a search for the big item
			double alphaBig = 0.5;
			//the prob of staying on high charge after a search for the little item
			double alphaSmall = 0.7;
			//the prob of depleting the battery after a search for the big item
			double betaBig = 0.3;
			//the prob of depleting the battery after a search for the small item
			double betaSmall = 0.2;

			//P(s'|a, s)

			//trans[s][a][s_]

			//searching for the big thing has a higher prob of depleting battery
			//retaining a high battery level after each action
			transEach[0][2][0] = alphaBig;
			transEach[0][1][0] = alphaSmall;
			transEach[0][0][0] = 1.0;

			//transitioning from the high to the low battery level after each action
			transEach[0][2][1] = (1 - alphaBig);
			transEach[0][1][1] = (1 - alphaSmall);
			transEach[0][0][1] = 0.0;

			//transitioning from low to high on searching means the battery ran out
			transEach[1][2][0] = betaBig;
			transEach[1][1][0] = betaSmall;
			transEach[1][0][0] = 1.0;

			transEach[1][2][1] = (1 - betaBig);
			transEach[1][1][1] = (1 - betaSmall);
			transEach[1][0][1] = 0.0;

			trans = new double[numStates][numAct][numAct][numStates];
			System.out.println("trans");
			for (int s1 = 0; s1 < numStatesEach; s1++) {
				for (int s2 = 0; s2 < numStatesEach; s2++) {
					for (int a1 = 0; a1 < numAct; a1++) {
						for (int a2 = 0; a2 < numAct; a2++) {
							for (int s1_ = 0; s1_ < numStatesEach; s1_++) {
								for (int s2_ = 0; s2_ < numStatesEach; s2_++) {
									int s = s1 * numStatesEach + s2;
									int s_ = s1_ * numStatesEach + s2_;
									//  transition function: P[STATES][ACTIONS][ACTIONS][STATES]
									trans[s][a1][a2][s_] = transEach[s1][a1][s1_] * transEach[s2][a2][s2_];

								}
							}
						}
					}
				}
			}

			//observations are independent too

			//obs give state for agent so 0=high and 1=low

			//P(o|s, a, s')

			double[][][][] obsEach = new double[numObs][numStates][numAct][numStates];

			//this isn't very efficient
			for (int s = 0; s < numStatesEach; s++) {
				for (int a = 0; a < numAct; a++) {
					for (int s_ = 0; s_ < numStatesEach; s_++) {
						for (int o = 0; o < numObs; o++) {
							if (o == s_) {
								obsEach[o][s][a][s_] = 1.0;
							}
						}
					}
				}
			}

			//obs[o1][o2][s][a1][a2][s_]
			obs = new double[numObs][numObs][numStates][numAct][numAct][numStates];
			System.out.println("obs");
			for (int o1 = 0; o1 < numObs; o1++) {
				for (int o2 = 0; o2 < numObs; o2++) {
					for (int s1 = 0; s1 < numStatesEach; s1++) {
						for (int s2 = 0; s2 < numStatesEach; s2++) {
							for (int a1 = 0; a1 < numAct; a1++) {
								for (int a2 = 0; a2 < numAct; a2++) {
									for (int s1_ = 0; s1_ < numStatesEach; s1_++) {
										for (int s2_ = 0; s2_ < numStatesEach; s2_++) {
											int s = s1 * numStatesEach + s2;
											int s_ = s1_ * numStatesEach + s2_;
											obs[o1][o2][s][a1][a2][s_] = obsEach[o1][s1][a1][s1_]
													* obsEach[o2][s2][a2][s2_];
										}
									}
								}
							}
						}
					}
				}
			}

			double dep = -10;
			//only get big when both search for it in the same step
			double big = 5;
			//each agent that searches for the small will get that reward (so twice for both searching for small)
			double small = 2;
			//the reward when depending on the following state as well
			double[][][][][] rewDep = new double[2][numStates][numAct][numAct][numStates];

			//both searching for the big item gets a big reward
			for (int agent = 0; agent < 2; agent++) {
				//consider all states since the ones in which a battery is depleted will be overwritten below
				for (int s = 0; s < numStates; s++) {
					for (int s_ = 0; s_ < numStates; s_++) {
						rewDep[agent][s][2][2][s_] = big;
						rewDep[agent][s][2][1][s_] = small;
						rewDep[agent][s][1][2][s_] = small;
						rewDep[agent][s][1][1][s_] = 2 * small;
						rewDep[agent][s][1][0][s_] = small;
						rewDep[agent][s][0][1][s_] = small;
					}
				}
			}

			//I have to remember that other state transitions like 1 to 2 and 2 to 1
			//mean that one agent has depleted the battery

			//depleting the battery during any action is bad (except recharging)
			for (int agent = 0; agent < 2; agent++) {
				for (int a1 = 0; a1 < numAct; a1++) {
					//recharge is 0
					if (a1 != 0) {
						for (int a2 = 0; a2 < numAct; a2++) {
							//neither recharged
							if (a2 != 0) {
								//both
								rewDep[agent][3][a1][a2][0] = 2 * dep;
								//agent 1
								rewDep[agent][3][a1][a2][1] = dep;
								//agent 2
								rewDep[agent][3][a1][a2][2] = dep;
								//agent 1
								rewDep[agent][2][a1][a2][0] = dep;
								//switch
								rewDep[agent][2][a1][a2][1] = dep;
								//agent 2
								rewDep[agent][1][a1][a2][0] = dep;
								//switch
								rewDep[agent][1][a1][a2][2] = dep;
							}
							//agent 2 recharged, agent 1 didn't
							else {
								//agent 2 was low
								rewDep[agent][3][a1][a2][0] = dep;
								//agent 2 was high
								rewDep[agent][2][a1][a2][0] = dep;
							}
						}
					}
					//agent 1 recharged
					else {
						for (int a2 = 0; a2 < numAct; a2++) {
							//agent 1 recharged, agent 2 didn't
							if (a2 != 0) {
								//agent 1 was low
								rewDep[agent][3][a1][a2][0] = dep;
								//agent 1 was high
								rewDep[agent][1][a1][a2][0] = dep;

							}
							//both recharged -- do nothing

						}
					}
				}
			}

			//rew[agent][state][act1][act2];
			rew = new double[2][numStates][numAct][numAct];

			//R(s, a1, a2)=/Sigma_{i=1}^numStates P(s'|s, a1, a2) * R(s, a1, a2)

			//rewards are symmetrical
			for (int agent = 0; agent < 2; agent++) {
				for (int s = 0; s < numStates; s++) {
					for (int a1 = 0; a1 < numAct; a1++) {
						for (int a2 = 0; a2 < numAct; a2++) {
							for (int s_ = 0; s_ < numStates; s_++) {
								rew[agent][s][a1][a2] += trans[s][a1][a2][s_] * rewDep[agent][s][a1][a2][s_];
							}
						}
					}
				}
			}

			break;

		/**
		 * broadcast (MABC)
		 */

		case 10:

			double[] startDist1 = new double[4];
			startDist1[0] = 1;
			startDist1[1] = 0;
			startDist1[2] = 0;
			startDist1[3] = 0;

			double[] startDist2 = new double[4];
			startDist2[0] = 0;
			startDist2[1] = 1;
			startDist2[2] = 0;
			startDist2[3] = 0;

			double[] startDist3 = new double[4];
			startDist3[0] = 0.25;
			startDist3[1] = 0.25;
			startDist3[2] = 0.25;
			startDist3[3] = 0.25;

			double[] startDist4 = new double[4];
			startDist4[0] = 0;
			startDist4[1] = 0;
			startDist4[2] = 1;
			startDist4[3] = 0;

			double[] startDist5 = new double[4];
			startDist5[0] = 0;
			startDist5[1] = 0;
			startDist5[2] = 0;
			startDist5[3] = 1;

			startDist = startDist2;

			trans = new double[4][2][2][4];
			for (int s = 0; s < trans.length; s++) {
				for (int a1 = 0; a1 < trans[0].length; a1++) {
					for (int a2 = 0; a2 < trans[0][0].length; a2++) {
						for (int s_ = 0; s_ < trans[0][0][0].length; s_++) {
							// the case where neither has something to send
							if (s == 0) {
								switch (s_) {
								case 0:
									trans[s][a1][a2][s_] = 0.09;
									break;
								case 1:
									trans[s][a1][a2][s_] = 0.81;
									break;
								case 2:
									trans[s][a1][a2][s_] = 0.01;
									break;
								case 3:
									trans[s][a1][a2][s_] = 0.09;
									break;
								}
							}
							// the case where only agent 1 has something to send
							if (s == 1) {
								if (a1 == 1) {
									switch (s_) {
									case 0:
										trans[s][a1][a2][s_] = 0.09;
										break;
									case 1:
										trans[s][a1][a2][s_] = 0.81;
										break;
									case 2:
										trans[s][a1][a2][s_] = 0.01;
										break;
									case 3:
										trans[s][a1][a2][s_] = 0.09;
										break;
									}
								} else {
									if (s_ == 1) {
										trans[s][a1][a2][s_] = 0.9;
									} else if (s_ == 3) {
										trans[s][a1][a2][s_] = 0.1;
									} else {
										trans[s][a1][a2][s_] = 0;
									}
								}
							}
							// the case where only agent 2 has something to send
							if (s == 2) {
								if (a2 == 1) {
									switch (s_) {
									case 0:
										trans[s][a1][a2][s_] = 0.09;
										break;
									case 1:
										trans[s][a1][a2][s_] = 0.81;
										break;
									case 2:
										trans[s][a1][a2][s_] = 0.01;
										break;
									case 3:
										trans[s][a1][a2][s_] = 0.09;
										break;
									case 4:
										trans[s][a1][a2][s_] = 0;
										break;
									}
								} else {
									if (s_ == 2) {
										trans[s][a1][a2][s_] = 0.1;
									} else if (s_ == 3) {
										trans[s][a1][a2][s_] = 0.9;
									} else {
										trans[s][a1][a2][s_] = 0;
									}
								}
							}
							// the case where they both have something to send
							if (s == 3) {
								if (a1 == a2) {
									if (s_ == 3) {
										trans[s][a1][a2][s_] = 1;
									} else {
										trans[s][a1][a2][s_] = 0;
									}
								} else if (a1 == 1) {
									if (s_ == 2) {
										trans[s][a1][a2][s_] = 0.1;
									} else if (s_ == 3) {
										trans[s][a1][a2][s_] = 0.9;
									} else {
										trans[s][a1][a2][s_] = 0;
									}
								} else {
									if (s_ == 1) {
										trans[s][a1][a2][s_] = 0.9;
									} else if (s_ == 3) {
										trans[s][a1][a2][s_] = 0.1;
									} else {
										trans[s][a1][a2][s_] = 0;
									}
								}
							}
						}
					}
				}
			}
			obs = new double[5][5][4][2][2][4];
			// observation rules:
			// 0 = no buffer, no send
			// 1 = no buffer, send
			// 2 = buffer, no send
			// 3 = buffer, send
			// 4 = buffer, collision
			boolean send1, send2;
			int sendstatus;
			for (int o1 = 0; o1 < obs.length; o1++) {
				for (int o2 = 0; o2 < obs[0].length; o2++) {
					for (int s = 0; s < obs[0][0].length; s++) {
						for (int a1 = 0; a1 < obs[0][0][0].length; a1++) {
							for (int a2 = 0; a2 < obs[0][0][0][0].length; a2++) {
								for (int s_ = 0; s_ < obs[0][0][0][0][0].length; s_++) {
									// did node 1 actually attempt to broadcast with a full buffer?
									if ((s == 1 || s == 3) && (a1 == 1)) {
										send1 = true;
									} else {
										send1 = false;
									}
									// did node 2 actually attempt to broadcast with a full buffer?
									if ((s == 2 || s == 3) && (a2 == 1)) {
										send2 = true;
									} else {
										send2 = false;
									}

									// was there no bradcast, a successful broadcast, or a collision?
									if (!send1 && !send2) {
										sendstatus = 0;
									} else if (send1 && send2) {
										sendstatus = 2;
									} else {
										sendstatus = 1;
									}
									if ((sendstatus == 0) && (s_ == 0)) {
										if ((o1 == 0) && (o2 == 0)) {
											obs[o1][o2][s][a1][a2][s_] = 1;
										}
									} else if ((sendstatus == 0) && (s_ == 1)) {
										if ((o1 == 2) && (o2 == 0)) {
											obs[o1][o2][s][a1][a2][s_] = 1;
										}
									} else if ((sendstatus == 0) && (s_ == 2)) {
										if ((o1 == 0) && (o2 == 2)) {
											obs[o1][o2][s][a1][a2][s_] = 1;
										}
									} else if ((sendstatus == 0) && (s_ == 3)) {
										if ((o1 == 2) && (o2 == 2)) {
											obs[o1][o2][s][a1][a2][s_] = 1;
										}
									} else if ((sendstatus == 1) && (s_ == 0)) {
										if ((o1 == 1) && (o2 == 1)) {
											obs[o1][o2][s][a1][a2][s_] = 1;
										}
									} else if ((sendstatus == 1) && (s_ == 1)) {
										if ((o1 == 3) && (o2 == 1)) {
											obs[o1][o2][s][a1][a2][s_] = 1;
										}
									} else if ((sendstatus == 1) && (s_ == 2)) {
										if ((o1 == 1) && (o2 == 3)) {
											obs[o1][o2][s][a1][a2][s_] = 1;
										}
									} else if ((sendstatus == 1) && (s_ == 3)) {
										if ((o1 == 3) && (o2 == 3)) {
											obs[o1][o2][s][a1][a2][s_] = 1;
										}
									} else if (sendstatus == 2) {
										if ((o1 == 4) && (o2 == 4)) {
											obs[o1][o2][s][a1][a2][s_] = 1;
										}
									} else {
										if ((o1 == 0) && (o2 == 0)) {
											obs[o1][o2][s][a1][a2][s_] = 1;
										}
									}
								}
							}
						}
					}
				}
			}

			rew = new double[2][4][2][2];
			for (int s = 0; s < rew[0].length; s++) {
				for (int a1 = 0; a1 < rew[0][0].length; a1++) {
					for (int a2 = 0; a2 < rew[0][0][0].length; a2++) {
						if ((s == 3 && a1 == 1 && a2 == 0) || (s == 1 && a1 == 1)) {
							rew[0][s][a1][a2] = 1.0;
							rew[1][s][a1][a2] = 1.0;
						} else if ((s == 3 && a1 == 0 && a2 == 1) || (s == 2 && a2 == 1)) {
							rew[0][s][a1][a2] = 1.0;
							rew[1][s][a1][a2] = 1.0;
						} else {
							rew[0][s][a1][a2] = 0;
							rew[1][s][a1][a2] = 0;
						}
					}
				}
			}

			break;

		/**
		 * Multiagent tiger (Tiger-A)
		 */
		case 22:

			//multiagent
			//Tiger-Problem-A
			startDist = new double[2];
			startDist[0] = 0.5;
			startDist[1] = 0.5;

			// STATES
			//   0: (hungry tiger, untold riches)
			//   1: (untold riches, hungry tiger)
			int m_states = 2;

			// ACTIONS
			//   0: openL
			//   1: openR
			//   2: listen
			int m_actions = 3;

			// OBSERVATIONS
			//   0: tigerL
			//   1: tigerR
			int m_observations = 2;

			// initial state
			//m_initialState = new ProbVector(m_states);
			//m_initialState.SetValueAt(0, 0.5);
			//m_initialState.SetValueAt(1, 0.5);

			// horizon
			// HAS TO BE SET FROM OUTSIDE!!

			// transition function: trans[STATES][ACTIONS][ACTIONS][STATES]
			trans = new double[m_states][m_actions][m_actions][m_states];
			for (int s = 0; s < m_states; s++) {
				for (int a1 = 0; a1 < m_actions; a1++) {
					for (int a2 = 0; a2 < m_actions; a2++) {
						for (int s_ = 0; s_ < m_states; s_++) {
							trans[s][a1][a2][s_] = 0.5;
						}
					}
				}
			}
			trans[0][2][2][0] = 1.0;
			trans[0][2][2][1] = 0.0;
			trans[1][2][2][0] = 0.0;
			trans[1][2][2][1] = 1.0;

			// newobservation function
			//   obs[Observation1][Observation2][State1][Action1][Action2][State2]

			// observation function
			//   obs[OBSERVATIONS][OBSERVATIONS][STATES][ACTIONS][ACTIONS][STATES]
			obs = new double[m_observations][m_observations][m_states][m_actions][m_actions][m_states];
			for (int s = 0; s < m_states; s++) {
				for (int a1 = 0; a1 < m_actions; a1++) {
					for (int a2 = 0; a2 < m_actions; a2++) {
						for (int o1 = 0; o1 < m_observations; o1++) {
							for (int o2 = 0; o2 < m_observations; o2++) {
								for (int s_ = 0; s_ < m_states; s_++) {
									obs[o1][o2][s][a1][a2][s_] = 0.25;
								}
							}
						}
					}
				}
			}
			//Tiger switched without somebody opening --> 0 probability
			obs[0][0][0][2][2][1] = 0.25;
			obs[1][0][0][2][2][1] = 0.25;
			obs[0][1][0][2][2][1] = 0.25;
			obs[1][1][0][2][2][1] = 0.25;
			obs[0][0][1][2][2][0] = 0.25;
			obs[1][0][1][2][2][0] = 0.25;
			obs[0][1][1][2][2][0] = 0.25;
			obs[1][1][1][2][2][0] = 0.25;

			//Tiger stayed, nobody openend, heard different noises
			//this is 0.85 for each agent hearing the correct obs and .15 wrong
			obs[0][0][0][2][2][0] = 0.7225;
			obs[1][0][0][2][2][0] = 0.1275;
			obs[0][1][0][2][2][0] = 0.1275;
			obs[1][1][0][2][2][0] = 0.0225;
			obs[0][0][1][2][2][1] = 0.0225;
			obs[1][0][1][2][2][1] = 0.1275;
			obs[0][1][1][2][2][1] = 0.1275;
			obs[1][1][1][2][2][1] = 0.7225;

			// reward function A
			//   rew[STATE][ACTION1][ACTION2]
			rew = new double[2][m_states][m_actions][m_actions];
			for (int agent = 0; agent < 2; agent++) {
				rew[agent][0][0][0] = -50;
				rew[agent][0][0][1] = -100;
				rew[agent][0][1][0] = -100;
				rew[agent][0][1][1] = 20;
				rew[agent][1][0][0] = 20;
				rew[agent][1][0][1] = -100;
				rew[agent][1][1][0] = -100;
				rew[agent][1][1][1] = -50;

				rew[agent][0][0][2] = -101;
				rew[agent][0][2][0] = -101;
				rew[agent][0][1][2] = 9;
				rew[agent][0][2][1] = 9;
				rew[agent][1][0][2] = 9;
				rew[agent][1][2][0] = 9;
				rew[agent][1][1][2] = -101;
				rew[agent][1][2][1] = -101;

				rew[agent][0][2][2] = -2;
				rew[agent][1][2][2] = -2;
			}

			break;

		// meeting in a grid  3by3 with 9 obs
		case 62:
			meetingGrid(3, false);
			//checkSumToOne(trans,obs);

			break;

		// meeting in a grid  3by3 with 9 obs, but must meet in the corners
		case 63:
			boolean corners = true;

			meetingGrid(3, corners);

			break;

		case 77:
			//original, deterministic mars rover
			marsRover(0);

			break;

		case 78:
			//mars rover with stochastic movements
			marsRover(1);

			break;

		/**
		 * box pushing problem (but may not be the same one that Sven used in
		 * his paper)
		 */
		case 99:

			startDist = new double[100];
			startDist[27] = 1.0;

			createStateTableForBoxPushing();
			createObservationTableForBoxPushing();
			//Supposedly the non-repeated version doesn't work
			InitBoxPushingRepeated();
			//   InitBoxPushing();

			break;

		}
	}

	private void createStateTableForBoxPushing() {

		//state table:
		st = new stateTable[100];
		for (int i = 0; i <= 99; i++) {
			st[i] = new stateTable();
		}
		//fill first column
		st[0].agent1Position = 1;
		//agent1Position for state 1 not well defined
		st[2].agent1Position = 1;
		st[3].agent1Position = 2;
		for (int i = 4; i <= 51; i++) {
			st[i].agent1Position = 1;
		}
		for (int i = 52; i <= 83; i++) {
			st[i].agent1Position = 2;
		}
		for (int i = 84; i <= 99; i++) {
			st[i].agent1Position = 3;
		}
		//fill second column
		//do we have to take care of states 0..3??
		for (int i = 4; i <= 15; i++) {
			st[i].agent1Orientation = north;
		}
		for (int i = 16; i <= 27; i++) {
			st[i].agent1Orientation = east;
		}
		for (int i = 28; i <= 39; i++) {
			st[i].agent1Orientation = south;
		}
		for (int i = 40; i <= 51; i++) {
			st[i].agent1Orientation = west;
		}
		for (int i = 52; i <= 59; i++) {
			st[i].agent1Orientation = north;
		}
		for (int i = 60; i <= 67; i++) {
			st[i].agent1Orientation = east;
		}
		for (int i = 68; i <= 75; i++) {
			st[i].agent1Orientation = south;
		}
		for (int i = 76; i <= 83; i++) {
			st[i].agent1Orientation = west;
		}
		for (int i = 84; i <= 87; i++) {
			st[i].agent1Orientation = north;
		}
		for (int i = 88; i <= 91; i++) {
			st[i].agent1Orientation = east;
		}
		for (int i = 92; i <= 95; i++) {
			st[i].agent1Orientation = south;
		}
		for (int i = 96; i <= 99; i++) {
			st[i].agent1Orientation = west;
		}
		//fill third column
		// agent2Position for state 0 not well defined!!
		st[1].agent2Position = 4;
		st[2].agent2Position = 4;
		st[3].agent2Position = 3;
		int a = 0;
		int currPos = 2;
		for (int i = 4; i <= 51; i++) {
			st[i].agent2Position = currPos;
			a++;
			if (a == 4) {
				a = 0;
				currPos += 1;
				if (currPos == 5) {
					currPos = 2;
				}
			}
		}
		a = 0;
		currPos = 3;
		for (int i = 52; i <= 83; i++) {
			st[i].agent2Position = currPos;
			a++;
			if (a == 4) {
				a = 0;
				currPos += 1;
				if (currPos == 5) {
					currPos = 3;
				}
			}
		}
		for (int i = 84; i <= 99; i++) {
			st[i].agent2Position = 4;
		}
		//fill fourth column
		//do we have to take care of states 0..3
		for (int i = 4; i <= 99; i++) {
			st[i].agent2Orientation = i % 4;
			//end of filling stateTable
		}
	}

	// OBSERVATIONS
	//   0: empty field
	//   1: wall
	//   2: other agent
	//   3: small box
	//   4: large box

	private void createObservationTableForBoxPushing() {
		ot = new observationTable[100];
		for (int i = 0; i <= 99; i++) {
			ot[i] = new observationTable();
		}
		for (int i = 0; i <= 3; i++) {
			ot[i].observation1 = 0;
			ot[i].observation2 = 0;
		}
		//fill first column
		for (int i = 4; i <= 15; i++) {
			ot[i].observation1 = 3;
		}
		for (int i = 16; i <= 19; i++) {
			ot[i].observation1 = 2;
		}
		for (int i = 20; i <= 27; i++) {
			ot[i].observation1 = 0;
		}
		for (int i = 28; i <= 39; i++) {
			ot[i].observation1 = 1;
		}
		for (int i = 40; i <= 51; i++) {
			ot[i].observation1 = 1;
		}
		for (int i = 52; i <= 59; i++) {
			ot[i].observation1 = 4;
		}
		for (int i = 60; i <= 63; i++) {
			ot[i].observation1 = 2;
		}
		for (int i = 64; i <= 67; i++) {
			ot[i].observation1 = 0;
		}
		for (int i = 68; i <= 75; i++) {
			ot[i].observation1 = 1;
		}
		for (int i = 76; i <= 83; i++) {
			ot[i].observation1 = 0;
		}
		for (int i = 84; i <= 87; i++) {
			ot[i].observation1 = 4;
		}
		for (int i = 88; i <= 91; i++) {
			ot[i].observation1 = 2;
		}
		for (int i = 92; i <= 95; i++) {
			ot[i].observation1 = 1;
		}
		for (int i = 96; i <= 99; i++) {
			ot[i].observation1 = 0;
		}
		//now fill rest of column 2
		for (int i = 4; i <= 99; i++) {
			if (st[i].agent2Orientation == north) {
				if (st[i].agent2Position == 2) {
					ot[i].observation2 = 4;
				}
				if (st[i].agent2Position == 3) {
					ot[i].observation2 = 4;
				}
				if (st[i].agent2Position == 4) {
					ot[i].observation2 = 3;
				}
			}
			if (st[i].agent2Orientation == east) {
				if (st[i].agent2Position == 2) {
					ot[i].observation2 = 0;
				}
				if (st[i].agent2Position == 3) {
					ot[i].observation2 = 0;
				}
				if (st[i].agent2Position == 4) {
					ot[i].observation2 = 1;
				}
			}
			if (st[i].agent2Orientation == south) {
				ot[i].observation2 = 1;
			}
			if (st[i].agent2Orientation == west) {
				if (st[i].agent2Position == 2) {
					ot[i].observation2 = 2;
				}
				if (st[i].agent2Position == 3 && st[i].agent1Position == 2) {
					ot[i].observation2 = 2;
				}
				if (st[i].agent2Position == 3 && st[i].agent1Position == 1) {
					ot[i].observation2 = 0;
				}
				if (st[i].agent2Position == 4 && st[i].agent1Position == 3) {
					ot[i].observation2 = 2;
				}
				if (st[i].agent2Position == 4 && st[i].agent1Position < 3) {
					ot[i].observation2 = 0;
				}
			}
		}
	}

	/***************************************************************************
	 * Beginning of BoxPushing Repeated
	 **************************************************************************/

	private void InitBoxPushingRepeated() {
		//define basic parameters of the problem here

		// Goal STATES
		//   0: only left box reached goal
		//   1: only right box reached goal
		//   2: both small boxes reached goal
		//   3: large box reached the goal

		// Other agent states (non-goal position, i.e. goal not reached!!)
		// 4..99

		// ACTIONS
		//   0: turn left
		//   1: turn right
		//   2: move forward
		//   3: stay

		// OBSERVATIONS
		//   0: empty field
		//   1: wall
		//   2: other agent
		//   3: small box
		//   4: large box

		// transition function: trans[STATES][ACTIONS][ACTIONS][STATES]
		trans = new double[100][4][4][100];

		for (int s = 0; s < trans.length; s++) {
			for (int a1 = 0; a1 < trans[0].length; a1++) {
				for (int a2 = 0; a2 < trans[0][0].length; a2++) {
					for (int s_ = 0; s_ < trans[0][0][0].length; s_++) {

						// the case where a goal state was reached before
						if (s <= 3) {
							//if (startStateSet.contains(new Integer(s_)))
							//    trans[s][a1][a2][s_] = 0.0625;
							if (s_ == 27) {
								trans[s][a1][a2][s_] = 1.0;
							} else {
								trans[s][a1][a2][s_] = 0.0;
							}
						}

						//no goal state was reached before
						//what if a goal state is supposed to be reached in the
						// next step
						else if (s > 3 && s_ <= 3) {
							if (st[s].agent1Orientation == north && st[s].agent1Position == 1 && a1 == 2) {
								if (st[s].agent2Orientation == north && st[s].agent2Position == 4 && a2 == 2) {
									if (s_ == 0) {
										trans[s][a1][a2][s_] = 0.09;
									} else if (s_ == 1) {
										trans[s][a1][a2][s_] = 0.09;
									} else if (s_ == 2) {
										trans[s][a1][a2][s_] = 0.81;
									} else if (s_ == 12) {
										trans[s][a1][a2][s_] = 0.01;
									} else {
										trans[s][a1][a2][s_] = 0.0;
									}
								} else if (s_ == 0) {
									trans[s][a1][a2][s_] = 0.9;
								} else if (s_ != 0 && a1inPlace(s, s_) && a2movementOK(s, a1, a2, s_)) {
									trans[s][a1][a2][s_] = 0.09;
								} else if (!a1movementOK(s, a1, a2, s_) && a1inPlace(s, s_)
										&& !a2movementOK(s, a1, a2, s_) && a2inPlace(s, s_)) {
									trans[s][a1][a2][s_] = 0.01;
								} else {
									trans[s][a1][a2][s_] = 0.0;
								}
							}
							if (st[s].agent2Orientation == north && st[s].agent2Position == 4 && a2 == 2) {
								if (st[s].agent1Orientation != north || st[s].agent1Position != 1 || a1 != 2) {
									if (s_ == 1) {
										trans[s][a1][a2][s_] = 0.9;
									} else if (s_ != 1 && a2inPlace(s, s_) && a1movementOK(s, a1, a2, s_)) {
										trans[s][a1][a2][s_] = 0.09;
									} else if (!a2movementOK(s, a1, a2, s_) && a2inPlace(s, s_)
											&& !a1movementOK(s, a1, a2, s_) && a1inPlace(s, s_)) {
										trans[s][a1][a2][s_] = 0.01;
									} else {
										trans[s][a1][a2][s_] = 0.0;
									}
								}
							}
							if (s == 52 && a1 == 2 && a2 == 2) {
								if (s_ == 3) {
									trans[s][a1][a2][s_] = 0.81;
								} else if (s_ == 52) {
									trans[s][a1][a2][s_] = 0.19;
								} else {
									trans[s][a1][a2][s_] = 0.0;
								}
							}
						}

						//now we assume that no goal state is supposed to be
						// reached in the next step
						else if (s > 3 && s_ > 3) {
							double transProb = 0;
							if (a1movementOK(s, a1, a2, s_) && a2movementOK(s, a1, a2, s_)) {
								transProb = 0.81;
							}
							if (a1movementOK(s, a1, a2, s_)
									//&& !a2movementOK(s, a1, a2, s_)
									&& a2inPlace(s, s_)) {
								transProb += 0.09;
							}
							if (//!a1movementOK(s, a1, a2, s_) &&
							a1inPlace(s, s_) && a2movementOK(s, a1, a2, s_)) {
								transProb += 0.09;
							}
							if (//!a1movementOK(s, a1, a2, s_) &&
							a1inPlace(s, s_)
									//&& !a2movementOK(s, a1, a2, s_)
									&& a2inPlace(s, s_)) {
								transProb += 0.01;
							}
							trans[s][a1][a2][s_] = transProb;

							//special cases
							if (s == 23 && a1 == 2 && a2 == 2) {
								if (s_ == 19) {
									trans[s][a1][a2][s_] = 0.09;
								}
								if (s_ == 23) {
									trans[s][a1][a2][s_] = 0.82;
								}
								if (s_ == 63) {
									trans[s][a1][a2][s_] = 0.09;
								}
							}
							if (s == 52 && a1 == 2 && a2 == 2) {
								if (s_ == 3) {
									trans[s][a1][a2][s_] = 0.81;
								} else if (s_ == 52) {
									trans[s][a1][a2][s_] = 0.19;
								} else {
									trans[s][a1][a2][s_] = 0.0;
								}
							}
							if (s == 67 && a1 == 2 && a2 == 2) {
								if (s_ == 63) {
									trans[s][a1][a2][s_] = 0.09;
								}
								if (s_ == 67) {
									trans[s][a1][a2][s_] = 0.82;
								}
								if (s_ == 90) {
									trans[s][a1][a2][s_] = 0.09;
								}
							}
						} else {
							trans[s][a1][a2][s_] = 0.0;
						}

					}
				}
			}
		}

		// observation function
		// obs[Observation1][Observation2][State1][Action1][Action2][State2]
		obs = new double[5][5][100][4][4][100];

		for (int o1 = 0; o1 < obs.length; o1++) {
			for (int o2 = 0; o2 < obs[0].length; o2++) {
				for (int s = 0; s < obs[0][0].length; s++) {
					for (int a1 = 0; a1 < obs[0][0][0].length; a1++) {
						for (int a2 = 0; a2 < obs[0][0][0][0].length; a2++) {
							for (int s_ = 0; s_ < obs[0][0][0][0][0].length; s_++) {
								if (ot[s_].observation1 == o1 && ot[s_].observation2 == o2) {
									obs[o1][o2][s][a1][a2][s_] = 1;
								} else {
									obs[o1][o2][s][a1][a2][s_] = 0.0;
								}
							}
						}
					}
				}
			}
		}

		// reward function
		// general instructions:
		// 1. the penalties for bumping into each other are collected separately
		// 2. each agent separately collects the penalty for moving the large
		// box alone
		// 3. each agent separately collects the reward for moving the large box
		// rew[STATE][ACTION1][ACTION2]
		rew = new double[2][100][4][4];

		for (int s = 0; s < rew[0].length; s++) {
			for (int a1 = 0; a1 < rew[0][0].length; a1++) {
				for (int a2 = 0; a2 < rew[0][0][0].length; a2++) {
					if (s < 4) {
						rew[0][s][a1][a2] = 0;
						rew[1][s][a1][a2] = 0;
					} else {
						double rewA1 = -0.1;
						double rewA2 = -0.1;
						// first determine reward for A1
						if (a1 != 2) {
							rewA1 += 0;
						} else {//from now on agent 1 moves forward
							if (s >= 4 && s <= 15) {
								rewA1 += 10;
							}
							if (s >= 16 && s <= 27) {
								rewA1 += 0;
							}
							if (s >= 28 && s <= 51) {
								rewA1 += -5;
							}
							if (s >= 52 && s <= 59) {
								if (s != 52 || a2 != 2) {
									rewA1 += -5;
								} else {
									rewA1 += 50;
								}
							}
							if (s >= 60 && s <= 67) {
								rewA1 += 0;
							}
							if (s >= 68 && s <= 75) {
								rewA1 += -5;
							}
							if (s >= 84 && s <= 87) {
								rewA1 += -5;
							}
							if (s >= 88 && s <= 91) {
								rewA1 += 0;
							}
							if (s >= 92 && s <= 95) {
								rewA1 += -5;
							}
						}
						//now determine reward for A2
						if (a2 != 2) {
							rewA2 += 0;
						} else {//from now on agent 2 moves forward
							if (s == 4) {
								rewA2 += -5;
							}
							if (s == 5) {
								rewA2 += 0;
							}
							if (s == 6) {
								rewA2 += -5;
							}
							if (s == 7) {
								rewA2 += 0;
							}
							if (s == 8) {
								rewA2 += -5;
							}
							if (s == 9) {
								rewA2 += 0;
							}
							if (s == 10) {
								rewA2 += -5;
							}
							if (s == 11) {
								rewA2 += 0;
							}
							if (s == 12) {
								rewA2 += 10;
							}
							if (s == 13) {
								rewA2 += -5;
							}
							if (s == 14) {
								rewA2 += -5;
							}
							if (s == 15) {
								rewA2 += 0;
							}
							if (s == 16) {
								rewA2 += -5;
							}
							if (s == 17) {
								rewA2 += 0;
							}
							if (s == 18) {
								rewA2 += -5;
							}
							if (s == 19) {
								rewA2 += 0;
							}
							if (s == 20) {
								rewA2 += -5;
							}
							if (s == 21) {
								rewA2 += 0;
							}
							if (s == 22) {
								rewA2 += -5;
							}
							if (s == 23) {
								rewA2 += 0;
							}
							if (s == 24) {
								rewA2 += 10;
							}
							if (s == 25) {
								rewA2 += -5;
							}
							if (s == 26) {
								rewA2 += -5;
							}
							if (s == 27) {
								rewA2 += 0;
							}
							if (s == 28) {
								rewA2 += -5;
							}
							if (s == 29) {
								rewA2 += 0;
							}
							if (s == 30) {
								rewA2 += -5;
							}
							if (s == 31) {
								rewA2 += 0;
							}
							if (s == 32) {
								rewA2 += -5;
							}
							if (s == 33) {
								rewA2 += 0;
							}
							if (s == 34) {
								rewA2 += -5;
							}
							if (s == 35) {
								rewA2 += 0;
							}
							if (s == 36) {
								rewA2 += 10;
							}
							if (s == 37) {
								rewA2 += -5;
							}
							if (s == 38) {
								rewA2 += -5;
							}
							if (s == 39) {
								rewA2 += 0;
							}
							if (s == 40) {
								rewA2 += -5;
							}
							if (s == 41) {
								rewA2 += 0;
							}
							if (s == 42) {
								rewA2 += -5;
							}
							if (s == 43) {
								rewA2 += 0;
							}
							if (s == 44) {
								rewA2 += -5;
							}
							if (s == 45) {
								rewA2 += 0;
							}
							if (s == 46) {
								rewA2 += -5;
							}
							if (s == 47) {
								rewA2 += 0;
							}
							if (s == 48) {
								rewA2 += 10;
							}
							if (s == 49) {
								rewA2 += -5;
							}
							if (s == 50) {
								rewA2 += -5;
							}
							if (s == 51) {
								rewA2 += 0;
							}
							if (s == 52) {
								if (a1 == 2) {
									rewA2 += 50;
								} else {
									rewA2 += -5;
								}
							}
							if (s == 53) {
								rewA2 += 0;
							}
							if (s == 54) {
								rewA2 += -5;
							}
							if (s == 55) {
								rewA2 += 0;
							}
							if (s == 56) {
								rewA2 += 10;
							}
							if (s == 57) {
								rewA2 += -5;
							}
							if (s == 58) {
								rewA2 += -5;
							}
							if (s == 59) {
								rewA2 += 0;
							}
							if (s == 60) {
								rewA2 += -5;
							}
							if (s == 61) {
								rewA2 += 0;
							}
							if (s == 62) {
								rewA2 += -5;
							}
							if (s == 63) {
								rewA2 += 0;
							}
							if (s == 64) {
								rewA2 += 10;
							}
							if (s == 65) {
								rewA2 += -5;
							}
							if (s == 66) {
								rewA2 += -5;
							}
							if (s == 67) {
								rewA2 += 0;
							}
							if (s == 68) {
								rewA2 += -5;
							}
							if (s == 69) {
								rewA2 += 0;
							}
							if (s == 70) {
								rewA2 += -5;
							}
							if (s == 71) {
								rewA2 += 0;
							}
							if (s == 72) {
								rewA2 += 10;
							}
							if (s == 73) {
								rewA2 += -5;
							}
							if (s == 74) {
								rewA2 += -5;
							}
							if (s == 75) {
								rewA2 += 0;
							}
							if (s == 76) {
								rewA2 += -5;
							}
							if (s == 77) {
								rewA2 += 0;
							}
							if (s == 78) {
								rewA2 += -5;
							}
							if (s == 79) {
								rewA2 += 0;
							}
							if (s == 80) {
								rewA2 += 10;
							}
							if (s == 81) {
								rewA2 += -5;
							}
							if (s == 82) {
								rewA2 += -5;
							}
							if (s == 83) {
								rewA2 += 0;
							}
							if (s == 84) {
								rewA2 += 10;
							}
							if (s == 85) {
								rewA2 += -5;
							}
							if (s == 86) {
								rewA2 += -5;
							}
							if (s == 87) {
								rewA2 += 0;
							}
							if (s == 88) {
								rewA2 += 10;
							}
							if (s == 89) {
								rewA2 += -5;
							}
							if (s == 90) {
								rewA2 += -5;
							}
							if (s == 91) {
								rewA2 += 0;
							}
							if (s == 92) {
								rewA2 += 10;
							}
							if (s == 93) {
								rewA2 += -5;
							}
							if (s == 94) {
								rewA2 += -5;
							}
							if (s == 95) {
								rewA2 += 0;
							}
							if (s == 96) {
								rewA2 += 10;
							}
							if (s == 97) {
								rewA2 += -5;
							}
							if (s == 98) {
								rewA2 += -5;
							}
							if (s == 99) {
								rewA2 += 0;
							}
						}
						//now determine penalty for bumping into each other
						double rewBoth = 0;
						//count penalty twice if both agents move into each
						// other simultaneously
						//first, agent1 bumping into agent2
						if (a1 != 2) {
							rewBoth += 0;
						} else {
							if (s >= 16 && s <= 19) {
								rewBoth += -5;
							}
							if (s >= 60 && s <= 63) {
								rewBoth += -5;
							}
							if (s >= 88 && s <= 91) {
								rewBoth += -5;
							}
						}
						//second, agent2 bumping into agent1
						if (a2 != 2) {
							rewBoth += 0;
						} else {
							if (s == 7 || s == 19 || s == 31 || s == 43) {
								rewBoth += -5;
							}
							if (s == 55 || s == 63 || s == 71 || s == 79) {
								rewBoth += -5;
							}
							if (s == 87 || s == 91 || s == 95 || s == 99) {
								rewBoth += -5;
							}
						}
						//third, both agents moving and then bumping into each
						// other
						if (a1 != 2 || a2 != 2) {
							rewBoth += 0;
						} else {
							if (s == 23 || s == 67) {
								rewBoth += -10;
							}
						}
						//now determine total reward

						rew[0][s][a1][a2] = rewA1 + rewA2 + rewBoth;
						rew[1][s][a1][a2] = rewA1 + rewA2 + rewBoth;
					}
				}
			}
		}

		//      new reward function #2 with additional succesor state
		// general instructions:
		// 1. the penalties for bumping into each other are collected separately
		// 2. each agent separately collects the penality for moving the large
		// box allone
		// 3. each agent separately collects the reward for moving the large box
		// rew2[STATE][ACTION1][ACTION2][STATE]
		double[][][][] rew2 = new double[100][4][4][100];

		for (int s = 0; s < rew2.length; s++) {
			for (int a1 = 0; a1 < rew2[0].length; a1++) {
				for (int a2 = 0; a2 < rew2[0][0].length; a2++) {
					for (int s_ = 0; s_ < rew2.length; s_++) {
						double rewA1 = -0.1;
						double rewA2 = -0.1;
						if (s == 48 && a1 == 2 && a2 == 2 && s_ == 1) {
							//      System.out.println("Problem?!");
						}
						if (s > 3)//i.e. agents can get rewards/penalties for
						// the next step
						{
							//first determine reward for A1
							if (a1 != 2) {
								rewA1 += 0;
							} else {//from now on agent 1 moves forward
								if (a1movementOK(s, a1, a2, s_)) {
									if (s >= 4 && s <= 15) {
										rewA1 += 10;
									}
									if (s >= 16 && s <= 27) {
										rewA1 += 0;
									}
									if (s >= 28 && s <= 51) {
										rewA1 += -5;
									}
									if (s >= 52 && s <= 59) {
										if (s != 52 || a2 != 2) {
											rewA1 += -5;
										} else {
											rewA1 += 50;
										}
									}
									if (s >= 60 && s <= 67) {
										rewA1 += 0;
									}
									if (s >= 68 && s <= 75) {
										rewA1 += -5;
									}
									if (s >= 84 && s <= 87) {
										rewA1 += -5;
									}
									if (s >= 88 && s <= 91) {
										rewA1 += 0;
									}
									if (s >= 92 && s <= 95) {
										rewA1 += -5;
									}
								} else {//a1movement not ok
									rewA1 += 0;
								}
							} //end of a1 moves forward

							//now determine reward for A2
							if (a2 != 2) {
								rewA2 += 0;
							} else {//from now on agent 2 moves forward
								if (a2movementOK(s, a1, a2, s_)) {
									if (s == 4) {
										rewA2 += -5;
									}
									if (s == 5) {
										rewA2 += 0;
									}
									if (s == 6) {
										rewA2 += -5;
									}
									if (s == 7) {
										rewA2 += 0;
									}
									if (s == 8) {
										rewA2 += -5;
									}
									if (s == 9) {
										rewA2 += 0;
									}
									if (s == 10) {
										rewA2 += -5;
									}
									if (s == 11) {
										rewA2 += 0;
									}
									if (s == 12) {
										rewA2 += 10;
									}
									if (s == 13) {
										rewA2 += -5;
									}
									if (s == 14) {
										rewA2 += -5;
									}
									if (s == 15) {
										rewA2 += 0;
									}
									if (s == 16) {
										rewA2 += -5;
									}
									if (s == 17) {
										rewA2 += 0;
									}
									if (s == 18) {
										rewA2 += -5;
									}
									if (s == 19) {
										rewA2 += 0;
									}
									if (s == 20) {
										rewA2 += -5;
									}
									if (s == 21) {
										rewA2 += 0;
									}
									if (s == 22) {
										rewA2 += -5;
									}
									if (s == 23) {
										rewA2 += 0;
									}
									if (s == 24) {
										rewA2 += 10;
									}
									if (s == 25) {
										rewA2 += -5;
									}
									if (s == 26) {
										rewA2 += -5;
									}
									if (s == 27) {
										rewA2 += 0;
									}
									if (s == 28) {
										rewA2 += -5;
									}
									if (s == 29) {
										rewA2 += 0;
									}
									if (s == 30) {
										rewA2 += -5;
									}
									if (s == 31) {
										rewA2 += 0;
									}
									if (s == 32) {
										rewA2 += -5;
									}
									if (s == 33) {
										rewA2 += 0;
									}
									if (s == 34) {
										rewA2 += -5;
									}
									if (s == 35) {
										rewA2 += 0;
									}
									if (s == 36) {
										rewA2 += 10;
									}
									if (s == 37) {
										rewA2 += -5;
									}
									if (s == 38) {
										rewA2 += -5;
									}
									if (s == 39) {
										rewA2 += 0;
									}
									if (s == 40) {
										rewA2 += -5;
									}
									if (s == 41) {
										rewA2 += 0;
									}
									if (s == 42) {
										rewA2 += -5;
									}
									if (s == 43) {
										rewA2 += 0;
									}
									if (s == 44) {
										rewA2 += -5;
									}
									if (s == 45) {
										rewA2 += 0;
									}
									if (s == 46) {
										rewA2 += -5;
									}
									if (s == 47) {
										rewA2 += 0;
									}
									if (s == 48) {
										rewA2 += 10;
									}
									if (s == 49) {
										rewA2 += -5;
									}
									if (s == 50) {
										rewA2 += -5;
									}
									if (s == 51) {
										rewA2 += 0;
									}
									if (s == 52) {
										if (a1 == 2) {
											rewA2 += 50;
										} else {
											rewA2 += -5;
										}
									}
									if (s == 53) {
										rewA2 += 0;
									}
									if (s == 54) {
										rewA2 += -5;
									}
									if (s == 55) {
										rewA2 += 0;
									}
									if (s == 56) {
										rewA2 += 10;
									}
									if (s == 57) {
										rewA2 += -5;
									}
									if (s == 58) {
										rewA2 += -5;
									}
									if (s == 59) {
										rewA2 += 0;
									}
									if (s == 60) {
										rewA2 += -5;
									}
									if (s == 61) {
										rewA2 += 0;
									}
									if (s == 62) {
										rewA2 += -5;
									}
									if (s == 63) {
										rewA2 += 0;
									}
									if (s == 64) {
										rewA2 += 10;
									}
									if (s == 65) {
										rewA2 += -5;
									}
									if (s == 66) {
										rewA2 += -5;
									}
									if (s == 67) {
										rewA2 += 0;
									}
									if (s == 68) {
										rewA2 += -5;
									}
									if (s == 69) {
										rewA2 += 0;
									}
									if (s == 70) {
										rewA2 += -5;
									}
									if (s == 71) {
										rewA2 += 0;
									}
									if (s == 72) {
										rewA2 += 10;
									}
									if (s == 73) {
										rewA2 += -5;
									}
									if (s == 74) {
										rewA2 += -5;
									}
									if (s == 75) {
										rewA2 += 0;
									}
									if (s == 76) {
										rewA2 += -5;
									}
									if (s == 77) {
										rewA2 += 0;
									}
									if (s == 78) {
										rewA2 += -5;
									}
									if (s == 79) {
										rewA2 += 0;
									}
									if (s == 80) {
										rewA2 += 10;
									}
									if (s == 81) {
										rewA2 += -5;
									}
									if (s == 82) {
										rewA2 += -5;
									}
									if (s == 83) {
										rewA2 += 0;
									}
									if (s == 84) {
										rewA2 += 10;
									}
									if (s == 85) {
										rewA2 += -5;
									}
									if (s == 86) {
										rewA2 += -5;
									}
									if (s == 87) {
										rewA2 += 0;
									}
									if (s == 88) {
										rewA2 += 10;
									}
									if (s == 89) {
										rewA2 += -5;
									}
									if (s == 90) {
										rewA2 += -5;
									}
									if (s == 91) {
										rewA2 += 0;
									}
									if (s == 92) {
										rewA2 += 10;
									}
									if (s == 93) {
										rewA2 += -5;
									}
									if (s == 94) {
										rewA2 += -5;
									}
									if (s == 95) {
										rewA2 += 0;
									}
									if (s == 96) {
										rewA2 += 10;
									}
									if (s == 97) {
										rewA2 += -5;
									}
									if (s == 98) {
										rewA2 += -5;
									}
									if (s == 99) {
										rewA2 += 0;
									}
								} else { //a2 movement not ok
									rewA2 += 0;
								}
							} //end of a2 moves forward
						} //end of (s > 3)

						//now determine penalty for bumping into each other
						double rewBoth = 0;
						//count penalty twice if both agents move into each
						// other simultaneously
						//first, agent1 bumping into agent2
						if (a1 != 2) {
							rewBoth += 0;
						} else {
							if (a1movementOK(s, a1, a2, s_)) {
								if (s >= 16 && s <= 19) {
									rewBoth += -5;
								}
								if (s >= 60 && s <= 63) {
									rewBoth += -5;
								}
								if (s >= 88 && s <= 91) {
									rewBoth += -5;
								}
							}
						}
						//second, agent2 bumping into agent1
						if (a2 != 2) {
							rewBoth += 0;
						} else {
							if (a2movementOK(s, a1, a2, s_)) {
								if (s == 7 || s == 19 || s == 31 || s == 43) {
									rewBoth += -5;
								}
								if (s == 55 || s == 63 || s == 71 || s == 79) {
									rewBoth += -5;
								}
								if (s == 87 || s == 91 || s == 95 || s == 99) {
									rewBoth += -5;
								}
							}
						}
						//third, both agents moving and then bumping into each
						// other
						if (a1 != 2 || a2 != 2) {
							rewBoth += 0;
						} else {
							if (s == 23 || s == 67) {
								if (a1movementOK(s, a1, a2, s_) && a2movementOK(s, a1, a2, s_)) {
									rewBoth += -10;
								}
							}
						}
						//now determine total reward
						rew2[s][a1][a2][s_] = rewA1 + rewA2 + rewBoth;
						if (trans[s][a1][a2][s_] == 0.0) {
							rew2[s][a1][a2][s_] = -100000;
						}
					}
				}
			}
		}

		//rew2 depends on start and end state, need to make rew that depends only on start state

		for (int s = 0; s < rew2.length; s++) {
			for (int a1 = 0; a1 < rew2[s].length; a1++) {
				for (int a2 = 0; a2 < rew2[s][a1].length; a2++) {
					rew[0][s][a1][a2] = 0;
					rew[1][s][a1][a2] = 0;
					for (int s_ = 0; s_ < rew2[s][a1][a2].length; s_++) {
						rew[0][s][a1][a2] += rew2[s][a1][a2][s_] * trans[s][a1][a2][s_];
						rew[1][s][a1][a2] += rew2[s][a1][a2][s_] * trans[s][a1][a2][s_];

					}
				}
			}
		}

		//begin test
		for (int s = 0; s < trans.length; s++) {
			for (int a1 = 0; a1 < trans[0].length; a1++) {
				for (int a2 = 0; a2 < trans[0][0].length; a2++) {
					double test = 0.0;
					for (int s_ = 0; s_ < trans[0][0][0].length; s_++) {
						test += trans[s][a1][a2][s_];
						if (s == -1 && a1 == 2 && a2 == 2) {
							System.out.println("State:" + s + ", A1: " + a1 + ", A2: " + a2 + ", S_ = " + s_
									+ ", Prob: " + trans[s][a1][a2][s_]);
						}

					}
					if (test != 1.0) {
						System.out.println("State:" + s + ", A1: " + a1 + ", A2: " + a2 + ", Prob: " + test);
					}
				}
			}
		}

		//end test

	}

	//helpers for box pushing
	//if a1movementOK == true this means that if agent 1's action was successful,
	// the agents can have reached successor state s_
	private boolean a1movementOK(int s, int a1, int a2, int s_) {
		boolean r = true;
		if (a1 == 0) {//turn left
			if (st[s_].agent1Orientation != ((st[s].agent1Orientation + 3) % 4)) {
				r = false;
			}
			if (st[s_].agent1Position != st[s].agent1Position) {
				r = false;
			}
			if (s_ == 0 || s_ == 2 || s_ == 3) {
				r = false;
			}
		}
		if (a1 == 1) {//turn right
			if (st[s_].agent1Orientation != ((st[s].agent1Orientation + 1) % 4)) {
				r = false;
			}
			if (st[s_].agent1Position != st[s].agent1Position) {
				r = false;
			}
			if (s_ == 0 || s_ == 2 || s_ == 3) {
				r = false;
			}
		}
		if (a1 == 3) {//stay in place
			if (st[s_].agent1Orientation != st[s].agent1Orientation) {
				r = false;
			}
			if (st[s_].agent1Position != st[s].agent1Position) {
				r = false;
			}
			if (s_ == 0 || s_ == 2 || s_ == 3) {
				r = false;
			}
		}
		if (a1 == 2) {//move forward
			if (st[s_].agent1Orientation != st[s].agent1Orientation) {
				r = false;
			}
			if (st[s].agent1Orientation == north) {
				if (st[s_].agent1Position != st[s].agent1Position) {
					r = false;
				}
				if (st[s].agent1Position == 1 && (s_ != 0 && s_ != 2)) {
					r = false;
				}
				if (s == 52 && a2 == 2 && s_ != 3) {
					r = false;
				} else if (st[s].agent1Position > 1 && s_ < 4) {
					r = false;
				}
				//special case for both agents pushing big box
				if (s == 52 && a2 == 2 && s_ == 3) {
					r = true;
				}
				//I need the following line for the case where a2 reaches the goal and I got a penalty
				if (s_ == 1 && st[s].agent1Position != 1) {
					r = true;
				}
			}
			if (st[s].agent1Orientation == south) {
				if (st[s_].agent1Position != st[s].agent1Position) {
					r = false;
				}
				if (s_ == 1) {
					r = true;
				}
			}
			if ((st[s].agent1Orientation == west) && (st[s].agent1Position == 1)) {
				if (st[s_].agent1Position != st[s].agent1Position) {
					r = false;
				}
				if (s_ == 1) {
					r = true;
				}
			}
			if ((st[s].agent1Orientation == west) && (st[s].agent1Position != 1)) {
				if (st[s_].agent1Position != st[s].agent1Position - 1) {
					r = false;
				}
				if (s_ == 1) {
					r = true;
				}
			}
			if ((st[s].agent1Orientation == east) && (st[s].agent2Position == st[s].agent1Position + 1)) {
				if (st[s_].agent1Position != st[s].agent1Position) {
					r = false;
				}
				if (s_ == 1) {
					r = true;
				}
			}
			if ((st[s].agent1Orientation == east) && (st[s].agent2Position > st[s].agent1Position + 1)) {
				if (st[s_].agent1Position != st[s].agent1Position + 1) {
					r = false;
				}
				if (s_ == 1) {
					r = true;
				}
			}
			//          check if both agents bumped into each other
			if (a2 == 2) {
				if (s == 23 || s == 67) {
					if (st[s_].agent1Position != st[s].agent1Position) {
						r = false;
					}
				}
			}
		}
		return r;
	}

	private boolean a2movementOK(int s, int a1, int a2, int s_) {
		boolean r = true;
		if (a2 == 0) {//turn left
			if (st[s_].agent2Orientation != ((st[s].agent2Orientation + 3) % 4)) {
				r = false;
			}
			if (st[s_].agent2Position != st[s].agent2Position) {
				r = false;
			}
			if (s_ == 1 || s_ == 2 || s_ == 3) {
				r = false;
			}
		}
		if (a2 == 1) {//turn right
			if (st[s_].agent2Orientation != ((st[s].agent2Orientation + 1) % 4)) {
				r = false;
			}
			if (st[s_].agent2Position != st[s].agent2Position) {
				r = false;
			}
			if (s_ == 1 || s_ == 2 || s_ == 3) {
				r = false;
			}
		}
		if (a2 == 3) {//stay in place
			if (st[s_].agent2Orientation != st[s].agent2Orientation) {
				r = false;
			}
			if (st[s_].agent2Position != st[s].agent2Position) {
				r = false;
			}
			if (s_ == 1 || s_ == 2 || s_ == 3) {
				r = false;
			}
		}
		if (a2 == 2) {//move forward
			//check for orientation for all cases
			if (st[s_].agent2Orientation != st[s].agent2Orientation) {
				r = false;
			}
			if (st[s].agent2Orientation == north) {
				if (st[s_].agent2Position != st[s].agent2Position) {
					r = false;
				}
				if (st[s].agent2Position == 4 && s_ != 1 && s_ != 2) {
					r = false;
				}
				if (s == 52 && a1 == 2 && s_ != 3) {
					r = false;
				} else if (st[s].agent2Position < 4 && s_ < 4) {
					r = false;
				}
				//special case for big box pushing
				if (s == 52 && a1 == 2 && s_ == 3) {
					r = true;
				}
				//get penalty even though agent1 reaches goal
				if (s_ == 0 && st[s].agent2Position != 4) {
					r = true;
				}
			}
			if (st[s].agent2Orientation == south) {
				if (st[s_].agent2Position != st[s].agent2Position) {
					r = false;
				}
				if (s_ == 0) {
					r = true;
				}
			}
			if ((st[s].agent2Orientation == east) && (st[s].agent2Position == 4)) {
				if (st[s_].agent2Position != st[s].agent2Position) {
					r = false;
				}
				if (s_ == 0) {
					r = true;
				}
			}
			if ((st[s].agent2Orientation == east) && (st[s].agent2Position != 4)) {
				if (st[s_].agent2Position != st[s].agent2Position + 1) {
					r = false;
				}
				if (s_ == 0) {
					r = true;
				}
			}
			if ((st[s].agent2Orientation == west) && (st[s].agent1Position == st[s].agent2Position - 1)) {
				if (st[s_].agent2Position != st[s].agent2Position) {
					r = false;
				}
				if (s_ == 0) {
					r = true;
				}
			}
			if ((st[s].agent2Orientation == west) && (st[s].agent1Position < st[s].agent2Position - 1)) {
				if (st[s_].agent2Position != st[s].agent2Position - 1) {
					r = false;
				}
				if (s_ == 0) {
					r = true;
				}
			}
			//check if both agents bumped into each other
			if (a1 == 2) {
				if (s == 23 || s == 67) {
					if (st[s_].agent2Position != st[s].agent2Position) {
						r = false;
					}
				}
			}
		}
		return r;
	}

	private boolean a1inPlace(int s, int s_) {
		boolean r = true;
		if ((st[s].agent1Orientation != st[s_].agent1Orientation) || (st[s].agent1Position != st[s_].agent1Position)) {
			r = false;
		}
		return r;
	}

	private boolean a2inPlace(int s, int s_) {
		boolean r = true;
		if ((st[s].agent2Orientation != st[s_].agent2Orientation) || (st[s].agent2Position != st[s_].agent2Position)) {
			r = false;
		}
		return r;
	}

	//2by2 grid with sites that have 2 conditions each
	//all this stuff is hard coded
	//type 0 is the original, fully observable with deterministic movements version
	//typ1 1 is still fully observable, but movements slip (stay in place) with a small prob
	private void marsRover(int type) {

		//actions: 0=up, 1=down, 2=left, 3=right, 4=drill, 5=sample
		int numAct = 6;

		int numSitesEach = 4;

		//int numCondSite=2;
		//conditions are bit strings so 0000= no site changed, where 0101= sites 0 and 2 sampled
		int numCondTotal = 16;

		int numStates = numSitesEach * numSitesEach * numCondTotal;

		//obs are location and if site is completed or not already
		//obs: s1_ if not sampled, numSitesEach+s1_ otherwise
		//obs: 0=s0, 1=s1, 2=s2, 3=s3
		//		4=s0,sampled, 5=s1,sampled, 6=s2,sampled, 7=s3,sampled
		int numObs = numSitesEach * 2;

		//states: 0=top left, 1=bottom left, 2=top right, 3=bottom right
		startDist = new double[numStates];
		//start with both agents in the top left with no sites sampled
		startDist[0] = 1.0;
		//prob move [s][a][s_]
		double[][][] transMove = new double[numSitesEach][4][numSitesEach];

		if (type == 0) {
			//starting in top left
			transMove[0][0][0] = 1.0;
			transMove[0][1][1] = 1.0;
			transMove[0][2][0] = 1.0;
			transMove[0][3][2] = 1.0;
			//starting in bottom left
			transMove[1][0][0] = 1.0;
			transMove[1][1][1] = 1.0;
			transMove[1][2][1] = 1.0;
			transMove[1][3][3] = 1.0;
			//starting in top right
			transMove[2][0][2] = 1.0;
			transMove[2][1][3] = 1.0;
			transMove[2][2][0] = 1.0;
			transMove[2][3][2] = 1.0;
			//starting in bottom right
			transMove[3][0][2] = 1.0;
			transMove[3][1][3] = 1.0;
			transMove[3][2][1] = 1.0;
			transMove[3][3][3] = 1.0;
		} else if (type == 1) {
			//starting in top left
			transMove[0][0][0] = 1.0;
			transMove[0][1][0] = 0.1;
			transMove[0][1][1] = 0.9;
			transMove[0][2][0] = 1.0;
			transMove[0][3][0] = 0.1;
			transMove[0][3][2] = 0.9;
			//starting in bottom left
			transMove[1][0][0] = 0.9;
			transMove[1][0][1] = 0.1;
			transMove[1][1][1] = 1.0;
			transMove[1][2][1] = 1.0;
			transMove[1][3][1] = 0.1;
			transMove[1][3][3] = 0.9;
			//starting in top right
			transMove[2][0][2] = 1.0;
			transMove[2][1][3] = 0.9;
			transMove[2][1][2] = 0.1;
			transMove[2][2][0] = 0.9;
			transMove[2][2][2] = 0.1;
			transMove[2][3][2] = 1.0;
			//starting in bottom right
			transMove[3][0][2] = 0.9;
			transMove[3][0][3] = 0.1;
			transMove[3][1][3] = 1.0;
			transMove[3][2][1] = 0.9;
			transMove[3][2][3] = 0.1;
			transMove[3][3][3] = 1.0;
		} else {
			throw new Error("I have only coded types 0 and 1 so far");
		}
		trans = new double[numStates][numAct][numAct][numStates];
		//neither agent drilled or sampled
		for (int a1 = 0; a1 < 4; a1++) {
			for (int a2 = 0; a2 < 4; a2++) {

				for (int s1 = 0; s1 < numSitesEach; s1++) {
					for (int s2 = 0; s2 < numSitesEach; s2++) {
						for (int cond = 0; cond < numCondTotal; cond++) {

							//condition doesn't change
							for (int s1_ = 0; s1_ < numSitesEach; s1_++) {
								for (int s2_ = 0; s2_ < numSitesEach; s2_++) {

									trans[s1 * numSitesEach * numCondTotal + s2 * numCondTotal + cond][a1][a2][s1_
											* numSitesEach * numCondTotal + s2_ * numCondTotal
											+ cond] = transMove[s1][a1][s1_] * transMove[s2][a2][s2_];//prob1[s1][a1][s1_]*prob1[s2][a2][s2_]

								}
							}
						}
					}
				}

			}
		}
		//agent one drilled or sampled, but agent 2 moved
		for (int a1 = 4; a1 < 6; a1++) {
			for (int a2 = 0; a2 < 4; a2++) {

				for (int s1 = 0; s1 < numSitesEach; s1++) {
					for (int s2 = 0; s2 < numSitesEach; s2++) {
						for (int cond = 0; cond < numCondTotal; cond++) {

							int cond_ = cond;
							//condition changes to sampled regardless of drilling or sampling for the current site

							String condBinary = Integer.toBinaryString(cond);
							condBinary = "000" + condBinary;

							if (condBinary.charAt(condBinary.length() - s1 - 1) == '1') {
								//don't do anything here
								//	System.out.println("char is 1");
							} else {
								//update the condition by switching the appropriate bit for the state
								cond_ = cond + (int) Math.pow(2, s1);
							}

							//if all the sites are sampled repeat the problem
							if (cond_ == 15) {
								trans[s1 * numSitesEach * numCondTotal + s2 * numCondTotal + cond][a1][a2][0] = 1.0;
							} else {
								int s1_ = s1;
								for (int s2_ = 0; s2_ < numSitesEach; s2_++) {
									if (transMove[s2][a2][s2_] > 1) {
										//				System.out.println("transMove["+s2+"]["+a2+"]["+s2_+"]="+transMove[s2][a2][s2_]);
										System.exit(1);
									}
									trans[s1 * numSitesEach * numCondTotal + s2 * numCondTotal + cond][a1][a2][s1_
											* numSitesEach * numCondTotal + s2_ * numCondTotal
											+ cond_] = transMove[s2][a2][s2_];//prob1[s1][a1][s1_]*prob1[s2][a2][s2_]

								}
							}

						}
					}
				}

			}
		}
		//agent one moved, but agent 2 drilled or sampled
		for (int a2 = 4; a2 < 6; a2++) {
			for (int a1 = 0; a1 < 4; a1++) {

				for (int s1 = 0; s1 < numSitesEach; s1++) {
					for (int s2 = 0; s2 < numSitesEach; s2++) {
						for (int cond = 0; cond < numCondTotal; cond++) {

							int cond_ = cond;
							//condition changes to sampled regardless of drilling or sampling for the current site

							String condBinary = Integer.toBinaryString(cond);
							condBinary = "000" + condBinary;

							if (condBinary.charAt(condBinary.length() - s2 - 1) == '1') {
								//don't do anything here
							} else {
								//update the condition by switching the appropriate bit for the state
								cond_ = cond + (int) Math.pow(2, s2);
							}

							//if all the sites are sampled repeat the problem
							if (cond_ == 15) {
								trans[s1 * numSitesEach * numCondTotal + s2 * numCondTotal + cond][a1][a2][0] = 1.0;
							} else {
								int s2_ = s2;
								for (int s1_ = 0; s1_ < numSitesEach; s1_++) {

									trans[s1 * numSitesEach * numCondTotal + s2 * numCondTotal + cond][a1][a2][s1_
											* numSitesEach * numCondTotal + s2_ * numCondTotal
											+ cond_] = transMove[s1][a1][s1_];//prob1[s1][a1][s1_]*prob1[s2][a2][s2_]
								}
							}
						}
					}
				}

			}
		}
		//neither agent moved
		for (int a1 = 4; a1 < 6; a1++) {
			for (int a2 = 4; a2 < 6; a2++) {

				for (int s1 = 0; s1 < numSitesEach; s1++) {
					for (int s2 = 0; s2 < numSitesEach; s2++) {
						for (int cond = 0; cond < numCondTotal; cond++) {

							int cond_ = cond;
							//first change condition as the result of one agent then go to the other
							//condition changes to sampled regardless of drilling or sampling for the current site

							String condBinary = Integer.toBinaryString(cond);
							condBinary = "000" + condBinary;

							if (condBinary.charAt(condBinary.length() - s2 - 1) == '1') {
								//don't do anything here
							} else {
								//update the condition by switching the appropriate bit for the state
								cond_ = cond + (int) Math.pow(2, s2);
							}

							condBinary = Integer.toBinaryString(cond_);
							condBinary = "000" + condBinary;
							if (condBinary.charAt(condBinary.length() - s1 - 1) == '1') {
								//don't do anything here
							} else {
								//update the condition by switching the appropriate bit for the state
								cond_ = cond_ + (int) Math.pow(2, s1);
							}

							//if all the sites are sampled repeat the problem
							if (cond_ == 15) {
								trans[s1 * numSitesEach * numCondTotal + s2 * numCondTotal + cond][a1][a2][0] = 1.0;
							} else {
								trans[s1 * numSitesEach * numCondTotal + s2 * numCondTotal + cond][a1][a2][s1
										* numSitesEach * numCondTotal + s2 * numCondTotal + cond_] = 1.0;//prob1[s1][a1][s1_]*prob1[s2][a2][s2_]
							}
						}
					}
				}

			}
		}

		obs = new double[numObs][numObs][numStates][numAct][numAct][numStates];

		//some of these observations are impossible because the given state s_ cannot be transitioned to from s by taking a
		//s doesn't matter - obs only depends on s_
		for (int s = 0; s < numStates; s++) {
			for (int a1 = 0; a1 < numAct; a1++) {
				for (int a2 = 0; a2 < numAct; a2++) {
					for (int s1_ = 0; s1_ < numSitesEach; s1_++) {
						for (int s2_ = 0; s2_ < numSitesEach; s2_++) {
							for (int cond_ = 0; cond_ < numCondTotal; cond_++) {
								//if the resulting state is sampled, adjust the obs acccordingly
								int o1 = s1_;
								if (bitIs1(cond_, s1_)) {
									o1 = numSitesEach + s1_;
								}
								int o2 = s2_;
								if (bitIs1(cond_, s2_)) {
									o2 = numSitesEach + s2_;
								}
								obs[o1][o2][s][a1][a2][s1_ * numSitesEach * numCondTotal + s2_ * numCondTotal
										+ cond_] = 1.0;
								//			O[STATES][ACTIONS][ACTIONS][OBSERVATIONS][OBSERVATIONS][STATES]

							}
						}
					}
				}
			}
		}

		//   rew[STATE][ACTION1][ACTION2]
		rew = new double[2][numStates][numAct][numAct];

		for (int a1 = 0; a1 < 6; a1++) {
			for (int a2 = 0; a2 < 6; a2++) {

				for (int s1 = 0; s1 < numSitesEach; s1++) {
					for (int s2 = 0; s2 < numSitesEach; s2++) {
						for (int cond = 0; cond < numCondTotal; cond++) {

							int s = s1 * numSitesEach * numCondTotal + s2 * numCondTotal + cond;
							//both agents moving - get a small negative reward (could add something more for hitting a wall)
							if (a1 < 4 && a2 < 4) {
								rew[0][s][a1][a2] = -0.2;
								rew[1][s][a1][a2] = -0.2;
							}
							//only agent one sampling or drilling
							else if (a1 >= 4 && a2 < 4) {
								//if state is already sampled get negative reward (plus movement penalty for other)
								if (bitIs1(cond, s1)) {
									rew[0][s][a1][a2] = -1.1;
									rew[1][s][a1][a2] = -1.1;
								}
								//if drill, the site is wrecked, so get bigger negative reward (plus movement penalty)
								else {
									if (a1 == 4) {
										rew[0][s][a1][a2] = -10.1;
										rew[1][s][a1][a2] = -10.1;
									}
									//if sample, get small positive reward (minus movement pen. for other)
									if (a1 == 5) {
										rew[0][s][a1][a2] = 1.9;
										rew[1][s][a1][a2] = 1.9;
									}
								}
							}
							//only agent two sampling or drilling
							else if (a2 >= 4 && a1 < 4) {
								//if state is already sampled get negative reward
								if (bitIs1(cond, s2)) {
									rew[0][s][a1][a2] = -1.1;
									rew[1][s][a1][a2] = -1.1;
								}
								//if drill, the site is wrecked, so get bigger negative reward
								else {
									if (a2 == 4) {
										rew[0][s][a1][a2] = -10.1;
										rew[1][s][a1][a2] = -10.1;
									}
									//if sample, get small positive reward
									if (a2 == 5) {
										rew[0][s][a1][a2] = 1.9;
										rew[1][s][a1][a2] = 1.9;
									}
								}
							}
							//both sampling or drilling
							else if (a2 >= 4 && a1 >= 4) {
								//if both agents at the same state which hasn't been sampled
								if (s1 == s2 && !bitIs1(cond, s1)) {
									//and drilling
									if (a1 == 4 && a2 == 4) {
										//if at a site that needs to be drilled get bigger positive
										if (s1 == 0 || s1 == 3) {
											rew[0][s][a1][a2] = 6;
											rew[1][s][a1][a2] = 6;
										}
										//else sample is wrecked so get negative
										else {
											rew[0][s][a1][a2] = -10;
											rew[1][s][a1][a2] = -10;
										}
									} else {
										//if either drills sample is wrecked so get negative
										if (a1 == 4 || a2 == 4) {
											rew[0][s][a1][a2] = -10;
											rew[1][s][a1][a2] = -10;
										} else {
											//if at a site that needs to be drilled get small positive
											if (s1 == 0 || s1 == 3) {
												rew[0][s][a1][a2] = 1;
												rew[1][s][a1][a2] = 1;
											}
											//else get the regular sampling reward
											else {
												rew[0][s][a1][a2] = 2;
												rew[1][s][a1][a2] = 2;
											}
										}
									}

								}
								//everything else is additive
								else {
									//if state of agent 1 is already sampled get negative reward
									if (bitIs1(cond, s1)) {
										rew[0][s][a1][a2] = -1;
										rew[1][s][a1][a2] = -1;

									} else {
										//not sampled alread and drilled -- bad
										if (a1 == 4) {
											rew[0][s][a1][a2] = -10;
											rew[1][s][a1][a2] = -10;
										} else {
											rew[0][s][a1][a2] = 2;
											rew[1][s][a1][a2] = 2;
										}
									}
									//if state of agent 2 is already sampled get negative reward (or make bigger)
									if (bitIs1(cond, s2)) {
										rew[0][s][a1][a2] += -1;
										rew[1][s][a1][a2] += -1;
									} else {
										//not sampled already and drilled -- bad
										if (a1 == 4) {
											rew[0][s][a1][a2] += -10;
											rew[1][s][a1][a2] += -10;
										} else {
											rew[0][s][a1][a2] += 2;
											rew[1][s][a1][a2] += 2;
										}
									}
								}
							}
						}
					}
				}
			}
		}

	}

	private boolean bitIs1(int cond, int si) {
		String condBinary = Integer.toBinaryString(cond);
		condBinary = "000" + condBinary;

		if (condBinary.charAt(condBinary.length() - si - 1) == '1') {
			return true;
		} else {
			return false;
		}
	}

	/**
	 *
	 *
	 * @param size
	 *            total size is size*size
	 * @param corners
	 *            whether the agents have to meet in the two corners or anywhere
	 */
	private void meetingGrid(int size, boolean corners) {

		//		   Actions: up down left right stay
		int numAct = 5;
		//obs start at the top left and then go to the right until the row is done
		//then they go to the next row and start from the left again
		int numObs = 4;
		if (size > 2) {
			numObs = 9;
		}
		//states are defined in the same way as the obs
		int numStatesEach = size * size;

		startDist = new double[numStatesEach * numStatesEach];

		//start with one agent in the bottom left and the other at the top right
		startDist[(size - 1) * numStatesEach + numStatesEach - size] = 1.0;

		System.out.println("start dist has state " + ((size - 1) * numStatesEach + numStatesEach - size) + "=1.0");
		double[][][] trans1 = new double[numStatesEach][numAct][numStatesEach];
		double[][][] trans2 = new double[numStatesEach][numAct][numStatesEach];

		//move in desired direction with prob 0.6
		//move in another direction or stay with prob 0.1

		for (int sE = 0; sE < numStatesEach; sE++) {
			for (int a = 0; a < numAct; a++) {
				trans1[sE][a] = new double[numStatesEach];
				trans2[sE][a] = new double[numStatesEach];
				//try to go in the direction of the action
				if (a == 4) {
					trans1[sE][a][sE] = 1.0;
					trans2[sE][a][sE] = 1.0;
				}
				//up
				if (a == 0) {

					trans1[sE][a][sE] = 0.1;
					trans2[sE][a][sE] = 0.1;

					//if state is in top row, can't go up
					if (sE < size) {
						trans1[sE][a][sE] += 0.6;
						trans2[sE][a][sE] += 0.6;
					} else {
						trans1[sE][a][sE - size] = 0.6;
						trans2[sE][a][sE - size] = 0.6;
					}
					//if on left side can't move left
					if (sE % size == 0) {
						trans1[sE][a][sE] += 0.1;
						trans2[sE][a][sE] += 0.1;
					} else {
						trans1[sE][a][sE - 1] = 0.1;
						trans2[sE][a][sE - 1] = 0.1;
					}
					//if on right side
					if (sE % size == (size - 1)) {
						trans1[sE][a][sE] += 0.1;
						trans2[sE][a][sE] += 0.1;
					} else {
						trans1[sE][a][sE + 1] = 0.1;
						trans2[sE][a][sE + 1] = 0.1;
					}
					//if on bottom
					if (sE + size >= numStatesEach) {
						trans1[sE][a][sE] += 0.1;
						trans2[sE][a][sE] += 0.1;
					} else {
						trans1[sE][a][sE + size] = 0.1;
						trans2[sE][a][sE + size] = 0.1;
					}

				}
				//down
				if (a == 1) {

					trans1[sE][a][sE] = 0.1;
					trans2[sE][a][sE] = 0.1;

					//if state is in top row
					if (sE < size) {
						trans1[sE][a][sE] += 0.1;
						trans2[sE][a][sE] += 0.1;
					} else {
						trans1[sE][a][sE - size] = 0.1;
						trans2[sE][a][sE - size] = 0.1;
					}
					//if on left side can't move left
					if (sE % size == 0) {
						trans1[sE][a][sE] += 0.1;
						trans2[sE][a][sE] += 0.1;
					} else {
						trans1[sE][a][sE - 1] = 0.1;
						trans2[sE][a][sE - 1] = 0.1;
					}
					//if on right side
					if (sE % size == (size - 1)) {
						trans1[sE][a][sE] += 0.1;
						trans2[sE][a][sE] += 0.1;
					} else {
						trans1[sE][a][sE + 1] = 0.1;
						trans2[sE][a][sE + 1] = 0.1;
					}
					//if on bottom
					if (sE + size >= numStatesEach) {
						trans1[sE][a][sE] += 0.6;
						trans2[sE][a][sE] += 0.6;
					} else {
						trans1[sE][a][sE + size] = 0.6;
						trans2[sE][a][sE + size] = 0.6;
					}
				}
				//left
				if (a == 2) {
					trans1[sE][a][sE] = 0.1;
					trans2[sE][a][sE] = 0.1;
					//if state is in top row
					if (sE < size) {
						trans1[sE][a][sE] += 0.1;
						trans2[sE][a][sE] += 0.1;
					} else {
						trans1[sE][a][sE - size] = 0.1;
						trans2[sE][a][sE - size] = 0.1;
					}
					//if on left side can't move left
					if (sE % size == 0) {
						trans1[sE][a][sE] += 0.6;
						trans2[sE][a][sE] += 0.6;
					} else {
						trans1[sE][a][sE - 1] = 0.6;
						trans2[sE][a][sE - 1] = 0.6;
					}
					//if on right side
					if (sE % size == (size - 1)) {
						trans1[sE][a][sE] += 0.1;
						trans2[sE][a][sE] += 0.1;
					} else {
						trans1[sE][a][sE + 1] = 0.1;
						trans2[sE][a][sE + 1] = 0.1;
					}
					//if on bottom
					if (sE + size >= numStatesEach) {
						trans1[sE][a][sE] += 0.1;
						trans2[sE][a][sE] += 0.1;
					} else {
						trans1[sE][a][sE + size] = 0.1;
						trans2[sE][a][sE + size] = 0.1;
					}
				}
				//right
				if (a == 3) {
					trans1[sE][a][sE] = 0.1;
					trans2[sE][a][sE] = 0.1;
					//if state is in top row
					if (sE < size) {
						trans1[sE][a][sE] += 0.1;
						trans2[sE][a][sE] += 0.1;
					} else {
						trans1[sE][a][sE - size] = 0.1;
						trans2[sE][a][sE - size] = 0.1;
					}
					//if on left side can't move left
					if (sE % size == 0) {
						trans1[sE][a][sE] += 0.1;
						trans2[sE][a][sE] += 0.1;
					} else {
						trans1[sE][a][sE - 1] = 0.1;
						trans2[sE][a][sE - 1] = 0.1;
					}
					//if on right side
					if (sE % size == (size - 1)) {
						trans1[sE][a][sE] += 0.6;
						trans2[sE][a][sE] += 0.6;
					} else {
						trans1[sE][a][sE + 1] = 0.6;
						trans2[sE][a][sE + 1] = 0.6;
					}
					//if on bottom
					if (sE + size >= numStatesEach) {
						trans1[sE][a][sE] += 0.1;
						trans2[sE][a][sE] += 0.1;
					} else {
						trans1[sE][a][sE + size] = 0.1;
						trans2[sE][a][sE + size] = 0.1;
					}
				}

			}
		}

		trans = new double[numStatesEach * numStatesEach][numAct][numAct][numStatesEach * numStatesEach];
		// transition function: P[STATES][ACTIONS][ACTIONS][STATES]
		for (int s1 = 0; s1 < numStatesEach; s1++) {
			for (int s2 = 0; s2 < numStatesEach; s2++) {
				int s = s1 * numStatesEach + s2;
				for (int a1 = 0; a1 < numAct; a1++) {
					for (int a2 = 0; a2 < numAct; a2++) {
						for (int s1_ = 0; s1_ < numStatesEach; s1_++) {
							for (int s2_ = 0; s2_ < numStatesEach; s2_++) {
								int s_ = s1_ * numStatesEach + s2_;

								trans[s][a1][a2][s_] = trans1[s1][a1][s1_] * trans2[s2][a2][s2_];
							}
						}
					}
				}
			}
		}

		//obs
		double[][][] obs1 = new double[numObs][numStatesEach][numAct];
		double[][][] obs2 = new double[numObs][numStatesEach][numAct];
		if (numStatesEach == numObs) {
			for (int o = 0; o < numObs; o++) {
				for (int sE_ = 0; sE_ < numStatesEach; sE_++) {
					for (int a = 0; a < numAct; a++) {
						if (o == sE_) {
							obs1[o][sE_][a] = 1.0;
							obs2[o][sE_][a] = 1.0;
						}
					}
				}
			}
		} else {
			for (int sE_ = 0; sE_ < numStatesEach; sE_++) {
				for (int a = 0; a < numAct; a++) {
					//top row
					if (sE_ < size) {
						//top left corner
						if (sE_ % size == 0) {
							obs1[0][sE_][a] = 1.0;
							obs2[0][sE_][a] = 1.0;
						}
						//top right corner
						else if (sE_ % size == 0) {
							obs1[2][sE_][a] = 1.0;
							obs2[2][sE_][a] = 1.0;
						}
						//top middle
						else {
							obs1[1][sE_][a] = 1.0;
							obs2[1][sE_][a] = 1.0;
						}
					}
					//if on bottom
					else if (sE_ + size >= numStatesEach) {
						//bottom left corner
						if (sE_ % size == 0) {
							obs1[6][sE_][a] = 1.0;
							obs2[6][sE_][a] = 1.0;
						}
						//bottom right corner
						else if (sE_ % size == 0) {
							obs1[8][sE_][a] = 1.0;
							obs2[8][sE_][a] = 1.0;
						}
						//bottom middle
						else {
							obs1[7][sE_][a] = 1.0;
							obs2[7][sE_][a] = 1.0;
						}

					}
					//if on left side
					else if (sE_ % size == 0) {
						obs1[3][sE_][a] = 1.0;
						obs2[3][sE_][a] = 1.0;
					}
					//if on right side
					else if (sE_ % size == (size - 1)) {
						obs1[5][sE_][a] = 1.0;
						obs2[5][sE_][a] = 1.0;
					} else {
						obs1[4][sE_][a] = 1.0;
						obs2[4][sE_][a] = 1.0;
					}
				}
			}
		}
		//   O[STATES][ACTIONS][ACTIONS][OBSERVATIONS][OBSERVATIONS][STATES]
		obs = new double[numObs][numObs][numStatesEach * numStatesEach][numAct][numAct][numStatesEach * numStatesEach];
		for (int o1 = 0; o1 < numObs; o1++) {
			for (int o2 = 0; o2 < numObs; o2++) {
				for (int s1 = 0; s1 < numStatesEach; s1++) {
					for (int s2 = 0; s2 < numStatesEach; s2++) {
						int s = s1 * numStatesEach + s2;
						for (int a1 = 0; a1 < numAct; a1++) {
							for (int a2 = 0; a2 < numAct; a2++) {
								for (int s1_ = 0; s1_ < numStatesEach; s1_++) {
									for (int s2_ = 0; s2_ < numStatesEach; s2_++) {
										int s_ = s1_ * numStatesEach + s2_;
										obs[o1][o2][s][a1][a2][s_] = obs1[o1][s1_][a1] * obs2[o2][s2_][a2];
									}
								}
							}
						}
					}
				}
			}
		}
		//rew
		//   R[STATES][ACTIONS][ACTIONS][STATES]
		rew = new double[2][numStatesEach * numStatesEach][numAct][numAct];
		for (int s1 = 0; s1 < numStatesEach; s1++) {
			for (int s2 = 0; s2 < numStatesEach; s2++) {
				int s = s1 * numStatesEach + s2;
				for (int a1 = 0; a1 < numAct; a1++) {
					for (int a2 = 0; a2 < numAct; a2++) {
						if (corners) {
							if (s1 == s2) {
								if (s1 == 0 || s1 == (numStatesEach - 1)) {

									rew[0][s][a1][a2] = 1.0;
									rew[1][s][a1][a2] = 1.0;
								}
							}
						}
						//in general case, get reward any time the agents are in the same state
						else if (s1 == s2) {

							rew[0][s][a1][a2] = 1.0;
							rew[1][s][a1][a2] = 1.0;

						}
					}
				}
			}
		}
	}
}
