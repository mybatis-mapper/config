/*
 * Copyright 2020-2022 the original author or authors.
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

package io.mybatis.config;

import org.junit.Assert;
import org.junit.Test;

public class ConfigHelperTest {

  @Test
  public void testGetProperty() {
    String name = "config-test.name";

    Assert.assertEquals("v2.0", ConfigHelper.getStr(name));
    Assert.assertNotNull(ConfigHelper.getStr("user.dir"));

    Assert.assertEquals("测试代码", ConfigHelper.getStr("desc"));


    System.setProperty("config-test.properties", "config-test-user.properties");
    System.setProperty("config-test.version", "v1.1");

    ConfigHelper.reload();

    Assert.assertEquals("custom", ConfigHelper.getStr(name));
    Assert.assertNull(ConfigHelper.getStr("desc"));

    System.clearProperty("config-test.properties");
    System.clearProperty("config-test.version");
  }
}