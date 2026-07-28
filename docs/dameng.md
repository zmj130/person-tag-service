# 达梦检查与部署

不需要在项目文档中提供数据库地址、用户名或密码。已完成以下只读检查：

```sql
SELECT * FROM V$VERSION;

SELECT USER FROM DUAL;

SELECT PARA_NAME, PARA_VALUE
FROM V$DM_INI
WHERE PARA_NAME IN ('COMPATIBLE_MODE', 'CASE_SENSITIVE');
```

## 已确认的实例信息

- 数据库：DM Database Server 64 V8
- 内部版本：`0x7000d`
- 构建标识：`03134284458-20251113-301923-20178`
- 当前查询账号：`DATA_CLOUD`，仅代表执行检查时使用的账号，不作为本项目业务账号
- `COMPATIBLE_MODE=7`：部分兼容 PostgreSQL
- 项目业务用户名和密码：部署前另行创建，通过环境变量注入，不写入仓库

模式 7 有利于当前项目使用 `||` 字符串拼接等通用/PostgreSQL 风格语法，但只是“部分兼容”，不能把达梦直接视为 PostgreSQL。初始化脚本仍需在目标实例实际执行验证。

当前项目参考本机其他项目使用 `com.dameng:DmJdbcDriver18:8.1.3.140`。首次联库先使用该版本执行连接、建表和 CRUD 测试；同时向数据库管理员获取该实例安装介质自带的 JDK 8 JDBC 驱动版本进行对照。不能仅因为能够建立连接就忽略驱动与服务端版本差异。

## 执行前检查

- 使用独立用户/模式，确认表名 `PT_*` 没有冲突。
- 数据库和客户端脚本编码使用 UTF-8。
- 新库依次执行 `V1__init.sql`、`V2__indicator_rule_profile.sql`；演示数据和清理脚本统一放在 `db/dm/demo`，生产环境不要执行。
- 将数据库密码、远程令牌和调度令牌放入部署环境变量，不写入仓库。
- 执行初始化后检查唯一约束和索引是否创建成功。

## Windows JDK 8 编码注意

本机 JDK 8 默认平台编码为 GBK。测试配置已经显式设置 `spring.sql.init.encoding=UTF-8`，否则中文规则可能在测试初始化时乱码。生产 SQL 应由明确使用 UTF-8 的达梦客户端执行；应用源码和 Maven 资源编码也统一为 UTF-8。
