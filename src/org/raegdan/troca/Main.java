package org.raegdan.troca;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class Main {
	
	private static boolean verbose = false;
	private static boolean json = false;
	
	private static void printException(Exception e) {
		System.err.println();
		System.err.println();
		System.out.println("Error occurred while querying!");
//		System.out.println("Please check your network connectivity, currency codes validity and make sure the data source selected supports requested currency pair.");
		System.err.println("Error message: " + e.getMessage());
		System.err.println("Error class: " + e.getClass().toString());
		System.exit(10);
	}
	
	private static void printRate (String pair, String value, int maxlen) {
		System.out.println(String.format("%1$-" + maxlen + "s%2$s", pair, value));
	}
	
	private static void printRates (HashMap<String, Double> rates) {
		int maxlen = 0;
		for (Entry<String, Double> rate : rates.entrySet())
			if (rate.getKey().length() > maxlen) maxlen = rate.getKey().length();
		maxlen++;
		
		for (Entry<String, Double> rate : rates.entrySet()) {
			if (rate.getValue() != 0.0)
				printRate(rate.getKey(), rate.getValue().toString(), maxlen);
			else
				printRate(rate.getKey(), "N/A", maxlen);
		}
	}
	
	private static void printRatesAsJSON(HashMap<String, Double> rates) {
		JsonNodeFactory factory = new JsonNodeFactory(true);
		ObjectNode rootNode = new ObjectNode(factory);
		
		for (Entry<String, Double> rate : rates.entrySet())
			rootNode.put(rate.getKey(), rate.getValue());
		
		System.out.println(rootNode.toString());
	}
	
	private static void queryRate(HashSet<String> fromCurrencies, HashSet<String> toCurrencies, ExchangeAPI handler) {
		class QueryThread extends Thread {
			private ExchangeAPI handler;
			private HashSet<String> fromCurrencies;
			private HashSet<String> toCurrencies;
			private HashMap<String, Double> rates;
			
			public void setFromCurrencies(HashSet<String> fromCurrencies) {
				this.fromCurrencies = fromCurrencies;
			}

			public void setToCurrencies(HashSet<String> toCurrencies) {
				this.toCurrencies = toCurrencies;
			}
			
			public HashMap<String, Double> getRates() {
				return rates;
			}

			public void setHandler(ExchangeAPI handler) {
				this.handler = handler;
			}

			@Override
			public void run() {
				try {
					rates = handler.queryRates(fromCurrencies, toCurrencies);
				} catch (Exception e) {
					printException(e);
				}
			}
		};
		
		if (verbose) System.out.println("Data source: " + handler.getDataSource());
		
		QueryThread query = new QueryThread();
		query.setHandler(handler);
		query.setFromCurrencies(fromCurrencies);
		query.setToCurrencies(toCurrencies);
		query.start();
		
		if (verbose) {
			System.out.print("Querying... ");

			int i = 0;
			String[] propeller = new String[] {"|", "/", "—", "\\"};
			
			while (query.isAlive()) {
				System.out.print(propeller[i % 4]);
				
				try {
					TimeUnit.MILLISECONDS.sleep(250);
				} catch (InterruptedException e) {
					printException(e);
				}
				
				System.out.print("\b");
				i++;
			}
			
			System.out.println();
		} else {
			while (query.isAlive());
		}
		
		if (json)
			printRatesAsJSON(query.getRates());
		else
			printRates(query.getRates());
	}

	private static void usage() {
		usage(null);
	}
	
	private static void usage(String errorMsg) {
		System.out.println("troca - cross-platform currency rates querier");
		System.out.println();
		if (errorMsg != null) {
			System.out.println("Invalid arguments: " + errorMsg);
			System.out.println();
		}

		System.out.println("Usage:");
		System.out.println("  -f | --from currency1[,currency2,currency3,...] : currency to convert from");
		System.out.println("  -t | --to currency1[,currency2,currency3,...]   : currency to convert to");
		System.out.println("  [ -v | --verbose ]                              : verbose output");
		System.out.println("  [ -s | --source data_source ]                   : choose data source");
		System.out.println("  [ -j | --json ]                                 : JSON output (not human-readable, but simply parseable)");
		System.out.println();
		System.out.println("Data sources:");
		System.out.println("  y | yahoo    : Yahoo Finance (default) -- supports most of world currencies");
		System.out.println("  c | coinbase : Coinbase.com -- accurate rates of Bitcoin to fiat currencies");
		System.out.println();
//		System.out.println("ISO currency codes:");
//		
//		int i = 1;
//		System.out.print("  ");
//		for (String code : ExchangeAPI.getCurrencyCodes()) {
//			System.out.print(code + " ");
//			if (i % 12 == 0) {
//				System.out.println();
//				System.out.print("  ");
//			}
//			i++;
//		}
//		System.out.println();
//		System.out.println();
		System.out.println("About:");
		
		// Protection from spam spiders parsing GitHub
		byte[] addr = {0x72, 0x61, 0x65, 0x67, 0x64, 0x61, 0x6e, 0x40, 0x67, 0x6d, 0x61, 0x69, 0x6c, 0x2e, 0x63, 0x6f, 0x6d};
		System.out.println("  Written by Raegdan [" + new String(addr) + "]");
		
		System.out.println("  License: GNU GPL v3");
		System.out.println("  \"troca\" is the portuguese for \"exchange\".");
		System.out.println();
		System.exit(errorMsg != null ? 1 : 0);
	}
	
	private static HashSet<String> parseCurrencies(String currencyArgument) {
		HashSet<String> result = new HashSet<>();
		if (!currencyArgument.contains(",")) currencyArgument += ",";
		String[] currencies = currencyArgument.split("\\,");
		for (String currency : currencies) result.add(currency);
		return result;
	}
	
	public static void main(String[] args) {
		
		String fromCurrency = null, toCurrency = null;
		ExchangeAPI handler = null;
		
		if (args.length == 0) usage();
		
		for (int i = 0; i < args.length; i++) {
			switch (args[i]) {
			case "--from":
			case "-f":
				i++;
				if (i >= args.length) usage("--from requires an argument");
				fromCurrency = args[i];
				break;
				
			case "--to":
			case "-t":
				i++;
				if (i >= args.length) usage("--to requires an argument");
				toCurrency = args[i];
				break;
				
			case "--help":
			case "-h":
				usage();
				break;
			
			case "--source":
			case "-s":
				i++;
				if (i >= args.length) usage("--source requires an argument");
				switch (args[i]) {
				case "coinbase":
				case "c":
					handler = new CoinbaseComExchangeAPI();
					break;
				
				case "yahoo":
				case "y":
					handler = new YahooFinanceExchangeAPI();
					break;

				default:
					usage(args[i] + " is not a valid argument for --source, check available sources");
				}
				break;
				
			case "--verbose":
			case "-v":
				verbose = true;
				break;
				
			case "--json":
			case "-j":
				json = true;
				break;
			}
		}
		
		if (handler == null) handler = new YahooFinanceExchangeAPI();
		
		if (fromCurrency != null && toCurrency != null) {
			queryRate(
				parseCurrencies(fromCurrency), 
				parseCurrencies(toCurrency),
				handler
			);
			System.exit(0);
		} else {
			usage("missing some mandatory argument(s)");
		}
	}
}
