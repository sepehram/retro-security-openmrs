/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.retrosecurity.api;


import java.io.*;
import java.util.*;

import com.declarativa.interprolog.*;
import com.xsb.interprolog.*;


public class EngineCommunication implements EngineInterface, Runnable{
	private String G;
	private String T;
	private String GG;
	private Object[] solutions = new Object [100];
	private static XSBSubprocessEngine engine; 
	static String modulePath = "/home/sep/Desktop/test_rep/retrosecurity";
	static String moduleSourcePath = modulePath + "/api/src/main/java/org/openmrs/module/retrosecurity";
	static String logFilePath = moduleSourcePath + "/storage/log1.txt";
	static String queryResultPath = moduleSourcePath + "/storage/qr.txt";

	private int assertionCount = 0;

	private List<DerivationListener> listeners = new ArrayList<DerivationListener>();


	public EngineCommunication() {
		engine = new XSBSubprocessEngine("/home/sep/Downloads/XSB/bin");

		addFact("dynamic loggedfunccall/3 as incremental"); 
		addFact("dynamic funccall/3 as incremental"); 
		addFact("dynamic funccall/3 as incremental"); 
		addFact("dynamic funccall/4 as incremental"); 
		addFact("dynamic funccall/6 as incremental"); 
		addFact("dynamic funccall/4 as incremental"); 
		addFact("dynamic funccall/3 as incremental"); 
		addFact("dynamic funccall/4 as incremental"); 
		
		addFact("assert((loggedfunccall(T0, getAllergies, U0) :- funccall(T0, getAllergies, U0), funccall(T1, breakTheGlass, U1), @<(T1, T0), funccall(T2, getCountOfPatients, U2, X2), @<(T2, T0), funccall(T3, getPatients, U3, X3, Y3, Z3), @<(T3, T0), funccall(T4, getPatientOrPromotePerson, U4, X4), @<(T4, T0), funccall(T5, getAllPatientIdentifierTypes, U5), @<(T5, T0), funccall(T6, getPatient, U6, X6), @<(T6, T0), @<(T1, T2), @=(U1, admin), @<(X4, Z3), @=(U4, admin), @=(U0, U4), @=(admin, U6)))"); 
		
	
		new Thread(this, "EngineT").start();
	}


	public void run() {
		try{logName("run() is called.");} catch (IOException e){System.err.println("err");}
	}
	

	public int query(String queryMsg, String queryResponseFormat) throws Throwable{
		logName("query() called.");
		setGoal(queryMsg, queryResponseFormat);
        	return resolve();
	}

	private int resolve() throws Throwable{
		logName("resolve() called.");
		solutions = (Object[]) engine.deterministicGoal(GG,"[LM]")[0];
		String queryResult = "Solution length: " + solutions.length + ".\n\n";
		for(int i=0; i < solutions.length; i++) queryResult += "(" + solutions[i] + ")\n\n";
		writeFile(queryResult);
		return solutions.length;
	}


	public void addFact(String assertion) {
		engine.command(assertion);
		updateAssertionCount(assertion);
		if (assertion.startsWith("assert")) {
			notifyListeners();
		}
	}	

	private void updateAssertionCount(String assertion) {
		if (assertion.startsWith("assert")) {
			assertionCount++;
		}
		if (assertion.startsWith("retract")) {
			assertionCount--;
		}
		return;		
	}

	public int getAssertionCount(){
		return assertionCount;
	}
	

	private void setGoal(String goal, String queryResponseFormat) {
		this.G = goal;
		this.T = queryResponseFormat;
		this.GG = "findall(TM, ("+G+",buildTermModel("+T+",TM)), L), ipObjectSpec('ArrayOfObject',L,LM)";
		return;
	}


	public void addListener(DerivationListener toAdd) {
		listeners.add(toAdd);
	}

	private void notifyListeners() {
		// Notify everybody that may be interested.
		for (DerivationListener dl : listeners)
			dl.factAdded(engine);
	}


	/*write to file*/
	private void writeFile(String st) throws IOException {
		PrintWriter writer = new PrintWriter(queryResultPath, "UTF-8");
		writer.println(st);
		writer.close();
	}

	/*append to file*/
	private void logName(String st)  throws IOException {
		PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(logFilePath, true)));
		out.println(st);
		out.close();
	}
}

