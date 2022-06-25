package io.mybatis.config.custom;

import io.mybatis.config.defaults.VersionConfig;

public class TestVersionConfig extends VersionConfig {

  @Override
  protected String getConfigName() {
    return "mybatis-config-test";
  }

  @Override
  protected String getVersionKey() {
    return "config-test.version";
  }

}
