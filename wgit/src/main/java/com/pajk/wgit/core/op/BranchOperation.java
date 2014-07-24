/*******************************************************************************
 * Copyright (C) 2007, Dave Watson <dwatson@mimvista.com>
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2006, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2010, Jens Baumgart <jens.baumgart@sap.com>
 * Copyright (C) 2010, 2011, Mathias Kinzler <mathias.kinzler@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.pajk.wgit.core.op;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.CheckoutResult;
import org.eclipse.jgit.api.CheckoutResult.Status;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.util.FileUtils;

/**
 * This class implements checkouts of a specific revision. A check is made that
 * this can be done without data loss.
 */
public class BranchOperation extends BaseOperation {

	private final String target;

	private CheckoutResult result;

	/**
	 * Construct a {@link BranchOperation} object for a {@link Ref}.
	 * 
	 * @param repository
	 * @param target
	 *            a {@link Ref} name or {@link RevCommit} id
	 * @param delete
	 *            true to delete missing projects on new branch, false to close
	 *            them
	 */
	public BranchOperation(Repository repository, String target) {
		super(repository);
		this.target = target;
	}

	public void execute() throws RuntimeException {
		CheckoutCommand co = new Git(repository).checkout();
		co.setForce(true);
		co.setCreateBranch(true);
		co.setName(target);
		co.setStartPoint("origin/" + target);
		try {
			co.call();
		} catch (CheckoutConflictException e) {
			return;
		} catch (JGitInternalException e) {
			throw new RuntimeException(e);
		} catch (GitAPIException e) {
			throw new RuntimeException(e);
		} finally {
			BranchOperation.this.result = co.getResult();
		}
		if (result.getStatus() == Status.NONDELETED)
			retryDelete(result.getUndeletedList());
		synchronized (this) {
			if (postTasks != null)
				for (PostExecuteTask task : postTasks)
					task.postExecute(repository);
		}
	}

	/**
	 * @return the result of the operation
	 */
	public CheckoutResult getResult() {
		return result;
	}

	void retryDelete(List<String> pathList) {
		// try to delete, but for a short time only
		long startTime = System.currentTimeMillis();
		for (String path : pathList) {
			if (System.currentTimeMillis() - startTime > 1000)
				break;
			File fileToDelete = new File(repository.getWorkTree(), path);
			if (fileToDelete.exists())
				try {
					// Only files should be passed here, thus
					// we ignore attempt to delete submodules when
					// we switch to a branch without a submodule
					if (!fileToDelete.isFile())
						FileUtils.delete(fileToDelete, FileUtils.RETRY);
				} catch (IOException e) {
					// ignore here
				}
		}
	}

}
