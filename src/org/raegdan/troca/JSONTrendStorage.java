package org.raegdan.troca;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class JSONTrendStorage extends TrendStorage {

	private ObjectNode rootNode;
	private File f;
	private boolean canKillBadData;

	// ////////////////////////////////////////////////

	public JSONTrendStorage(String filename, boolean canKillBadData)
			throws Exception {
		this.canKillBadData = canKillBadData;

		f = new File(filename);
		if (!f.exists()) {
			makeFile();
		} else {
			checkFile();
		}

		rootNode = mapJSON();
	}

	@Override
	public void storeRates(HashMap<String, Double> rates, String dataSource,
			int utcTimestamp) throws Exception {
		JsonNodeFactory jsnf = new JsonNodeFactory(true);
		ObjectNode dsNode = checkJsonStructure(rootNode, jsnf, dataSource);

		for (Entry<String, Double> rate : rates.entrySet()) {
			ObjectNode currencyPairNode = checkJsonStructure(dsNode, jsnf,
					rate.getKey(), dataSource + " --> " + rate.getKey());

			if (currencyPairNode.get(utcTimestamp) != null) {
				throw new Exception(
						"Check your computer's clock -- we've got timestamp "
								+ Integer.toString(utcTimestamp)
								+ " collision at " + dataSource + " --> "
								+ rate.getKey());
			}

			currencyPairNode.put(Integer.toString(utcTimestamp),
					rate.getValue());
		}

		saveFile();
	}

	@Override
	public String getTrendStorage() {
		return "JSON storage";
	}

	// ////////////////////////////////////////////////

	private ObjectNode checkJsonStructure(ObjectNode parentNode,
			JsonNodeFactory jsnf, String key) throws Exception {
		return checkJsonStructure(parentNode, jsnf, key, key);
	}

	private ObjectNode checkJsonStructure(ObjectNode parentNode,
			JsonNodeFactory jsnf, String key, String keyForErrMsg)
			throws Exception {
		JsonNode buf = parentNode.get(key);

		if (buf == null) {
			parentNode.put(key, new ObjectNode(jsnf));
			buf = parentNode.get(key);
		} else if (!buf.isObject()) {
			if (canKillBadData) {
				parentNode.remove(key);
				parentNode.put(key, new ObjectNode(jsnf));
				buf = parentNode.get(key);
			} else {
				throw new Exception(
						"Existing JSON structure for "
								+ keyForErrMsg
								+ " is invalid; you gave no permission to kill corrupt data.");
			}
		}

		return (ObjectNode) buf;
	}

	private void makeFile() throws Exception {
		try {
			f.createNewFile();
			flushFile();
		} catch (Exception e) {
			throw new Exception("Error while trying to create storage file "
					+ f.getAbsolutePath()
					+ " -- check your rights and path validity!");
		}
	}

	private void flushFile() throws Exception {
		FileWriter fw = new FileWriter(f);
		fw.flush();
		fw.write("{}"); // Init with minimal JSON Object
		fw.close();
	}

	private void checkFile() throws Exception {
		if (!f.isFile())
			throw new Exception(
					f.getAbsolutePath()
							+ " is not a file! Check whether it's a dir / socket / etc.!");
		if (!f.canWrite())
			throw new Exception(f.getAbsolutePath()
					+ " is not writable! Check your rights!");
	}

	private ObjectNode mapJSON() throws Exception {
		ObjectNode jsn;

		try {
			jsn = new ObjectMapper().readValue(f, ObjectNode.class);
		} catch (IOException e) {
			throw new Exception("Got input/output error trying to parse "
					+ f.getAbsolutePath());
		} catch (Exception e) {
			// assuming we got invalid JSON in file
			if (canKillBadData) {
				flushFile();
				jsn = new ObjectNode(new JsonNodeFactory(true));
			} else {
				throw new Exception(
						f.getAbsolutePath()
								+ " seems to contain invalid JSON and you gave no permission to flush invalid storage.");
			}
		}

		return jsn;
	}

	private void saveFile() throws Exception {
		try {
			FileWriter fw = new FileWriter(f);
			fw.flush();
			fw.write(new ObjectMapper().writerWithDefaultPrettyPrinter()
					.writeValueAsString(rootNode) + "\r\n");
			fw.close();
		} catch (IOException e) {
			throw new Exception("Input/output error trying to save "
					+ f.getAbsolutePath() + ".");
		}
	}
}
