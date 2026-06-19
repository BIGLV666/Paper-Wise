package org.example.paperwise.Service;


import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;

@Slf4j
@Service
public class PdfService {

    @Autowired
    private AiGeneratedCardService aiGeneratedCardService;

    private Object[] getTextFromBytes(byte[] fileBytes, String fileName) throws IOException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(fileBytes);
             PDDocument document = PDDocument.load(bais)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            return new Object[]{text, fileName};
        }}






    /**
     * 从PDF文件生成卡片
     * <p>读取PDF文件内容后，异步调用AI服务进行内容解析和卡片生成</p>
     *
     * @param userId 用户ID
     * @param file PDF文件
     * @param sessionId 会话ID，用于追踪处理状态
     */
    public void generateFromPdf(Long userId, MultipartFile file, String sessionId) throws IOException {
        byte[] fileBytes = file.getBytes();
        String fileName = file.getOriginalFilename();
        Object[] val = getTextFromBytes(fileBytes, fileName);
        aiGeneratedCardService.asyncProcessPdf(userId, sessionId, val);
    }















}