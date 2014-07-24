package com.pajk.wgit.core.service;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jgit.api.CheckoutResult;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.transport.FetchResult;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;

import com.pajk.wgit.core.op.BranchOperation;
import com.pajk.wgit.core.op.CloneOperation;
import com.pajk.wgit.core.op.CreateLocalBranchOperation;
import com.pajk.wgit.core.op.CreateLocalBranchOperation.UpstreamConfig;
import com.pajk.wgit.core.op.FetchOperation;
import com.pajk.wgit.core.op.IWGitOperation.PostExecuteTask;
import com.pajk.wgit.core.op.PushOperation;

public class GitService {

	private String gitPraentPath = "D:/share/";

	// https://github.com/eclipse/egit.git
	private String getGirdir(String remoteUrl) {
		String[] strs = remoteUrl.split("/");
		String projectNames = strs[strs.length - 1];
		String projectPath = gitPraentPath
				+ projectNames.substring(0, projectNames.indexOf(".")) + "/";
		return projectPath;
	}

	public void cloneBranch(String remoteUrl, String branchName) {
		URIish uri;
		try {
			uri = new URIish(remoteUrl);
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
		String projectPath = getGirdir(remoteUrl);
		CloneOperation clop = new CloneOperation(uri, true, null, new File(
				projectPath), "refs/heads/" + branchName, "origin", 0);
		clop.execute();
	}

	public FetchResult fetchBranch(String remoteUrl, String brachName) {
		String projectPaht = getGirdir(remoteUrl) + Constants.DOT_GIT;
		FetchOperation fetchOperation;
		try {
			Repository repository = new FileRepository(projectPaht);
			StoredConfig config = repository.getConfig();
			RemoteConfig remoteConfig;
			remoteConfig = new RemoteConfig(config, "origin");
			remoteConfig.addURI(new URIish(remoteUrl));
			remoteConfig.addFetchRefSpec(new RefSpec(
					"+refs/heads/*:refs/remotes/origin/*"));
			remoteConfig.update(config);
			config.setString(ConfigConstants.CONFIG_BRANCH_SECTION, brachName,
					ConfigConstants.CONFIG_KEY_REMOTE, "origin");
			config.setString(ConfigConstants.CONFIG_BRANCH_SECTION, brachName,
					ConfigConstants.CONFIG_KEY_MERGE, "refs/heads/" + brachName);
			config.save();
			fetchOperation = new FetchOperation(repository, remoteConfig, 60,
					false);
			fetchOperation.execute();
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
		return fetchOperation.getOperationResult();

	}

	public CheckoutResult swtichBranch(final String remoteUrl,
			final String branchName) {

		String projectPaht = getGirdir(remoteUrl) + Constants.DOT_GIT;
		Repository repository;
		try {
			repository = new FileRepository(projectPaht);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		BranchOperation checkout = new BranchOperation(repository, branchName);
		checkout.execute();
		return checkout.getResult();
	}

	public Map<String, Object> createBranch(final String remoteUrl,
			final String branchName, final String sourceBranch) {

		String projectPaht = getGirdir(remoteUrl) + Constants.DOT_GIT;
		final Map<String, Object> result = new HashMap<String, Object>();
		try {
			Repository repository = new FileRepository(projectPaht);
			Ref ref = repository.getRef(sourceBranch);
			if (ref == null)
				throw new IllegalArgumentException(sourceBranch
						+ " has not checkout");
			CreateLocalBranchOperation createBranch = new CreateLocalBranchOperation(
					repository, branchName, repository.getRef(sourceBranch),
					UpstreamConfig.NONE);
			createBranch.addPostExecuteTask(new PostExecuteTask() {
				public void postExecute(Repository repository)
						throws RuntimeException {
					BranchOperation checkout = new BranchOperation(repository,
							branchName);
					checkout.execute();
					result.put("checkOutResult", checkout.getResult());
				}
			});
			createBranch.addPostExecuteTask(new PostExecuteTask() {
				public void postExecute(Repository repository)
						throws RuntimeException {
					PushOperation pop = new PushOperation(repository, false, 0);
					pop.execute();
					result.put("pushResult", pop.getOperationResult());
				}
			});
			createBranch.execute();

		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return result;
	}
}
