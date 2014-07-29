package com.pajk.wgit.core.op;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jgit.api.FetchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.FetchResult;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.TagOpt;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.osgi.util.NLS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pajk.wgit.core.CoreException;
import com.pajk.wgit.core.internal.CoreText;

/**
 * Used to fetch from another Repository
 */
public class FetchOperation extends BaseOperation {

	private static Logger logger = LoggerFactory
			.getLogger(FetchOperation.class);

	private final RemoteConfig rc;

	private final URIish uri;

	private final int timeout;

	private final List<RefSpec> specs;

	private final boolean dryRun;

	private FetchResult operationResult;

	private CredentialsProvider credentialsProvider;

	private TagOpt tagOpt;

	public FetchOperation(Repository repository, URIish uri, int timeout,
			boolean dryRun) {
		super(repository);
		this.timeout = timeout;
		this.dryRun = dryRun;
		this.uri = uri;
		List<RefSpec> specs = new ArrayList<RefSpec>();
		specs.add(new RefSpec("+refs/heads/*:refs/remotes/origin/*"));
		specs.add(new RefSpec("+refs/tags/*:refs/tags/*"));
		specs.add(new RefSpec("+refs/notes/*:refs/notes/*"));
		this.specs = specs;
		this.rc = null;

	}

	/**
	 * Constructs a FetchOperation based on URI and RefSpecs
	 * 
	 * @param repository
	 * @param uri
	 * @param refSpecs
	 * @param timeout
	 * @param dryRun
	 * 
	 */
	public FetchOperation(Repository repository, URIish uri,
			List<RefSpec> refSpecs, int timeout, boolean dryRun) {
		super(repository);
		this.timeout = timeout;
		this.dryRun = dryRun;
		this.uri = uri;
		this.specs = refSpecs;
		this.rc = null;
	}

	/**
	 * Constructs a FetchOperation based on a RemoteConfig
	 * 
	 * @param repository
	 * @param config
	 * @param timeout
	 * @param dryRun
	 */
	public FetchOperation(Repository repository, RemoteConfig config,
			int timeout, boolean dryRun) {
		super(repository);
		this.timeout = timeout;
		this.dryRun = dryRun;
		this.uri = null;
		this.specs = null;
		this.rc = config;
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

	/**
	 * @param tagOpt
	 */
	public void setTagOpt(TagOpt tagOpt) {
		this.tagOpt = tagOpt;
	}

	/**
	 * @return the result, or <code>null</code> if the operation has not been
	 *         executed
	 */
	public FetchResult getOperationResult() {
		return operationResult;
	}

	public void execute() throws CoreException {

		if (operationResult != null)
			throw new IllegalStateException(CoreText.OperationAlreadyExecuted);
		FetchCommand command;
		if (rc == null)
			command = new Git(repository).fetch()
					.setRemote(uri.toPrivateString()).setRefSpecs(specs);
		else
			command = new Git(repository).fetch().setRemote(rc.getName());
		command.setCredentialsProvider(credentialsProvider).setTimeout(timeout)
				.setDryRun(dryRun);
		if (tagOpt != null)
			command.setTagOpt(tagOpt);
		try {
			operationResult = command.call();
		} catch (JGitInternalException e) {
			String message = NLS.bind(CoreText.FetchOperation_failed,
					repository.toString(), e.getMessage());
			logger.debug(message, e);
			throw new CoreException(message, e);
		} catch (Exception e) {
			String message = NLS.bind(CoreText.FetchOperation_failed,
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
		result.setMessage(getOperationResult().getMessages());
		return result;
	}
}
