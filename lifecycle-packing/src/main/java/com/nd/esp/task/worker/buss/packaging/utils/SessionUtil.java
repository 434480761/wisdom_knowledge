package com.nd.esp.task.worker.buss.packaging.utils;

import java.util.HashMap;
import java.util.Map;

import com.nd.esp.task.worker.buss.packaging.Constant;
import com.nd.gaea.client.http.WafSecurityHttpClient;

public class SessionUtil {
    
    private static Map<String,Map<String,Object>> cacheSessions = new HashMap<String,Map<String,Object>>();
    
    /**
     * 调用cs接口获取session
     *
     * @param uid 用户id
     *
     * @return session id
     */
//    public static String createSession(String uid) {
//        return SessionUtil.createSession(uid, Constant.CS_INSTANCE_MAP.get(Constant.CS_DEFAULT_INSTANCE).getUrl(),
//                Constant.CS_INSTANCE_MAP.get(Constant.CS_DEFAULT_INSTANCE).getPath(),
//                Constant.CS_INSTANCE_MAP.get(Constant.CS_DEFAULT_INSTANCE).getServiceId());
//    }

    /**
     * 调用cs接口获取session
     *
     * @param uid 用户id
     * @param url 获取session的api
     * @param path 请求session的作用path
     * @param serviceId 服务id
     * 
     */
    public static String createSession(String uid, String url, String path, String serviceId) {
        if (cacheSessions.containsKey(uid + "@" + serviceId + path)) {
            Map<String, Object> sessionBefore = cacheSessions.get(uid + "@" + serviceId + path);
            long expireTime = Long.parseLong(String.valueOf(sessionBefore.get("expire_at")));
            if (expireTime - System.currentTimeMillis() > 6000000) {
                return String.valueOf(sessionBefore.get("session"));
            }
        }
        Map<String, Object> requestBody = new HashMap<String, Object>();
        requestBody.put("path", path);
        requestBody.put("service_id", serviceId);
        requestBody.put("uid",uid);
        requestBody.put("role",Constant.FILE_OPERATION_ROLE);
        requestBody.put("expires",Constant.FILE_OPERATION_EXPIRETIME);
        WafSecurityHttpClient wafSecurityHttpClient = new WafSecurityHttpClient();
        url = url + "/sessions";
        Map<String, Object> session = wafSecurityHttpClient.postForObject( url, requestBody, Map.class);
        cacheSessions.put(uid+"@"+serviceId+path, session);
        return String.valueOf(session.get("session"));
    }
    /**   
     * @desc:获取Href中对应的cs实例key  ${ref-path}/edu
     * @createtime: 2015年6月25日 
     * @author: liuwx 
     * @param href
     * @see Constant.CSInstanceInfo
     * @see Constant#CS_INSTANCE_MAP
     * @return
     */
    public static String getHrefInstanceKey(String href){
        if(StringUtils.isEmpty(href)){
            throw new IllegalArgumentException("href必须不为空");
        }
   
        int secondSlash=href.indexOf("/", href.indexOf("/")+1);
        return href.substring(0, secondSlash);
    }
}
