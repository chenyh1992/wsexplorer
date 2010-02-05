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
package com.powers.wsexplorer.ws;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;

import com.powers.wsexplorer.gui.GUIUtil;

public class ColorFactory {

	private static Map<RGB,Color> rgbMap = new HashMap<RGB,Color>();
	
	private ColorFactory() {}
	
	public static Color getColor(RGB rgb){
		Color color = rgbMap.get(rgb);
		if(color == null){
			color = GUIUtil.getColor(rgb);
			rgbMap.put(rgb, color);
		}
		
		return color;
	}
}
