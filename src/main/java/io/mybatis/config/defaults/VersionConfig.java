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
import io.mybatis.config.ConfigHelper;
import io.mybatis.config.util.ResourceUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 形如 mybais-provider-v1.0.properties 带版本号的属性配置文件
 * <p>
 * 假设存在下面的配置文件
 * <ol>
 *   <li>mybais-provider-v1.0.properties</li>
 *   <li>mybais-provider-v1.5.properties</li>
 *   <li>mybais-provider-v2.0.properties</li>
 * </ol>
 * 当某个版本修改配置文件默认值时，为了兼容低版本，提供一个新版本的配置文件修改默认值，用户可以通过指定版本选择使用某个版本的配置文件。
 * <p>
 * 在示例中，mybais-provider 对应 {@link #getConfigName()}，是配置文件前缀。
 * <p>
 * 其中 {@link #getVersionKey()} 的值类似 provider.version，当通过 provider.version=v1.5 指定版本时，就会使用 mybais-provider-v1.5.properties 配置。
 * <p>
 * 当指定的版本介于两个版本之间时，使用低版本。版本不在范围时，使用就近的版本。
 * <p>
 * 例如 v2.1, v2.0 时使用 v2.0 配置，v1.3, v1.0 使用 v1.0 配置，v0.9 时超出范围，使用就近版本 v1.0
 *
 * @author liuzh
 */
public abstract class VersionConfig implements Config {
  public static final String     FILE_TYPE = ".properties";
  protected volatile  Properties properties;

  @Override
  public int getOrder() {
    return VERSION_ORDER;
  }

  /**
   * 获取配置文件名前缀
   */
  protected abstract String getConfigName();

  /**
   * 获取版本号对应的 key
   */
  protected abstract String getVersionKey();

  /**
   * 获取版本配置文件所在路径
   */
  protected String getConfigPath() {
    return getClass().getPackage().getName().replaceAll("\\.", "/");
  }

  /**
   * 跳过读取指定的 key
   *
   * @param key 属性
   */
  protected boolean skipKey(String key) {
    return getVersionKey().equals(key);
  }

  /**
   * 初始化
   */
  protected void init() {
    Properties props = getVersionProperties();
    if (props != null) {
      this.properties = props;
    } else {
      this.properties = new Properties();
    }
  }

  /**
   * 获取所有版本配置文件
   */
  protected File[] getConfigFiles() {
    String configName = getConfigName();
    String configPath = getConfigPath();

    ClassLoader cl = ResourceUtil.getDefaultClassLoader();
    URL resource = cl != null ? cl.getResource(configPath) : ClassLoader.getSystemResource(configPath);
    if (resource == null) {
      return null;
    }
    //TODO 测试 jar 包中是否正常
    return new File(resource.getPath()).listFiles((dir, name) -> name.startsWith(configName));
  }

  /**
   * 获取选择的配置文件
   *
   * @param files   可选配置文件
   * @param version 选择的版本号
   */
  protected File chooseConfig(File[] files, String version) {
    if (files == null || files.length == 0) {
      return null;
    }
    List<File> list = Arrays.stream(files).sorted(Comparator.comparing(File::getName).reversed()).collect(Collectors.toList());
    File file = null;
    if (version == null || version.isEmpty()) {
      file = list.get(0);
    } else {
      String configName = getConfigName();
      Pattern pattern = Pattern.compile(configName + "-(v\\d+\\.\\d+)\\" + FILE_TYPE);
      ConfigVersion chooseConfigVersion = new ConfigVersion(version);
      for (File f : list) {
        String name = f.getName();
        Matcher matcher = pattern.matcher(name);
        if (matcher.find()) {
          String v = matcher.group(1);
          if (chooseConfigVersion.compareTo(new ConfigVersion(v)) >= 0) {
            file = f;
            break;
          }
        }
      }
      if (file == null) {
        file = list.get(list.size() - 1);
      }
    }
    return file;
  }

  /**
   * 获取版本配置
   */
  protected Properties getVersionProperties() {
    //所有版本的配置文件
    File[] files = getConfigFiles();
    //指定的版本
    String version = ConfigHelper.getStr(getVersionKey());
    //选择指定版本的配置文件
    File file = chooseConfig(files, version);
    if (file == null) {
      return null;
    }
    Properties props = new Properties();
    try (InputStream is = new FileInputStream(file)) {
      props.load(is);
    } catch (IOException e) {
      throw new RuntimeException("file: " + file + " not exists", e);
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

  /**
   * 版本比对
   */
  public static class ConfigVersion implements Comparable<ConfigVersion> {
    private final int x;
    private final int y;

    public ConfigVersion(String version) {
      if (version.startsWith("v")) {
        version = version.substring(1);
      }
      String[] strings = version.split("\\.");
      this.x = Integer.parseInt(strings[0]);
      this.y = Integer.parseInt(strings[1]);
    }

    @Override
    public int compareTo(ConfigVersion o) {
      if (this.x == o.x) {
        return this.y - o.y;
      }
      return this.x - o.x;
    }
  }
}
