#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
高并发在线考试系统 - 性能压力测试脚本
测试场景：
1. 大量考生同时获取考试令牌
2. 大量考生同时进入考试
3. 大量考生在考试中频繁修改答案
4. 考试时间耗尽时大量考生同时提交
"""

import requests
import threading
import time
import json
from concurrent.futures import ThreadPoolExecutor, as_completed
from collections import defaultdict
import statistics

# ==================== 配置参数 ====================
BASE_URL = "http://localhost:8080"  # 修改为实际的服务地址

# 测试参数
TOTAL_STUDENTS = 1000  # 总考生数
CONCURRENT_USERS = 100  # 并发用户数
EXAM_ID = 1  # 考试ID（需要先创建）
EXAM_TOKEN = None  # 考试令牌（会自动获取）

# 题目数量（用于模拟答题）
QUESTION_COUNT = 50

# 测试结果统计
test_results = {
    "get_token": {"success": 0, "failed": 0, "times": []},
    "start_exam": {"success": 0, "failed": 0, "times": []},
    "save_answer": {"success": 0, "failed": 0, "times": []},
    "submit_exam": {"success": 0, "failed": 0, "times": []},
}

# ==================== 辅助函数 ====================

def print_statistics(test_name, results):
    """打印测试统计信息"""
    success = results["success"]
    failed = results["failed"]
    total = success + failed
    times = results["times"]
    
    if times:
        avg_time = statistics.mean(times)
        min_time = min(times)
        max_time = max(times)
        p95_time = statistics.quantiles(times, n=20)[18] if len(times) > 20 else max_time
        p99_time = statistics.quantiles(times, n=100)[98] if len(times) > 100 else max_time
    else:
        avg_time = min_time = max_time = p95_time = p99_time = 0
    
    print(f"\n{'='*60}")
    print(f"测试场景: {test_name}")
    print(f"{'='*60}")
    print(f"总请求数: {total}")
    print(f"成功: {success} ({success/total*100:.2f}%)")
    print(f"失败: {failed} ({failed/total*100:.2f}%)")
    if times:
        print(f"平均响应时间: {avg_time:.2f}ms")
        print(f"最小响应时间: {min_time:.2f}ms")
        print(f"最大响应时间: {max_time:.2f}ms")
        print(f"P95响应时间: {p95_time:.2f}ms")
        print(f"P99响应时间: {p99_time:.2f}ms")
        print(f"QPS: {total/(sum(times)/1000) if sum(times) > 0 else 0:.2f}")

def login_student(username, password):
    """学生登录，获取JWT token"""
    url = f"{BASE_URL}/exam-online/auth/login"
    data = {
        "username": username,
        "password": password
    }
    try:
        response = requests.post(url, json=data, timeout=10)
        if response.status_code == 200:
            result = response.json()
            if result.get("code") == 200:
                return result.get("data", {}).get("token")
    except Exception as e:
        print(f"登录失败 {username}: {e}")
    return None

def register_student(username, password):
    """注册学生"""
    url = f"{BASE_URL}/exam-online/auth/register"
    data = {
        "username": username,
        "password": password
    }
    try:
        response = requests.post(url, json=data, timeout=10)
        if response.status_code == 200:
            result = response.json()
            return result.get("code") == 200
    except Exception as e:
        print(f"注册失败 {username}: {e}")
    return False

# ==================== 测试场景 ====================

def test_get_exam_token(student_id, jwt_token):
    """测试场景1: 获取考试令牌"""
    url = f"{BASE_URL}/exam-online/execute/token/{EXAM_ID}"
    headers = {
        "Authorization": f"Bearer {jwt_token}",
        "Content-Type": "application/json"
    }
    
    start_time = time.time()
    try:
        response = requests.get(url, headers=headers, timeout=10)
        elapsed = (time.time() - start_time) * 1000
        
        if response.status_code == 200:
            result = response.json()
            if result.get("code") == 200:
                test_results["get_token"]["success"] += 1
                test_results["get_token"]["times"].append(elapsed)
                return result.get("data")  # 返回令牌
            else:
                test_results["get_token"]["failed"] += 1
        else:
            test_results["get_token"]["failed"] += 1
    except Exception as e:
        test_results["get_token"]["failed"] += 1
        elapsed = (time.time() - start_time) * 1000
        print(f"获取令牌失败 [学生{student_id}]: {e}")
    
    return None

def test_start_exam(student_id, jwt_token, exam_token):
    """测试场景2: 进入考试"""
    url = f"{BASE_URL}/exam-online/execute/start/{EXAM_ID}"
    headers = {
        "Authorization": f"Bearer {jwt_token}",
        "X-Exam-Token": exam_token,
        "Content-Type": "application/json"
    }
    
    start_time = time.time()
    try:
        response = requests.post(url, headers=headers, timeout=10)
        elapsed = (time.time() - start_time) * 1000
        
        if response.status_code == 200:
            result = response.json()
            if result.get("code") == 200:
                test_results["start_exam"]["success"] += 1
                test_results["start_exam"]["times"].append(elapsed)
                return result.get("data")  # 返回examRecordId
            else:
                test_results["start_exam"]["failed"] += 1
        else:
            test_results["start_exam"]["failed"] += 1
    except Exception as e:
        test_results["start_exam"]["failed"] += 1
        elapsed = (time.time() - start_time) * 1000
        print(f"进入考试失败 [学生{student_id}]: {e}")
    
    return None

def test_save_answer(student_id, jwt_token, exam_token, exam_record_id, question_id, answer):
    """测试场景3: 保存答案（高频操作）"""
    url = f"{BASE_URL}/exam-online/execute/answer"
    headers = {
        "Authorization": f"Bearer {jwt_token}",
        "X-Exam-Token": exam_token,
        "Content-Type": "application/json"
    }
    data = {
        "examRecordId": exam_record_id,
        "questionId": question_id,
        "studentAnswer": answer
    }
    
    start_time = time.time()
    try:
        response = requests.post(url, headers=headers, json=data, timeout=10)
        elapsed = (time.time() - start_time) * 1000
        
        if response.status_code == 200:
            result = response.json()
            if result.get("code") == 200:
                test_results["save_answer"]["success"] += 1
                test_results["save_answer"]["times"].append(elapsed)
                return True
            else:
                test_results["save_answer"]["failed"] += 1
        else:
            test_results["save_answer"]["failed"] += 1
    except Exception as e:
        test_results["save_answer"]["failed"] += 1
        print(f"保存答案失败 [学生{student_id}, 题目{question_id}]: {e}")
    
    return False

def test_submit_exam(student_id, jwt_token, exam_token, exam_record_id):
    """测试场景4: 提交考试"""
    url = f"{BASE_URL}/exam-online/execute/submit/{exam_record_id}"
    headers = {
        "Authorization": f"Bearer {jwt_token}",
        "X-Exam-Token": exam_token,
        "Content-Type": "application/json"
    }
    
    start_time = time.time()
    try:
        response = requests.post(url, headers=headers, timeout=30)
        elapsed = (time.time() - start_time) * 1000
        
        if response.status_code == 200:
            result = response.json()
            if result.get("code") == 200:
                test_results["submit_exam"]["success"] += 1
                test_results["submit_exam"]["times"].append(elapsed)
                return True
            else:
                test_results["submit_exam"]["failed"] += 1
        else:
            test_results["submit_exam"]["failed"] += 1
    except Exception as e:
        test_results["submit_exam"]["failed"] += 1
        print(f"提交考试失败 [学生{student_id}]: {e}")
    
    return False

# ==================== 完整测试流程 ====================

def student_test_workflow(student_id):
    """单个学生的完整测试流程"""
    username = f"student_{student_id}"
    password = "123456"
    
    # 1. 注册学生（如果不存在）
    # register_student(username, password)
    
    # 2. 登录获取JWT
    jwt_token = login_student(username, password)
    if not jwt_token:
        print(f"学生{student_id}登录失败")
        return
    
    # 3. 获取考试令牌
    exam_token = test_get_exam_token(student_id, jwt_token)
    if not exam_token:
        print(f"学生{student_id}获取考试令牌失败")
        return
    
    # 4. 进入考试
    exam_record_id = test_start_exam(student_id, jwt_token, exam_token)
    if not exam_record_id:
        print(f"学生{student_id}进入考试失败")
        return
    
    # 5. 模拟答题（高频操作）
    for question_id in range(1, QUESTION_COUNT + 1):
        answer = f"答案_{student_id}_{question_id}"
        test_save_answer(student_id, jwt_token, exam_token, exam_record_id, question_id, answer)
        # 模拟思考时间
        time.sleep(0.1)
    
    # 6. 提交考试（提前结束）
    test_submit_exam(student_id, jwt_token, exam_token, exam_record_id)

# ==================== 并发测试场景 ====================

def test_scenario_1_concurrent_get_token():
    """场景1: 并发获取考试令牌"""
    print("\n" + "="*60)
    print("场景1: 并发获取考试令牌测试")
    print("="*60)
    
    def worker(student_id):
        username = f"student_{student_id}"
        jwt_token = login_student(username, "123456")
        if jwt_token:
            return test_get_exam_token(student_id, jwt_token)
        return None
    
    with ThreadPoolExecutor(max_workers=CONCURRENT_USERS) as executor:
        futures = [executor.submit(worker, i) for i in range(1, TOTAL_STUDENTS + 1)]
        for future in as_completed(futures):
            future.result()
    
    print_statistics("并发获取考试令牌", test_results["get_token"])

def test_scenario_2_concurrent_start_exam():
    """场景2: 并发进入考试"""
    print("\n" + "="*60)
    print("场景2: 并发进入考试测试")
    print("="*60)
    
    # 先获取一个令牌（所有学生使用同一个令牌）
    username = "student_1"
    jwt_token = login_student(username, "123456")
    if jwt_token:
        global EXAM_TOKEN
        EXAM_TOKEN = test_get_exam_token(1, jwt_token)
    
    if not EXAM_TOKEN:
        print("无法获取考试令牌，跳过测试")
        return
    
    def worker(student_id):
        username = f"student_{student_id}"
        jwt_token = login_student(username, "123456")
        if jwt_token:
            return test_start_exam(student_id, jwt_token, EXAM_TOKEN)
        return None
    
    with ThreadPoolExecutor(max_workers=CONCURRENT_USERS) as executor:
        futures = [executor.submit(worker, i) for i in range(1, TOTAL_STUDENTS + 1)]
        for future in as_completed(futures):
            future.result()
    
    print_statistics("并发进入考试", test_results["start_exam"])

def test_scenario_3_concurrent_save_answer():
    """场景3: 并发保存答案（高频操作）"""
    print("\n" + "="*60)
    print("场景3: 并发保存答案测试（高频操作）")
    print("="*60)
    
    # 准备数据
    username = "student_1"
    jwt_token = login_student(username, "123456")
    if not jwt_token:
        print("无法登录，跳过测试")
        return
    
    if not EXAM_TOKEN:
        EXAM_TOKEN = test_get_exam_token(1, jwt_token)
    
    exam_record_id = test_start_exam(1, jwt_token, EXAM_TOKEN)
    if not exam_record_id:
        print("无法进入考试，跳过测试")
        return
    
    # 模拟大量并发保存答案
    def worker(question_id):
        return test_save_answer(1, jwt_token, EXAM_TOKEN, exam_record_id, question_id, f"答案_{question_id}")
    
    # 每个学生修改所有题目，模拟高频操作
    total_requests = TOTAL_STUDENTS * QUESTION_COUNT
    
    with ThreadPoolExecutor(max_workers=CONCURRENT_USERS * 10) as executor:
        futures = []
        for student_id in range(1, TOTAL_STUDENTS + 1):
            for question_id in range(1, QUESTION_COUNT + 1):
                futures.append(executor.submit(worker, question_id))
        
        for future in as_completed(futures):
            future.result()
    
    print_statistics("并发保存答案", test_results["save_answer"])

def test_scenario_4_concurrent_submit():
    """场景4: 并发提交考试（考试时间耗尽场景）"""
    print("\n" + "="*60)
    print("场景4: 并发提交考试测试（模拟考试时间耗尽）")
    print("="*60)
    
    def worker(student_id):
        username = f"student_{student_id}"
        jwt_token = login_student(username, "123456")
        if not jwt_token:
            return False
        
        if not EXAM_TOKEN:
            exam_token = test_get_exam_token(student_id, jwt_token)
            if not exam_token:
                return False
        else:
            exam_token = EXAM_TOKEN
        
        exam_record_id = test_start_exam(student_id, jwt_token, exam_token)
        if not exam_record_id:
            return False
        
        # 提交考试
        return test_submit_exam(student_id, jwt_token, exam_token, exam_record_id)
    
    with ThreadPoolExecutor(max_workers=CONCURRENT_USERS) as executor:
        futures = [executor.submit(worker, i) for i in range(1, TOTAL_STUDENTS + 1)]
        for future in as_completed(futures):
            future.result()
    
    print_statistics("并发提交考试", test_results["submit_exam"])

# ==================== 主函数 ====================

def main():
    """主测试函数"""
    print("="*60)
    print("高并发在线考试系统 - 性能压力测试")
    print("="*60)
    print(f"测试配置:")
    print(f"  服务地址: {BASE_URL}")
    print(f"  总考生数: {TOTAL_STUDENTS}")
    print(f"  并发用户数: {CONCURRENT_USERS}")
    print(f"  考试ID: {EXAM_ID}")
    print(f"  题目数量: {QUESTION_COUNT}")
    print("="*60)
    
    # 执行各个测试场景
    start_time = time.time()
    
    # 场景1: 并发获取考试令牌
    test_scenario_1_concurrent_get_token()
    
    # 场景2: 并发进入考试
    test_scenario_2_concurrent_start_exam()
    
    # 场景3: 并发保存答案（高频操作）
    test_scenario_3_concurrent_save_answer()
    
    # 场景4: 并发提交考试
    test_scenario_4_concurrent_submit()
    
    total_time = time.time() - start_time
    
    # 打印总体统计
    print("\n" + "="*60)
    print("总体测试统计")
    print("="*60)
    print(f"总测试时间: {total_time:.2f}秒")
    
    total_requests = sum(
        r["success"] + r["failed"] 
        for r in test_results.values()
    )
    total_success = sum(r["success"] for r in test_results.values())
    total_failed = sum(r["failed"] for r in test_results.values())
    
    print(f"总请求数: {total_requests}")
    print(f"总成功数: {total_success} ({total_success/total_requests*100:.2f}%)")
    print(f"总失败数: {total_failed} ({total_failed/total_requests*100:.2f}%)")
    print(f"平均QPS: {total_requests/total_time:.2f}")

if __name__ == "__main__":
    main()

