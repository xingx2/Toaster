/*
 * Copyright © 2015 CopyLeft(c) and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.toaster.impl;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RpcRegistration;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.toaster.rev150105.ToasterService;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ToasterProvider implements BindingAwareProvider, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(ToasterProvider.class);

    private RpcRegistration<ToasterService> rpcReg = null;
    
    private ListenerRegistration<DataChangeListener> dataChangeListenerRegistration = null;

    @Override
    public void onSessionInitiated(ProviderContext session) {
        //从session中获取broker
        DataBroker broker = session.getSALService(DataBroker.class);
        //将broker交给实现者
        ToasterServiceImpl service = new ToasterServiceImpl(broker);
        //注册rpc 和dataChange
        rpcReg = session.addRpcImplementation(ToasterService.class, service);
        dataChangeListenerRegistration = broker
                .registerDataChangeListener(LogicalDatastoreType.CONFIGURATION, service.TOASTER_IID,
                        service, DataChangeScope.SUBTREE);
        LOG.info("ToasterProvider Session Initiated");
    }

    @Override
    public void close() throws Exception {
        if(rpcReg != null){
            rpcReg.close();
        }
        if(dataChangeListenerRegistration != null){
            dataChangeListenerRegistration.close();
        }
        LOG.info("ToasterProvider Closed");
    }
}
