package com.pajk.wgit.web;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pajk.wgit.core.service.Content;
import com.pajk.wgit.core.service.GitService;
import com.pajk.wgit.core.service.Result;

public class WgitServlet extends HttpServlet {

	private static Logger logger = LoggerFactory.getLogger(WgitServlet.class);

	private static final long serialVersionUID = -7580238338264068216L;

	private String encode = "UTF-8";

	@Override
	public void init(ServletConfig config) throws ServletException {
		String encode = config.getInitParameter("encode");
		if (encode != null)
			this.encode = encode;

	}

	@Override
	protected void service(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		response.setCharacterEncoding(encode);
		response.setContentType("application/json; charset=utf-8");
		try {
			Content content = processorParameter(request);
			if (content.get("command") == null) {
				throw new RuntimeException("command is null");
			}
			Result result = GitService.getInstance().doCommand(content);
			response(response, result);
		} catch (Throwable th) {
			logger.debug(th.getMessage(), th);
			Result result = new Result();
			result.setResultCode("001");
			result.setMessage(th.getMessage());
			response(response, result);
		}
	}

	private Content processorParameter(HttpServletRequest request) {
		Content context = new Content();
		context.putAll(request.getParameterMap());
		return context;
	}

	private void response(HttpServletResponse response, Result result)
			throws IOException {
		response.getWriter().write(result.toJson());
	}

}
