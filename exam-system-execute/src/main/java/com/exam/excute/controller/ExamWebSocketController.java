package com.exam.excute.controller;

import com.exam.excute.service.AntiCheatService;
import com.exam.excute.service.ExamSyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.util.Map;

/**
 * 考试WebSocket控制器
 */
@Controller
public class ExamWebSocketController {

    @Autowired
    private AntiCheatService antiCheatService;

    @Autowired
    private ExamSyncService examSyncService;

    /**
     * 处理心跳
     */
    @MessageMapping("/exam/heartbeat")
    public void handleHeartbeat(@Payload Map<String, Object> payload, SimpMessageHeaderAccessor headerAccessor) {
        Long examRecordId = Long.valueOf(payload.get("examRecordId").toString());
        Long studentId = Long.valueOf(payload.get("studentId").toString());
        antiCheatService.recordHeartbeat(examRecordId, studentId);
    }

    /**
     * 处理切屏事件
     */
    @MessageMapping("/exam/switch")
    public void handleSwitch(@Payload Map<String, Object> payload) {
        Long examRecordId = Long.valueOf(payload.get("examRecordId").toString());
        Long studentId = Long.valueOf(payload.get("studentId").toString());
        antiCheatService.recordSwitch(examRecordId, studentId);
        
        // 推送警告
        examSyncService.pushWarning(examRecordId, "检测到切屏行为，请注意！");
    }

    /**
     * 处理窗口失焦
     */
    @MessageMapping("/exam/blur")
    public void handleBlur(@Payload Map<String, Object> payload) {
        Long examRecordId = Long.valueOf(payload.get("examRecordId").toString());
        Long studentId = Long.valueOf(payload.get("studentId").toString());
        antiCheatService.recordBlur(examRecordId, studentId);
    }

    /**
     * 处理窗口聚焦
     */
    @MessageMapping("/exam/focus")
    public void handleFocus(@Payload Map<String, Object> payload) {
        Long examRecordId = Long.valueOf(payload.get("examRecordId").toString());
        Long studentId = Long.valueOf(payload.get("studentId").toString());
        antiCheatService.recordFocus(examRecordId, studentId);
    }

    /**
     * 处理答案保存
     */
    @MessageMapping("/exam/answer")
    public void handleAnswer(@Payload Map<String, Object> payload) {
        Long examRecordId = Long.valueOf(payload.get("examRecordId").toString());
        Long questionId = Long.valueOf(payload.get("questionId").toString());
        String studentAnswer = payload.get("studentAnswer").toString();
        
        examSyncService.saveAnswerRealtime(examRecordId, questionId, studentAnswer);
    }
}

