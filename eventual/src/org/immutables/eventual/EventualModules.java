/*
 * Copyright 2015 Hotwire. All Rights Reserved.
 *
 * This software is the proprietary information of Hotwire.
 * Use is subject to license terms.
 */
package org.immutables.eventual;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Injector;
import com.google.inject.Module;
import java.util.concurrent.Executor;

/**
 * Creates special mix-in module created from defining class with special asynchronous
 * transformation methods annotated with {@literal @}{@link EventuallyProvides}.
 * <p>
 * Basic example
 * 
 * <pre>
 * public class Providers {
 *   {@literal @}EventuallyProvides
 *   C combine(A a, B b) {
 *     return new C(a.value(), b.getProperty());
 *   }
 * 
 *   {@literal @}Exposed
 *   {@literal @}EventuallyProvides
 *   Z transformed(C c) {
 *     return c.transformed();
 *   }
 * }
 * 
 * Module module = EventualModules.definedBy(Providers.class);
 * </pre>
 * 
 * Having dependency on ListenableFuture&lt;A&gt; and ListenableFuture&lt;B&gt;, this module exposed
 * combined and transformed ListenableFuture&lt;Z&gt; available to injector.
 * <p>
 * <em>While super-classes could be used and will be scanned for such methods, method overriding is
 * not handled properly so avoid overriding provider methods. Use delegation to regular methods if some
 * functionality should be implemented or overridden.
 * </em>
 * <p>
 * To customize dispatching injector could provided with binding to {@literal @}
 * {@link EventuallyAsync} {@link Executor}
 * @see EventuallyProvides
 * @see EventuallyAsync
 */
public final class EventualModules {

  /**
   * Create a module filled with futures combined in interdependencies.
   * @param asyncProviderClass class which defined future transformations
   * @return the module
   */
  public static Module providedBy(Class<?> asyncProviderClass) {
    return new EventualProvidersModule<>(asyncProviderClass);
  }

  /**
   * Converts injector which injects future values into the future for module, which is when
   * instantiated as injector could inject unwrapped values from completed futures found in input
   * injector.
   * @param futureInjecting the injector of future values
   * @return the future module
   */
  public static ListenableFuture<Module> completedFrom(Injector futureInjecting) {
    return CompletedFutureModule.from(futureInjecting);
  }
}
