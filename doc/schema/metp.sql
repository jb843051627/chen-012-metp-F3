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
  biz_no varchar(64) DEFAULT NULL COMMENT '探测环境违规整改卡号',
  stage int DEFAULT NULL COMMENT '当前关口 0..5',
  status int DEFAULT NULL COMMENT '卡的落定 0在办 1已解除 2已归档',
  content varchar(255) DEFAULT NULL COMMENT '查处记事',
  last_action varchar(64) DEFAULT NULL COMMENT '最近一次过口动作',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='探测环境违规整改事务卡';

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

-- 探测环境违法建设查处案卷（chen-012）
-- 一卷一号，六段顺序办理：受理 1 / 调查 2 / 告知 3 / 处理决定 4 / 复查 5 / 结案 6。
-- 查处经过落 t_metp_case_record，只追加、不改不删；结案即死卷。

CREATE TABLE IF NOT EXISTS t_metp_case (
  id bigint NOT NULL COMMENT '主键',
  case_no varchar(64) NOT NULL COMMENT '查处案卷号(一卷一号)',
  clue_no varchar(64) DEFAULT NULL COMMENT '违法线索清单转入编号',
  building_name varchar(255) NOT NULL COMMENT '违法建筑物名称(去重锚点)',
  stage int NOT NULL DEFAULT '1' COMMENT '案卷当前所在段 1受理 2调查 3告知 4处理决定 5复查 6结案',
  status int NOT NULL DEFAULT '0' COMMENT '案卷情形 0在办 1已结案(死卷)',
  -- 调查段技术意见：一进告知段即冻结，此后各段照录
  tech_distance decimal(12,2) DEFAULT NULL COMMENT '与仪器平台水平距离(米)',
  tech_azimuth varchar(32) DEFAULT NULL COMMENT '所挡方位角',
  tech_height decimal(12,2) DEFAULT NULL COMMENT '高出仪器平台米数',
  tech_frozen int NOT NULL DEFAULT '0' COMMENT '技术意见冻结标记 0未冻结 1进告知段后冻结',
  -- 告知段拆除意见（复查退回时在主表上换为重做值，旧值留底于 t_metp_case_record）
  demolish_height decimal(12,2) DEFAULT NULL COMMENT '告知段载明的应拆除高度(米)',
  -- 复查段：县局执法承办人与台站长各点头一次
  review_officer_sign int NOT NULL DEFAULT '0' COMMENT '执法承办人复查点头 0未点 1已点',
  review_chief_sign int NOT NULL DEFAULT '0' COMMENT '台站长复查点头 0未点 1已点',
  accept_time datetime DEFAULT NULL COMMENT '受理建卷时刻',
  close_time datetime DEFAULT NULL COMMENT '结案时刻(死卷落定)',
  del_flag int NOT NULL DEFAULT '0' COMMENT '删除标记 恒为0：案卷不许抽删',
  create_by varchar(64) DEFAULT NULL COMMENT '建卷人(系统自落)',
  create_time datetime DEFAULT NULL COMMENT '建卷时间(系统自落)',
  update_by varchar(64) DEFAULT NULL COMMENT '最近办理人(系统自落)',
  update_time datetime DEFAULT NULL COMMENT '最近办理时间(系统自落)',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  UNIQUE KEY uk_case_no (case_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='探测环境违法建设查处案卷';

-- 同一建筑物名下已有未结案卷的，不再另开第二卷：门禁行随卷建、随结案摘。
CREATE TABLE IF NOT EXISTS t_metp_case_active (
  id bigint NOT NULL COMMENT '主键',
  building_name varchar(255) NOT NULL COMMENT '违法建筑物名称',
  case_id bigint NOT NULL COMMENT '在办案卷主键',
  case_no varchar(64) NOT NULL COMMENT '在办案卷号',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_active_building (building_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='在办查处案卷去重门禁(一建筑物一在办卷)';

-- 查处经过：一卷从头到尾的每一笔。只许 INSERT，不许 UPDATE/DELETE。
-- 退回重做时旧笔 voided=1 原样留底，页面只摆 current=1 的一条，旧话不抹。
-- 复查点头的旧签名一律原封不动；退回后另起一回，由主表点头标记与 seq 先后区分新旧。
CREATE TABLE IF NOT EXISTS t_metp_case_record (
  id bigint NOT NULL COMMENT '主键',
  case_id bigint NOT NULL COMMENT '所属案卷主键',
  case_no varchar(64) NOT NULL COMMENT '案卷号',
  seq int NOT NULL COMMENT '卷内次序(逐笔递增，查处经过按此正列/倒列)',
  stage int NOT NULL COMMENT '本笔所在段 1..6',
  action varchar(32) NOT NULL COMMENT '动作 SUBMIT递交 SIGN点头 RETURN退回重做 CLOSE结案',
  current_flag int NOT NULL DEFAULT '1' COMMENT '该段当前生效记载 1页面摆出 0已被重做替换',
  voided int NOT NULL DEFAULT '0' COMMENT '作废留底标记 0有效 1退回重做前的旧话(留底不抹)',
  operator varchar(64) NOT NULL COMMENT '办理人(系统自落，不许补记)',
  operate_time datetime(3) NOT NULL COMMENT '办理时刻(系统自落，精确到毫秒定先后)',
  sign_role varchar(32) DEFAULT NULL COMMENT '点头角色 OFFICER执法承办人 CHIEF台站长',
  tech_distance decimal(12,2) DEFAULT NULL COMMENT '本笔落定时的水平距离(米)',
  tech_azimuth varchar(32) DEFAULT NULL COMMENT '本笔落定时的所挡方位角',
  tech_height decimal(12,2) DEFAULT NULL COMMENT '本笔落定时的高出平台米数',
  demolish_height decimal(12,2) DEFAULT NULL COMMENT '本笔落定时的应拆除高度(米)',
  content varchar(1000) DEFAULT NULL COMMENT '该段记载内容',
  PRIMARY KEY (id),
  KEY idx_case_seq (case_id, seq)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='查处经过(只追加留底账)';

-- 初始档案数据（id=1 启用 / id=2 停用）
-- 两条初始档案：id=0 在册、id=1 已停测。
-- 在册观测要素项数上限取 12/40/120 项（等于上限算高一档）。
INSERT IGNORE INTO t_metp_stn_doc (id, site_no, site_name, site_type, road_name, th1_max, th2_max, th3_max, status, del_flag, create_by, create_time)
VALUES (0, 'TQ00', '江南省临川国家基本气象站基础档案', '国家基本气象站', '江南省—临川市—城东区', 12.00, 40.00, 120.00, 0, 0, 'seed', NOW()),
       (1, 'TQ01', '旧临川市西郊气象观测站基础档案（已停测）', '一般气象站', '江南省—临川市—城西区', 12.00, 40.00, 120.00, 1, 0, 'seed', NOW());

