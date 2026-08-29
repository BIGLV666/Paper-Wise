package org.example.paperwise.Service;


import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;

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
            if (text == null || text.trim().length() < 20) {
                text = extractWithTesseract(document);
            }
            if (text == null || text.isBlank()) {
                throw new IllegalArgumentException("PDF 没有可识别的文本内容，请确认文件不是空白扫描件");
            }
            return new Object[]{text, fileName};
        }}

    private String extractWithTesseract(PDDocument document) throws IOException {
        PDFRenderer renderer = new PDFRenderer(document);
        StringBuilder result = new StringBuilder();
        for (int page = 0; page < document.getNumberOfPages(); page++) {
            Path image = Files.createTempFile("paperwise-pdf-", ".png");
            Path output = Files.createTempFile("paperwise-ocr-", "");
            try {
                ImageIO.write(renderer.renderImageWithDPI(page, 200), "png", image.toFile());
                Files.deleteIfExists(output);
                Process process = new ProcessBuilder("tesseract", image.toString(), output.toString(), "-l", "chi_sim+eng")
                        .redirectErrorStream(true).start();
                if (!process.waitFor(120, TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                    throw new IllegalStateException("OCR 处理超时");
                }
                if (process.exitValue() != 0) {
                    String error = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                    throw new IllegalStateException("OCR 处理失败：" + error);
                }
                Path txt = Path.of(output + ".txt");
                if (Files.exists(txt)) result.append(Files.readString(txt, StandardCharsets.UTF_8)).append("\n\n");
                Files.deleteIfExists(txt);
            } catch (IOException e) {
                throw new IllegalStateException("检测到扫描图片 PDF，但服务器未安装 Tesseract OCR，请安装 tesseract 并配置 chi_sim+eng 语言包", e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("OCR 处理被中断", e);
            } finally {
                Files.deleteIfExists(image);
                Files.deleteIfExists(output);
                Files.deleteIfExists(Path.of(output + ".txt"));
            }
        }
        return result.toString();
    }






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
