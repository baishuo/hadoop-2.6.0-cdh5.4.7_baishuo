/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.mapred;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Map;

import junit.framework.Assert;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.CleanupQueue.PathDeletionContext;
import org.apache.hadoop.security.UserGroupInformation;
import org.junit.Test;

public class TestCleanupQueue {
  @Test (timeout = 2000)
  public void testCleanupQueueClosesFilesystem() throws IOException,
      InterruptedException, NoSuchFieldException, IllegalAccessException {
    Configuration conf = new Configuration();
    File file = new File("afile.txt");
    file.createNewFile();
    Path path = new Path(file.getAbsoluteFile().toURI());
    
    FileSystem.get(conf);
    Assert.assertEquals(1, getFileSystemCacheSize());
    
    // With UGI, should close FileSystem
    CleanupQueue cleanupQueue = new CleanupQueue();
    PathDeletionContext context = new PathDeletionContext(path, conf,
        UserGroupInformation.getLoginUser(), null, null);
    cleanupQueue.addToQueue(context);
    
    while (getFileSystemCacheSize() > 0) {
      Thread.sleep(100);
    }
    
    file.createNewFile();
    FileSystem.get(conf);
    Assert.assertEquals(1, getFileSystemCacheSize());
    
    // Without UGI, should not close FileSystem
    context = new PathDeletionContext(path, conf);
    cleanupQueue.addToQueue(context);
    
    while (file.exists()) {
      Thread.sleep(100);
    }
    Assert.assertEquals(1, getFileSystemCacheSize());
  }

  /**
   * Different than upstream because we don't want to FileSystem#getCacheSize
   * in Hadoop 2
   */
  private int getFileSystemCacheSize() throws NoSuchFieldException,
      IllegalAccessException {
    Field f = FileSystem.class.getDeclaredField("CACHE"); 
    f.setAccessible(true);
    Object cache = f.get(FileSystem.class);
    f = cache.getClass().getDeclaredField("map");
    f.setAccessible(true);
    Map<?, ?> map = (Map<?, ?>) f.get(cache);
    return map.size();
  }
}
