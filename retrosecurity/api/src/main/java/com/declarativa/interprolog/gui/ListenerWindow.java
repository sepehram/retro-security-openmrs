/* 
Author: Miguel Calejo
Contact: info@interprolog.com, www.interprolog.com
Copyright InterProlog Consulting / Renting Point Lda, Portugal 2014
Use and distribution, without any warranties, under the terms of the
Apache License, as per http://www.apache.org/licenses/LICENSE-2.0.html
*/
package com.declarativa.interprolog.gui;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Event;
import java.awt.FileDialog;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.UndoableEditEvent;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.JTextComponent;
import javax.swing.text.Style;
import javax.swing.text.StyleContext;
import javax.swing.text.rtf.RTFEditorKit;
import javax.swing.undo.UndoManager;

import com.declarativa.interprolog.AbstractPrologEngine;
import com.declarativa.interprolog.ObjectExamplePair;
import com.declarativa.interprolog.PrologEngine;
import com.declarativa.interprolog.XSBPeer;
import com.declarativa.interprolog.util.IPException;

/** A simple Prolog listener, with a consult menu and an history mechanism. 
This should be sub-classed, in order to define sendToProlog()*/
@SuppressWarnings("serial")
public abstract class ListenerWindow extends JFrame implements WindowListener{	
	public JTextArea prologInput; 
	public MyJTextPane prologOutput;
	protected JMenu historyMenu, fileMenu; 
	int firstHistoryItem = -1;
	Vector<LoadedFile> loadedFiles;
	protected static int topLevelCount = 0;
	public AbstractPrologEngine engine = null;
	protected boolean mayExitApp;
	protected JSplitPane splitPane;
	private static Map<Component,Cursor> previousCursors = new HashMap<Component,Cursor>();
	/** For coloring the output pane */
	protected StyleContext styleContext;
	
	protected void initializeOutputStyles() {}
	
	public class MyJTextPane extends JTextPane{
		Action copyAction = new AbstractAction("Copy"){
			@Override
			public void actionPerformed(ActionEvent e) {
				copyAsRTF();
			}	
		};

		MyJTextPane(StyleContext styleContext){
			super(new DefaultStyledDocument());
		}
		public void append(String str, Style a){
			try {
				getDocument().insertString(getDocument().getLength(), str, a);
				/* Buggy (at least on Mac): returns always the default style!
				System.out.println("insertString:"+str+"\n"+a);
				System.out.println("STYLE:"+((DefaultStyledDocument)getDocument()).getLogicalStyle(getDocument().getLength()-1));*/
			} catch (BadLocationException e) {
				System.err.println("Problems appending text:"+e);
			}
		}
		public void append(String S){
			append(S,null);
		}
		public void copyAsRTF(){
			if (getSelectionStart()==getSelectionEnd())
				return;
			try {
				getToolkit().getSystemClipboard().setContents(new RTFSelection(), null);
			} catch (IllegalStateException e) {
				beep(); e.printStackTrace();
				return;
			} catch (IOException e) {
				beep(); e.printStackTrace();
			} catch (BadLocationException e) {
				beep(); e.printStackTrace();
			}
			
		}
		class RTFSelection extends StringSelection{
			private DataFlavor RTF = new DataFlavor("text/rtf", "RTF");
			byte[] data;

			public RTFSelection() throws IOException, BadLocationException {
				super(getSelectedText());
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				RTFEditorKit editorKit = new RTFEditorKit();
				editorKit.write(out, getStyledDocument(), getSelectionStart(), getSelectionEnd() );
				data = out.toByteArray();
				out.close();
			}

			@Override
			public DataFlavor[] getTransferDataFlavors() {
				DataFlavor[] flavors = super.getTransferDataFlavors();
				DataFlavor[] result = Arrays.copyOf(flavors, flavors.length+1);
				result[result.length-1] = RTF;
				return result;
			}
			public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
				if (flavor.equals(RTF))
					return new ByteArrayInputStream(data);
				else 
					return super.getTransferData(flavor);
			}
			public boolean isDataFlavorSupported(DataFlavor flavor) {
				if (RTF.equals(flavor)) return true;
				else return super.isDataFlavorSupported(flavor);
			}
		}
		
		public String getToolTipText(MouseEvent event){
			
			int P = viewToModel(event.getPoint());
			DefaultStyledDocument doc = ((DefaultStyledDocument)getDocument());
			
			/* System.out.println("--");
			System.out.println(doc.getLogicalStyle(P));
			System.out.println(doc.getLogicalStyle(P).getClass());
			System.out.println(doc.getParagraphElement(P));
			System.out.println(doc.getParagraphElement(P).getAttributes());
			System.out.println(doc.getParagraphElement(P).getAttributes().getResolveParent());
			System.out.println(doc.getParagraphElement(P).getAttributes().getAttribute(AttributeSet.ResolveAttribute));
			System.out.println(doc.getCharacterElement(P));
			System.out.println(doc.getCharacterElement(P).getAttributes());
			System.out.println(doc.getCharacterElement(P).getAttributes().getAttribute(AttributeSet.ResolveAttribute)); */

			// Somehow... we're not getting the logical style :-( 
			String tip = attributesToTooltip(doc.getLogicalStyle(P));
			if (tip != null) return tip;
			else return super.getToolTipText(event);
		}
	}
	protected String attributesToTooltip(AttributeSet attributes) {
		return null;
	}
	public ListenerWindow(AbstractPrologEngine e){
		this(e,true,true);
	}
	public ListenerWindow(AbstractPrologEngine e, boolean autoDisplay, boolean mayExitApp){
		this(e,autoDisplay,mayExitApp,null);
	}
	public ListenerWindow(AbstractPrologEngine e, boolean autoDisplay, boolean mayExitApp, Rectangle R){
		super("PrologEngine listener (Swing)");
		if (e==null)
			throw new IPException("missing Prolog engine");
		if (R!=null)
			setBounds(R);
		e.waitUntilAvailable();
		this.mayExitApp=mayExitApp;
		engine=e;
		String VF = e.getImplementationPeer().visualizationFilename();
		if (engine.getLoadFromJar()) engine.consultFromPackage(VF,ListenerWindow.class);
		else engine.consultRelative(VF,ListenerWindow.class);
		engine.teachMoreObjects(guiExamples());
		if (!e.deterministicGoal("retractall(ipListenerWindow(_)), asserta(ipListenerWindow("+e.registerJavaObject(this)+"))"))
			throw new IPException("could not assert ipListenerWindow"); 
		

		if (engine==null) dispose(); // no interface object permitted!
		else topLevelCount++;
		debug=engine.isDebug();
		
		loadedFiles = new Vector<LoadedFile>();
		styleContext = new StyleContext();

		constructWindowContents();
		constructMenu();
		
				
		addWindowListener(this);
		listenerGreeting(e);
        if (autoDisplay) {
		    setVisible(true);
		    focusInput();	
		}
		
	}
	
	protected void listenerGreeting(PrologEngine e){
		prologOutput.append("Welcome to an InterProlog top level\n"+e.getPrologVersion() + "\n",null);
	}
			
	// WindowListener methods
	public void windowOpened(WindowEvent e){}
	public void windowClosed(WindowEvent e){}
	public void windowIconified(WindowEvent e){}
	public void windowClosing(WindowEvent e){
		dispose();
		if (mayExitApp){
			engine.shutdown();
			topLevelCount--;
			if (topLevelCount <= 0) System.exit(0);
				// should check whether any relevant windows are changed...
		}
	}
	public void windowActivated(WindowEvent e){
		prologInput.requestFocus();
	}
	public void windowDeactivated(WindowEvent e){}
	public void windowDeiconified(WindowEvent e){}
	
	public static ObjectExamplePair[] guiExamples() {
		ObjectExamplePair[] examples = {
			PredicateTableModel.example(),
			TermListModel.example(),
			TermTreeModel.example(),
			new ObjectExamplePair("ArrayOfTermTreeModel",new TermTreeModel[0]),
			XSBTableModel.example(),
		};
		return examples;
	}
	
	protected void constructWindowContents(){
		Font prologFont = new Font("Courier",Font.PLAIN,12);
		Container c = getContentPane();
		c.setLayout(new BorderLayout());
		prologOutput = new MyJTextPane(styleContext); 
		prologOutput.setFont(prologFont); 
		prologOutput.setEditable(false); 
		prologOutput.setToolTipText("Here's Prolog console output");
		//prologOutput.setLineWrap(true);  // Swing used to crash with large amounts of text...
		prologOutput.setDoubleBuffered(true); // Use Swing double screen buffer
        prologOutput.getAccessibleContext().setAccessibleName("Prolog Console Output");
        popupEditMenuFor(prologOutput);
		initializeOutputStyles();
		JScrollPane piscroller = new JScrollPane();
		prologInput = new JTextArea(4,80); prologInput.setFont(prologFont); prologInput.setLineWrap(true);
		prologInput.setToolTipText("Prolog input, sent when you press enter. Drag and drop .P files here to reconsult them");
        prologInput.getAccessibleContext().setAccessibleName("Prolog Input");
        popupEditMenuFor(prologInput);
		piscroller.getViewport().add(prologInput); 
		
		prologInput.addKeyListener(new KeyAdapter(){
			public void keyPressed(KeyEvent e){
				if (e.getKeyCode()==KeyEvent.VK_ENTER) {
					e.consume();
					sendToProlog();
				} else if (e.getKeyCode()==KeyEvent.VK_UP ){
					e.consume();
					String S = prologInput.getText();
					int item = findInHistory(S);
					if (item==-1 && historyCount()>0) prologInput.setText(historyItem(historyCount()-1));
					else if (item>0) prologInput.setText(historyItem(item-1));
				} else if (e.getKeyCode()==KeyEvent.VK_DOWN){
					e.consume();
					String S = prologInput.getText();
					int item = findInHistory(S);
					if (item>=0 && item < historyCount()-1) prologInput.setText(historyItem(item+1));
				} else if ((e.getKeyCode() == KeyEvent.VK_D && ! AbstractPrologEngine.isWindowsOS()) && e.isControlDown()){
					//Unix end of file
					e.consume();
					sendToProlog("\0x4");
					//System.out.println("Ctrl-D being sent as end of file");
				} else if ((e.getKeyCode() == KeyEvent.VK_Z && AbstractPrologEngine.isWindowsOS()) && e.isControlDown()){
					//...and Windows
					e.consume();
					sendToProlog("\0x1A");
					//System.out.println("Ctrl-Z being sent as end of file");
				}
			}
		});


		JScrollPane scroller = new JScrollPane();
		scroller.getViewport().add(prologOutput);
		splitPane = new JSplitPane (JSplitPane.VERTICAL_SPLIT, scroller, prologInput);
		c.add("Center",splitPane);
		setSize(600,600);
		splitPane.setDividerLocation(500);
		//j.resetToPreferredSizes();
		validate();
		
		DropTargetListener dropHandler = new DropTargetListener(){
			public void dragOver(DropTargetDragEvent dtde){}
			public void dropActionChanged(DropTargetDragEvent dtde){}
			public void dragExit(DropTargetEvent dte){}
			public void drop(DropTargetDropEvent dtde){
				handlePrologInputDnD(dtde);
			}
			public void dragEnter(DropTargetDragEvent dtde){
				// System.out.println("dragEnter:"+dtde);
			}
		};
		new DropTarget(prologInput,dropHandler);
		new DropTarget(prologOutput,dropHandler);
		new DropTarget(this,dropHandler);
	}
	
	static class MyUndoManager extends UndoManager{
		Action undoAction, redoAction;
		MyUndoManager(){
			undoAction = new AbstractAction("Undo"){
				@Override
				public void actionPerformed(ActionEvent e) {
					undo();
					updateActions();
				}
			};
			redoAction = new AbstractAction("Redo"){
				@Override
				public void actionPerformed(ActionEvent e) {
					redo();
					updateActions();
				}
			};
			undoAction.setEnabled(false);
			redoAction.setEnabled(false);
		}
		protected void updateActions() {
			redoAction.setEnabled(canRedo());
			undoAction.setEnabled(canUndo());
		}
		public void undoableEditHappened(UndoableEditEvent e){
			super.undoableEditHappened(e);
			updateActions();
		}

	}
	
	static private JPopupMenu popupEditMenuFor(final JTextComponent text){
		ActionMap map = text.getActionMap();
		final JPopupMenu menu = new JPopupMenu();
		
		if (text.isEditable()){
			MyUndoManager undoManager = new MyUndoManager();
		    text.getDocument().addUndoableEditListener(undoManager);
			KeyStroke undoKey = KeyStroke.getKeyStroke(KeyEvent.VK_Z,Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
			KeyStroke redoKey = KeyStroke.getKeyStroke(KeyEvent.VK_Z,Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()+Event.SHIFT_MASK);
			text.getActionMap().put("UNDO!", undoManager.undoAction);
			text.getInputMap().put(undoKey, "UNDO!");
			text.getActionMap().put("REDO!", undoManager.redoAction);
			text.getInputMap().put(redoKey, "REDO!");
		    menu.add(undoManager.undoAction);
		    menu.add(undoManager.redoAction);
			
			Action cut = map.get(DefaultEditorKit.cutAction);
			cut.putValue(Action.NAME, "Cut");
			menu.add(cut);
		}
		// Action copy = map.get(DefaultEditorKit.copyAction);
		// Mac bug? Or JTextPane weirdness? But the following works:
		Action copy;
		if (text instanceof MyJTextPane){ // kludgy ;-)
			KeyStroke copyKey = KeyStroke.getKeyStroke(KeyEvent.VK_C,Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
			text.getInputMap().put(copyKey, "COPY!");
			copy = ((MyJTextPane)text).copyAction;
			text.getActionMap().put("COPY!", copy);
		} else 
			copy = new DefaultEditorKit.CopyAction();
		copy.putValue(Action.NAME, "Copy");
		menu.add(copy);
		if (text.isEditable()){
			Action paste = map.get(DefaultEditorKit.pasteAction);
			paste.putValue(Action.NAME, "Paste");
			menu.add(paste);
		}
		
		text.addMouseListener(new MouseAdapter(){
			public void mousePressed(MouseEvent e) {
				if (e.isPopupTrigger()){
					menu.show(text, e.getX(), e.getY());
				}
			}
		});
		return menu;
	}
	
	void handlePrologInputDnD(DropTargetDropEvent dtde){
		//System.out.println("drop:"+dtde);
		try{
			Transferable transferable = dtde.getTransferable();
			/*
			DataFlavor[] flavors = transferable.getTransferDataFlavors();
			for (int f=0;f<flavors.length;f++)
				System.out.println("Flavor:"+flavors[f]);*/
			int action = dtde.getDropAction();
			if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)){	
				if (engine.isIdle()){
					dtde.acceptDrop(action);
					final java.util.List<?> files = (java.util.List<?>)transferable.getTransferData(DataFlavor.javaFileListFlavor);
					dtde.getDropTargetContext().dropComplete(true);
					boolean allPs = true;
					for (int f=0;f<files.size();f++){
						if (!droppableFile((File)files.get(f))) {
							allPs=false; break;
						}
					}
					if(!allPs) 
						errorMessage(badFilesDroppedMessage());
					else {
						prologOutput.append("\nHandling "+((files.size()>1 ? files.size()+" files...\n" : files.size()+" file...\n")),null);
						Runnable r = new Runnable(){
							public void run(){
								boolean crashed = false;
								Toolkit.getDefaultToolkit().sync();
								for (int f=0;f<files.size() && !crashed;f++){
									File file = (File)files.get(f);
									if (!processDraggedFile(file)) crashed = true;
								}
								if (crashed) prologOutput.append("...terminated with errors.\n",null);
								else prologOutput.append("...done.\n",null);
								scrollToBottom();
							}
						};
						SwingUtilities.invokeLater(r);
					}
				} else {
					dtde.rejectDrop();
					errorMessage("You can not consult files while Prolog is working");
				}
			} else dtde.rejectDrop();
		} catch (Exception e){
			throw new IPException("Problem dropping:"+e);
		}
	}
	protected boolean droppableFile(File f){
		return f.getName().endsWith(".P");
	}
	protected String badFilesDroppedMessage(){
		return "All dragged files must be Prolog source files";
	}
	
	public boolean processDraggedFile(File f){
		if (!checkEngineAvailable()) return false;
		if (engine.consultAbsolute(f)) {
			addToReloaders(f,"consult");
			return true;
		} else {
			errorMessage("Problems reconsulting "+f.getName());
			return false;
		}
	}
	
	public void errorMessage(String m){
		beep();
		JOptionPane.showMessageDialog(this,m,"Error",JOptionPane.ERROR_MESSAGE);
	}
	
	protected boolean checkEngineAvailable(){
		if (engine.isAvailable()) return true;
		JOptionPane.showMessageDialog(this,"Please end the current top goal first","Warning",JOptionPane.WARNING_MESSAGE);	
		return false;	
	}
	
	protected JMenu constructFileMenu(JMenuBar mb){
		JMenu fileMenu;
		fileMenu = new JMenu("File"); 
        fileMenu.setMnemonic('F');
        mb.add(fileMenu);
		
		addItemToMenu(fileMenu,"Consult...", new ActionListener(){
			public void actionPerformed(ActionEvent e){
				reconsultFile();
			}
		});
		
		if (engine.getImplementationPeer() instanceof XSBPeer)
			addItemToMenu(fileMenu,"Load dynamically...",new ActionListener(){
				public void actionPerformed(ActionEvent e){
					load_dynFile();
				}
			});
				
		fileMenu.addSeparator();
		return fileMenu;
	}
	
	void constructMenu(){
		JMenuBar mb; 
		mb = new JMenuBar(); 
		
		fileMenu = constructFileMenu(mb);
        //mb.add(fileMenu);
		
		mb.add(constructToolsMenu());
		
		historyMenu = new JMenu("History",true); 
        historyMenu.setMnemonic('H');
        mb.add(historyMenu); 
		addItemToMenu(historyMenu,"Clear Listener's Output",new ActionListener(){
			public void actionPerformed(ActionEvent e){
				prologOutput.setText("");
			}
		});
		historyMenu.addSeparator(); // to avoid Swing bug handling key events
		firstHistoryItem = historyMenu.getItemCount();
		setJMenuBar(mb);
	}
	
	protected JMenu constructToolsMenu(){
		JMenu toolMenu = new JMenu("Tools"); 
        toolMenu.setMnemonic('T');
		addInterPrologItems(toolMenu);		
		return toolMenu;
	}
	
	protected void addInterPrologItems(JMenu toolMenu){
		final JCheckBoxMenuItem debugging = new JCheckBoxMenuItem("Engine debugging");
		toolMenu.add(debugging);
		debugging.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				engine.setDebug(debugging.isSelected());
			}
		});

		addItemToMenu(toolMenu,"Java Object Specifications",new ActionListener(){
			public void actionPerformed(ActionEvent e){
				engine.command("showObjectVariables");
			}
		});
				
		addItemToMenu(toolMenu,"Interrupt Prolog",new ActionListener(){
			public void actionPerformed(ActionEvent e){
				engine.interrupt();
			}
		}).setToolTipText("Brutal interrupt of engine, may lead to inconsistent state of object streams");
	}
	
	class HistoryListener implements ActionListener{
		JTextComponent targetText;
		String memory;
		HistoryListener(JTextComponent t,String s){
			targetText=t; memory=s;
		}
		public void actionPerformed(ActionEvent e){
			targetText.replaceSelection(memory);
		}
	}
	
	public static JMenuItem addItemToMenu(JMenu menu,String item,ActionListener handler) {
		JMenuItem menuItem = new JMenuItem(item);
		menu.add(menuItem);
		menuItem.addActionListener(handler);
		return menuItem;
	}
	
	/** accelerator requires the command (Mac) or ctrl (other systems) modifier */
    public static JMenuItem addItemToMenu(JMenu menu, String item, int accelerator, ActionListener handler) {
		JMenuItem menuItem = new JMenuItem(item);
        menuItem.setAccelerator(
        	KeyStroke.getKeyStroke(accelerator, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		menu.add(menuItem);
		menuItem.addActionListener(handler);
		return menuItem;
	}

	/** accelerator requires the command (Mac) or ctrl (other systems) modifier */
    public static JMenuItem addItemToMenu(JPopupMenu menu, String item, int accelerator, ActionListener handler) {
		JMenuItem menuItem = new JMenuItem(item);
        menuItem.setAccelerator(
        	KeyStroke.getKeyStroke(accelerator, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		menu.add(menuItem);
		menuItem.addActionListener(handler);
		return menuItem;
	}

    public static JMenuItem addItemToMenu(JPopupMenu menu, String item, ActionListener handler) {
		JMenuItem menuItem = new JMenuItem(item);
		menu.add(menuItem);
		menuItem.addActionListener(handler);
		return menuItem;
	}

	public void sendToProlog(){
		sendToProlog(null);
	}

	public abstract void sendToProlog(String invisiblePostfix);

	static final int HISTORY_WIDTH = 60;
	
	protected void addToHistory(){
		JMenuItem item;
		String goal = prologInput.getText();
		if (goal.equals(";") || goal.equals("")) return; // not worthy remembering
		int maxHistoryItems = Toolkit.getDefaultToolkit().getScreenSize().height / 20;
		if (historyMenu.getItemCount()>=maxHistoryItems) historyMenu.remove(firstHistoryItem);
		int K = findInHistory(goal);
		if (K!=-1) 
			historyMenu.remove(firstHistoryItem+K);
		if (goal.length()>HISTORY_WIDTH) historyMenu.add(item = new JMenuItem(goal.substring(0,HISTORY_WIDTH-1)+"..."));
		else historyMenu.add(item = new JMenuItem(goal));
		item.addActionListener(new HistoryListener(prologInput,goal));
	}
	
	/** Assumes only one ActionListener per history menu item */
	String historyItem(int n){
		return ((HistoryListener)historyMenu.getItem(n+firstHistoryItem).getActionListeners()[0]).memory;
	}
	
	/** find last item in history menu whose visible chars (except ...) match S */
	int findInHistory(String S){
		for (int i=historyMenu.getItemCount()-1; i>=firstHistoryItem; i--){
			if (historyItem(i-firstHistoryItem).equals(S)) return i-firstHistoryItem;
		}
		return -1;
	}
	
	int historyCount(){
		return historyMenu.getItemCount() - firstHistoryItem;
	}
	
	static class LoadedFile{
		File file; String method;
		LoadedFile(File file,String method){
			this.file=file; this.method=method;
		}
		public boolean equals(LoadedFile o){
			return file.equals(o.file) && method.equals(o.method);
		}
	}
	
	protected void addToReloaders(File file,String method){
		final LoadedFile lf = new LoadedFile(file,method);
		if (!loadedFiles.contains(lf)){
			loadedFiles.addElement(lf);
			addItemToMenu(fileMenu,file.getName(),new ActionListener(){
				public void actionPerformed(ActionEvent e){
					if (!checkEngineAvailable()) return;
					engine.command(lf.method+"('"+engine.unescapedFilePath(lf.file.getAbsolutePath())+ "')");
				}
			});
		}
	}
	
	public boolean successfulCommand(String s){
		try {
			return engine.command(s);
		}
		catch(Exception e){
			System.err.println("Trouble in successfulCommand for "+s+":\n"+e);
		}
		return false;
	}
	
	protected void reconsultFile(){
		if (!checkEngineAvailable()) return;
		String nome,directorio; File filetoreconsult=null;
		FileDialog d = new FileDialog(this,"Consult file...");
		d.setVisible(true);
		nome = d.getFile(); directorio = d.getDirectory();
		if (nome!=null) {
			filetoreconsult = new File(directorio,nome);
			if (engine.consultAbsolute(filetoreconsult))
				addToReloaders(filetoreconsult,"consult");
		}
	}

	/** For XSB only */
	protected void load_dynFile(){
		if (!checkEngineAvailable()) return;
		String nome,directorio; File filetoreconsult=null;
		FileDialog d = new FileDialog(this,"load_dyn file...");
		d.setVisible(true);
		nome = d.getFile(); directorio = d.getDirectory();
		if (nome!=null) {
			filetoreconsult = new File(directorio,nome);
			if (successfulCommand("load_dyn('"+engine.unescapedFilePath(filetoreconsult.getAbsolutePath())+ "')"))
				addToReloaders(filetoreconsult,"load_dyn");
		}
	}

	public void focusInput(){
		prologInput.selectAll();
		prologInput.requestFocus();
	}
		
	public void scrollToBottom(){
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				if (prologOutput.isShowing()) {
					int offset = prologOutput.getDocument().getEndPosition().getOffset();
					if (offset>0) offset--; /* OBOB hack */
					prologOutput.setCaretPosition(offset);
					try {
						// If we're in a JScrollPane, force scrolling to bottom and left
						JScrollBar scrollbarV = ((JScrollPane)((JViewport)(prologOutput.getParent())).getParent()).getVerticalScrollBar();
						scrollbarV.setValue(scrollbarV.getMaximum());
						JScrollBar scrollbarH = ((JScrollPane)((JViewport)(prologOutput.getParent())).getParent()).getHorizontalScrollBar();
						scrollbarH.setValue(scrollbarH.getMinimum());
					} catch (Exception e) {/* We're not in a JScrollPane, forget it! */};
				}
			}
		});
	}
	
	public static boolean debug = false;
	protected static String[] prologStartCommands=null;
	public static boolean loadFromJar = true;

	public static String commonMain(String args[]) {
		String initialFile = null;
		commonGreeting();
		if (args.length>=1){
			int i=0;
			while(i<args.length){
				if (args[i].toLowerCase().startsWith("-d")) {
					debug=true;
					i++;
				} else if (args[i].toLowerCase().startsWith("-nojar")){
					loadFromJar=false;
					i++;
				} else if (args[i].equals("-initfile")) {
                        initialFile = args[i + 1];
                        i = i + 2;
				} else {
					prologStartCommands = remainingArgs(args,i);
					break;
				}
			}
		} // else throw new IPException("Missing arguments in command line");
		return initialFile;
	}
	
	public static void commonGreeting(){
		System.out.println("Welcome "+System.getProperty("user.name")+" to InterProlog "+AbstractPrologEngine.version+" on Java "+
			System.getProperty("java.version") + " ("+
			System.getProperty("java.vendor") + "), "+ 
			System.getProperty("os.name") + " "+
			System.getProperty("os.version"));
	}
	
	public static String[] commandArgs(String[] args){
		return remainingArgs(args,0);
	}
	/** This handles args in a peculiar way to please the Windows batch file interpreter: 
	 the main executable/dir arg is the last one... its args are before it */
	public static String[] remainingArgs(String[] args,int first){
		if (args.length<first+1) throw new IPException("Missing arguments in command line");
		String[] cmds = new String[args.length-first];
		if (cmds.length==1) {
			cmds[0] = args[first];
			return cmds;
		}
		for (int i=first;i<args.length;i++){
			if (i==args.length-1) // last one is the Prolog executable/dir:
				cmds[0] = args[i];
			else cmds[i-first+1]=args[i];
		}
		return cmds;
	}
	
	public static void beep(){
		Toolkit.getDefaultToolkit().beep();
	}	
	
	public JTextComponent getOutputPane(){
		return prologOutput;
	}
	
	public AbstractPrologEngine getEngine(){
		return engine;
	}	

	public static void setWaitCursor(Component c) {
		if (c==null) return;
		Cursor wait = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR);
		if (c.getCursor().equals(wait)) return;
		previousCursors.put(c,c.getCursor());
		c.setCursor(wait);
	}

	public static void restoreCursor(Component C) {
		if (C==null) return;
		Cursor previousCursor = previousCursors.remove(C);
		if(previousCursor != null) {
			C.setCursor(previousCursor);
		}
	}
	/** This method does nothing. Subclasses may have a Windows menu which they should add to W...*/
	public void addWindowsMenuTo(Container W){}
}