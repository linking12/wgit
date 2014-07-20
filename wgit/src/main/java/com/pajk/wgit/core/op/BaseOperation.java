package com.pajk.wgit.core.op;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.jgit.lib.Repository;

abstract class BaseOperation implements IWGitOperation{
	protected final Repository repository;

	protected Collection<PreExecuteTask> preTasks;

	protected Collection<PostExecuteTask> postTasks;

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
	public synchronized void addPreExecuteTask(final PreExecuteTask task) {
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
