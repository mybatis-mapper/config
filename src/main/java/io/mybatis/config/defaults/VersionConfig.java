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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    return getClass().getPackage().getName().replaceAll("\\.", "/") + "/";
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
   * 选择版本
   *
   * @param versions 所有版本
   * @param version  选择的版本
   */
  private String chooseVersion(List<String> versions, String version) {
    if (versions == null || versions.isEmpty()) {
      return null;
    }
    Collections.sort(versions);
    if (version == null || version.isEmpty()) {
      return versions.get(versions.size() - 1);
    }
    ConfigVersion configVersion = new ConfigVersion(version);
    Pattern pattern = Pattern.compile(getConfigName() + "-(v\\d+\\.\\d+)\\" + FILE_TYPE);
    for (String ver : versions) {
      Matcher matcher = pattern.matcher(ver);
      if (matcher.find()) {
        String v = matcher.group(1);
        if (configVersion.compareTo(new ConfigVersion(v)) >= 0) {
          return ver;
        }
      }
    }
    return versions.get(0);
  }

  /**
   * 选择版本
   */
  private InputStream chooseFile(JarFile jarFile, String version) throws IOException {
    String configName = getConfigName();
    String configPath = getConfigPath();
    Enumeration<JarEntry> entries = jarFile.entries();
    Map<String, JarEntry> entryMap = new HashMap<>();
    while (entries.hasMoreElements()) {
      JarEntry entry = entries.nextElement();
      String name = entry.getName();
      if (name.startsWith(configPath)) {
        // 正则匹配，提取版本号和文件内容
        name = name.substring(configPath.length());
        if (name.startsWith(configName)) {
          entryMap.put(name, entry);
        }
      }
    }
    String chooseVersion = chooseVersion(new ArrayList<>(entryMap.keySet()), version);
    if (chooseVersion == null) {
      return null;
    }
    JarEntry jarEntry = entryMap.get(chooseVersion);
    return jarFile.getInputStream(jarEntry);
  }

  /**
   * 选择版本
   */
  private InputStream chooseFile(File file, String version) throws IOException {
    String configName = getConfigName();
    File[] files = file.listFiles();
    if (files == null || files.length == 0) {
      return null;
    }
    Map<String, File> fileMap = new HashMap<>();
    for (File f : files) {
      if (f.getName().startsWith(configName)) {
        fileMap.put(f.getName(), f);
      }
    }
    String chooseVersion = chooseVersion(new ArrayList<>(fileMap.keySet()), version);
    if (chooseVersion == null) {
      return null;
    }
    return new FileInputStream(fileMap.get(chooseVersion));
  }

  /**
   * 获取版本配置
   */
  protected Properties getVersionProperties() {
    String version = ConfigHelper.getStr(getVersionKey());
    // 读取资源
    URL resource = getClass().getResource("");
    if (resource == null) {
      return null;
    }
    InputStream inputStream = null;
    if (resource.getProtocol().equals("file")) {
      if (resource.getPath().endsWith(".jar")) {
        try {
          JarFile jarFile = new JarFile(resource.getPath());
          inputStream = chooseFile(jarFile, version);
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      } else {
        try {
          File file = new File(resource.toURI());
          inputStream = chooseFile(file, version);
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
    } else if (resource.getProtocol().equals("jar")) {
      try {
        JarFile jarFile = ((JarURLConnection) resource.openConnection()).getJarFile();
        inputStream = chooseFile(jarFile, version);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    if (inputStream == null) {
      return null;
    }
    Properties props = new Properties();
    try {
      props.load(inputStream);
    } catch (IOException e) {
      throw new RuntimeException(e);
    } finally {
      try {
        inputStream.close();
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
