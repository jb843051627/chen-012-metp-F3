package com.fc.v2.mapper.auto;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fc.v2.model.auto.TMetpCaseRecord;

/**
 * 查处经过数据层。
 * 只追加：业务层只调 insert，不提供 update/delete；旧话以 voided 留底，绝不抹除。
 *
 * @author fuce
 * @date 2026-09-27
 */
public interface TMetpCaseRecordMapper extends BaseMapper<TMetpCaseRecord> {

}
