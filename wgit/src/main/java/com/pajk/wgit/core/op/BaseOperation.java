package com.pajk.wgit.core.op;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.jgit.errors.AmbiguousObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

import com.pajk.wgit.core.CoreException;
import com.pajk.wgit.core.internal.Utils;

public abstract class BaseOperation implements IWGitOperation {

	private static final String gitPraentPath = Utils
			.getPropertiesByClasspath().getProperty("gitPraentPath");

	public static String getGirdir(String remoteUrl) {
		String[] strs = remoteUrl.split("/");
		String projectNames = strs[strs.length - 1];
		String projectPath = gitPraentPath
				+ projectNames.substring(0, projectNames.indexOf(".")) + "/";
		return projectPath;
	}

	public static boolean isRepositoryExist(String remoteUrl) {
		String projectPath = getGirdir(remoteUrl);
		File tempFile = new File(projectPath);
		if (tempFile.exists())
			return tempFile.isDirectory();
		else
			return false;
	}

	public RevCommit getRevCommit(String revision)
			throws RevisionSyntaxException, AmbiguousObjectException,
			IncorrectObjectTypeException, IOException {
		RevWalk walk = new RevWalk(repository);
		ObjectId objId = repository.resolve(revision);
		RevCommit revCommit = walk.parseCommit(objId);
		walk.dispose();
		return revCommit;
	}

	protected final Repository repository;

	protected Collection<PreExecuteTask> preTasks;

	protected Collection<PostExecuteTask> postTasks;

	BaseOperation(final String remoteUrl) throws IOException {
		String projectPath = getGirdir(remoteUrl) + Constants.DOT_GIT;
		this.repository = new FileRepository(projectPath);

	}

	BaseOperation(final Repository repository) {
		this.repository = repository;
	}

	/**
	 * Invoke all pre-execute tasks
	 * 
	 * @param monitor
	 * @throws CoreException
	 */
	protected void preExecute() throws RuntimeException {
		synchronized (this) {
			if (preTasks != null)
				for (PreExecuteTask task : preTasks)
					task.preExecute(repository);
		}
	}

	/**
	 * Invoke all post-execute tasks
	 * 
	 * @param monitor
	 * @throws CoreException
	 */
	protected void postExecute() throws RuntimeException {
		synchronized (this) {
			if (postTasks != null)
				for (PostExecuteTask task : postTasks)
					task.postExecute(repository);
		}
	}

	/**
	 * @param task
	 *            to be performed before execution
	 */
	public synchronized void addPreExecuteTask(PreExecuteTask task) {
		if (preTasks == null)
			preTasks = new ArrayList<PreExecuteTask>();
		preTasks.add(task);
	}

	/**
	 * @param task
	 *            to be performed after execution
	 */
	public synchronized void addPostExecuteTask(PostExecuteTask task) {
		if (postTasks == null)
			postTasks = new ArrayList<PostExecuteTask>();
		postTasks.add(task);
	}

}
