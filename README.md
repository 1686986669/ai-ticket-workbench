# AI 工单工作台

面向 Java 后端与 AI 集成岗位的学习作品：提交故障工单、分页查询、状态流转、并发版本校验和辅助排查建议。包含原生 HTML/CSS/JavaScript 页面，从接口、数据库到页面形成完整演示。

## 启动

JDK 17+、Maven 3.6.3+：

```bash
mvn verify
mvn spring-boot:run
```

访问 http://127.0.0.1:8080 。默认 H2 文件库位于 `data/`，重启保留工单。
Docker 可选：`docker compose up --build`，仅绑定本机 8080，使用命名卷持久化。

## 功能与设计

- Controller 接收并校验参数，Service 处理分类和状态规则，MyBatis Mapper 访问 H2。
- 状态只允许 `OPEN -> IN_PROGRESS -> RESOLVED`；更新要求传入当前 version，过期版本返回 409。
- 使用参数绑定和有上限分页；页面用 `textContent` 渲染输入，避免把工单当 HTML。
- 辅助建议默认为 `demo-rule`，是规则输出，**没有调用大模型**。
- `AI_MODE=live` 配合 `LLM_API_KEY`、`LLM_MODEL` 和可选 HTTPS `LLM_BASE_URL` 启用模型；Key 只在后端读取，HTTP 请求有连接和响应超时。

不使用 Spring AI 是为了清楚展示原始模型 HTTP 集成和异常处理；也没有实现 Spring Cloud、MySQL 或大数据技术，不应据此声称掌握这些栈。

## 验证

`mvn verify` 包含真实 Spring Boot + H2 + MyBatis + MockMvc 集成测试：状态流转、过期版本、无效参数、分页边界、404、规则建议、SQL 文本和首页。测试不调用真实模型。`scripts/smoke.sh` 对启动后的服务执行健康检查和创建/查询工单。

## 适用边界

这是本地学习演示，没有登录、租户隔离、速率限制和生产审计。不要直接开放到公网或录入真实工单。分类是关键词规则，模型建议不会自动改变状态。真实调用会把工单标题与描述发给配置的模型服务并可能计费。Docker 路径和真实模型应在目标环境单独验证。

详见 [接口与部署文档](docs/api-and-operations.md)。
