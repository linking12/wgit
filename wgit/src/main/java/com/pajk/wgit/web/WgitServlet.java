package com.pajk.wgit.web;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class WgitServlet extends HttpServlet {

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
	}
}
