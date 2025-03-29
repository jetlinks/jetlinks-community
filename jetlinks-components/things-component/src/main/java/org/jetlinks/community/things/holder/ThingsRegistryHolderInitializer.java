package org.jetlinks.community.things.holder;

import org.jetlinks.core.things.ThingsRegistry;

public class ThingsRegistryHolderInitializer {

   public static void init(ThingsRegistry registry) {
       ThingsRegistryHolder.REGISTRY = registry;
   }

}
