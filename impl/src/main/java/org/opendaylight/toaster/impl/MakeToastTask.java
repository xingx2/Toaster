/*
 * Copyright © 2015 CopyLeft(c) and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.toaster.impl;

import java.util.concurrent.Callable;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.toaster.rev150105.MakeToastInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MakeToastTask implements Callable<Void>{
     private static final Logger LOG = LoggerFactory.getLogger(MakeToastTask.class);
     
     private final MakeToastInput toastRequest;
     
     public MakeToastTask(MakeToastInput toastRequest){
         this.toastRequest = toastRequest;
     }
     
     public Void call(){
         try{
             LOG.info("makeToast start,Doneness: 5 (***)"+",Type:"+toastRequest.getToasterToastType());
             Thread.sleep(10000);
             LOG.info("makeToast end.....");
         }catch (InterruptedException e){
             LOG.info("interrupted while making the toast");
         }
         return null;
     }
}