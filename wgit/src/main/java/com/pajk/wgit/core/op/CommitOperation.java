package com.pajk.wgit.core.op;

import java.io.IOException;
import java.util.Date;
import java.util.TimeZone;

import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.util.RawParseUtils;
import org.eclipse.osgi.util.NLS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pajk.wgit.core.CoreException;
import com.pajk.wgit.core.internal.CoreText;

public class CommitOperation extends BaseOperation {

	private static Logger logger = LoggerFactory
			.getLogger(CommitOperation.class);

	private String author;

	private String committer;

	private String message;

	RevCommit commit = null;

	public CommitOperation(Repository repository, String author,
			String committer, String message) throws CoreException {
		super(repository);
		this.author = author;
		this.committer = committer;
		this.message = message;
	}

	public void execute() throws CoreException {
		Git git = new Git(repository);
		try {
			CommitCommand commitCommand = git.commit();
			setAuthorAndCommitter(commitCommand);
			commit = commitCommand.setAll(true).setMessage(message)
					.setInsertChangeId(true).call();
		} catch (JGitInternalException e) {
			logger.debug(CoreText.CommonOperation_InternalError, e);
			throw new CoreException(CoreText.CommonOperation_InternalError, e);
		} catch (GitAPIException e) {
			logger.debug(e.getLocalizedMessage(), e);
			throw new CoreException(e.getLocalizedMessage(), e);
		}

	}

	/**
	 * @return the newly created commit if committing was successful, null
	 *         otherwise.
	 */
	public RevCommit getCommit() {
		return commit;
	}

	private void setAuthorAndCommitter(CommitCommand commitCommand)
			throws CoreException {
		final Date commitDate = new Date();
		final TimeZone timeZone = TimeZone.getDefault();

		final PersonIdent enteredAuthor = RawParseUtils
				.parsePersonIdent(author);
		final PersonIdent enteredCommitter = RawParseUtils
				.parsePersonIdent(committer);
		if (enteredAuthor == null)
			throw new CoreException(NLS.bind(
					CoreText.CommitOperation_errorParsingPersonIdent, author));
		if (enteredCommitter == null)
			throw new CoreException(
					NLS.bind(CoreText.CommitOperation_errorParsingPersonIdent,
							committer));

		PersonIdent authorIdent;
		if (repository.getRepositoryState().equals(
				RepositoryState.CHERRY_PICKING_RESOLVED)) {
			RevWalk rw = new RevWalk(repository);
			try {
				ObjectId cherryPickHead = repository.readCherryPickHead();
				authorIdent = rw.parseCommit(cherryPickHead).getAuthorIdent();
			} catch (IOException e) {
				throw new IllegalStateException(e);
			} finally {
				rw.release();
			}
		} else {
			authorIdent = new PersonIdent(enteredAuthor, commitDate, timeZone);
		}

		final PersonIdent committerIdent = new PersonIdent(enteredCommitter,
				commitDate, timeZone);

		commitCommand.setAuthor(authorIdent);
		commitCommand.setCommitter(committerIdent);
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
