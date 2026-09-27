package com.fc.v2.common.exception;

/**
 * 案卷流转违章：跳段、同段重复递交、死卷再记事、技术意见冻结后擅改、复查缺签等。
 * 由全局异常解析器转成接口回话（AjaxResult），章程全部落在服务层，接口回话说了算。
 *
 * @author fuce
 * @date 2026-09-27
 */
public class CaseFlowException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public CaseFlowException(String message) {
        super(message);
    }
}
