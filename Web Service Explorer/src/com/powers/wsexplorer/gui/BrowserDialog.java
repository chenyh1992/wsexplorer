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

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

public class BrowserDialog extends Dialog {

	protected Object result;
	protected Shell shell;
	Browser browser = null;
	String url = null;
	
	/**
	 * Create the dialog
	 * @param parent
	 * @param style
	 */
	public BrowserDialog(Shell parent, int style) {
		super(parent, style);
	}

	/**
	 * Create the dialog
	 * @param parent
	 */
	public BrowserDialog(Shell parent) {
		this(parent, SWT.NONE);
	}

	/**
	 * Open the dialog
	 * @return the result
	 */
	public Object open() {
		createContents();
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
		shell = new Shell(getParent(), SWT.MIN | SWT.APPLICATION_MODAL | SWT.TITLE | SWT.MAX | SWT.BORDER | SWT.CLOSE);
		shell.setLayout(new FormLayout());
		shell.setSize(800, 613);
		shell.setText("Browser View");

		browser = new Browser(shell, SWT.NONE);
		final FormData fd_browser = new FormData();
		fd_browser.bottom = new FormAttachment(100, 0);
		fd_browser.left = new FormAttachment(0, 5);
		fd_browser.top = new FormAttachment(0, 5);
		fd_browser.right = new FormAttachment(100, 0);
		browser.setLayoutData(fd_browser);
		browser.setUrl(url);
		//
	}

	public void setURL(String s){
		this.url = s;
	}
}
