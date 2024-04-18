package com.cusob.auth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

@Component
public class LoginMvcConfigurerAdapter extends WebMvcConfigurationSupport {

    @Autowired
    private UserLoginInterceptor userLoginInterceptor;

    /**
     * add Interceptors
     * @param registry
     */
    protected void addInterceptors(InterceptorRegistry registry) {

        // 对swagger的请求不进行拦截 todo 可修改
        String[] excludePatterns = new String[]{"/swagger-resources/**", "/webjars/**", "/v2/**", "/swagger-ui.html/**",
                "/api", "/api-docs", "/api-docs/**", "/doc.html/**"};

        registry.addInterceptor(userLoginInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/user/login")
                .excludePathPatterns("/user/register")
                .excludePathPatterns("/user/sendVerifyCode")
                .excludePathPatterns("/user/forgetPassword")
                .excludePathPatterns("/user/sendCodeForPassword")
                .excludePathPatterns("/user/registerForInvited")
                .excludePathPatterns("/plan/price/**")
                .excludePathPatterns("/read/**")
                .excludePathPatterns("/book/**")
                .excludePathPatterns("/unsubscribe/**")
                .excludePathPatterns("/captcha/**")
                .excludePathPatterns(excludePatterns)
        ;
    }

    /**
     * 设置静态资源映射
     * @param registry
     */
    protected void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**").addResourceLocations("classpath:/static/");
        registry.addResourceHandler("/doc.html").addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("/webjars/**").addResourceLocations("classpath:/META-INF/resources/webjars/");
    }
}
