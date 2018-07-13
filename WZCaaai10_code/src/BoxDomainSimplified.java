import java.io.*;
import java.util.*;

class BoxDomainSimplified
{
	public class Box
	{
		public int X, Y, Weight;
		public int ID;

		public Box(int iX, int iY, int iWeight, int iID)
		{
			X = iX;
			Y = iY;
			Weight = iWeight;
			ID = iID;
		}

		public boolean IsTargetLocation(Location l)
		{
			return l.X == X && l.Y == 1;
		}
	}

	public int Width ;
	public int Length ;
	public int Agents ;
	public List<Box> Boxes ;
	public String Name ;
	public int[][] Map ;

	public BoxDomainSimplified(String sBoxDescriptionFile)
	{
		try{
			ReadDescriptionFile(sBoxDescriptionFile);

			String sName = sBoxDescriptionFile.substring(sBoxDescriptionFile.lastIndexOf('\\') + 1);
			if (Agents == 0)
				Agents = 2;
			sName = sName.split(".")[0];
			Name = "Box-" + sName;
		}
		catch(Exception e){}
	}

	private void ReadDescriptionFile(String sBoxDescriptionFile) throws Exception
	{
		BufferedReader sr = new BufferedReader(new InputStreamReader(new FileInputStream(sBoxDescriptionFile)));
		String sSizeLine = sr.readLine();
		String[] asSizeLine = sSizeLine.split(",");
		Width = Integer.parseInt(asSizeLine[0].trim());
		Length = Integer.parseInt(asSizeLine[1].trim());

		//Location.Width = Width;
		//Location.Length = Length;

		Map = new int[Width][Length];
		Boxes = new LinkedList<Box>();
		for (int i = 0; i < Length; i++)
		{
			String sLine = sr.readLine();
			for (int j = 0; j < Width; j++)
			{
				if (sLine.charAt(j) == 'X')
					Map[j][i] = 0;
				if (sLine.charAt(j) == '1')
				{
					Map[j][i] = 1;
					Boxes.add(new Box(j + 1, i + 1, 1, Boxes.size()));
				}
				if (sLine.charAt(j) == '2')
				{
					Map[j][i] = 2;
					Boxes.add(new Box(j + 1, i + 1, 2, Boxes.size()));
				}
				if (sLine.charAt(j) == 'A')
				{
					Map[j][i] = -1;
					Agents++;
				}
			}
		}
	}



	public void WriteDecPOMDPDomain(String sOutputFile) throws Exception
	{
		PrintWriter swOutput = new PrintWriter(sOutputFile);
		swOutput.println("# " + Name);
		swOutput.println("agents: 2");
		swOutput.println("discount: 1");
		swOutput.println("values: reward");

		List<Location> lLocations = new LinkedList<Location>();
		for (int i = 1; i <= Width; i++)
			for (int j = 1; j <= Length; j++)
				lLocations.add(new Location(i, j));

		List<List<Location>> lAllStates = ComputeStates(0, lLocations);
		swOutput.print("states:");
		for (List<Location> l : lAllStates)
			swOutput.print(" " + GetState(l));
		swOutput.print(" deadend");
		swOutput.println();
		List<List<Location>> lStartStates = ComputeInitialStates(0);
		swOutput.print("start include:");
		for (List<Location> l : lStartStates)
			swOutput.print(" " + GetState(l));
		swOutput.println();

		swOutput.println("actions:");
		String sActions = "move-up move-down move-left move-right push-up push-down push-left push-right observe-box declare-goal";
		swOutput.println(sActions);
		swOutput.println(sActions);

		swOutput.println("observations:");
		String sObs = "null yes no";
		for (int iBox = 0; iBox < Boxes.size(); iBox++)
			sObs += " b" + (iBox + 1);
		swOutput.println(sObs);
		swOutput.println(sObs);

		int idx = 0;

		swOutput.println("T : * :");
		swOutput.println("identity");

		//swOutput.println();
		swOutput.println("O : * * : * : null null : 1.0");
		swOutput.println("R : * * : * : * : * : -1.0");

		for (List<Location> lStart : lAllStates)
		{
			//swOutput.println();

			idx++;
			if (idx % 100 == 0)
				System.out.print("\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b" + idx);

			boolean bGoalState = true;
			for (int iBox = 0; iBox < Boxes.size(); iBox++)
			{
				if (!Boxes.get(iBox).IsTargetLocation(lStart.get(Agents + iBox)))
					bGoalState = false;
			}



			for (String sAction1 : sActions.split(" "))
			{
				String[] aAction1 = sAction1.split("-");
				for (String sAction2 : sActions.split(" "))
				{
					String[] aAction2 = sAction2.split("-");

					if (bGoalState)
					{
						if (sAction1 == "declare-goal" && sAction2 == "declare-goal")
						{
							//swOutput.println("T: " + sAction1 + " " + sAction2 + " : " + GetState(lStart) + " : " + GetState(lStart) + " : 1.0");
							//swOutput.println("O: " + sAction1 + " " + sAction2 + " : " + GetState(lStart) + " : null null : 1.0");
							swOutput.println("R: " + sAction1 + " " + sAction2 + " : " + GetState(lStart) + " : * : * : 0.0");
							continue;
						}
					}
					else if (sAction1 == "declare-goal" || sAction2 == "declare-goal")
					{
						swOutput.println("T: " + sAction1 + " " + sAction2 + " : " + GetState(lStart) + " : deadend : 1.0");
						swOutput.println("T: " + sAction1 + " " + sAction2 + " : " + GetState(lStart) + " : " + GetState(lStart) + " : 0.0");
						//swOutput.println("O: " + sAction1 + " " + sAction2 + " : " + GetState(lStart) + " : null null : 1.0");
						//swOutput.println("R: " + sAction1 + " " + sAction2 + " : " + GetState(lStart) + " : * : * : -1.0");
						continue;
					}

					boolean bLegalPush = true;
					int iPushedBox1 = GetPushedBox(0, lStart);
					int iPushedBox2 = GetPushedBox(1, lStart);
					if (aAction1[0] == "push" && iPushedBox1 == -1)
					{
						bLegalPush = false;
					}
					else if (aAction1[0] != "push")
						iPushedBox1 = -1;
					if (aAction2[0] == "push" && iPushedBox2 == -1)
					{
						bLegalPush = false;
					}
					else if (aAction2[0] != "push")
						iPushedBox2 = -1;

					if (iPushedBox1 == iPushedBox2 && iPushedBox1 != -1)//joint push
					{
						if (aAction1[1] != aAction2[1])//not same direction
							bLegalPush = false;
					}
					List<Location> lEnd = null;
					double dReward = 0.0;

					if (bLegalPush)
					{
						lEnd = new LinkedList<Location>();
						lEnd.add(lStart.get(0).Move(aAction1[1]));//agent 1
						lEnd.add(lStart.get(1).Move(aAction2[1]));//agent 2

						for (int iBox = 0; iBox < Boxes.size(); iBox++)
						{
							if (iBox == iPushedBox1 && iPushedBox1 == iPushedBox2)//joint push
							{
								Location lDest = lStart.get(Agents + iBox).Move(aAction1[1]);
								if (Boxes.get(iBox).IsTargetLocation(lStart.get(Agents + iBox)))
									dReward = -1;
								else if (Boxes.get(iBox).IsTargetLocation(lDest))
									dReward = 1;
								lEnd.add(lDest);
							}
							else if (iBox == iPushedBox1 && Boxes.get(iBox).Weight == 1)
							{
								Location lDest = lStart.get(Agents + iBox).Move(aAction1[1]);
								if (Boxes.get(iBox).IsTargetLocation(lStart.get(Agents + iBox)))
									dReward = -1;
								else if (Boxes.get(iBox).IsTargetLocation(lDest))
									dReward = 1;
								lEnd.add(lDest);//agent 1
							}
							else if (iBox == iPushedBox2 && Boxes.get(iBox).Weight == 1)
							{
								Location lDest = lStart.get(Agents + iBox).Move(aAction2[1]);
								if (Boxes.get(iBox).IsTargetLocation(lStart.get(Agents + iBox)))
									dReward = -1;
								else if (Boxes.get(iBox).IsTargetLocation(lDest))
									dReward = 1;
								lEnd.add(lDest);//agent 2
							}
							else
								lEnd.add(lStart.get(Agents + iBox));
						}


					}

					String sObs1 = "null";
					if (aAction1[0] == "observe")
					{
						if (aAction1[1] == "box")
						{
							int iBox = GetPushedBox(0, lStart);
							if (iBox > -1)
								sObs1 = "b" + (iBox + 1);
						}
						if (aAction1[1] == "other")
						{
							if (lStart.get(0) == lStart.get(1))
								sObs1 = "yes";
							else
								sObs1 = "no";
						}
					}
					String sObs2 = "null";
					if (aAction2[0] == "observe")
					{
						if (aAction2[1] == "box")
						{
							int iBox = GetPushedBox(1, lStart);
							if (iBox > -1)
								sObs2 = "b" + (iBox + 1);
						}
						if (aAction2[1] == "other")
						{
							if (lStart.get(0) == lStart.get(1))
								sObs1 = "yes";
							else
								sObs1 = "no";
						}
					}

					dReward = -1;//for goal based model

					if (lEnd == null)
					{
						lEnd = lStart;
						dReward = -10;
					}

					if (!Same(lEnd, lStart))
					{
						swOutput.println("T: " + sAction1 + " " + sAction2 + " : " + GetState(lStart) + " : " + GetState(lEnd) + " : 1.0");
						swOutput.println("T: " + sAction1 + " " + sAction2 + " : " + GetState(lStart) + " : " + GetState(lStart) + " : 0.0");
					}

					if (sObs1 != "null" || sObs2 != "null")
					{
						swOutput.println("O: " + sAction1 + " " + sAction2 + " : " + GetState(lStart) + " : null null : 0.0");
						swOutput.println("O: " + sAction1 + " " + sAction2 + " : " + GetState(lStart) + " : " + sObs1 + " " + sObs2 + " : 1.0");
					}
					if (dReward != -1)
						swOutput.println("R: " + sAction1 + " " + sAction2 + " : " + GetState(lStart) + " : * : * : " + dReward);

				}
			}
		}

		//swOutput.println("T: * : deadend : deadend : 1.0");
		swOutput.println("O: * : deadend : null null : 1.0");
		swOutput.println("R: * : deadend : * : * : -1.0");
		//swOutput.println("R: " + sAction1 + " " + sAction2 + " : deadend : * : * : 0.0");

		swOutput.close();

	}

	public void GetDecPOMDPDomain1D(ProblemDataSmall model)
	{
		Map<String,Integer> dStates = new HashMap<String,Integer>();
		Map<String,Integer> dActions = new HashMap<String,Integer>();
		Map<String,Integer> dObservations = new HashMap<String,Integer>();


		List<Location> lLocations = new LinkedList<Location>();
		for (int i = 1; i <= Width; i++)
			for (int j = 1; j <= Length; j++)
				lLocations.add(new Location(i, j));

		List<List<Location>> lAllStates = ComputeStates(0, lLocations);
		List<List<Location>> lFilteredStates = new ArrayList<List<Location>>();
		for(List<Location> l : lAllStates){
			if(l.get(0).Y == 2 && l.get(1).Y == 2)
				lFilteredStates.add(l);
		}
		lAllStates = lFilteredStates;
		int cStates = lAllStates.size() + 1; //+1 for deadend
		int cActions = 10;
		int cObservations = 1 + Boxes.size();
		for(int i = 0 ; i < lAllStates.size(); i++)
			dStates.put(GetState(lAllStates.get(i)), i);
		dStates.put("deadend", cStates - 1);

		int i = 0;
		dObservations.put("null",0);
		for(Box b : Boxes){
			dObservations.put("b" + (b.ID + 1), dObservations.size());
			i++;
		}

		//set start state distribution
		List<List<Location>> lStartStates = ComputeInitialStates(0);
		List<Integer> lStartStateIndexes = new LinkedList<Integer>();
		for (List<Location> l : lStartStates){
			System.out.println(GetState(l));
			lStartStateIndexes.add(dStates.get(GetState(l)));
		}
		model.startDist = new double[cStates];
		for(i = 0 ; i < cStates ; i++){
			if(lStartStateIndexes.contains(i))
				model.startDist[i] = 1.0 / lStartStateIndexes.size();
			else
				model.startDist[i] = 0;
		}

		String sActions = "move-left move-right push-up push-down observe-box declare-goal";
		i = 0;
		for (String sAction : sActions.split(" "))
		{
			dActions.put(sAction, i);
			i++;
		}
		int idx = 0;

		model.trans = new double[cStates][cActions][cActions][cStates];
		model.rew = new double[Agents][cStates][cActions][cActions];
		model.obs = new double[cObservations][cObservations][cStates][cActions][cActions][cStates];


		for(int iState = 0 ; iState < cStates ; iState++){
			for(int iAction1 = 0 ; iAction1 < cActions ; iAction1++){
				for(int iAction2 = 0; iAction2 < cActions; iAction2++){
					model.trans[iState][iAction1][iAction2][iState] = 1.0;
					for(int iEndState = 0 ; iEndState < cStates ; iEndState++){
						model.obs[0][0][iState][iAction1][iAction2][iEndState] = 1.0;
					}
					for(i = 0 ; i < Agents ; i++)
						model.rew[i][iState][iAction1][iAction2] = -1.0;

				}
			}
		}

		int iDeadendState = dStates.get("deadend");

		for (List<Location> lStart : lAllStates)
		{
			//swOutput.println();
			int iStartState = dStates.get(GetState(lStart));

			idx++;
			if (idx % 100 == 0)
				System.out.print("\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b" + idx);

			boolean bGoalState = true;
			for (int iBox = 0; iBox < Boxes.size(); iBox++)
			{
				if (!Boxes.get(iBox).IsTargetLocation(lStart.get(Agents + iBox)))
					bGoalState = false;
			}
			//if(bGoalState)
			//	System.out.println("*");


			for (String sAction1 : sActions.split(" "))
			{
				int iAction1 = dActions.get(sAction1);
				String[] aAction1 = sAction1.split("-");
				for (String sAction2 : sActions.split(" "))
				{
					int iAction2 = dActions.get(sAction2);
					String[] aAction2 = sAction2.split("-");

					if (bGoalState)
					{
						if (sAction1.equals("declare-goal") && sAction2.equals("declare-goal"))
						{

							//swOutput.println("T: " + sAction1 + " " + sAction2 + " : " + GetState(lStart) + " : " + GetState(lStart) + " : 1.0");
							//swOutput.println("O: " + sAction1 + " " + sAction2 + " : " + GetState(lStart) + " : null null : 1.0");
							//swOutput.println("R: " + sAction1 + " " + sAction2 + " : " + GetState(lStart) + " : * : * : 0.0");


							for(i = 0 ; i < Agents ; i++)
								model.rew[i][iStartState][iAction1][iAction2] = 0.0;
							model.trans[iStartState][iAction1][iAction2][iStartState] = 1.0;
							model.obs[0][0][iStartState][iAction1][iAction2][iStartState] = 1.0;

							continue;
						}
					}
					else if (sAction1.equals("declare-goal") || sAction2.equals("declare-goal"))
					{
						//swOutput.println("T: " + sAction1 + " " + sAction2 + " : " + GetState(lStart) + " : deadend : 1.0");
						//swOutput.println("T: " + sAction1 + " " + sAction2 + " : " + GetState(lStart) + " : " + GetState(lStart) + " : 0.0");
						//swOutput.println("O: " + sAction1 + " " + sAction2 + " : " + GetState(lStart) + " : null null : 1.0");
						//swOutput.println("R: " + sAction1 + " " + sAction2 + " : " + GetState(lStart) + " : * : * : -1.0");

						for(i = 0 ; i < Agents ; i++)
							model.rew[i][iStartState][iAction1][iAction2] = -1.0;
						model.trans[iStartState][iAction1][iAction2][iStartState] = 0.0;
						model.trans[iStartState][iAction1][iAction2][iDeadendState] = 1.0;
						model.obs[0][0][iStartState][iAction1][iAction2][iDeadendState] = 1.0;


						continue;
					}

					//if(iStartState == 215 && aAction1[0].equals("push") && aAction2[0].equals("push"))
					//	System.out.println("*");

					boolean bLegalPush = true;
					int iPushedBox1 = GetPushedBox(0, lStart);
					int iPushedBox2 = GetPushedBox(1, lStart);
					if (aAction1[0].equals("push") && iPushedBox1 == -1)
					{
						bLegalPush = false;
					}
					else if (!aAction1[0].equals("push"))
						iPushedBox1 = -1;
					if (aAction2[0].equals("push") && iPushedBox2 == -1)
					{
						bLegalPush = false;
					}
					else if (!aAction2[0].equals("push"))
						iPushedBox2 = -1;

					if (iPushedBox1 == iPushedBox2 && iPushedBox1 != -1)//joint push
					{
						if (!aAction1[1].equals(aAction2[1]))//not same direction
							bLegalPush = false;
					}
					List<Location> lEnd = null;
					double dReward = 0.0;

					if (bLegalPush)
					{
						lEnd = new LinkedList<Location>();
						lEnd.add(lStart.get(0));//agent 1
						lEnd.add(lStart.get(1));//agent 2

						for (int iBox = 0; iBox < Boxes.size(); iBox++)
						{
							if (iBox == iPushedBox1 && iPushedBox1 == iPushedBox2)//joint push
							{
								Location lDest = lStart.get(Agents + iBox).Move(aAction1[1]);
								if (Boxes.get(iBox).IsTargetLocation(lStart.get(Agents + iBox)))
									dReward = -1;
								else if (Boxes.get(iBox).IsTargetLocation(lDest))
									dReward = 1;
								lEnd.add(lDest);
							}
							else if (iBox == iPushedBox1 && Boxes.get(iBox).Weight == 1)
							{
								Location lDest = lStart.get(Agents + iBox).Move(aAction1[1]);
								if (Boxes.get(iBox).IsTargetLocation(lStart.get(Agents + iBox)))
									dReward = -1;
								else if (Boxes.get(iBox).IsTargetLocation(lDest))
									dReward = 1;
								lEnd.add(lDest);//agent 1
							}
							else if (iBox == iPushedBox2 && Boxes.get(iBox).Weight == 1)
							{
								Location lDest = lStart.get(Agents + iBox).Move(aAction2[1]);
								if (Boxes.get(iBox).IsTargetLocation(lStart.get(Agents + iBox)))
									dReward = -1;
								else if (Boxes.get(iBox).IsTargetLocation(lDest))
									dReward = 1;
								lEnd.add(lDest);//agent 2
							}
							else
								lEnd.add(lStart.get(Agents + iBox));
						}


					}

					String sObs1 = "null";
					if (aAction1[0].equals("observe"))
					{
						if (aAction1[1].equals("box"))
						{
							int iBox = GetPushedBox(0, lStart);
							if (iBox > -1)
								sObs1 = "b" + (iBox + 1);
						}
						if (aAction1[1].equals("other"))
						{
							if (lStart.get(0) == lStart.get(1))
								sObs1 = "yes";
							else
								sObs1 = "no";
						}
					}
					String sObs2 = "null";
					if (aAction2[0].equals("observe"))
					{
						if (aAction2[1].equals("box"))
						{
							int iBox = GetPushedBox(1, lStart);
							if (iBox > -1)
								sObs2 = "b" + (iBox + 1);
						}
						if (aAction2[1].equals("other"))
						{
							if (lStart.get(0) == lStart.get(1))
								sObs1 = "yes";
							else
								sObs1 = "no";
						}
					}

					dReward = -1;//for goal based model

					if (lEnd == null)
					{
						lEnd = lStart;
						dReward = -10;
					}

					int iEndState = dStates.get(GetState(lEnd));

					//if (!Same(lEnd, lStart))
					{
						//swOutput.println("T: " + sAction1 + " " + sAction2 + " : " + GetState(lStart) + " : " + GetState(lEnd) + " : 1.0");
						//swOutput.println("T: " + sAction1 + " " + sAction2 + " : " + GetState(lStart) + " : " + GetState(lStart) + " : 0.0");
						model.trans[iStartState][iAction1][iAction2][iStartState] = 0.0;
						model.trans[iStartState][iAction1][iAction2][iEndState] = 1.0;
					}

					if (!sObs1.equals("null") || !sObs2.equals("null")) {
						try {
							int iObs1 = dObservations.get(sObs1);
							int iObs2 = dObservations.get(sObs2);
							//swOutput.println("O: " + sAction1 + " " + sAction2 + " : " + GetState(lStart) + " : null null : 0.0");
							//swOutput.println("O: " + sAction1 + " " + sAction2 + " : " + GetState(lStart) + " : " + sObs1 + " " + sObs2 + " : 1.0");
							model.obs[0][0][iStartState][iAction1][iAction2][iEndState] = 0.0;
							model.obs[iObs1][iObs2][iStartState][iAction1][iAction2][iEndState] = 1.0;
						} catch (Exception e) {
							System.out.println("*");
						}
					}
					if (dReward != -1) {
						//swOutput.println("R: " + sAction1 + " " + sAction2 + " : " + GetState(lStart) + " : * : * : " + dReward);
						for (i = 0; i < Agents; i++)
							model.rew[i][iStartState][iAction1][iAction2] = dReward;
					}

					//deadend
					//model.trans[iDeadendState][iAction1][iAction2][iDeadendState] = 1.0;
					//model.obs[0][0][iDeadendState][iAction1][iAction2][iDeadendState] = 1.0;
					//for(i = 0 ; i < Agents ; i++)
					//	model.rew[i][iDeadendState][iAction1][iAction2] = -1.0;
				}
			}
		}


		//swOutput.println("T: * : deadend : deadend : 1.0");
		//swOutput.println("O: * : deadend : null null : 1.0");
		//swOutput.println("R: * : deadend : * : * : -1.0");
		//swOutput.println("R: " + sAction1 + " " + sAction2 + " : deadend : * : * : 0.0");

		//swOutput.close();

		model.checkSumToOne(model.trans, model.obs);

		String[] plan = new String[]{"observe-box observe-box", "declare-goal declare-goal", "declare-goal declare-goal", "declare-goal declare-goal"};
		TestPlan(34,plan,dActions,model,cStates,cObservations);

		plan = new String[]{"observe-box observe-box", "push-up observe-box", "declare-goal declare-goal", "declare-goal declare-goal", "declare-goal declare-goal"};
		TestPlan(35,plan,dActions,model,cStates,cObservations);

		plan = new String[]{"observe-box observe-box", "observe-box push-up", "declare-goal declare-goal", "declare-goal declare-goal", "declare-goal declare-goal"};
		TestPlan(38,plan,dActions,model,cStates,cObservations);

		plan = new String[]{"observe-box observe-box", "push-up push-up", "declare-goal declare-goal", "declare-goal declare-goal", "declare-goal declare-goal"};
		TestPlan(39,plan,dActions,model,cStates,cObservations);
	}

	public void GetDecPOMDPDomain(ProblemDataSmall model) 
	{
		Map<String,Integer> dStates = new HashMap<String,Integer>(); 
		Map<String,Integer> dActions = new HashMap<String,Integer>(); 
		Map<String,Integer> dObservations = new HashMap<String,Integer>(); 
			
		
		List<Location> lLocations = new LinkedList<Location>();
		for (int i = 1; i <= Width; i++)
			for (int j = 1; j <= Length; j++)
				lLocations.add(new Location(i, j));

		List<List<Location>> lAllStates = ComputeStates(0, lLocations);
		int cStates = lAllStates.size() + 1; //+1 for deadend
		int cActions = 10;
		int cObservations = 1 + Boxes.size();
		for(int i = 0 ; i < lAllStates.size(); i++)
			dStates.put(GetState(lAllStates.get(i)), i);
		dStates.put("deadend", cStates - 1);
		
		int i = 0;
		dObservations.put("null",0);
		for(Box b : Boxes){
			dObservations.put("b" + (b.ID + 1), dObservations.size());
			i++;
		}

		//set start state distribution
		List<List<Location>> lStartStates = ComputeInitialStates(0);
		List<Integer> lStartStateIndexes = new LinkedList<Integer>();
		for (List<Location> l : lStartStates){
			System.out.println(GetState(l));
			lStartStateIndexes.add(dStates.get(GetState(l)));
		}
		model.startDist = new double[cStates];
		for(i = 0 ; i < cStates ; i++){
			if(lStartStateIndexes.contains(i))
				model.startDist[i] = 1.0 / lStartStateIndexes.size();
			else
				model.startDist[i] = 0;
		}
		
		String sActions = "move-up move-down move-left move-right push-up push-down push-left push-right observe-box declare-goal";
		i = 0;
		for (String sAction : sActions.split(" "))
		{
			dActions.put(sAction, i);
			i++;
		}
		int idx = 0;

		model.trans = new double[cStates][cActions][cActions][cStates];
		model.rew = new double[Agents][cStates][cActions][cActions];
		model.obs = new double[cObservations][cObservations][cStates][cActions][cActions][cStates];
		

		for(int iState = 0 ; iState < cStates ; iState++){
			for(int iAction1 = 0 ; iAction1 < cActions ; iAction1++){
				for(int iAction2 = 0; iAction2 < cActions; iAction2++){
					model.trans[iState][iAction1][iAction2][iState] = 1.0;
					for(int iEndState = 0 ; iEndState < cStates ; iEndState++){
						model.obs[0][0][iState][iAction1][iAction2][iEndState] = 1.0;
					}
					for(i = 0 ; i < Agents ; i++)
						model.rew[i][iState][iAction1][iAction2] = -1.0;
					
				}
			}
		}
		
		int iDeadendState = dStates.get("deadend");
		
		for (List<Location> lStart : lAllStates)
		{
			//swOutput.println();
			int iStartState = dStates.get(GetState(lStart));

			idx++;
			if (idx % 100 == 0)
				System.out.print("\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b" + idx);

			boolean bGoalState = true;
			for (int iBox = 0; iBox < Boxes.size(); iBox++)
			{
				if (!Boxes.get(iBox).IsTargetLocation(lStart.get(Agents + iBox)))
					bGoalState = false;
			}
			//if(bGoalState)
			//	System.out.println("*");


			for (String sAction1 : sActions.split(" "))
			{
				int iAction1 = dActions.get(sAction1);
				String[] aAction1 = sAction1.split("-");
				for (String sAction2 : sActions.split(" "))
				{
					int iAction2 = dActions.get(sAction2);
					String[] aAction2 = sAction2.split("-");

					if (bGoalState)
					{
						if (sAction1.equals("declare-goal") && sAction2.equals("declare-goal"))
						{
							
							//swOutput.println("T: " + sAction1 + " " + sAction2 + " : " + GetState(lStart) + " : " + GetState(lStart) + " : 1.0");
							//swOutput.println("O: " + sAction1 + " " + sAction2 + " : " + GetState(lStart) + " : null null : 1.0");
							//swOutput.println("R: " + sAction1 + " " + sAction2 + " : " + GetState(lStart) + " : * : * : 0.0");
							
							
							for(i = 0 ; i < Agents ; i++)
								model.rew[i][iStartState][iAction1][iAction2] = 0.0;
							model.trans[iStartState][iAction1][iAction2][iStartState] = 1.0;
							model.obs[0][0][iStartState][iAction1][iAction2][iStartState] = 1.0;
							
							continue;
						}
					}
					else if (sAction1.equals("declare-goal") || sAction2.equals("declare-goal"))
					{
						//swOutput.println("T: " + sAction1 + " " + sAction2 + " : " + GetState(lStart) + " : deadend : 1.0");
						//swOutput.println("T: " + sAction1 + " " + sAction2 + " : " + GetState(lStart) + " : " + GetState(lStart) + " : 0.0");
						//swOutput.println("O: " + sAction1 + " " + sAction2 + " : " + GetState(lStart) + " : null null : 1.0");
						//swOutput.println("R: " + sAction1 + " " + sAction2 + " : " + GetState(lStart) + " : * : * : -1.0");

						for(i = 0 ; i < Agents ; i++)
							model.rew[i][iStartState][iAction1][iAction2] = -1.0;
						model.trans[iStartState][iAction1][iAction2][iStartState] = 0.0;
						model.trans[iStartState][iAction1][iAction2][iDeadendState] = 1.0;
						model.obs[0][0][iStartState][iAction1][iAction2][iDeadendState] = 1.0;

						
						continue;
					}

					//if(iStartState == 215 && aAction1[0].equals("push") && aAction2[0].equals("push"))
					//	System.out.println("*");

					boolean bLegalPush = true;
					int iPushedBox1 = GetPushedBox(0, lStart);
					int iPushedBox2 = GetPushedBox(1, lStart);
					if (aAction1[0].equals("push") && iPushedBox1 == -1)
					{
						bLegalPush = false;
					}
					else if (!aAction1[0].equals("push"))
						iPushedBox1 = -1;
					if (aAction2[0].equals("push") && iPushedBox2 == -1)
					{
						bLegalPush = false;
					}
					else if (!aAction2[0].equals("push"))
						iPushedBox2 = -1;

					if (iPushedBox1 == iPushedBox2 && iPushedBox1 != -1)//joint push
					{
						if (!aAction1[1].equals(aAction2[1]))//not same direction
							bLegalPush = false;
					}
					List<Location> lEnd = null;
					double dReward = 0.0;

					if (bLegalPush)
					{
						lEnd = new LinkedList<Location>();
						lEnd.add(lStart.get(0).Move(aAction1[1]));//agent 1
						lEnd.add(lStart.get(1).Move(aAction2[1]));//agent 2

						for (int iBox = 0; iBox < Boxes.size(); iBox++)
						{
							if (iBox == iPushedBox1 && iPushedBox1 == iPushedBox2)//joint push
							{
								Location lDest = lStart.get(Agents + iBox).Move(aAction1[1]);
								if (Boxes.get(iBox).IsTargetLocation(lStart.get(Agents + iBox)))
									dReward = -1;
								else if (Boxes.get(iBox).IsTargetLocation(lDest))
									dReward = 1;
								lEnd.add(lDest);
							}
							else if (iBox == iPushedBox1 && Boxes.get(iBox).Weight == 1)
							{
								Location lDest = lStart.get(Agents + iBox).Move(aAction1[1]);
								if (Boxes.get(iBox).IsTargetLocation(lStart.get(Agents + iBox)))
									dReward = -1;
								else if (Boxes.get(iBox).IsTargetLocation(lDest))
									dReward = 1;
								lEnd.add(lDest);//agent 1
							}
							else if (iBox == iPushedBox2 && Boxes.get(iBox).Weight == 1)
							{
								Location lDest = lStart.get(Agents + iBox).Move(aAction2[1]);
								if (Boxes.get(iBox).IsTargetLocation(lStart.get(Agents + iBox)))
									dReward = -1;
								else if (Boxes.get(iBox).IsTargetLocation(lDest))
									dReward = 1;
								lEnd.add(lDest);//agent 2
							}
							else
								lEnd.add(lStart.get(Agents + iBox));
						}


					}

					String sObs1 = "null";
					if (aAction1[0].equals("observe"))
					{
						if (aAction1[1].equals("box"))
						{
							int iBox = GetPushedBox(0, lStart);
							if (iBox > -1)
								sObs1 = "b" + (iBox + 1);
						}
						if (aAction1[1].equals("other"))
						{
							if (lStart.get(0) == lStart.get(1))
								sObs1 = "yes";
							else
								sObs1 = "no";
						}
					}
					String sObs2 = "null";
					if (aAction2[0].equals("observe"))
					{
						if (aAction2[1].equals("box"))
						{
							int iBox = GetPushedBox(1, lStart);
							if (iBox > -1)
								sObs2 = "b" + (iBox + 1);
						}
						if (aAction2[1].equals("other"))
						{
							if (lStart.get(0) == lStart.get(1))
								sObs1 = "yes";
							else
								sObs1 = "no";
						}
					}

					dReward = -1;//for goal based model

					if (lEnd == null)
					{
						lEnd = lStart;
						dReward = -10;
					}
					
					int iEndState = dStates.get(GetState(lEnd));

					//if (!Same(lEnd, lStart))
					{
						//swOutput.println("T: " + sAction1 + " " + sAction2 + " : " + GetState(lStart) + " : " + GetState(lEnd) + " : 1.0");
						//swOutput.println("T: " + sAction1 + " " + sAction2 + " : " + GetState(lStart) + " : " + GetState(lStart) + " : 0.0");
						model.trans[iStartState][iAction1][iAction2][iStartState] = 0.0;
						model.trans[iStartState][iAction1][iAction2][iEndState] = 1.0;
					}

					if (!sObs1.equals("null") || !sObs2.equals("null")) {
						try {
							int iObs1 = dObservations.get(sObs1);
							int iObs2 = dObservations.get(sObs2);
							//swOutput.println("O: " + sAction1 + " " + sAction2 + " : " + GetState(lStart) + " : null null : 0.0");
							//swOutput.println("O: " + sAction1 + " " + sAction2 + " : " + GetState(lStart) + " : " + sObs1 + " " + sObs2 + " : 1.0");
							model.obs[0][0][iStartState][iAction1][iAction2][iEndState] = 0.0;
							model.obs[iObs1][iObs2][iStartState][iAction1][iAction2][iEndState] = 1.0;
						} catch (Exception e) {
							System.out.println("*");
						}
					}
					if (dReward != -1) {
						//swOutput.println("R: " + sAction1 + " " + sAction2 + " : " + GetState(lStart) + " : * : * : " + dReward);
						for (i = 0; i < Agents; i++)
							model.rew[i][iStartState][iAction1][iAction2] = dReward;
					}

						//deadend
					//model.trans[iDeadendState][iAction1][iAction2][iDeadendState] = 1.0;
					//model.obs[0][0][iDeadendState][iAction1][iAction2][iDeadendState] = 1.0;
					//for(i = 0 ; i < Agents ; i++)
					//	model.rew[i][iDeadendState][iAction1][iAction2] = -1.0;
				}
			}
		}

		
		//swOutput.println("T: * : deadend : deadend : 1.0");
		//swOutput.println("O: * : deadend : null null : 1.0");
		//swOutput.println("R: * : deadend : * : * : -1.0");
		//swOutput.println("R: " + sAction1 + " " + sAction2 + " : deadend : * : * : 0.0");

		//swOutput.close();
		
		model.checkSumToOne(model.trans, model.obs);

		String[] plan = new String[]{"observe-box observe-box", "declare-goal declare-goal", "declare-goal declare-goal", "declare-goal declare-goal"};
		TestPlan(210,plan,dActions,model,cStates,cObservations);

		plan = new String[]{"observe-box observe-box", "push-up move-up", "declare-goal declare-goal", "declare-goal declare-goal", "declare-goal declare-goal"};
		TestPlan(211,plan,dActions,model,cStates,cObservations);

		plan = new String[]{"observe-box observe-box", "move-up push-up", "declare-goal declare-goal", "declare-goal declare-goal", "declare-goal declare-goal"};
		TestPlan(214,plan,dActions,model,cStates,cObservations);

		plan = new String[]{"observe-box observe-box", "push-up push-up", "declare-goal declare-goal", "declare-goal declare-goal", "declare-goal declare-goal"};
		TestPlan(215,plan,dActions,model,cStates,cObservations);
	}

	public void TestPlan(int iStartState, String[] lPlan, Map<String,Integer> dActions, ProblemDataSmall model, int cStates, int cObservations){
		System.out.println("++++++++++++++++++++++");
		int iCurrentState = iStartState, iNextState = 0;
		int iAction1, iAction2;

		System.out.println("pr(s0)=" +model.startDist[iStartState]);

		for(int i = 0 ; i < lPlan.length ; i++){
			String[] a = lPlan[i].split(" ");
			iAction1 = dActions.get(a[0]);
			iAction2 = dActions.get(a[1]);
			iNextState = -1;
			for(int j = 0 ; j < cStates ; j++){
				if(model.trans[iCurrentState][iAction1][iAction2][j] == 1)
					iNextState = j;
			}
			double dR = model.rew[0][iCurrentState][iAction1][iAction2];
			int iObs1 = -1, iObs2 = -1;
			for(int j1 = 0 ; j1 < cObservations ; j1++){
				for(int j2 = 0 ; j2 < cObservations ; j2++){
					if(model.obs[j1][j2][iCurrentState][iAction1][iAction2][iNextState] == 1.0){
						iObs1 = j1;
						iObs2 = j2;
					}
				}
			}
			System.out.println(i + ") s = " + iCurrentState + ", s' = " + iNextState + ", a = [" + iAction1 + "," + iAction2 + "], o = [" + iObs1 + "," + iObs2 + "], r = " + dR);
			iCurrentState = iNextState;
		}
	}
	
	public void WriteProblem(String sPath) throws Exception
	{
		PrintWriter fos = new PrintWriter(new FileOutputStream(sPath + "\\p.pddl"));
		fos.print(GenerateProblem());
		fos.close();
	}

	private String GenerateProblem()
	{
		String sDomain = "(define \n";
		sDomain += "(problem " + Name + ")\n";
		sDomain += "(:domain " + Name + ")\n";
		sDomain += "(:goal (and";
		for (Box b : Boxes)
		{
			sDomain += " (box-at b" + b.ID + " " + GetPosition(b.X, 1) + ")";
		}
		sDomain += "))\n";
		sDomain += GetInitState();
		sDomain += ")";
		return sDomain;
	}

	private String GetPosition(int iX, int iY)
	{
		return "p" + iX + "-" + iY;
	}
	private String GetAdjacent(int iX1, int iY1, int iX2, int iY2)
	{
		return "\t(adj " + GetPosition(iX1, iY1) + " " + GetPosition(iX2, iY2) + ")\n";
	}
	private String GetAdjacents()
	{
		String sAdjacents = "";
		for (int iX = 1; iX <= Width; iX++)
		{
			for (int iY = 1; iY <= Length; iY++)
			{
				//sAdjacents += GetAdjacent(iX, iY, iX, iY);
				if (iX > 1)
					sAdjacents += GetAdjacent(iX, iY, iX - 1, iY);
				if (iX < Width)
					sAdjacents += GetAdjacent(iX, iY, iX + 1, iY);
				if (iY > 1)
					sAdjacents += GetAdjacent(iX, iY, iX, iY - 1);
				if (iY < Length)
					sAdjacents += GetAdjacent(iX, iY, iX, iY + 1);
			}
		}
		return sAdjacents;
	}

	private String GetInitState()
	{
		String sInit = "(:init\n";
		sInit += "\t(and\n";
		//sInit += "\t\t(eq move-n-push move-n-push)";
		//sInit += "\t\t(eq move-only move-only)";
		sInit += "\t\t(agent-at a1 " + GetPosition(Width, Length) + ")" + "\n";
		sInit += "\t\t(agent-at a2 " + GetPosition(1, Length) + ")" + "\n";
		sInit += GetAdjacents() + "\n";


		for (Box b : Boxes)
		{
			if (b.Weight == 1)
				sInit += "\t\t(not (heavy b" + b.ID + "))\n";
			else
				sInit += "\t\t(heavy b" + b.ID + ")\n";
			sInit += "\t\t(oneof (box-at b" + b.ID + " " + GetPosition(b.X, b.Y) + ") (box-at b" + b.ID + " " + GetPosition(b.X, 1) + "))\n";
		}

		sInit += "\t)\n)\n";
		return sInit;
	}

	public void WriteDomain(String sPath) throws Exception
	{
		//if (!Directory.Exists(sPath))
		//	Directory.CreateDirectory(sPath);
		PrintWriter fos = new PrintWriter(new FileOutputStream(sPath + "\\d.pddl"));
		//sw.print(GenerateDomain());
		GenerateDomain(fos);
		fos.close();
	}

	private void GenerateDomain(PrintWriter sw)
	{
		sw.print("(define \n");
		sw.print("(domain " + Name + ")\n");
		sw.print("(:types POS AGENT BOX PUSH)\n");
		sw.print(GenerateConstants() + "\n");
		sw.print(GeneratePredicates());
		GenerateActions(sw);
		sw.print(")");
	}

	private void GenerateActions(PrintWriter sw)
	{
		//sw.println(GenerateMoveAction());//problem - pushes always - simple conformant plan
		/*
            sw.println(GenerateMoveMoveAction());
            sw.println(GenerateMovePushAction());
            sw.println(GeneratePushMoveAction());
            sw.println(GeneratePushPushAction());
		 */

		sw.print(GenerateFactoredMoveAction());
		sw.print(GenerateFactoredPushAction());
		sw.print(GenerateFactoredJointPushAction());
		sw.print(GenerateFactoredObserveBoxAction());
		sw.print(GenerateFactoredObserveAgentAction());
		//sw.println(GenerateNoop());
	}

	private String GenerateMoveAction()
	{
		String sAction = "(:action move\n";
		sAction += "\t:parameters (?start1 - pos ?end1 - pos ?push1 - push ?start2 - pos ?end2 - pos ?push2 - push)\n";
		sAction += "\t:precondition (and (adj ?start1 ?end1) (adj ?start2 ?end2) (agent-at a1 ?start1) (agent-at a2 ?start2))\n";
		sAction += "\t:effect (and (not (agent-at a1 ?start1)) (agent-at a1 ?end1)  (not (agent-at a2 ?start2)) (agent-at a2 ?end2)\n";
		for (Box b : Boxes)
		{
			if (b.Weight == 1)//light boxes - single agent
			{
				sAction += "\t\t(when (and (box-at b" + b.ID + " ?start1) (= ?push1 move-n-push))\n";
				sAction += "\t\t\t(and (not (box-at b" + b.ID + " ?start1)) (box-at b" + b.ID + " ?end1)))\n";

				sAction += "\t\t(when (and (box-at b" + b.ID + " ?start2) (= ?push2 move-n-push))\n";
				sAction += "\t\t\t(and (not (box-at b" + b.ID + " ?start2)) (box-at b" + b.ID + " ?end2)))\n";
			}
			else//heavy boxes - two agents
			{
				sAction += "\t\t(when (and (box-at b" + b.ID + " ?start1) (= ?push1 move-n-push) (= ?push2 move-n-push) (= ?start1 ?start2) (= ?end1 ?end2))\n";
				sAction += "\t\t\t(and (not (box-at b" + b.ID + " ?start1)) (box-at b" + b.ID + " ?end1)))\n";
			}
		}

		sAction += "\t)\n";

		sAction += "\t:observe-agent a1 (and (agent-at a2 ?end1)";
		for (Box b : Boxes)
			sAction += " (box-at b" + b.ID + " ?end1)";
		sAction += ")\n";

		sAction += "\t:observe-agent a2 (and (agent-at a1 ?end2)";
		for (Box b : Boxes)
			sAction += " (box-at b" + b.ID + " ?end2)";
		sAction += ")\n";

		sAction += ")\n";
		//not sure how to do sensing
		return sAction;
	}

	private String GetMovePreconditions()
	{
		return "\t:precondition (and (adj ?start1 ?end1) (adj ?start2 ?end2) (agent-at a1 ?start1) (agent-at a2 ?start2)";
	}

	private String GetMoveObligatoryEffects()
	{
		return "\t:effect (and (not (agent-at a1 ?start1)) (agent-at a1 ?end1)  (not (agent-at a2 ?start2)) (agent-at a2 ?end2)\n";
	}

	private String GetMoveParameters()
	{
		return "\t:parameters (?start1 - pos ?end1 - pos ?start2 - pos ?end2 - pos)\n";
	}

	private String GetMoveObservations()
	{
		String sObserve = "";
		sObserve += "\t:observe-agent a1 (and (agent-at a2 ?end1)";
		for (Box b : Boxes)
			sObserve += " (box-at b" + b.ID + " ?end1)";
		sObserve += ")\n";

		sObserve += "\t:observe-agent a2 (and (agent-at a1 ?end2)";
		for (Box b : Boxes)
			sObserve += " (box-at b" + b.ID + " ?end2)";
		sObserve += ")\n";
		return sObserve;
	}

	private String GetParameterizedMoveObservations(String sAgent)
	{
		String sObserve = "";
		sObserve += "\t:observe-agent " + sAgent + " (and ";
		for (int iAgent = 1; iAgent <= Agents; iAgent++)
			sObserve += " (agent-at a" + iAgent + " ?end)";
		for (Box b : Boxes)
			sObserve += " (box-at b" + b.ID + " ?end)";
		sObserve += ")\n";

		return sObserve;
	}

	/*
        private String GeneratePushPushAction()
        {
            String sAction = "(:action push-push\n";
            sAction += GetMoveParameters();
            sAction += GetMovePreconditions();

            sAction += "\n\t\t(or";
            for (Box b : Boxes)
            {
                sAction += " (box-at b" + b.ID + " ?start1)";
            }
            sAction += ")";
            sAction += "\n\t\t(or";
            for (Box b : Boxes)
            {
                sAction += " (box-at b" + b.ID + " ?start2)";
            }
            sAction += "))\n";

            sAction += GetMoveObligatoryEffects();
            for (Box b : Boxes)
            {
                if (b.Weight == 1)//light boxes - single agent
                {
                    sAction += "\t\t(when (box-at b" + b.ID + " ?start1)\n";
                    sAction += "\t\t\t(and (not (box-at b" + b.ID + " ?start1)) (box-at b" + b.ID + " ?end1)))\n";
                    sAction += "\t\t(when (box-at b" + b.ID + " ?start2)\n";
                    sAction += "\t\t\t(and (not (box-at b" + b.ID + " ?start2)) (box-at b" + b.ID + " ?end2)))\n";
                }
                else//heavy boxes - two agents
                {
                    sAction += "\t\t(when (and (box-at b" + b.ID + " ?start1) (= ?start1 ?start2) (= ?end1 ?end2))\n";
                    sAction += "\t\t\t(and (not (box-at b" + b.ID + " ?start1)) (box-at b" + b.ID + " ?end1)))\n";
                }
            }

            sAction += "\t)\n";

            sAction += GetMoveObservations();

            sAction += ")\n";
            //not sure how to do sensing
            return sAction;
        }

        private String GeneratePushMoveAction()
        {
            String sAction = "(:action push-move\n";
            sAction += GetMoveParameters();
            sAction += GetMovePreconditions();
            sAction += "\n\t\t(or";
            for (Box b : Boxes)
            {
                sAction += " (box-at b" + b.ID + " ?start1)";
            }
            sAction += "))\n";

            sAction += GetMoveObligatoryEffects();
            for (Box b : Boxes)
            {
                if (b.Weight == 1)//light boxes - single agent
                {
                    sAction += "\t\t(when (box-at b" + b.ID + " ?start1)\n";
                    sAction += "\t\t\t(and (not (box-at b" + b.ID + " ?start1)) (box-at b" + b.ID + " ?end1)))\n";
                }
            }

            sAction += "\t)\n";

            sAction += GetMoveObservations();

            sAction += ")\n";
            //not sure how to do sensing
            return sAction;
        }

        private String GenerateMovePushAction()
        {
            String sAction = "(:action move-push\n";
            sAction += GetMoveParameters();
            sAction += GetMovePreconditions();
            sAction += "\n\t\t(or";
            for (Box b : Boxes)
            {
                sAction += " (box-at b" + b.ID + " ?start2)";
            }
            sAction += "))\n";

            sAction += GetMoveObligatoryEffects();
            for (Box b : Boxes)
            {
                if (b.Weight == 1)//light boxes - single agent
                {
                    sAction += "\t\t(when (box-at b" + b.ID + " ?start2)\n";
                    sAction += "\t\t\t(and (not (box-at b" + b.ID + " ?start2)) (box-at b" + b.ID + " ?end2)))\n";
                }
            }

            sAction += "\t)\n";

            sAction += GetMoveObservations();

            sAction += ")\n";
            //not sure how to do sensing
            return sAction;
        }

        private String GenerateMoveMoveAction()
        {
            String sAction = "(:action move-move\n";
            sAction += GetMoveParameters();
            sAction += GetMovePreconditions() + ")\n";
            sAction += GetMoveObligatoryEffects();
            sAction += "\t)\n";

            sAction += GetMoveObservations();

            sAction += ")\n";
            //not sure how to do sensing
            return sAction;
        }
	 */


	private String GeneratePushPushAction()
	{
		String sAction = "(:action push-push\n";
		sAction += "\t:parameters (?start1 - pos ?end1 - pos ?b1 - box ?start2 - pos ?end2 - pos ?b2 - box)\n";
		sAction += GetMovePreconditions() + " (box-at ?b1 ?start1) (box-at ?b2 ?start2))\n";
		sAction += GetMoveObligatoryEffects();

		sAction += "\t\t(when (not (heavy ?b1))\n";
		sAction += "\t\t\t(and (not (box-at ?b1 ?start1)) (box-at ?b1 ?end1)))\n";
		sAction += "\t\t(when (and (not (heavy ?b2)))\n";
		sAction += "\t\t\t(and (not (box-at ?b2 ?start2)) (box-at ?b2 ?end2)))\n";

		sAction += "\t\t(when (and (heavy ?b1) (= ?b1 ?b2) (= ?start1 ?start2) (= ?end1 ?end2))\n";
		sAction += "\t\t\t(and (not (box-at ?b1 ?start1)) (box-at ?b1 ?end1)))\n";

		sAction += "\t)\n";

		sAction += GetMoveObservations();

		sAction += ")\n";
		//not sure how to do sensing
		return sAction;
	}

	private String GeneratePushMoveAction()
	{
		String sAction = "(:action push-move\n";
		sAction += "\t:parameters (?start1 - pos ?end1 - pos ?b1 - box ?start2 - pos ?end2 - pos)\n";
		sAction += GetMovePreconditions() + " (box-at ?b1 ?start1))\n";
		sAction += GetMoveObligatoryEffects();

		sAction += "\t\t(when (not (heavy ?b1))\n";
		sAction += "\t\t\t(and (not (box-at ?b1 ?start1)) (box-at ?b1 ?end1)))\n";

		sAction += "\t)\n";

		sAction += GetMoveObservations();

		sAction += ")\n";
		//not sure how to do sensing
		return sAction;
	}
	private String GenerateMovePushAction()
	{
		String sAction = "(:action move-push\n";
		sAction += "\t:parameters (?start1 - pos ?end1 - pos ?start2 - pos ?end2 - pos ?b2 - box)\n";
		sAction += GetMovePreconditions() + " (box-at ?b2 ?start2))\n";
		sAction += GetMoveObligatoryEffects();

		sAction += "\t\t(when (and (not (heavy ?b2)))\n";
		sAction += "\t\t\t(and (not (box-at ?b2 ?start2)) (box-at ?b2 ?end2)))\n";

		sAction += "\t)\n";

		sAction += GetMoveObservations();

		sAction += ")\n";
		//not sure how to do sensing
		return sAction;
	}


	private String GenerateMoveMoveAction()
	{
		String sAction = "(:action move-move\n";
		sAction += GetMoveParameters();
		sAction += GetMovePreconditions() + ")\n";
		sAction += GetMoveObligatoryEffects();
		sAction += "\t)\n";

		sAction += GetMoveObservations();

		sAction += ")\n";
		//not sure how to do sensing
		return sAction;
	}

	private String GenerateFactoredMoveAction()
	{
		String sAction = "(:action move\n";
		sAction += "\t:parameters (?start - pos ?end - pos ?a - agent)\n";

		sAction += "\t:precondition (and (adj ?start ?end) (agent-at ?a ?start))\n";
		sAction += "\t:effect (and (not (agent-at ?a ?start)) (agent-at ?a ?end))\n";

		//sAction += GetParameterizedMoveObservations("?a");

		sAction += ")\n";

		return sAction;
	}

	private String GenerateFactoredObserveAgentAction()
	{
		String sAction = "(:action observe-agent\n";
		sAction += "\t:parameters (?p - pos ?a1 - agent ?a2 - agent)\n";

		sAction += "\t:precondition (and (agent-at ?a1 ?p))\n";

		sAction += "\t:observe-agent ?a1  (agent-at ?a2 ?p)\n";
		sAction += ")\n";

		return sAction;
	}

	private String GenerateFactoredObserveBoxAction()
	{
		String sAction = "(:action observe-box\n";
		sAction += "\t:parameters (?p - pos ?a - agent ?b - box)\n";

		sAction += "\t:precondition (and (agent-at ?a ?p))\n";

		sAction += "\t:observe-agent ?a (box-at ?b ?p)\n";

		sAction += ")\n";

		return sAction;
	}


	private String GenerateFactoredPushAction()
	{
		String sAction = "(:action push\n";
		sAction += "\t:parameters (?start - pos ?end - pos ?b - box ?a - agent)\n";

		sAction += "\t:precondition (and (adj ?start ?end) (agent-at ?a ?start) (box-at ?b ?start) (not (heavy ?b)))\n";
		sAction += "\t:effect (and (not (agent-at ?a ?start)) (agent-at ?a ?end) (not (box-at ?b ?start)) (box-at ?b ?end))\n";

		//sAction += GetParameterizedMoveObservations("?a");

		sAction += ")\n";

		return sAction;
	}
	/*
        private String GenerateFactoredJointPushAction()
        {
            String sAction = "(:action joint-push\n";
            sAction += "\t:parameters (?start - pos ?end - pos ?b - box ?a1 - agent ?a2 - agent)\n";

            sAction += "\t:precondition (and (not (= ?a1 ?a2)) (adj ?start ?end) (agent-at ?a1 ?start) (agent-at ?a2 ?start) (box-at ?b ?start))\n";
            sAction += "\t:effect (and (not (agent-at ?a1 ?start)) (agent-at ?a1 ?end) (not (agent-at ?a2 ?start)) (agent-at ?a2 ?end) (not (box-at ?b ?start)) (box-at ?b ?end))\n";

            sAction += GetParameterizedMoveObservations("?a1");
            sAction += GetParameterizedMoveObservations("?a2");

            sAction += ")\n";

            return sAction;
        }
	 */
	private String GenerateFactoredJointPushAction()
	{
		String sAction = "(:action joint-push\n";
		sAction += "\t:parameters (?start - pos ?end - pos ?b - box)\n";

		sAction += "\t:precondition (and (adj ?start ?end) (agent-at a1 ?start) (agent-at a2 ?start) (box-at ?b ?start) (heavy ?b))\n";
		sAction += "\t:effect (and (not (agent-at a1 ?start)) (agent-at a1 ?end) (not (agent-at a2 ?start)) (agent-at a2 ?end) (not (box-at ?b ?start)) (box-at ?b ?end))\n";

		//sAction += GetParameterizedMoveObservations("a1");
		// sAction += GetParameterizedMoveObservations("a2");

		sAction += ")\n";

		return sAction;
	}


	private String GenerateConstants()
	{
		String sConstants = "(:constants\n\t";
		for (int i = 1; i <= Width; i++)
			for (int j = 1; j <= Length; j++)
				sConstants += " p" + i + "-" + j;
		sConstants += " - pos\n\t";
		for (Box b : Boxes)
			sConstants += " b" + b.ID;
		sConstants += " - box\n\t";
		sConstants += " a1 a2 - agent\n\t";
		sConstants += " move-n-push move-only - push\n";
		sConstants += ")\n";
		return sConstants;
	}

	private String GeneratePredicates()
	{
		String sPredicates = "(:predicates\n";
		sPredicates += "\t(adj ?i ?j - pos)\n";
		sPredicates += "\t(agent-at ?a - agent ?i - pos)\n";
		sPredicates += "\t(box-at ?b - box ?i - pos)\n";
		sPredicates += "\t(heavy ?b - box)\n";
		//sPredicates += "\t(eq ?p1 ?p2 - push)\n";
		sPredicates += ")\n";
		return sPredicates;
	}



	private String GenerateNoop()
	{
		String sAction = "(:action no-op\n";
		sAction += "\t:parameters (?a - agent)\n";

		//sAction += "\t:precondition \n";
		//sAction += "\t:effect \n";

		sAction += ")\n";

		return sAction;
	}


	private boolean Same(List<Location> l1, List<Location> l2)
	{
		if (l1 == l2)
			return true;
		if (l1 == null)
			return false;
		if (l2 == null)
			return false;
		for (int i = 0; i < l1.size(); i++)
		{
			if (l1.get(i).X != l2.get(i).X || l1.get(i).Y != l2.get(i).Y)
				return false;
		}
		return true;
	}

	private int GetPushedBox(int iAgent, List<Location> lState)
	{
		for (int iBox = 0; iBox < Boxes.size(); iBox++)
		{
			if (lState.get(iAgent).X == lState.get(Agents + iBox).X && lState.get(iAgent).Y == lState.get(Agents + iBox).Y)
				return iBox;
		}
		return -1;
	}

	public class Location
	{
		public int X ;
		public int Y ;
		//public static int Width, Length;
		public Location(int iX, int iY)
		{
			X = iX;
			Y = iY;
		}

		public  String ToString()
		{
			return X + "," + Y;
		}

		public Location Move(String sDirection)
		{
			//check out of bounds
			Location l = new Location(X, Y);
			if (sDirection.equals("up") && l.Y > 1)
				l.Y = l.Y - 1;
			if (sDirection.equals("down") && l.Y < Length)
				l.Y = l.Y + 1;
			if (sDirection.equals("left") && l.X > 1)
				l.X = l.X - 1;
			if (sDirection.equals("right") && l.X < Width)
				l.X = l.X + 1;
			return l;
		}
		/*
		public static boolean operator ==(Location l1, Location l2)
				{
			return l1.X == l2.X && l1.Y == l2.Y;
				}
		public static boolean operator !=(Location l1, Location l2)
				{
			return l1.X != l2.X || l1.Y != l2.Y;
				}
		 */
	}

	private static boolean StateNames = true;

	private String GetState(List<Location> lXY)
	{
		if (lXY == null)
		{
			//if (StateNames)
			return "deadend";
			//return "0";
		}
		String s = "";
		/*
            for (int i = 0; i < Agents; i++)
                s += "a" + (i+1) + "-" + lXY[i].X + "-" + lXY[i].Y + " ";
            for (int i = 0; i < Boxes.Count; i++)
                s += "b" + (i + 1) + "-" + lXY[Agents + i].X + "-" + lXY[Agents + i].Y + " ";
            s = s.Trim();
            s = s.Replace(' ', '_');
		 * */
		if (StateNames)
		{
			s += "A_";

			for (int i = 0; i < Agents; i++)
			{
				if (i == 0)
					s += lXY.get(i).X + "-" + lXY.get(i).Y + "_";
				else if (i==Agents - 1)
				{
					s += lXY.get(i).X + "-" + lXY.get(i).Y + "_";
				}
			}

			s = s.substring(0, s.length() - 1) + "_";
			s += "B_";
			for (int i = 0; i < Boxes.size(); i++)
				s += lXY.get(Agents + i).X + "-" + lXY.get(Agents + i).Y + "_";
			s = s.substring(0, s.length() - 1);
		}
		else
		{
			int id = 0;
			int iOffset = Math.max(Width, Length);
			for (int i = 0; i < lXY.size(); i++)
			{
				id *= iOffset;
				id += lXY.get(i).X - 1;
				id *= iOffset;
				id += lXY.get(i).Y - 1;
			}
			id++; //0 is deadend
			s = "s" + id;
		}
		return s;
	}

	private List<List<Location>> ComputeStates(int iCurrent, List<Location> lLocations)
	{
		List<List<Location>> lAll = new LinkedList<List<Location>>();
		if (iCurrent == Agents + Boxes.size() - 1) {
			for (Location sLoc : lLocations) {
				List<Location> l = new LinkedList<Location>();
				l.add(sLoc);
				lAll.add(l);
			}
		}
		else
		{
			List<List<Location>> lRest = ComputeStates(iCurrent + 1, lLocations);
			for (Location sLoc : lLocations)
			{
				for (List<Location> lRestLocations : lRest)
				{
					List<Location> l = new LinkedList<Location>();
					l.add(sLoc);
					l.addAll(lRestLocations);
					lAll.add(l);
				}
			}
		}
		return lAll;
	}

	private List<List<Location>> ComputeInitialStates(int iCurrentBox)
	{
		String sAgentStartPositions = "a1-" + Width + "-" + Length + "_a2-" + 1 + "-" + Length;


		List<List<Location>> lAll = new LinkedList<List<Location>>();
		Location l1 = new Location(Boxes.get(iCurrentBox).X, Boxes.get(iCurrentBox).Y);
		Location l2 = new Location(Boxes.get(iCurrentBox).X, 1);
		if (iCurrentBox == Boxes.size() - 1)
		{
			List<Location> l = new LinkedList<Location>();
			l.add(l1);
			lAll.add(l);
			l = new LinkedList<Location>();
			l.add(l2);
			lAll.add(l);
		}
		else
		{
			List<List<Location>> lRest = ComputeInitialStates(iCurrentBox + 1);
			for (List<Location> sRest : lRest)
			{
				List<Location> l = new LinkedList<Location>();
				if (iCurrentBox == 0)
				{
					l.add(new Location(Width, Length));
					l.add(new Location(1, Length));
				}
				l.add(l1);
				l.addAll(sRest);
				lAll.add(l);

				l = new LinkedList<Location>();
				if (iCurrentBox == 0)
				{
					l.add(new Location(Width, Length));
					l.add(new Location(1, Length));
				}
				l.add(l2);
				l.addAll(sRest);
				lAll.add(l);
			}
		}
		return lAll;
	}
}

