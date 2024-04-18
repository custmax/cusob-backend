package com.cusob.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cusob.constant.MqConst;
import com.cusob.dto.BookDto;
import com.cusob.entity.Book;
import com.cusob.exception.CusobException;
import com.cusob.mapper.BookMapper;
import com.cusob.result.ResultCodeEnum;
import com.cusob.service.BookService;
import com.cusob.service.MailService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;

@Service
public class BookServiceImpl extends ServiceImpl<BookMapper, Book> implements BookService {

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private MailService mailService;

    @Value("${cusob.email.brooks}")
    private String mailBrooks;

    @Value("${cusob.email.daybreak}")
    private String mailDaybreak;

    /**
     * book a demo
     * @param bookDto
     */
    @Override
    public void bookDemo(BookDto bookDto) {
        this.paramVerify(bookDto);
        Book book = new Book();
        BeanUtils.copyProperties(bookDto, book);
        baseMapper.insert(book);
        rabbitTemplate.convertAndSend(MqConst.EXCHANGE_BOOK_DIRECT,
                MqConst.ROUTING_BOOK_DEMO, book);
    }

    /**
     * email Notify
     * @param book
     */
    @Override
    public void emailNotify(Book book) {
        String name = book.getName();
        String subject = name + " is booking a Demo";
        name = "name: " + name;
        String email = "email: " + book.getEmail();
        String phone = "phone: " + book.getPhone();
        String message = "message: " + book.getMessage();
        String content = name + "\n" + email + "\n" + phone + "\n" + message;
        String mail = mailDaybreak + "," + mailBrooks;
        mailService.sendTextMailMessage(mail, subject, content);
    }

    private void paramVerify(BookDto bookDto) {
        if (!StringUtils.hasText(bookDto.getName())){
            throw new CusobException(ResultCodeEnum.NAME_IS_EMPTY);
        }
        if (!StringUtils.hasText(bookDto.getEmail())){
            throw new CusobException(ResultCodeEnum.EMAIL_IS_EMPTY);
        }
        if (!StringUtils.hasText(bookDto.getPhone())){
            throw new CusobException(ResultCodeEnum.PHONE_IS_EMPTY);
        }
    }
}
