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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

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

	final String ENDPOINTS_FILE = "endpoints.txt";
	
	final Shell shell = new Shell();
	
	BlockingQueue<Runnable> q = new ArrayBlockingQueue<Runnable>(10);
	ThreadPoolExecutor tpe = new ThreadPoolExecutor(5, 10, 10, TimeUnit.SECONDS, q);
	CompletionService<String> ecs = new ExecutorCompletionService<String>(tpe);
	
	AtomicBoolean shouldStopProgressBar = new AtomicBoolean(false);
	List<String> comboItems = getEndpointHistory();
	
	private Combo endpointCombo;
	private Text responseText;
	private Text requestText;
	ProgressBar progressBar;
	Label statusText;
	
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
		shell.setMinimumSize(new Point(620, 665));
		shell.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(final DisposeEvent e) {
				
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
		wsExplorerLabel.setFont(SWTResourceManager.getFont("", 14, SWT.NONE));
		wsExplorerLabel.setText("WS Explorer");

		endpointCombo = new Combo(shell, SWT.NONE);
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

		Button sendButton2;
		sendButton2 = new Button(shell, SWT.NONE);
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
					URL url = new URL(endpointText);
				} catch(MalformedURLException me){
					// don't save 
					save = false;
				}
				
				if(save){
					saveEndpoint(endpointText);
				}
				

				
				
				ExecuteWS aExecuteWS = new ExecuteWS(endpointText, msgText);
				Future<String> executeWsFuture = ecs.submit(aExecuteWS);
				
				PopulateResponse aPopulateResponse = new PopulateResponse(shell, responseText, progressBar, statusText, executeWsFuture);
				ecs.submit(aPopulateResponse);
				
				IncrementProgressBar aIncrementProgressBar = new IncrementProgressBar(shell, progressBar);
				ecs.submit(aIncrementProgressBar);
				
			}
		});
		sendButton2.setText("Send");


		final Menu menu = new Menu(shell, SWT.BAR);
		shell.setMenuBar(menu);

		final MenuItem newSubmenuMenuItem = new MenuItem(menu, SWT.CASCADE);
		newSubmenuMenuItem.setText("File");

		final Menu menu_1 = new Menu(newSubmenuMenuItem);
		newSubmenuMenuItem.setMenu(menu_1);

		final MenuItem newItemMenuItem_1 = new MenuItem(menu_1, SWT.NONE);
		newItemMenuItem_1.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				shell.close();
			}
		});
		
		newItemMenuItem_1.setText("Exit");

		ToolBar toolBar;
		toolBar = new ToolBar(shell, SWT.NONE);

		final ToolItem newItemToolItem_5 = new ToolItem(toolBar, SWT.PUSH);
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
		newItemToolItem_5.setText("Save");

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
		statusText.setBackground(SWTResourceManager.getColor(192, 192, 192));
		final GroupLayout groupLayout = new GroupLayout(shell);
		groupLayout.setHorizontalGroup(
			groupLayout.createParallelGroup(GroupLayout.LEADING)
				.add(toolBar, GroupLayout.DEFAULT_SIZE, 824, Short.MAX_VALUE)
				.add(groupLayout.createSequentialGroup()
					.addContainerGap()
					.add(wsExplorerLabel, GroupLayout.PREFERRED_SIZE, 143, GroupLayout.PREFERRED_SIZE)
					.addContainerGap(672, Short.MAX_VALUE))
				.add(groupLayout.createSequentialGroup()
					.addContainerGap()
					.add(groupLayout.createParallelGroup(GroupLayout.LEADING)
						.add(endpointLabel)
						.add(endpointCombo, GroupLayout.DEFAULT_SIZE, 534, Short.MAX_VALUE))
					.add(281, 281, 281))
				.add(groupLayout.createSequentialGroup()
					.addContainerGap()
					.add(groupLayout.createParallelGroup(GroupLayout.LEADING)
						.add(requestText, GroupLayout.DEFAULT_SIZE, 775, Short.MAX_VALUE)
						.add(groupLayout.createSequentialGroup()
							.add(1, 1, 1)
							.add(requestLabel, GroupLayout.PREFERRED_SIZE, 69, GroupLayout.PREFERRED_SIZE)))
					.add(40, 40, 40))
				.add(GroupLayout.TRAILING, groupLayout.createSequentialGroup()
					.addContainerGap()
					.add(groupLayout.createParallelGroup(GroupLayout.TRAILING)
						.add(GroupLayout.LEADING, responseText, GroupLayout.DEFAULT_SIZE, 776, Short.MAX_VALUE)
						.add(GroupLayout.LEADING, groupLayout.createSequentialGroup()
							.add(1, 1, 1)
							.add(responseLabel, GroupLayout.PREFERRED_SIZE, 69, GroupLayout.PREFERRED_SIZE)
							.addPreferredGap(LayoutStyle.RELATED, 705, Short.MAX_VALUE))
						.add(groupLayout.createSequentialGroup()
							.add(sendButton2, GroupLayout.PREFERRED_SIZE, 60, GroupLayout.PREFERRED_SIZE)
							.addPreferredGap(LayoutStyle.RELATED, 529, Short.MAX_VALUE)
							.add(progressBar, GroupLayout.PREFERRED_SIZE, 187, GroupLayout.PREFERRED_SIZE)))
					.add(39, 39, 39))
				.add(GroupLayout.TRAILING, statusText, GroupLayout.DEFAULT_SIZE, 824, Short.MAX_VALUE)
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
									.add(requestText, GroupLayout.PREFERRED_SIZE, 190, GroupLayout.PREFERRED_SIZE)))
							.add(18, 18, 18)
							.add(groupLayout.createParallelGroup(GroupLayout.LEADING)
								.add(responseLabel, GroupLayout.PREFERRED_SIZE, 17, GroupLayout.PREFERRED_SIZE)
								.add(groupLayout.createSequentialGroup()
									.add(16, 16, 16)
									.add(responseText, GroupLayout.PREFERRED_SIZE, 167, GroupLayout.PREFERRED_SIZE))))
						.add(groupLayout.createSequentialGroup()
							.add(32, 32, 32)
							.add(wsExplorerLabel, GroupLayout.PREFERRED_SIZE, 28, GroupLayout.PREFERRED_SIZE)))
					.add(7, 7, 7)
					.add(groupLayout.createParallelGroup(GroupLayout.LEADING)
						.add(progressBar, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.add(sendButton2))
					.addPreferredGap(LayoutStyle.RELATED, 24, Short.MAX_VALUE)
					.add(statusText, GroupLayout.PREFERRED_SIZE, 18, GroupLayout.PREFERRED_SIZE))
		);

		final Menu menu_3 = new Menu(requestText);
		requestText.setMenu(menu_3);

		final MenuItem newItemMenuItem_3 = new MenuItem(menu_3, SWT.NONE);
		newItemMenuItem_3.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e){
				
				// get text from request text box
				String text = requestText.getText();
				if(text == null || text.equals("")){
					log("Didn't open browser view because there is nothing to display");
					return;
				}
				
				// create text for the web page
				StringBuffer sb = new StringBuffer();
				
				sb.append("<html><body>");
				sb.append(text);
				sb.append("</body></html>");
				
				// create web page file
				String fname = Calendar.getInstance().getTime().getTime() + ".tmp.html";
				PrintWriter pw = null;
				try {
					pw = SimpleIO.openFileForOutput(fname);
				} catch (Exception e1) {
					log("Caught exception: " + e1.getMessage());
					return;
				}
				
				pw.println(sb.toString());
				SimpleIO.close(pw);
				
				BrowserDialog bd = new BrowserDialog(shell);
				File f = new File(fname);
				String path = f.getAbsolutePath();
				bd.setURL(path);
				bd.open();
			}
		});
		newItemMenuItem_3.setText("Open In Browser View");

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
		shell.setLayout(groupLayout);
		shell.layout();
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
		
		public ExecuteWS(String endpointURL, String soapMessage){
			this.endpointURL = endpointURL;
			this.soapMessage = soapMessage;
		}
		
		@Override
		public String call() throws Exception {
			return WSUtil.sendAndReceiveSOAPMessage(endpointURL, soapMessage);
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
				// sleep
				Thread.sleep(300);
			}
			
			String text = future.get();
			
			if(text != null && !text.equals("")){
				
				if(text.contains("exception")){
					statusStringToSet.append(text);
					responseStringToSet.append("No response was given");
				} else {
					responseStringToSet.append(text);
					statusStringToSet.append("Got a response");
				}
			} else {
				statusStringToSet.append("Did not get a response back from Service");
			}
			
			
            shell.getDisplay().asyncExec(
                    new Runnable() {
                    public void run(){
                    	responseText.setText(responseStringToSet.toString());
                    	statusText.setText(statusStringToSet.toString());
                    	shouldStopProgressBar.set(true);
                    	pb.setSelection(100);
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
		
		public IncrementProgressBar(Shell shell, ProgressBar pb){
			this.shell = shell;
			this.pb = pb;
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

}
