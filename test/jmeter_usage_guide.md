# JMeter 测试计划使用指南

## 问题修复

已修复 JSON 解析错误，主要更改：
1. 设置 `postBodyRaw=true` 以发送原始 JSON
2. 修复 JSON 中的变量引用格式

## 使用步骤

### 1. 准备测试数据

在运行测试前，需要设置以下变量：

**方法1: 使用 User Defined Variables（推荐）**

在测试计划中添加 "User Defined Variables" 配置元件：

```
变量名: jwt_token
变量值: 你的JWT令牌（从登录接口获取）

变量名: exam_token  
变量值: 考试令牌（从获取令牌接口获取）

变量名: exam_record_id
变量值: 考试记录ID（从进入考试接口获取）
```

**方法2: 使用 CSV Data Set Config**

创建一个 CSV 文件 `test_data.csv`：
```csv
jwt_token,exam_token,exam_record_id
token1,token1,1
token2,token2,2
...
```

然后在测试计划中添加 "CSV Data Set Config" 配置元件。

### 2. 测试流程

**正确的测试顺序**：

1. **登录获取JWT**（需要先添加这个步骤）
   - 接口: `POST /exam-online/auth/login`
   - Body: `{"username":"student_1","password":"123456"}`
   - 使用 JSON Extractor 提取 token

2. **获取考试令牌**
   - 接口: `GET /exam-online/execute/token/{examId}`
   - Header: `Authorization: Bearer ${jwt_token}`
   - 使用 JSON Extractor 提取 exam_token

3. **进入考试**
   - 接口: `POST /exam-online/execute/start/{examId}`
   - Header: `Authorization: Bearer ${jwt_token}`
   - Header: `X-Exam-Token: ${exam_token}`
   - 使用 JSON Extractor 提取 exam_record_id

4. **保存答案**（高频操作）
   - 接口: `POST /exam-online/execute/answer`
   - Header: `Authorization: Bearer ${jwt_token}`
   - Header: `X-Exam-Token: ${exam_token}`
   - Body: `{"examRecordId":${exam_record_id},"questionId":${__Random(1,50,)},"studentAnswer":"答案_${__threadNum}_${__Random(1,1000,)}"}`

5. **提交考试**
   - 接口: `POST /exam-online/execute/submit/${exam_record_id}`
   - Header: `Authorization: Bearer ${jwt_token}`
   - Header: `X-Exam-Token: ${exam_token}`

### 3. 添加 JSON Extractor

在每个需要提取数据的请求后添加 "JSON Extractor"：

**提取JWT Token**:
- Variable names: `jwt_token`
- JSON Path expressions: `$.data.token`

**提取考试令牌**:
- Variable names: `exam_token`
- JSON Path expressions: `$.data`

**提取考试记录ID**:
- Variable names: `exam_record_id`
- JSON Path expressions: `$.data`

### 4. 配置线程组

**场景1: 并发获取考试令牌**
- 线程数: 100
- Ramp-up时间: 10秒
- 循环次数: 1

**场景2: 并发进入考试**
- 线程数: 100
- Ramp-up时间: 10秒
- 循环次数: 1

**场景3: 并发保存答案（高频操作）**
- 线程数: 100
- Ramp-up时间: 10秒
- 循环次数: 50（每个线程保存50次答案）

**场景4: 并发提交考试**
- 线程数: 100
- Ramp-up时间: 10秒
- 循环次数: 1

## 常见问题

### 1. JSON 解析错误

**错误**: `Unrecognized token '$'`

**原因**: JSON 中的 JMeter 变量没有被正确解析

**解决**:
- 确保 `postBodyRaw=true`
- 确保变量已正确定义
- 检查 JSON 格式是否正确

### 2. 变量未定义

**错误**: `${jwt_token}` 显示为字面量

**原因**: 变量未定义或未提取

**解决**:
- 添加 User Defined Variables
- 或使用 JSON Extractor 提取变量
- 确保变量名拼写正确

### 3. 401 未授权

**原因**: JWT token 无效或过期

**解决**:
- 重新登录获取 token
- 检查 token 是否正确提取
- 检查 Header 格式是否正确

## 优化建议

1. **使用 BeanShell 脚本**构建复杂的 JSON
2. **使用 CSV 文件**管理大量测试数据
3. **添加断言**验证响应结果
4. **使用监听器**查看测试结果
5. **分布式测试**使用多台机器进行压力测试

## 示例：完整的测试计划结构

```
Test Plan
├── User Defined Variables (设置默认值)
├── Thread Group 1: 登录
│   ├── HTTP Request: 登录
│   └── JSON Extractor: 提取 jwt_token
├── Thread Group 2: 获取考试令牌
│   ├── HTTP Request: 获取令牌
│   └── JSON Extractor: 提取 exam_token
├── Thread Group 3: 进入考试
│   ├── HTTP Request: 进入考试
│   └── JSON Extractor: 提取 exam_record_id
├── Thread Group 4: 保存答案（高频）
│   └── HTTP Request: 保存答案（循环50次）
└── Thread Group 5: 提交考试
    └── HTTP Request: 提交考试
```

