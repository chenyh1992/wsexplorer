package com.powers.wsexplorer.gui;

import java.text.Collator;
import java.util.Comparator;
import java.util.Map;

public class MapValueComparator implements Comparator<String> {
	
	static Collator collator = Collator.getInstance();
	Map<String, String> map = null;
	boolean reverse = false;
	
	public MapValueComparator(Map<String, String> map, boolean reverse){
		this.map = map;
		this.reverse = reverse;
	}
	
	public int compare(String key1, String key2) {
	
		String test1 = null;
		String test2 = null;
		
		if(map != null){
			test1 = map.get(key1);
			test2 = map.get(key2);
			
		} else {
			test1 = key1;
			test2 = key2;
		}
		
		if(reverse){
			return collator.compare(test2, test1);
		} else {
			return collator.compare(test1, test2);
		}
	}

}
