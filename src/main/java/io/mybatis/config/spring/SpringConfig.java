package io.mybatis.config.spring;

import io.mybatis.config.Config;

/**
 * 支持 Spring 方式的属性配置
 */
public class SpringConfig implements Config {
  @Override
  public int getOrder() {
    return SPRING_ORDER;
  }

  @Override
  public String getStr(String key) {
    return null;
  }
}
