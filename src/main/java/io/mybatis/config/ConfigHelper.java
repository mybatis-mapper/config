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

package io.mybatis.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;

/**
 * 配置工具类，按照优先级顺序获取配置值，参考 {@link Config}
 *
 * @author liuzh
 */
public class ConfigHelper {
  public static final Logger log = LoggerFactory.getLogger(ConfigHelper.class);

  /**
   * 所有配置实现
   */
  private static volatile List<Config> CONFIGS;

  /**
   * 获取配置信息
   *
   * @param key 配置键
   */
  public static String getStr(String key) {
    init();
    for (Config config : CONFIGS) {
      String value = config.getStr(key);
      if (value != null) {
        return value;
      }
    }
    return null;
  }

  /**
   * 获取配置信息
   *
   * @param key          配置键
   * @param defaultValue 默认值
   * @return 配置值
   */
  public static String getStr(String key, String defaultValue) {
    String val = getStr(key);
    if (val == null) {
      return defaultValue;
    }
    return val;
  }

  /**
   * 获取配置信息
   *
   * @param key 配置键
   * @return 配置值
   */
  public static Integer getInt(String key) {
    String val = getStr(key);
    return val == null ? null : Integer.parseInt(val);
  }

  /**
   * 获取配置信息
   *
   * @param key          配置键
   * @param defaultValue 默认值
   * @return 配置值
   */
  public static Integer getInt(String key, Integer defaultValue) {
    Integer val = getInt(key);
    return val != null ? val : defaultValue;
  }

  /**
   * 获取配置信息
   *
   * @param key 配置键
   * @return 配置值
   */
  public static boolean getBoolean(String key) {
    return Boolean.valueOf(getStr(key));
  }

  /**
   * 获取配置信息
   *
   * @param key          配置键
   * @param defaultValue 默认值
   * @return 配置值
   */
  public static boolean getBoolean(String key, boolean defaultValue) {
    String val = getStr(key);
    if (val == null) {
      return defaultValue;
    }
    return Boolean.valueOf(val);
  }

  /**
   * 初始化
   */
  private static void init() {
    if (CONFIGS == null) {
      synchronized (ConfigHelper.class) {
        if (CONFIGS == null) {
          CONFIGS = new ArrayList<>();
          ServiceLoader<Config> serviceLoader = ServiceLoader.load(Config.class);
          for (Config config : serviceLoader) {
            CONFIGS.add(config);
          }
          CONFIGS.sort(Comparator.comparing(Config::getOrder).reversed());
          CONFIGS.forEach(c -> log.debug("加载配置类: " + c.getClass().getName()));
        }
      }
    }
  }

  /**
   * 重新加载
   */
  public static void reload() {
    CONFIGS = null;
    init();
  }

}
