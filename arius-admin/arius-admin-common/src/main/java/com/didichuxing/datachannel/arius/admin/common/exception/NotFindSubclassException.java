package com.didichuxing.datachannel.arius.admin.common.exception;

import com.didichuxing.datachannel.arius.admin.client.constant.result.ResultType;

/**
 * @author linyunan
 * @date 2021-04-25
 */
public class NotFindSubclassException extends BaseRunTimeException {

	public NotFindSubclassException(String message) {
		super(message, ResultType.NO_FIND_SUB_CLASS);
	}
}
