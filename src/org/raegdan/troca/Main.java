package org.raegdan.troca;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class Main {

	public static final String VERSION = "0.0.2alpha";

	private static boolean verbose = false;
	private static boolean json = false;
	private static boolean fancyJson = false;
	private static boolean dbIsSet = false;
	private static boolean dbTypeIsSet = false;
	private static boolean dbTypeJSONForce = false;
	private static boolean dbTypeJSONForceIsSet = false;
	private static boolean prependTimestamp = false;
	private static boolean quiet = false;
	private static String db = "";
	private static String dbType = "";
	private static int daemon = 0;
	private static int langolier = 0;

	private static String ts() {
		return String.format("[%1$td.%1$tm.%1$tY %1$tH:%1$tM:%1$tS.%1$tL] ",
				Calendar.getInstance());
	}

	private static void out(String s, boolean noTS) {
		if (quiet)
			return;
		String ts = (prependTimestamp && !noTS) ? ts() : "";
		System.out.println(ts + s);
	}

	private static void err(String s, boolean noTS) {
		String ts = (prependTimestamp && !noTS) ? ts() : "";
		System.err.println(ts + s);
	}

	private static void out(String s) {
		out(s, false);
	}

	private static void err(String s) {
		err(s, false);
	}

	private static void out() {
		out("", true);
	}

	private static void err() {
		err("", true);
	}

	private static boolean sleeper(int ms) {
		if (ms == 0) {
			return false;
		} else {
			if (verbose) {
				out("Sleeping " + Integer.toString(ms) + " ms.");
				out();
			}
			try {
				Thread.sleep(ms);
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}

			return true;
		}
	}

	private static void printException(Exception e) {
		err();
		err();
		out("Error occurred while querying!");
		err("Error message: " + e.getMessage());
		err("Error class: " + e.getClass().toString());
		System.exit(10);
	}

	private static void printRate(String pair, String value, int maxlen) {
		out(String.format("%1$-" + maxlen + "s%2$s", pair, value));
	}

	private static void printRates(HashMap<String, Double> rates) {
		int maxlen = 0;
		for (Entry<String, Double> rate : rates.entrySet())
			if (rate.getKey().length() > maxlen)
				maxlen = rate.getKey().length();
		maxlen++;

		for (Entry<String, Double> rate : rates.entrySet()) {
			if (rate.getValue() != 0.0)
				printRate(rate.getKey(), rate.getValue().toString(), maxlen);
			else
				printRate(rate.getKey(), "N/A", maxlen);
		}
	}

	private static void printRatesAsJSON(HashMap<String, Double> rates,
			boolean fancy) {
		JsonNodeFactory factory = new JsonNodeFactory(true);
		ObjectNode rootNode = new ObjectNode(factory);

		for (Entry<String, Double> rate : rates.entrySet())
			rootNode.put(rate.getKey(), rate.getValue());

		try {
			out((fancy) ? new ObjectMapper().writerWithDefaultPrettyPrinter()
					.writeValueAsString(rootNode) : rootNode.toString(), true);
		} catch (JsonProcessingException e) {
			printException(e);
		}
	}

	private static HashMap<String, Double> queryRate(
			HashSet<String> fromCurrencies, HashSet<String> toCurrencies,
			ExchangeAPI handler) {
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
		}

		QueryThread query = new QueryThread();
		query.setHandler(handler);
		query.setFromCurrencies(fromCurrencies);
		query.setToCurrencies(toCurrencies);
		query.start();

		if (verbose) {
			System.out.print((prependTimestamp ? ts() : "") + "Querying... ");

			int i = 0;
			String[] propeller = new String[] { "|", "/", "—", "\\" };

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

			out();
		} else {
			while (query.isAlive())
				;
		}

		return query.getRates();
	}

	private static void usage() {
		usage(null);
	}

	private static void greet(String dataSource, String trendStorage) {
		out("Data source: " + dataSource + ".");
		if (dbIsSet) {
			out("Database type: " + trendStorage + ".");
			out("Database link: " + db + ".");
		}

		if (daemon > 0) {
			out("Running daemonized with interval of "
					+ Integer.toString(daemon) + " ms.");
		}
		out();
	}

	private static void usage(String errorMsg) {
		quiet = false;

		// Protection from spam spiders parsing GitHub
		byte[] addr = { 0x72, 0x61, 0x65, 0x67, 0x64, 0x61, 0x6e, 0x40, 0x67,
				0x6d, 0x61, 0x69, 0x6c, 0x2e, 0x63, 0x6f, 0x6d };

		out("troca - cross-platform currency rates querier");
		out();
		if (errorMsg != null) {
			out("Invalid arguments: " + errorMsg);
			out();
		}

		out("Usage:");
		out("  -f | --from currency1[,currency2,currency3,...] : currency to convert from");
		out("  -t | --to currency1[,currency2,currency3,...]   : currency to convert to");
		out("  [ -v | --verbose ]                              : verbose output, incompatible");
		out("                                                    with --quiet.");
		out("  [ -s | --source data_source ]                   : choose data source");
		out("  [ -j | --json [ --fancy ] ]                     : JSON output [ formatted ],");
		out("                                                    incompatible with --quiet");
		out("  [ --db-type database_type ]                     : Type of database to add");
		out("                                                    results into. Requires --db");
		out("  [ --db database_link ]                          : Link to database. Depends");
		out("                                                    on database type.");
		out("                                                    Requires --db-type.");
		out("  [ --daemon N ]                                  : Repeat query every N ms,");
		out("                                                    N = 5000+ (respect the data");
		out("                                                    sources servers, please!");
		out("  [ --timestamp ]                                 : Prepend timestamps to output.");
		out("                                                    Incompatible with --quiet.");
		out("  [ -q | --quiet ]                                : Print nothing, requires --db.");
		out("  [ -l | --langolier N ]                          : Spawn a Langolier that eats");
		out("                                                    database records older than N sec.");
		out();
		out("Data sources:");
		out("  y | yahoo    : Yahoo Finance (default) -- supports most of world currencies");
		out("  c | coinbase : Coinbase.com -- accurate rates of Bitcoin to fiat currencies");
		out();
		out("Database types:");
		out("  j | json     : JSON file");
		out("                 Requires --db to be a file path.");
		out("                 Specific parameters:");
		out("                   --db-type-json-force : If appending to JSON database fails");
		out("                                          due to JSON code corruption, allow");
		out("                                          troca to delete minimal-sufficient");
		out("                                          parts of corrupted JSON code to");
		out("                                          restore its validity. It can delete the");
		out("                                          whole file though, if its condition");
		out("                                          is unrestorable poor, so be careful.");
		out();
		out("About:");
		out("  Version " + VERSION + ".");
		out("  Written by Raegdan [ " + new String(addr) + " ].");
		out("  License: GNU GPL v3.");
		out("  \"troca\" is the portuguese for \"exchange\".");
		out();

		System.exit(errorMsg != null ? 1 : 0);
	}

	private static HashSet<String> parseCurrencies(String currencyArgument) {
		HashSet<String> result = new HashSet<>();
		if (!currencyArgument.contains(","))
			currencyArgument += ",";
		String[] currencies = currencyArgument.split("\\,");
		for (String currency : currencies)
			result.add(currency);
		return result;
	}

	public static void main(String[] args) {

		String fromCurrency = null, toCurrency = null;
		ExchangeAPI handler = null;
		TrendStorage storage = null;

		if (args.length == 0)
			usage();

		for (int i = 0; i < args.length; i++) {
			switch (args[i]) {
			case "--from":
			case "-f":
				i++;
				if (i >= args.length)
					usage("--from requires an argument");
				fromCurrency = args[i];
				break;

			case "--to":
			case "-t":
				i++;
				if (i >= args.length)
					usage("--to requires an argument");
				toCurrency = args[i];
				break;

			case "--help":
			case "-h":
				usage();
				break;

			case "--source":
			case "-s":
				i++;
				if (i >= args.length)
					usage("--source requires an argument");
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
					usage(args[i]
							+ " is not a valid argument for --source, check available sources");
				}
				break;

			case "--db-type":
				i++;
				if (i >= args.length)
					usage("--db-type requires an argument");
				dbType = args[i];
				dbTypeIsSet = true;
				break;

			case "--db":
				i++;
				if (i >= args.length)
					usage("--db requires an argument");
				db = args[i];
				dbIsSet = true;
				break;

			case "--db-type-json-force":
				dbTypeJSONForce = true;
				dbTypeJSONForceIsSet = true;
				break;

			case "--verbose":
			case "-v":
				verbose = true;
				break;

			case "--json":
			case "-j":
				json = true;
				break;

			case "--fancy":
				fancyJson = true;
				break;

			case "--daemon":
				i++;
				if (i >= args.length)
					usage("--daemon requires an argument");

				try {
					daemon = Integer.parseInt(args[i]);
				} catch (NumberFormatException e) {
					usage(args[i]
							+ " is not a valid argument for --daemon, integer number needed");
				}

				if (daemon < 5000) {
					usage(args[i]
							+ " is not a valid argument for --daemon, range is 5000 ... +INF");
				}

				break;
				
			case "-l":
			case "--langolier":
				i++;
				if (i >= args.length)
					usage("--langolier requires an argument");

				try {
					langolier = Integer.parseInt(args[i]);
					if (langolier < 1) throw new NumberFormatException();
				} catch (NumberFormatException e) {
					usage(args[i]
							+ " is not a valid argument for --langolier, integer number above zero needed");
				}			
				
				break;

			case "--quiet":
			case "-q":
				quiet = true;
				break;

			case "--timestamp":
				prependTimestamp = true;
				break;

			default:
				usage("unknown argument: " + args[i]);
			}

		}

		if (dbIsSet != dbTypeIsSet) {
			usage("--db and --db-type may be used only together.");
		}

		if (quiet && (prependTimestamp || verbose || json)) {
			usage("--timestamp, --json and --verbose make no sense together with --quiet.");
		}

		if (quiet && !dbIsSet) {
			usage("--quiet makes no sense without database output (--db-type and --db).");
		}
		
		if (langolier > 0 && !dbIsSet) {
			usage("--langolier is set without --db and --db-type: nowhere to expunge old entries from!");
		}

		if (dbIsSet) {
			switch (dbType) {
			case "json":
			case "j":
				try {
					storage = new JSONTrendStorage(db, dbTypeJSONForce);
				} catch (Exception e) {
					printException(e);
				}
				break;

			default:
				if (dbTypeJSONForceIsSet) {
					usage("--db-type-json-force is applicable to --db-type json only.");
				}

				usage("unknown database type: " + dbType);
			}
		}

		if (handler == null)
			handler = new YahooFinanceExchangeAPI();

		if (fancyJson && !json) {
			usage("--fancy is not allowed without --json");
		}

		if (fromCurrency == null || toCurrency == null) {
			usage("missing some mandatory argument(s)");
		}

		if (verbose)
			greet(handler.getDataSource(),
					(dbIsSet) ? storage.getTrendStorage() : null);

		do {
			HashMap<String, Double> rates = queryRate(
					parseCurrencies(fromCurrency), parseCurrencies(toCurrency),
					handler);

			if (json)
				printRatesAsJSON(rates, fancyJson);
			else
				printRates(rates);

			if (daemon > 0)
				out();

			if (dbIsSet) {
				try {
					storage.storeRates(rates, handler.getDataSource(),
							(int) (new Date().getTime() / 1000), (langolier > 0), langolier);
				} catch (Exception e) {
					printException(e);
				}
			}

		} while (sleeper(daemon));

		System.exit(0);
	}
}
