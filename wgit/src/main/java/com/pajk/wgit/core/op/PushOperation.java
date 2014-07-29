package com.pajk.wgit.core.op;

import java.io.IOException;
import java.io.OutputStream;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.eclipse.jgit.transport.RemoteRefUpdate.Status;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.osgi.util.NLS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pajk.wgit.core.CoreException;
import com.pajk.wgit.core.internal.CoreText;

/**
 * Push operation: pushing from local repository to one or many remote ones.
 */
public class PushOperation extends BaseOperation {

	private static Logger logger = LoggerFactory.getLogger(PushOperation.class);

	private final PushOperationSpecification specification;

	private final boolean dryRun;

	private final String remoteName;

	private final int timeout;

	private OutputStream out;

	private PushOperationResult operationResult;

	private CredentialsProvider credentialsProvider;

	public PushOperation(String remoteUrl, final boolean dryRun,
			final String remoteName, int timeout) throws IOException {
		super(remoteUrl);
		this.dryRun = dryRun;
		this.timeout = timeout;
		this.specification = null;
		this.remoteName = remoteName;
	}

	/**
	 * Create push operation for provided specification.
	 * 
	 * @param localDb
	 *            local repository.
	 * @param specification
	 *            specification of ref updates for remote repositories.
	 * @param dryRun
	 *            true if push operation should just check for possible result
	 *            and not really update remote refs, false otherwise - when push
	 *            should act normally.
	 * @param timeout
	 *            the timeout in seconds (0 for no timeout)
	 */
	public PushOperation(final Repository localDb, final boolean dryRun,
			int timeout) {
		this(localDb, null, null, dryRun, timeout);
	}

	/**
	 * Create push operation for provided specification.
	 * 
	 * @param localDb
	 *            local repository.
	 * @param specification
	 *            specification of ref updates for remote repositories.
	 * @param dryRun
	 *            true if push operation should just check for possible result
	 *            and not really update remote refs, false otherwise - when push
	 *            should act normally.
	 * @param timeout
	 *            the timeout in seconds (0 for no timeout)
	 */
	public PushOperation(final Repository localDb,
			final PushOperationSpecification specification,
			final boolean dryRun, int timeout) {
		this(localDb, null, specification, dryRun, timeout);
	}

	/**
	 * Creates a push operation for a remote configuration.
	 * 
	 * @param localDb
	 * @param remoteName
	 * @param dryRun
	 * @param timeout
	 */
	public PushOperation(final Repository localDb, final String remoteName,
			final boolean dryRun, int timeout) {
		this(localDb, remoteName, null, dryRun, timeout);
	}

	private PushOperation(final Repository localDb, final String remoteName,
			PushOperationSpecification specification, final boolean dryRun,
			int timeout) {
		super(localDb);
		this.specification = specification;
		this.dryRun = dryRun;
		this.remoteName = remoteName;
		this.timeout = timeout;
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
	 * @return push operation result
	 */
	public PushOperationResult getOperationResult() {
		if (operationResult == null)
			throw new IllegalStateException(CoreText.OperationNotYetExecuted);
		return operationResult;
	}

	/**
	 * @return operation specification, as provided in constructor (may be
	 *         <code>null</code>)
	 */
	public PushOperationSpecification getSpecification() {
		return specification;
	}

	/**
	 * Sets the output stream this operation will write sideband messages to.
	 * 
	 * @param out
	 *            the outputstream to write to
	 * @since 3.0
	 */
	public void setOutputStream(OutputStream out) {
		this.out = out;
	}

	public void execute() throws CoreException {

		if (operationResult != null)
			throw new IllegalStateException(CoreText.OperationAlreadyExecuted);

		if (this.specification != null)
			for (URIish uri : this.specification.getURIs()) {
				for (RemoteRefUpdate update : this.specification
						.getRefUpdates(uri))
					if (update.getStatus() != Status.NOT_ATTEMPTED)
						throw new IllegalStateException(
								CoreText.RemoteRefUpdateCantBeReused);
			}
		operationResult = new PushOperationResult();
		Git git = new Git(repository);
		try {
			Iterable<PushResult> results = null;
			if (remoteName != null) {
				results = git.push().setRemote(remoteName).setDryRun(dryRun)
						.setTimeout(timeout)
						.setCredentialsProvider(credentialsProvider)
						.setOutputStream(out).call();
			} else {
				results = git.push().setPushAll().setDryRun(dryRun)
						.setTimeout(timeout)
						.setCredentialsProvider(credentialsProvider)
						.setOutputStream(out).call();
			}
			for (PushResult result : results) {
				operationResult.addOperationResult(result.getURI(), result);
			}
		} catch (JGitInternalException e) {
			String message = NLS.bind(CoreText.PushOperation_failed,
					repository.toString(), e.getMessage());
			logger.debug(message, e);
			throw new CoreException(message, e);
		} catch (Exception e) {
			String message = NLS.bind(CoreText.PushOperation_failed,
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
			return result;
		}
		PushOperationResult pushResult = this.getOperationResult();
		if (!pushResult.isSuccessfulConnectionForAnyURI()) {
			result.setResultCode("001");
			result.setMessage(pushResult.getErrorStringForAllURis());
		}
		return result;
	}
}
