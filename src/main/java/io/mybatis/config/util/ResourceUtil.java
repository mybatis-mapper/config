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

package io.mybatis.config.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Objects;

public class ResourceUtil {
  public static final String CLASSPATH_PREFIX = "classpath:";

  /**
   * 获取默认类加载器，参考 Spring ClassUtils
   */
  public static ClassLoader getDefaultClassLoader() {
    ClassLoader cl = null;
    try {
      cl = Thread.currentThread().getContextClassLoader();
    } catch (Throwable ignored) {
    }
    if (cl == null) {
      cl = ResourceUtil.class.getClassLoader();
      if (cl == null) {
        try {
          cl = ClassLoader.getSystemClassLoader();
        } catch (Throwable ignored) {
        }
      }
    }
    return Objects.requireNonNull(cl);
  }

  public static URL getResource(String name) {
    return getDefaultClassLoader().getResource(name);
  }

  public static Enumeration<URL> getResources(String name) {
    try {
      return getDefaultClassLoader().getResources(name);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static File getClasspathFile(String resourceLocation) throws FileNotFoundException {
    if (resourceLocation == null) {
      throw new NullPointerException("Resource location must not be null");
    }
    return getFile(resourceLocation.startsWith(CLASSPATH_PREFIX) ? resourceLocation : CLASSPATH_PREFIX + resourceLocation);
  }

  public static File getFile(String resourceLocation) throws FileNotFoundException {
    if (resourceLocation == null) {
      throw new NullPointerException("Resource location must not be null");
    }
    if (resourceLocation.startsWith(CLASSPATH_PREFIX)) {
      String path = resourceLocation.substring(CLASSPATH_PREFIX.length());
      String description = "class path resource [" + path + "]";
      URL url = getResource(path);
      if (url == null) {
        throw new FileNotFoundException(description +
            " cannot be resolved to absolute file path because it does not exist");
      }
      return getFile(url, description);
    }
    try {
      // try URL
      return getFile(new URL(resourceLocation));
    } catch (MalformedURLException ex) {
      // no URL -> treat as file path
      return new File(resourceLocation);
    }
  }

  public static File getFile(URL resourceUrl) throws FileNotFoundException {
    return getFile(resourceUrl, "URL");
  }

  public static File getFile(URL resourceUrl, String description) throws FileNotFoundException {
    if (resourceUrl == null) {
      throw new NullPointerException("Resource URL must not be null");
    }
    if (!"file".equals(resourceUrl.getProtocol())) {
      throw new FileNotFoundException(
          description + " cannot be resolved to absolute file path " +
              "because it does not reside in the file system: " + resourceUrl);
    }
    try {
      return new File(toURI(resourceUrl).getSchemeSpecificPart());
    } catch (URISyntaxException ex) {
      // Fallback for URLs that are not valid URIs (should hardly ever happen).
      return new File(resourceUrl.getFile());
    }
  }

  public static File getFile(URI resourceUri) throws FileNotFoundException {
    return getFile(resourceUri, "URI");
  }

  public static File getFile(URI resourceUri, String description) throws FileNotFoundException {
    if (resourceUri == null) {
      throw new NullPointerException("Resource URI must not be null");
    }
    if (!"file".equals(resourceUri.getScheme())) {
      throw new FileNotFoundException(
          description + " cannot be resolved to absolute file path " +
              "because it does not reside in the file system: " + resourceUri);
    }
    return new File(resourceUri.getSchemeSpecificPart());
  }

  public static URI toURI(URL url) throws URISyntaxException {
    return toURI(url.toString());
  }

  public static URI toURI(String location) throws URISyntaxException {
    return new URI(replace(location, " ", "%20"));
  }

  public static String replace(String inString, String oldPattern, String newPattern) {
    if (!hasLength(inString) || !hasLength(oldPattern) || newPattern == null) {
      return inString;
    }
    int index = inString.indexOf(oldPattern);
    if (index == -1) {
      // no occurrence -> can return input as-is
      return inString;
    }

    int capacity = inString.length();
    if (newPattern.length() > oldPattern.length()) {
      capacity += 16;
    }
    StringBuilder sb = new StringBuilder(capacity);

    int pos = 0;  // our position in the old string
    int patLen = oldPattern.length();
    while (index >= 0) {
      sb.append(inString, pos, index);
      sb.append(newPattern);
      pos = index + patLen;
      index = inString.indexOf(oldPattern, pos);
    }

    sb.append(inString, pos, inString.length());
    return sb.toString();
  }

  public static boolean hasLength(String str) {
    return (str != null && !str.isEmpty());
  }
}
