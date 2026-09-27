package com.fc.v2.common.exception;

/**
 * 查处卷宗流转被拒：跳段、重复递交、死卷再办、复查人数未点齐、
 * 拆除高度对不上未退回等，一律在服务层挡下，接口回话说了算。
 *
 * @author fuce
 * @date 2026-09-26
 */
public class FixFlowException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public FixFlowException(String message) {
        super(message);
    }
}
