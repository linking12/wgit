package com.pajk.wgit.core.service;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;

import com.pajk.wgit.core.op.CloneOperation;
import com.pajk.wgit.core.op.FetchOperation;

public class GitService {

	private String gitPraentPath = "/Users/apple/Desktop/git/";

	// https://github.com/eclipse/egit.git
	private String getGirdir(String remoteUrl) {
		String[] strs = remoteUrl.split("/");
		String projectNames = strs[strs.length - 1];
		String projectPath = gitPraentPath
				+ projectNames.substring(0, projectNames.indexOf(".")) + "/";
		return projectPath;
	}

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

	public void fetchBranch(String remoteUrl, String brachName)
			throws URISyntaxException, IOException {
		String projectPaht = getGirdir(remoteUrl) + Constants.DOT_GIT;
		Repository repository = new FileRepository(projectPaht);
		StoredConfig config = repository.getConfig();
		RemoteConfig remoteConfig = new RemoteConfig(config, "origin");
		remoteConfig.addURI(new URIish(remoteUrl));
		remoteConfig.addFetchRefSpec(new RefSpec(
				"+refs/heads/*:refs/remotes/origin/*"));
		remoteConfig.update(config);
		config.setString(ConfigConstants.CONFIG_BRANCH_SECTION, brachName,
				ConfigConstants.CONFIG_KEY_REMOTE, "origin");
		config.setString(ConfigConstants.CONFIG_BRANCH_SECTION, brachName,
				ConfigConstants.CONFIG_KEY_MERGE, "refs/heads/" + brachName);
		config.save();
		FetchOperation fetchOperation = new FetchOperation(repository,
				remoteConfig, 60, false);
		fetchOperation.execute();
	}
}
