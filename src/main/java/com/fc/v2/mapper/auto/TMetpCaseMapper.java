package com.fc.v2.mapper.auto;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fc.v2.model.auto.TMetpCase;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 探测环境违法建设查处案卷数据层
 *
 * @author fuce
 * @date 2026-09-27
 */
public interface TMetpCaseMapper extends BaseMapper<TMetpCase> {

    /**
     * 行锁取卷：流转每一笔都先锁行再判段，防止并发下跳段、重复点头与同建筑开二卷。
     */
    @Select("SELECT * FROM t_metp_case WHERE id = #{id} FOR UPDATE")
    TMetpCase selectByIdForUpdate(@Param("id") Long id);
}
