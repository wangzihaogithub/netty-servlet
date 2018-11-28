/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.netty.springboot.server;

import com.github.netty.springboot.NettyProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

/**
 * servlet容器自动配置
 * @author 84215
 */
@Configuration
public class NettyEmbeddedAutoConfiguration {

    @Bean("nettyServerFactory")
    @DependsOn("nettyProperties")
    @ConditionalOnMissingBean(NettyProperties.class)
    public NettyEmbeddedServletContainerFactory nettyEmbeddedServletContainerFactory(@Qualifier("nettyProperties") NettyProperties nettyProperties){
        return new NettyEmbeddedServletContainerFactory(nettyProperties);
    }

}
