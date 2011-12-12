/*
 *   Copyright 2011 Nick Powers.
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

import java.util.LinkedList;
import java.util.List;

import org.eclipse.swt.custom.ExtendedModifyEvent;
import org.eclipse.swt.custom.ExtendedModifyListener;
import org.eclipse.swt.custom.StyledText;

/**
 * Code taken from: http://www.java2s.com/Code/Java/SWT-JFace-Eclipse/SWTUndoRedo.htm
 * 
 * This class enables a StyledText to undo and redo. This class is expted to be used in the following way:<br>
 * <ul>
 * 	<li>Create an instance of this class giving it a non-null StyledText</li>
 *  <li>Call <code>styledText.addExtendedModifyListener()</code> with an input of <code>yourUndoRedoListner.getListner()</code>.
 * </ul> 
 */
public class UndoRedoListener {

	private static final int MAX_STACK_SIZE = 30;
	
	private final List<String> undoStack;
	private final List<String> redoStack;
	private final StyledText styledText;
	private ExtendedModifyListener listener = null;
	
	/**
	 * Main constructor for this class.
	 * @param styledText  a non-null StyledText widget
	 */
	public UndoRedoListener(StyledText styledText){
		if(styledText == null) { throw new RuntimeException("StyledText input is null/invalid"); }
	    undoStack = new LinkedList<String>();
	    redoStack = new LinkedList<String>();
	    this.styledText = styledText;
	}
	
	/**
	 * Gets an <code>ExtendedModifyListener</code> that keeps track of text entered and removed from
	 * the StyledText object. 
	 * @return
	 */
	public ExtendedModifyListener getListener(){
		if(listener == null){
			listener =  new UndoRedoExtendedModifyListener();
		}
		
		return listener;
	}
	
	/**
	 * Performs an undo for the last text entered in the <code>StyledText</code>.
	 */
	public void undo() {
		if (undoStack.size() > 0) {
			String lastEdit = (String) undoStack.remove(0);
			int editLength = lastEdit.length();
			String currText = styledText.getText();
			int startReplaceIndex = currText.length() - editLength;
			if(startReplaceIndex >= 0){
				styledText.replaceTextRange(startReplaceIndex, editLength, "");
				redoStack.add(0, lastEdit);
			}
		}
	}

	/**
	 * Performs an redo for the last text entered in the <code>StyledText</code>.
	 */
	public void redo() {
		if (redoStack.size() > 0) {
			String text = (String) redoStack.remove(0);
			moveCursorToEnd();
			styledText.append(text);
			moveCursorToEnd();
		}
	}
	
	private void moveCursorToEnd() {
		 styledText.setCaretOffset(styledText.getText().length());
	 }
	 
	/**
	 * A private, inner class to get an implemention of <code>ExtendedModifyListener</code>
	 * which contains the logic to keep track of edits for {@link UndoRedoListener#undo()} and {@link UndoRedoListener#redo()}.
	 *
	 */
	 private class UndoRedoExtendedModifyListener implements ExtendedModifyListener {

		@Override
		public void modifyText(ExtendedModifyEvent event) {
			String currText = styledText.getText();
	        String newText = currText.substring(event.start, event.start + event.length);
	        if (newText != null && newText.length() > 0) {
	          if (undoStack.size() == MAX_STACK_SIZE) {
	            undoStack.remove(undoStack.size() - 1);
	          }
	          undoStack.add(0, newText);
	        }
		}
		 
	 }
}
