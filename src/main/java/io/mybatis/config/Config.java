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

/**
 * 获取配置信息，默认系统变量高于环境变量设置。
 * <p>
 * 推荐的优先级顺序为 spring > system > env > 自定义配置文件.properties
 *
 * @author liuzh
 */
public interface Config {
  /**
   * 低优先级
   */
  int LOW_ORDER = 0;

  /**
   * 版本配置
   */
  int VERSION_ORDER = 100;

  /**
   * 用户配置
   */
  int USER_ORDER = 200;

  /**
   * 环境变量
   */
  int ENV_ORDER = 300;

  /**
   * 系统变量
   */
  int SYSTEM_ORDER = 400;

  /**
   * Spring 配置优先级更高，并且包含了 Spring 的环境变量、系统变量、运行参数
   */
  int SPRING_ORDER = 500;

  /**
   * 高优先级
   */
  int HIGH_ORDER = 1000;

  /**
   * @return 执行顺序，数字越大优先级越高，越早执行
   */
  default int getOrder() {
    return 0;
  }

  /**
   * 获取配置信息
   *
   * @param key 配置键
   */
  String getStr(String key);

  /**
   * 获取配置信息
   *
   * @param key          配置键
   * @param defaultValue 默认值
   * @return 配置值
   */
  default String getStr(String key, String defaultValue) {
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
  default Integer getInt(String key) {
    String val = getStr(key);
    if (val == null) {
      return null;
    }
    return Integer.parseInt(val);
  }

  /**
   * 获取配置信息
   *
   * @param key          配置键
   * @param defaultValue 默认值
   * @return 配置值
   */
  default Integer getInt(String key, Integer defaultValue) {
    Integer val = getInt(key);
    return val != null ? val : defaultValue;
  }


  /**
   * 获取配置信息
   *
   * @param key 配置键
   * @return 配置值
   */
  default boolean getBoolean(String key) {
    return Boolean.valueOf(getStr(key));
  }

  /**
   * 获取配置信息
   *
   * @param key          配置键
   * @param defaultValue 默认值
   * @return 配置值
   */
  default boolean getBoolean(String key, boolean defaultValue) {
    String val = getStr(key);
    if (val == null) {
      return defaultValue;
    }
    return Boolean.valueOf(val);
  }

}
