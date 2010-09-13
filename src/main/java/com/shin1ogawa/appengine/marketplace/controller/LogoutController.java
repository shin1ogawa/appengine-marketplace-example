package com.shin1ogawa.appengine.marketplace.controller;

import org.slim3.controller.Controller;
import org.slim3.controller.Navigation;

import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;

/**
 * @author shin1ogawa
 */
public class LogoutController extends Controller {

	@Override
	protected Navigation run() throws Exception {
		UserService us = UserServiceFactory.getUserService();
		if (us.isUserLoggedIn()) {
			return redirect(us.createLogoutURL("/"));
		}
		return redirect("/");
	}
}
