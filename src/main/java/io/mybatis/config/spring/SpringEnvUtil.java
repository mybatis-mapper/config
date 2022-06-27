package io.mybatis.config.spring;

import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;

/**
 * Spring 环境配置工具类，Spring boot 时自动注册，纯 Spring 时需要自己配置才能生效
 *
 * @author liuzh
 */
public class SpringEnvUtil implements EnvironmentAware {
  private static Environment environment;

  @Override
  public void setEnvironment(Environment environment) {
    SpringEnvUtil.environment = environment;
  }

  public static String getStr(String key) {
    return environment != null ? SpringEnvUtil.environment.getProperty(key) : null;
  }
}
