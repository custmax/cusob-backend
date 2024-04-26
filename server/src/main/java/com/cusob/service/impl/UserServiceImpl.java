package com.cusob.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cusob.auth.AuthContext;
import com.cusob.constant.MqConst;
import com.cusob.constant.RedisConst;
import com.cusob.dto.ForgetPasswordDto;
import com.cusob.dto.UpdatePasswordDto;
import com.cusob.dto.UserDto;
import com.cusob.dto.UserLoginDto;
import com.cusob.entity.Company;
import com.cusob.entity.Email;
import com.cusob.entity.PlanPrice;
import com.cusob.entity.User;
import com.cusob.exception.CusobException;
import com.cusob.mapper.UserMapper;
import com.cusob.properties.JwtProperties;
import com.cusob.result.ResultCodeEnum;
import com.cusob.service.CompanyService;
import com.cusob.service.MailService;
import com.cusob.service.PlanPriceService;
import com.cusob.service.UserService;
import com.cusob.utils.JwtUtil;
import com.cusob.vo.UserLoginVo;
import com.cusob.vo.UserVo;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.TimeUnit;


@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private JwtProperties jwtProperties;

    @Autowired
    private MailService mailService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private CompanyService companyService;

    @Autowired
    private PlanPriceService planPriceService;

    @Value("${cusob.url}")
    private String baseUrl;

    @Resource
    private RabbitTemplate rabbitTemplate;

    /**
     * add User(User Register)
     * @param userDto
     */
    @Transactional
    @Override
    public void addUser(UserDto userDto) {
        this.paramEmptyVerify(userDto);
        this.registerVerify(userDto);

        String password = userDto.getPassword();
        User user = new User();
        BeanUtils.copyProperties(userDto, user);
        user.setCompanyId(0L); // default companyId=0
        // Password encryption
        user.setPassword(DigestUtils.md5DigestAsHex(password.getBytes()));
        user.setPermission(User.SUPER_ADMIN);
        user.setIsAvailable(User.DISABLE); // TODO 默认禁用
        baseMapper.insert(user);

        Company company = new Company();
        company.setCompanyName(userDto.getCompany());
        company.setAdminId(user.getId());
        company.setPlanId(PlanPrice.FREE); // default free plan
        companyService.saveCompany(company);

        user.setCompanyId(company.getId());
        baseMapper.updateById(user);

        rabbitTemplate.convertAndSend(MqConst.EXCHANGE_REGISTER_DIRECT,
                MqConst.ROUTING_REGISTER_SUCCESS, user.getId());
    }

    private void registerVerify(UserDto userDto) {
        String verifyCode = userDto.getVerifyCode();
        if (!StringUtils.hasText(verifyCode)){
            throw new CusobException(ResultCodeEnum.VERIFY_CODE_EMPTY);
        }
        String email = userDto.getEmail();
        String code = (String) redisTemplate.opsForValue().get(RedisConst.REGISTER_PREFIX + email);
        if (!verifyCode.equals(code)){
            throw new CusobException(ResultCodeEnum.VERIFY_CODE_WRONG);
        }

        User userDb = this.getUserByEmail(email);
        if (userDb != null){
            throw new CusobException(ResultCodeEnum.EMAIL_IS_REGISTERED);
        }
    }

    private void paramEmptyVerify(UserDto userDto) {
        if (!StringUtils.hasText(userDto.getEmail())){
            throw new CusobException(ResultCodeEnum.EMAIL_IS_EMPTY);
        }
        if(!StringUtils.hasText(userDto.getPhone())){
            throw new CusobException(ResultCodeEnum.PHONE_IS_EMPTY);
        }
    }

    /**
     * Register For Invited
     * @param userDto
     */
    @Override
    public void registerForInvited(UserDto userDto, String encode) {
        this.paramEmptyVerify(userDto);
        this.registerVerify(userDto);
        byte[] decode = Base64.getDecoder().decode(URLDecoder.decode(encode));
        String emailInviter = new String(decode);
        String key = RedisConst.INVITE_PREFIX + emailInviter;
        Set<String> members = redisTemplate.opsForSet().members(key);
        if (members==null){
            throw new CusobException(ResultCodeEnum.INVITE_LINK_INVALID);
        }
        if (!members.contains(userDto.getEmail())){
            throw new CusobException(ResultCodeEnum.EMAIL_NOT_INVITED);
        }

        User user = new User();
        BeanUtils.copyProperties(userDto, user);
        String password = userDto.getPassword();
        // Password encryption
        user.setPassword(DigestUtils.md5DigestAsHex(password.getBytes()));
        User inviter = this.getUserByEmail(emailInviter);
        user.setCompanyId(inviter.getCompanyId());
        user.setPermission(User.USER);
        user.setIsAvailable(User.DISABLE);  // todo default disable
        baseMapper.insert(user);
    }

    /**
     * add Admin
     * @param userId
     */
    @Override
    public void addAdmin(Long userId) {
        User user = baseMapper.selectById(userId);
        Integer permission = user.getPermission();
        if (!permission.equals(User.USER)){
            throw new CusobException(ResultCodeEnum.USER_IS_ADMIN);
        }
        user.setPermission(User.ADMIN);
        baseMapper.updateById(user);
    }

    /**
     * remove Admin
     * @param userId
     */
    @Override
    public void removeAdmin(Long userId) {
        Long id = AuthContext.getUserId();
        User user = baseMapper.selectById(id);
        Company company = companyService.getById(user.getCompanyId());
        // Only Super Admin can remove admin
        if (!company.getAdminId().equals(id)){
            throw new CusobException(ResultCodeEnum.NO_OPERATION_PERMISSIONS);
        }
        User toRemove = baseMapper.selectById(userId);
        if (!toRemove.getPermission().equals(User.ADMIN)){
            throw new CusobException(ResultCodeEnum.REMOVE_ADMIN_FAIL);
        }
        toRemove.setPermission(User.USER);
        baseMapper.updateById(toRemove);
    }

    /**
     * remove User
     * @param userId
     */
    @Override
    public void removeUser(Long userId) {

        Long adminId = AuthContext.getUserId();
        User admin = baseMapper.selectById(adminId);
        Integer permission = admin.getPermission();
        User toRemove = baseMapper.selectById(userId);
        Integer toRemovePermission = toRemove.getPermission();
        // Normal user can't remove others
        if (permission<=toRemovePermission){
            throw new CusobException(ResultCodeEnum.NO_OPERATION_PERMISSIONS);
        }
        baseMapper.deleteById(userId);
    }

    /**
     * get UserInfo by id
     * @return
     */
    @Override
    public UserVo getUserInfo() {
        Long userId = AuthContext.getUserId();
        User user = baseMapper.selectById(userId);
        UserVo userVo = new UserVo();
        BeanUtils.copyProperties(user, userVo);
        return userVo;
    }

    /**
     * update UserInfo
     * @param userVo
     */
    @Override
    public void updateUserInfo(UserVo userVo) {
        UserDto userDto = new UserDto();
        BeanUtils.copyProperties(userVo, userDto);
        this.paramEmptyVerify(userDto);
        User user = new User();
        BeanUtils.copyProperties(userVo, user);
        baseMapper.updateById(user);
    }

    /**
     * get UserList
     * @param pageParam
     * @return
     */
    @Override
    public IPage<User> getUserList(Page<User> pageParam) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        Long userId = AuthContext.getUserId();
        User user = baseMapper.selectById(userId);
        wrapper.eq(User::getCompanyId, user.getCompanyId());
        IPage<User> page = baseMapper.selectPage(pageParam, wrapper);
        return page;
    }

    /**
     * user login
     * @param userLoginDto
     * @return
     */
    @Override
    public UserLoginVo login(UserLoginDto userLoginDto) {
        String email = userLoginDto.getEmail();
        String password = userLoginDto.getPassword();

        // select user from table email
        User user = baseMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getEmail, email)
        );
        // Email is not registered
        if (user==null){
            throw new CusobException(ResultCodeEnum.EMAIL_NOT_EXIST);
        }
        // Password is wrong
        String psd = DigestUtils.md5DigestAsHex(password.getBytes());
        if (!psd.equals(user.getPassword())){
            throw new CusobException(ResultCodeEnum.PASSWORD_WRONG);
        }
        // todo User is Disable(During the internal test, you cannot log in temporarily)
        if (user.getIsAvailable().equals(User.DISABLE)){
            throw new CusobException(ResultCodeEnum.USER_IS_DISABLE);
        }

        // Generate token
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("companyId", user.getCompanyId());
        String token = JwtUtil.createJWT(jwtProperties.getSecretKey(), jwtProperties.getTtl(), claims);

        UserLoginVo userLoginVo = UserLoginVo.builder()
                .id(user.getId())
                .lastName(user.getLastName())
                .firstName(user.getFirstName())
                .avatar(user.getAvatar())
                .token(token)
                .build();

        return userLoginVo;
    }

    /**
     * update password
     * @param updatePasswordDto
     */
    @Override
    public void updatePassword(UpdatePasswordDto updatePasswordDto) {
        String oldPassword = updatePasswordDto.getOldPassword();
        // old password is empty
        if (!StringUtils.hasText(oldPassword)){
            throw new CusobException(ResultCodeEnum.OLD_PASSWORD_EMPTY);
        }
        String oldPsd = DigestUtils.md5DigestAsHex(oldPassword.getBytes());
        Long userId = AuthContext.getUserId();
        User user = baseMapper.selectById(userId);
        // old password is wrong
        if (!oldPsd.equals(user.getPassword())){
            throw new CusobException(ResultCodeEnum.OLD_PASSWORD_WRONG);
        }
        String newPassword = updatePasswordDto.getNewPassword();
        String newPsd = DigestUtils.md5DigestAsHex(newPassword.getBytes());
        user.setPassword(newPsd);
        baseMapper.updateById(user);
    }

    /**
     * send verify code for updating password
     */
    @Override
    public void sendCodeForPassword(String email) {
        User user = this.getUserByEmail(email);
        if (user==null){
            throw new CusobException(ResultCodeEnum.EMAIL_NOT_EXIST);
        }
        String code = String.valueOf((int)((Math.random() * 9 + 1) * Math.pow(10,5)));
        String subject = "Password Reset Instructions for Your Email Marketing Platform Account";
        // todo 待优化
        String content = "Hi " + user.getFirstName() + ",\n" +
                "Forgot your password?\n" +
                "We received a request to reset the password for your account.\n" +
                "To reset your password, copy the verification code below:\n" +
                code;
        String key = RedisConst.PASSWORD_PREFIX + email;
        redisTemplate.opsForValue().set(key, code, RedisConst.PASSWORD_TIMEOUT, TimeUnit.MINUTES);
        Email mail = new Email();
        mail.setEmail(email);
        mail.setSubject(subject);
        mail.setContent(content);
        rabbitTemplate.convertAndSend(MqConst.EXCHANGE_PASSWORD_DIRECT, MqConst.ROUTING_FORGET_PASSWORD, mail);
    }

    /**
     * get User By Email
     * @param email
     * @return
     */
    @Override
    public User getUserByEmail(String email) {
        User user = baseMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getEmail, email)
        );
        return user;
    }

    /**
     * send Email For Register Success
     */
    @Override
    public void sendEmailForRegisterSuccess(Long userId) {
        User user = baseMapper.selectById(userId);
        String email = user.getEmail();
        String subject = "Welcome to Our Email Marketing Platform! New User Guide";
        // todo 待优化
        String content = "Hi,\n" +
                "We are glad to you explore our suite of software and see how our technology stack can help you meet your business needs.\n" +
                "Our system is in internal testing and will be notified by email when it is officially released.\n" +
                "If you have any questions about your account, please feel free to contact us support@cusob.com\n" +
                "Thank you for using our services";
        mailService.sendTextMailMessage(email, subject, content);
    }

    /**
     * send Verify Code for signing up
     */
    @Override
    public void sendVerifyCode(String email) {
        if (!StringUtils.hasText(email)){
            throw new CusobException(ResultCodeEnum.EMAIL_IS_EMPTY);
        }
        String code = String.valueOf((int)((Math.random() * 9 + 1) * Math.pow(10,5)));
        String subject = code + " is your Cusob verification code";
        // todo 待优化
        String content = "<html><body><h1>Hello World!</h1></body></html>";
        Email mail = new Email();
        mail.setEmail(email);
        mail.setSubject(subject);
        mail.setContent(content);
        String key = RedisConst.REGISTER_PREFIX + email;
        // set verify code ttl 30 minutes
        redisTemplate.opsForValue().set(key, code, RedisConst.REGISTER_TIMEOUT, TimeUnit.MINUTES);
        rabbitTemplate.convertAndSend(MqConst.EXCHANGE_SIGN_DIRECT, MqConst.ROUTING_SEND_CODE, mail);
    }

    /**
     * forget password
     * @param forgetPasswordDto
     */
    @Override
    public void forgetPassword(ForgetPasswordDto forgetPasswordDto) {
        String email = forgetPasswordDto.getEmail();
        if (!StringUtils.hasText(email)){
            throw new CusobException(ResultCodeEnum.EMAIL_IS_EMPTY);
        }
        String verifyCode = forgetPasswordDto.getVerifyCode();
        if (!StringUtils.hasText(verifyCode)){
            throw new CusobException(ResultCodeEnum.VERIFY_CODE_EMPTY);
        }
        String password = forgetPasswordDto.getPassword();
        if (!StringUtils.hasText(password)){
            throw new CusobException(ResultCodeEnum.PASSWORD_IS_EMPTY);
        }
        String code = (String) redisTemplate.opsForValue().get(RedisConst.PASSWORD_PREFIX + email);
        if (!verifyCode.equals(code)){
            throw new CusobException(ResultCodeEnum.VERIFY_CODE_WRONG);
        }
        User user = this.getUserByEmail(email);
        if (user==null){
            throw new CusobException(ResultCodeEnum.EMAIL_NOT_EXIST);
        }
        user.setPassword(DigestUtils.md5DigestAsHex(password.getBytes()));
        baseMapper.updateById(user);
    }

    /**
     * Invite colleagues to join
     * @param email
     */
    @Override
    public void invite(String email) {
        Company company = companyService.getById(AuthContext.getCompanyId());
        // Free user
        if (company.getPlanId().equals(PlanPrice.FREE)){
            throw new CusobException(ResultCodeEnum.NO_PERMISSION);
        }

        Integer count = this.getUserCount();
        PlanPrice plan = planPriceService.getPlanById(company.getPlanId());
        String planName = plan.getName();

        Long userId = AuthContext.getUserId();
        User inviter = baseMapper.selectById(userId);
        String key = RedisConst.INVITE_PREFIX + inviter.getEmail();

        int num = 0;
        Long size = redisTemplate.opsForSet().size(key);
        if (size!=null){
            num += size.intValue();
        }
        // Essentials user
        if (planName.equals(PlanPrice.ESSENTIALS)){
            if (count + num >=PlanPrice.ESSENTIALS_USER_LIMIT){
                throw new CusobException(ResultCodeEnum.USER_NUMBER_FULL);
            }
        }
        // Standard user
        if (planName.equals(PlanPrice.STANDARD)){
            if (count + num >=PlanPrice.STANDARD_USER_LIMIT){
                throw new CusobException(ResultCodeEnum.USER_NUMBER_FULL);
            }
        }

        if (!StringUtils.hasText(email)){
            throw new CusobException(ResultCodeEnum.EMAIL_IS_EMPTY);
        }
        User user = this.getUserByEmail(email);
        if (user!=null){
            throw new CusobException(ResultCodeEnum.EMAIL_IS_REGISTERED);
        }

        String subject = inviter.getFirstName() + " " + inviter.getLastName() + " is inviting you to join the " +
                inviter.getCompany(); // todo 待优化
        String encode = Base64.getEncoder().encodeToString(inviter.getEmail().getBytes());
        String content = "Hi,\n" +
                subject +
                inviter.getCompany() + ".\n" +
                "Click the link below to join: \n" +
                baseUrl + "/signupByInvite?verifyCode=" + URLEncoder.encode(encode)
                ;
        redisTemplate.opsForSet().add(key, email);
        redisTemplate.expire(key, RedisConst.INVITE_TIMEOUT, TimeUnit.MINUTES);

        Email mail = new Email();
        mail.setEmail(email);
        mail.setSubject(subject);
        mail.setContent(content);
        rabbitTemplate.convertAndSend(MqConst.EXCHANGE_INVITE_DIRECT,
                MqConst.EXCHANGE_INVITE_DIRECT,
                mail
        );
    }

    /**
     * get User Count
     */
    private Integer getUserCount() {
        Long companyId = AuthContext.getCompanyId();
        Integer count = baseMapper.selectCount(
                new LambdaQueryWrapper<User>()
                        .eq(User::getCompanyId, companyId)
        );
        return count;
    }


}
