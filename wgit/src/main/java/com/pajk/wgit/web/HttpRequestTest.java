package com.pajk.wgit.web;

import com.pajk.wgit.core.HttpInvoker;

public class HttpRequestTest {

	public static void main(String[] args) {
		RequestEntity request = new RequestEntity();
		request.setRemoteurl("git@10.0.128.104:root/totoro.git");
		request.setCommand("merge");
		request.setSource("test");
		request.setTarget("develop");
		String message = HttpInvoker.getInstance().sendGetRequest(
				"http://localhost:8080/wgit/api", request);
		System.out.println(message);

	}

}
