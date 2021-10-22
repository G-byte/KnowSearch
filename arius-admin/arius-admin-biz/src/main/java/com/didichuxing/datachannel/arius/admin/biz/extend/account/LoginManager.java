package com.didichuxing.datachannel.arius.admin.biz.extend.account;

import com.didichuxing.datachannel.arius.admin.client.bean.common.Result;
import com.didichuxing.datachannel.arius.admin.client.bean.dto.account.LoginDTO;
import com.didichuxing.datachannel.arius.admin.client.bean.dto.user.AriusUserInfoDTO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Created by linyunan on 2021-06-28
 */
public interface LoginManager {

	/**
	 * 登录校验
	 */
	Result loginAuthenticateAndGetUserInfo(HttpServletRequest request, HttpServletResponse response, LoginDTO loginDTO);

	/**
	 * 登出
	 */
	Result logout(HttpServletRequest request, HttpServletResponse response);

	/**
	 * 注册
	 * @param userInfoDTO  用户信息
	 * @param appId        用户所绑定项目
	 */
	Result<Long> register(AriusUserInfoDTO userInfoDTO, Integer appId);
}
