package com.pajk.wgit.core.op;
import org.eclipse.jgit.lib.Repository;


public interface IWGitOperation {

	/**
	 * Executes the operation
	 * 
	 * @param monitor
	 *            a progress monitor, or <code>null</code> if progress reporting
	 *            and cancellation are not desired
	 * @throws CoreException
	 */
	void execute() throws RuntimeException;

	

	/**
	 * A task to be performed before execution begins
	 */
	interface PreExecuteTask {

		/**
		 * Executes the task
		 * 
		 * @param repository
		 *            the git repository
		 * 
		 * @param monitor
		 *            a progress monitor, or <code>null</code> if progress
		 *            reporting and cancellation are not desired
		 * @throws CoreException
		 */
		void preExecute(Repository repository)
				throws RuntimeException;
	}

	/**
	 * A task to be performed after execution completes
	 */
	interface PostExecuteTask {

		/**
		 * Executes the task
		 * 
		 * @param repository
		 *            the git repository
		 * 
		 * @param monitor
		 *            a progress monitor, or <code>null</code> if progress
		 *            reporting and cancellation are not desired
		 * @throws CoreException
		 */
		void postExecute(Repository repository)
				throws RuntimeException;
	}

}
