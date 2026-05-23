package org.example.paperwise.Until;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class EmailUntil {
    private static final Logger log =  LoggerFactory.getLogger(EmailUntil.class);
    @Autowired
    private JavaMailSender mailSender;
    @Value("${spring.mail.username}")
    private String from;
    @Async
    public void sendActivationEmail(String to,String subject,String content){
        try{
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(content);
            mailSender.send(message);
            log.info("Email Sent{}",to);
        }catch(Exception e){
            log.error(e.getMessage());
            throw new RuntimeException("失败"+e.getMessage());
        }
    }




}