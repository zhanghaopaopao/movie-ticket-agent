# Movie Ticket Agent Backend

Spring Boot 3.5 + JDK 21 + MyBatis-Plus 多模块后端项目骨架。

## Modules

```
┌──────────┐
│  server   │  Spring Boot 入口，控制器，配置
├──────────┤
│   pojo    │  数据对象定义（entity、dto、vo）
├──────────┤
│  common   │  通用工具、Result 响应包装、异常、常量
└──────────┘
```

依赖方向：`server → pojo → common`（server 依赖 pojo，pojo 依赖 common，common 无模块依赖）

- **common**：共享模块边界，提供 `Result<T>` 通用响应包装、`ResultCode` 状态码常量、`BusinessException` 业务异常、`CommonConstants` 通用常量。
- **pojo**：预留 `entity`、`dto`、`vo` 子包，实体类使用 MyBatis-Plus 注解映射数据库表。
- **server**：Spring Boot 启动入口，MyBatis-Plus 配置，Web 层（控制器、全局异常处理等）。

## Requirements

- JDK 21
- Maven Wrapper（`mvnw` / `mvnw.cmd`）

## Run

骨架阶段排除了数据源自动配置，无需数据库即可启动：

```powershell
$env:JAVA_HOME='C:\Path\To\JDK-21'
.\mvnw.cmd -pl server -am spring-boot:run
```

服务监听 `http://localhost:8080`，可通过 Actuator 端点查看健康状态：

```powershell
curl http://localhost:8080/actuator/health
```

## Verify

```powershell
$env:JAVA_HOME='C:\Path\To\JDK-21'
.\mvnw.cmd clean verify
```

## Roadmap

数据库连接、实体、Mapper、控制器、Service、数据迁移、种子数据、认证鉴权、Agent 集成将在后续迭代中逐步添加。
