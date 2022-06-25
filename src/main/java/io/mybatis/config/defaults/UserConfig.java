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

package io.mybatis.config.defaults;

import io.mybatis.config.Config;
import io.mybatis.config.util.ResourceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * 用户配置
 *
 * @author liuzh
 */
public abstract class UserConfig implements Config {
  public static final Logger     log       = LoggerFactory.getLogger(UserConfig.class);
  public static final String     FILE_TYPE = ".properties";
  protected volatile  Properties properties;

  @Override
  public int getOrder() {
    return USER_ORDER;
  }

  /**
   * 获取文件名对应的 key
   */
  protected abstract String getConfigKey();

  /**
   * 获取默认配置名
   */
  protected abstract String getConfigName();

  /**
   * 跳过读取指定的 key
   *
   * @param key 属性
   */
  protected boolean skipKey(String key) {
    return getConfigKey().equals(key);
  }

  /**
   * 初始化
   */
  protected void init() {
    Properties props = getUserProperties();
    if (props != null) {
      this.properties = props;
    } else {
      this.properties = new Properties();
    }
  }

  /**
   * 获取用户配置文件
   */
  protected Properties getUserProperties() {
    String requestedFile = System.getProperty(getConfigKey());
    String propFileName = requestedFile != null ? requestedFile : getConfigName();
    if (!propFileName.endsWith(FILE_TYPE)) {
      propFileName += FILE_TYPE;
    }
    // 用户目录下面的配置（指定或默认）
    File file = new File(propFileName);
    if (!file.exists()) {
      if (requestedFile != null) {
        // 类路径下的配置，指定的不存在就报错
        try {
          file = ResourceUtil.getFile(requestedFile);
        } catch (FileNotFoundException e) {
          log.warn("指定的用户配置文件: " + requestedFile + " 不存在");
        }
        try {
          file = ResourceUtil.getClasspathFile(requestedFile);
        } catch (FileNotFoundException e) {
          log.warn("指定的用户配置文件在类路径下: " + requestedFile + " 不存在");
        }
      } else {
        // 默认文件，非用户指定时
        try {
          file = ResourceUtil.getClasspathFile(propFileName);
        } catch (FileNotFoundException e) {
          try {
            file = ResourceUtil.getClasspathFile("/" + propFileName);
          } catch (FileNotFoundException e2) {
            try {
              // 包下面的配置
              String path = getClass().getPackage().getName().replaceAll("\\.", "/");
              file = ResourceUtil.getClasspathFile(path + "/" + propFileName);
            } catch (FileNotFoundException ignored) {

            }
          }
        }
      }
    }
    Properties props = new Properties();
    if (file.exists()) {
      try (InputStream in = new FileInputStream(file)) {
        props.load(in);
      } catch (IOException ignored) {

      }
    }
    return props;
  }

  @Override
  public String getStr(String key) {
    if (skipKey(key)) {
      return null;
    }
    if (this.properties == null) {
      synchronized (this) {
        if (this.properties == null) {
          this.init();
        }
      }
    }
    return properties.getProperty(key);
  }

}
