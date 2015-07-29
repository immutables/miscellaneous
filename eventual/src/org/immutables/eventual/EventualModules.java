/*
 * Copyright 2015 Hotwire. All Rights Reserved.
 *
 * This software is the proprietary information of Hotwire.
 * Use is subject to license terms.
 */
package org.immutables.eventual;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Binder;
import com.google.inject.Exposed;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.PrivateBinder;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * Creates special mix-in module created from defining class with special asynchronous
 * transformation methods annotated with {@literal @}{@link Eventually.Provides}.
 * <p>
 * Basic example
 * 
 * <pre>
 * public class Providers {
 *   {@literal @}Eventually.Provides
 *   C combine(A a, B b) {
 *     return new C(a.value(), b.getProperty());
 *   }
 * 
 *   {@literal @}Exposed
 *   {@literal @}Eventually.Provides
 *   Z transformed(C c) {
 *     return c.transformed();
 *   }
 * }
 * 
 * Module module = EventualModules.definedBy(new Providers);
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
 * You can annotate class with the {@literal @}{@code Singleton} annotation to have all futures be
 * singletons in module created by {@link #providedBy(Object)} module.
 * <p>
 * To customize dispatching injector could provided with binding to {@literal @}
 * {@link Eventually.Async} {@link Executor}
 * @see Eventually
 */
public final class EventualModules {
  private EventualModules() {}

  /**
   * Create a module filled with futures combined in interdependencies.
   * Use returned module separately or together with other module to create injector which
   * will contain interrelated futures bounded, then use {@link #completedFrom(Injector)} create
   * future module that binds all dereferenced future values which were {@link Exposed}.
   * @param eventuallyProvider object which defined future transformations annotated with
   *          {@link Eventually.Provides}
   * @return the module
   */
  public static Module providedBy(Object eventuallyProvider) {
    return new EventualModule(createPartial(eventuallyProvider));
  }

  /**
   * Create a module filled with futures combined in interdependencies.
   * Use returned module separately or together with other module to create injector which
   * will contain interrelated futures bounded, then use {@link #completedFrom(Injector)} create
   * future module that binds all dereferenced future values which were {@link Exposed}.
   * @param eventuallyProvider class which defined future transformations annotated with
   *          {@link Eventually.Provides}
   * @return the module
   */
  public static Module providedBy(Class<?> eventuallyProvider) {
    return providedBy((Object) eventuallyProvider);
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

  // safe unchecked, will be used only for reading
  @SuppressWarnings("unchecked")
  private static EventualProviders<?> createPartial(Object eventuallyProvider) {
    if (eventuallyProvider instanceof Class<?>) {
      return new EventualProviders<>(null, (Class<?>) eventuallyProvider);
    }
    return new EventualProviders<>(eventuallyProvider, (Class<Object>) eventuallyProvider.getClass());
  }

  /** This is done to group providers partials with a single private binder when using builder. */
  private static class EventualModule implements Module {
    private final EventualProviders<?>[] partials;

    EventualModule(EventualProviders<?>... partials) {
      this.partials = partials;
    }

    @Override
    public void configure(Binder binder) {
      PrivateBinder privateBinder = binder.newPrivateBinder();
      for (EventualProviders<?> partial : partials) {
        partial.configure(privateBinder);
      }
    }
  }

  /**
   * Builder to fluently create and handle eventual provider modules.
   */
  public static final class Builder {
    private final List<Module> modules = Lists.newArrayList();
    private final List<EventualProviders<?>> partials = Lists.newArrayList();

    public Builder add(Module module) {
      modules.add(module);
      return this;
    }

    public Builder add(Object providersInstance) {
      partials.add(createPartial(providersInstance));
      return this;
    }

    public Builder add(Class<?> providersClass) {
      return add((Object) providersClass);
    }

    public ListenableFuture<Module> toFuture() {
      return completedFrom(Guice.createInjector(eventualModules()));
    }

    public Injector joinInjector() {
      Module module = Futures.getUnchecked(toFuture());
      return Guice.createInjector(module);
    }

    private List<Module> eventualModules() {
      List<Module> result = Lists.newArrayList(modules);
      result.add(new EventualModule(Iterables.toArray(partials, EventualProviders.class)));
      return result;
    }
  }
}
