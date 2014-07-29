package com.pajk.wgit.core.service;

import java.io.IOException;
import java.net.URISyntaxException;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.pajk.wgit.core.op.CloneOperation;
import com.pajk.wgit.core.op.CreateLocalBranchOperation;
import com.pajk.wgit.core.op.IWGitOperation.Result;

@RequestMapping("/restful")
@Controller
public class RestGitService {

	@RequestMapping(value = "/create/{remoteUrl}/{fromBranch}/{Branch}", method = RequestMethod.GET)
	public ModelAndView create(@PathVariable String remoteUrl,
			@PathVariable String fromBranch, @PathVariable String Branch) {
		Result result = null;
		try {
			CreateLocalBranchOperation create = new CreateLocalBranchOperation(
					remoteUrl, fromBranch, Branch);
			result = create.run();
		} catch (IOException e) {
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
		} catch (URISyntaxException e) {
			result = new Result();
			result.setResultCode("001");
			result.setMessage(e.getMessage());
		}
		ModelAndView model = new ModelAndView("jsonView");
		model.addObject(result);
		return model;
	}

}
