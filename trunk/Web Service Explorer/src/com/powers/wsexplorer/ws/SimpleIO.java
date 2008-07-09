/*
All Purpose MD5 is a simple, fast, and easy-to-use GUI for calculating and testing MD5s.
Copyright (C) 2006  Nick Powers

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
or go to http://www.gnu.org/licenses/gpl.html.
 */

package com.powers.wsexplorer.ws;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

public class SimpleIO {

	/**
	 * 
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
	 * 
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
	 * 
	 * @param pw
	 */
	public static void close(PrintWriter pw) {
		if (pw != null) {
			pw.close();
		}
	}

	public static void close(BufferedReader br) {
		if (br != null) {
			try {
				br.close();
			} catch (IOException e) {
			}
		}
	}

	public static boolean exists(String filename) {
		File file = new File(filename);
		return (file.exists());
	}

}
