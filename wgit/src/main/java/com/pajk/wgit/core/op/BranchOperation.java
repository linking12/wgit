package com.pajk.wgit.core.op;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.CheckoutResult;
import org.eclipse.jgit.api.CheckoutResult.Status;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.util.FileUtils;
import org.eclipse.osgi.util.NLS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pajk.wgit.core.CommitUtil;
import com.pajk.wgit.core.CoreException;
import com.pajk.wgit.core.RepositoryUtil;
import com.pajk.wgit.core.internal.CoreText;

public class BranchOperation extends BaseOperation {

	private static Logger logger = LoggerFactory
			.getLogger(BranchOperation.class);

	private final String target;

	private CheckoutResult result;

	public BranchOperation(Repository repository, String target) {
		super(repository);
		this.target = target;
	}

	public BranchOperation(String remoteUrl, String target) throws IOException {
		super(remoteUrl);
		this.target = target;
	}

	public void execute() throws CoreException {
		CheckoutCommand co = new Git(repository).checkout();
		try {
			Set<String> allBranchs = RepositoryUtil.getAllBranchs(repository);
			if (allBranchs.contains(target)) {// swtich branch
				co.setForce(true);
				co.setCreateBranch(true);
				co.setName(target);
				co.setStartPoint("origin/" + target);
			} else {
				RevCommit revCommit = super.getRevCommit(target);
				co.setStartPoint(revCommit);
				co.setName(CommitUtil.getBranchName(revCommit, repository));
			}
		} catch (Throwable e) {
			throw new CoreException(e.getLocalizedMessage(), e);
		}
		try {
			co.call();
		} catch (CheckoutConflictException e) {
			return;
		} catch (JGitInternalException e) {
			String message = NLS.bind(
					CoreText.BranchOperation_performingBranch, target,
					e.getMessage());
			logger.debug(message, e);
			throw new CoreException(message, e);
		} catch (GitAPIException e) {
			String message = NLS.bind(
					CoreText.BranchOperation_performingBranch, target,
					e.getMessage());
			logger.debug(message, e);
			throw new CoreException(message, e);
		} finally {
			BranchOperation.this.result = co.getResult();
		}
		if (result.getStatus() == Status.NONDELETED)
			retryDelete(result.getUndeletedList());
		synchronized (this) {
			if (postTasks != null)
				for (PostExecuteTask task : postTasks)
					task.postExecute(repository);
		}
	}

	/**
	 * @return the result of the operation
	 */
	public CheckoutResult getResult() {
		return result;
	}

	private void retryDelete(List<String> pathList) {
		// try to delete, but for a short time only
		long startTime = System.currentTimeMillis();
		for (String path : pathList) {
			if (System.currentTimeMillis() - startTime > 1000)
				break;
			File fileToDelete = new File(repository.getWorkTree(), path);
			if (fileToDelete.exists())
				try {
					// Only files should be passed here, thus
					// we ignore attempt to delete submodules when
					// we switch to a branch without a submodule
					if (!fileToDelete.isFile())
						FileUtils.delete(fileToDelete, FileUtils.RETRY);
				} catch (IOException e) {
					// ignore here
				}
		}
	}

	@Override
	public Result run() {
		Result result = new Result();
		try {
			this.execute();
		} catch (Exception e) {
			result.setResultCode("001");
			result.setMessage(e.getMessage());
			return result;
		}
		CheckoutResult checkoutResult = this.getResult();
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
		result.setMessage(messageList.toString());
		return result;
	}
}
