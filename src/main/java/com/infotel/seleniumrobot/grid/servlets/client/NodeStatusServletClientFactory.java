package com.infotel.seleniumrobot.grid.servlets.client;

import java.lang.reflect.InvocationTargetException;
import java.net.URL;

public class NodeStatusServletClientFactory {

    Class<? extends INodeStatusServletClient> clientClass;

    public NodeStatusServletClientFactory() {
        clientClass = NodeStatusServletClient.class;
    }

    public NodeStatusServletClientFactory(Class<? extends INodeStatusServletClient> clientClass) {
        this.clientClass = clientClass;
    }

    public INodeStatusServletClient createNodeStatusServletClient(URL nodeUrl) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        return clientClass.getDeclaredConstructor(String.class, Integer.class).newInstance(nodeUrl.getHost(), nodeUrl.getPort());
    }
    public INodeStatusServletClient createNodeStatusServletClient(String host, int port) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        return clientClass.getDeclaredConstructor(String.class, Integer.class).newInstance(host, port);
    }
}
