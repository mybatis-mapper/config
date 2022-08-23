/*
 * Copyright 2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.mybatis.config.spring;

import io.mybatis.config.Config;

/**
 * 支持 Spring 方式的属性配置，由于依赖 Spring EnvironmentAware 接口，当没有初始化时，过早执行时无法获取 Spring 配置
 */
public class SpringConfig implements Config {
  /**
   * 跳过当前方法获取
   */
  private volatile boolean skip = false;

  @Override
  public int getOrder() {
    return SPRING_ORDER;
  }

  @Override
  public String getStr(String key) {
    try {
      return skip ? null : SpringEnvUtil.getStr(key);
    } catch (NoClassDefFoundError e) {
      //找不到类时，说明不是 Spring 运行环境
      skip = true;
      return null;
    }
  }
}
