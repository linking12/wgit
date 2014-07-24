package com.pajk.wgit.core.op;

import java.io.IOException;

import org.eclipse.jgit.api.CreateBranchCommand.SetupUpstreamMode;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.revwalk.RevCommit;

public class CreateLocalBranchOperation extends BaseOperation {

	private final String name;

	private final Ref ref;

	private final RevCommit commit;

	private final UpstreamConfig upstreamConfig;

	/**
	 * @param repository
	 * @param name
	 *            the name for the new local branch (without prefix)
	 * @param ref
	 *            the branch or tag to base the new branch upon
	 * @param config
	 *            how to do the upstream configuration
	 */
	public CreateLocalBranchOperation(Repository repository, String name,
			Ref ref, UpstreamConfig config) {
		super(repository);
		this.name = name;
		this.ref = ref;
		this.commit = null;
		this.upstreamConfig = config;
	}

	/**
	 * @param repository
	 * @param name
	 *            the name for the new local branch (without prefix)
	 * @param commit
	 *            a commit to base the new branch upon
	 */
	public CreateLocalBranchOperation(Repository repository, String name,
			RevCommit commit) {
		super(repository);
		this.name = name;
		this.ref = null;
		this.commit = commit;
		this.upstreamConfig = null;
	}

	public void execute() throws RuntimeException {

		Git git = new Git(repository);
		try {
			synchronized (this) {
				if (preTasks != null)
					for (PreExecuteTask task : preTasks)
						task.preExecute(repository);
			}
			if (ref != null) {
				SetupUpstreamMode mode;
				if (upstreamConfig == UpstreamConfig.NONE)
					mode = SetupUpstreamMode.NOTRACK;
				else
					mode = SetupUpstreamMode.SET_UPSTREAM;
				git.branchCreate().setName(name).setStartPoint(ref.getName())
						.setUpstreamMode(mode).call();
			} else
				git.branchCreate().setName(name).setStartPoint(commit)
						.setUpstreamMode(SetupUpstreamMode.NOTRACK).call();
			synchronized (this) {
				if (postTasks != null)
					for (PostExecuteTask task : postTasks)
						task.postExecute(repository);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		if (UpstreamConfig.REBASE == upstreamConfig) {
			// set "branch.<name>.rebase" to "true"
			StoredConfig config = repository.getConfig();
			config.setBoolean(ConfigConstants.CONFIG_BRANCH_SECTION, name,
					ConfigConstants.CONFIG_KEY_REBASE, true);
			try {
				config.save();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

	}

	/**
	 * Describes how to configure the upstream branch
	 */
	public static enum UpstreamConfig {
		/** Rebase */
		REBASE(),
		/** Merge */
		MERGE(),
		/** No configuration */
		NONE();

		/**
		 * Get the default upstream config for the specified repository and
		 * upstream branch ref.
		 * 
		 * @param repo
		 * @param upstreamRefName
		 * @return the default upstream config
		 */
		public static UpstreamConfig getDefault(Repository repo,
				String upstreamRefName) {
			String autosetupMerge = repo.getConfig().getString(
					ConfigConstants.CONFIG_BRANCH_SECTION, null,
					ConfigConstants.CONFIG_KEY_AUTOSETUPMERGE);
			if (autosetupMerge == null)
				autosetupMerge = ConfigConstants.CONFIG_KEY_TRUE;
			boolean isLocalBranch = upstreamRefName
					.startsWith(Constants.R_HEADS);
			boolean isRemoteBranch = upstreamRefName
					.startsWith(Constants.R_REMOTES);
			if (!isLocalBranch && !isRemoteBranch)
				return NONE;
			boolean setupMerge = autosetupMerge
					.equals(ConfigConstants.CONFIG_KEY_ALWAYS)
					|| (isRemoteBranch && autosetupMerge
							.equals(ConfigConstants.CONFIG_KEY_TRUE));
			if (!setupMerge)
				return NONE;
			String autosetupRebase = repo.getConfig().getString(
					ConfigConstants.CONFIG_BRANCH_SECTION, null,
					ConfigConstants.CONFIG_KEY_AUTOSETUPREBASE);
			if (autosetupRebase == null)
				autosetupRebase = ConfigConstants.CONFIG_KEY_NEVER;
			boolean setupRebase = autosetupRebase
					.equals(ConfigConstants.CONFIG_KEY_ALWAYS)
					|| (autosetupRebase
							.equals(ConfigConstants.CONFIG_KEY_LOCAL) && isLocalBranch)
					|| (autosetupRebase
							.equals(ConfigConstants.CONFIG_KEY_REMOTE) && isRemoteBranch);
			if (setupRebase)
				return REBASE;
			return MERGE;
		}
	}

}
