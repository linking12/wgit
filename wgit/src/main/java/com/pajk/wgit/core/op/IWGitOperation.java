package com.pajk.wgit.core.op;

import org.eclipse.jgit.lib.Repository;

public interface IWGitOperation {

	/**
	 * Executes the operation
	 */
	Result run();

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
		void preExecute(Repository repository) throws RuntimeException;
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
		void postExecute(Repository repository) throws RuntimeException;
	}

	class Result {
		private String resultCode;
		private String message;

		public Result() {
			resultCode = "000";
		}

		public String getResultCode() {
			return resultCode;
		}

		public void setResultCode(String resultCode) {
			this.resultCode = resultCode;
		}

		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}

		public String toJson() {
			StringBuilder json = new StringBuilder();
			if (resultCode.equals("000")) {
				json.append("{\"code\":000");
				json.append(",\"content\":\"").append(message).append("\"");
				json.append("}");
			} else {
				json.append("{\"code\":").append(resultCode);
				json.append(",\"error\":\"").append(message).append("\"");
				json.append("}");
			}
			return json.toString();
		}
	}

}
