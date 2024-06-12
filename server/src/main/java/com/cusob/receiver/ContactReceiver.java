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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
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
            key = {MqConst.ROUTING_CHECK_EMAIL}
    ))
    public void checkEmail(Contact contact, Message message, Channel channel) throws IOException {

        String email = contact.getEmail();
        Long groupId = contact.getGroupId();
        String domain = email.split("@")[1];
        boolean check = true;
        try {
            //检查MX记录
            Record[] records = new Lookup(domain, Type.MX).run();
            if (!(records != null && records.length > 0)) {
                check = false;
            }else {
                for (Record record : records) {
                    MXRecord mxRecord = (MXRecord)record;
                    Socket socket = new Socket(mxRecord.getTarget().toString(), 25);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    PrintWriter writer = new PrintWriter(socket.getOutputStream(), true) ;

                    readResponse(reader);
                    sendCommand(writer, "HELO " + domain);
                    readResponse(reader);
                    sendCommand(writer, "MAIL FROM:<verify@" + domain + ">");
                    readResponse(reader);
                    sendCommand(writer, "RCPT TO:<" + email + ">");
                    String response = readResponse(reader);

                    // Check for 250 or 550 response code
                    if (response.startsWith("250")) {
                        sendCommand(writer, "QUIT");
                        break;
                    } else if (response.startsWith("550")) {
                        sendCommand(writer, "QUIT");
                        check = false;
                    }
                }
            }
            contactService.updateByEmail(email, groupId,contact.getUserId(), check ? 1 : 0);
        } catch (Exception e) {
            contactService.updateByEmail(email, groupId,contact.getUserId(),0);
        }finally {
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        }

    }
}
