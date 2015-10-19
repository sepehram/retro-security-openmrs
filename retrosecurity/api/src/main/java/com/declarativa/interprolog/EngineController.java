/* 
Author: Miguel Calejo
Contact: info@interprolog.com, www.interprolog.com
Copyright InterProlog Consulting / Renting Point Lda, Portugal 2014
Use and distribution, without any warranties, under the terms of the
Apache License, as per http://www.apache.org/licenses/LICENSE-2.0.html
*/
package com.declarativa.interprolog;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.util.HashSet;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import com.declarativa.interprolog.gui.ListenerWindow;


public class EngineController implements PrologEngineListener{
	HashSet<Object> items = new HashSet<Object>();
	public final AbstractAction pauseContinueAction, stopAction;
	boolean pauseRequested = false, pauseEnded = false, inPause=false, prologCanWork=true;
	public static final String STOP_MESSAGE = "Query Stopped";
	
	@SuppressWarnings("serial")
	public EngineController(){
		// This action will oscillate between Pause(disabled/enabled)/Continue:
		pauseContinueAction = new AbstractAction("Pause"){
			public void actionPerformed(ActionEvent e){
				if (getValue(NAME).equals("Pause")){
					setEnabled(false);
					stopAction.setEnabled(false);
					pauseRequested = true; pauseEnded=false;
				} else { // "Continue" clicked
					prepareForPause();
					stopAction.setEnabled(true);
					pauseEnded = true;
				}
			}
			public void prepareForPause(){
				putValue(NAME,"Pause");
				pauseContinueAction.putValue(Action.SHORT_DESCRIPTION,"Click to pause the engine");
			}
			@SuppressWarnings("unused")
			public void prepareforContinue(){ // called dynamically bellow...
				putValue(NAME,"Continue");
				pauseContinueAction.putValue(Action.SHORT_DESCRIPTION,"Click to resume execution");
			}
		};
		pauseContinueAction.setEnabled(false);
		myMessage(pauseContinueAction,"prepareForPause");
		// This action will not change name, just enabled state:
		stopAction = new AbstractAction("Stop"){
			public void actionPerformed(ActionEvent e){
				setEnabled(false);
				pauseContinueAction.setEnabled(false);
				prologCanWork = false;
			}
		};
		stopAction.setEnabled(false);
		stopAction.putValue(Action.SHORT_DESCRIPTION,"Click to stop (end) the query");
	}
	
	/** The user has started a query */
	public void queryStarted(){
		pauseContinueAction.setEnabled(true);
		stopAction.setEnabled(true);
		//System.out.println("queryStarted");
	}
	/** The user query ended */
	public void queryEnded(){
		pauseContinueAction.setEnabled(false);
		stopAction.setEnabled(false);
		//System.out.println("queryEnd");
	}
	
	// PrologEngineListener methods:
	
	public String willWork(PrologEngine source){
		//System.out.print("(p)");
		if (pauseRequested) {
			myMessage(pauseContinueAction,"prepareforContinue");
			pauseContinueAction.setEnabled(true);
		}
		inPause=true;
		if (!source.isAvailable())
			availabilityChanged(source);
		while(pauseRequested & ! pauseEnded) Thread.yield();
		inPause=false;
		if (!source.isAvailable())
			availabilityChanged(source);
		if (pauseRequested) pauseRequested=false;
		//System.out.println("prologCanWork:"+prologCanWork+","+this);
		if (prologCanWork) return null;
		prologCanWork = true;
		return STOP_MESSAGE;
	}	
	
	public void javaMessaged(PrologEngine source){
		// System.out.print("(j)");
	}
	/** In addition to being messaged by the engine, this method also gets called by this controller when entering/leave pause */
	public void availabilityChanged(final PrologEngine source){
		// System.out.print("availabilityChanged:"+source.isAvailable());
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				if (source.isAvailable()) queryEnded();
				else queryStarted();
				for (Object item:items){
					if (item instanceof Component){
						((Component)item).setEnabled(source.isAvailable()||inPause);
						Container top = null;
						if (item instanceof JComponent)
							top = ((JComponent)item).getTopLevelAncestor(); //TODO: NOT working, gets us nulls!
						//System.out.println(item.getComponent());
						if (top!=null){
							if (!(source.isAvailable()||inPause))
								ListenerWindow.setWaitCursor(top);
							else ListenerWindow.restoreCursor(top);
						}
					} else if (item instanceof Action){
						((Action)item).setEnabled(source.isAvailable()||inPause);
					}
				}
			}
		});
	}
	
	
	/** This menu item should be enabled only when the engine is available or paused. 
	Its window will get a busy cursor when the engine is busy */
	public void disableWhenBusy(Component item){
		items.add(item);
	}
	public void disableWhenBusy(Action item){
		items.add(item);
	}
	
	/** For those annoying situations where anonymous classes do not expose their simple methods (void result and no args) elsewhere */
	public static void myMessage(Object target,String method){
		try{target.getClass().getMethod(method).invoke(target);}
		catch(Exception e){throw new RuntimeException(e);}
	}
	
	public boolean isInPause(){
		return inPause;
	}
	
	public void stop(){
		stopAction.setEnabled(false);
		pauseContinueAction.setEnabled(false);
		prologCanWork = false;
	}
}
