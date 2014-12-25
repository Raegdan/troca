package org.raegdan.troca;

import java.util.HashMap;
import java.util.HashSet;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class YahooFinanceExchangeAPI extends ExchangeAPI {
	
	@Override
	public HashMap<String, Double> queryRates(HashSet<String> fromCurrencies,
			HashSet<String> toCurrencies) throws Exception {
		String response;
		
		checkFormat(fromCurrencies);
		checkFormat(toCurrencies);
		
		try {
			response = queryYahooFinance(fromCurrencies, toCurrencies);
		} catch (Exception e) {
			throw new Exception("Failed to query Yahoo Finance -- check your network.");
		}
		
		HashMap<String, Double> rates = parseJSONYahooFinanceResponse(response);
				
		return rates;
	}
	
	@Override
	public String getDataSource() {
		return "Yahoo Finance API";
	}
	
	private void checkFormat (HashSet<String> currencies) throws Exception {
		for (String currency : currencies) {
			if (currency.length() != 3)
				throw new Exception("Wrong code: \"" + currency + "\". Yahoo Finance uses three-letter currency codes.");
		}
	}

	private String queryYahooFinance(HashSet<String> fromCurrency, HashSet<String> toCurrency)
			throws Exception {	
		
		String buffer = "";
		for (String fc : fromCurrency) {
			for (String tc : toCurrency) {
				if (fc.equalsIgnoreCase(tc)) continue;
				buffer += fc.toUpperCase() + tc.toUpperCase() + ",";
			}
		}
		buffer = buffer.subSequence(0, buffer.length() - 1).toString();
	
		String validHostname = "query.yahooapis.com";
		String requestURL = "https://query.yahooapis.com/v1/public/yql?q=select%20*%20from%20yahoo.finance.xchange%20where%20pair%20in%20(%22"
									+ buffer
									+ "%22)&format=json&env=store%3A%2F%2Fdatatables.org%2Falltableswithkeys";

		return doHTTPSRequest(requestURL, validHostname);
	}
	
	private void putRateItem (JsonNode rateItem, HashMap<String, Double> buffer) throws Exception {
		buffer.put(
			rateItem.findValue("id").asText().subSequence(0, 3).toString()
				+ "/"
				+ rateItem.findValue("id").asText().subSequence(3, 6).toString(), 
			Double.valueOf(rateItem.findValue("Rate").asText())
		);
	}
	
	private HashMap<String, Double> parseJSONYahooFinanceResponse(String response)
			throws Exception {
		
		HashMap<String, Double> buffer = new HashMap<>();
		
		JsonNode ratesRoot = new ObjectMapper().readValue(response, JsonNode.class)
				.findValue("query")
				.findValue("results")
				.findValue("rate");
		
		if (ratesRoot.isObject()) {
			putRateItem(ratesRoot, buffer);
		} else if (ratesRoot.isArray()) {
			for (JsonNode rateItem : ratesRoot) putRateItem(rateItem, buffer);
		}
		
		return buffer;
	}
}
