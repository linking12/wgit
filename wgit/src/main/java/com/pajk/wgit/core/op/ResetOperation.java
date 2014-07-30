package com.pajk.wgit.core.op;

import java.io.IOException;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.osgi.util.NLS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pajk.wgit.core.CoreException;
import com.pajk.wgit.core.internal.CoreText;

public class ResetOperation extends BaseOperation {

	private static Logger logger = LoggerFactory
			.getLogger(ResetOperation.class);

	private final String revision;

	private final ResetType type;

	public ResetOperation(Repository repository, String revision, ResetType type) {
		super(repository);
		this.revision = revision;
		this.type = type;
	}

	public ResetOperation(String remoteUrl, String revision, ResetType type)
			throws IOException {
		super(remoteUrl);
		this.revision = revision;
		this.type = type;
	}

	public void execute() throws CoreException {
		String preVision = null;
		try {
			RevWalk walk = new RevWalk(repository);
			ObjectId objId = repository.resolve(revision);
			RevCommit revCommit = walk.parseCommit(objId);
			preVision = revCommit.getParent(0).getName();
		} catch (Throwable e) {
			String message = NLS.bind(CoreText.GetPrevision_failed,
					repository.toString(), e.getMessage());
			logger.debug(message, e);
			throw new CoreException(message, e);
		}
		ResetCommand reset = Git.wrap(repository).reset();
		reset.setMode(type);
		reset.setRef(preVision);
		try {
			reset.call();
		} catch (CheckoutConflictException e) {
			String message = NLS.bind(CoreText.ResetOperation_failed,
					repository.toString(), e.getMessage());
			logger.debug(message, e);
			throw new CoreException(message, e);
		} catch (GitAPIException e) {
			String message = NLS.bind(CoreText.ResetOperation_failed,
					repository.toString(), e.getMessage());
			logger.debug(message, e);
			throw new CoreException(message, e);
		} finally {
			repository.close();
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
		return result;
	}

}
