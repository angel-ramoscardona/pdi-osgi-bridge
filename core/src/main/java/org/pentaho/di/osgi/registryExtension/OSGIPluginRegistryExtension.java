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

import com.google.common.annotations.VisibleForTesting;
import org.pentaho.di.core.KettleClientEnvironment;
import org.pentaho.di.core.plugins.PluginInterface;
import org.pentaho.di.core.plugins.PluginRegistry;
import org.pentaho.di.core.plugins.PluginRegistryExtension;
import org.pentaho.di.core.plugins.PluginTypeInterface;
import org.pentaho.di.core.plugins.RegistryPlugin;
import org.pentaho.di.osgi.OSGIPluginTracker;
import org.pentaho.di.osgi.OSGIPluginType;
import org.pentaho.di.osgi.StatusGetter;
import org.pentaho.di.osgi.service.lifecycle.PluginRegistryOSGIServiceLifecycleListener;
import org.pentaho.platform.api.engine.IApplicationContext;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.engine.core.system.StandaloneApplicationContext;
import org.pentaho.platform.osgi.KarafBoot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by bryan on 8/15/14.
 */
@RegistryPlugin(id = "OSGIRegistryPlugin", name = "OSGI")
public class OSGIPluginRegistryExtension implements PluginRegistryExtension {
  private static OSGIPluginRegistryExtension INSTANCE;
  private OSGIPluginTracker tracker = OSGIPluginTracker.getInstance();
  private Logger logger = LoggerFactory.getLogger( getClass() );
  private KarafBoot boot = new KarafBoot();
  private StatusGetter<Boolean> kettleClientEnvironmentInitialized = new StatusGetter<Boolean>() {
    @Override public Boolean get() {
      return KettleClientEnvironment.isInitialized();
    }
  };
  private AtomicBoolean initializedKaraf = new AtomicBoolean( false );

  public OSGIPluginRegistryExtension() {
    INSTANCE = this;
  }

  public static OSGIPluginRegistryExtension getInstance() {
    if ( INSTANCE == null ) {
      throw new IllegalStateException( "Kettle is supposed to construct this first" );
    }
    return INSTANCE;
  }

  // FOR UNIT TEST ONLY
  protected static void setInstance( OSGIPluginRegistryExtension instance ) {
    INSTANCE = instance;
  }

  // FOR UNIT TEST ONLY
  protected void setTracker( OSGIPluginTracker tracker ) {
    this.tracker = tracker;
  }

  // FOR UNIT TEST ONLY
  protected void setLogger( Logger logger ) {
    this.logger = logger;
  }


  // FOR UNIT TEST ONLY
  protected void setKettleClientEnvironmentInitialized( StatusGetter<Boolean> kettleClientEnvironmentInitialized ) {
    this.kettleClientEnvironmentInitialized = kettleClientEnvironmentInitialized;
  }

  @VisibleForTesting
  void setKarafBoot( KarafBoot boot ){
    this.boot = boot;
  }

  public KarafBoot getKarafBoot(){
    return boot;
  }

  @SuppressWarnings( "squid:S2276" ) // can't use a monitor here, but blocking in here is appropriate
  public synchronized void init( final PluginRegistry registry ) {
    if ( PentahoSystem.getInitializedStatus() != PentahoSystem.SYSTEM_INITIALIZED_OK && !initializedKaraf.getAndSet(
      true ) ) {
      String userDir = System.getProperty( "pentaho.user.dir", "." );
      IApplicationContext context = new StandaloneApplicationContext( userDir, userDir );
      PentahoSystem.init( context );
      boot.startup( null );
    }
    boolean success = false;
    PluginRegistry.addPluginType( OSGIPluginType.getInstance() );
    tracker.addPluginLifecycleListener( PluginInterface.class,
      new PluginRegistryOSGIServiceLifecycleListener( registry ) );
    logger.info( "Registered lifecycle listener with OSGIPluginTracker" );
    while ( !success ) {
      success = tracker.registerPluginClass( PluginInterface.class );
      if ( success ) {
        logger.info( "Registered PluginInterface with OSGIPluginTracker" );
      } else {
        logger.info( "Unable to register PluginInterface with OSGIPluginTracker; waiting and retrying" );
        try {
          Thread.sleep( 1000 );
        } catch ( InterruptedException e ) {
          Thread.currentThread().interrupt();
        }
      }
    }
  }

  @Override
  public void searchForType( PluginTypeInterface pluginType ) {
    boolean success = false;
    while ( !success ) {
      success = tracker.registerPluginClass( pluginType.getClass() );
      logger.info( String.format( "%s registered with OSGIPluginTracker", pluginType.getClass() ) );
      if ( !success ) {
        logger.info( String.format( "Unable to register %s with OSGIPluginTracker; waiting and retrying", pluginType.getClass() ) );
        try {
          Thread.sleep( 1000 );
        } catch ( InterruptedException e ) {
          Thread.currentThread().interrupt();
        }
      }
    }
  }

  @Override
  public String getPluginId( Class<? extends PluginTypeInterface> pluginType, Object pluginClass ) {
    try {
      return (String) tracker.getBeanPluginProperty( pluginType, pluginClass, "ID" );
    } catch ( Exception e ) {
      logger.error( e.getMessage(), e );
    }
    return null;
  }
}
