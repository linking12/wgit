/*******************************************************************************
 * Copyright (c) 2010, 2014 SAP AG and others.
 * Copyright (C) 2012, 2013 Tomasz Zarna <tzarna@gmail.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Stefan Lay (SAP AG) - initial implementation
 *    Tomasz Zarna (IBM) - merge squash, bug 382720
 *******************************************************************************/
package com.pajk.wgit.core.op;

import java.io.IOException;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeCommand;
import org.eclipse.jgit.api.MergeCommand.FastForwardMode;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.ConcurrentRefUpdateException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.merge.MergeStrategy;

import com.pajk.wgit.core.internal.CoreText;

/**
 * This class implements the merge of a ref with the current head
 * 
 */
public class MergeOperation extends BaseOperation {

	private final String refName;

	private MergeStrategy mergeStrategy;

	private Boolean squash;

	private FastForwardMode fastForwardMode;

	private Boolean commit;

	private MergeResult mergeResult;

	/**
	 * @param repository
	 * @param refName
	 *            name of a commit which should be merged
	 */
	public MergeOperation(Repository repository, String refName) {
		super(repository);
		this.refName = refName;
	}

	/**
	 * Create a MergeOperation object
	 * 
	 * @param repository
	 * @param refName
	 *            name of a commit which should be merged
	 * @param mergeStrategy
	 *            the strategy to use for merge
	 */
	public MergeOperation(Repository repository, String refName,
			String mergeStrategy) {
		super(repository);
		this.refName = refName;
		if (mergeStrategy != null)
			this.mergeStrategy = MergeStrategy.get(mergeStrategy);
	}

	/**
	 * @param squash
	 *            true to squash merge commits
	 */
	public void setSquash(boolean squash) {
		this.squash = Boolean.valueOf(squash);
	}

	/**
	 * @param ffmode
	 *            set the fast forward mode
	 * @since 3.0
	 */
	public void setFastForwardMode(FastForwardMode ffmode) {
		this.fastForwardMode = ffmode;
	}

	/**
	 * @param commit
	 *            set the commit option
	 * @since 3.1
	 */
	public void setCommit(boolean commit) {
		this.commit = Boolean.valueOf(commit);
	}

	/**
	 * @return the merge result, or <code>null</code> if this has not been
	 *         executed or if an exception occurred
	 */
	public MergeResult getResult() {
		return this.mergeResult;
	}

	public void execute() throws RuntimeException {

		if (mergeResult != null)
			throw new RuntimeException(refName
					+ CoreText.OperationAlreadyExecuted);
		Git git = new Git(repository);
		synchronized (this) {
			if (preTasks != null)
				for (PreExecuteTask task : preTasks)
					task.preExecute(repository);
		}
		MergeCommand merge = git.merge();
		try {
			Ref ref = repository.getRef(refName);
			if (ref != null)
				merge.include(ref);
			else
				merge.include(ObjectId.fromString(refName));
		} catch (IOException e) {
			throw new RuntimeException(CoreText.MergeOperation_InternalError, e);
		}
		if (fastForwardMode != null)
			merge.setFastForward(fastForwardMode);
		if (commit != null)
			merge.setCommit(commit.booleanValue());
		if (squash != null)
			merge.setSquash(squash.booleanValue());
		if (mergeStrategy != null) {
			merge.setStrategy(mergeStrategy);
		}
		try {
			mergeResult = merge.call();
			if (MergeResult.MergeStatus.NOT_SUPPORTED.equals(mergeResult
					.getMergeStatus()))
				throw new RuntimeException(refName + mergeResult.toString());
		} catch (NoHeadException e) {
			throw new RuntimeException(
					CoreText.MergeOperation_MergeFailedNoHead, e);
		} catch (ConcurrentRefUpdateException e) {
			throw new RuntimeException(
					CoreText.MergeOperation_MergeFailedRefUpdate, e);
		} catch (CheckoutConflictException e) {
			mergeResult = new MergeResult(e.getConflictingPaths());
			return;
		} catch (GitAPIException e) {
			throw new RuntimeException(e.getLocalizedMessage(), e.getCause());
		}

	}
}
