#!/bin/bash

# 高并发在线考试系统 - 快速测试脚本
# 使用 curl 进行简单的压力测试

BASE_URL="http://localhost:8080"
EXAM_ID=1
TOTAL_STUDENTS=100
CONCURRENT=10

echo "=========================================="
echo "高并发在线考试系统 - 快速压力测试"
echo "=========================================="
echo "服务地址: $BASE_URL"
echo "考试ID: $EXAM_ID"
echo "总考生数: $TOTAL_STUDENTS"
echo "并发数: $CONCURRENT"
echo "=========================================="

# 颜色输出
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 测试场景1: 并发获取令牌
echo -e "\n${YELLOW}场景1: 并发获取考试令牌${NC}"
success=0
failed=0

for i in $(seq 1 $TOTAL_STUDENTS); do
    {
        response=$(curl -s -w "\n%{http_code}" -X GET \
            -H "Authorization: Bearer YOUR_JWT_TOKEN" \
            "$BASE_URL/exam-online/execute/token/$EXAM_ID")
        
        http_code=$(echo "$response" | tail -n1)
        if [ "$http_code" = "200" ]; then
            ((success++))
        else
            ((failed++))
        fi
    } &
    
    # 控制并发数
    if (( i % CONCURRENT == 0 )); then
        wait
    fi
done
wait

echo -e "${GREEN}成功: $success${NC}"
echo -e "${RED}失败: $failed${NC}"

# 测试场景2: 并发进入考试
echo -e "\n${YELLOW}场景2: 并发进入考试${NC}"
success=0
failed=0

EXAM_TOKEN="YOUR_EXAM_TOKEN"  # 需要先获取

for i in $(seq 1 $TOTAL_STUDENTS); do
    {
        response=$(curl -s -w "\n%{http_code}" -X POST \
            -H "Authorization: Bearer YOUR_JWT_TOKEN" \
            -H "X-Exam-Token: $EXAM_TOKEN" \
            "$BASE_URL/exam-online/execute/start/$EXAM_ID")
        
        http_code=$(echo "$response" | tail -n1)
        if [ "$http_code" = "200" ]; then
            ((success++))
        else
            ((failed++))
        fi
    } &
    
    if (( i % CONCURRENT == 0 )); then
        wait
    fi
done
wait

echo -e "${GREEN}成功: $success${NC}"
echo -e "${RED}失败: $failed${NC}"

# 测试场景3: 并发保存答案（高频操作）
echo -e "\n${YELLOW}场景3: 并发保存答案（高频操作）${NC}"
success=0
failed=0

EXAM_RECORD_ID=1  # 需要先进入考试获取

for i in $(seq 1 $((TOTAL_STUDENTS * 10))); do
    {
        question_id=$((RANDOM % 50 + 1))
        answer="答案_$i"
        
        response=$(curl -s -w "\n%{http_code}" -X POST \
            -H "Authorization: Bearer YOUR_JWT_TOKEN" \
            -H "X-Exam-Token: $EXAM_TOKEN" \
            -H "Content-Type: application/json" \
            -d "{\"examRecordId\":$EXAM_RECORD_ID,\"questionId\":$question_id,\"studentAnswer\":\"$answer\"}" \
            "$BASE_URL/exam-online/execute/answer")
        
        http_code=$(echo "$response" | tail -n1)
        if [ "$http_code" = "200" ]; then
            ((success++))
        else
            ((failed++))
        fi
    } &
    
    if (( i % (CONCURRENT * 10) == 0 )); then
        wait
    fi
done
wait

echo -e "${GREEN}成功: $success${NC}"
echo -e "${RED}失败: $failed${NC}"

echo -e "\n${GREEN}测试完成！${NC}"

