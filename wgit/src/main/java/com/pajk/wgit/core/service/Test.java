package com.pajk.wgit.core.service;

import java.net.URISyntaxException;

public class Test {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws URISyntaxException {
		GitService git = new GitService();
		// git.cloneBranch("git@10.0.128.104:root/totoro.git", "develop");
		// git.createBranch("git@10.0.128.104:root/totoro.git", "test1",
		// "develop");
		// git.fetchBranch("git@10.0.128.104:root/totoro.git", "test");
		git.swtichBranch("git@10.0.128.104:root/totoro.git", "test");
	}
}
