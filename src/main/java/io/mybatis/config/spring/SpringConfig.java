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
