/*******************************************************************************
 * Copyright (c) 2010 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler <mathias.kinzler@sap.com> - initial implementation
 *******************************************************************************/
package com.pajk.wgit.core.op;

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

import com.pajk.wgit.core.internal.CoreText;

/**
 * Wraps the JGit API {@link PullCommand} into an operation
 */
public class PullOperation extends BaseOperation {

	private final Map<Repository, Object> results = new LinkedHashMap<Repository, Object>();

	private final int timeout;

	private CredentialsProvider credentialsProvider;

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

	public void execute() throws RuntimeException {

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
			results.put(repository, CoreText.PullOperation_DetachedHeadMessage
					+ e.getMessage());
		} catch (InvalidConfigurationException e) {
			results.put(
					repository,
					CoreText.PullOperation_PullNotConfiguredMessage
							+ e.getMessage());
		} catch (GitAPIException e) {
			results.put(repository, e.getMessage());
		} catch (JGitInternalException e) {
			results.put(repository, e.getMessage());
		}

	}
}
