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

import org.eclipse.swt.graphics.RGB;

public class Options {

	public boolean ignoreHostCertificates;
	public static final String IGNORE_HOST_CERTIFICATS_KEY = "ignore.host.certificates";
	public static final RGB DefaultRGB =   new RGB(0,  0,  0);
	public static final RGB ValueRGB =     new RGB(42, 0,  255);
	public static final RGB TagRGB =       new RGB(63, 127,127);
	public static final RGB AttributeRGB = new RGB(102,0,  102);
	public static final RGB ScriptRGB =    new RGB(136,0,  0);
	public static final RGB CommentRGB =   new RGB(102,102,0);
	public static final RGB ErrorRGB =     new RGB(255,0,  0);
	public static final RGB TagValueRGB =  DefaultRGB;
}
