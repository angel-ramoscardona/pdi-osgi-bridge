/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 by Hitachi Vantara, LLC : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2029-07-20
 ******************************************************************************/


package org.pentaho.di.osgi.registryExtension;

import org.slf4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.pentaho.di.core.exception.KettlePluginException;
import org.pentaho.di.core.plugins.PluginInterface;
import org.pentaho.di.core.plugins.PluginRegistry;
import org.pentaho.di.core.plugins.PluginRegistryPluginType;
import org.pentaho.di.osgi.OSGIPluginTracker;
import org.pentaho.di.osgi.OSGIPluginType;
import org.pentaho.di.osgi.StatusGetter;
import org.pentaho.di.osgi.service.lifecycle.PluginRegistryOSGIServiceLifecycleListener;
import org.pentaho.platform.osgi.KarafBoot;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

/**
 * Created by bryan on 8/15/14.
 */
public class OSGIPluginRegistryExtensionTest {
  private OSGIPluginRegistryExtension cachedInstance;
  private OSGIPluginTracker tracker;
  private Logger logger;
  private KarafBoot karafBoot;
  private StatusGetter<Boolean> kettleClientEnvironmentInitialized;

  @Before
  public void setup() {
    try {
      cachedInstance = OSGIPluginRegistryExtension.getInstance();
    } catch ( IllegalStateException e ) {
      cachedInstance = null;
    }
    OSGIPluginRegistryExtension.setInstance( null );
    OSGIPluginRegistryExtension extension = new OSGIPluginRegistryExtension();
    tracker = mock( OSGIPluginTracker.class );
    extension.setTracker( tracker );
    logger = mock( Logger.class );
    extension.setLogger( logger );
    karafBoot = mock( KarafBoot.class );
    extension.setKarafBoot( karafBoot );
    kettleClientEnvironmentInitialized = mock( StatusGetter.class );
    extension.setKettleClientEnvironmentInitialized( kettleClientEnvironmentInitialized );
  }

  @After
  public void tearDown() {
    OSGIPluginRegistryExtension.setInstance( cachedInstance );
  }

  @Test
  public void testInit() {
    PluginRegistry registry = mock( PluginRegistry.class );
    when( kettleClientEnvironmentInitialized.get() ).thenReturn( true );
    when( tracker.registerPluginClass( any() ) ).thenReturn( true );
    OSGIPluginRegistryExtension.getInstance().init( registry );
    verify( karafBoot ).startup( null );
    verify( tracker ).registerPluginClass( PluginInterface.class );
    verify( tracker ).addPluginLifecycleListener( any( Class.class ),
      any( PluginRegistryOSGIServiceLifecycleListener.class ) );
  }

  @Test
  public void testSearchForType() {
    when( tracker.registerPluginClass( any() ) ).thenReturn( true );
    OSGIPluginRegistryExtension.getInstance().searchForType( OSGIPluginType.getInstance() );
    verify( tracker ).registerPluginClass( OSGIPluginType.class );
  }

  @Test
  public void testGetPluginId() {
    Object pluginClass = mock( Object.class );
    String id = "TEST_ID";
    when( tracker.getBeanPluginProperty( OSGIPluginType.class, pluginClass, "ID" ) ).thenReturn( id );
    assertEquals( id, OSGIPluginRegistryExtension.getInstance().getPluginId( OSGIPluginType.class, pluginClass ) );
  }

  @Test
  public void testGetPluginException() {
    Object pluginClass = mock( Object.class );
    String id = "TEST_ID";
    RuntimeException runtimeException = new RuntimeException( "TEST_MESSAGE" );
    when( tracker.getBeanPluginProperty( OSGIPluginType.class, pluginClass, "ID" ) ).thenThrow( runtimeException );
    assertEquals( null, OSGIPluginRegistryExtension.getInstance().getPluginId( OSGIPluginType.class, pluginClass ) );
    verify( logger ).error( runtimeException.getMessage(), runtimeException );
  }

  @Test
  public void testNativeRegistration() throws KettlePluginException {

    List<PluginInterface> plugins = PluginRegistry.getInstance().getPlugins(PluginRegistryPluginType.class);
    int initialSize = plugins != null ? plugins.size() : 0;

    PluginRegistryPluginType pluginRegistryPluginType = new PluginRegistryPluginType();
    pluginRegistryPluginType.searchPlugins();
    plugins = PluginRegistry.getInstance().getPlugins(PluginRegistryPluginType.class);
    assertTrue( plugins.size() > initialSize );

  }
}
