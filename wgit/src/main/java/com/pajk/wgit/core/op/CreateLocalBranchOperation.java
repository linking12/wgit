package com.pajk.wgit.core.op;

import java.io.IOException;
import java.net.URISyntaxException;

import org.eclipse.jgit.api.CreateBranchCommand.SetupUpstreamMode;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.osgi.util.NLS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pajk.wgit.core.CoreException;
import com.pajk.wgit.core.internal.CoreText;

public class CreateLocalBranchOperation extends BaseOperation {

	private static Logger logger = LoggerFactory
			.getLogger(CreateLocalBranchOperation.class);

	private final String name;

	private final String sourceName;

	private final Ref ref;

	private final RevCommit commit;

	private final UpstreamConfig upstreamConfig;

	private URIish remoteURI;

	public URIish getRemoteURI() {
		return remoteURI;
	}

	public void setRemoteURI(URIish remoteURI) {
		this.remoteURI = remoteURI;
	}

	/**
	 * @param repository
	 * @param name
	 *            the name for the new local branch (without prefix)
	 * @param ref
	 *            the branch or tag to base the new branch upon
	 * @param config
	 *            how to do the upstream configuration
	 */
	public CreateLocalBranchOperation(Repository repository, String sourceName,
			String name, Ref ref, UpstreamConfig config) {
		super(repository);
		this.name = name;
		this.ref = ref;
		this.commit = null;
		this.upstreamConfig = config;
		this.sourceName = sourceName;
	}

	public CreateLocalBranchOperation(String remoteUrl, String sourceName,
			String name) throws IOException {
		super(remoteUrl);
		this.name = name;
		this.ref = repository.getRef(name);
		this.commit = null;
		this.upstreamConfig = UpstreamConfig.NONE;
		this.sourceName = sourceName;
		try {
			this.setRemoteURI(new URIish(remoteUrl));
		} catch (URISyntaxException e) {
		}
	}

	/**
	 * @param repository
	 * @param name
	 *            the name for the new local branch (without prefix)
	 * @param commit
	 *            a commit to base the new branch upon
	 */
	public CreateLocalBranchOperation(Repository repository, String sourceName,
			String name, RevCommit commit) {
		super(repository);
		this.sourceName = sourceName;
		this.name = name;
		this.ref = null;
		this.commit = commit;
		this.upstreamConfig = null;
	}

	public void execute() throws CoreException {

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
			String message = NLS.bind(
					CoreText.CreateLocalBranchOperation_failed, name,
					e.getMessage());
			logger.debug(message, e);
			throw new CoreException(message, e);
		}

		if (UpstreamConfig.REBASE == upstreamConfig) {
			// set "branch.<name>.rebase" to "true"
			StoredConfig config = repository.getConfig();
			config.setBoolean(ConfigConstants.CONFIG_BRANCH_SECTION, name,
					ConfigConstants.CONFIG_KEY_REBASE, true);
			try {
				config.save();
			} catch (IOException e) {
				String message = NLS
						.bind(CoreText.CreateLocalBranchOperation_RebaseBranch_failed,
								name, e.getMessage());
				logger.debug(message, e);
				throw new CoreException(message, e);
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

	@Override
	public Result run() {
		Result result = new Result();
		this.addPreExecuteTask(new PreExecuteTask() {
			@Override
			public void preExecute(Repository repository)
					throws RuntimeException {
				try {
					Ref ref = repository.getRef(sourceName);
					if (ref != null)
						new FetchOperation(repository, remoteURI, 60, false)
								.execute();
					else
						new BranchOperation(repository, sourceName).execute();
				} catch (IOException e) {
					// ignore here
				}
			}
		});
		this.addPostExecuteTask(new PostExecuteTask() {
			@Override
			public void postExecute(Repository repository)
					throws RuntimeException {
				new PushOperation(repository, false, 0).execute();
			}
		});
		try {
			this.execute();
		} catch (Exception e) {
			result.setResultCode("001");
			result.setMessage(e.getMessage());
		}
		return result;
	}
}
