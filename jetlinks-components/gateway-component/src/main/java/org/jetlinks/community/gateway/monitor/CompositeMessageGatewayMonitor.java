package org.jetlinks.community.gateway.monitor;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

class CompositeMessageGatewayMonitor implements MessageGatewayMonitor {

   private List<MessageGatewayMonitor> monitors=new ArrayList<>();

   public CompositeMessageGatewayMonitor add(MessageGatewayMonitor... monitors) {
       return add(Arrays.asList(monitors));
   }

   public CompositeMessageGatewayMonitor add(Collection<MessageGatewayMonitor> monitors) {
       this.monitors.addAll(monitors);
       return this;
   }

   protected void doWith(Consumer<MessageGatewayMonitor> monitorConsumer) {
       monitors.forEach(monitorConsumer);
   }

   @Override
   public void totalSession(long sessionNumber) {
       doWith(monitor->monitor.totalSession(sessionNumber));
   }

   @Override
   public void acceptedSession() {
       doWith(MessageGatewayMonitor::acceptedSession);
   }

   @Override
   public void closedSession() {
       doWith(MessageGatewayMonitor::closedSession);
   }

   @Override
   public void subscribed() {
       doWith(MessageGatewayMonitor::subscribed);
   }

   @Override
   public void unsubscribed() {
       doWith(MessageGatewayMonitor::unsubscribed);
   }

   @Override
   public void dispatched(String connector) {
       doWith(monitor->monitor.dispatched(connector));
   }

   @Override
   public void acceptMessage() {
       doWith(MessageGatewayMonitor::acceptMessage);
   }

   @Override
   public void dispatchError(String connector, String sessionId, Throwable error) {
       doWith(monitor->monitor.dispatchError(connector, sessionId, error));
   }
}
