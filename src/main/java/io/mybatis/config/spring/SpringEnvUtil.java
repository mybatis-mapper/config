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

package io.mybatis.config.spring;

import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static io.mybatis.config.defaults.VersionConfig.FILE_TYPE;

/**
 * Spring 环境配置工具类，Spring boot 时自动注册，纯 Spring 时需要自己配置才能生效
 *
 * @author liuzh
 */
public class SpringEnvUtil implements EnvironmentAware {
  private static Environment environment;
  private static boolean     enabled;

  public static String getStr(String key) {
    return (environment != null && enabled) ? SpringEnvUtil.environment.getProperty(key) : null;
  }

  @Override
  public void setEnvironment(Environment environment) {
    SpringEnvUtil.environment = environment;
    // 可以通过属性配置是否启用 Spring 支持，默认支持
    SpringEnvUtil.enabled = environment.getProperty("io.mybatis.config.spring.enabled",
        Boolean.class, true);
  }


  public static Map<String, URI> getResource() throws IOException {
    Map<String, URI> fileMap = new HashMap<>();

    ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
    Resource[] resources = resolver.getResources("classpath*:io/mybatis/provider/config/*.properties");
    for (Resource resource : resources) {
      URI uri = resource.getURI();

      String urlString = uri.toString();
      if (urlString.endsWith(FILE_TYPE)) {
        Path path = Paths.get(uri);
        // 获取文件名
        Path fileName = path.getFileName();
        fileMap.put(fileName.toString(), uri);
      }
    }
    return fileMap;
  }
}
