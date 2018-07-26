package com.github.netty.servlet;

import javax.servlet.*;
import java.util.*;

/**
 *
 * @author acer01
 *  2018/7/14/014
 */
public class ServletRegistration implements javax.servlet.ServletRegistration, javax.servlet.ServletRegistration.Dynamic {

    private String servletName;
    private Servlet servlet;
    private ServletConfig servletConfig;
    private ServletContext servletContext;
    private Map<String,String> initParameterMap;
    private ServletRegistration self;
    private Set<String> mappingSet;
    private boolean asyncSupported;

    public ServletRegistration(String servletName, Servlet servlet,ServletContext servletContext) {
        this.servletName = servletName;
        this.servlet = servlet;
        this.servletContext = servletContext;
        this.initParameterMap = new HashMap<>();
        this.mappingSet = new HashSet<>();
        this.asyncSupported = false;
        this.self = this;

        this.servletConfig = new ServletConfig() {
            @Override
            public String getServletName() {
                return self.servletName;
            }

            @Override
            public javax.servlet.ServletContext getServletContext() {
                return self.servletContext;
            }

            @Override
            public String getInitParameter(String name) {
                return self.getInitParameter(name);
            }

            @Override
            public Enumeration<String> getInitParameterNames() {
                return Collections.enumeration(self.getInitParameters().keySet());
            }
        };
    }

    public ServletConfig getServletConfig() {
        return servletConfig;
    }

    public Servlet getServlet() {
        return servlet;
    }

    public boolean isAsyncSupported() {
        return asyncSupported;
    }

    @Override
    public Set<String> addMapping(String... urlPatterns) {
        mappingSet.addAll(Arrays.asList(urlPatterns));
        for(String pattern : urlPatterns) {
            try {
                servletContext.addServletMapping(pattern,getName(),servlet);
            } catch (ServletException e) {
                e.printStackTrace();
            }
        }
        return mappingSet;
    }

    @Override
    public Collection<String> getMappings() {
        return mappingSet;
    }

    @Override
    public String getRunAsRole() {
        return null;
    }

    @Override
    public String getName() {
        return servletName;
    }

    @Override
    public String getClassName() {
        return servlet.getClass().getName();
    }

    @Override
    public boolean setInitParameter(String name, String value) {
        return false;
    }

    @Override
    public String getInitParameter(String name) {
        return initParameterMap.get(name);
    }

    @Override
    public Set<String> setInitParameters(Map<String, String> initParameters) {
        this.initParameterMap = initParameters;
        return initParameterMap.keySet();
    }

    @Override
    public Map<String, String> getInitParameters() {
        return initParameterMap;
    }

    //==============

    @Override
    public void setLoadOnStartup(int loadOnStartup) {

    }

    @Override
    public Set<String> setServletSecurity(ServletSecurityElement constraint) {
        return null;
    }

    @Override
    public void setMultipartConfig(MultipartConfigElement multipartConfig) {

    }

    @Override
    public void setRunAsRole(String roleName) {

    }

    @Override
    public void setAsyncSupported(boolean isAsyncSupported) {
        this.asyncSupported = isAsyncSupported;
    }
}
