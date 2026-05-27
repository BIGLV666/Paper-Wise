package org.example.paperwise.Controller;

import lombok.extern.slf4j.Slf4j;
import org.example.paperwise.Dto.Result;
import org.example.paperwise.Service.PdfService;
import org.example.paperwise.entry.Card;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/paperwise/pdf")
public class PdfController {
    @Autowired
    private PdfService pdfService;
    @PostMapping("/generatefrompdf")
    public Result<List<Card>>generatePdf(@RequestParam("file")MultipartFile file, @RequestAttribute Long userid){
        if(file.isEmpty()){
            log.info("文件为空");
            return Result.error("请传输pdf文件");
        }
        if(file.getContentType().isEmpty()){
            log.info("空文件后缀");
            return Result.error("请传输pdf文件");
        }
        if (!file.getContentType().equals("application/pdf")) {
            log.info("格式不对");
            return Result.error("请传输pdf文件");
        }
        if(file.getSize()>10*1024*1024){
            return Result.error("文件不超过10MB");
        }
        try {
            List<Card>cards= pdfService.generateFromPdf(userid,file);
            return Result.success(cards);
        }catch (Exception e){
            return Result.error(e.getMessage());
        }
    }
}
