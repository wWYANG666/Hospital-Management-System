# 数据库初始化与迁移

所有手工 SQL 集中在 `database/`。应用仅在医务端执行已有的 Java 启动迁移；患者端关闭初始化。SQL 不再作为静态资源打入 JAR。

## 新环境

在项目根目录启动 MySQL 客户端：

```powershell
mysql --default-character-set=utf8mb4 -u root -p
```

然后执行：

```sql
SOURCE database/init.sql;
```

入口按依赖顺序创建基础表、处方、库存入库、医嘱和病历，再补齐药品字段。脚本中不导入用户、病历或密码。随后设置数据库环境变量并先启动医务端，执行 `BusinessSchemaMigration` 和报告缴费迁移；再启动患者端。

如需演示科室、药品和检查项目，可选执行：

```sql
SOURCE database/seed-department-items.sql;
```

## 已有数据库

整理目录没有改动现有数据库。保留原数据库和账号，缺少字段时按需选用以下脚本；不要批量执行目录内全部文件。

| 文件 | 用途 |
| --- | --- |
| `migrations/upgrade-medicine-safe.sql` | 检查并补齐药品采购价、库存预警、厂家等字段 |
| `migrations/upgrade-prescription-safe.sql` | 检查并补齐处方付款及取药字段 |
| `migrations/add-medical-record-columns-safe.sql` | 补齐病历住院关联、病情更新字段和外键 |
| `migrations/upgrade-hospitalization-appointment-id-safe.sql` | 补齐住院与挂号关联及外键 |
| `migrations/upgrade-schedule-expert.sql` | 旧排班表增加普通/专家门诊类型；只执行一次 |
| `migrations/upgrade-medical-order-quantity.sql` | 旧医嘱数量字段；通常已由 Java 启动迁移补齐 |
| `migrations/legacy-upgrade-hospitalization.sql` | 历史住院扩展脚本，不保证重复执行；新环境使用 `init.sql` |

首次建表文件仍按业务模块拆分，便于检查依赖关系，使用者只需运行 `init.sql`。迁移与首次建表承担不同职责，保留迁移是为了兼容旧库。

## 自动迁移职责

- `BusinessSchemaMigration`：住院申请、结算、医嘱数量和费用、报告缴费字段，以及付款记录表。
- `ReportPaidColumnMigration`：保留原有待处理报告缴费状态回填规则。
- `EmptyTableAutoIncrementReset`：保留原有空表序列维护逻辑。

手工旧版 `upgrade-report-paid.sql` 的历史回填规则与 Java 逻辑不同，因此已移出主项目存入备份，未把它当成等价脚本自动执行。
