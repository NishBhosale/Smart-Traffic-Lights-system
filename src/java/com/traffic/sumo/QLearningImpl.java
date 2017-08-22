/*   
  QLearning Algorithm implementation
*/

package com.traffic.sumo;

import java.io.IOException;
import java.util.Collection;
import it.polito.appeal.traci.Lane;
import it.polito.appeal.traci.Vehicle;

public class QLearningImpl {

	private static final int nEpisodes = 1000, nActions = 4;
	// Qlearning parameters
	private double alpha, gamma;
	private double[] qTable;
	private int[] lastAction;

	QLearningImpl() {
		alpha = 0.7;
		gamma = 0.9;
		lastAction = new int[nEpisodes];
		qTable = new double[nEpisodes];
		for (int i = 0; i < nEpisodes; ++i) {
			qTable[i] = 0;
		}
	}

	public int decidePolicy(int state) {
		int bestAction = 0;
		double bestQ = -100;
		for (int i = 0; i < nActions; ++i) {
			if (qTable[nActions * state + i] > bestQ) {
				bestQ = qTable[nActions * state + i];
				bestAction = i;
			}
		}
		lastAction[state] = bestAction;
		return bestAction;
	}

	public double learn(int a, int s, int sp, int laneReward) {
		double newQ = (1 - alpha) * qTable[s * nActions + a] + alpha * (laneReward + gamma * maxQ(sp));
		qTable[s * nActions + a] = newQ;
		return newQ;
	}

	public int laneReward(Collection<Vehicle> vehicles, Lane lane) {
		int reward = 0;
		for (Vehicle veh : vehicles) {
			try {
				if (veh.getLaneId().getID().equals(lane.getID()) && veh.getSpeed() == 0.0) {
					if (veh.getType().toString().equals("emergency"))
						reward += 5;
					reward++;
				}
			} catch (IOException e) {

				e.printStackTrace();
			}
		}
		return reward;
	}

	public double maxQ(int s) {
		double ret = -1;
		for (int i = 0; i < nActions; ++i) {
			ret = Math.max(ret, qTable[s * nActions + i]);
		}
		return ret;
	}

}
