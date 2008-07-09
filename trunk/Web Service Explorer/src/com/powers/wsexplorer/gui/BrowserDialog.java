package com.powers.wsexplorer.gui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
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
		shell = new Shell(getParent(), SWT.APPLICATION_MODAL | SWT.TITLE | SWT.BORDER | SWT.CLOSE);
		shell.setLayout(new GridLayout());
		shell.setSize(621, 568);
		shell.setText("Browser View");

		browser = new Browser(shell, SWT.NONE);
		browser.setLayoutData(new GridData(610, 504));
		browser.setUrl(url);

		final Button okButton = new Button(shell, SWT.NONE);
		final GridData gd_okButton = new GridData(SWT.CENTER, SWT.CENTER, false, false);
		gd_okButton.widthHint = 55;
		okButton.setLayoutData(gd_okButton);
		okButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				shell.close();
			}
		});
		okButton.setText("OK");
		//
	}

	public void setURL(String s){
		this.url = s;
	}
}
