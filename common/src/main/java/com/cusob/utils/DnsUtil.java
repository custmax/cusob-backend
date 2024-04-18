package com.cusob.utils;

import org.xbill.DNS.*;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class DnsUtil {

    public static List<String> checkSpf(String domain){
        try {
            Lookup lookup = new Lookup(domain, Type.TXT);
            lookup.setResolver(new SimpleResolver("8.8.8.8")); // 使用Google的公共DNS服务器，你也可以使用其他DNS服务器
            Record[] records = lookup.run();
            List<String> spfTxtRecords = new ArrayList<>();
            if (records==null || records.length==0){
                return null;
            }
            for (Record record : records) {
                if (record instanceof TXTRecord) {
                    TXTRecord txtRecord = (TXTRecord) record;
                    List<String> txts = txtRecord.getStrings();
                    for (String txt : txts) {
                        if (txt.startsWith("v=spf1")) { // SPF记录通常以"v=spf1"开头
                            spfTxtRecords.add(txt);
                        }
                    }
                }
            }
            return spfTxtRecords;
        } catch (TextParseException | UnknownHostException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static List<String> checkDkim(String domain){
        // todo selector may update
        String selector = "s1"; // DKIM选择器，通常与你的邮件服务器配置相关 自定义
        try {
            // 构造DKIM记录的查询名称，通常是"<selector>._domainkey.<domain>"
            String dkimRecordName = selector + "."  + "_domainkey."+ domain;
            Lookup lookup = new Lookup(dkimRecordName, Type.TXT);
            lookup.setResolver(new SimpleResolver("8.8.8.8")); // 使用Google的公共DNS服务器
            Record[] records = lookup.run();
            if (records==null || records.length==0){
                return null;
            }
            List<String> dkimTxtRecords = new ArrayList<>();
            for (Record record : records) {
                if (record instanceof TXTRecord) {
                    TXTRecord txtRecord = (TXTRecord) record;
                    List<String> txts = txtRecord.getStrings();
                    for (String txt : txts) {
                        // DKIM记录可能包含特定的格式，但通常直接包含公钥信息
                        dkimTxtRecords.add(txt);
                    }
                }
            }

            return dkimTxtRecords;
        } catch (TextParseException | UnknownHostException e) {
            e.printStackTrace();
            return null;
        }
    }


    public static void main(String[] args) {
        String domain = "daybreakhust.top";
        System.out.println(checkDkim(domain).get(0));
    }
}
