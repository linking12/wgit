package com.pajk.wgit.core.internal;

import org.eclipse.osgi.util.NLS;

public class CoreText extends NLS {

	private static final String BUNDLE_NAME = "com.pajk.wgit.core.internal.coretext";

	public static String RepositoryUtil_DirectoryIsNotGitDirectory;

	public static String RepositoryUtil_noHead;

	public static String BranchOperation_performingBranch;

	public static String CloneOperation_failed;

	public static String CloneOperation_failed_cleanup;

	public static String CreateLocalBranchOperation_failed;

	public static String CreateLocalBranchOperation_RebaseBranch_failed;

	public static String OperationAlreadyExecuted;

	public static String FetchOperation_failed;

	public static String CommonOperation_InternalError;

	public static String MergeOperation_MergeFailedNoHead;

	public static String MergeOperation_MergeFailedRefUpdate;

	public static String PullOperation_DetachedHeadMessage;

	public static String PullOperation_PullNotConfiguredMessage;

	public static String PullOperation_Failed;

	public static String RemoteRefUpdateCantBeReused;

	public static String PushOperation_failed;

	public static String OperationNotYetExecuted;

	public static String GetPrevision_failed;

	public static String ResetOperation_failed;

	public static String CommitOperation_errorParsingPersonIdent;

	static {
		initializeMessages(BUNDLE_NAME, CoreText.class);
	}

}
