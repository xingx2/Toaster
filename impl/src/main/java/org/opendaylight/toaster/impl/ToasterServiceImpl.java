/*
 * Copyright © 2015 CopyLeft(c) and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.toaster.impl;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.toaster.rev150105.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.toaster.rev150105.Toaster.ToasterStatus;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcError.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.Date;
import java.text.SimpleDateFormat;
import java.lang.management.ManagementFactory;  

public class ToasterServiceImpl implements ToasterService, DataChangeListener {

    private static final Logger LOG = LoggerFactory.getLogger(ToasterServiceImpl.class);
     
    private final AtomicReference<Future<?>> currentMakeToastTask = new AtomicReference<>();
     
    final InstanceIdentifier<Toaster> TOASTER_IID = InstanceIdentifier.builder(Toaster.class).build();
     
    private final ExecutorService executor = Executors.newFixedThreadPool(1);
     
    private DataBroker broker;
     
    public ToasterServiceImpl(DataBroker broker) {
        this.broker = broker;
    }

    @Override
    public Future<RpcResult<java.lang.Void>> cancelToast() {
        LOG.info("cancelToast");
        final InstanceIdentifier<Toaster> TOASTER_IID = InstanceIdentifier.builder(Toaster.class).build();
        final ReadWriteTransaction tx = broker.newReadWriteTransaction();
        ListenableFuture<Optional<Toaster>> readFuture =
        tx.read( LogicalDatastoreType.OPERATIONAL, TOASTER_IID );

        //将Optional<Toaster>类型的ListenableFuture转换成Void的ListenableFuture
        final ListenableFuture<Void> commitFuture =
        Futures.transform( readFuture, new AsyncFunction<Optional<Toaster>,Void>() {

            @Override
            public ListenableFuture<Void> apply(
                final Optional<Toaster> toasterData ) throws Exception {
                    //获取toaster的tasterStatus
            ToasterStatus toasterStatus = ToasterStatus.Down;
            if( toasterData.isPresent() ) {
                toasterStatus = toasterData.get().getToasterStatus();
            }

                        //判断当前的状态是不是Up
            if( toasterStatus == ToasterStatus.Down ) {
                           //如果是Down状态，则抛出异常
                LOG.info("the toaster is not running!");
                return Futures.immediateFailedCheckedFuture(
                new TransactionCommitFailedException( "", RpcResultBuilder.newWarning( ErrorType.APPLICATION, "not-in-use",
                   "Toaster is not running", null, null, null ) ) );
            } else{
                            //如果是up状态，则修改成down状态
                tx.put( LogicalDatastoreType.OPERATIONAL, TOASTER_IID,
                    new ToasterBuilder().setToasterStatus( ToasterStatus.Down ).build());
                return tx.submit();
            }
        }
    } );

        //添加callback函数
        Futures.addCallback( commitFuture, new FutureCallback<Void>() {
            @Override
            public void onSuccess( final Void result ) {
                // 如果更新data store成功则执行makeToast
                LOG.info("******Task was canceled.******");
            }

            @Override
            public void onFailure( final Throwable ex ) {
                LOG.debug( "Failed to commit Toaster status", ex );
            }
        }
        );
        return Futures.immediateFuture(RpcResultBuilder.<Void> success().build());
    }

    @Override
    public Future<RpcResult<java.lang.Void>> makeToast(final MakeToastInput input) {
        LOG.info("makeToast: {}",input);
        
        String name = ManagementFactory.getRuntimeMXBean().getName();  
        LOG.info(name); 
        // get pid  
        String pid = name.split("@")[0];  
        LOG.info("#########pid is:"+pid);  
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//设置日期格式
        String time = df.format(new Date());
        LOG.info("#########time is "+time);// new Date()为获取当前系统时间
        String tid = Long.toString(Thread.currentThread().getId());
        LOG.info("#########tid is:"+tid);
        record("makeToast",time,pid,tid);
         
        final InstanceIdentifier<Toaster> TOASTER_IID = InstanceIdentifier.builder(Toaster.class).build();
        final ReadWriteTransaction tx = broker.newReadWriteTransaction();
        ListenableFuture<Optional<Toaster>> readFuture =
        tx.read( LogicalDatastoreType.OPERATIONAL, TOASTER_IID );
         
        //将Optional<Toaster>类型的ListenableFuture转换成Void的ListenableFuture
        final ListenableFuture<Void> commitFuture =
        Futures.transform( readFuture, new AsyncFunction<Optional<Toaster>,Void>() {

            @Override
            public ListenableFuture<Void> apply(
                final Optional<Toaster> toasterData ) throws Exception {
                        //获取toaster的tasterStatus
                ToasterStatus toasterStatus = ToasterStatus.Down;
                if( toasterData.isPresent() ) {
                    toasterStatus = toasterData.get().getToasterStatus();
                }

                        //判断当前的状态是不是Up
                if( toasterStatus == ToasterStatus.Up ) {
                            //如果是Up状态，则抛出异常
                    LOG.info("the toaster is already using,please wait a moment!");
                    return Futures.immediateFailedCheckedFuture(
                        new TransactionCommitFailedException( "", RpcResultBuilder.newWarning( ErrorType.APPLICATION, "in-use",
                            "Toaster is busy", null, null, null ) ) );
                } else{
                            //如果是down状态，则修改成Up状态
                    tx.put( LogicalDatastoreType.OPERATIONAL, TOASTER_IID,
                        new ToasterBuilder().setToasterStatus( ToasterStatus.Up ).build());
                    return tx.submit();
                }
            }
        } );
         
        //添加callback函数
        Futures.addCallback( commitFuture, new FutureCallback<Void>() {
            @Override
            public void onSuccess( final Void result ) {
                // 如果更新data store成功则执行makeToast
            LOG.info("******Task Starts******");
            currentMakeToastTask.set( executor.submit(new MakeToastTask(input)));
            }

            @Override
            public void onFailure( final Throwable ex ) {
                LOG.debug( "Failed to commit Toaster status", ex );
            }
        }
        );
         
         return Futures.immediateFuture(RpcResultBuilder.<Void> success().build());
     }

    @Override
    public void onDataChanged( final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change ) {
        DataObject dataObject = change.getUpdatedSubtree();
        if( dataObject instanceof Toaster )
        {
            Toaster toaster = (Toaster) dataObject;
            LOG.info("onDataChanged - new Toaster config: {}", toaster);
        }
    }

    public void record(String name, String time, String pid, String tid){
        InstanceIdentifier<Info> INFO_IID = InstanceIdentifier.builder(Info.class).build();
        final WriteTransaction tx = broker.newWriteOnlyTransaction();
        InfoBuilder ib = new InfoBuilder();
        ib.setName(name);
        ib.setTime(time);
        ib.setPid(pid);
        ib.setTid(tid);
        tx.put( LogicalDatastoreType.OPERATIONAL, INFO_IID, ib.build());
        tx.submit();
    }
}