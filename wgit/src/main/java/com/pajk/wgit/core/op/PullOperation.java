package com.pajk.wgit.core.op;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.MergeResult.MergeStatus;
import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.errors.DetachedHeadException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidConfigurationException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.osgi.util.NLS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pajk.wgit.core.CoreException;
import com.pajk.wgit.core.internal.CoreText;

/**
 * Wraps the JGit API {@link PullCommand} into an operation
 */
public class PullOperation extends BaseOperation {

	private static Logger logger = LoggerFactory.getLogger(PullOperation.class);

	private final Map<Repository, Object> results = new LinkedHashMap<Repository, Object>();

	private final int timeout;

	private CredentialsProvider credentialsProvider;

	public PullOperation(String remoteUrl, int timeout) throws IOException {
		super(remoteUrl);
		this.timeout = timeout;
	}

	/**
	 * @param repositories
	 *            the repository
	 * @param timeout
	 *            in seconds
	 */
	public PullOperation(Repository repository, int timeout) {
		super(repository);
		this.timeout = timeout;
	}

	public boolean refreshNeeded(PullResult pullResult) {
		if (pullResult == null)
			return true;
		MergeResult mergeResult = pullResult.getMergeResult();
		if (mergeResult == null)
			return true;
		if (mergeResult.getMergeStatus() == MergeStatus.ALREADY_UP_TO_DATE)
			return false;
		return true;
	}

	/**
	 * @return the results, or an empty Map if this has not been executed
	 */
	public Map<Repository, Object> getResults() {
		return this.results;
	}

	/**
	 * @param credentialsProvider
	 */
	public void setCredentialsProvider(CredentialsProvider credentialsProvider) {
		this.credentialsProvider = credentialsProvider;
	}

	/**
	 * @return the operation's credentials provider
	 */
	public CredentialsProvider getCredentialsProvider() {
		return credentialsProvider;
	}

	public void execute() throws CoreException {

		if (!results.isEmpty())
			throw new RuntimeException(CoreText.OperationAlreadyExecuted);

		PullCommand pull = new Git(repository).pull();
		PullResult pullResult = null;
		try {
			pull.setTimeout(timeout);
			pull.setCredentialsProvider(credentialsProvider);
			pullResult = pull.call();
			results.put(repository, pullResult);
		} catch (DetachedHeadException e) {
			String message = NLS.bind(
					CoreText.PullOperation_DetachedHeadMessage,
					repository.toString(), e.getMessage());
			logger.debug(message, e);
			throw new CoreException(message, e);
		} catch (InvalidConfigurationException e) {
			String message = NLS.bind(
					CoreText.PullOperation_PullNotConfiguredMessage,
					repository.toString(), e.getMessage());
			logger.debug(message, e);
			throw new CoreException(message, e);
		} catch (GitAPIException e) {
			String message = NLS.bind(CoreText.PullOperation_Failed,
					repository.toString(), e.getMessage());
			logger.debug(message, e);
			throw new CoreException(message, e);
		} catch (JGitInternalException e) {
			String message = NLS.bind(CoreText.PullOperation_Failed,
					repository.toString(), e.getMessage());
			logger.debug(message, e);
			throw new CoreException(message, e);
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
		}
		PullResult pullresult = (PullResult) this.getResults().get(repository);
		boolean isSuccess = pullresult.isSuccessful();
		if (!isSuccess) {
			result.setResultCode("001");
			result.setMessage(pullresult.toString());
			return result;
		}
		return result;
	}
}
