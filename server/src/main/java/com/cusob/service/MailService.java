package com.cusob.service;

import com.cusob.entity.Sender;

import java.util.Date;

public interface MailService {

    /**
     * 发送纯文本邮件
     * @param to
     * @param subject
     * @param text
     */
    void sendTextMailMessage(String to,String subject,String text);

    /**
     * send email
     * @param sender
     * @param senderName
     * @param to
     * @param content
     * @param subject
     */
    void sendEmail(Sender sender, String senderName, String to, String content, String subject);

    /**
     * send email by simple-java-mail
     * @param sender
     * @param senderName
     * @param to
     * @param content
     * @param subject
     */
    void sendSimpleEmail(Sender sender, String senderName, String to, String content, String subject);

}
