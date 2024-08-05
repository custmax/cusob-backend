package com.cusob.auth;


public class AuthContext {

    private static ThreadLocal<Long> userId = new ThreadLocal<>();

    private static ThreadLocal<Long> companyId = new ThreadLocal<>();

    public static Long getUserId(){
        return userId.get();
    }

    public static void setUserId(Long _userId){
        userId.set(_userId);
    }

    public static Long getCompanyId(){
        return companyId.get();
    }

    public static void setCompanyId(Long _companyId){
        companyId.set(_companyId);


    }
}
