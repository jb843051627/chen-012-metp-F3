-- metp 气象探测环境保护与气象观测数据质量控制管理 -- schema (chen-012)
-- 列名与基线实体契约（@TableName/@TableField）逐列对齐，改列必须同步实体。
-- 库：chen_012

CREATE TABLE IF NOT EXISTS t_metp_duty_task (
  id bigint NOT NULL COMMENT '主键',
  item_no varchar(64) DEFAULT NULL COMMENT '到期派工单号',
  due_at datetime DEFAULT NULL COMMENT '本项标定应当到期的日终截止时刻',
  amount decimal(12,2) DEFAULT NULL COMMENT '单条提前派发的自然日数',
  status int DEFAULT NULL COMMENT '条目情形 0待派 1已派 2派不出去',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='标定巡检到期派工条目';

CREATE TABLE IF NOT EXISTS t_metp_elem_card (
  id bigint NOT NULL COMMENT '主键',
  bill_no varchar(64) DEFAULT NULL COMMENT '观测要素挂用卡号',
  site_id int DEFAULT NULL COMMENT '所属气象台站基础档案',
  site_no varchar(64) DEFAULT NULL COMMENT '气象台站编号',
  qty decimal(12,2) DEFAULT NULL COMMENT '本卡三源合并后在册要素项数(项)',
  fine_amt decimal(12,2) DEFAULT NULL COMMENT '本年度新增要素项数(项)',
  grade_level int DEFAULT NULL COMMENT '资料保障等级档',
  status int DEFAULT NULL COMMENT '进展 0待核对 1已核对 2已定档',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='观测要素挂用建档卡';

CREATE TABLE IF NOT EXISTS t_metp_fix_card (
  id bigint NOT NULL COMMENT '主键',
  biz_no varchar(64) DEFAULT NULL COMMENT '卷号(一卷一号，受理时落下)',
  building_name varchar(128) DEFAULT NULL COMMENT '违建建筑物名称(同一座建筑物判重)',
  clue_no varchar(64) DEFAULT NULL COMMENT '来源线索编号(月初清单转入/省台重查)',
  stage int DEFAULT NULL COMMENT '当前停在的段 1调查 2告知 3处理决定 4复查 5结案(受理即建卷，受理段已办完)',
  status int DEFAULT NULL COMMENT '卡的落定 0在办 1已解除 2已归档(结案即死卷)',
  horizontal_dist decimal(12,2) DEFAULT NULL COMMENT '与仪器平台的水平距离(米) 调查段落下，进告知段即冻结',
  blocked_azimuth decimal(12,2) DEFAULT NULL COMMENT '遮挡方位角(度) 调查段落下，进告知段即冻结',
  excess_height decimal(12,2) DEFAULT NULL COMMENT '高出仪器平台(米) 调查段落下，进告知段即冻结',
  demolish_height decimal(12,2) DEFAULT NULL COMMENT '告知书写的拆除高度(米) 告知段落下，复查须与调查记录对得上',
  decision_text varchar(500) DEFAULT NULL COMMENT '处理决定正文',
  sign_round int DEFAULT '0' COMMENT '复查轮次(退回一次进一轮)',
  officer_sign_by varchar(64) DEFAULT NULL COMMENT '本轮复查-县局执法承办人点头人',
  officer_sign_time datetime DEFAULT NULL COMMENT '本轮复查-执法承办人点头时刻',
  chief_sign_by varchar(64) DEFAULT NULL COMMENT '本轮复查-台站长点头人',
  chief_sign_time datetime DEFAULT NULL COMMENT '本轮复查-台站长点头时刻',
  open_key varchar(128) DEFAULT NULL COMMENT '在办判重键=建筑物名称；结案置空(唯一索引保证同一座建筑物在办仅一卷)',
  content varchar(255) DEFAULT NULL COMMENT '查处记事(最近一笔)',
  last_action varchar(64) DEFAULT NULL COMMENT '最近一次过口动作',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  UNIQUE KEY uk_fix_card_open (open_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='探测环境违规整改事务卡';

-- 查处经过：只增不改不删的事件流水。谁递的、几点几分递的由系统落下，事后补记一概不认；
-- 退回作废旧话留底(退回一笔的 voided_seqs 指向被作废的序号)，先人记录原封不动。
CREATE TABLE IF NOT EXISTS t_metp_fix_log (
  id bigint NOT NULL COMMENT '主键',
  card_id bigint NOT NULL COMMENT '所属卷主键',
  seq int NOT NULL COMMENT '卷内序号(自1递增，卷内唯一，正倒列皆以此为准)',
  stage int NOT NULL COMMENT '本笔所属段 0受理 1调查 2告知 3处理决定 4复查 5结案',
  action varchar(32) NOT NULL COMMENT '过口动作 ACCEPT/INVESTIGATE/NOTIFY/DECIDE/REVIEW_SIGN/RETURN/CLOSE',
  horizontal_dist decimal(12,2) DEFAULT NULL COMMENT '本笔落下时的水平距离快照(米)',
  blocked_azimuth decimal(12,2) DEFAULT NULL COMMENT '本笔落下时的遮挡方位角快照(度)',
  excess_height decimal(12,2) DEFAULT NULL COMMENT '本笔落下时的高出平台快照(米)',
  demolish_height decimal(12,2) DEFAULT NULL COMMENT '告知笔的拆除高度(米)',
  sign_role varchar(16) DEFAULT NULL COMMENT '复查点头角色 OFFICER执法承办人/CHIEF台站长',
  sign_round int DEFAULT NULL COMMENT '复查点头所属轮次',
  from_stage int DEFAULT NULL COMMENT '退回笔：退回前所在段',
  to_stage int DEFAULT NULL COMMENT '退回笔：退到那一段',
  voided_seqs varchar(255) DEFAULT NULL COMMENT '退回笔：被作废旧话的卷内序号(逗号分隔，留底不抹)',
  content varchar(500) DEFAULT NULL COMMENT '本笔记事',
  operator varchar(64) NOT NULL COMMENT '递交人(系统落下，不认补记)',
  operate_time datetime NOT NULL COMMENT '递交时刻(系统落下，不认补记)',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  UNIQUE KEY uk_fix_log_seq (card_id, seq),
  KEY idx_fix_log_card (card_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='查处经过流水(只增不改不删)';

CREATE TABLE IF NOT EXISTS t_metp_move_bill (
  id bigint NOT NULL COMMENT '主键',
  bill_no varchar(64) DEFAULT NULL COMMENT '台站迁移变更核准单号',
  node_no int DEFAULT NULL COMMENT '当前所在层 0..3',
  sign_mode int DEFAULT NULL COMMENT '同层核准方式 0任一人 1两名点齐',
  need_count int DEFAULT NULL COMMENT '本层应签人数',
  sign_count int DEFAULT NULL COMMENT '本层已签人数',
  status int DEFAULT NULL COMMENT '报批情形 0在核 1已核讫 2已打回',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='台站迁移变更逐级核准单';

CREATE TABLE IF NOT EXISTS t_metp_qc_line (
  id bigint NOT NULL COMMENT '主键',
  rule_code varchar(64) DEFAULT NULL COMMENT '观测质控界限代号',
  rule_name varchar(128) DEFAULT NULL COMMENT '质控界限名称',
  th1_max decimal(12,2) DEFAULT NULL COMMENT '一档要素偏差封顶值',
  th2_max decimal(12,2) DEFAULT NULL COMMENT '二档要素偏差封顶值',
  th3_max decimal(12,2) DEFAULT NULL COMMENT '三档要素偏差封顶值',
  eff_start datetime DEFAULT NULL COMMENT '启用之日',
  eff_end datetime DEFAULT NULL COMMENT '交棒之日(不含)',
  priority int DEFAULT NULL COMMENT '并档顺位(数值越大越优先)',
  status int DEFAULT NULL COMMENT '界限的情形 0现行 1已停用',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='观测数据质量控制界限';

CREATE TABLE IF NOT EXISTS t_metp_stn_doc (
  id bigint NOT NULL COMMENT '主键',
  site_no varchar(64) DEFAULT NULL COMMENT '气象台站编号',
  site_name varchar(128) DEFAULT NULL COMMENT '台站基础档案名称',
  site_type varchar(32) DEFAULT NULL COMMENT '台站类别类',
  road_name varchar(128) DEFAULT NULL COMMENT '所在地(省—市—区县)',
  th1_max decimal(12,2) DEFAULT NULL COMMENT '在册观测要素数一档上限(项)',
  th2_max decimal(12,2) DEFAULT NULL COMMENT '二档上限(项)',
  th3_max decimal(12,2) DEFAULT NULL COMMENT '三档上限(项)',
  status int DEFAULT NULL COMMENT '档案情形 0在册 1已停测',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='气象台站基础档案';

CREATE TABLE IF NOT EXISTS t_metp_submit_row (
  id bigint NOT NULL COMMENT '主键',
  batch_no varchar(64) DEFAULT NULL COMMENT '观测数据报送册号',
  row_no int DEFAULT NULL COMMENT '原册内行次',
  item_code varchar(64) DEFAULT NULL COMMENT '时次要素代号',
  qty decimal(12,2) DEFAULT NULL COMMENT '本行填报记录条数(条)',
  status int DEFAULT NULL COMMENT '行进展 0待销 1已转存 2挂驳回',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='正点观测数据核收转存行';

-- 初始档案数据（id=1 启用 / id=2 停用）
-- 两条初始档案：id=0 在册、id=1 已停测。
-- 在册观测要素项数上限取 12/40/120 项（等于上限算高一档）。
INSERT IGNORE INTO t_metp_stn_doc (id, site_no, site_name, site_type, road_name, th1_max, th2_max, th3_max, status, del_flag, create_by, create_time)
VALUES (0, 'TQ00', '江南省临川国家基本气象站基础档案', '国家基本气象站', '江南省—临川市—城东区', 12.00, 40.00, 120.00, 0, 0, 'seed', NOW()),
       (1, 'TQ01', '旧临川市西郊气象观测站基础档案（已停测）', '一般气象站', '江南省—临川市—城西区', 12.00, 40.00, 120.00, 1, 0, 'seed', NOW());

