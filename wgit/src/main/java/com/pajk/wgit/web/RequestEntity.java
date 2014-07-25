package com.pajk.wgit.web;

public class RequestEntity {

	private String command;

	private String remoteurl;

	private String branch;

	private String source;

	private String target;

	public String getCommand() {
		return command;
	}

	public void setCommand(String command) {
		this.command = command;
	}

	public String getRemoteurl() {
		return remoteurl;
	}

	public void setRemoteurl(String remoteurl) {
		this.remoteurl = remoteurl;
	}

	public String getBranch() {
		return branch;
	}

	public void setBranch(String branch) {
		this.branch = branch;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public String getTarget() {
		return target;
	}

	public void setTarget(String target) {
		this.target = target;
	}

}
