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

import java.util.Stack;

import org.eclipse.swt.custom.ExtendedModifyEvent;
import org.eclipse.swt.custom.ExtendedModifyListener;
import org.eclipse.swt.custom.StyledText;

/**
 * 
 * This class enables a StyledText to undo and redo. This class is expected to be used in the following way:<br>
 * <ul>
 * 	<li>Create an instance of this class giving it a non-null StyledText</li>
 *  <li>Call <code>styledText.addExtendedModifyListener()</code> with an input of <code>yourUndoRedoListner.getListner()</code>.
 * </ul> 
 */
public class UndoRedoListener {

	private static final int UNDO_LIMIT = 500;
	
	private Stack<TextChange> undoStack = new Stack<TextChange>(); // The stack used to store the undo information
	private Stack<TextChange> redoStack = new Stack<TextChange>(); // The stack used to store the redo information
	private transient boolean ignoreUndo = false;
	private transient boolean ignoreRedo = false;
	
	private final StyledText styledText;
	private final WSExplorer wse;
	private ExtendedModifyListener listener = null;
	
	/**
	 * Main constructor for this class.
	 * @param styledText  a non-null StyledText widget
	 */
	public UndoRedoListener(StyledText styledText, WSExplorer wse){
		if(styledText == null) { throw new IllegalArgumentException("StyledText input is null/invalid"); }
	    this.styledText = styledText;
	    this.wse = wse;
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
	 * Undo the last edits until the stack is empty or <code>UNDO_LIMIT</code> is hit.
	 */
	public void undo() {
		// Make sure undo stack isn't empty
		if (!undoStack.empty()) {
			// Get the last change

			TextChange change = undoStack.pop();

			String redoText = styledText.getTextRange(change.getStart(), change.getLength());
			TextChange tc = new TextChange(change.getStart(), (redoText.length() > 0 ? 0 : change.getReplacedText().length()),redoText);
			// Set the flag. Otherwise, the replaceTextRange call will get
			// placed
			// on the undo stack
			ignoreUndo = true;
			// Replace the changed text
			styledText.replaceTextRange(change.getStart(), change.getLength(), change.getReplacedText());

			// Move the caret
			styledText.setCaretOffset(change.getStart());

			// Scroll the screen
			//styledText.setTopIndex(styledText.getLineAtOffset(change.getStart()));

			// add to the redo stack
			if (!ignoreRedo) {
				// Push this change onto the changes stack
				redoStack.push(tc);
				if (redoStack.size() > UNDO_LIMIT)
					redoStack.remove(0);
			}

			ignoreUndo = false;
		}
	}

	/**
	 * Redoes the last edits
	 */
	public void redo() {
		TextChange change = null;
		try {
			if (!redoStack.empty()) {
				change = redoStack.pop();

				if (change.getStart() + change.getLength() > styledText.getText().length()) {
					return;
				}
				ignoreRedo = true;
				styledText.replaceTextRange(change.getStart(), change.getLength(),
						change.getReplacedText());
				// Move the caret
				styledText.setCaretOffset(change.getStart()
						+ change.getReplacedText().length());
				// Scroll the screen
				styledText.setTopIndex(styledText.getLineAtOffset(change.getStart()));

				ignoreRedo = false;
			}
		} catch (Exception e) {
			wse.log("Caught an exception while trying to redo..." + change+ " text size: " + styledText.getText().length(), e);
		}
	}
	
	// TextChange Inner Class
	public static class TextChange {
		// The starting offset of the change
		private int start;
		// The length of the change
		private int length;
		// The replaced text
		String replacedText;

		/**
		 * Constructs a TextChange
		 * 
		 * @param start
		 *            the starting offset of the change
		 * @param length
		 *            the length of the change
		 * @param replacedText
		 *            the text that was replaced
		 */
		public TextChange(int start, int length, String replacedText) {
			this.start = start;
			this.length = length;
			this.replacedText = replacedText;
		}

		public int getStart() {
			return start;
		}

		public int getLength() {
			return length;
		}

		public String getReplacedText() {
			return replacedText;
		}

		@Override
		public String toString() {
			return "start=" + start + "length=" + length + "text="+ replacedText;
		}
	}
	
	/**
	 * A private, inner class to get an implemention of <code>ExtendedModifyListener</code>
	 * which contains the logic to keep track of edits for {@link UndoRedoListener#undo()} and {@link UndoRedoListener#redo()}.
	 *
	 */
	private class UndoRedoExtendedModifyListener implements ExtendedModifyListener {

		@Override
		public void modifyText(ExtendedModifyEvent event) {
			if (!ignoreUndo) {
				// Push this change onto the changes stack
				TextChange tc = new TextChange(event.start, event.length,event.replacedText);
				undoStack.push(tc);

				if (undoStack.size() > UNDO_LIMIT)
					undoStack.remove(0);
			}
		}
		
	}
}
