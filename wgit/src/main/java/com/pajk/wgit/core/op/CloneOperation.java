package com.pajk.wgit.core.op;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.util.FileUtils;
import org.eclipse.osgi.util.NLS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pajk.wgit.core.CoreException;
import com.pajk.wgit.core.internal.CoreText;
import com.pajk.wgit.core.internal.Utils;

public class CloneOperation implements IWGitOperation {

	private static Logger logger = LoggerFactory
			.getLogger(CloneOperation.class);

	private final URIish uri;

	private final boolean allSelected;

	private boolean cloneSubmodules;

	private final Collection<Ref> selectedBranches;

	private final File workdir;

	private final File gitdir;

	private final String refName;

	private final String remoteName;

	private final int timeout;

	private CredentialsProvider credentialsProvider;

	private List<PostExecuteTask> postCloneTasks;

	private static final String gitPraentPath = Utils
			.getPropertiesByClasspath().getProperty("gitPraentPath");

	private String getGirdir(String remoteUrl) {
		String[] strs = remoteUrl.split("/");
		String projectNames = strs[strs.length - 1];
		String projectPath = gitPraentPath
				+ projectNames.substring(0, projectNames.indexOf(".")) + "/";
		return projectPath;
	}

	public CloneOperation(final URIish uri, final boolean allSelected,
			final Collection<Ref> selectedBranches, final File workdir,
			final String refName, final String remoteName, int timeout) {
		this.uri = uri;
		this.allSelected = allSelected;
		this.selectedBranches = selectedBranches;
		this.workdir = workdir;
		this.gitdir = new File(workdir, Constants.DOT_GIT);
		this.refName = refName;
		this.remoteName = remoteName;
		this.timeout = timeout;
	}

	public CloneOperation(final String remoteUrl, final String branchName)
			throws URISyntaxException {
		String projectPath = getGirdir(remoteUrl) + Constants.DOT_GIT;
		this.uri = new URIish(remoteUrl);
		this.allSelected = true;
		this.selectedBranches = null;
		this.workdir = new File(projectPath);
		this.gitdir = new File(workdir, Constants.DOT_GIT);
		this.refName = "refs/heads/" + branchName;
		this.remoteName = "origin";
		this.timeout = 0;
	}

	/**
	 * Sets a credentials provider
	 * 
	 * @param credentialsProvider
	 */
	public void setCredentialsProvider(CredentialsProvider credentialsProvider) {
		this.credentialsProvider = credentialsProvider;
	}

	/**
	 * @param cloneSubmodules
	 *            true to initialize and update submodules
	 */
	public void setCloneSubmodules(boolean cloneSubmodules) {
		this.cloneSubmodules = cloneSubmodules;
	}

	/**
	 * @return The git directory which will contain the repository
	 */
	public File getGitDir() {
		return gitdir;
	}

	/**
	 * @param task
	 *            to be performed after clone
	 */
	public synchronized void addPostCloneTask(PostExecuteTask task) {
		if (postCloneTasks == null)
			postCloneTasks = new ArrayList<PostExecuteTask>();
		postCloneTasks.add(task);
	}

	public void execute() throws CoreException {
		Repository repository = null;
		try {
			CloneCommand cloneRepository = Git.cloneRepository();
			cloneRepository.setCredentialsProvider(credentialsProvider);
			if (refName != null)
				cloneRepository.setBranch(refName);
			else
				cloneRepository.setNoCheckout(true);
			cloneRepository.setDirectory(workdir);
			cloneRepository.setRemote(remoteName);
			cloneRepository.setURI(uri.toString());
			cloneRepository.setTimeout(timeout);
			cloneRepository.setCloneAllBranches(allSelected);
			cloneRepository.setCloneSubmodules(cloneSubmodules);
			if (selectedBranches != null) {
				List<String> branches = new ArrayList<String>();
				for (Ref branch : selectedBranches)
					branches.add(branch.getName());
				cloneRepository.setBranchesToClone(branches);
			}
			Git git = cloneRepository.call();
			repository = git.getRepository();
			synchronized (this) {
				if (postCloneTasks != null)
					for (PostExecuteTask task : postCloneTasks)
						task.postExecute(git.getRepository());
			}
		} catch (final Exception e) {
			String message = NLS.bind(CoreText.CloneOperation_failed,
					e.getMessage());
			logger.debug(message, e);
			try {
				if (repository != null)
					repository.close();
				FileUtils.delete(workdir, FileUtils.RECURSIVE);
			} catch (IOException ioe) {
				throw new CoreException(CoreText.CloneOperation_failed_cleanup,
						e);
			}
		} finally {
			if (repository != null)
				repository.close();
		}

	}

	@Override
	public Result run() {
		Result result = new Result();
		try {
			this.execute();
		} catch (RuntimeException e) {
			result.setResultCode("001");
			result.setMessage(e.getMessage());
		}
		return result;
	}
}
