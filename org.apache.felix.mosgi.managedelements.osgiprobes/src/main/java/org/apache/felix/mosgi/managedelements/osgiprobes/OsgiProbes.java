/*
 *   Copyright 2005 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.felix.mosgi.managedelements.osgiprobes;

/**
 * TODO : Should listen to Agent Service lifecycle
 *        Need to change ObjectName
 *        Should listen to serviceLifecycle
**/

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.NotificationBroadcasterSupport;


import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceListener;

import org.apache.felix.framework.cache.BundleCache;

import org.osgi.service.log.LogService;

public class OsgiProbes extends NotificationBroadcasterSupport implements BundleActivator, OsgiProbesMBean, ServiceListener {

  private String version = null;
  private static String tabNameString = "TabUI:name=OsgiProbes";

  private ObjectName tabName = null;
  private MBeanServer server = null;
  private BundleContext bc = null;
  private ServiceReference sr = null;


  ////////////////////////////////////////////////////////
  //     TabIfc (from OsgiProbesMBean)                  //
  ////////////////////////////////////////////////////////
  public String getBundleName() {
    return this.bc.getProperty("insa.jmxconsole.tab.url.osgiprobestab");
  }


  ////////////////////////////////////////////////////////
  //       BundleActivator                              //
  ////////////////////////////////////////////////////////
  public void start(BundleContext context) throws Exception  {
    this.bc=context;
    this.version=(String)bc.getBundle().getHeaders().get(Constants.BUNDLE_VERSION);
    this.log(LogService.LOG_INFO, "Starting OsgiProbe MBean " + this.version,null);
    this.tabName=new ObjectName(tabNameString);
    this.sr = context.getServiceReference(MBeanServer.class.getName());
    if (sr!=null){
      this.connectToAgent(sr);
    }
    this.log(LogService.LOG_INFO, "OsgiProbes MBean "+this.version+" started", null);
  }


  public void stop(BundleContext context) {
    this.log(LogService.LOG_INFO, "Stopping OsgiProbes MBean "+this.version, null);
    if (this.server!=null){
      this.disconnectFromAgent();
    }
    this.sr=null;
    this.log(LogService.LOG_INFO, "OsgiProbes MBean "+this.version+" stopped", null);
    this.bc=null;
  }

  ////////////////////////////////////////////////////////
  //       OsgiProbesMBean                              //
  ////////////////////////////////////////////////////////
  public String getFwVersion(){
    return this.bc.getProperty(Constants.FRAMEWORK_VERSION);
  }
  
  public String getFwVendor (){
    return this.bc.getProperty(Constants.FRAMEWORK_VENDOR);
  }
  
  public String getFwLanguage(){
    return this.bc.getProperty(Constants.FRAMEWORK_LANGUAGE);
  }
  
  public String getFwOsName(){
    return this.bc.getProperty(Constants.FRAMEWORK_OS_NAME);
  }
  
  public String getFwOsVersion(){
    return this.bc.getProperty(Constants.FRAMEWORK_OS_VERSION);
  }
  
  public String getFwProcessor(){
    return this.bc.getProperty(Constants.FRAMEWORK_PROCESSOR);
  }
  
  public String getFwExeEnv(){
    return System.getProperty("java.version");
//    return (String) this.bc.getProperty(Constants.FRAMEWORK_EXECUTIONENVIRONMENT);
  }

  public String getProfile(){
    return this.bc.getProperty(BundleCache.CACHE_PROFILE_PROP);
  }

  ////////////////////////////////////////////////////////
  //       ServiceListener                              //
  ////////////////////////////////////////////////////////
  public void serviceChanged(ServiceEvent event) {
    ServiceReference sr=event.getServiceReference();
    Object service=bc.getService(sr);
    if (this.server==null && event.getType()==ServiceEvent.REGISTERED && service instanceof MBeanServer){
      this.connectToAgent(sr);
    }
    if (this.server!=null){
      if(event.getType()==ServiceEvent.UNREGISTERING && service instanceof MBeanServer){
        this.disconnectFromAgent();
      }
    }
  }


  private void connectToAgent(ServiceReference sr){
    this.log(LogService.LOG_INFO, "Registering to agent", null);
    try{
      this.server=(MBeanServer)this.bc.getService(sr);
      this.server.registerMBean(this, tabName);
      this.bc.addServiceListener(this);
    }catch (Exception e){
      e.printStackTrace();
    }
    this.log(LogService.LOG_INFO, "Registered to agent", null);
  }
  
  private void disconnectFromAgent(){
    this.log(LogService.LOG_INFO, "Unregistering from agent", null);
    this.bc.removeServiceListener(this);
    try {
      server.unregisterMBean(tabName);
    } catch (Exception e) {
      e.printStackTrace();
    }
    this.server=null;
    this.bc.ungetService(this.sr);
    this.log(LogService.LOG_INFO, "Unregistered from agent", null);
  }

  private void log(int prio, String message, Throwable t){
    if (this.bc!=null){
      ServiceReference logSR=this.bc.getServiceReference(LogService.class.getName());
      if (logSR!=null){
        ((LogService)this.bc.getService(logSR)).log(prio, message, t);
      }else{
        System.out.println("No Log Service");
      }
    }else{
      System.out.println(this.getClass().getName()+".log: No bundleContext");
    }
  }

}
