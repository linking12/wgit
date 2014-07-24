/*******************************************************************************
 * Copyright (C) 2008, Marek Zawirski <marek.zawirski@gmail.com>
 * Copyright (C) 2011, Mathias Kinzler <mathias.kinzler@sap.com>
 * Copyright (C) 2012, Robin Stocker <robin@nibor.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.pajk.wgit.core.op;

import java.io.OutputStream;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.eclipse.jgit.transport.RemoteRefUpdate.Status;
import org.eclipse.jgit.transport.URIish;

import com.pajk.wgit.core.internal.CoreText;

/**
 * Push operation: pushing from local repository to one or many remote ones.
 */
public class PushOperation extends BaseOperation {

	private final PushOperationSpecification specification;

	private final boolean dryRun;

	private final String remoteName;

	private final int timeout;

	private OutputStream out;

	private PushOperationResult operationResult;

	private CredentialsProvider credentialsProvider;

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

	public void execute() throws RuntimeException {

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
			throw new RuntimeException(e);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}
}
