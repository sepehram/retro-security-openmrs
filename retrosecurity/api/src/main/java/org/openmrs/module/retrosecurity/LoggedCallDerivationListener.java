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


public class LoggedCallDerivationListener implements DerivationListener {
	
	static String G = "loggedfunccall(X0,X1,X2)";
	static String T = "(X0,X1,X2)";
	static String modulePath = "/home/sep/Desktop/test_rep/retrosecurity";
	static String moduleSourcePath = modulePath + "/api/src/main/java/org/openmrs/module/retrosecurity";
	static String derivationStoragePath = moduleSourcePath + "/storage/ds.txt";
	private Object[] solutions = new Object [100];
	private List<String> solutionList = new ArrayList<String>();
	private int priorSolutionListLength = 0;
	private String GG;


	public LoggedCallDerivationListener() {
		GG = "findall(TM, ("+G+",buildTermModel("+T+",TM)), L), ipObjectSpec('ArrayOfObject',L,LM)";
	}

	@Override
	public void factAdded(XSBSubprocessEngine engine) {
		//String GG = "findall(TM, ("+G+",buildTermModel("+T+",TM)), L), ipObjectSpec('ArrayOfObject',L,LM)";
		solutions = (Object[]) engine.deterministicGoal(GG,"[LM]")[0];
		buildSolutionList();
		if (solutionList.size() > priorSolutionListLength) {
			String ssl = solutionListToString();
			try {
				writeFile(ssl);
			} catch(IOException e) {
				System.err.println("Error writing to " + derivationStoragePath);
			}
			priorSolutionListLength = solutionList.size();
		}
	}

	private void buildSolutionList() {
		for(int i=0; i < solutions.length; i++) {
			if (!solutionList.contains(solutions[i].toString())) {
				solutionList.add(solutions[i].toString());
			}
		}
	}

	private String solutionListToString() {
		String ssl = "";
		for(int i=0; i < solutionList.size(); i++) {
			ssl += "(" + solutionList.get(i) + ")\n\n";
		}
		return ssl;
	}
	
	/* write to file */
	private void writeFile(String st) throws IOException {
		PrintWriter writer = new PrintWriter(derivationStoragePath, "UTF-8");
		writer.println(st);
		writer.close();
	}
}


