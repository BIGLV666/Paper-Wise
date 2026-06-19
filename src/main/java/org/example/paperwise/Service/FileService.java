package org.example.paperwise.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class FileService {
    @Autowired
    private PdfService pdfService;

    @Autowired
    private AiGeneratedCardService aiGeneratedCardService;


    /**
     * 根据文件类型分发处理
     * <p>根据文件扩展名将任务分发给对应的处理器，当前支持PDF文件</p>
     *
     * @param userId 用户ID
     * @param file 上传的文件
     * @param sessionId 会话ID
     */
    public void asyncGenerate(Long userId, MultipartFile file, String sessionId) throws IOException {
        if (file.isEmpty()) {
            throw new RuntimeException("空文件");
        }

        String fileName = file.getOriginalFilename();
        String extension = fileName.substring(fileName.lastIndexOf(".")).toLowerCase();

        switch (extension) {
            case "pdf":
                pdfService.generateFromPdf(userId, file, sessionId);
                return;
        }
    }
}
