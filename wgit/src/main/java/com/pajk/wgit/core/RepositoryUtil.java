/*******************************************************************************
 * Copyright (c) 2010, 2014 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *******************************************************************************/
package com.pajk.wgit.core;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

import org.eclipse.core.internal.runtime.Activator;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.CheckoutEntry;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.ReflogEntry;
import org.eclipse.jgit.lib.ReflogReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryCache.FileKey;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevObject;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevTag;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.FileTreeIterator;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.WorkingTreeIterator;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.util.StringUtils;

import com.pajk.wgit.core.internal.CoreText;

/**
 * Utility class for handling Repositories in the UI.
 */
public class RepositoryUtil {

	/**
	 * The preferences to store the directories known to the Git Repositories
	 * view
	 */
	public static final String PREFS_DIRECTORIES = "GitRepositoriesView.GitDirectories"; //$NON-NLS-1$

	private final Map<String, Map<String, String>> commitMappingCache = new HashMap<String, Map<String, String>>();

	private final Map<String, String> repositoryNameCache = new HashMap<String, String>();

	private final Map<String, String> prefs = new HashMap<String, String>();

	/**
	 * Clients should obtain an instance from {@link Activator}
	 */
	RepositoryUtil() {
		// nothing
	}

	/**
	 * Used by {@link Activator}
	 */
	void dispose() {
		commitMappingCache.clear();
		repositoryNameCache.clear();
	}

	/**
	 * Tries to map a commit to a symbolic reference.
	 * <p>
	 * This value will be cached for the given commit ID unless refresh is
	 * specified. The return value will be the full name, e.g.
	 * "refs/remotes/someBranch", "refs/tags/v.1.0"
	 * <p>
	 * Since this mapping is not unique, the following precedence rules are
	 * used:
	 * <ul>
	 * <li>Tags take precedence over branches</li>
	 * <li>Local branches take preference over remote branches</li>
	 * <li>Newer references take precedence over older ones where time stamps
	 * are available. Use commiter time stamp from commit if no stamp can be
	 * found on the tag</li>
	 * <li>If there are still ambiguities, the reference name with the highest
	 * lexicographic value will be returned</li>
	 * </ul>
	 * 
	 * @param repository
	 *            the {@link Repository}
	 * @param commitId
	 *            a commit
	 * @param refresh
	 *            if true, the cache will be invalidated
	 * @return the symbolic reference, or <code>null</code> if no such reference
	 *         can be found
	 */
	public String mapCommitToRef(Repository repository, String commitId,
			boolean refresh) {
		synchronized (commitMappingCache) {

			if (!ObjectId.isId(commitId)) {
				return null;
			}

			try {
				ReflogReader reflogReader = repository
						.getReflogReader(Constants.HEAD);
				if (reflogReader != null) {
					List<ReflogEntry> lastEntry = reflogReader
							.getReverseEntries();
					for (ReflogEntry entry : lastEntry) {
						if (entry.getNewId().name().equals(commitId)) {
							CheckoutEntry checkoutEntry = entry.parseCheckout();
							if (checkoutEntry != null) {
								Ref ref = repository.getRef(checkoutEntry
										.getToBranch());
								if (ref != null) {
									if (ref.getObjectId().getName()
											.equals(commitId))
										return checkoutEntry.getToBranch();
									ref = repository.peel(ref);
								}
								if (ref != null) {
									ObjectId id = ref.getPeeledObjectId();
									if (id != null
											&& id.getName().equals(commitId))
										return checkoutEntry.getToBranch();
								}
							}
						}
					}
				}
			} catch (IOException e) {
				// ignore here
			}

			Map<String, String> cacheEntry = commitMappingCache.get(repository
					.getDirectory().toString());
			if (!refresh && cacheEntry != null
					&& cacheEntry.containsKey(commitId)) {
				// this may be null in fact
				return cacheEntry.get(commitId);
			}
			if (cacheEntry == null) {
				cacheEntry = new HashMap<String, String>();
				commitMappingCache.put(repository.getDirectory().getPath(),
						cacheEntry);
			} else {
				cacheEntry.clear();
			}

			Map<String, Date> tagMap = new HashMap<String, Date>();
			try {
				RevWalk rw = new RevWalk(repository);
				Map<String, Ref> tags = repository.getRefDatabase().getRefs(
						Constants.R_TAGS);
				for (Ref tagRef : tags.values()) {
					RevObject any = rw.parseAny(repository.resolve(tagRef
							.getName()));
					if (any instanceof RevTag) {
						RevTag tag = (RevTag) any;
						if (tag.getObject().name().equals(commitId)) {
							Date timestamp;
							if (tag.getTaggerIdent() != null) {
								timestamp = tag.getTaggerIdent().getWhen();
							} else {
								try {
									RevCommit commit = rw.parseCommit(tag
											.getObject());
									timestamp = commit.getCommitterIdent()
											.getWhen();
								} catch (IncorrectObjectTypeException e) {
									// not referencing a comit.
									timestamp = null;
								}
							}
							tagMap.put(tagRef.getName(), timestamp);
						}
					} else if (any instanceof RevCommit) {
						RevCommit commit = ((RevCommit) any);
						if (commit.name().equals(commitId))
							tagMap.put(tagRef.getName(), commit
									.getCommitterIdent().getWhen());
					} // else ignore here
				}
			} catch (IOException e) {
				// ignore here
			}

			String cacheValue = null;

			if (!tagMap.isEmpty()) {
				// we try to obtain the "latest" tag
				Date compareDate = new Date(0);
				for (Map.Entry<String, Date> tagEntry : tagMap.entrySet()) {
					if (tagEntry.getValue() != null
							&& tagEntry.getValue().after(compareDate)) {
						compareDate = tagEntry.getValue();
						cacheValue = tagEntry.getKey();
					}
				}
				// if we don't have time stamps, we sort
				if (cacheValue == null) {
					String compareString = ""; //$NON-NLS-1$
					for (String tagName : tagMap.keySet()) {
						if (tagName.compareTo(compareString) >= 0) {
							cacheValue = tagName;
							compareString = tagName;
						}
					}
				}
			}

			if (cacheValue == null) {
				// we didnt't find a tag, so let's look for local branches
				Set<String> branchNames = new TreeSet<String>();
				// put this into a sorted set
				try {
					Map<String, Ref> remoteBranches = repository
							.getRefDatabase().getRefs(Constants.R_HEADS);
					for (Ref branch : remoteBranches.values()) {
						if (branch.getObjectId().name().equals(commitId)) {
							branchNames.add(branch.getName());
						}
					}
				} catch (IOException e) {
					// ignore here
				}
				if (!branchNames.isEmpty()) {
					// get the last (sorted) entry
					cacheValue = branchNames.toArray(new String[branchNames
							.size()])[branchNames.size() - 1];
				}
			}

			if (cacheValue == null) {
				// last try: remote branches
				Set<String> branchNames = new TreeSet<String>();
				// put this into a sorted set
				try {
					Map<String, Ref> remoteBranches = repository
							.getRefDatabase().getRefs(Constants.R_REMOTES);
					for (Ref branch : remoteBranches.values()) {
						if (branch.getObjectId().name().equals(commitId)) {
							branchNames.add(branch.getName());
						}
					}
					if (!branchNames.isEmpty()) {
						// get the last (sorted) entry
						cacheValue = branchNames.toArray(new String[branchNames
								.size()])[branchNames.size() - 1];
					}
				} catch (IOException e) {
					// ignore here
				}
			}
			cacheEntry.put(commitId, cacheValue);
			return cacheValue;
		}
	}

	/**
	 * Return a cached UI "name" for a Repository
	 * <p>
	 * This uses the name of the working directory. In case of a bare
	 * repository, the repository directory name is used.
	 * 
	 * @param repository
	 * @return the name
	 */
	public String getRepositoryName(final Repository repository) {
		File dir;
		// Use working directory name for non-bare repositories
		if (!repository.isBare())
			dir = repository.getWorkTree();
		else
			dir = repository.getDirectory();

		if (dir == null)
			return ""; //$NON-NLS-1$

		synchronized (repositoryNameCache) {
			final String path = dir.getPath().toString();
			String name = repositoryNameCache.get(path);
			if (name != null)
				return name;
			name = dir.getName();
			repositoryNameCache.put(path, name);
			return name;
		}
	}

	/**
	 * @return the underlying preferences
	 */
	public Map<String, String> getPreferences() {
		return prefs;
	}

	private Set<String> getRepositories() {
		String dirs;
		synchronized (prefs) {
			dirs = prefs.get(PREFS_DIRECTORIES); //$NON-NLS-1$
		}
		if (dirs == null || dirs.length() == 0)
			return Collections.emptySet();
		Set<String> configuredStrings = new HashSet<String>();
		StringTokenizer tok = new StringTokenizer(dirs, File.pathSeparator);
		while (tok.hasMoreTokens())
			configuredStrings.add(tok.nextToken());
		return configuredStrings;
	}

	/**
	 * 
	 * @return the list of configured Repository paths; will be sorted
	 */
	public List<String> getConfiguredRepositories() {
		final List<String> repos = new ArrayList<String>(getRepositories());
		Collections.sort(repos);
		return repos;
	}

	private String getPath(File repositoryDir) {
		try {
			return repositoryDir.getCanonicalPath();
		} catch (IOException e) {
			return repositoryDir.getAbsolutePath();
		}
	}

	/**
	 * 
	 * @param repositoryDir
	 *            the Repository path
	 * @return <code>true</code> if the repository path was not yet configured
	 * @throws IllegalArgumentException
	 *             if the path does not "look" like a Repository
	 */
	public boolean addConfiguredRepository(File repositoryDir)
			throws IllegalArgumentException {
		synchronized (prefs) {

			if (!FileKey.isGitRepository(repositoryDir, FS.DETECTED))
				throw new IllegalArgumentException(MessageFormat.format(
						CoreText.RepositoryUtil_DirectoryIsNotGitDirectory,
						repositoryDir));

			String dirString = getPath(repositoryDir);

			List<String> dirStrings = getConfiguredRepositories();
			if (dirStrings.contains(dirString)) {
				return false;
			} else {
				Set<String> dirs = new HashSet<String>();
				dirs.addAll(dirStrings);
				dirs.add(dirString);
				saveDirs(dirs);
				return true;
			}
		}
	}

	/**
	 * @param file
	 * @return <code>true</code> if the configuration was changed by the remove
	 */
	public boolean removeDir(File file) {
		synchronized (prefs) {

			String dir = getPath(file);

			Set<String> dirStrings = new HashSet<String>();
			dirStrings.addAll(getConfiguredRepositories());
			if (dirStrings.remove(dir)) {
				saveDirs(dirStrings);
				return true;
			}
			return false;
		}
	}

	private void saveDirs(Set<String> gitDirStrings) {
		StringBuilder sb = new StringBuilder();
		for (String gitDirString : gitDirStrings) {
			sb.append(gitDirString);
			sb.append(File.pathSeparatorChar);
		}

		prefs.put(PREFS_DIRECTORIES, sb.toString());
	}

	/**
	 * Does the collection of repository returned by
	 * {@link #getConfiguredRepositories()} contain the given repository?
	 * 
	 * @param repository
	 * @return true if contains repository, false otherwise
	 */
	public boolean contains(final Repository repository) {
		return contains(getPath(repository.getDirectory()));
	}

	/**
	 * Does the collection of repository returned by
	 * {@link #getConfiguredRepositories()} contain the given repository
	 * directory?
	 * 
	 * @param repositoryDir
	 * @return true if contains repository directory, false otherwise
	 */
	public boolean contains(final String repositoryDir) {
		return getRepositories().contains(repositoryDir);
	}

	/**
	 * Get short branch text for given repository
	 * 
	 * @param repository
	 * @return short branch text
	 * @throws IOException
	 */
	public String getShortBranch(Repository repository) throws IOException {
		Ref head = repository.getRef(Constants.HEAD);
		if (head == null || head.getObjectId() == null)
			return CoreText.RepositoryUtil_noHead;

		if (head.isSymbolic())
			return repository.getBranch();

		String id = head.getObjectId().name();
		String ref = mapCommitToRef(repository, id, false);
		if (ref != null)
			return Repository.shortenRefName(ref) + ' ' + id.substring(0, 7);
		else
			return id.substring(0, 7);
	}

	/**
	 * Resolve HEAD and parse the commit. Returns null if HEAD does not exist or
	 * could not be parsed.
	 * <p>
	 * Only use this if you don't already have to work with a RevWalk.
	 * 
	 * @param repository
	 * @return the commit or null if HEAD does not exist or could not be parsed.
	 * @throws Throwable
	 * @since 2.2
	 */
	public static List<String> parseBranchCommit(Repository repository,
			String branchName) throws Throwable {
		List<String> allcommit = new ArrayList<String>();
		if (!hasCommits(repository)) {
			return null;
		}
		try {
			ObjectId branchObject;
			if (StringUtils.isEmptyOrNull(branchName)) {
				branchObject = repository.resolve(Constants.HEAD);
			} else {
				branchObject = repository.resolve(branchName);
			}

			RevWalk walk = new RevWalk(repository);
			walk.sort(RevSort.COMMIT_TIME_DESC);
			RevCommit head = walk.parseCommit(branchObject);
			walk.markStart(head);
			Iterator<RevCommit> it = walk.iterator();
			while (it.hasNext()) {
				RevCommit commit = it.next();
				allcommit.add(commit.getFullMessage());
			}
			walk.dispose();
		} catch (Throwable t) {
			throw t;
		}
		return allcommit;
	}

	public static boolean hasCommits(Repository repository) {
		if (repository != null && repository.getDirectory().exists()) {
			return (new File(repository.getDirectory(), "objects").list().length > 2)
					|| (new File(repository.getDirectory(), "objects/pack")
							.list().length > 0);
		}
		return false;
	}

	/**
	 * Checks if resource with given path is to be ignored.
	 * 
	 * @param path
	 *            Path to be checked
	 * @return true if the path matches an ignore rule or no repository mapping
	 *         could be found, false otherwise
	 * @throws IOException
	 * @since 2.3
	 */
	public static boolean isIgnored(String path) throws IOException {
		Repository repository = new FileRepository(path);
		TreeWalk walk = new TreeWalk(repository);
		try {
			walk.addTree(new FileTreeIterator(repository));
			walk.setFilter(PathFilter.create(path));
			while (walk.next()) {
				WorkingTreeIterator workingTreeIterator = walk.getTree(0,
						WorkingTreeIterator.class);
				if (walk.getPathString().equals(path))
					return workingTreeIterator.isEntryIgnored();
				if (workingTreeIterator.getEntryFileMode()
						.equals(FileMode.TREE))
					walk.enterSubtree();
			}
		} finally {
			walk.release();
		}
		return false;
	}

	/**
	 * Checks if given repository is in the 'detached HEAD' state.
	 * 
	 * @param repository
	 *            the repository to check
	 * @return <code>true</code> if the repository is in the 'detached HEAD'
	 *         state, <code>false</code> if it's not or an error occurred
	 * @since 3.2
	 */
	public static boolean isDetachedHead(Repository repository) {
		try {
			return ObjectId.isId(repository.getFullBranch());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static Set<String> getAllBranchs(Repository repository) {
		List<RefModel> localBranchs = getLocalBranches(repository, false, -1);
		List<RefModel> remoteBranchs = getRemoteBranches(repository, false, -1);
		Set<String> allBranchs = new HashSet<String>();
		for (RefModel ref : localBranchs) {
			allBranchs.add(ref.toString());
		}
		for (RefModel ref : remoteBranchs) {
			String branchName = ref.toString();
			allBranchs.add(branchName.split("/")[1]);
		}
		return allBranchs;
	}

	public static List<RefModel> getLocalBranches(Repository repository,
			boolean fullName, int maxCount) {
		return getRefs(repository, Constants.R_HEADS, fullName, maxCount);
	}

	public static List<RefModel> getRemoteBranches(Repository repository,
			boolean fullName, int maxCount) {
		return getRefs(repository, Constants.R_REMOTES, fullName, maxCount);
	}

	private static List<RefModel> getRefs(Repository repository, String refs,
			boolean fullName, int maxCount) {
		List<RefModel> list = new ArrayList<RefModel>();
		if (maxCount == 0) {
			return list;
		}
		if (!hasCommits(repository)) {
			return list;
		}
		try {
			Map<String, Ref> map = repository.getRefDatabase().getRefs(refs);
			RevWalk rw = new RevWalk(repository);
			for (Entry<String, Ref> entry : map.entrySet()) {
				Ref ref = entry.getValue();
				RevObject object = rw.parseAny(ref.getObjectId());
				String name = entry.getKey();
				if (fullName && !StringUtils.isEmptyOrNull(refs)) {
					name = refs + name;
				}
				list.add(new RefModel(name, ref, object));
			}
			rw.dispose();
			Collections.sort(list);
			Collections.reverse(list);
			if (maxCount > 0 && list.size() > maxCount) {
				list = new ArrayList<RefModel>(list.subList(0, maxCount));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return list;
	}

}
