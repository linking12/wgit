package com.pajk.wgit.core.service;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jgit.api.CheckoutResult;
import org.eclipse.jgit.api.CheckoutResult.Status;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.merge.ResolveMerger.MergeFailureReason;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.FetchResult;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;

import com.pajk.wgit.core.RepositoryUtil;
import com.pajk.wgit.core.internal.Utils;
import com.pajk.wgit.core.op.BranchOperation;
import com.pajk.wgit.core.op.CloneOperation;
import com.pajk.wgit.core.op.CreateLocalBranchOperation;
import com.pajk.wgit.core.op.CreateLocalBranchOperation.UpstreamConfig;
import com.pajk.wgit.core.op.FetchOperation;
import com.pajk.wgit.core.op.IWGitOperation.PostExecuteTask;
import com.pajk.wgit.core.op.IWGitOperation.PreExecuteTask;
import com.pajk.wgit.core.op.MergeOperation;
import com.pajk.wgit.core.op.PushOperation;
import com.pajk.wgit.core.op.PushOperationResult;

public class GitService {

	private static final String gitPraentPath = Utils
			.getPropertiesByClasspath().getProperty("gitPraentPath");

	private static class Holder {
		private static GitService instance = new GitService();
	}

	public static synchronized GitService getInstance() {
		return Holder.instance;
	}

	// https://github.com/eclipse/egit.git
	private String getGirdir(String remoteUrl) {
		String[] strs = remoteUrl.split("/");
		String projectNames = strs[strs.length - 1];
		String projectPath = gitPraentPath
				+ projectNames.substring(0, projectNames.indexOf(".")) + "/";
		return projectPath;
	}

	private void cloneBranch(String remoteUrl, String branchName) {
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

	private FetchResult fetchBranch(String remoteUrl, String brachName) {
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

	private CheckoutResult swtichBranch(final String remoteUrl,
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

	private Map<String, Object> createBranch(final String remoteUrl,
			final String branchName, final String sourceBranch) {
		String projectPaht = getGirdir(remoteUrl) + Constants.DOT_GIT;
		final Map<String, Object> result = new HashMap<String, Object>();
		try {
			Repository repository = new FileRepository(projectPaht);
			CreateLocalBranchOperation createBranch = new CreateLocalBranchOperation(
					repository, branchName, repository.getRef(sourceBranch),
					UpstreamConfig.NONE);
			createBranch.addPreExecuteTask(new PreExecuteTask() {
				@Override
				public void preExecute(Repository repository)
						throws RuntimeException {
					Ref sourceref = null;
					try {
						sourceref = repository.getRef(sourceBranch);
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
					if (sourceref == null) {
						BranchOperation checkout = new BranchOperation(
								repository, branchName);
						checkout.execute();
						result.put("checkOutResult", checkout.getResult());
					} else {
						FetchResult fetch = fetchBranch(remoteUrl, sourceBranch);
						result.put("fetchResult", fetch);
					}
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

	private MergeResult mergeBranch(final String remoteUrl,
			final String sourceBranch, final String targetBranch) {
		String projectPaht = getGirdir(remoteUrl) + Constants.DOT_GIT;
		MergeOperation mergeOperation;
		try {
			Repository repository = new FileRepository(projectPaht);
			Ref sourceref = repository.getRef(sourceBranch);
			Ref targetref = repository.getRef(sourceBranch);
			if (sourceref == null || targetref == null)
				throw new IllegalArgumentException(
						sourceref == null ? sourceBranch : targetBranch
								+ " has not checkout");
			String source = Constants.R_HEADS + sourceBranch;
			mergeOperation = new MergeOperation(repository, source);

			mergeOperation.addPreExecuteTask(new PreExecuteTask() {
				public void preExecute(Repository repository)
						throws RuntimeException {
					fetchBranch(remoteUrl, sourceBranch);
				}
			});

			mergeOperation.addPreExecuteTask(new PreExecuteTask() {
				public void preExecute(Repository repository)
						throws RuntimeException {
					fetchBranch(remoteUrl, targetBranch);
				}
			});
			mergeOperation.execute();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return mergeOperation.getResult();
	}

	private RevCommit getRevCommit(String remoteUrl, String branchName) {
		String projectPaht = getGirdir(remoteUrl) + Constants.DOT_GIT;
		RevCommit commit;
		try {
			Repository repository = new FileRepository(projectPaht);
			Ref branch = repository.getRef(branchName);
			if (branch == null)
				swtichBranch(remoteUrl, branchName);
			commit = RepositoryUtil.parseBranchCommit(repository, branchName);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return commit;
	}

	public Result doCommand(Content content) {
		String command = content.get("command");
		String remoteUrl = content.get("remoteurl");
		Result result = new Result();
		switch (command) {
		case "clone": {
			String branchName = content.get("branch");
			cloneBranch(remoteUrl, branchName);
			result.setResultCode("000");
			result.setMessage("success");
			break;
		}
		case "fetch": {
			String branchName = content.get("branch");
			FetchResult fetchreuslt = fetchBranch(remoteUrl, branchName);
			result.setResultCode("000");
			result.setMessage(fetchreuslt.getMessages());
			break;
		}
		case "swtich": {
			String branchName = content.get("branch");
			CheckoutResult checkoutResult = swtichBranch(remoteUrl, branchName);
			result.setResultCode("000");
			if (checkoutResult.getStatus() != Status.OK)
				result.setResultCode("001");
			result.setMessage(getcheckoutMessage(checkoutResult));
			break;
		}
		case "create": {
			String sourceBranch = content.get("source");
			String targetBranch = content.get("target");
			Map<String, Object> reusltMap = createBranch(remoteUrl,
					targetBranch, sourceBranch);
			CheckoutResult checkout = (CheckoutResult) reusltMap
					.get("checkOutResult");
			FetchResult fetch = (FetchResult) reusltMap.get("fetchResult");
			PushOperationResult push = (PushOperationResult) reusltMap
					.get("pushResult");
			if (push != null) {
				if (push.isSuccessfulConnectionForAnyURI()) {
					result.setResultCode("000");
					result.setMessage("success");
				} else {
					result.setResultCode("001");
					result.setMessage(push.getErrorStringForAllURis());
				}
			} else {
				result.setResultCode("001");
				if (checkout.getStatus() != Status.OK) {
					result.setResultCode("001");
					result.setMessage(getcheckoutMessage(checkout));
				} else {
					result.setResultCode("001");
					result.setMessage(fetch.getMessages());
				}
			}
			break;
		}
		case "merge": {
			String sourceBranch = content.get("source");
			String targetBranch = content.get("target");
			MergeResult mergeResult = mergeBranch(remoteUrl, sourceBranch,
					targetBranch);
			if (mergeResult != null) {
				if (mergeResult.getMergeStatus().isSuccessful()) {
					result.setResultCode("000");
					result.setMessage("success");
				} else {
					result.setResultCode("001");
					result.setMessage(getmergeMessage(mergeResult));
				}
			} else {
				result.setResultCode("001");
				result.setMessage("fail exception");
			}
			break;
		}
		case "getcommt": {
			String branchName = content.get("branch");
			RevCommit commit = getRevCommit(remoteUrl, branchName);
			result.setResultCode("000");
			result.setMessage(commit.getName());
		}

		}

		return null;
	}

	private String getcheckoutMessage(CheckoutResult checkoutResult) {
		List<String> messageList = null;
		if (checkoutResult.getStatus() == Status.CONFLICTS)
			messageList = checkoutResult.getConflictList();
		if (checkoutResult.getStatus() == Status.NONDELETED)
			messageList = checkoutResult.getUndeletedList();
		if (messageList == null) {
			messageList = new ArrayList<String>(0);
			messageList.addAll(checkoutResult.getModifiedList());
			messageList.addAll(checkoutResult.getRemovedList());
		}
		return messageList.toString();
	}

	private String getmergeMessage(MergeResult mergeResult) {
		Map<String, int[][]> allConflicts = mergeResult.getConflicts();
		StringBuilder sb = new StringBuilder();
		for (String path : allConflicts.keySet()) {
			int[][] c = allConflicts.get(path);
			sb.append("Path:Conflicts in file " + path + ";Result is:");
			for (int i = 0; i < c.length; ++i) {
				System.out.println("  Conflict #" + i);
				for (int j = 0; j > (c[i].length) - 1; ++j) {
					if (c[i][j] >= 0)
						sb.append("    Chunk for "
								+ mergeResult.getMergedCommits()[j]
								+ " starts on line #" + c[i][j]);
				}
			}
		}
		Map<String, MergeFailureReason> failreason = mergeResult
				.getFailingPaths();
		for (String path : failreason.keySet()) {
			MergeFailureReason reason = failreason.get(path);
			sb.append("Path:Conflicts in file " + path + ";Result is:"
					+ reason.toString());
		}
		return sb.toString();

	}
}
