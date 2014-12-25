package org.raegdan.troca;

import java.util.HashMap;
import java.util.HashSet;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class CoinbaseComExchangeAPI extends ExchangeAPI {
	@Override
	public HashMap<String, Double> queryRates(HashSet<String> fromCurrencies, HashSet<String> toCurrencies) 
			throws Exception {
		String response;
		try {
			response = queryCoinbaseCom();
		} catch (Exception e) {
			throw new Exception("Failed to query coinbase.com -- check your network.");
		}
		
		HashMap<String, Double> rates;
		try {
			rates = findCurrencyPairs(response, fromCurrencies, toCurrencies);
		} catch (NumberFormatException e) {
			throw new Exception("Coinbase.com returned the rate of invalid format. This should not normally occur -- contact troca team.");
		} catch (Exception e) {
			throw new Exception("Failed to parse coinbase.com response. May be its API is broken or has been unexpectedly changed. This should not normally occur -- contact troca team.");
		}
		
		return rates;
	}
	
	@Override
	public String getDataSource() {
		return "Coinbase.com API";
	}
	
	private String queryCoinbaseCom() throws Exception {
		String validHostname = "api.coinbase.com";
		String requestURL = "https://api.coinbase.com/v1/currencies/exchange_rates";
		
		return doHTTPSRequest(requestURL, validHostname);
	}
	
	private void putRateItem (JsonNode rateItem, String fromCurrency, String toCurrency, HashMap<String, Double> buffer) throws Exception{
		buffer.put(
			fromCurrency.toUpperCase() + "/" + toCurrency.toUpperCase(),
			rateItem == null ? 0.0 : Double.parseDouble(rateItem.asText())
		);
	}
	
	private HashMap<String, Double> findCurrencyPairs (String coinbaseComResponse, HashSet<String> fromCurrencies, HashSet<String> toCurrencies)
			throws Exception {
		JsonNode jsonResponse = new ObjectMapper().readValue(coinbaseComResponse, JsonNode.class);
		HashMap<String, Double> result = new HashMap<>();
		
		for (String fc : fromCurrencies) {
			for (String tc : toCurrencies) {
				putRateItem(
					jsonResponse.findValue(fc.toLowerCase() + "_to_" + tc.toLowerCase()),
					fc,
					tc, 
					result
				);
			}
		}

		return result;
	}
}
