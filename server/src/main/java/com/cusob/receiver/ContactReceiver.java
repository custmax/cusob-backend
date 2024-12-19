package com.cusob.receiver;

import com.cusob.constant.MqConst;
import com.cusob.dto.ContactDto;
import com.cusob.entity.Contact;
import com.cusob.exception.CusobException;
import com.cusob.result.ResultCodeEnum;
import com.cusob.service.ContactService;
import com.cusob.service.GroupService;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.MXRecord;
import org.xbill.DNS.Record;
import org.xbill.DNS.Type;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Base64;
import java.util.Map;

import static com.cusob.utils.EmailUtil.readResponse;
import static com.cusob.utils.EmailUtil.sendCommand;

@Component
public class ContactReceiver {

    @Autowired
    private ContactService contactService;
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_CHECK_EMAIL, durable = "true"),
            exchange = @Exchange(value = MqConst.EXCHANGE_CHECK_DIRECT),
            key = {MqConst.ROUTING_CHECK_EMAIL}//路由key
    ))
    public void checkEmail(Contact contact, Message message, Channel channel) throws IOException {
        String email = contact.getEmail();
        Long groupId = contact.getGroupId();
        boolean check = true;

        try {
            if (email == null || !email.contains("@")) {
                // 如果 email 为空或者不包含 '@' 符号，直接设置 check 为 false
                check = false;
            } else {
                String domain = email.split("@")[1]; // 获取域名
                // 检查MX记录
                Record[] records = new Lookup(domain, Type.MX).run(); // 查询MX记录
                if (!(records != null && records.length > 0)) { // 如果没有MX记录
                    check = false;
                } else {
                    for (Record record : records) {
                        MXRecord mxRecord = (MXRecord) record;
                        Socket socket = null;
                        SSLSocket sslSocket = null;
                        BufferedReader reader = null;
                        PrintWriter writer = null;

                        try {
                            // 初始连接
                            socket = new Socket(mxRecord.getTarget().toString(), 25);
                            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                            writer = new PrintWriter(socket.getOutputStream(), true);

                            readResponse(reader); // 初始问候

                            // 声明支持ESMTP
                            sendCommand(writer, "EHLO " + domain);
                            String ehloResponse = readResponse(reader);

                            // 如果服务器支持STARTTLS，先进行TLS加密
                            if (ehloResponse.contains("STARTTLS")) {
                                sendCommand(writer, "STARTTLS");
                                readResponse(reader);

                                // 使用SSLSocket进行后续通信
                                SSLSocketFactory sslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
                                sslSocket = (SSLSocket) sslSocketFactory.createSocket(
                                        socket,
                                        socket.getInetAddress().getHostAddress(),
                                        socket.getPort(),
                                        true);
                                sslSocket.startHandshake();

                                // 更新reader和writer为SSL版本
                                reader = new BufferedReader(new InputStreamReader(sslSocket.getInputStream()));
                                writer = new PrintWriter(sslSocket.getOutputStream(), true);

                                // 重新发送EHLO
                                sendCommand(writer, "EHLO " + domain);
                                readResponse(reader);
                            }

                            // 进行AUTH LOGIN认证
                            sendCommand(writer, "AUTH LOGIN");
                            readResponse(reader);

                            // 发送Base64编码的用户名
                            String username = "your_username"; // 需要替换为实际的用户名
                            sendCommand(writer, Base64.getEncoder().encodeToString(username.getBytes()));
                            readResponse(reader);

                            // 发送Base64编码的密码
                            String password = "your_password"; // 需要替换为实际的密码
                            sendCommand(writer, Base64.getEncoder().encodeToString(password.getBytes()));
                            readResponse(reader);

                            // 认证成功后继续原有的验证流程
                            sendCommand(writer, "MAIL FROM:<verify@" + domain + ">");
                            readResponse(reader);

                            sendCommand(writer, "RCPT TO:<" + email + ">");
                            String response = readResponse(reader);

                            if (response.startsWith("250")) {
                                sendCommand(writer, "QUIT");
                                break;
                            } else if (response.startsWith("550")) {
                                sendCommand(writer, "QUIT");
                                check = false;
                            }
                        } finally {
                            // 关闭所有资源
                            try {
                                if (reader != null) reader.close();
                                if (writer != null) writer.close();
                                if (sslSocket != null) sslSocket.close();
                                if (socket != null) socket.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
            contactService.updateByEmail(email, groupId, contact.getUserId(), check ? 1 : 0); // 更新状态
        } catch (Exception e) {
            contactService.updateByEmail(email, groupId, contact.getUserId(), 1);
            // 如果抛异常，可能是无法连接到相应的SMTP服务器，无法用这种方式判断存不存在，则先按存在处理
            e.printStackTrace(); // 打印异常信息
        } finally {
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        }
    }


//    @RabbitListener(bindings = @QueueBinding(
//            value = @Queue(value = MqConst.QUEUE_CHECK_EMAIL, durable = "true"),
//            exchange = @Exchange(value = MqConst.EXCHANGE_CHECK_DIRECT),
//            key = {MqConst.ROUTING_CHECK_EMAIL}//路由key
//    ))
//    public void checkEmail(Contact contact, Message message, Channel channel) throws IOException {
//
//        String email = contact.getEmail();
//        Long groupId = contact.getGroupId();
//        String domain = email.split("@")[1];//获取域名
//        boolean check = true;
//        try {
//            //检查MX记录
//            Record[] records = new Lookup(domain, Type.MX).run();//查询MX记录
//            if (!(records != null && records.length > 0)) {//如果没有MX记录
//                check = false;
//            }else {
//                for (Record record : records) {//遍历MX记录
//                    MXRecord mxRecord = (MXRecord)record;//获取MX记录
//                    //连接
//                    Socket socket = new Socket(mxRecord.getTarget().toString(), 25);
//                    BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
//                    PrintWriter writer = new PrintWriter(socket.getOutputStream(), true) ;
//
//                    readResponse(reader);
//                    //握手
//                    sendCommand(writer, "HELO " + domain);
//                    readResponse(reader);
//                    //身份
//                    sendCommand(writer, "MAIL FROM:<verify@" + domain + ">");
//                    readResponse(reader);
//                    //验证
//                    sendCommand(writer, "RCPT TO:<" + email + ">");
//                    String response = readResponse(reader);
//
//                    // 断开
//                    if (response.startsWith("250") ) {
//                        sendCommand(writer, "QUIT");
//                        break;
//                    } else if (response.startsWith("550")) {
//                        sendCommand(writer, "QUIT");
//                        check = false;
//                    }
//                }
//            }
//            contactService.updateByEmail(email, groupId,contact.getUserId(), check ? 1 : 0);//更新状态
//        } catch (Exception e) {
//            contactService.updateByEmail(email, groupId,contact.getUserId(),1);
//            //如果抛异常，可能是无法连接到相应的SMTP服务器，无法用这种方式判断存不存在，则先按存在处理
//        }finally {
//            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
//        }
//
//    }
}
