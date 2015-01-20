package org.raegdan.troca;

import java.util.HashMap;

public class TrendStorage {

	public void storeRates(HashMap<String, Double> rates, String dataSource,
			int utcTimestamp) throws Exception {
		storeRates(rates, dataSource, utcTimestamp, false, 0);
	}	
	
	public void storeRates(HashMap<String, Double> rates, String dataSource,
			int utcTimestamp, boolean expungeOldData, int expungeThreshold) throws Exception {
		// Override this to implement a trend storage
	}

	public String getTrendStorage() {
		return "No storage -- this is a stub class";
	}
}
