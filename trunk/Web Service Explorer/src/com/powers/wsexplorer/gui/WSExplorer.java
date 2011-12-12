/*
 *   Copyright 2008 Nick Powers.
 *   This file is part of WSExplorer.
 *
 *   WSExplorer is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   WSExplorer is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with WSExplorer.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.powers.wsexplorer.gui;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.xml.soap.SOAPConnection;

import org.apache.commons.lang.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Scrollable;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

import com.powers.wsexplorer.ws.SimpleIO;
import com.powers.wsexplorer.ws.WSUtil;
import com.swtdesigner.SWTResourceManager;

public class WSExplorer {

	final static String VERSION = "0.7";
	final int CONTROL_A = 'A' - 0x40; // the "control A" character
	final int CONTROL_S = 'S' - 0x40; // the "control S" character
	final int CONTROL_F = 'F' - 0x40; // the "control S" character
	final int CONTROL_Z = 'Z' - 0x40; // the "control Z" character
	final int CONTROL_Y = 'Y' - 0x40; // the "control Y" character
	
	private static final String UTF8 = "UTF-8";
	private static final String XML_EXT = "xml";
	private static String[] XML_FILTER_EXT = { "*.xml"};
	
	private final static String SAVED_STATE_FILE = "saved_state.txt";
	private final static String ENDPOINTS_FILE = "endpoints.txt";
	private final static String SOAP_TEMPLATE_FILE = "/SOAPTemplate.xml";
	private final static String GPLV3_FILE = "/gpl-3.0.txt";
	private final static String SECONDS = "Seconds";
	private final static String MILLISECONDS = "Milliseconds";
	final static String[] TIMEOUT_CHOICES = {SECONDS, MILLISECONDS};
	private transient boolean CancelWasPressed = false;
	private static SOAPConnection CONNECTION = null;
	private static final Image CHECK_IMAGE = SWTResourceManager.getImage(WSExplorer.class, "/check.png");
	private static final Image X_IMAGE = SWTResourceManager.getImage(WSExplorer.class, "/x.png");
	
	private final Shell shell = new Shell();
	
	private BlockingQueue<Runnable> q = new ArrayBlockingQueue<Runnable>(10);
	private ThreadPoolExecutor tpe = new ThreadPoolExecutor(5, 10, 10, TimeUnit.SECONDS, q);
	private CompletionService<String> ecs = new ExecutorCompletionService<String>(tpe);
	
	private AtomicBoolean isSending = new AtomicBoolean(false);
	private AtomicBoolean shouldStopProgressBar = new AtomicBoolean(false);
	private List<String> comboItems = getEndpointHistory();
	private Map<String, Scrollable> itemsToSaveState = new HashMap<String, Scrollable>();
	
	private Combo endpointCombo;
	private StyledText responseText;
	private StyledText requestText;
	private ProgressBar progressBar;
	private Label statusText;
	private Exception statusTextException;
	private Options options = null;
	private Button cancelButton = null;
	private Label timeElapsedLabel;
	private Label statusLabel;
	
	private UndoRedoListener requestTextUndoRedoListener = null;
	/**
	 * Launch the application
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			WSExplorer window = new WSExplorer();
			window.open();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Open the window
	 */
	public void open() {
		final Display display = Display.getDefault();
		
		shell.setText("WS Explorer");
		shell.setLayout(new FormLayout());
		shell.setImage(SWTResourceManager.getImage(WSExplorer.class, "/Earth-Scan-16x16.png"));
		shell.setMinimumSize(new Point(620, 665));
		shell.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(final DisposeEvent e) {
				tpe.shutdownNow();
				
				saveEndpointsToFile();
				saveStateToFile(itemsToSaveState, SAVED_STATE_FILE);
				
				// delete any temporary files used to display in 'Browser View'
				deleteTemporaryFiles();
				
			}
		});
		//

		
		
		Label requestLabel;
		
		
		Label responseLabel;

		Label wsExplorerLabel;
		wsExplorerLabel = new Label(shell, SWT.NONE);
		wsExplorerLabel.setImage(SWTResourceManager.getImage(WSExplorer.class, "/logo.png"));
		final FormData fd_wsExplorerLabel = new FormData();
		fd_wsExplorerLabel.bottom = new FormAttachment(0, 66);
		fd_wsExplorerLabel.top = new FormAttachment(0, 38);
		fd_wsExplorerLabel.right = new FormAttachment(0, 218);
		fd_wsExplorerLabel.left = new FormAttachment(0, 6);
		wsExplorerLabel.setLayoutData(fd_wsExplorerLabel);
		wsExplorerLabel.setToolTipText("Web Service Explorer");
		wsExplorerLabel.setFont(SWTResourceManager.getFont("", 14, SWT.NONE));
		wsExplorerLabel.setText("Web Service Explorer");

		endpointCombo = new Combo(shell, SWT.NONE);
		final FormData fd_endpointCombo = new FormData();
		fd_endpointCombo.right = new FormAttachment(100, -140);
		fd_endpointCombo.bottom = new FormAttachment(0, 117);
		fd_endpointCombo.top = new FormAttachment(0, 96);
		fd_endpointCombo.left = new FormAttachment(0, 9);
		endpointCombo.setLayoutData(fd_endpointCombo);
		endpointCombo.setToolTipText("Enter Endpoint URL. Right-click to clear a single or all items from the list");
		// set the items in the combo from a file
		if(comboItems.size() > 0){
			String[] s = (String[]) comboItems.toArray(new String[comboItems.size()]);
			endpointCombo.setItems(s);
		} else {
			System.out.println("No saved endpoints found");
		}

		
		
		Label endpointLabel;
		endpointLabel = new Label(shell, SWT.NONE);
		final FormData fd_endpointLabel = new FormData();
		fd_endpointLabel.bottom = new FormAttachment(0, 89);
		fd_endpointLabel.top = new FormAttachment(0, 76);
		fd_endpointLabel.right = new FormAttachment(0, 51);
		fd_endpointLabel.left = new FormAttachment(0, 9);
		endpointLabel.setLayoutData(fd_endpointLabel);
		endpointLabel.setText("Endpoint");



		progressBar = new ProgressBar(shell, SWT.NONE);
		final FormData fd_progressBar = new FormData();
		fd_progressBar.bottom = new FormAttachment(100, -22);
		fd_progressBar.top = new FormAttachment(100, -39);
		fd_progressBar.left = new FormAttachment(100, -189);
		fd_progressBar.right = new FormAttachment(100, -2);
		progressBar.setLayoutData(fd_progressBar);
		progressBar.setToolTipText("Progress Of Current Action");

		Button sendButton2;
		sendButton2 = new Button(shell, SWT.NONE);
		final FormData fd_sendButton2 = new FormData();
		fd_sendButton2.bottom = new FormAttachment(100, -30);
		fd_sendButton2.top = new FormAttachment(100, -53);
		fd_sendButton2.right = new FormAttachment(0, 69);
		fd_sendButton2.left = new FormAttachment(0, 9);
		sendButton2.setLayoutData(fd_sendButton2);
		sendButton2.setToolTipText("Send The Message");
		sendButton2.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				String endpointText = endpointCombo.getText();
				if(endpointText == null || endpointText.equals("")){
					statusText.setText("endpoint is null or empty");
					return;
				}
				
				String msgText = requestText.getText();
				if(msgText == null || msgText.equals("")){
					statusText.setText("SOAP request is null or empty");
					return;
				}
				
				// SAVE TO ENDPOINT URL LIST OF STRINGS
				boolean save = true;
				// check if we really want to save this...
				try {
					new URL(endpointText);
				} catch(MalformedURLException me){
					// don't save 
					save = false;
				}
				
				if(save){
					saveEndpoint(endpointText);
				}
				

				// clear response text
				responseText.setText("");
				// clear status image
				setStatusImage(null);
				
				// set status bar...
				log("Sending...");
				statusTextException = null;
				cancelButton.setEnabled(true);
				
				CONNECTION = WSUtil.getConnection();
				
				if(isSending.get()){
					log("Currently sending a message. You can only send one at a time...");
					return;
				}
				
				isSending.set(true);
				ExecuteWS aExecuteWS = new ExecuteWS(endpointText, msgText, CONNECTION);
				Future<String> executeWsFuture = ecs.submit(aExecuteWS);
				
				PopulateResponse aPopulateResponse = new PopulateResponse(shell, responseText, progressBar, statusText, executeWsFuture);
				ecs.submit(aPopulateResponse);
				
				IncrementProgressBar aIncrementProgressBar = new IncrementProgressBar(shell, progressBar, shouldStopProgressBar);
				ecs.submit(aIncrementProgressBar);
				
				IncrementTimeElapsed aIncrementTimeElapsed = new IncrementTimeElapsed(isSending);
				Future<String> timeElapsedFuture = ecs.submit(aIncrementTimeElapsed);
				
				PopulateTimeElapsed aPopulateTimeElapsed = new PopulateTimeElapsed(timeElapsedFuture);
				ecs.submit(aPopulateTimeElapsed);
			}
		});
		sendButton2.setText("Send");


		final Menu menu = new Menu(shell, SWT.BAR);
		shell.setMenuBar(menu);

		final MenuItem newSubmenuMenuItem = new MenuItem(menu, SWT.CASCADE);
		newSubmenuMenuItem.setText("File");

		// main menu
		final Menu menu_1 = new Menu(newSubmenuMenuItem);
		newSubmenuMenuItem.setMenu(menu_1);

		// menu item 1 - OPTIONS
		final MenuItem optionsMenuItem = new MenuItem(menu_1, SWT.NONE);
		optionsMenuItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				
				OptionsDialog od = new OptionsDialog(shell);
				od.setOptions(options);
				String resultMessage = (String)od.open();
				log(resultMessage);
			}
		});
		
		optionsMenuItem.setText("Options");
		
		// menu item 2 - EXIT
		final MenuItem newItemMenuItem_1 = new MenuItem(menu_1, SWT.NONE);
		newItemMenuItem_1.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				shell.close();
			}
		});
		
		newItemMenuItem_1.setText("Exit");
		
		// ==== EDIT MENU
		final MenuItem editMenu = new MenuItem(menu, SWT.CASCADE);
		editMenu.setText("Edit");

		final Menu editSubMenu = new Menu(editMenu);
		editMenu.setMenu(editSubMenu);
		
		final MenuItem findMenuItem = new MenuItem(editSubMenu, SWT.NONE);
		findMenuItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				FindDialog findDialog = new FindDialog(shell, responseText);
				findDialog.open();
			}
		});
		findMenuItem.setText("Find");
		
		

		final MenuItem helpMenu = new MenuItem(menu, SWT.CASCADE);
		helpMenu.setText("Help");

		final Menu menu_5 = new Menu(helpMenu);
		helpMenu.setMenu(menu_5);

		final MenuItem aboutMenuItem = new MenuItem(menu_5, SWT.NONE);
		aboutMenuItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				MessageBox mb = new MessageBox(shell, SWT.OK | SWT.ICON_INFORMATION);
                mb.setText("About WSExplorer");
                mb.setMessage("WSExplorer is an open source program written by Nick Powers.\n" +
                		"WSExplorer is protected by the GNU General Public License v3 (http://www.gnu.org/licenses/gpl.html).\n\n" +
                		"Version: "+VERSION+"\n" +
                		"Project Site: http://code.google.com/p/wsexplorer/");
                
                mb.open();
				
			}
		});
		aboutMenuItem.setText("About");

		final MenuItem viewLicenseMenuItem = new MenuItem(menu_5, SWT.NONE);
		viewLicenseMenuItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				
				TextDialog td = new TextDialog(shell);
				td.setTitle("GPL License");
				td.setText(getText(getClass().getResourceAsStream(GPLV3_FILE)));
				td.open();
				
			}
		});
		viewLicenseMenuItem.setText("View License");

		final MenuItem viewStackTraceMenuItem = new MenuItem(menu_5, SWT.NONE);
		viewStackTraceMenuItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
		
		 		if(statusTextException != null){
					TextDialog td = new TextDialog(shell);
					td.setTitle("Stack Trace");
					td.setText(getStrackTraceAsString(statusTextException));
					td.open();
				}
				 
			}
		});
		viewStackTraceMenuItem.setText("View Stacktrace");
		
		
		

		ToolBar toolBar;
		toolBar = new ToolBar(shell, SWT.NONE);
		final FormData fd_toolBar = new FormData();
		fd_toolBar.right = new FormAttachment(0, 323);
		fd_toolBar.bottom = new FormAttachment(0, 39);
		fd_toolBar.top = new FormAttachment(0, 0);
		fd_toolBar.left = new FormAttachment(0, 0);
		toolBar.setLayoutData(fd_toolBar);

		final ToolItem newItemToolItem_5 = new ToolItem(toolBar, SWT.PUSH);
		newItemToolItem_5.setText("Save");
		newItemToolItem_5.setImage(SWTResourceManager.getImage(WSExplorer.class, "/Save-icon.png"));
		newItemToolItem_5.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				
				saveToFile(requestText.getText(), XML_EXT);
			}
		});
		newItemToolItem_5.setToolTipText("Save the text in the Request TextBox");


		final ToolItem newItemToolItem_6 = new ToolItem(toolBar, SWT.PUSH);
		newItemToolItem_6.setImage(SWTResourceManager.getImage(WSExplorer.class, "/Open-icon.png"));
		newItemToolItem_6.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				
				FileDialog fd = new FileDialog(shell, SWT.OPEN);
				fd.setFilterExtensions(XML_FILTER_EXT);
				fd.open();
				if(fd.getFileName() != null && !fd.getFileName().equalsIgnoreCase("")){
			         String filename = fd.getFilterPath() + "\\" + fd.getFileName();
			         String text = getText(new File(filename));
			         requestText.setText(text);
             } else {
                     log("Did not choose a file");
             }
				
			}
		});
		newItemToolItem_6.setToolTipText("Load text into the Request TextBox");
		newItemToolItem_6.setText("Open");

		final ToolItem newItemToolItem_4 = new ToolItem(toolBar, SWT.SEPARATOR);
		newItemToolItem_4.setText(" ");
		newItemToolItem_4.setWidth(32);

		final ToolItem newItemToolItem = new ToolItem(toolBar, SWT.PUSH);
		newItemToolItem.setImage(SWTResourceManager.getImage(WSExplorer.class, "/Delete-icon.png"));
		newItemToolItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				requestText.setText("");
				responseText.setText("");
			}
		});
		newItemToolItem.setToolTipText("Clear All TextBoxes");
		newItemToolItem.setText("All");

		final ToolItem newItemToolItem_1 = new ToolItem(toolBar, SWT.PUSH);
		newItemToolItem_1.setImage(SWTResourceManager.getImage(WSExplorer.class, "/Delete-icon.png"));
		newItemToolItem_1.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				requestText.setText("");
			}
		});
		newItemToolItem_1.setToolTipText("Clear Request TextBox");
		newItemToolItem_1.setText("Request");

		final ToolItem newItemToolItem_2 = new ToolItem(toolBar, SWT.PUSH);
		newItemToolItem_2.setImage(SWTResourceManager.getImage(WSExplorer.class, "/Delete-icon.png"));
		newItemToolItem_2.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				responseText.setText("");
			}
		});
		newItemToolItem_2.setToolTipText("Clear Response TextBox");
		newItemToolItem_2.setText("Response");

		
		statusText = new Label(shell, SWT.BORDER);
		final FormData fd_statusText = new FormData();
		fd_statusText.bottom = new FormAttachment(100, 0);
		fd_statusText.top = new FormAttachment(100, -18);
		fd_statusText.left = new FormAttachment(0, 0);
		fd_statusText.right = new FormAttachment(100, 0);
		statusText.setLayoutData(fd_statusText);
		statusText.setToolTipText("Status Messages Appear Here. Right-Click for error details (if an error exists).");
		statusText.setBackground(SWTResourceManager.getColor(192, 192, 192));
		
		
		// Popup for Status Text
		final Menu statusTextPopUpMenu = new Menu(statusText);
		statusText.setMenu(statusTextPopUpMenu);

		// View Stack Trace Menu Item
		final MenuItem seeStackTrace_statusText_menuItem = new MenuItem(statusTextPopUpMenu, SWT.NONE);
		seeStackTrace_statusText_menuItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				if(statusTextException != null){
					TextDialog td = new TextDialog(shell);
					td.setTitle("Stack Trace");
					td.setText(getStrackTraceAsString(statusTextException));
					td.open();
				}
				
			}
		});
		seeStackTrace_statusText_menuItem.setText("See Full Stack Trace");
		
		// Clear Status Menu Item
		final MenuItem clearStatusText_menuItem = new MenuItem(statusTextPopUpMenu, SWT.NONE);
		clearStatusText_menuItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				log("");
			}
		});
		clearStatusText_menuItem.setText("Clear Status");
		
		
		
		
		final Menu menu_2 = new Menu(endpointCombo);
		endpointCombo.setMenu(menu_2);

		final MenuItem newItemMenuItem = new MenuItem(menu_2, SWT.NONE);
		newItemMenuItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				String s = endpointCombo.getText();
				if(s != null && !s.equals("")){
					comboItems.remove(s);
					
					updateEndpointCombo("");
				}
				
			}
		});
		
		newItemMenuItem.setText("Clear Current Item");

		final MenuItem newItemMenuItem_2 = new MenuItem(menu_2, SWT.NONE);
		newItemMenuItem_2.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				
				 MessageBox mb = new MessageBox(shell, SWT.YES | SWT.NO);
				 mb.setText("Clear All Items In Combo Box?");
				 mb.setMessage("Are you sure you want to clear the entire combo box?");
				 if(mb.open() == SWT.YES){
						comboItems.clear();
						updateEndpointCombo("");
						log("Cleared combo box");
				 }

			}
		});
		newItemMenuItem_2.setText("Clear All Items");

		cancelButton = new Button(shell, SWT.NONE);
		final FormData fd_cancelButton = new FormData();
		fd_cancelButton.left = new FormAttachment(100, -252);
		fd_cancelButton.top = new FormAttachment(100, -43);
		fd_cancelButton.bottom = new FormAttachment(100, -20);
		fd_cancelButton.right = new FormAttachment(progressBar, -3, SWT.LEFT);
		cancelButton.setLayoutData(fd_cancelButton);
		cancelButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				CancelWasPressed = true;
				log("Cancelling current operation...");
			}
		});
		cancelButton.setToolTipText("Cancel the current operation.");
		cancelButton.setText("Cancel");
		
		cancelButton.setEnabled(false);

		
		timeElapsedLabel = new Label(shell, SWT.NONE);
		final FormData fd_timeElapsedLabel = new FormData();
		fd_timeElapsedLabel.top = new FormAttachment(100, -54);
		fd_timeElapsedLabel.bottom = new FormAttachment(progressBar, -2, SWT.DEFAULT);
		fd_timeElapsedLabel.left = new FormAttachment(100, -182);
		fd_timeElapsedLabel.right = new FormAttachment(100, -2);
		timeElapsedLabel.setLayoutData(fd_timeElapsedLabel);

		final SashForm sashForm = new SashForm(shell, SWT.VERTICAL);
		sashForm.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		final FormData fd_sashForm = new FormData();
		fd_sashForm.bottom = new FormAttachment(100, -59);
		fd_sashForm.right = new FormAttachment(100, -2);
		fd_sashForm.left = new FormAttachment(0, 7);
		fd_sashForm.top = new FormAttachment(0, 122);
		sashForm.setLayoutData(fd_sashForm);

		final Composite composite = new Composite(sashForm, SWT.NONE);
		composite.setLayout(new FormLayout());
		requestLabel = new Label(composite, SWT.NONE);
		final FormData fd_requestLabel = new FormData();
		fd_requestLabel.bottom = new FormAttachment(0, 16);
		fd_requestLabel.top = new FormAttachment(0, 3);
		fd_requestLabel.right = new FormAttachment(0, 43);
		fd_requestLabel.left = new FormAttachment(0, 3);
		requestLabel.setLayoutData(fd_requestLabel);
		requestLabel.setText("Request");
		
		requestText = new StyledText(composite, SWT.V_SCROLL | SWT.MULTI | SWT.H_SCROLL | SWT.BORDER);
		final FormData fd_requestText = new FormData();
		fd_requestText.top = new FormAttachment(0, 19);
		fd_requestText.bottom = new FormAttachment(100, -4);
		fd_requestText.left = new FormAttachment(0, 3);
		fd_requestText.right = new FormAttachment(100, -2);
		requestText.setLayoutData(fd_requestText);
		
		// add undo listener
		requestTextUndoRedoListener = new UndoRedoListener(requestText);
		requestText.addExtendedModifyListener(requestTextUndoRedoListener.getListener());
		
		requestText.addKeyListener(new KeyAdapter() {
			public void keyPressed(final KeyEvent e) {
		          if (e.character == CONTROL_A) {
		        	  requestText.selectAll();
		           } else if(e.character == CONTROL_S){
		        	   saveToFile(requestText.getText(), XML_EXT);
		           } else if(e.character == CONTROL_F){
		        	   FindDialog findDialog = new FindDialog(shell,requestText);
		        	   findDialog.open();
		           } else if(e.character == CONTROL_Z){
		        	   requestTextUndoRedoListener.undo();
		           } else if(e.character == CONTROL_Y){
		        	   requestTextUndoRedoListener.redo();
		           }
		          
		          
			}
		});
		requestText.setFont(SWTResourceManager.getFont("Courier New", 10, SWT.NONE));
		requestText.addLineStyleListener(new HTMLLineStyler());
		

		final Menu requestPopUpMenu = new Menu(requestText);
		requestText.setMenu(requestPopUpMenu);

		addTextOperationsToPopupMenu(requestText, requestPopUpMenu);
		addSeperator(requestPopUpMenu);
		
		final MenuItem openInBrowserView_request_popUp = new MenuItem(requestPopUpMenu, SWT.NONE);
		openInBrowserView_request_popUp.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e){
				
				openInBrowserView(requestText.getText());

			}
		});
		openInBrowserView_request_popUp.setText("Open In Browser View");

		final MenuItem newItemMenuItem_3 = new MenuItem(requestPopUpMenu, SWT.NONE);
		newItemMenuItem_3.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				
				String text = getText(getClass().getResourceAsStream(SOAP_TEMPLATE_FILE)); 
				requestText.setText(text);
				
			}
		});
		newItemMenuItem_3.setText("Populate With SOAP Template");

		// pretty print menu item for request text box
		final MenuItem prettyPrint_request_popUp = new MenuItem(requestPopUpMenu, SWT.NONE);
		prettyPrint_request_popUp.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				String text = requestText.getText();
				if(text == null || ("").equalsIgnoreCase(text)){
					log("Did not pretty print...there is nothing to format!");
					return;
				}
				
				String prettyText = null;
				try {
					prettyText = WSUtil.prettyPrint(text);
					requestText.setText(prettyText);
					log("Text was pretty printed");
				}catch (Exception ex){
					log("Unable to pretty print due to malformed XML");
				}
				
			}
		});
		prettyPrint_request_popUp.setText("Pretty Print");

		final Composite composite_1 = new Composite(sashForm, SWT.NONE);
		composite_1.setLayout(new FormLayout());
		responseLabel = new Label(composite_1, SWT.NONE);
		final FormData fd_responseLabel = new FormData();
		fd_responseLabel.bottom = new FormAttachment(0, 16);
		fd_responseLabel.top = new FormAttachment(0, 3);
		fd_responseLabel.right = new FormAttachment(0, 50);
		fd_responseLabel.left = new FormAttachment(0, 3);
		responseLabel.setLayoutData(fd_responseLabel);
		responseLabel.setText("Response");

		responseText = new StyledText(composite_1, SWT.V_SCROLL | SWT.MULTI | SWT.H_SCROLL | SWT.BORDER);
		final FormData fd_responseText = new FormData();
		fd_responseText.top = new FormAttachment(0, 19);
		fd_responseText.right = new FormAttachment(100, 2);
		fd_responseText.left = new FormAttachment(0, 3);
		fd_responseText.bottom = new FormAttachment(100, -7);
		responseText.setLayoutData(fd_responseText);
		responseText.addKeyListener(new KeyAdapter() {
			public void keyPressed(final KeyEvent e) {
	          if (e.character == CONTROL_A) {
	        	  responseText.selectAll();
	           } else if(e.character == CONTROL_S){
	        	   saveToFile(responseText.getText(), XML_EXT);
	           } else if(e.character == CONTROL_F){
	        	   FindDialog findDialog = new FindDialog(shell,responseText);
	        	   findDialog.open();
	           }
			}
		});
		responseText.setFont(SWTResourceManager.getFont("Courier New", 10, SWT.NONE));
		responseText.addLineStyleListener(new HTMLLineStyler());

		final Menu responsePopUpMenu = new Menu(responseText);
		responseText.setMenu(responsePopUpMenu);

		addTextOperationsToPopupMenu(responseText, responsePopUpMenu);
		addSeperator(responsePopUpMenu);
		
		
		final MenuItem openInBrowserView_response_popUp = new MenuItem(responsePopUpMenu, SWT.NONE);
		openInBrowserView_response_popUp.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				
				openInBrowserView(responseText.getText());
				
			}
		});
		openInBrowserView_response_popUp.setText("Open In Browser View");

		
		// pretty print menu item for response text box
		final MenuItem prettyPrint_response_popUp = new MenuItem(responsePopUpMenu, SWT.NONE);
		prettyPrint_response_popUp.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				String text = responseText.getText();
				if(text == null || ("").equalsIgnoreCase(text)){
					log("Did not pretty print...there is nothing to format!");
					return;
				}
				
				String prettyText = null;
				try {
					prettyText = WSUtil.prettyPrint(text);
					responseText.setText(prettyText);
					log("Text was pretty printed");
				}catch (Exception ex){
					log("Unable to pretty print due to malformed XML");
				}
				
			}
		});
		prettyPrint_response_popUp.setText("Pretty Print");

		final Label label = new Label(shell, SWT.NONE);
		label.setToolTipText("WS Explorer");
		label.setImage(SWTResourceManager.getImage(WSExplorer.class, "/Earth-Scan-icon-128.png"));
		final FormData fd_label = new FormData();
		fd_label.bottom = new FormAttachment(0, 131);
		fd_label.top = new FormAttachment(0, 3);
		fd_label.left = new FormAttachment(100, -140);
		fd_label.right = new FormAttachment(100, -12);
		label.setLayoutData(fd_label);

		statusLabel = new Label(shell, SWT.NONE);
		final FormData fd_label_1 = new FormData();
		fd_label_1.bottom = new FormAttachment(100, -26);
		fd_label_1.right = new FormAttachment(0, 108);
		fd_label_1.top = new FormAttachment(100, -57);
		fd_label_1.left = new FormAttachment(sendButton2, 3, SWT.DEFAULT);
		statusLabel.setLayoutData(fd_label_1);
		sashForm.setWeights(new int[] {1, 1 });
		shell.layout();
		
		
		// ===================================================
		// do any finishing stuff with fully populated widgets
		// ===================================================
		
		options = getOptions();
		
		if(options.ignoreHostCertificates){
			try {
				WSUtil.ignoreCertificates();
			} catch (Exception e) {
				log(e.getMessage(), e);
				statusTextException = e;
				e.printStackTrace();
			}
		}
		
		//shell.pack();
		// ===================================================
		// done with dealing with fully populated widgets
		// ===================================================
		
		putItemsToSaveState();
		loadStateFromFile(SAVED_STATE_FILE, itemsToSaveState);
		
		shell.open();
		
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		
	}

	public void setStatusImage(final Boolean status){
		shell.getDisplay().asyncExec(
            new Runnable() {
            public void run(){
            	
            	if(status == null){
            		statusLabel.setImage(null);
            	} else if(status){
        			statusLabel.setImage(CHECK_IMAGE);
        		} else {
        			statusLabel.setImage(X_IMAGE);
        		}
            }});
	}
	
	private void saveStateToFile(Map<String, Scrollable> itemsToSave, String filename){
		if(itemsToSave == null || StringUtils.isBlank(filename)) { return; }
		
		Properties props = new Properties();
		
		Scrollable s = null;
		Text text = null;
		StyledText styledText = null;
		Combo combo = null;
		String value = null;
		
		Set<String> keys = itemsToSave.keySet();
		
		
		for(String str : keys){
			if(StringUtils.isBlank(str)){ continue; }
			
			s = itemsToSave.get(str);
			
			if(s instanceof Text){
				text = (Text)s;
			} else if(s instanceof StyledText){
				styledText = (StyledText)s;
			} else if(s instanceof Combo){
				combo = (Combo)s;
			} 
			
			// get the text out of the widget
			if(text != null){
				value = text.getText();
			} else if(styledText != null){
				value = styledText.getText();
			} else if(combo != null){
				value = combo.getText();
			} 
			
			if(StringUtils.isNotBlank(value)){
				props.put(str, value);
			}
			
			text = null;
			combo = null;
			styledText = null;
		}
		
		
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(new File(filename));
			props.store(fos, "State Saved");
		} catch (Exception e) {
			log("Unable to save state",e);
		}
	}
	
	
	private void loadStateFromFile(String filename, Map<String, Scrollable> itemsToSave){
		Properties props = new Properties();
		
		File file = new File(filename);
		if(!file.exists()) {
			log("No Saved State file (this is fine the first time)"); 
			return;
		}
		
		try {
			props.load(new FileInputStream(file));
		} catch (Exception e) {
			log("Unable to load state from the file " + filename, e);
			return;
		}
		
		Scrollable s = null;
		String value = null;
		Text text = null;
		StyledText styledText = null;
		Combo combo = null;
		
		Set<String> keys = itemsToSave.keySet();
		for(String key : keys){
			value = props.getProperty(key);
			s = itemsToSave.get(key);
			
			// if null or empty, just keep going to the next one
			if(StringUtils.isBlank(value) || s == null){
				continue;
			}
			
			// find the right widget
			if(s instanceof Text){
				text = (Text)s;
			} else if(s instanceof StyledText){
				styledText = (StyledText)s;
			} else if(s instanceof Combo){
				combo = (Combo)s;
			}
			
			// populate the right widget
			if(text != null){
				text.setText(value);
			} else if(styledText != null){
				styledText.setText(value);
			} else if(combo != null){
				combo.setText(value);
			}
			
			text = null;
			combo = null;
			styledText = null;
			
		}
	}
	
	private void putItemsToSaveState(){
		itemsToSaveState.put("endpointCombo", endpointCombo);
		itemsToSaveState.put("requestText", requestText);
		itemsToSaveState.put("responseText", responseText);
	}
	
	/**
	 * Handles the execution of the call. It's design to be used in a <tt>CompletionService</tt>.
	 *
	 */
	public class ExecuteWS implements Callable<String> {

		String endpointURL = null;
		String soapMessage = null;
		SOAPConnection connection = null;
		
		public ExecuteWS(String endpointURL, String soapMessage, SOAPConnection connection){
			this.endpointURL = endpointURL;
			this.soapMessage = soapMessage;
			this.connection = connection;
		}
		
		@Override
		public String call() throws Exception {
			return WSUtil.sendAndReceiveSOAPMessage(endpointURL, soapMessage, connection);
		}
		
	}
	
	/**
	 * Handles populating the response text box with the result when the 
	 * result is available. It takes the <tt>Future</tt> that is generated
	 * when submitting the <tt>ExecuteWS</tt> Class to the <tt>CompletionService</tt>.
	 */
	public class PopulateResponse implements Callable<String> {

		final Shell shell;
		final StyledText responseText;
		final ProgressBar pb;
		final Label statusText;
		final Future<String> future;
		
		public PopulateResponse(Shell shell, StyledText responseText,
				ProgressBar pb, Label statusText, Future<String> future) {
			super();
			this.shell = shell;
			this.responseText = responseText;
			this.pb = pb;
			this.statusText = statusText;
			this.future = future;
		}

		@Override
		public String call() throws Exception {

			final StringBuffer statusStringToSet = new StringBuffer();
			final StringBuffer responseStringToSet = new StringBuffer();
			
			while(!future.isDone()){
				// check if cancel was clicked...
				if(CancelWasPressed) {
					if(CONNECTION != null) {CONNECTION.close(); CONNECTION = null;}
					cancelSoapSend(future);
				} else {
					// sleep
					Thread.sleep(300);
				}
			}
			
			isSending.set(false);
			String text = "";
			try {
				text = future.get();
			} catch(CancellationException e){
				statusStringToSet.append("Operation was cancelled successfully");
			}
			
			// close the connection if it's open
			if(CONNECTION != null) {CONNECTION.close();}
			
			System.out.println("Text from the call: "+text);
			if(text != null && !text.equals("") && !CancelWasPressed){
				
				if(text.contains(WSUtil.ERROR_PREFIX)){
					statusStringToSet.append(text);
					responseStringToSet.append("No response was given");
					statusTextException = WSUtil.CURRENT_EXCEPTION;
					setStatusImage(false);
				} else {
					responseStringToSet.append(text);
					statusStringToSet.append("Got a response");
					setStatusImage(true);
				}
			} else {
				if(!CancelWasPressed){
					statusStringToSet.append("Did not get a response back from Service");
					setStatusImage(false);
				}
			}
			
			CancelWasPressed = false;
			
            shell.getDisplay().asyncExec(
                    new Runnable() {
                    public void run(){
                    	final String text = responseStringToSet.toString();
                    	final String prettyText = WSUtil.prettyPrint(text);
                    	responseText.setText(prettyText != null ? prettyText : text);
                    	statusText.setText(statusStringToSet.toString());
                    	shouldStopProgressBar.set(true);
                    	pb.setSelection(100);
                    	cancelButton.setEnabled(false);
                    }});

			
			
			return "";
		}
		
	}
	
	/**
	 * Increments the progress bar a little bit at a time until
	 * a process is finished. It stops at 90 so that is isn't
	 * reporting %100 when a task isn't finished.
	 */
	public class IncrementProgressBar implements Callable<String> {

		ProgressBar pb = null;
		Shell shell = null;
		AtomicBoolean shouldStopProgressBar = null;
		
		public IncrementProgressBar(Shell shell, ProgressBar pb, AtomicBoolean shouldStopProgressBar){
			this.shell = shell;
			this.pb = pb;
			this.shouldStopProgressBar = shouldStopProgressBar;
		}
		
		@Override
		public String call() throws Exception {

			final int inc = 4;
			final long sleepTime = 250;
			
			shouldStopProgressBar.set(false);
			
            shell.getDisplay().syncExec(
                    new Runnable() {
                    public void run(){
                    	pb.setSelection(0);
                    }});
			
            while(!shouldStopProgressBar.get()){
                shell.getDisplay().syncExec(
                        new Runnable() {
                        public void run(){
                        	
                        	if(pb.getSelection() >= 100){
                        		pb.setSelection(0);
                        	} else {
                        		pb.setSelection(pb.getSelection()+inc);
                        	} 
//                        	else {
//                        		shouldStopProgressBar.set(true);
//                        	}
                        }});
                
                Thread.sleep(sleepTime);
            }
            
            shouldStopProgressBar.set(false);
            
			return "";
		}
		
	}
	
	/**
	 * Increments the elapsed time counter until sending is done.
	 * @author np3849
	 *
	 */
	public class IncrementTimeElapsed implements Callable<String> {

		AtomicBoolean isSending = null;
		Calendar now = Calendar.getInstance();
		
		public IncrementTimeElapsed(AtomicBoolean isSending){
			this.isSending = isSending;
		}
		
		@Override
		public String call() throws Exception {
			String timeString = "";
			final long sleepTime = 1000;
			
			// update while we are trying to send
			while(isSending.get()){
				timeString = setTimeElapsed(now);
				Thread.sleep(sleepTime);
			}
            
			return timeString;
		}
		
	}
	
	/**
	 * Populates the elapsed time in the status bar.
	 * @author np3849
	 *
	 */
	public class PopulateTimeElapsed implements Callable<String> {

		Future<String> timeElapsedFuture = null;
		
		PopulateTimeElapsed(Future<String> timeElapsedFuture){
			this.timeElapsedFuture = timeElapsedFuture;
		}
		
		@Override
		public String call() throws Exception {
			log(" - " + timeElapsedFuture.get(), true);
			return "";
		}
		
	}
	
	
	/**
	 * Put text in the label at the bottom of the
	 * screen.
	 * @param t Text to display on the bottom label
	 */
	public void log(final String t){
        shell.getDisplay().asyncExec(
                new Runnable() {
                public void run(){
                	if(t == null){ return;}
                	statusText.setText(t);
                }});
	}
	
	public void log(final String t, final Exception e){
        shell.getDisplay().asyncExec(
                new Runnable() {
                public void run(){
                	statusText.setText(t);
                	statusTextException = e;
                }});
	}
	
	public void log(final String t, final boolean append){
        shell.getDisplay().asyncExec(
                new Runnable() {
                public void run(){
                	if(append){
                		statusText.setText(statusText.getText() + " " + t);
                	} else {
                		statusText.setText(t);
                	}
                }});
	}
	
	/**
	 * Sets the timeElapsedLabel with time since 'started'
	 * @param started
	 */
	public String setTimeElapsed(Calendar started){
		Calendar c = Calendar.getInstance();
		
		long start = started.getTimeInMillis();
		long now = c.getTimeInMillis();
		
		long elapsedMillis = now - start;
		
		int elapsedSeconds = (int)elapsedMillis / 1000;
		int elapsedMins = (int)elapsedMillis / (60*1000);
		int elapsedHours = (int)elapsedMillis / (60*60*1000);
		
		if(elapsedMins > 0) { elapsedSeconds -= (elapsedMins * 60); } // make it roll
		if(elapsedHours > 0) { elapsedMins -= (elapsedHours * 60); } // make it roll
		
		// format our String
		StringBuffer sb = new StringBuffer();
		sb.append("Elapsed Time: ");
		
		String timeType = null;
		
		if(elapsedSeconds == 1){
			timeType = " Second";
		} else {
			timeType = " Seconds";
		}
		
		if(elapsedHours > 0){
			sb.append(elapsedHours);
			sb.append(".");
			
			if(elapsedHours == 1 && elapsedMins == 1 && elapsedSeconds == 1){
				timeType = " Hour";
			} else {
				timeType = " Hours";
			}
		}
		
		if(elapsedMins > 0){
			sb.append(elapsedMins);
			sb.append(".");
			
			if(elapsedMins == 1 && elapsedSeconds == 1){
				timeType = " Minute";
			} else {
				timeType = " Minutes";
			}
		}
		
		sb.append(elapsedSeconds);
		sb.append(timeType);
		
		setTimeElapsedLabel(sb.toString());
		
		return sb.toString();
	}
	
	private void setTimeElapsedLabel(final String time){
        shell.getDisplay().asyncExec(
                new Runnable() {
                public void run(){
                	timeElapsedLabel.setText(time);
                }});
	}
	
	/**
	 * Gets all the text from the given file.
	 * @param is InputStream to get the text from
	 * @return the contents of the file
	 */
	public String getText(InputStream is){
		StringBuilder sb = new StringBuilder();
		String line = null;
		
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(is, UTF8));
			while ((line=reader.readLine()) != null) {
				sb.append(line);
				sb.append("\n");
			}
		} catch(Exception e){
			e.printStackTrace();
			return null;
		}

			// removes last \n
			return sb.substring(0,sb.length()-1);
	}
	
	/**
	 * Gets all the text from the given file.
	 * @param f File to get the text from
	 * @return the contents of the file
	 */
	public String getText(File f){
		StringBuilder sb = new StringBuilder();
		String line = null;
		
		try {
			BufferedReader reader = new BufferedReader(new FileReader(f));
			while ((line=reader.readLine()) != null) {
				sb.append(line);
				sb.append("\n");
			}
		} catch(Exception e){
			e.printStackTrace();
			return null;
		}

		// removes last \n
		return sb.substring(0,sb.length()-1);
	}

	/**
	 * Get the history from the properties file.
	 * @return list of saved endpoints
	 */
	public List<String> getEndpointHistory(){
		List<String> l = new ArrayList<String>();
		String line = null;

		
		BufferedReader br = SimpleIO.openFileForInput(ENDPOINTS_FILE);
		try {
			
			if(br != null){
				while((line = br.readLine()) != null){
					l.add(line);
				}
			}
			
		} catch(IOException e){
			return null;
		}
		
		return l;
	}
	
	/**
	 * Saves the endpoint in the combo box and our internal data structure.
	 * 
	 * @param s endpoint String to be saved.
	 */
	public void saveEndpoint(String s){
		if(!comboItems.contains(s)){
			comboItems.add(s);
		}
		
		updateEndpointCombo(s);
	}
	
	/**
	 * Updates the endpoint combo box with the internal data structure.
	 * It also sets the recently entered text to still be displayed as the
	 * selected text in the combo box (otherwise it would be blank).
	 * 
	 * @param s String to populate the currently selected text
	 */
	public void updateEndpointCombo(String s){
		String[] sArray = (String[]) comboItems.toArray(new String[comboItems.size()]);
		endpointCombo.setItems(sArray);
		endpointCombo.setText(s);
	}
	
	/**
	 * Saves the endpoints in the internal data structure to a file,
	 * one-per-line.
	 */
	public void saveEndpointsToFile(){
		PrintWriter pw = null;
		try {
			pw = SimpleIO.openFileForOutput(ENDPOINTS_FILE);
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		
		for(String s : comboItems){
			pw.println(s);
		}
		
		SimpleIO.close(pw);
	}
	
	public void deleteTemporaryFiles(){
		File f = new File(".");
		File deleteFile = null;
		
		String[] files = f.list(new TempFileFilter());
		for(int i=0; i<files.length; i++){
			
			deleteFile = new File(files[i]);
			if(deleteFile != null){
				deleteFile.delete();
			}
			
			deleteFile = null;
		}
	}

	/**
	 * Opens the text given inside a browser view as XML.
	 * @param text
	 */
	public void openInBrowserView(String text){
		
		if(text == null || text.equals("")){
			log("Didn't open browser view because there is nothing to display");
			return;
		}
		
		
		// create web page file
		String fname = Calendar.getInstance().getTime().getTime() + ".tmp.xml";
		PrintWriter pw = null;
		try {
			pw = SimpleIO.openFileForOutput(fname);
		} catch (Exception e1) {
			log("Caught exception: " + e1.getMessage());
			return;
		}
		
		pw.println(text);
		SimpleIO.close(pw);
		
		BrowserDialog bd = new BrowserDialog(shell);
		File f = new File(fname);
		String path = f.getAbsolutePath();
		bd.setURL(path);
		bd.open();

	}
	
	public String getStrackTraceAsString(Throwable t){
		if(t == null) { return null; }
		
		final Writer result = new StringWriter();
	    final PrintWriter printWriter = new PrintWriter(result);
	    t.printStackTrace(printWriter);
	    return result.toString();
	}
	
	public Options getOptions(){
		Options o = new Options();
		Properties options = GUIUtil.readPropertiesFile(OptionsDialog.OPTIONS_FILE);
		if(options != null){
			o.ignoreHostCertificates = Boolean.valueOf((String)options.get(Options.IGNORE_HOST_CERTIFICATS_KEY));
			// add any more options...
		}

		return o;
	}
	
	
	public void saveToFile(String text) {

		FileDialog fd = new FileDialog(shell, SWT.SAVE);
		fd.open();

		if (fd.getFileName() != null && !fd.getFileName().equalsIgnoreCase("")) {
			String filename = fd.getFilterPath() + "\\" + fd.getFileName();
			PrintWriter pw = null;

			try {
				pw = SimpleIO.openFileForOutput(filename);
			} catch (Exception e1) {
				log(e1.getMessage());
				return;
			}

			pw.println(text);
			SimpleIO.close(pw);

			log("Saved file '" + fd.getFileName() + "'");
		}
	}
	
	public void saveToFile(String text, String ext) {
		
		FileDialog fd = new FileDialog(shell, SWT.SAVE);
		fd.setFilterExtensions(new String[] {"*."+ext});
		fd.open();

		if (fd.getFileName() != null && !fd.getFileName().equalsIgnoreCase("")) {
			String filename = fd.getFilterPath() + "\\" + fd.getFileName();
			PrintWriter pw = null;

			try {
				pw = SimpleIO.openFileForOutput(filename);
			} catch (Exception e1) {
				log(e1.getMessage());
				return;
			}

			pw.println(text);
			SimpleIO.close(pw);

			log("Saved file '" + fd.getFileName() + "'");
		}
	}
	
	
	/**
	 * Cancel the current sending operation.
	 * @param future
	 */
	public void cancelSoapSend(Future<String> future){
		future.cancel(true);
	}
	
	public void setCancelButton(final boolean enable){
        shell.getDisplay().asyncExec(
                new Runnable() {
                public void run(){
                	cancelButton.setEnabled(enable);
                }});
		
	}
	
	/**
	 * Add the normal cut,copy,paste operations. These go away once the menu is customized.
	 * @param text the text widget that will be modified when the menu items are clicked
	 * @param popupMenu the popup menu to add the items
	 */
	private void addTextOperationsToPopupMenu(final StyledText text, final Menu popupMenu){
		
		// Cut
		final MenuItem cutTextPopUpMenu = new MenuItem(popupMenu, SWT.NONE);
		cutTextPopUpMenu.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				text.cut();
			}
		});
		cutTextPopUpMenu.setText("Cut");
		
		// Copy
		final MenuItem copyTextPopUpMenu = new MenuItem(popupMenu, SWT.NONE);
		copyTextPopUpMenu.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				text.copy();	
			}
		});
		copyTextPopUpMenu.setText("Copy");
		
		// Paste
		final MenuItem pasteTextPopUpMenu = new MenuItem(popupMenu, SWT.NONE);
		pasteTextPopUpMenu.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				text.paste();
			}
		});
		pasteTextPopUpMenu.setText("Paste");
		
		// Select All
		final MenuItem selectAllTextPopUpMenu = new MenuItem(popupMenu, SWT.NONE);
		selectAllTextPopUpMenu.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				text.selectAll();
			}
		});
		selectAllTextPopUpMenu.setText("Select All");
		
		
		// Clear All
		final MenuItem clearAllTextPopUpMenu = new MenuItem(popupMenu, SWT.NONE);
		clearAllTextPopUpMenu.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				text.setText(StringUtils.EMPTY);
			}
		});
		clearAllTextPopUpMenu.setText("Clear All");
		

	}
	
	/**
	 * Add a seperator to the menu.
	 * @param popupMenu
	 */
	private void addSeperator(Menu popupMenu){
		new MenuItem(popupMenu, SWT.SEPARATOR);
	}
}
