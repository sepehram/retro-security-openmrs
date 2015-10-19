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
package org.openmrs.module.retrosecurity.advice;

import java.lang.reflect.Method;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.springframework.aop.AfterReturningAdvice;
import org.springframework.aop.MethodBeforeAdvice;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import java.io.*;
import java.util.*;

import com.declarativa.interprolog.*;
import com.xsb.interprolog.*;

import org.openmrs.User;

import org.openmrs.module.retrosecurity.api.*;


/**
 * AOP class used to intercept and log calls to PatientService methods
 */
public class PatientServiceAdvice implements MethodBeforeAdvice {
	
	protected static final Log log = LogFactory.getLog(PatientServiceAdvice.class);
	static int ts = 0;
	static final int precision = 5; // no. of digits to represent time stamp
	static String modulePath = "/home/sep/Desktop/test_rep/retrosecurity";
	static String moduleSourcePath = modulePath + "/api/src/main/java/org/openmrs/module/retrosecurity";
	static String logFilePath = moduleSourcePath + "/storage/log1.txt";
	static String G = "loggedfunccall(X0,X1,X2)";	// e.g., "loggedfunccall(X,Y,Z)";
	static String T = "(X0,X1,X2)";	//e.g., "(X,Y,Z)";
	//static String GG = "findall(TM, ("+G+",buildTermModel("+T+",TM)), L), ipObjectSpec('ArrayOfObject',L,LM)";
	static Object[] solutions = new Object [100];
	User currentUser = Context.getAuthenticatedUser();
	//static int [] triggerInvocationCount = new int [0];

	boolean memLeakProtection = true;
	ArrayList<String> w = new ArrayList<String>();
	String GG1;

	static EngineCommunication ec;
	static LoggedCallDerivationListener lcdl;			

	
	/**
	 * @see org.springframework.aop.MethodBeforeAdvice#before(Method, Object[], Object)
	 */
	public void before(Method method, Object[] args, Object target) throws Throwable {
		User currentUser = Context.getAuthenticatedUser();
		/*logName("Method " + method.getName() + " called at time " + preciseTimestamp(precision, ts) + ". sol length:" + solutions.length );
		logName("Args: ");
		for(int i=0; i < args.length; i++){
			logName("" + args[i]);
		}*/
		if (ts == 0){ // the first time invoked: run the engine thread
			ec = new EngineCommunication();
			lcdl = new LoggedCallDerivationListener();
			ec.addListener(lcdl);
		}

		if (memLeakProtection) {
			if (method.getName().equals("getAllergies")){
				ec.addFact("assert(funccall("  + preciseTimestamp(precision, ts) + ", "  + method.getName() + ", " + currentUser  + "))"); 
			}
			if (method.getName().equals("breakTheGlass")){
				if (!w.contains("breakTheGlass")){
					ec.addFact("assert(funccall("  + preciseTimestamp(precision, ts) + ", "  + method.getName() + ", " + currentUser  + "))"); 
					int solutionLen = ec.query("funccall(T1, breakTheGlass, U1), funccall(T2, getCountOfPatients, U2, X2), @<(T1, T2), @=(U1, admin)", "(X2,T1,U2,T2,U1)");
					if (solutionLen > 0) {
						w.add("breakTheGlass");
						w.add("getCountOfPatients");
					}
				}
			}
			if (method.getName().equals("getCountOfPatients")){
				if (!w.contains("getCountOfPatients")){
					ec.addFact("assert(funccall("  + preciseTimestamp(precision, ts) + ", "  + method.getName() + ", " + currentUser  + ", " + args[0] + "))"); 
					int solutionLen = ec.query("funccall(T1, breakTheGlass, U1), funccall(T2, getCountOfPatients, U2, X2), @<(T1, T2), @=(U1, admin)", "(X2,T1,U2,T2,U1)");
					if (solutionLen > 0) {
						w.add("breakTheGlass");
						w.add("getCountOfPatients");
					}
				}
			}
			if (method.getName().equals("getPatients")){
				ec.addFact("assert(funccall("  + preciseTimestamp(precision, ts) + ", "  + method.getName() + ", " + currentUser  + ", " + args[0] + ", " + args[1] + ", " + args[2] + "))"); 
			}
			if (method.getName().equals("getPatientOrPromotePerson")){
				ec.addFact("assert(funccall("  + preciseTimestamp(precision, ts) + ", "  + method.getName() + ", " + currentUser  + ", " + args[0] + "))"); 
			}
			if (method.getName().equals("getAllPatientIdentifierTypes")){
				if (!w.contains("getAllPatientIdentifierTypes")){
					ec.addFact("assert(funccall("  + preciseTimestamp(precision, ts) + ", "  + method.getName() + ", " + currentUser  + "))"); 
					w.add("getAllPatientIdentifierTypes");
				}
			}
			if (method.getName().equals("getPatient")){
				if (!w.contains("getPatient")){
					ec.addFact("assert(funccall("  + preciseTimestamp(precision, ts) + ", "  + method.getName() + ", " + currentUser  + ", " + args[0] + "))"); 
					int solutionLen = ec.query("funccall(T6, getPatient, U6, X6), @=(admin, U6)", "(T6,X6,U6)");
					if (solutionLen > 0) {
						w.add("getPatient");
					}
					else{
						ec.addFact("retract(funccall("  + preciseTimestamp(precision, ts) + ", "  + method.getName() + ", " + currentUser  + ", " + args[0] + "))");
					}
				}
			}
			
		} 
		else {
			if (method.getName().equals("getAllergies")){
				ec.addFact("assert(funccall("  + preciseTimestamp(precision, ts) + ", "  + method.getName() + ", " + currentUser  + "))"); 
			}
			if (method.getName().equals("breakTheGlass")){
				ec.addFact("assert(funccall("  + preciseTimestamp(precision, ts) + ", "  + method.getName() + ", " + currentUser  + "))"); 
			}
			if (method.getName().equals("getCountOfPatients")){
				ec.addFact("assert(funccall("  + preciseTimestamp(precision, ts) + ", "  + method.getName() + ", " + currentUser  + ", " + args[0] + "))"); 
			}
			if (method.getName().equals("getPatients")){
				ec.addFact("assert(funccall("  + preciseTimestamp(precision, ts) + ", "  + method.getName() + ", " + currentUser  + ", " + args[0] + ", " + args[1] + ", " + args[2] + "))"); 
			}
			if (method.getName().equals("getPatientOrPromotePerson")){
				ec.addFact("assert(funccall("  + preciseTimestamp(precision, ts) + ", "  + method.getName() + ", " + currentUser  + ", " + args[0] + "))"); 
			}
			if (method.getName().equals("getAllPatientIdentifierTypes")){
				ec.addFact("assert(funccall("  + preciseTimestamp(precision, ts) + ", "  + method.getName() + ", " + currentUser  + "))"); 
			}
			if (method.getName().equals("getPatient")){
				ec.addFact("assert(funccall("  + preciseTimestamp(precision, ts) + ", "  + method.getName() + ", " + currentUser  + ", " + args[0] + "))"); 
			}
			
		}
		
			

		if (method.getName().equals("queryLog")){
			ec.query(G, T);
			logName("No. of assertions in db: " + ec.getAssertionCount());
		}
		
		ts++;
	}
	
	/*
	public Object invoke(MethodInvocation invocation) throws Throwable {
		Object rval = new Object();
		solutions = new Object [100];
		logName(invocation.getMethod().getName());
		if (invocation.getMethod().getName().equals("queryLog")){
			rval = invocation.proceed();
			solutions = (Object[])engine.deterministicGoal(GG,"[LM]")[0];
			logName("sol length: " + solutions.length);
			for(int i=0; i < solutions.length; i++) logName("Solution "+i+":  "+solutions[i]);

			return solutions;
		}
		solutions = null;
		return rval;
	}
	*/

	/*append to file*/
	private void logName(String st)  throws IOException {
		PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(logFilePath, true)));
		out.println(st);
		out.close();
	}

	/*write to file*/
	private void writeFile(String st) throws IOException {
		PrintWriter writer = new PrintWriter(logFilePath, "UTF-8");
		writer.println(st);
		writer.close();
	}

	private String preciseTimestamp (int prcsn, int timestamp){
		String pts = "t";
		for(int i = 0; i < prcsn - ("" + timestamp).length(); i++){
			pts = pts + "0";
		}
		pts = pts + timestamp;
		return pts;
	}

}

