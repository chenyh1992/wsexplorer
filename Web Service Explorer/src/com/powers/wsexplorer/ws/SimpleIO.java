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

package com.powers.wsexplorer.ws;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

public class SimpleIO {

	/**
	 * Convenience method to open a file for output.
	 * @param filename
	 * @return
	 */
	public static PrintWriter openFileForOutput(String filename)
			throws Exception {
		if (filename == null) {
			return null;
		}

		PrintWriter pw = null;
		File file = new File(filename);
		if (file.exists()) {
			if (!file.canWrite()) {
				throw new Exception("Unable to write to file " + filename);
			}
		}

		try {
			pw = new PrintWriter(new FileOutputStream(file));
		} catch (FileNotFoundException e) {
			return null;
		}

		return pw;
	}

	/**
	 * Convenience method to open a file for input.
	 * @param filename
	 * @return
	 */
	public static BufferedReader openFileForInput(String filename) {
		if (filename == null) {
			return null;
		}

		BufferedReader br = null;

		try {
			br = new BufferedReader(new FileReader(filename));
		} catch (FileNotFoundException e) {
			return null;
		}

		return br;
	}

	/**
	 * Close a connection quietly.
	 * @param closeMe any stream to be closed
	 */
	public static void close(Closeable closeMe) {
		if (closeMe != null) {
			try {
				closeMe.close();
			} catch (IOException e) {
				// ignore
			}
		}
	}

	/**
	 * Convenience method to check if a files exists.
	 * @param filename
	 * @return
	 */
	public static boolean exists(String filename) {
		File file = new File(filename);
		return (file.exists());
	}

}
