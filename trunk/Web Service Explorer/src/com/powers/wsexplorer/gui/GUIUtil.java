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
import java.util.Properties;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

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
	
}
