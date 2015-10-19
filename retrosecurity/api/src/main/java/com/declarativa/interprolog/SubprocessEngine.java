/* 
Author: Miguel Calejo
Contact: info@interprolog.com, www.interprolog.com
Copyright InterProlog Consulting / Renting Point Lda, Portugal 2014
Use and distribution, without any warranties, under the terms of the
Apache License, as per http://www.apache.org/licenses/LICENSE-2.0.html
*/
package com.declarativa.interprolog;
import com.declarativa.interprolog.util.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.reflect.*;

/** A PrologEngine implemented over TCP/IP sockets. A SubprocessEngine object represents and gives access to a running Prolog process in background.
Multiple instances correspond to multiple Prolog processes, outside the Java Virtual Machine. 
*/
public abstract class SubprocessEngine extends AbstractPrologEngine{

	private static final String STDOUT = "stdout";
	private static final String STDERR = "stderr";
	protected boolean outAndErrMerged;
	private boolean engineStarted = false;
    Process prolog;
    private PrintWriter prologStdin;
    protected OutputHandler stdoutHandler, stderrHandler; // stderrHandler will be null if outAndErrMerged==true
    ServerSocket serverSocket;
    protected Socket socket;
    ServerSocket intServerSocket=null; protected Socket intSocket=null; // Used only for a particular way of interrupting Prolog
    String interruptCommand=null; // Used only for UNIX
    Vector<ClientRecognizer> listeners = new Vector<ClientRecognizer>();
    protected boolean available;
	Recognizer promptTrigger = peer.makePromptRecognizer();
	Recognizer breakTrigger = peer.makeBreakRecognizer();
	boolean slowWindowsShutdown = false;
	protected boolean mustUseSocketInterrupt = false;

	protected RecognizerListener availableSetter = new RecognizerListener(){
		public void recognized(Recognizer source,Object extra,String originStd){
			boolean shouldFire = !isAvailable();
			available=true;
			if (shouldFire) fireAvailabilityChange();
			progressMessage("I'm available! source:"+source+" extra:"+extra);
		}
	};            

    
    static class ClientRecognizer extends Recognizer implements RecognizerListener{
        PrologOutputListener client;
        ClientRecognizer(PrologOutputListener client){
            this.client=client;
            addRecognizerListener(this);
        }
        public void recognized(Recognizer source,Object extra,String originStd){
            // client.print((String)extra);
            if (originStd.equals(STDOUT))
            	client.printStdout((String)extra);
            else if (originStd.equals(STDERR))
            	client.printStderr((String)extra);
            else throw new IPException("Bad originStd:"+originStd);
        }
    }
    
    /**
     * Add a PrologOutputListener to this engine.  All stdout and stderr output will be routed to the client.
     * @param client An object interested in receiving messages depicting Prolog's progress
     * @see com.declarativa.interprolog.PrologOutputListener
     */
    public synchronized void addPrologOutputListener(PrologOutputListener client) {
        ClientRecognizer RE = new ClientRecognizer(client);
        listeners.addElement(RE);
        addPrologStdoutListener(RE);
        addPrologStderrListener(RE);
    }
	
	public synchronized void removePrologOutputListener(PrologOutputListener client){
		for (int i=0;i<listeners.size();i++) {
			ClientRecognizer cr = listeners.elementAt(i);
			if (cr.client==client) {
				listeners.removeElementAt(i);
				removePrologStdoutListener(cr);
				removePrologStderrListener(cr);
			}
		}
	}
	
	/** 
         * Add a OutputListener to get output from Prolog's standard output.
         * This is a lower level interface than addPrologOutputListener(PrologOutputListener).
         * @param client An object interested in Prolog's standard output
         * @see com.declarativa.interprolog.util.OutputListener
         */
	public void addPrologStdoutListener(OutputListener client){
		stdoutHandler.addOutputListener(client);
	}
	
	public void addPrologStderrListener(OutputListener l){
		if (stderrHandler!=null)
			stderrHandler.addOutputListener(l);
	}
	
	public void removePrologStdoutListener(OutputListener l){
		stdoutHandler.removeOutputListener(l);
	}
	
	public void removePrologStderrListener(OutputListener l){
		if (stderrHandler!=null)
			stderrHandler.removeOutputListener(l);
	}
	
        /** Construct a SubprocessEngine, launching a Prolog process in background.
         * @param prologCommand[] The command array to launch Prolog, as if given from a console shell.
         * Must not be null. First element will be the prolog executable, subsequent ones will be startup args for the Prolog engine
         * @param debug If true this engine will send debugging messages to System.out
         * @see SubprocessEngine#shutdown
         * @see SubprocessEngine#teachMoreObjects(ObjectExamplePair[])
         * @see SubprocessEngine#setDebug(boolean)
         */
	protected SubprocessEngine(String[] prologCommands, boolean outAndErrMerged, boolean debug, boolean loadFromJar) {
            super((prologCommands==null?null:prologCommands[0]),debug,loadFromJar);
            this.outAndErrMerged = outAndErrMerged;
            // Let's make sure PrologEngines get their finalize() message when we exit
            if (System.getProperty("java.version").compareTo("1.3")>=0) {
                Runtime.getRuntime().addShutdownHook(new Thread("Subprocess shutdown"){
                    public void run(){
                        if (prolog!=null) prolog.destroy();
                    }
                });
            } else {
                // For JDK 1.2 - considered unsafe
                // To avoid seeing warnings about deprecated methods
                // call the following instead of
                // System.runFinalizersOnExit(true);
                try{
                    Method finalizeOnExit = System.class.getMethod("runFinalizersOnExit",
                                            new Class[]{boolean.class});
                    finalizeOnExit.invoke(null,new Object[]{new Boolean(true)}); // for static methods first arg of invoke is ignored
                } catch (Exception e){
                	System.err.println("Could not call runFinalizersOnExit"); 
                }
                
            }
            
			promptTrigger.addRecognizerListener(availableSetter);
			breakTrigger.addRecognizerListener(availableSetter);                	
    }
	/*
	public SubprocessEngine(String prologCommand, boolean debug){
		super(prologCommand,debug);
	}
	
	public SubprocessEngine(String startPrologCommand){
		super(startPrologCommand);
	}
	
	public SubprocessEngine(boolean debug){
		super(debug);
	}
	
	public SubprocessEngine(){
		super();
	}
	*/
	protected void initSubprocess(String[] prologCommands){
		try {
			if (prologCommands==null) 
				prologCommands = new String[]{prologBinDirectoryOrCommand};
			else { // if prologCommand is just the Prolog base dir, make sure to obtain the executable path:
				prologBinDirectoryOrCommand = executablePath(prologCommands[0]);
				prologCommands[0] = prologBinDirectoryOrCommand;
			}

			prolog = createProcess(prologCommands);
			// No explicit buffering, because it's already being done by our Process's streams
			// If not, OutputHandler will handle the issue
			stdoutHandler = new OutputHandler(prolog.getInputStream(),(debug?System.err:null),STDOUT);
			if (!outAndErrMerged)
				stderrHandler = new OutputHandler(prolog.getErrorStream(),(debug?System.err:null),STDERR);
			setDetectPromptAndBreak(true);
			stdoutHandler.start();
			if (!outAndErrMerged)
				stderrHandler.start();
			Thread.yield(); // let's try to catch Prolog output ASAP
			prologStdin = new PrintWriter(prolog.getOutputStream());
			
			postCreateHack(prologCommands);
		
			loadInitialFiles();
			
			String myHost=clientHostname(); 
			progressMessage("Allocating the ServerSocket...");
			serverSocket = new ServerSocket(0); // let the system pick a port
			progressMessage("server port:"+serverSocket.getLocalPort());
			// waitUntilAvailable(); // Hangs Yap
			command("ipinitialize('"+myHost+"',"+
				serverSocket.getLocalPort()+","+
				registerJavaObject(this)+","+
				debug +
			")");
			progressMessage("Waiting for the socket to accept...");
			socket = serverSocket.accept();
			
			progressMessage("Teaching examples to Prolog...");
			PrologOutputObjectStream bootobjects = buildPrologOutputObjectStream(socket.getOutputStream());
			ObjectOutputStream oos = bootobjects.getObjectStream();
			teachIPobjects(oos);
			teachBasicObjects(oos);
			bootobjects.flush();
			progressMessage("Sent all examples...");
			waitUntilAvailable();
			setupCallbackServer();
			prepareInterrupt(myHost); // OS-dependent Prolog interrupt generation, must be after the previous step
			waitUntilAvailable();            
			interPrologFileLoaded = true;
			deterministicGoal("ipPrologEngine(_E), javaMessage(_E,setEngineStarted)");
			while(!engineStarted && !isIdle()) Thread.yield();
			//sendAndFlushLn("");
			waitUntilAvailable();
			
			progressMessage("Ended SubprocessEngine constructor");
		} catch (IOException e){
				throw new IPException("Could not launch Prolog executable:"+e);
		}
	}
	
	protected void postCreateHack(String[] prologCommands){}
	
	protected String clientHostname(){
		return "127.0.0.1"; // to avoid annoying Windows dialup attempt
	}
	
	public void setEngineStarted(){
		engineStarted = true;
	}
	
	protected PrologOutputObjectStream buildPrologOutputObjectStream(OutputStream os) throws IOException{
		return new PrologOutputObjectStream(os);
	}
	
	protected Process createProcess(String[] prologCommands) throws IOException{
        progressMessage("Launching subprocess "+Arrays.toString(prologCommands));
        ProcessBuilder PB = new ProcessBuilder(prologCommands);
        PB.redirectErrorStream(outAndErrMerged);
        // return Runtime.getRuntime().exec(prologCommands);
        return PB.start();
    }
	public void setDebug(boolean debug){
		stdoutHandler.setDebugStream(debug?System.err:null);
		if (stderrHandler!=null)
			stderrHandler.setDebugStream(debug?System.err:null);
		super.setDebug(debug);
	}
	
	/** Prolog is thought to be idle */
	public boolean isAvailable(){
		return available;
	}
	
	protected void setupCallbackServer(){
		prologHandler = new Thread(null,null,"Prolog handler" 
			/*,JAVA_STACK_SIZE platform dependend...if later used, the main thread also needs it*/){
			public void run(){
				try{
					while(!shutingDown) {
						progressMessage("Waiting to receive object");
						Object x = receiveObject();
						progressMessage("Received object",x);
						Object y = handleCallback(x);
						progressMessage("Handled object and computed",y);
						if (y!=null) sendObject(y);
					}
				} 
				catch (SocketException e){
                    // If this happens, it means there was a communications error
                    // with prolog.  We have to abort all current goals so that
                    // calls are not wait()-ing forever.
                    if (!shutingDown) {
                        IPException toThrow = new PrologHaltedException("Prolog death detected in socket at setupCallbackServer, goal was "+
                        	(goalsToExecute.isEmpty()?"none!":goalsToExecute.lastElement().getGoal().getGoal()), 
                        	e);
                        SubprocessEngine.this.endAllTasks(toThrow);
                        available = false;
                    }
				}
				catch (EOFException e){
                    // If this happens, it means there was a communications error
                    // with prolog.  We have to abort all current goals so that
                    // calls are not wait()-ing forever.
                    if (!shutingDown) {
                        IPException toThrow = new PrologHaltedException("Prolog death detected in EOF at setupCallbackServer, goal was "+
                        	(goalsToExecute.isEmpty()?"none!":goalsToExecute.lastElement().getGoal()), 
                        	e);
                        SubprocessEngine.this.endAllTasks(toThrow);
                        available = false;
                    }
				}
				catch (Exception e){
					 IPException toThrow = new PrologHaltedException("Terrible exception in setupCallbackServer",e);
					 // The Prolog engine may not have died, but we're unable to communicate
                        SubprocessEngine.this.endAllTasks(toThrow);
                        available = false;
                        throw toThrow;
                }
                catch (Error e){
                	System.err.println("Obscure error with cause:"+e.getCause());
                	System.err.println("Stack traces for all threads follow:");
                	printAllStackTraces();
                    available = false;
                	throw e;
                }
            }
        };
        progressMessage("Starting up callback service...");
		prologHandler.setName("Prolog handler");
		prologHandler.start();
	}
		
	protected Object receiveObject() throws IOException{
     	progressMessage("entering receiveObject()");
   		Object x=null;
    	try{
			ObjectInputStream ios = new ObjectInputStream(socket.getInputStream());
			x = ios.readObject();
		} catch (ClassNotFoundException e){
			x = e;
		}
     	progressMessage("exiting receiveObject():"+x);
		return x;
	}
	
    protected void sendObject(Object y) throws IOException{
    	progressMessage("entering sendObject",y);
		PrologOutputObjectStream poos = 
		    buildPrologOutputObjectStream(socket.getOutputStream());
		poos.writeObject(y);
		poos.flush(); // this actually writes to the socket stream
    	progressMessage("exiting sendObject",y);
	}
	
	/** Shuts down the background Prolog process as well as the dependent Java threads.
	*/
	public synchronized void shutdown(){
		super.shutdown();
		boolean shouldFire = isAvailable();
		available=false;
		if (shouldFire) fireAvailabilityChange();
		stdoutHandler.setIgnoreStreamEnd(true);
		if (stderrHandler!=null)
			stderrHandler.setIgnoreStreamEnd(true);
		if (shouldFire){
			doHalt();
			if (serverIsWindows()&&slowWindowsShutdown)
				try{Thread.sleep(100);} // seems useless after all...
				catch(InterruptedException ie){}
		}
        try{
            socket.close();
            serverSocket.close();
        }catch(IOException e) {throw new IPException("Problems closing sockets:"+e);}
        
        if(intServerSocket!=null){
			try {
				// closing sockets will stop them, no need to deprecate:
				// stdoutHandler.stop(); stderrHandler.stop(); cbhandler.stop();
				intSocket.close(); intServerSocket.close();
			}
			catch (IOException e) {throw new IPException("Problems closing sockets:"+e);}
			finally{
				if (!shouldFire || !serverIsWindows())  prolog.destroy(); // tries to avoid ugly messages on Windows
			} 
			// Might there be a reason to send "halt" to Prolog assynchronously? Or to delay shutdown and send it synchronously?
			// Assuming not.
		}
		else
	        if (!shouldFire || !serverIsWindows()) prolog.destroy();

		prologHandler.interrupt(); // kills javaMessage/deterministicGoal thread
	}
	
	protected void doHalt(){
		realCommand("halt");
	}
		
	public void setSlowWindowsShutdown(){
		slowWindowsShutdown = true;
	}
	
	/** Kill the Prolog background process. If you wish to make sure this message is sent on exiting, 
	use System.runFinalizersOnExit(true) on initialization
	*/
	protected void finalize() throws Throwable{
		if (prolog!=null) prolog.destroy();
	}
	
	protected void setDetectPromptAndBreak(boolean yes){
		if (yes==isDetectingPromptAndBreak()) return;
		if(yes){
			stdoutHandler.addOutputListener(promptTrigger);
			stdoutHandler.addOutputListener(breakTrigger);
			if (stderrHandler!=null){
				stderrHandler.addOutputListener(promptTrigger);
				stderrHandler.addOutputListener(breakTrigger);
			}
		} else{
			stdoutHandler.removeOutputListener(promptTrigger);
			stdoutHandler.removeOutputListener(breakTrigger);
			if (stderrHandler!=null){
				stderrHandler.removeOutputListener(promptTrigger);
				stderrHandler.removeOutputListener(breakTrigger);
			}
		}
	}
	protected boolean isDetectingPromptAndBreak(){
		return stdoutHandler.hasListener(promptTrigger) /*&& stderrHandler.hasListener(promptTrigger)*/ &&
			stdoutHandler.hasListener(breakTrigger) /*&& stderrHandler.hasListener(breakTrigger)*/;
	}
	
	/** Sends a String to Prolog's input. Its meaning will naturally depend on the current state of Prolog: it can be
        a top goal, or input to an ongoing computation */
	public synchronized void sendAndFlush(String s){
		boolean shouldFire = isAvailable();
		available=false;
		if (shouldFire) fireAvailabilityChange();
		prologStdin.print(s); prologStdin.flush();
	}
	
	public void sendAndFlushLn(String s){
		sendAndFlush(s+nl);
	}
	
	/** we'll resort to  Unix signals, or to a XSB built-in*/
	protected void prepareInterrupt(String myHost) throws IOException{ // requires successful startup steps
		if (serverIsWindows()){ 
			intServerSocket = new ServerSocket(0);
			command("setupWindowsInterrupt('"+myHost+"',"+intServerSocket.getLocalPort()+")");
			intSocket = intServerSocket.accept();
			progressMessage("interrupt prepared, using socket "+intSocket);
		} else {
			waitUntilAvailable();
			Object bindings[] = deterministicGoal("getPrologPID(N), ipObjectSpec('java.lang.Integer',Integer,[N],_)",
				"[Integer]");
			if (bindings==null) throw new IPException("Could not find Prolog's PID");
			progressMessage("Found Prolog process ID");
			
			if (mustUseSocketInterrupt)
				interruptCommand = unixSimpleInterruptCommand(bindings[0].toString());
			else
				interruptCommand = unixInterruptCommand(bindings[0].toString());
		}
	}
	
	/** ...although in some remote Unix scenarios we may use socket-based interrupts */
	protected String unixInterruptCommand(String PID){
		return unixSimpleInterruptCommand(PID);
	}
	static String unixSimpleInterruptCommand(String PID){
		return "/bin/kill -s INT "+PID;
	}
	
	public static byte ctrl_c=3;
	public static byte[] ctrlc = {3};
	/** @see #prepareInterrupt(String) **/
	protected synchronized void doInterrupt(){
	    setDetectPromptAndBreak(true);
	    try {
			if(mustUseSocketInterrupt||serverIsWindows()){
				progressMessage("Attempting to interrupt Prolog...");
				OutputStream IS = intSocket.getOutputStream();
				if (serverIsWindows()){ // ...thus having the ctrl-C built in for Java interrupting...
					IS.write(ctrlc); 
					IS.flush();
				} else{ // ...someone will receive this and exec the Unix command
					DataOutputStream dos = new DataOutputStream(IS);
					System.out.println("Piping interrupt:"+interruptCommand);
					dos.writeUTF(interruptCommand);
					dos.flush();
				}
				
			} else{
				// we'll just use a standard UNIX signal
				progressMessage("Interrupting Prolog with "+interruptCommand);
				Runtime.getRuntime().exec(interruptCommand);
			}
			
	    } 
	    catch(IOException e) {throw new IPException("Exception in interrupt():"+e);}
		interruptTasks(); // kludge
	    waitUntilAvailable();
	    // sendAndFlushLn("abort."); // supposedly leaves break mode... it does not work in XSB 3.2
	    //sendAndFlushLn("end_of_file."); leave break mode... BUT this would resume the interrupted computation :-(
	    // waitUntilAvailable();
		progressMessage("Leaving doInterrupt");
	}
	

	/** This implementation may get stuck if the command includes variables, because the Prolog
	top level interpreter may offer to compute more solutions; use variables prefixed with '_' */
	public boolean realCommand(String s){
		progressMessage("COMMAND",s); // not displaying the "." to avoid building a new Java string here...
		sendAndFlushLn(s+".");
		return true; // we do not really know
	}
	
	public Object[] deterministicGoal(String G, String OVar, Object[] objectsP, String RVars){
		// No Prolog threads being used by InterProlog in this version, so if necessary let's wait until...
		while(otherComputationHappening()) Thread.yield();
		if (isIdle()) return firstGoal(G, OVar, objectsP, RVars);
		else return super.deterministicGoal(G, OVar, objectsP, RVars);
	}
	
	/* This is a lame (incomplete) implementation: the implemented condition is sufficient but not necessary. 
	So 'false' does not ensure that some other independent dg call is open.
	A complete implementation would require keeping causal links between dG calls and underlying javaMessage calls
	(perhaps by linking the timestamps on both sides in the javaMessage predicate) */
	private synchronized boolean otherComputationHappening(){
		return dgThreads.size() > 0 && messagesExecuting.size()==0;
	}

	/** Very alike deterministicGoal except that it sends the initial GoalFromJava object over the socket */
    protected Object[] firstGoal(String G, String OVar, Object[] objectsP, String RVars) {

    	topGoalHasStarted = true;
    	Object[] resultToReturn=null;
        GoalToExecute goalToDo;
        ResultFromProlog result;
 		int mytimestamp = incGoalTimestamp();
		boolean shouldFire = isAvailable();
		available=false;
		if (shouldFire) fireAvailabilityChange();
        try{
       		GoalFromJava GO = makeDGoalObject(G, OVar, objectsP, RVars, mytimestamp);
       		progressMessage("Prepared GoalFromJava",GO);
            progressMessage("Schedulling (first) goal ",G);
            goalToDo = new GoalToExecute(GO);
            
            goalToDo.setFirstGoalStatus();
            scheduleGoal(goalToDo);
 			goalToDo.prologWasCalled();
                        //if(this.isWindowsOS()){
 			pushDGthread(goalToDo.getCallerThread());
			    //}
            //setupErrorHandling();
           	sendObject(GO);
 			realCommand(deterministicGoalString()); // assynchronous
 			
           	result = goalToDo.waitForResult();
           	lastSolutionWasUndefined = result.undefined;
           	progressMessage("firstGoal - Got result for ",goalToDo);
            // goalToDo is forgotten by handleCallback
            if (result.succeeded)
                resultToReturn = result.rVars;
           	available = true; // so we can dispense with prompt recognition insofar as firstGoal goes
           	fireAvailabilityChange();
        } catch (IPException e) {
            throw e;
        } catch (SocketException e) {
            if (shutingDown) throw new UnavailableResultException("Goal was "+G);
            else throw new IPException("Problem in deterministicGoal:"+e);
        } catch (Exception e) {
            throw new IPException("Problem in deterministicGoal:"+e);
        } finally{
			topGoalHasStarted = false; // is this OK? this assumes no initiative from the Prolog side, which is probably correct
			//removeErrorHandling();
			progressMessage("Leaving firstGoal for ",G);
        }
		if (goalToDo.wasAborted()) {
			if (shutingDown) throw new UnavailableResultException("IP aborted goal was "+G);
			else throw new IPAbortedException(G+" was aborted");
		}
		if (goalToDo.wasInterrupted()) throw new IPInterruptedException(G+" was interrupted");
		if (result.wasInterrupted(this)) 
			throw new IPInterruptedException(G+" was interrupted, Prolog detected"); 
		// if (result.error!=null) throw new IPException (result.error.toString());
		if (result.error!=null) {
			if (result.error instanceof IPException) throw (IPException)result.error;
			else throw new IPPrologError(result.error /*+" in goal "+G*/);
		}
		if (result.timestamp!=mytimestamp)
			throw new IPException("bad timestamp in deterministicGoal, got "+result.timestamp+" instead of "+goalTimestamp);
        return resultToReturn;
    }
        
    /** Depending on the engine mode (e.g. Prolog toploop, Flora shell...) we may need to wrap the Prolog goal into something more*/
    protected String deterministicGoalString(){
    	return "deterministicGoal";
    }

	protected Object doSomething(){
		if (onlyFirstGoalSchedulled()) return null;
		else return super.doSomething();
	}
	
	protected synchronized boolean onlyFirstGoalSchedulled(){
		return isIdle() || (messagesExecuting.size()==0 && goalsToExecute.size()==1 && 
			((GoalToExecute)goalsToExecute.elementAt(0)).isFirstGoal());
	}
	
	// deterministicGoal helpers
	
    protected void setupErrorHandling(){
		setDetectPromptAndBreak(false);
		if (stderrHandler!=null)
			stderrHandler.addOutputListener(errorTrigger); 
		stdoutHandler.addOutputListener(errorTrigger); // needed because some engines may fuse stdout and stderr
		final Thread current = Thread.currentThread();
		// We could dispense creating this every time:
		errorHandler = new RecognizerListener(){
			public void recognized(Recognizer source,Object extra,String originStd){
			    current.interrupt(); 
			}
		    };
		errorTrigger.addRecognizerListener(errorHandler);
    }
    
    protected void removeErrorHandling(){
    	errorTrigger.removeRecognizerListener(errorHandler);
    	if (stderrHandler!=null)
    		stderrHandler.removeOutputListener(errorTrigger);
    	stdoutHandler.removeOutputListener(errorTrigger);
    	errorHandler=null;
		setDetectPromptAndBreak(true);
    }
    private RecognizerListener errorHandler=null;
    Recognizer errorTrigger = new Recognizer("++Error",true); // was "++Error: " for XSB 2.4
    /** Useful for testing */
	public static class OutputDumper implements PrologOutputListener{
		Writer w; 
		boolean available = true;
		String filename;
		public OutputDumper(String filename){
			this.filename=filename;
			try {
				w = new FileWriter(filename);
			} catch (IOException ioe){
				throw new RuntimeException("Failed to create dumper:"+ioe);
			}
		}
		public void print(String s){
			try{
				if (available) w.write(s); // Do not flush, might affect performance measurement
				else System.err.println("Lost output to file "+filename+":"+s);
			} catch (IOException ioe){
				throw new RuntimeException("Failed to write dumper:"+ioe);
			}
		}
		public void close(){
			try{
				w.close();
				available = false;
			} catch (IOException ioe){
				throw new RuntimeException("Failed to close dumper:"+ioe);
			}
		}
		@Override
		public void printStdout(String s) {
		}
		@Override
		public void printStderr(String s) {			
		}
	}
}
