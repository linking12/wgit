package com.pajk.wgit.core.service;

public class Test {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		GitService git = new GitService();
		git.cloneBranch("https://github.com/linking12/wgit.git");

	}

}
