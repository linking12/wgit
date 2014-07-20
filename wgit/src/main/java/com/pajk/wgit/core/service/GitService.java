package com.pajk.wgit.core.service;

import java.io.File;
import java.net.URISyntaxException;

import org.eclipse.jgit.transport.URIish;

import com.pajk.wgit.core.op.CloneOperation;

public class GitService {

	private String gitPraentPath;

	public void cloneBranch(String remoteUrl) {
		URIish uri;
		try {
			uri = new URIish(remoteUrl);
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
		CloneOperation clop = new CloneOperation(uri, true, null, new File(
				gitPraentPath), "refs/heads/master", "origin", 0);
		clop.execute();
	}

}
