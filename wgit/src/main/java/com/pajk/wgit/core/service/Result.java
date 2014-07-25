package com.pajk.wgit.core.service;

public class Result {

	private String resultCode;
	private String message;

	public Result() {
		resultCode = "000";
	}

	public String getResultCode() {
		return resultCode;
	}

	public void setResultCode(String resultCode) {
		this.resultCode = resultCode;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String toJson() {
		StringBuilder json = new StringBuilder();
		if (resultCode.equals("000")) {
			json.append("{\"code\":000");
			json.append(",\"content\":\"").append(message).append("\"");
			json.append("}");
		} else {
			json.append("{\"code\":").append(resultCode);
			json.append(",\"error\":\"").append(message).append("\"");
			json.append("}");
		}
		return json.toString();
	}

}
