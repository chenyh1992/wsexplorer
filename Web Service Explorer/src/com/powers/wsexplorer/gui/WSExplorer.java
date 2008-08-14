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
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Properties;
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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.grouplayout.GroupLayout;
import org.eclipse.swt.layout.grouplayout.LayoutStyle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

import com.powers.wsexplorer.ws.SimpleIO;
import com.powers.wsexplorer.ws.WSUtil;
import com.swtdesigner.SWTResourceManager;

public class WSExplorer {

	final static String VERSION = "0.4";
	
	final static String ENDPOINTS_FILE = "endpoints.txt";
	final static String SOAP_TEMPLATE_FILE = "SOAPTemplate.xml";
	final static String GPLV3_FILE = "gpl-3.0.txt";
	final static String SECONDS = "Seconds";
	final static String MILLISECONDS = "Milliseconds";
	final static String[] TIMEOUT_CHOICES = {SECONDS, MILLISECONDS};
	transient boolean CancelWasPressed = false;
	static SOAPConnection CONNECTION = null;
	
	final Shell shell = new Shell();
	
	BlockingQueue<Runnable> q = new ArrayBlockingQueue<Runnable>(10);
	ThreadPoolExecutor tpe = new ThreadPoolExecutor(5, 10, 10, TimeUnit.SECONDS, q);
	CompletionService<String> ecs = new ExecutorCompletionService<String>(tpe);
	
	AtomicBoolean isSending = new AtomicBoolean(false);
	AtomicBoolean shouldStopProgressBar = new AtomicBoolean(false);
	List<String> comboItems = getEndpointHistory();
	
	private Combo endpointCombo;
	private Text responseText;
	private Text requestText;
	ProgressBar progressBar;
	Label statusText;
	Exception statusTextException;
	Options options = null;
	Button cancelButton = null;
	
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
		shell.setImage(SWTResourceManager.getImage(WSExplorer.class, "/Earth-Scan-16x16.png"));
		shell.setMinimumSize(new Point(620, 665));
		shell.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(final DisposeEvent e) {
				tpe.shutdownNow();
				
				saveEndpointsToFile();
				deleteTemporaryFiles();
				
			}
		});
		//

		shell.open();
		
		requestText = new Text(shell, SWT.V_SCROLL | SWT.MULTI | SWT.H_SCROLL | SWT.BORDER);
		requestText.setFont(SWTResourceManager.getFont("Courier New", 8, SWT.NONE));

		Label requestLabel;
		requestLabel = new Label(shell, SWT.NONE);
		requestLabel.setText("Request");

		responseText = new Text(shell, SWT.V_SCROLL | SWT.MULTI | SWT.H_SCROLL | SWT.BORDER);
		responseText.setFont(SWTResourceManager.getFont("Courier New", 8, SWT.NONE));

		Label responseLabel;
		responseLabel = new Label(shell, SWT.NONE);
		responseLabel.setText("Response");

		Label wsExplorerLabel;
		wsExplorerLabel = new Label(shell, SWT.NONE);
		wsExplorerLabel.setToolTipText("Web Service Explorer");
		wsExplorerLabel.setFont(SWTResourceManager.getFont("", 14, SWT.NONE));
		wsExplorerLabel.setText("WS Explorer");

		endpointCombo = new Combo(shell, SWT.NONE);
		endpointCombo.setToolTipText("Enter Endpoint URL");
		// set the items in the combo from a file
		if(comboItems.size() > 0){
			String[] s = (String[]) comboItems.toArray(new String[comboItems.size()]);
			endpointCombo.setItems(s);
		} else {
			System.out.println("No saved endpoints found");
		}

		
		
		Label endpointLabel;
		endpointLabel = new Label(shell, SWT.NONE);
		endpointLabel.setText("Endpoint");



		progressBar = new ProgressBar(shell, SWT.NONE);
		progressBar.setToolTipText("Progress Of Current Action");

		Button sendButton2;
		sendButton2 = new Button(shell, SWT.NONE);
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
				td.setText(getText(new File(GPLV3_FILE)));
				td.open();
				
			}
		});
		viewLicenseMenuItem.setText("View License");

		ToolBar toolBar;
		toolBar = new ToolBar(shell, SWT.NONE);

		final ToolItem newItemToolItem_5 = new ToolItem(toolBar, SWT.PUSH);
		newItemToolItem_5.setText("Save");
		newItemToolItem_5.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				
				FileDialog fd = new FileDialog(shell, SWT.SAVE);
				fd.open();
				
				 if(fd.getFileName() != null && !fd.getFileName().equalsIgnoreCase("")){
					 String filename = fd.getFilterPath() + "\\" + fd.getFileName();
					 PrintWriter pw = null;
					 
					 
					 try {
						pw = SimpleIO.openFileForOutput(filename);
					} catch (Exception e1) {
						log(e1.getMessage());
						return;
					}
					 
					 pw.println(requestText.getText());
					 SimpleIO.close(pw);
					 
					 log("Saved file '"+fd.getFileName()+"'");
				 }
				
			}
		});
		newItemToolItem_5.setToolTipText("Save the text in the Request TextBox");


		final ToolItem newItemToolItem_6 = new ToolItem(toolBar, SWT.PUSH);
		newItemToolItem_6.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				
				FileDialog fd = new FileDialog(shell, SWT.OPEN);
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
		newItemToolItem_6.setText("Load");

		final ToolItem newItemToolItem_4 = new ToolItem(toolBar, SWT.SEPARATOR);
		newItemToolItem_4.setWidth(32);

		final ToolItem newItemToolItem = new ToolItem(toolBar, SWT.PUSH);
		newItemToolItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				requestText.setText("");
				responseText.setText("");
			}
		});
		newItemToolItem.setToolTipText("Clear All TextBoxes");
		newItemToolItem.setText("Clear All");

		final ToolItem newItemToolItem_1 = new ToolItem(toolBar, SWT.PUSH);
		newItemToolItem_1.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				requestText.setText("");
			}
		});
		newItemToolItem_1.setToolTipText("Clear Request TextBox");
		newItemToolItem_1.setText("Clear Request");

		final ToolItem newItemToolItem_2 = new ToolItem(toolBar, SWT.PUSH);
		newItemToolItem_2.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				responseText.setText("");
			}
		});
		newItemToolItem_2.setToolTipText("Clear Response TextBox");
		newItemToolItem_2.setText("Clear Response");

		
		statusText = new Label(shell, SWT.BORDER);
		statusText.setToolTipText("Status Messages Appear Here");
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

		final Menu responsePopUpMenu = new Menu(responseText);
		responseText.setMenu(responsePopUpMenu);

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
		
		
		final Menu requestPopUpMenu = new Menu(requestText);
		requestText.setMenu(requestPopUpMenu);

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
				
				String text = getText(new File(SOAP_TEMPLATE_FILE)); 
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
		cancelButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				CancelWasPressed = true;
				log("Cancelling current operation...");
			}
		});
		cancelButton.setToolTipText("Cancel the current operation.");
		cancelButton.setText("Cancel");
		
		cancelButton.setEnabled(false);
		
		final GroupLayout groupLayout = new GroupLayout(shell);
		groupLayout.setHorizontalGroup(
			groupLayout.createParallelGroup(GroupLayout.LEADING)
				.add(toolBar, GroupLayout.DEFAULT_SIZE, 612, Short.MAX_VALUE)
				.add(groupLayout.createSequentialGroup()
					.addContainerGap()
					.add(wsExplorerLabel, GroupLayout.PREFERRED_SIZE, 143, GroupLayout.PREFERRED_SIZE)
					.addContainerGap(460, Short.MAX_VALUE))
				.add(groupLayout.createSequentialGroup()
					.addContainerGap()
					.add(groupLayout.createParallelGroup(GroupLayout.LEADING)
						.add(responseText, GroupLayout.DEFAULT_SIZE, 564, Short.MAX_VALUE)
						.add(groupLayout.createSequentialGroup()
							.add(1, 1, 1)
							.add(responseLabel, GroupLayout.PREFERRED_SIZE, 69, GroupLayout.PREFERRED_SIZE)
							.addPreferredGap(LayoutStyle.RELATED, 494, Short.MAX_VALUE))
						.add(groupLayout.createSequentialGroup()
							.add(sendButton2, GroupLayout.PREFERRED_SIZE, 60, GroupLayout.PREFERRED_SIZE)
							.addPreferredGap(LayoutStyle.RELATED, 252, Short.MAX_VALUE)
							.add(cancelButton, GroupLayout.PREFERRED_SIZE, 60, GroupLayout.PREFERRED_SIZE)
							.addPreferredGap(LayoutStyle.RELATED)
							.add(progressBar, GroupLayout.PREFERRED_SIZE, 187, GroupLayout.PREFERRED_SIZE)))
					.add(39, 39, 39))
				.add(statusText, GroupLayout.DEFAULT_SIZE, 612, Short.MAX_VALUE)
				.add(groupLayout.createSequentialGroup()
					.addContainerGap()
					.add(groupLayout.createParallelGroup(GroupLayout.LEADING)
						.add(requestText, GroupLayout.DEFAULT_SIZE, 563, Short.MAX_VALUE)
						.add(groupLayout.createSequentialGroup()
							.add(1, 1, 1)
							.add(requestLabel, GroupLayout.PREFERRED_SIZE, 69, GroupLayout.PREFERRED_SIZE))
						.add(groupLayout.createSequentialGroup()
							.add(groupLayout.createParallelGroup(GroupLayout.LEADING)
								.add(endpointLabel)
								.add(endpointCombo, GroupLayout.DEFAULT_SIZE, 464, Short.MAX_VALUE))
							.add(99, 99, 99)))
					.add(40, 40, 40))
		);
		groupLayout.setVerticalGroup(
			groupLayout.createParallelGroup(GroupLayout.LEADING)
				.add(groupLayout.createSequentialGroup()
					.add(groupLayout.createParallelGroup(GroupLayout.LEADING)
						.add(groupLayout.createSequentialGroup()
							.add(toolBar, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE)
							.add(51, 51, 51)
							.add(endpointLabel)
							.addPreferredGap(LayoutStyle.RELATED)
							.add(endpointCombo, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
							.add(22, 22, 22)
							.add(groupLayout.createParallelGroup(GroupLayout.LEADING)
								.add(requestLabel, GroupLayout.PREFERRED_SIZE, 17, GroupLayout.PREFERRED_SIZE)
								.add(groupLayout.createSequentialGroup()
									.add(16, 16, 16)
									.add(requestText, GroupLayout.DEFAULT_SIZE, 189, Short.MAX_VALUE)))
							.add(18, 18, 18)
							.add(groupLayout.createParallelGroup(GroupLayout.LEADING)
								.add(responseLabel, GroupLayout.PREFERRED_SIZE, 17, GroupLayout.PREFERRED_SIZE)
								.add(groupLayout.createSequentialGroup()
									.add(16, 16, 16)
									.add(responseText, GroupLayout.DEFAULT_SIZE, 168, Short.MAX_VALUE))))
						.add(groupLayout.createSequentialGroup()
							.add(32, 32, 32)
							.add(wsExplorerLabel, GroupLayout.PREFERRED_SIZE, 28, GroupLayout.PREFERRED_SIZE)))
					.add(7, 7, 7)
					.add(groupLayout.createParallelGroup(GroupLayout.LEADING)
						.add(progressBar, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.add(groupLayout.createParallelGroup(GroupLayout.BASELINE)
							.add(sendButton2)
							.add(cancelButton)))
					.add(17, 17, 17)
					.add(statusText, GroupLayout.PREFERRED_SIZE, 18, GroupLayout.PREFERRED_SIZE))
		);
		shell.setLayout(groupLayout);
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
		
		// ===================================================
		// done with dealing with fully populated widgets
		// ===================================================
		
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		shell.pack();
		
		
		
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

		Shell shell = null;
		Text responseText = null;
		ProgressBar pb = null;
		Label statusText = null;
		Future<String> future = null;
		
		public PopulateResponse(Shell shell, Text responseText,
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
				} else {
					responseStringToSet.append(text);
					statusStringToSet.append("Got a response");
				}
			} else {
				if(!CancelWasPressed){
					statusStringToSet.append("Did not get a response back from Service");
				}
			}
			
			CancelWasPressed = false;
			
            shell.getDisplay().asyncExec(
                    new Runnable() {
                    public void run(){
                    	responseText.setText(responseStringToSet.toString());
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

			final int inc = 5;
			final long sleepTime = 750;
			
			shouldStopProgressBar.set(false);
			
            shell.getDisplay().asyncExec(
                    new Runnable() {
                    public void run(){
                    	pb.setSelection(0);
                    }});
			
            while(!shouldStopProgressBar.get()){
                shell.getDisplay().asyncExec(
                        new Runnable() {
                        public void run(){
                        	if(pb.getSelection() < 90){ 
                        	pb.setSelection(pb.getSelection()+inc);
                        	} else {
                        		shouldStopProgressBar.set(true);
                        	}
                        }});
                
                Thread.sleep(sleepTime);
            }
            
            shouldStopProgressBar.set(false);
            
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
	
	/**
	 * Gets all the text from the given file.
	 * @param f File to get the text from
	 * @return the contents of the file
	 */
	public String getText(File f){
		StringBuffer sb = new StringBuffer();
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

		return sb.toString();
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
}
