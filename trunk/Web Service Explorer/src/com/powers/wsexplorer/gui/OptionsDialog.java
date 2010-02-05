/*
 *   Copyright 2010 Nick Powers.
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

import java.util.Properties;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

public class OptionsDialog extends Dialog {

	protected Object result;
	protected Shell shell;
	boolean centered = true;
	public static final String OPTIONS_FILE = "options.properties";
	Options options = null;
	
	// Widgets
	Button ignoreHostCertificatesButton = null;
	
	public OptionsDialog(Shell parent) {
		super(parent);
	}

	public OptionsDialog(Shell parent, int style) {
		super(parent, style);
	}

	/**
	 * Open the dialog
	 * @return the result
	 */
	public Object open() {
		createContents();
		
		if(centered){ GUIUtil.center(shell);}
		
		loadOptions(options);
		
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
		shell = new Shell(getParent(), SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
		shell.setSize(505, 258);
		shell.setText("Options");

		final Button saveButton = new Button(shell, SWT.NONE);
		saveButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				
				saveOptions();
				
				result = "Options saved";
				shell.dispose();
				
			}
		});
		saveButton.setText("Save");
		saveButton.setBounds(171, 193, 59, 23);

		final Button cancelButton = new Button(shell, SWT.NONE);
		cancelButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				
				shell.dispose();
				result = "Options discarded";
			}
		});
		cancelButton.setBounds(271, 193, 59, 23);
		cancelButton.setText("Cancel");

		final Group sendOptionsGroup = new Group(shell, SWT.NONE);
		sendOptionsGroup.setText("Send Options");
		sendOptionsGroup.setBounds(10, 10, 302, 163);

		ignoreHostCertificatesButton = new Button(sendOptionsGroup, SWT.CHECK);
		ignoreHostCertificatesButton.setBounds(10, 21,195, 16);
		ignoreHostCertificatesButton.setText("Ignore Host Certificates (for HTTPS)*");

		final Label restartRequiredLabel = new Label(shell, SWT.NONE);
		restartRequiredLabel.setText("* Restart Required");
		restartRequiredLabel.setBounds(10, 176, 123, 13);
		//
	}
	
	public void isCentered(boolean centered){
		this.centered = centered;
	}
	
	public void saveOptions(){
		Properties props = GUIUtil.readPropertiesFile(OPTIONS_FILE);

		options.ignoreHostCertificates = ignoreHostCertificatesButton.getSelection();
		props.put(Options.IGNORE_HOST_CERTIFICATS_KEY, String.valueOf(options.ignoreHostCertificates));
		
		GUIUtil.saveProperties(props, OPTIONS_FILE);
	}
	
	private void loadOptions(Options o){
		ignoreHostCertificatesButton.setSelection(o.ignoreHostCertificates);
		
	}
	
	public void setOptions(Options options){
		this.options = options;
	}

}
