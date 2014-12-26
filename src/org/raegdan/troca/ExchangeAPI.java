package org.raegdan.troca;

import java.io.IOException;
import java.net.URL;
import java.util.Currency;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Set;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

public class ExchangeAPI {

	private static final String currencyCodes[];

	static {
		Set<Currency> lc = Currency.getAvailableCurrencies();
		currencyCodes = new String[lc.size()];
		int i = 0;
		Iterator<Currency> lci = lc.iterator();
		while (lci.hasNext()) {
			currencyCodes[i] = lci.next().getCurrencyCode();
			i++;
		}
	}

	public HashMap<String, Double> queryRates(HashSet<String> fromCurrencies,
			HashSet<String> toCurrencies) throws Exception {
		// Override this to implement a real exchange client
		return null;
	}

	public String getDataSource() {
		return "No source -- this is a stub class";
	}

	public static String[] getCurrencyCodes() {
		return currencyCodes;
	}

	public static String doHTTPSRequest(String requestURL, String validHostname)
			throws IOException {
		class ExchangeAPIHostnameVerifier implements HostnameVerifier {

			private String validHostname;

			public void setValidHostname(String validHostname) {
				this.validHostname = validHostname;
			}

			@Override
			public boolean verify(String arg0, SSLSession arg1) {
				return (arg0.equalsIgnoreCase(validHostname));
			}
		}

		URL u = new URL(requestURL);
		HttpsURLConnection conn = (HttpsURLConnection) u.openConnection();
		ExchangeAPIHostnameVerifier hv = new ExchangeAPIHostnameVerifier();
		hv.setValidHostname(validHostname);
		conn.setHostnameVerifier(hv);

		if (conn.getResponseCode() != HttpsURLConnection.HTTP_OK)
			throw new IOException();
		Scanner s = new Scanner(conn.getInputStream());
		String buffer = "";
		while (s.hasNext()) {
			buffer += s.next();
		}
		s.close();
		return buffer;
	}
}
