package com.github.netty.servlet;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author acer01
 * @date 2018/7/14/014
 */
public class ServletFilterRegistration implements FilterRegistration,FilterRegistration.Dynamic {

    private String filterName;
    private Filter filter;

    public ServletFilterRegistration(String filterName, Filter filter) {
        this.filterName = filterName;
        this.filter = filter;
    }

    public Filter getFilter() {
        return filter;
    }

    @Override
    public void addMappingForServletNames(EnumSet<DispatcherType> dispatcherTypes, boolean isMatchAfter, String... servletNames) {

    }

    @Override
    public Collection<String> getServletNameMappings() {
        return null;
    }

    @Override
    public void addMappingForUrlPatterns(EnumSet<DispatcherType> dispatcherTypes, boolean isMatchAfter, String... urlPatterns) {

    }

    @Override
    public Collection<String> getUrlPatternMappings() {
        return null;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public String getClassName() {
        return null;
    }

    @Override
    public boolean setInitParameter(String name, String value) {
        return false;
    }

    @Override
    public String getInitParameter(String name) {
        return null;
    }

    @Override
    public Set<String> setInitParameters(Map<String, String> initParameters) {
        return null;
    }

    @Override
    public Map<String, String> getInitParameters() {
        return null;
    }

    @Override
    public void setAsyncSupported(boolean isAsyncSupported) {

    }
}
