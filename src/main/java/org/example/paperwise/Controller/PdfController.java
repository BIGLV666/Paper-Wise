/**
 * PDF文档控制器
 * <p>提供PDF文档解析、AI生成卡片等功能</p>
 *
 * @author PaperWise Team
 */
package org.example.paperwise.Controller;

import lombok.extern.slf4j.Slf4j;
import org.example.paperwise.Dto.Result;
import org.example.paperwise.Service.PdfService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * PDF控制器
 * <p>处理PDF文档上传和AI卡片生成</p>
 */
@Slf4j
@RestController
@RequestMapping("/paperwise/pdf")
public class PdfController {

    @Autowired
    private PdfService pdfService;

    /**
     * 从PDF文件生成卡片
     * <p>支持PDF文档解析，自动提取内容生成学习卡片</p>
     * @param file PDF文件（大小不超过10MB）
     * @param userid 用户ID
     * @return 生成会话ID，用于后续查询生成结果
     */
    @PostMapping("/generatefrompdf")
    public Result<String> generatePdf(@RequestParam("file") MultipartFile file, @RequestAttribute Long userid) {
        // 文件非空验证
        if (file.isEmpty()) {
            log.info("文件为空");
            return Result.error("请传输pdf文件");
        }

        // 文件类型验证
        if (file.getContentType() == null || file.getContentType().isEmpty()) {
            log.info("空文件后缀");
            return Result.error("请传输pdf文件");
        }

        // PDF格式验证
        if (!file.getContentType().equals("application/pdf")) {
            log.info("格式不对");
            return Result.error("请传输pdf文件");
        }

        // 文件大小验证（10MB限制）
        if (file.getSize() > 10 * 1024 * 1024) {
            return Result.error("文件不超过10MB");
        }

        try {
            String sessionId = UUID.randomUUID().toString();
            pdfService.generateFromPdf(userid, file, sessionId);
            return Result.success(sessionId);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
