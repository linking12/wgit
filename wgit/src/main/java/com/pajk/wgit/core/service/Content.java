package com.pajk.wgit.core.service;

import java.util.HashMap;
import java.util.Map;

public class Content {
	private Map<String, String> contextParameter = new HashMap<String, String>();

	public String get(String key) {
		return contextParameter.get(key);
	}

	public boolean put(String key, String value) {
		return contextParameter.put(key, value) == value;
	}

	public void putAll(Map<String, String[]> map) {
		for (String key : map.keySet()) {
			String[] value = map.get(key);
			contextParameter.put(key, value[0]);
		}
	}

	public String getParams() {
		StringBuffer data = new StringBuffer();
		for (Map.Entry<String, String> entry : contextParameter.entrySet()) {
			data.append(String.format("&%s=%s", entry.getKey(),
					entry.getValue()));
		}
		return data.toString();
	}
}
