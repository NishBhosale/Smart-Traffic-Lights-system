package com.traffic.sumo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import it.polito.appeal.traci.Edge;
import it.polito.appeal.traci.Lane;
import it.polito.appeal.traci.LaneListQuery;
import it.polito.appeal.traci.LightState;
import it.polito.appeal.traci.ReadTLStateQuery;
import it.polito.appeal.traci.Repository;
import it.polito.appeal.traci.SumoTraciConnection;
import it.polito.appeal.traci.TLState;
import it.polito.appeal.traci.TrafficLight;
import it.polito.appeal.traci.Vehicle;

public class SmartTrafficLight {

	public static SumoTraciConnection conn;

	public static int getIndex(String[] states, String state) {
		for (int i = 0; i < states.length; i++) {
			if (states[i].equals(state))
				return i;
		}
		return -1;
	}

	// Method for sorting
	public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
		Map<K, V> result = new LinkedHashMap<>();
		Stream<Entry<K, V>> st = map.entrySet().stream();

		st.sorted(Comparator.comparing(e -> e.getValue())).forEach(e -> result.put(e.getKey(), e.getValue()));

		return result;
	}

	public static void main(String[] args) {
		int a, s, sp;
		String currentLightState = "";
		int currentPhase = 0;
		int count = 0;

		// Set input network
		conn = new SumoTraciConnection("data\\cross.sumocfg", 1000);
		// Set SUMO-GUI path
		System.setProperty("it.polito.appeal.traci.sumo_exe", "sumo-gui");

		QLearningImpl learning = new QLearningImpl();

		String[] states = new String[4];
		states[0] = "rGrG";
		states[1] = "yryr";
		states[2] = "GrGr";
		states[3] = "ryry";
		try {

			conn.addOption("start", "1");
			conn.addOption("quit-on-end", "1");

			// Run SUMO server
			conn.runServer();

			Collection<TrafficLight> tldetails = conn.getTrafficLightRepository().getAll().values();
			TrafficLight tlight = null;

			while (true) {
				for (TrafficLight tl : tldetails) {

					tlight = tl;
					ReadTLStateQuery currentState = tl.queryReadState();
					TLState tlstate = currentState.get();
					currentLightState = tlstate.lightState;
					if (currentPhase == 0)
						currentPhase = tl.getDefaultCurrentPhaseDuration();
				}

				Collection<Vehicle> vehicles = conn.getVehicleRepository().getAll().values();
				s = getIndex(states, currentLightState);
				a = learning.decidePolicy(s);

				if (s == states.length)
					sp = 0;
				else
					sp = s + 1;
				Collection<Lane> allLanes = conn.getLaneRepository().getAll().values();
				Collection<Lane> currentLanes = new ArrayList<Lane>();

				for (Vehicle veh : vehicles) {
					for (Lane lane : allLanes) {
						if (veh.getLaneId().getID().equals(lane.getID())) {
							if (!currentLanes.contains(lane))
								currentLanes.add(lane);
						}
					}

				}

				Map<String, Double> Qs = new HashMap<String, Double>();

				for (Lane lane : currentLanes) {
					int lanereward = learning.laneReward(vehicles, lane);
					Qs.put(lane.getID(), learning.learn(a, s, sp, lanereward));
				}

				Map<String, Double> sortedQs = sortByValue(Qs);
				Object[] lanesQs = sortedQs.keySet().toArray();

				String laneId = "";
				if (lanesQs.length > 0)
					laneId = lanesQs[lanesQs.length - 1].toString();
				count++;
				if (count >= ((currentPhase - (currentPhase / 9)) / 1000)) {
					count = 0;
					if ((laneId.contains("1") || laneId.contains("2"))) {
						if (currentPhase == 39000) {
							currentPhase = 9000;
						} else if (currentPhase > 6000) {
							tlight.changeLightsState(new TLState("yryr"));
							currentPhase = 6000;
						} else if (currentPhase == 6000) {
							tlight.changeLightsState(new TLState(states[0])); // rGrG
							currentPhase = 39000;

						}

					} else if (laneId.contains("3") || laneId.contains("4")) {
						if (currentPhase == 10000) {

							currentPhase = 9000;
						} else if (currentPhase > 6000) {
							tlight.changeLightsState(new TLState("ryry"));
							currentPhase = 6000;
						} else if (currentPhase == 6000) {
							tlight.changeLightsState(new TLState(states[2])); // GrGr
							currentPhase = 10000;
						}
					}
					tlight.changePhaseDuration(currentPhase);
				}

				conn.nextSimStep();

			}

		} catch (Exception e) {
			e.printStackTrace();

		}

	}

}
