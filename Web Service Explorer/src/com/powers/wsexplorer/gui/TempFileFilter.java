package com.powers.wsexplorer.gui;

import java.io.File;
import java.io.FilenameFilter;

public class TempFileFilter implements FilenameFilter {

	@Override
	public boolean accept(File dir, String name) {
		return name.contains("tmp");
	}


}
