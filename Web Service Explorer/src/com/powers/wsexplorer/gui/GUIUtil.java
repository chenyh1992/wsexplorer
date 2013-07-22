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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.Collator;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

public class GUIUtil {

	/**
	 * Center the the shell to it's parent.
	 * @param shell
	 */
	public static void center(Shell shell){
		Rectangle parentSize = shell.getParent().getBounds();
		Rectangle mySize = shell.getBounds();

		int locationX, locationY;
		locationX = (parentSize.width - mySize.width)/2+parentSize.x;
		locationY = (parentSize.height - mySize.height)/2+parentSize.y;

		shell.setLocation(new Point(locationX, locationY));

	}
	
	/**
	 * Read the properties file from the class-path.
	 * @param file
	 * @return
	 */
	public static Properties readPropertiesFile(String file) {
		Properties p = new Properties();
		//InputStream fis = ClassLoader.getSystemClassLoader().getResourceAsStream(file);
		try {
			p.load(new FileInputStream(file));
		} catch (Exception e) {
			return null;
		} 
		return p;
	}
	
	public static void saveProperties(Properties p, String file){
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(new File(file));
			p.store(fos, "User saved");
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		if(fos != null){
			try {
				fos.close();
			} catch (IOException e) {
				e.printStackTrace();
				// ignore
			}
		}
	}
	
	public static Color getColor(RGB rgb){
		return new Color(Display.getCurrent(), rgb);
	}
	
	/**
	 * Clears the table of all rows and columns.<br>
	 * Uses removeAll, then disposes each TableColumn.
	 * 
	 * @param table table to clear all items
	 */
	public static void clearTable(final Table table){ 
		
		table.removeAll();
		TableColumn[] columns = table.getColumns();
		
		for(TableColumn c : columns){
			c.dispose();
		}
		
	}
	

	public static void clearTableLeaveHeaders(final Table table){ 
		
		while (table.getColumnCount() > 1 ) {
		    table.getColumns()[0].dispose();
		}
		
//		TableColumn[] columns = table.getColumns();
//		for(int i=1; i<columns.length; i++){
//			table.remove(i);
//			columns[i].dispose();
//		}
		
	}
	
	/**
	 * Takes the map and returns the keys as a sorted list.
	 * Sorted by using the Collator. 
	 * 
	 * @see Collator
	 * @param map
	 * @return List of sorted keys
	 */
	public static List<String> sortKeys(Map<String,?> map){
		
		Set<String> keys = map.keySet();
		List<String> listToSort = new LinkedList<String>();
		listToSort.addAll(keys);
		Collections.sort(listToSort, Collator.getInstance(Locale.getDefault()));
		
		return listToSort;
	}
	
	public static Listener createTextSortListener(final Table table, final List<String> list){
		Listener sortListener = new Listener() {
	        public void handleEvent(Event e) {
	        	
	        	final int direction = table.getSortDirection();
	        	
	            Collator collator = Collator.getInstance(Locale.getDefault());
	            TableColumn column = (TableColumn)e.widget;       	   

	            Collections.sort(list, collator);
	            
	            if (direction == SWT.DOWN) {
	            	Collections.reverse(list);
	            	table.setSortDirection(SWT.UP);
	            } else {
	            	table.setSortDirection(SWT.DOWN);
	            }
	            
	            table.removeAll();
	            
	    		Iterator<String> itr = list.iterator();
	    		while(itr.hasNext()){
	    			String value = itr.next();
	    			if(StringUtils.isNotBlank(value)){
	    				TableItem ti = new TableItem(table, SWT.BORDER);
	    				ti.setText(0, value);
	    			}
	    		}
	            
	            table.setSortColumn(column);
	        }
	    };
	    
	    return sortListener;
	}
	
public static Listener createTextSortListener(final Table table, final Map<String,String> map){
		
		Listener sortListener = new Listener() {
	        public void handleEvent(Event e) {
	        	
	        	final int direction = table.getSortDirection();
	        	
	            Collator collator = Collator.getInstance(Locale.getDefault());
	            TableColumn column = (TableColumn)e.widget;       
	            
	            Set<String> keys = map.keySet();
	            List<String> l = new LinkedList<String>();
	            l.addAll(keys);
	            

	            Collections.sort(l, collator);
	            
	            if (direction == SWT.DOWN) {
	            	Collections.reverse(l);
	            	table.setSortDirection(SWT.UP);
	            } else {
	            	table.setSortDirection(SWT.DOWN);
	            }
	            
	            table.removeAll();
	            
	    		Iterator<String> itr = l.iterator();
	    		String key = null;
	    		String value = null;
	    		
	    		while(itr.hasNext()){
	    			key = itr.next();
	    			if(StringUtils.isNotBlank(key)){
	    				TableItem ti = new TableItem(table, SWT.BORDER);
	    				ti.setText(0, key);
	    				value = map.get(key);
	    				if(StringUtils.isNotBlank(value)){
	    					ti.setText(1, value);
	    				}
	    			}

	    		}
	            
	            table.setSortColumn(column);
	        }
	    };
	    
	    return sortListener;
	}
	
	
	public static Listener createTextSortListener(final Table table, final Map<String,String> map, final boolean sortOnValues){
		
		Listener sortListener = new Listener() {
	        public void handleEvent(Event e) {
	        	
	        	final int direction = table.getSortDirection();
	        	
	            Collator collator = Collator.getInstance(Locale.getDefault());
	            TableColumn column = (TableColumn)e.widget;
	            
	            Set<String> keys = null; 
	            List<String> l = new LinkedList<String>();
	            Iterator<String> itr = null;
	            
	            if(sortOnValues){
	            	
	            	TreeMap<String,String> tm = new TreeMap<String,String>(new MapValueComparator(map, direction == SWT.DOWN));
	            	tm.putAll(map);
	            	
		            if (direction == SWT.DOWN) {
		            	table.setSortDirection(SWT.UP);
		            } else {
		            	table.setSortDirection(SWT.DOWN);
		            }
		            
		            itr = tm.keySet().iterator();
	            } else {
	            	
	            	keys = map.keySet();
		            l.addAll(keys);
		            Collections.sort(l, collator);
	            	
		            if (direction == SWT.DOWN) {
		            	Collections.reverse(l);
		            	table.setSortDirection(SWT.UP);
		            } else {
		            	table.setSortDirection(SWT.DOWN);
		            }
		            
		            itr = l.iterator();
	            }
	            
	           
	            // remove all table data
	            table.removeAll();
	            
	            
	    		String key = null;
	    		String value = null;
	    		
	    		while(itr.hasNext()){
	    			key = itr.next();
	    			if(StringUtils.isNotBlank(key)){
	    				TableItem ti = new TableItem(table, SWT.BORDER);
	    				ti.setText(0, key);
	    				value = map.get(key);
	    				if(StringUtils.isNotBlank(value)){
	    					ti.setText(1, value);
	    				}
	    			}

	    		}
	            
	            table.setSortColumn(column);
	        }
	    };
	    
	    return sortListener;
	}
	
	/**
	 * Calls pack() on all TableColumn widgest resizing to the preferred size.
	 * @param t Table in which to get the columns to call pack()
	 */
	public static void packAllColumns(Table t){
		if(t == null) { return; }
		
		TableColumn[] tcs = t.getColumns();
		for(TableColumn tc : tcs){
			if(tc != null){
				tc.pack();
			}
		}
	}
	
}
