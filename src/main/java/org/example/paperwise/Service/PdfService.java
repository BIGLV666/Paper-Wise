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






    //读取pdf
    public void generateFromPdf(Long userId, MultipartFile file, String sessionId) throws IOException {
        // 在同步上下文中读取文件内容
        byte[] fileBytes = file.getBytes();
        String fileName = file.getOriginalFilename();
        Object[] val = getTextFromBytes(fileBytes, fileName);
        // 调用异步方法处理
        aiGeneratedCardService.asyncProcessPdf(userId, sessionId,val);

    }















}