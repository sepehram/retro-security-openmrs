import org.json.*;
import java.io.*;
import java.util.*;

public class AdviceRewriter {

	static String jsonFile = "./json/LS.json";
	static String configFile = "./config/config";
	static String configTemplateFile = "./templates/config_template";
	static String engineCommTemplateFile = "./templates/engine_comm_template";
	static String adviceTemplateFile = "./templates/advice_template";
	static String loggedDerivationTemplateFile = "./templates/logged_derivation_template";
	static String retroSecServiceImplTemplateFile = "./templates/retro_sec_service_impl_template";

	String modulePath;
	String xsbBinPath;
	String goalVars;	//e.g., (X,Y,Z) for loggedfunccall, it is set in assetClauses()

		

	JSONObject jsonWholeObj;
	JSONArray logprogObj;
	JSONObject [] fullHornClauseObj;
	String [] hornClauseType;
	JSONObject [] mainHornClauseObj;
	JSONObject [] fullHeadObj;
	String [] headType;
	JSONObject [] headLiteralObj;
	String [] headLiteralSymbolType;
	String [] headLiteralType;
	String [] headLiteralName;
	JSONArray headLiteralArgsObj;
	JSONObject [] [] headLiteralArgObj;
	String [] [] headLiteralArgType;
	String [] [] headLiteralArgName;
	JSONArray bodyObj;
	JSONObject [] [] bodyLiteralObj;
	String [] [] bodyLiteralSymbolType;
	String [] [] bodyLiteralType;
	String [] [] bodyLiteralName;
	JSONArray bodyLiteralArgsObj;
	JSONObject [] [] [] bodyLiteralArgObj;
	String [] [] [] bodyLiteralArgType;
	String [] [] [] bodyLiteralArgName;

	int triggerCount;
	ArrayList<String> triggerArgs;
	ArrayList<String> [] extraConditionArgs;
	ArrayList<String> advicePoints; // list of advice points to be wriiten in config.xml

	public static void main(String args[]) throws IOException{
		new AdviceRewriter();		
	}

	public AdviceRewriter () throws IOException{
		// init data
		initializeData();
		
		// extract advice points and update corr. argument names
		extractAdvicePoints();
		

		// write the config by replacing <ADVICES> in its template
		writeConfigXML();
		
		
		// write the advice file beside other classes
		writeAdviceFile();

		// write templates with paths from config
		writePaths();	

	}
		
	static String readFile(String fileName) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(fileName));
		try {
			StringBuilder sb = new StringBuilder();
			String line = br.readLine();

			while (line != null) {
				sb.append(line);
				sb.append("\n");
				line = br.readLine();
			}
			return sb.toString();
		} finally {
        		br.close();
    		}
	}
	
	private static void writeFile(String fileName, String content)  throws IOException {
		PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(fileName, true)));
		out.println(content);
		out.close();
	}


	private void initializeData() throws IOException{
		/* read the json file */
		String jsonContent = readFile(jsonFile);
		/* parse the json file*/
		jsonWholeObj = new JSONObject(jsonContent);
		logprogObj = jsonWholeObj.getJSONArray("logprog");
		fullHornClauseObj = new JSONObject [logprogObj.length()];
		hornClauseType = new String [logprogObj.length()];
		mainHornClauseObj = new JSONObject [logprogObj.length()];
		fullHeadObj = new JSONObject [logprogObj.length()];
		headType = new String [logprogObj.length()];
		headLiteralObj = new JSONObject [logprogObj.length()];
		headLiteralSymbolType = new String [logprogObj.length()];
		headLiteralType = new String [logprogObj.length()];
		headLiteralName = new String [logprogObj.length()];
		headLiteralArgObj = new JSONObject [logprogObj.length()] [];
		headLiteralArgType = new String [logprogObj.length()] [];
		headLiteralArgName = new String [logprogObj.length()] [];
		bodyLiteralObj = new JSONObject [logprogObj.length()] [];
		bodyLiteralSymbolType = new String [logprogObj.length()] [];
		bodyLiteralType = new String [logprogObj.length()] [];
		bodyLiteralName = new String [logprogObj.length()] [];
		bodyLiteralArgObj = new JSONObject [logprogObj.length()] [] [];
		bodyLiteralArgType = new String [logprogObj.length()] [] [];
		bodyLiteralArgName = new String [logprogObj.length()] [] [];

		triggerCount = 0;
		triggerArgs = new ArrayList<String>();
		extraConditionArgs = new ArrayList [logprogObj.length()];
		advicePoints = new ArrayList<String>(); // list of advice points to be wriiten in config.xml

		for (int i = 0; i < logprogObj.length(); i++){
			fullHornClauseObj[i] = logprogObj.getJSONObject(i);
			hornClauseType[i] = fullHornClauseObj[i].getString("hc_type");
			mainHornClauseObj[i] = fullHornClauseObj[i].getJSONObject("horn_clause");
			fullHeadObj[i] = mainHornClauseObj[i].getJSONObject("head");
			headType[i] = fullHeadObj[i].getString("head_type");
			headLiteralObj[i] = fullHeadObj[i].getJSONObject("literal");
			headLiteralSymbolType[i] = headLiteralObj[i].getString("symbol_type");
			headLiteralType[i] = headLiteralObj[i].getString("literal_type");
			headLiteralName[i] = headLiteralObj[i].getString("literal_name");
			headLiteralArgsObj = headLiteralObj[i].getJSONArray("args");
			headLiteralArgObj[i] = new JSONObject [headLiteralArgsObj.length()];
			headLiteralArgType[i] = new String [headLiteralArgsObj.length()];
			headLiteralArgName[i] = new String [headLiteralArgsObj.length()];
			for (int j = 0; j < headLiteralArgsObj.length(); j++){
				headLiteralArgObj[i][j] = headLiteralArgsObj.getJSONObject(j);
				headLiteralArgType[i][j] = headLiteralArgObj[i][j].getJSONObject("arg").getString("arg_type");
				headLiteralArgName[i][j] = headLiteralArgObj[i][j].getJSONObject("arg").getString("arg_name");
			}
			bodyObj = mainHornClauseObj[i].getJSONArray("body");
			bodyLiteralObj[i] = new JSONObject [bodyObj.length()];
			bodyLiteralSymbolType[i] = new String [bodyObj.length()];
			bodyLiteralType[i] = new String [bodyObj.length()];
			bodyLiteralName[i] = new String [bodyObj.length()];
			bodyLiteralArgObj[i] = new JSONObject [bodyObj.length()] [];
			bodyLiteralArgType[i] = new String [bodyObj.length()] [];
			bodyLiteralArgName[i] = new String [bodyObj.length()] [];
			for (int k = 0; k < bodyObj.length(); k++){
				bodyLiteralObj[i][k] = bodyObj.getJSONObject(k).getJSONObject("literal");
				bodyLiteralSymbolType[i][k] = bodyLiteralObj[i][k].getString("symbol_type");
				bodyLiteralType[i][k] = bodyLiteralObj[i][k].getString("literal_type");
				bodyLiteralName[i][k] = bodyLiteralObj[i][k].getString("literal_name");
				bodyLiteralArgsObj = bodyLiteralObj[i][k].getJSONArray("args");
				bodyLiteralArgObj[i][k] = new JSONObject [bodyLiteralArgsObj.length()];
				bodyLiteralArgType[i][k] = new String [bodyLiteralArgsObj.length()];
				bodyLiteralArgName[i][k] = new String [bodyLiteralArgsObj.length()];
				for (int j = 0; j < bodyLiteralArgsObj.length(); j++){
					bodyLiteralArgObj[i][k][j] = bodyLiteralArgsObj.getJSONObject(j);
					bodyLiteralArgType[i][k][j] = bodyLiteralArgObj[i][k][j].getJSONObject("arg").getString("arg_type");
					bodyLiteralArgName[i][k][j] = bodyLiteralArgObj[i][k][j].getJSONObject("arg").getString("arg_name");
				}
			}
		}
		
	}

	
	private void extractAdvicePoints(){
		for(int i = 0; i < fullHornClauseObj.length; i++){
			if (hornClauseType[i].equals("log_spec")){ // for all logging specs
				for (int k = 0; k < bodyLiteralName[i].length; k++){
					if (bodyLiteralName[i][k].equals("funccall")){ // if the body literal is an event or trigger
						// extract the advice point from the method path
						String advicePoint = bodyLiteralArgName[i][k][1].substring(0, bodyLiteralArgName[i][k][1].indexOf("#"));
						// add the advice point to the list of advice points
						if (!advicePoints.contains(advicePoint)){
							advicePoints.add(advicePoint);
						}
						// drop the path from method and just keep the name
						bodyLiteralArgName[i][k][1] = bodyLiteralArgName[i][k][1].substring(bodyLiteralArgName[i][k][1].indexOf("#") + 1, bodyLiteralArgName[i][k][1].length());
						
					}
				}
			}
		}
	}

	
	private void writeConfigXML() throws IOException{
		String configTemplate = readFile(configTemplateFile);
		String rewrittenConfig;

		String define_advice_points = "";

		for (int i = 0; i < advicePoints.size(); i++) {
			define_advice_points += "<advice>\n\t\t<point>" + advicePoints.get(i) + "</point>\n\t\t<class>org.openmrs.module.retrosecurity.advice.PatientServiceAdvice</class>\n\t</advice>\n\n\t";
		}
		rewrittenConfig = configTemplate.replace("<ADVICES>", define_advice_points);
		writeFile("./output/config.xml", rewrittenConfig);
	}


	private void writeAdviceFile() throws IOException {
		String engineCommTemplate = readFile(engineCommTemplateFile);
		String rewrittenEngineComm;
 
		String adviceTemplate = readFile(adviceTemplateFile);
		String rewrittenAdvice;

		String loggedDerivationTemplate = readFile(loggedDerivationTemplateFile);
		String rewrittenLoggedDerivation;

		String retroSecServiceImplTemplate = readFile(retroSecServiceImplTemplateFile);
		String rewrittenRetroSecServiceImpl;


		/* define the predicates */
		rewrittenEngineComm = engineCommTemplate.replace("<DEF_PREDS>", definePredicates());		

		/* assert the horn clauses and list extra condtion variables*/
		rewrittenEngineComm = rewrittenEngineComm.replace("<ASSERT_CLAUSES>", assertClauses());

		rewrittenAdvice = adviceTemplate.replace("<TRIGGER_COUNT>", "" + triggerCount);

		/* check trigger invocations and their potential args */
		rewrittenAdvice = rewrittenAdvice.replace("<CALL_CHECKS1>", checkInvocations1());
		rewrittenAdvice = rewrittenAdvice.replace("<CALL_CHECKS2>", checkInvocations2());

		/* define paths to module and xsb bin and write them */
		writePaths();

		rewrittenEngineComm = rewrittenEngineComm.replace("<MODULE_PATH>", "\""+ modulePath + "\"");
		rewrittenEngineComm = rewrittenEngineComm.replace("<XSB_PATH>", "\""+ xsbBinPath + "\"");
		rewrittenAdvice = rewrittenAdvice.replace("<MODULE_PATH>", "\""+ modulePath + "\"");
		rewrittenLoggedDerivation = loggedDerivationTemplate.replace("<MODULE_PATH>", "\""+ modulePath + "\"");
		rewrittenRetroSecServiceImpl = retroSecServiceImplTemplate.replace("<MODULE_PATH>", "\""+ modulePath + "\"");


		/* define goals for listeners and instant querying */
		rewrittenAdvice = rewrittenAdvice.replace("<GOAL>", "\"loggedfunccall"+ goalVars + "\"");
		rewrittenAdvice = rewrittenAdvice.replace("<GOAL_VARS>", "\""+ goalVars + "\"");
		rewrittenLoggedDerivation = rewrittenLoggedDerivation.replace("<GOAL>", "\"loggedfunccall"+ goalVars + "\"");
		rewrittenLoggedDerivation = rewrittenLoggedDerivation.replace("<GOAL_VARS>", "\""+ goalVars + "\"");

		
		/* generate the files in output dir */
		writeFile("./output/EngineCommunication.java", rewrittenEngineComm);
		writeFile("./output/PatientServiceAdvice.java", rewrittenAdvice);
		writeFile("./output/LoggedCallDerivationListener.java", rewrittenLoggedDerivation);
		writeFile("./output/RetroSecurityServiceImpl.java", rewrittenRetroSecServiceImpl);
	}


	private String definePredicates() {
		String define_predicates = "";
		for(int i = 0; i < headLiteralName.length; i++){
			if (headLiteralSymbolType[i].equals("user_defined")){
				define_predicates += "addFact(\"dynamic " + headLiteralName[i] + "/" + headLiteralArgName[i].length +" as incremental\"); \n\t\t";
			}
			
			for (int k = 0; k < bodyLiteralName[i].length; k++){
				if (bodyLiteralSymbolType[i][k].equals("user_defined")){
					define_predicates += "addFact(\"dynamic " + bodyLiteralName[i][k] + "/" + bodyLiteralArgName[i][k].length +" as incremental\"); \n\t\t";
				}				
			}
		}

		return define_predicates;
	}

	
	private String assertClauses() {
		String assert_clauses = "";
		for(int i = 0; i < headLiteralName.length; i++){
			assert_clauses += "addFact(\"assert(("; 
			if (headType[i].equals("pred")){
				assert_clauses += headLiteralName[i] + "(";
				for (int j = 0; j < headLiteralArgName[i].length; j++){
					assert_clauses += headLiteralArgName[i][j];
					if (j != headLiteralArgName[i].length - 1) { // if not the last arg, put a comma
						assert_clauses += ", ";
					}
				}
				assert_clauses += ")";
				
			} else { // head type is empty!
				assert_clauses += "false";
			} 
			extraConditionArgs[i] = new ArrayList<String>();
			if (bodyLiteralName[i].length != 0){ // if body is not empty
				assert_clauses += " :- ";
				for (int k = 0; k < bodyLiteralName[i].length; k++){
					assert_clauses += bodyLiteralName[i][k] + "(";
					for (int j = 0; j < bodyLiteralArgName[i][k].length; j++){
						assert_clauses += bodyLiteralArgName[i][k][j];
						if (j != bodyLiteralArgName[i][k].length - 1) { // if not the last arg, put a comma
							assert_clauses += ", ";
						}
					}
					assert_clauses += ")";
					if (k != bodyLiteralName[i].length - 1){ // if not the last body literal, put a comma (conj)
						assert_clauses += ", ";
					}
					// set the goal and goal vars
					if (headLiteralName[i].equals("loggedfunccall")) {
						goalVars = "(";
						for (int j = 0; j < headLiteralArgName[i].length; j++){
							goalVars += "X" + j;
							if (j != headLiteralArgName[i].length - 1) { // if not the last arg, put a comma
								goalVars += ",";
							}
						}
						goalVars += ")";
					}
					/*
					// count the number of triggers
					if (headLiteralName[i].equals("loggedfunccall") && bodyLiteralName[i][k].equals("funccall")
						&& !headLiteralArgName[i][1].equals(bodyLiteralArgName[i][k][1])){
						triggerCount++;
					}
					// list variables of extra condition predicates
					if (bodyLiteralType[i][k].equals("extra_cond")){
						for(int j = 0; j < bodyLiteralArgName[i][k].length; j++){
							if (bodyLiteralArgType[i][k][j].equals("var")){
								extraConditionArgs[i].add(bodyLiteralArgName[i][k][j]);
							}
						}
					}
					*/
				}
			}		
			assert_clauses += "))\"); \n\t\t";
		}
		return assert_clauses;	
	}


	private String checkInvocations1() {
		String check_calls = "";
		String potential_args = "";

		String funccallTimestampVar = "";
		String funccallMethodName = "";
		ArrayList<JSONObject> corrPredicateClass = new ArrayList<JSONObject>();
		String corrPredicateClassConjunct = "";
		ArrayList<String> corrTriggersOfPredicateClass = new ArrayList<String>();
		ArrayList<String> classFV = new ArrayList<String> ();
		String corrIQRF = "";
		int numberOfFV = 0;
		String logEventTimestamp = "";

		//int tc = 0;
		for(int i = 0; i < fullHornClauseObj.length; i++){
			if (hornClauseType[i].equals("log_spec")){ // for all logging specs
				logEventTimestamp = headLiteralArgName[i][0];
				for (int k = 0; k < bodyLiteralName[i].length; k++){
					potential_args = "";
					if (bodyLiteralName[i][k].equals("funccall")){ // if the body literal is an event or trigger
						funccallTimestampVar = bodyLiteralArgName[i][k][0];
						funccallMethodName = bodyLiteralArgName[i][k][1];
						corrPredicateClass = predicateClass (funccallTimestampVar);
						corrPredicateClassConjunct = predicateClassConjunct (funccallTimestampVar);
						corrTriggersOfPredicateClass = triggersOfPredicateClass (funccallTimestampVar);
						classFV = classOfFV(funccallTimestampVar);
						corrIQRF = intermediateQueryResponseFormat(funccallTimestampVar);
						numberOfFV = bodyLiteralArgName[i][k].length - 1; //excluding the method name (not var)
						if (isLogEvent(funccallMethodName)){ //RelevantCall-8
							check_calls += "if (method.getName().equals(\"" + funccallMethodName + "\")){\n\t\t\t\t" + "ec.addFact(\"assert(funccall(\"  + preciseTimestamp(precision, ts) + \", \"  + method.getName() + \", \" + currentUser <POTENTIAL_ARGS> + \"))\"); \n\t\t\t}\n\t\t\t";
						}
						else if (isTrigger(funccallMethodName)) { 
							if (corrPredicateClass.size() == 1) { //RelevantCall-2
								check_calls += "if (method.getName().equals(\"" + funccallMethodName + "\")){\n\t\t\t\t" + "if (!w.contains(\"" + funccallMethodName +"\")){\n\t\t\t\t\t" + "ec.addFact(\"assert(funccall(\"  + preciseTimestamp(precision, ts) + \", \"  + method.getName() + \", \" + currentUser <POTENTIAL_ARGS> + \"))\"); \n\t\t\t\t\tw.add(\"" + funccallMethodName + "\");\n\t\t\t\t}\n\t\t\t}\n\t\t\t";
							}
							else { // corrPredicateClass.size() > 1
								if (classFV.size() == numberOfFV) { // RelevantCall-3 and RelevantCall-4
									check_calls += "if (method.getName().equals(\"" + funccallMethodName + "\")){\n\t\t\t\t" + "if (!w.contains(\"" + funccallMethodName +"\")){\n\t\t\t\t\t" + "ec.addFact(\"assert(funccall(\"  + preciseTimestamp(precision, ts) + \", \"  + method.getName() + \", \" + currentUser <POTENTIAL_ARGS> + \"))\"); \n\t\t\t\t\tint solutionLen = ec.query(\"" + corrPredicateClassConjunct + "\", \"" + corrIQRF + "\");\n\t\t\t\t\tif (solutionLen > 0) {\n\t\t\t\t\t\tw.add(\"" + funccallMethodName + "\");\n\t\t\t\t\t}\n\t\t\t\t\telse{\n\t\t\t\t\t\tec.addFact(\"retract(funccall(\"  + preciseTimestamp(precision, ts) + \", \"  + method.getName() + \", \" + currentUser <POTENTIAL_ARGS> + \"))\");\n\t\t\t\t\t}\n\t\t\t\t}\n\t\t\t}\n\t\t\t";
								}
								else{
									if (!classFV.contains(logEventTimestamp)) { // RelevantCall-5 and 6
										check_calls += "if (method.getName().equals(\"" + funccallMethodName + "\")){\n\t\t\t\t" + "if (!w.contains(\"" + funccallMethodName +"\")){\n\t\t\t\t\t" + "ec.addFact(\"assert(funccall(\"  + preciseTimestamp(precision, ts) + \", \"  + method.getName() + \", \" + currentUser <POTENTIAL_ARGS> + \"))\"); \n\t\t\t\t\tint solutionLen = ec.query(\"" + corrPredicateClassConjunct + "\", \"" + corrIQRF + "\");\n\t\t\t\t\tif (solutionLen > 0) {";
										for(int l = 0; l < corrTriggersOfPredicateClass.size(); l++){
											check_calls += "\n\t\t\t\t\t\tw.add(\"" + corrTriggersOfPredicateClass.get(l) + "\");";
										}

										check_calls += "\n\t\t\t\t\t}\n\t\t\t\t}\n\t\t\t}\n\t\t\t";
									}
									else{ // RelevantCall-7
										check_calls += "if (method.getName().equals(\"" + funccallMethodName + "\")){\n\t\t\t\t" + "ec.addFact(\"assert(funccall(\"  + preciseTimestamp(precision, ts) + \", \"  + method.getName() + \", \" + currentUser <POTENTIAL_ARGS> + \"))\"); \n\t\t\t}\n\t\t\t";
									}
								}
							}
						}

						// put the args						
						if (bodyLiteralArgName[i][k].length > 3){ // if the body literal comes with args for invocation
							for(int l = 0; l < bodyLiteralArgName[i][k].length - 3; l++){
								potential_args += " + \", \" + args[" + l + "]";
							}
						}
						check_calls = check_calls.replace("<POTENTIAL_ARGS>", potential_args);
						/*
						if (!headLiteralArgName[i][1].equals(bodyLiteralArgName[i][k][1])){ //if literal is trigger
							// list variables in the trigger predicate
							for (int j = 0; j < bodyLiteralArgName[i][k].length; j++){
								if (bodyLiteralArgType[i][k][j].equals("var")){
									triggerArgs.add(bodyLiteralArgName[i][k][j]);
								}
							}							
							if (Collections.disjoint(triggerArgs, extraConditionArgs[i])) {
								
								check_calls = check_calls.replace("<CHECK_TRIGGER_INVOCATION_COUNT>", " && (triggerInvocationCount[" + tc +"] == 0)");
								check_calls = check_calls.replace("<UPDATE_TRIGGER_INVOCATION_COUNT>", "\n\t\t\ttriggerInvocationCount[" + tc + "]++;");
							}
							else{
								check_calls = check_calls.replace("<CHECK_TRIGGER_INVOCATION_COUNT>", "");
								check_calls = check_calls.replace("<UPDATE_TRIGGER_INVOCATION_COUNT>", "");
							}
							tc++;
						}
						else{ // literal is logging event
							check_calls = check_calls.replace("<CHECK_TRIGGER_INVOCATION_COUNT>", "");
							check_calls = check_calls.replace("<UPDATE_TRIGGER_INVOCATION_COUNT>", "");
						}
						triggerArgs.clear();
						*/
					}
				}
			}
		}
		return check_calls;
	}

	
	private String checkInvocations2() {
		String check_calls = "";
		String potential_args = "";

		for(int i = 0; i < fullHornClauseObj.length; i++){
			if (hornClauseType[i].equals("log_spec")){ // for all logging specs
				for (int k = 0; k < bodyLiteralName[i].length; k++){
					potential_args = "";
					if (bodyLiteralName[i][k].equals("funccall")){ // if the body literal is an event or trigger
						check_calls += "if (method.getName().equals(\"" + bodyLiteralArgName[i][k][1] + "\")){\n\t\t\t\t" + "ec.addFact(\"assert(funccall(\"  + preciseTimestamp(precision, ts) + \", \"  + method.getName() + \", \" + currentUser <POTENTIAL_ARGS> + \"))\"); \n\t\t\t}\n\t\t\t";

						// put the args						
						if (bodyLiteralArgName[i][k].length > 3){ // if the body literal comes with args for invocation
							for(int l = 0; l < bodyLiteralArgName[i][k].length - 3; l++){
								potential_args += " + \", \" + args[" + l + "]";
							}
						}
						check_calls = check_calls.replace("<POTENTIAL_ARGS>", potential_args);
					}
				}
			}
		}
		return check_calls;
	}



	public ArrayList<String> union(List<String> list1, List<String> list2) {
		Set<String> set = new HashSet<String>();
		set.addAll(list1);
		set.addAll(list2);
		return new ArrayList<String>(set);
	}

	public List<String> intersection(List<String> list1, List<String> list2) {
		List<String> list = new ArrayList<String>();
		for (String t : list1) {
			if(list2.contains(t)) {
				list.add(t);
			}
		}
		return list;
	}

	
	// returns the list of free variables in body literal object [i, k]
	private ArrayList<String> literalFV(int i, int k) {
		ArrayList<String> triggerFV = new ArrayList<String>();
		for (int j = 0; j < bodyLiteralArgName[i][k].length; j++) {
			if (bodyLiteralArgType[i][k][j].equals("var")){
				triggerFV.add(bodyLiteralArgName[i][k][j]);
			}
		}
		return triggerFV;
	}

	private boolean isTimestampOrderingLiteral (int i, int k){
		if (bodyLiteralType[i][k].equals("timestamp_ordering")) {
			return true;
		}
		return false;
	}

	// returns equiv. classes of fv's
	private ArrayList<String> classOfFV (String variableName) {
		ArrayList<String> classFV = new ArrayList<String>();
		boolean [] [] includedLiteralFV = new boolean [headLiteralName.length] [];
		boolean itemsAdded = true; 
		for(int i = 0; i < headLiteralName.length; i++){
			includedLiteralFV [i] = new boolean [bodyLiteralName[i].length];
			for (int k = 0; k < bodyLiteralName[i].length; k++){
				includedLiteralFV [i][k] = false;
			}
			if (hornClauseType[i].equals("log_spec")){ // for all logging specs
				for (int k = 0; k < bodyLiteralName[i].length; k++){
					if (literalFV(i, k).contains(variableName) && !isTimestampOrderingLiteral(i,k)){
						classFV = union (literalFV(i, k), classFV); //System.out.println(i + " " + k);
						includedLiteralFV [i][k] = true;
					}
				}
			}
		}
		while(itemsAdded){
			itemsAdded = false;
			for(int i = 0; i < headLiteralName.length; i++){
				if (hornClauseType[i].equals("log_spec")){ // for all logging specs
					for (int k = 0; k < bodyLiteralName[i].length; k++){
						if (!(intersection(classFV, literalFV(i,k))).isEmpty() && !includedLiteralFV [i][k] &&
							!isTimestampOrderingLiteral(i,k)){
							classFV = union (literalFV(i, k), classFV);
							includedLiteralFV [i][k] = true; //System.out.println(i + "-" + k);
							itemsAdded = true;
						}
					}
				}
			}
		}
		return classFV;
		
	}

	
	private ArrayList<JSONObject> predicateClass(String variableName) {
		ArrayList<String> classFV = classOfFV(variableName);
		ArrayList<JSONObject> predicateClassList = new ArrayList<JSONObject>();
		for(int i = 0; i < headLiteralName.length; i++){
			if (hornClauseType[i].equals("log_spec")){ // for all logging specs
				for (int k = 0; k < bodyLiteralName[i].length; k++){
					if (!isTimestampOrderingLiteral(i,k)){
						for (int j = 0; j < bodyLiteralArgName[i][k].length; j++) {
							if (bodyLiteralArgType[i][k][j].equals("var") && 
									classFV.contains(bodyLiteralArgName[i][k][j])){
								predicateClassList.add(bodyLiteralObj[i][k]);
								break;
							}
						}						
					}
				}
			}
		}
		return predicateClassList;
	}

	private String predicateClassConjunct (String variableName) {
		ArrayList<JSONObject> predicateClassList = predicateClass(variableName);
		String conjunct = "";
		String argName;
		for (int k = 0; k < predicateClassList.size(); k++){
			conjunct += predicateClassList.get(k).getString("literal_name") + "(";
			for (int j = 0; j < predicateClassList.get(k).getJSONArray("args").length(); j++){
				argName = predicateClassList.get(k).getJSONArray("args").getJSONObject(j).getJSONObject("arg").getString("arg_name");
				if (predicateClassList.get(k).getString("literal_name").equals("funccall") &&
						j == 1){ // if arg is the method name, then drop the pass
					argName = argName.substring(argName.indexOf("#") + 1, argName.length());
				}
				conjunct += argName;

				if (j != predicateClassList.get(k).getJSONArray("args").length() - 1) { // if not the last arg, put a comma
					conjunct += ", ";
				}
			}
			conjunct += ")";
			if (k != predicateClassList.size() - 1){ // if not the last body literal, put a comma (conj)
				conjunct += ", ";
			}
		}
		return conjunct;
	}


	private boolean isLogEvent (String methodName) {
		for (int i = 0; i < headLiteralName.length; i++){
			if (hornClauseType[i].equals("log_spec")){ // for all logging specs
				if (methodName.equals(headLiteralArgName[i][1])) return true;
			}
		}
		return false;
	}


	private boolean isTrigger (String methodName) {
		for(int i = 0; i < headLiteralName.length; i++){
			if (hornClauseType[i].equals("log_spec")){ // for all logging specs
				for (int k = 0; k < bodyLiteralName[i].length; k++){
					if (bodyLiteralName[i][k].equals("funccall")){ // if the body literal is an event or trigger
						if (!isLogEvent(methodName) &&
							bodyLiteralArgName[i][k][1].equals(methodName)) return true;
					}
				}
			}
		}
		return false;
	}

	private String intermediateQueryResponseFormat (String variableName){
		ArrayList<String> classFV = classOfFV(variableName);
		String iqrf = "(";
		for (int l = 0; l < classFV.size(); l++){
			iqrf += classFV.get(l);
			if (l != classFV.size() - 1){
				iqrf += ",";
			}
		}
		iqrf += ")";
		return iqrf;
	}


	private ArrayList<String> triggersOfPredicateClass (String variableName) {
		ArrayList<JSONObject> corrPredicateClass = predicateClass(variableName);
		ArrayList<String> topc = new ArrayList<String>();
		for (int k = 0; k < corrPredicateClass.size(); k++){
			if (corrPredicateClass.get(k).getString("literal_name").equals("funccall")){ 
				String methodName = corrPredicateClass.get(k).getJSONArray("args").getJSONObject(1).getJSONObject("arg").getString("arg_name"); 
				methodName = methodName.substring(methodName.indexOf("#") + 1, methodName.length());
				if (isTrigger(methodName)){
					topc.add(methodName);
				}
			}
		}
		return topc;
	}


	private void writePaths() throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(configFile));
		for(String line; (line = br.readLine()) != null; ) {
			String [] part = line.split("=");
			if (part[0].equals("module_path")) {
				modulePath = part[1];
			}
			if (part[0].equals("xsb_bin_path")) {
				xsbBinPath = part[1];
			}
		}
	}
}
