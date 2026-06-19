# PaperWise 智能学习卡片平台

PaperWise 是一个基于 Spring Boot 的智能学习卡片平台后端系统，提供闪卡管理、收藏夹分享、错题本、SM-2 间隔重复复习、AI 对话助手、AI 自动生成题目，以及 PDF 导入出题等核心功能。

---

## 📋 目录

- [技术栈](#技术栈)
- [项目结构](#项目结构)
- [功能模块](#功能模块)
- [数据库设计](#数据库设计)
- [API 接口](#api-接口)
- [认证与安全](#认证与安全)
- [外部依赖服务](#外部依赖服务)
- [定时任务](#定时任务)
- [快速开始](#快速开始)
- [架构概览](#架构概览)
- [开发说明](#开发说明)

---

## 🛠️ 技术栈

| 类别 | 技术 | 版本 |
|------|------|------|
| **框架** | Spring Boot | 3.5.14 |
| | Spring Security | - |
| | Spring AOP | - |
| **语言** | Java | 17 |
| **持久层** | MyBatis-Plus | 3.5.5 |
| | MySQL | 8 |
| **缓存** | Redis | - |
| | Lettuce | - |
| **认证** | JWT (jjwt) | 0.12.x |
| **AI 对话** | LangChain4j | - |
| | Ollama | qwen2.5:7b |
| **AI 出题** | 阿里云通义千问 | DashScope API |
| **PDF 解析** | Apache PDFBox | 2.0.30 |
| **文档** | Knife4j / SpringDoc OpenAPI | - |
| **其他** | Lombok、Fastjson2、OkHttp、Spring Mail | - |

---

## 📁 项目结构

```
PaperWise/
├── pom.xml                                    # Maven 依赖配置
├── src/main/
│   ├── java/org/example/paperwise/
│   │   ├── PaperWiseApplication.java         # 启动类（@EnableAsync / @EnableScheduling）
│   │   ├── Advice/                            # 全局异常、AOP 切面（限流、浏览量）
│   │   ├── Config/                            # Security、Redis、CORS、MyBatis、LangChain 等配置
│   │   ├── Controller/                        # REST 控制器（11 个）
│   │   ├── Dto/                               # 请求/响应 DTO
│   │   ├── entry/                             # 数据库实体（13 个）
│   │   ├── enums/                             # CardType、CardDifficulty、CardMastery
│   │   ├── Interface/                         # 自定义注解 @RateLimit、@LookCount
│   │   ├── Mapper/                            # MyBatis Mapper 接口
│   │   ├── Service/                           # 业务逻辑层
│   │   ├── Task/                              # 定时任务（Redis 计数同步、排行榜）
│   │   └── Until/                             # 工具类（JWT、邮件、密码加密等）
│   └── resources/
│       ├── application.yml                    # 应用配置
│       ├── Sql/User.sql                       # 数据库建表脚本
│       ├── Mapper/*.xml                       # MyBatis XML 映射
│       └── logback-spring.xml                 # 日志配置
└── src/test/                                  # 单元测试
```

---

## ✨ 功能模块

### 1. 👤 用户系统（UserService）

- **注册**：邮箱验证码，验证码存 Redis
- **登录**：返回 JWT Token，有效期 7 天
- **邮箱激活**：激活账号
- **修改密码**：旧密码验证 / 邮箱验证码重置
- **用户资料**：获取用户资料及统计（卡片数、复习数、掌握数）

### 2. 📚 闪卡管理（CardService）

- **CRUD**：单张/批量创建、更新、删除卡片
- **查询**：按题型分类查询、分页列表
- **题型支持**：单选、多选、判断、填空、简答
- **卡片属性**：
  - 难度：`EASY` / `MEDIUM` / `HARD`
  - 掌握度：`NOT_STARTED` / `LEARNING` / `MASTERED`

### 3. 📁 收藏夹（FavoritesService）

- **管理**：创建/删除收藏夹，添加/移除卡片
- **分享**：公开/私有切换，生成分享链接（7 天有效）
- **复制**：复制他人公开收藏夹
- **统计**：浏览量、点赞数（Redis 实时计数，定时同步到 MySQL）

### 4. 🌐 社区（CommunityService）

- **发现**：公开收藏夹发现（按浏览量/点赞量/默认排序）
- **排行榜**：Redis ZSET 排行榜 Top 10（每 5 分钟修正）

### 5. ❌ 错题本（WrongQuestionService）

- **添加错题**：将做错的卡片加入错题本
- **自动记录**：加入错题时自动创建 SM-2 复习记录

### 6. 🔄 间隔重复复习（WrongReviewService）

采用 **SM-2（SuperMemo 2）** 算法调度复习：

| 参数 | 字段 | 说明 |
|------|------|------|
| n | `stage` | 连续成功复习次数 |
| I | `intervalDays` | 下次复习间隔（天） |
| EF | `easinessFactor` | 难易系数，初始 2.5，最小 1.3 |
| q | `quality` | 回忆质量 0~5，q < 3 视为失败 |

**算法规则**：

1. `EF' = EF + (0.1 - (5-q) × (0.08 + (5-q) × 0.02))`，若 EF' < 1.3 则取 1.3
2. q < 3：重置 n=0，间隔 I=1 天
3. q ≥ 3：n+1；n=1 时 I=1，n=2 时 I=6，n>2 时 I = round(上次间隔 × EF')
4. 连续成功 **10 次**视为掌握，自动从错题本和复习队列移除

**每日统计**：完成数、剩余数、连续打卡天数等。

### 7. 🤖 AI 对话助手（RAGService + AiService）

- **模型**：基于 LangChain4j 接入本地 Ollama 模型
- **对话**：支持多轮对话，会话/消息持久化
- **缓存**：聊天历史 Redis 缓存加速读取
- **工具调用**：Tool Calling 能力（查询用户卡片等）

### 8. ✍️ AI 自动生成题目（AiGeneratedCardService + QianwenService）

- **输入**：从文本或 PDF 内容调用通义千问生成结构化题目（JSON）
- **异步**：异步生成，按 `sessionId` 分组管理
- **导入**：支持批量导入到个人卡片库或指定收藏夹

### 9. 📄 PDF 导入（PdfService + FileService）

- **上传**：上传 PDF（最大 10MB），PDFBox 提取文本
- **生成**：提取后异步调用 AI 生成题目，返回 `sessionId` 供前端轮询

---

## 🗄️ 数据库设计

**数据库名**：`paperwise`，完整 DDL 见 `src/main/resources/Sql/User.sql`。

| 表名 | 说明 |
|------|------|
| `user` | 用户账号信息 |
| `card` | 闪卡数据 |
| `favorites` | 收藏夹（card_ids 为 JSON 数组） |
| `card_in_favorites_record` | 卡片-收藏夹关联表 |
| `share` | 分享链接（uuid 主键，7 天过期） |
| `favorites_like_record` | 收藏夹点赞记录 |
| `wrong_question` | 错题本记录 |
| `wrong_review` | SM-2 复习调度（含 easiness_factor） |
| `wrong_review_stats` | 每日复习统计 |
| `ai_generated_card` | AI 生成题目暂存区（PENDING/HANDLED） |
| `chat_session` | AI 对话会话 |
| `chat_message` | AI 对话消息 |

---

## 🔌 API 接口

**Base URL**：`http://localhost:8080`

**统一响应格式**：

```json
{
  "code": 200,
  "message": "success",
  "data": { }
}
```

**认证方式**：除公开接口外，请求头需携带：

```
Authorization: Bearer <JWT_TOKEN>
```

拦截器解析 Token 后注入 `@RequestAttribute Long userid`。

**在线文档**：[http://localhost:8080/doc.html](http://localhost:8080/doc.html)

### 🔓 公开接口（无需 Token）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/paperwise/user/login` | 用户登录 |
| POST | `/paperwise/user/register` | 用户注册 |
| POST | `/paperwise/user/activation` | 邮箱激活 |
| GET | `/paperwise/share/**` | 分享链接访问 |

### 👤 用户 `/paperwise/user`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/updatepasswordforpassword` | 旧密码修改密码 |
| POST | `/updatepasswordforemail` | 邮箱验证码重置（限流 1 次/分钟） |
| POST | `/updatepasswordcode` | 确认验证码并重置密码 |
| POST | `/getuser` | 获取用户资料与统计 |

### 📚 卡片 `/paperwise/card`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/addcard` | 创建单张卡片 |
| PUT | `/updatecard` | 更新卡片 |
| DELETE | `/deletecard` | 删除卡片 |
| POST | `/addallcard` | 批量创建 |
| GET | `/getallcard` | 分页获取用户卡片 |
| GET | `/getcard` | 按 ID 获取卡片 |
| POST | `/getcardbyid` | 按 ID 获取卡片 |
| GET | `/getallquestiontype` | 获取所有题型分类及数量 |
| POST | `/getbyquestiontype` | 按题型分页查询 |

### 📁 收藏夹 `/paperwise/favorites`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/createfavorites` | 创建收藏夹 |
| POST | `/addcard` | 添加卡片 |
| POST | `/addAllCard` | 批量添加卡片 |
| DELETE | `/deletefavorites` | 删除收藏夹 |
| DELETE | `/deletecard` | 移除卡片 |
| GET | `/getallfavorites` | 分页获取收藏夹列表 |
| POST | `/getshareid` | 生成分享链接 |
| POST | `/updateispublic` | 切换公开/私有 |
| POST | `/copyfavorites` | 复制公开收藏夹 |
| POST | `/getallcard` | 按 ID 列表获取卡片 |
| GET | `/getpublicfavorites` | 查看公开收藏夹 |

### 👍 点赞 `/paperwise/favoriteslikerecord`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/getalllikefavoritesdto` | 获取点赞列表 |
| POST | `/uplikecount` | 点赞 |
| POST | `/deletelike` | 取消点赞 |
| GET | `/checklikestatus` | 检查点赞状态 |

### 🌐 社区 `/paperwise/community`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/getallfavoritestop` | 排行榜 Top 10 |
| POST | `/getallfavorites` | 分页浏览公开收藏夹 |

### ❌ 错题本 `/paperwise/wrongquestion`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/addwrongquestion` | 添加错题 |
| GET | `/getwrongquestiondto` | 获取错题列表 |
| DELETE | `/deletewrongquestion` | 删除错题 |

### 🔄 复习 `/paperwise/wrongreview`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/getwrongreviewdto` | 获取当日复习卡片 |
| GET | `/getcard` | 复习中获取卡片详情 |
| POST | `/updateReviewProgress` | 提交复习评分（quality 1~5） |
| GET | `/getwrongreviewtats` | 获取当日复习统计 |

### 🤖 AI 对话 `/paperwise/ai`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/ask` | 向 AI 提问 |
| GET | `/getallhistory` | 获取会话列表 |
| GET | `/getallmessage` | 获取会话消息 |
| DELETE | `/deletehistory` | 删除会话 |
| POST | `/updatetitle` | 修改会话标题 |

### ✍️ AI 生成题目 `/paperwise/aigeneratedcard`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/generatefromtext` | 从文本生成题目（异步） |
| GET | `/getaigeneratedcardbysessionid` | 按 sessionId 获取生成结果 |
| POST | `/batchcards` | 批量导入到卡片库 |
| POST | `/batchcardstofavorites` | 批量导入到收藏夹 |
| GET | `/getallhistoryrecord` | 获取生成历史 |

### 📄 PDF `/paperwise/pdf`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/generatefrompdf` | 上传 PDF 并 AI 生成题目 |

---

## 🔐 认证与安全

```
客户端                    服务端
  │                         │
  ├── POST /login ─────────►│ 验证用户名密码
  │◄── JWT Token ───────────┤
  │                         │
  ├── 业务请求 ─────────────►│ LoginInterceptor 解析 Bearer Token
  │   Authorization: Bearer │ 注入 userid 到 RequestAttribute
  │◄── Result<T> ───────────┤
```

**安全机制**：

- **JWT**：HS256 签名，Subject 为 `userId`，有效期 604800000ms（7 天）
- **Spring Security**：CSRF 关闭，所有路径 `permitAll`；实际鉴权由自定义 `LoginInterceptor` 完成
- **CORS**：允许 `http://localhost:5173`（Vite 前端）
- **限流**：`@RateLimit` 注解 + Redis 滑动窗口（如邮箱重置 1 次/分钟）
- **全局异常**：`GlobalExceptionHandler` 统一捕获并返回 `Result.error()`

---

## 🔌 外部依赖服务

| 服务 | 地址 | 用途 |
|------|------|------|
| MySQL | `localhost:3306/paperwise` | 主数据库 |
| Redis | `localhost:6379` | 验证码、缓存、计数器、排行榜、限流 |
| Ollama | `localhost:11434` | AI 对话（`qwen2.5:7b`）、向量嵌入（`nomic-embed-text`） |
| QQ SMTP | `smtp.qq.com:587` | 注册/重置密码邮件 |
| 通义千问 | DashScope API | AI 自动生成题目 |

---

## ⏰ 定时任务

| 任务 | 类 | 频率 | 说明 |
|------|-----|------|------|
| 浏览量同步 | `_CountTask.upLookCount` | 每 60 秒 | Redis → MySQL |
| 点赞同步 | `_CountTask.upLikeCount` | 每 60 秒 | Redis → MySQL + 写入点赞记录 |
| 排行榜修正 | `_CountTask.rank` | 每 5 分钟 | 修正 Redis ZSET 分数 |

---

## 🚀 快速开始

### 环境要求

- JDK 17+
- Maven 3.6+
- MySQL 8
- Redis
- Ollama（AI 对话功能，可选）
- 通义千问 API Key（AI 出题功能，可选）

### 1. 初始化数据库

```sql
CREATE DATABASE paperwise CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE paperwise;
DELIMITER src/main/resources/Sql/User.sql;
```

> 💡 **提示**：若数据库已存在旧表结构，需手动补充 SM-2 字段：
>
> ```sql
> ALTER TABLE wrong_review ADD COLUMN easiness_factor DOUBLE NOT NULL DEFAULT 2.5;
> ```

### 2. 修改配置

编辑 `src/main/resources/application.yml`，配置以下项（**生产环境请使用环境变量，勿提交密钥**）：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/paperwise?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf-8
    username: root
    password: <your-password>
  data:
    redis:
      host: localhost
      port: 6379
  mail:
    username: <your-email>
    password: <smtp-auth-code>

jwt:
  secret: <your-jwt-secret>

qianwen:
  apikey: <your-dashscope-api-key>
```

### 3. 启动 Ollama（可选）

```bash
ollama pull qwen2.5:7b
ollama pull nomic-embed-text
ollama serve
```

### 4. 构建与运行

```bash
# 构建
mvn clean package -DskipTests

# 开发模式
mvn spring-boot:run

# 或运行 JAR
java -jar target/PaperWise-0.0.1-SNAPSHOT.jar
```

### 5. 验证

- **服务地址**：http://localhost:8080
- **API 文档**：http://localhost:8080/doc.html
- **登录示例**：

```bash
curl -X POST "http://localhost:8080/paperwise/user/login?username=test&password=123456"
```

---

## 🏗️ 架构概览

```
┌─────────────┐     ┌──────────────────────────────────────────┐
│  前端 Vue   │────►│           PaperWise Backend              │
│ localhost:  │     │  Controller → Service → Mapper → MySQL    │
│    5173     │     │       ↕ Redis（缓存/计数/限流/排行榜）      │
└─────────────┘     │       ↕ Ollama（AI 对话）                  │
                    │       ↕ 通义千问（AI 出题）                 │
                    │       ↕ QQ SMTP（邮件验证）                 │
                    └──────────────────────────────────────────┘
```

**分层架构**：

- **Controller 层**：接收 HTTP 请求，参数校验，调用 Service 层
- **Service 层**：业务逻辑处理，事务管理，调用 Mapper 层
- **Mapper 层**：数据库操作，SQL 执行
- **Redis 层**：缓存、计数器、排行榜、限流

---

## 💻 开发说明

### 新增接口

1. 在 `Controller` 包添加控制器方法
2. 在 `Service` 包实现业务逻辑
3. 如需自定义 SQL，在 `Mapper` 接口 + `resources/Mapper/*.xml` 中添加
4. 实体类放在 `entry` 包，DTO 放在 `Dto` 包

### 日志

- **慢接口告警**：`AopAdvice` 记录超过 200ms 的 Controller/Service 调用
- **SQL 日志**：MyBatis-Plus `log-impl: StdOutImpl`（开发环境）

### 注意事项

- ⚠️ `application.yml` 中含敏感信息，部署前请替换为环境变量
- ⚠️ Spring Security 配置为全放行，鉴权完全依赖 `LoginInterceptor`
- ⚠️ AI 生成题目为异步任务，前端需通过 `sessionId` 轮询结果
- ⚠️ 收藏夹 `card_ids` 字段为 JSON 数组，与 `card_in_favorites_record` 表并存

---

## 📄 License

本项目为私有学习项目，暂未指定开源协议。

---

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

---

## 📞 联系方式

如有问题，请通过以下方式联系：

- 提交 Issue
- 发送邮件

---

**PaperWise** © 2024 All Rights Reserved.
