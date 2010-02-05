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
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.swtdesigner.SWTResourceManager;

public class TextDialog extends Dialog {

	private Text text_1;
	protected Object result;
	protected Shell shell;
	String text = null;
	String title = null;
	boolean centered = true;
	
	/**
	 * Create the dialog
	 * @param parent
	 * @param style
	 */
	public TextDialog(Shell parent, int style) {
		super(parent, style);
	}

	/**
	 * Create the dialog
	 * @param parent
	 */
	public TextDialog(Shell parent) {
		this(parent, SWT.NONE);
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
		shell = new Shell(getParent(), SWT.MIN | SWT.TITLE | SWT.MAX | SWT.PRIMARY_MODAL | SWT.BORDER | SWT.RESIZE | SWT.CLOSE);
		shell.setLayout(new FillLayout());
		shell.setImage(SWTResourceManager.getImage(TextDialog.class, "/paper.png"));
		shell.setSize(669, 625);
		shell.setText(title);

		text_1 = new Text(shell, SWT.V_SCROLL | SWT.MULTI | SWT.H_SCROLL | SWT.BORDER);
		text_1.setFont(SWTResourceManager.getFont("Courier New", 10, SWT.NONE));

		
		text_1.setText(text);
		//
	}

	public void setText(String t){
		this.text = t;
	}
	
	public void setTitle(String title){
		this.title = title;
	}

	public void isCentered(boolean centered){
		this.centered = centered;
	}

}
