package com.github.netty.springboot;

import com.github.netty.core.util.ReflectUtil;
import com.github.netty.core.util.StringUtil;
import com.github.netty.rpc.exception.RpcException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Proxy;

/**
 * RPC客户端工厂类
 * @author 84215
 */
public class NettyRpcClientFactoryBean implements FactoryBean<Object>, InitializingBean,ApplicationContextAware {

    private Logger logger = LoggerFactory.getLogger(getClass());
    private Class<?> objectType;
    private Class<?> fallback;
    private ApplicationContext applicationContext;
    private String serviceId;
    private ClassLoader classLoader;

    @Override
    public Object getObject() throws Exception {
        NettyProperties nettyConfig = applicationContext.getBean(NettyProperties.class);
        NettyRpcLoadBalanced loadBalanced = applicationContext.getBean(NettyRpcLoadBalanced.class);
        String serviceName = getServiceName();

        NettyRpcClientProxy nettyRpcClientProxy = new NettyRpcClientProxy(serviceId,serviceName,objectType,nettyConfig,loadBalanced);
        Object instance = Proxy.newProxyInstance(classLoader,new Class[]{objectType},nettyRpcClientProxy);

        try {
            nettyRpcClientProxy.pingOnceAfterDestroy();
        }catch (RpcException e){
            logger.error("无法连接至远程地址 " + e.toString());
        }
        return instance;
    }

    private String getServiceName(){
        RequestMapping requestMapping = ReflectUtil.findAnnotation(objectType,RequestMapping.class);
        String serviceName = null;
        if(requestMapping != null) {
            String[] serviceNames = requestMapping.value();
            if(serviceNames.length > 0){
                serviceName = serviceNames[0];
            }
            if(serviceName == null || serviceName.isEmpty()){
                serviceNames = requestMapping.path();
            }
            if(serviceNames.length > 0){
                serviceName = serviceNames[0];
            }
        }
        if(serviceName == null || serviceName.isEmpty()){
            serviceName = "/"+StringUtil.firstLowerCase(objectType.getSimpleName());
        }
        return serviceName;
    }

    @Override
    public Class<?> getObjectType() {
        return objectType;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.isTrue(serviceId!= null && serviceId.length() > 0,"服务ID不能为空");
    }

    public Class<?> getFallback() {
        return fallback;
    }

    public void setFallback(Class<?> fallback) {
        this.fallback = fallback;
    }

    public void setObjectType(Class<?> objectType) {
        this.objectType = objectType;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

}
