# 接口与部署手册

## 接口

| 方法 | 路径 | 输入与结果 |
| --- | --- | --- |
| GET | /api/health | 200，status=UP，只检查 Web 存活 |
| POST | /api/tickets | title 1..120 字，description 1..4000 字，返回 201 |
| GET | /api/tickets?page=1&size=10 | page 1..10000，size 1..100，返回 items/total |
| GET | /api/tickets/{id} | 单条详情，不存在返回 404 |
| PATCH | /api/tickets/{id}/status | status、version；跳转错误或版本冲突返回 409 |
| POST | /api/tickets/{id}/suggestion | 返回 mode 和 suggestion；模型失败返回 502 |

请求正文均为 JSON。创建示例：`{"title":"查询超时","description":"数据库慢查询"}`。
状态更新示例：`{"status":"IN_PROGRESS","version":0}`。

## 数据和事务

tickets 的 id 为自增主键，version 在更新时递增。更新 SQL 将 id 和 version 放入 WHERE，通过受影响行数判断冲突。状态变更在事务内执行。列表稳定按 id 倒序；当前使用 OFFSET，深分页与计数开销尚未做压力测试。
H2 默认仅本地文件，MySQL 迁移需调整驱动、建表语法和集成测试，不能直接假设兼容。

## 本地运维

1. `mvn verify` 验证后 `mvn package`，运行 `java -jar target/ai-ticket-workbench-1.0.0.jar`。
2. `curl --fail http://127.0.0.1:8080/api/health` 检查进程；进一步创建并查询工单，确认数据库可用。
3. 停机后备份 data 目录；回滚应用包前检查数据库结构兼容性。本项目启动只执行 IF NOT EXISTS，不做 schema 迁移。
4. 端口占用：使用 `--server.port=8081`；H2 文件锁定：确认没有两个进程访问同一文件；502：检查模型配置、配额和网络。

部署前需补齐身份认证、授权、审计、敏感信息处理和调用成本限制。默认只监听 127.0.0.1。容器内部监听所有地址，但 compose 对外仅映射本机。

## 需求与验收

支持人员在页面创建工单，系统返回 ID；处理人员依次推进状态；另一个客户端持旧版本提交时收到 409；建议接口明确标记规则或 live 模式。测试按这些用户行为组织，避免仅镜像方法实现。

技术依据：[Spring Boot](https://docs.spring.io/spring-boot/3.5/system-requirements.html)、[MyBatis starter 兼容表](https://mybatis.org/spring-boot-starter/mybatis-spring-boot-autoconfigure/)。依赖版本固定以便复现，并非自动选择最新版本。
