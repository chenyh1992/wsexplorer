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

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.swtdesigner.SWTResourceManager;

public class FindDialog extends Dialog {

	protected Object result;
	protected Shell shell;
	boolean centered = true;
	StyledText textBoxToSearch = null;
	Label statusLabel = null;
	int startOffset = 0;
	Text textToSearchFor = null;
	Button matchCaseButton = null;
	Button matchWholeWordButton = null;
	Button nextButton = null;
	
	/**
	 * Create the dialog
	 * @param parent
	 * @param style
	 */
	private FindDialog(Shell parent, int style) {
		super(parent, style);
	}
	
	public FindDialog(Shell parent, StyledText textToSearch, int style) {
		this(parent, style);
		this.textBoxToSearch = textToSearch;
	}
	
	public FindDialog(Shell parent, StyledText textToSearch) {
		this(parent, textToSearch, SWT.NONE);
	}

	/**
	 * Open the dialog
	 * @return the result
	 */
	public Object open() {
		createContents();
		
		if(centered){ GUIUtil.center(shell);}
		
		shell.open();
		shell.layout();
		Display display = getParent().getDisplay();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		return result;
	}

	/**
	 * Create contents of the dialog
	 */
	protected void createContents() {
		shell = new Shell(getParent(), SWT.TITLE | SWT.BORDER | SWT.CLOSE);
		shell.setImage(SWTResourceManager.getImage(FindDialog.class, "/find-icon.png"));
		shell.setText("Find");
		shell.setLayout(new FormLayout());
		shell.setSize(380, 145);

		textToSearchFor = new Text(shell, SWT.BORDER);
		final FormData fd_text_1 = new FormData();
		fd_text_1.right = new FormAttachment(0, 365);
		fd_text_1.left = new FormAttachment(0, 35);
		textToSearchFor.setLayoutData(fd_text_1);
		
		textToSearchFor.addKeyListener(new KeyAdapter() {
			public void keyPressed(final KeyEvent e) {
				
				if (e.character == SWT.CR){
					log(""); //clear status box
					startOffset = search(textBoxToSearch, 
										 startOffset, 
										 textToSearchFor.getText(), 
										 matchCaseButton.getSelection(), 
										 matchWholeWordButton.getSelection());
				}
				
			}});
		
		
		
		Label findLabel;
		findLabel = new Label(shell, SWT.NONE);
		fd_text_1.bottom = new FormAttachment(findLabel, 19, SWT.TOP);
		fd_text_1.top = new FormAttachment(findLabel, 0, SWT.TOP);
		final FormData fd_findLabel = new FormData();
		fd_findLabel.right = new FormAttachment(0, 48);
		fd_findLabel.top = new FormAttachment(0, 21);
		fd_findLabel.bottom = new FormAttachment(0, 34);
		fd_findLabel.left = new FormAttachment(0, 8);
		findLabel.setLayoutData(fd_findLabel);
		findLabel.setText("Find:");

		nextButton = new Button(shell, SWT.NONE);
		nextButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				log(""); //clear status box
				startOffset = search(textBoxToSearch, 
									 startOffset, 
									 textToSearchFor.getText(), 
									 matchCaseButton.getSelection(), 
									 matchWholeWordButton.getSelection());
			}
		});
		final FormData fd_nextButton = new FormData();
		fd_nextButton.top = new FormAttachment(0, 67);
		fd_nextButton.right = new FormAttachment(0, 358);
		fd_nextButton.left = new FormAttachment(0, 247);
		nextButton.setLayoutData(fd_nextButton);
		nextButton.setText("Next");

		matchCaseButton = new Button(shell, SWT.CHECK);
		final FormData fd_matchCaseButton = new FormData();
		fd_matchCaseButton.top = new FormAttachment(0, 48);
		fd_matchCaseButton.left = new FormAttachment(0, 43);
		matchCaseButton.setLayoutData(fd_matchCaseButton);
		matchCaseButton.setText("Match case");

		matchWholeWordButton = new Button(shell, SWT.CHECK);
		final FormData fd_matchWholeWordButton = new FormData();
		fd_matchWholeWordButton.top = new FormAttachment(0, 48);
		fd_matchWholeWordButton.left = new FormAttachment(0, 135);
		matchWholeWordButton.setLayoutData(fd_matchWholeWordButton);
		matchWholeWordButton.setText("Match whole word only");

		statusLabel = new Label(shell, SWT.BORDER);
		statusLabel.setBackground(SWTResourceManager.getColor(192, 192, 192));
		final FormData fd_statusLabel = new FormData();
		fd_statusLabel.top = new FormAttachment(100, -15);
		fd_statusLabel.left = new FormAttachment(0, 0);
		fd_statusLabel.right = new FormAttachment(100, 0);
		fd_statusLabel.bottom = new FormAttachment(100, 0);
		statusLabel.setLayoutData(fd_statusLabel);
		//
	}

	public void isCentered(boolean centered){
		this.centered = centered;
	}
	
	public int search(StyledText st, int startOffset, String searchString, boolean caseSensitive, boolean matchWholeWord){
		if(searchString == null || "".equals(searchString)){ log("Enter something to search for!"); return 0;}
		
		String text = st.getText();
		if(text == null || "".equals(text)){ log("No text to search!"); return 0; }
		
		if(!caseSensitive){
			text = text.toLowerCase();
			searchString = searchString.toLowerCase();
		}
		
		if(matchWholeWord){
			StringBuffer sb = new StringBuffer();
			
			if(searchString.indexOf(0) != ' '){
				sb.append(" ");
			}
			
			sb.append(searchString);
			
			if(searchString.indexOf(searchString.length()-1) != ' '){
				sb.append(" ");
			}
			
			searchString = sb.toString();
		}
		
		// not case sensitive
		int index = text.indexOf(searchString, startOffset);
		int nextStart = 0;
		if(index < 0){
			
			if(startOffset == 0){
				log("No matches found!");
				st.setSelection(0);
			} else {
				log("No more found!");
				st.setSelection(0);
			}
			
		} else {
			nextStart = index + searchString.length();
			st.setSelection(index, nextStart);
		}
		
		return nextStart;
	}
	
	private void log(final String s){
		statusLabel.setText(s == null ? "" : s);
	}
	
}
