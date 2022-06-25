package io.mybatis.config.custom;

import io.mybatis.config.Config;
import org.junit.Assert;
import org.junit.Test;

public class CustomTest {

  @Test
  public void testUser() {
    Config config = new TestUserConfig();
    String desc = config.getStr("config-test.name");
    Assert.assertNull(desc);

    System.setProperty("config-test.properties", "config-test-user.properties");
    config = new TestUserConfig();
    desc = config.getStr("config-test.name");
    Assert.assertEquals("custom", desc);
    System.clearProperty("config-test.properties");
  }

  @Test
  public void testVersion() {
    Config config = new TestVersionConfig();
    String property = config.getStr("config-test.name");
    String desc = config.getStr("desc");
    Assert.assertEquals("v2.0", property);
    Assert.assertEquals("测试代码", desc);


    System.setProperty("config-test.version", "v1.1");
    config = new TestVersionConfig();
    property = config.getStr("config-test.name");
    desc = config.getStr("desc");
    Assert.assertEquals("v1.0", property);
    Assert.assertNull(desc);
    System.clearProperty("config-test.version");
  }

}
