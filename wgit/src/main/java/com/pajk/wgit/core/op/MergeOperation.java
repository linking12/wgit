package com.pajk.wgit.core.op;

import java.io.IOException;
import java.util.Map;

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
import org.eclipse.jgit.merge.ResolveMerger.MergeFailureReason;
import org.eclipse.osgi.util.NLS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pajk.wgit.core.CoreException;
import com.pajk.wgit.core.internal.CoreText;

/**
 * This class implements the merge of a ref with the current head
 * 
 */
public class MergeOperation extends BaseOperation {

	private static Logger logger = LoggerFactory
			.getLogger(MergeOperation.class);

	private final String refName;

	private MergeStrategy mergeStrategy;

	private Boolean squash;

	private FastForwardMode fastForwardMode;

	private Boolean commit;

	private MergeResult mergeResult;

	public MergeOperation(String remoteUrl, String refName) throws IOException {
		super(remoteUrl);
		this.refName = refName;
	}

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

	public void execute() throws CoreException {

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
			String message = NLS.bind(CoreText.CommonOperation_InternalError,
					e.getMessage());
			logger.debug(message, e);
			throw new CoreException(message, e);
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
				throw new CoreException(refName + mergeResult.toString());
		} catch (NoHeadException e) {
			throw new CoreException(CoreText.MergeOperation_MergeFailedNoHead,
					e);
		} catch (ConcurrentRefUpdateException e) {
			throw new CoreException(
					CoreText.MergeOperation_MergeFailedRefUpdate, e);
		} catch (CheckoutConflictException e) {
			mergeResult = new MergeResult(e.getConflictingPaths());
			return;
		} catch (GitAPIException e) {
			throw new CoreException(e.getLocalizedMessage(), e.getCause());
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
		result.setResultCode(mergeResult.getMergeStatus().isSuccessful() ? "000"
				: "001");
		Map<String, int[][]> allConflicts = mergeResult.getConflicts();
		StringBuilder sb = new StringBuilder();
		if (allConflicts != null) {
			for (String path : allConflicts.keySet()) {
				int[][] c = allConflicts.get(path);
				sb.append("Path:Conflicts in file " + path + ";Result is:");
				for (int i = 0; i < c.length; ++i) {
					for (int j = 0; j > (c[i].length) - 1; ++j) {
						if (c[i][j] >= 0)
							sb.append("    Chunk for "
									+ mergeResult.getMergedCommits()[j]
									+ " starts on line #" + c[i][j]);
					}
				}
			}
		}
		Map<String, MergeFailureReason> failreason = mergeResult
				.getFailingPaths();
		if (failreason != null) {
			for (String path : failreason.keySet()) {
				MergeFailureReason reason = failreason.get(path);
				sb.append("Path:" + path + ";Conflicts in file:"
						+ reason.toString());
			}
		}
		result.setMessage(sb.toString());
		return result;
	}
}
