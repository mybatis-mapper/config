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

import java.io.*;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.*;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
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
    Properties props = buildVersionProperties();
    if (props != null) {
      this.properties = props;
    } else {
      this.properties = new Properties();
    }
  }

  /**
   * 转换文件名，按版本号排序（升序）
   *
   * @param fileNames 文件名集合
   */
  private List<ConfigVersion> sortVersions(Collection<String> fileNames) {
    if (fileNames == null || fileNames.isEmpty()) {
      return null;
    }
    Pattern pattern = Pattern.compile(getConfigName() + "-(v\\d+\\.\\d+)\\" + FILE_TYPE);
    return fileNames.stream().map(fileName -> {
      Matcher matcher = pattern.matcher(fileName);
      if (matcher.find()) {
        return new ConfigVersion(matcher.group(1), fileName);
      }
      return null;
    }).filter(Objects::nonNull).sorted().collect(Collectors.toList());
  }

  /**
   * 选择版本
   *
   * @param versions 所有版本
   * @param version  选择的版本
   */
  private ConfigVersion chooseVersion(List<ConfigVersion> versions, String version) {
    if (versions == null || versions.isEmpty()) {
      return null;
    }
    //没有指定版本时使用最新版本
    if (version == null || version.isEmpty()) {
      return versions.get(versions.size() - 1);
    }
    ConfigVersion configVersion = new ConfigVersion(version);
    //从最高版本进行比较，选择的版本高于或等于配置版本时，就选择该版本
    for (int i = versions.size() - 1; i >= 0; i--) {
      if (configVersion.compareTo(versions.get(i)) >= 0) {
        return versions.get(i);
      }
    }
    //选择的版本不高于所有版本时，使用最小版本
    return versions.get(0);
  }

  /**
   * 构建 Properties
   *
   * @param versions      所有版本
   * @param chooseVersion 选择的版本
   * @param toInputStream 转换流
   * @throws IOException 文件异常
   */
  private Properties build(List<ConfigVersion> versions, ConfigVersion chooseVersion, Function<ConfigVersion, InputStream> toInputStream) throws IOException {
    if (chooseVersion == null) {
      return null;
    }
    InputStream is;
    Properties prop = null;
    for (ConfigVersion configVersion : versions) {
      if (configVersion != chooseVersion) {
        prop = new Properties(prop);
        is = toInputStream.apply(configVersion);
        if (is != null) {
          prop.load(is);
          is.close();
        }
      }
    }
    prop = new Properties(prop);
    is = toInputStream.apply(chooseVersion);
    if (is != null) {
      prop.load(is);
      is.close();
    }
    return prop;
  }

  /**
   * 选择版本
   */
  private Properties chooseFromJarFile(JarFile jarFile, String version) throws IOException {
    String configName = getConfigName();
    String configPath = getConfigPath();
    Enumeration<JarEntry> entries = jarFile.entries();
    Map<String, JarEntry> entryMap = new HashMap<>();
    while (entries.hasMoreElements()) {
      JarEntry entry = entries.nextElement();
      String name = entry.getName();
      if (name.startsWith(configPath)) {
        name = name.substring(configPath.length());
        if (name.startsWith(configName)) {
          entryMap.put(name, entry);
        }
      }
    }
    List<ConfigVersion> versions = sortVersions(new ArrayList<>(entryMap.keySet()));
    ConfigVersion chooseVersion = chooseVersion(versions, version);
    return build(versions, chooseVersion, configVersion -> {
      try {
        return jarFile.getInputStream(entryMap.get(chooseVersion.getFileName()));
      } catch (IOException e) {
        return null;
      }
    });
  }

  /**
   * 选择版本
   */
  private Properties chooseFromFile(File file, String version) throws IOException {
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
    List<ConfigVersion> versions = sortVersions(new ArrayList<>(fileMap.keySet()));
    ConfigVersion chooseVersion = chooseVersion(versions, version);
    return build(versions, chooseVersion, configVersion -> {
      try {
        return new FileInputStream(fileMap.get(configVersion.getFileName()));
      } catch (FileNotFoundException e) {
        return null;
      }
    });
  }

  /**
   * 获取版本配置
   */
  protected Properties buildVersionProperties() {
    String version = ConfigHelper.getStr(getVersionKey());
    // 读取资源
    URL resource = getClass().getResource("");
    if (resource == null) {
      return null;
    }
    if (resource.getProtocol().equals("file")) {
      if (resource.getPath().endsWith(".jar")) {
        try {
          JarFile jarFile = new JarFile(resource.getPath());
          return chooseFromJarFile(jarFile, version);
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      } else {
        try {
          File file = new File(resource.toURI());
          return chooseFromFile(file, version);
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
    } else if (resource.getProtocol().equals("jar")) {
      try {
        JarFile jarFile = ((JarURLConnection) resource.openConnection()).getJarFile();
        return chooseFromJarFile(jarFile, version);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    return null;
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
    private final String fileName;

    public ConfigVersion(String version) {
      this(version, null);
    }

    public ConfigVersion(String version, String fileName) {
      this.fileName = fileName;
      if (version.startsWith("v")) {
        version = version.substring(1);
      }
      String[] strings = version.split("\\.");
      this.x = Integer.parseInt(strings[0]);
      this.y = Integer.parseInt(strings[1]);
    }

    public String getFileName() {
      return fileName;
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
