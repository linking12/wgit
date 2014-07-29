package com.pajk.wgit.core.service;

import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.pajk.wgit.core.RepositoryUtil;
import com.pajk.wgit.core.op.BaseOperation;
import com.pajk.wgit.core.op.BranchOperation;
import com.pajk.wgit.core.op.CloneOperation;
import com.pajk.wgit.core.op.CreateLocalBranchOperation;
import com.pajk.wgit.core.op.FetchOperation;
import com.pajk.wgit.core.op.IWGitOperation.PreExecuteTask;
import com.pajk.wgit.core.op.IWGitOperation.Result;
import com.pajk.wgit.core.op.MergeOperation;
import com.pajk.wgit.core.op.PushOperation;

@RequestMapping("/restful")
@Controller
public class RestGitService {

	@RequestMapping(value = "/create", method = RequestMethod.GET)
	public ModelAndView create(
			@RequestParam(value = "remoteUrl", required = true) String remoteUrl,
			@RequestParam(value = "from", required = true) String fromBranch,
			@RequestParam(value = "to", required = true) String toBranch) {
		Result result = null;
		try {
			CreateLocalBranchOperation create = new CreateLocalBranchOperation(
					remoteUrl, fromBranch, toBranch);
			result = create.run();
		} catch (Throwable e) {
			result = new Result();
			result.setResultCode("001");
			result.setMessage(e.getMessage());
		}
		ModelAndView model = new ModelAndView("jsonView");
		model.addObject(result);
		return model;
	}

	@RequestMapping(value = "/clone/{Branch}", method = RequestMethod.GET)
	public ModelAndView clone(
			@RequestParam(value = "remoteUrl", required = true) String remoteUrl,
			@PathVariable String Branch) {
		Result result = null;
		try {
			CloneOperation clone = new CloneOperation(remoteUrl, Branch);
			result = clone.run();
		} catch (Throwable e) {
			result = new Result();
			result.setResultCode("001");
			result.setMessage(e.getMessage());
		}
		ModelAndView model = new ModelAndView("jsonView");
		model.addObject(result);
		return model;
	}

	@RequestMapping(value = "/fetch", method = RequestMethod.GET)
	public ModelAndView fetch(
			@RequestParam(value = "remoteUrl", required = true) String remoteUrl) {
		Result result = null;
		try {
			FetchOperation fetch = new FetchOperation(remoteUrl, 60, false);
			result = fetch.run();
		} catch (Throwable e) {
			result = new Result();
			result.setResultCode("001");
			result.setMessage(e.getMessage());
		}
		ModelAndView model = new ModelAndView("jsonView");
		model.addObject(result);
		return model;
	}

	@RequestMapping(value = "/push", method = RequestMethod.GET)
	public ModelAndView push(
			@RequestParam(value = "remoteUrl", required = true) String remoteUrl,
			@RequestParam(value = "branch", required = true) String branchName) {
		Result result = null;
		try {
			PushOperation push = new PushOperation(remoteUrl, false,
					branchName, 0);
			result = push.run();
		} catch (Throwable e) {
			result = new Result();
			result.setResultCode("001");
			result.setMessage(e.getMessage());
		}
		ModelAndView model = new ModelAndView("jsonView");
		model.addObject(result);
		return model;
	}

	@RequestMapping(value = "/merger", method = RequestMethod.GET)
	public ModelAndView merger(
			@RequestParam(value = "remoteUrl", required = true) final String remoteUrl,
			@RequestParam(value = "from", required = true) final String source,
			@RequestParam(value = "to", required = true) final String target) {
		Result result = null;
		try {
			MergeOperation merge = new MergeOperation(remoteUrl, source);
			merge.addPreExecuteTask(new PreExecuteTask() {
				public void preExecute(Repository repository)
						throws RuntimeException {
					try {
						if (repository.getRef(source) == null)
							new BranchOperation(remoteUrl, source).execute();
						else
							new FetchOperation(remoteUrl, 60, false);
						if (repository.getRef(target) == null)
							new BranchOperation(remoteUrl, target).execute();
						else
							new FetchOperation(remoteUrl, 60, false);
					} catch (Exception e) {
						throw new RuntimeException(e);
					} finally {
						try {
							if (!repository.getBranch().equals(target))
								new BranchOperation(remoteUrl, target)
										.execute();
						} catch (Exception e) {
							throw new RuntimeException(e);
						}

					}
				}
			});
			result = merge.run();
		} catch (Throwable e) {
			result = new Result();
			result.setResultCode("001");
			result.setMessage(e.getMessage());
		}
		ModelAndView model = new ModelAndView("jsonView");
		model.addObject(result);
		return model;
	}

	@RequestMapping(value = "/getcommit", method = RequestMethod.GET)
	public ModelAndView getcommit(
			@RequestParam(value = "remoteUrl", required = true) String remoteUrl,
			@RequestParam(value = "branch", required = true) String branchName) {
		Result result = null;
		try {
			Repository repository = new FileRepository(
					BaseOperation.getGirdir(remoteUrl) + Constants.DOT_GIT);
			RevCommit commit = RepositoryUtil.parseBranchCommit(repository,
					branchName);
			result = new Result();
			result.setMessage(commit.getName());
		} catch (Throwable e) {
			result = new Result();
			result.setResultCode("001");
			result.setMessage(e.getMessage());
		}
		ModelAndView model = new ModelAndView("jsonView");
		model.addObject(result);
		return model;
	}

}
