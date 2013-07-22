package com.powers.wsexplorer.gui;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import com.swtdesigner.SWTResourceManager;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

public class HeaderDialog extends Dialog {

	protected Object result;
	protected Shell shell;
	private Table table;
	private TableColumn nameColumn = null;
	private TableColumn valueColumn = null;
	
	private Map<String,String> headers = new HashMap<String,String>();
	
	/**
	 * Create the dialog.
	 * @param parent
	 * @param style
	 */
	public HeaderDialog(Shell parent, int style) {
		super(parent, SWT.DIALOG_TRIM | SWT.SYSTEM_MODAL);
		setText("Headers");
	}
	
	public HeaderDialog(Shell parent) {
		super(parent, SWT.DIALOG_TRIM);
		setText("Headers");
	}
	
	public HeaderDialog(Shell parent, Map<String,String> headers) {
		super(parent, SWT.DIALOG_TRIM);
		setText("Headers");
		this.headers = headers;
	}

	/**
	 * Open the dialog.
	 * @return the result
	 */
	public Object open() {
		createContents();
		shell.open();
		shell.layout();
		Display display = getParent().getDisplay();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		return headers;
	}

	/**
	 * Create contents of the dialog.
	 */
	private void createContents() {
		//headers.put("test","blah");
		//headers.put("test2","blah2");
		
		shell = new Shell(getParent(), getStyle());
		shell.setSize(500, 300);
		shell.setText(getText());
		shell.setLayout(new FillLayout(SWT.HORIZONTAL));
		shell.setImage(SWTResourceManager.getImage(HeaderDialog.class, "/headers.png"));
		
		Composite composite = new Composite(shell, SWT.NONE);
		composite.setLayout(new FormLayout());
		
		table = new Table(composite, SWT.BORDER | SWT.FULL_SELECTION);
		FormData fd_table = new FormData();
		fd_table.bottom = new FormAttachment(100);
		fd_table.right = new FormAttachment(0, 494);
		fd_table.left = new FormAttachment(0);
		table.setLayoutData(fd_table);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		
		nameColumn = new TableColumn(table, SWT.NONE);
		nameColumn.setWidth(250);
		nameColumn.setText("Name");
		
		valueColumn = new TableColumn(table, SWT.NONE);
		valueColumn.setWidth(250);
		valueColumn.setText("Value");

		populateTable(this.headers);
		
		final TableEditor editor = new TableEditor(table);
		
		ToolBar toolBar = new ToolBar(composite, SWT.FLAT);
		fd_table.top = new FormAttachment(toolBar);
		FormData fd_toolBar = new FormData();
		fd_toolBar.right = new FormAttachment(100, -298);
		fd_toolBar.left = new FormAttachment(0);
		fd_toolBar.top = new FormAttachment(0);
		toolBar.setLayoutData(fd_toolBar);
		
		ToolItem addMenuItem = new ToolItem(toolBar, SWT.NONE);
		addMenuItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				addEmptyEntry();
			}
		});
		addMenuItem.setToolTipText("Add Header");
		addMenuItem.setImage(SWTResourceManager.getImage(WSExplorer.class, "/add.png"));
		
		ToolItem deleteMenuItem = new ToolItem(toolBar, SWT.NONE);
		deleteMenuItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				
				table.remove (table.getSelectionIndices ());
				
//				TableItem[] tis = table.getSelection();
//				for(TableItem ti : tis){
//					//String key = ti.getText(0);
//					//System.out.println(key);
//					//headers.remove(key);
//					ti.dispose();
//				}
				
			}
		});
		deleteMenuItem.setImage(SWTResourceManager.getImage(WSExplorer.class, "/Delete-icon.png"));
		
		ToolItem tltmNewItem_2 = new ToolItem(toolBar, SWT.NONE);
		tltmNewItem_2.setWidth(15);
		
		ToolItem saveMenuItem = new ToolItem(toolBar, SWT.NONE);
		saveMenuItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Map<String,String> headersFromTable = new HashMap<String,String>();
				
				// table to map
				TableItem[] tableItems = table.getItems();
				for(TableItem item : tableItems){
					String key = item.getText(0);
					String value = item.getText(1);
					
					if(StringUtils.isEmpty(key) || StringUtils.isEmpty(value)){
						continue;
					}
					
					headersFromTable.put(key, value);
				}
				
				headers = headersFromTable; // store in "this"
				
			}
		});
		saveMenuItem.setToolTipText("Save");
		saveMenuItem.setImage(SWTResourceManager.getImage(WSExplorer.class, "/Save-icon.png"));
		
		editor.horizontalAlignment = SWT.LEFT;
		editor.grabHorizontal = true;
		editor.minimumWidth = 50;

		// code from Eclipse Snippets (http://www.eclipse.org/swt/snippets/)
		// http://git.eclipse.org/c/platform/eclipse.platform.swt.git/tree/examples/org.eclipse.swt.snippets/src/org/eclipse/swt/snippets/Snippet124.java
		table.addListener (SWT.MouseDoubleClick, new Listener () {
			public void handleEvent (Event event) {
				//boolean newEntry = true;
				
				Rectangle clientArea = table.getClientArea();
				Point pt = new Point (event.x, event.y);
				int index = table.getTopIndex();
				
				while (index < table.getItemCount ()) {
					boolean visible = false;
					final TableItem item = table.getItem (index);
					
					for(int i=0; i<table.getColumnCount (); i++) {
						Rectangle rect = item.getBounds (i);
						if (rect.contains (pt)) {
							//newEntry = false;
							final int column = i;
							final Text text = new Text (table, SWT.NONE);
							
							Listener textListener = new Listener () {
								public void handleEvent (final Event e) {
									switch (e.type) {
										case SWT.FocusOut:
											if(!item.isDisposed()){
												item.setText(column, text.getText ());
												text.dispose();
											}
											break;
										case SWT.Traverse:
											switch (e.detail) {
												case SWT.TRAVERSE_RETURN:
													item.setText (column, text.getText ());
													//FALL THROUGH
												case SWT.TRAVERSE_ESCAPE:
													text.dispose ();
													e.doit = false;
											}
											break;
									}
								}
							};
							
							text.addListener (SWT.FocusOut, textListener);
							text.addListener (SWT.Traverse, textListener);
							editor.setEditor (text, item, i);
							text.setText (item.getText (i));
							text.selectAll ();
							text.setFocus ();
							return;
						}
						if (!visible && rect.intersects (clientArea)) {
							visible = true;
						}
					}
					if (!visible) return;
					index++;
				}
				
				//if(newEntry){
					//System.out.println("new entry");
				//}
			}
		});
		
		
	}
	
	public void addHeader(String name, String value){
		headers.put(name, value);
		addRow(name,value);
	}
	
	public void addEmptyEntry(){
		addRow("AddKey", "AddValue");
	}
	
	private void addRow(String name, String value){
		addTableItem(name, value);
	}
	
	private void populateTable(Map<String,String> map){
		if(map == null || map.isEmpty()) { return; }
		
		List<String> sortedList = GUIUtil.sortKeys(map);
		String value = null;

		for(String key : sortedList){
			if(StringUtils.isNotBlank(key)){
				value = map.get(key);
				if(StringUtils.isNotBlank(value)){
					addTableItem(key, value);
				}
			}
		}
		
		//GUIUtil.packAllColumns(table);
		
		//nameColumn.addListener(SWT.Selection, GUIUtil.createTextSortListener(table, map));
		//valueColumn.addListener(SWT.Selection, GUIUtil.createTextSortListener(table, map, true));
	}

	private void addTableItem(String apiName, String code){
		TableItem ti = new TableItem(table, SWT.BORDER);
		ti.setText(0, apiName);
		ti.setText(1, code);
	}
	
	public Map<String,String> getHeaders(){
		return headers;
	}
}
