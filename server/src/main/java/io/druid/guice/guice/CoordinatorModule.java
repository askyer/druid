/*
 * Druid - a distributed column store.
 * Copyright (C) 2012, 2013  Metamarkets Group Inc.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package io.druid.guice.guice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.metamx.common.concurrent.ScheduledExecutorFactory;
import com.metamx.druid.db.DatabaseRuleManager;
import com.metamx.druid.db.DatabaseRuleManagerConfig;
import com.metamx.druid.db.DatabaseRuleManagerProvider;
import com.metamx.druid.db.DatabaseSegmentManager;
import com.metamx.druid.db.DatabaseSegmentManagerConfig;
import com.metamx.druid.db.DatabaseSegmentManagerProvider;
import com.metamx.druid.master.DruidMaster;
import com.metamx.druid.master.DruidMasterConfig;
import com.metamx.druid.master.LoadQueueTaskMaster;
import io.druid.client.ServerInventoryViewConfig;
import io.druid.client.indexing.IndexingServiceClient;
import io.druid.server.http.MasterRedirectInfo;
import io.druid.server.http.RedirectFilter;
import io.druid.server.http.RedirectInfo;
import io.druid.server.http.RedirectServlet;
import org.apache.curator.framework.CuratorFramework;

/**
 */
public class CoordinatorModule implements Module
{
  @Override
  public void configure(Binder binder)
  {
    ConfigProvider.bind(binder, DruidMasterConfig.class);
    ConfigProvider.bind(binder, ServerInventoryViewConfig.class);

    JsonConfigProvider.bind(binder, "druid.manager.segment", DatabaseSegmentManagerConfig.class);
    JsonConfigProvider.bind(binder, "druid.manager.rules", DatabaseRuleManagerConfig.class);

    binder.bind(RedirectServlet.class).in(LazySingleton.class);
    binder.bind(RedirectFilter.class).in(LazySingleton.class);

    binder.bind(DatabaseSegmentManager.class)
          .toProvider(DatabaseSegmentManagerProvider.class)
          .in(ManageLifecycle.class);

    binder.bind(DatabaseRuleManager.class)
          .toProvider(DatabaseRuleManagerProvider.class)
          .in(ManageLifecycle.class);

    binder.bind(IndexingServiceClient.class).in(LazySingleton.class);

    binder.bind(RedirectInfo.class).to(MasterRedirectInfo.class).in(LazySingleton.class);

    binder.bind(DruidMaster.class);
  }

  @Provides @LazySingleton
  public LoadQueueTaskMaster getLoadQueueTaskMaster(
      CuratorFramework curator, ObjectMapper jsonMapper, ScheduledExecutorFactory factory, DruidMasterConfig config
  )
  {
    return new LoadQueueTaskMaster(curator, jsonMapper, factory.create(1, "Master-PeonExec--%d"), config);
  }
}