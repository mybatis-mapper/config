package io.mybatis.config.custom;

import io.mybatis.config.defaults.UserConfig;

public class TestUserConfig extends UserConfig {

  @Override
  protected String getConfigKey() {
    return "config-test.properties";
  }

  @Override
  protected String getConfigName() {
    return "mybatis-config-test";
  }

}
