/*
    Copyright 2013-2015 Immutables.org authors
    
   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at
   
       http://www.apache.org/licenses/LICENSE-2.0
   
   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package org.immutables.eventual;

import com.google.common.util.concurrent.UncheckedExecutionException;
import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.reflect.Invokable;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Binder;
import com.google.inject.Exposed;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.PrivateBinder;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.concurrent.Executor;
import org.immutables.eventual.CompletedModule.CompletionCriteria;
import static com.google.common.base.Preconditions.checkNotNull;

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

  public interface Invoker {
    <T, R> R invoke(Invokable<T, R> invokable, T instance, Object... objects)
        throws InvocationTargetException, IllegalAccessException;
  }

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
    return new EventualModule(createPartial(checkNotNull(eventuallyProvider)));
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
   * @param injectingFutures the injector of future values
   * @return the future module
   */
  public static ListenableFuture<Module> completedFrom(Injector injectingFutures) {
    return CompletedModule.from(checkNotNull(injectingFutures), CompletionCriteria.ALL);
  }

  /**
   * Converts injector which injects future values into the future for module, which is when
   * instantiated as injector could inject unwrapped values from successfull futures found in input
   * injector. Failed futures will be omitted from injector, Care need to be taken
   * @param injectingFutures the injector of future values
   * @return the future module
   */
  public static ListenableFuture<Module> successfulFrom(Injector injectingFutures) {
    return CompletedModule.from(checkNotNull(injectingFutures), CompletionCriteria.SUCCESSFUL);
  }

  // safe unchecked, will be used only for reading
  @SuppressWarnings("unchecked")
  private static Providers<?> createPartial(Object eventuallyProvider) {
    if (eventuallyProvider instanceof Class<?>) {
      return new Providers<>(null, (Class<?>) eventuallyProvider);
    }
    return new Providers<>(eventuallyProvider, (Class<Object>) eventuallyProvider.getClass());
  }

  /** Group providers partials with a single private binder when using builder. */
  private static class EventualModule implements Module {
    private final Providers<?>[] partials;

    EventualModule(Providers<?>... partials) {
      this.partials = partials;
    }

    @Override
    public void configure(Binder binder) {
      PrivateBinder privateBinder = binder.newPrivateBinder();
      for (Providers<?> partial : partials) {
        partial.configure(privateBinder);
      }
    }
  }

  /**
   * Builder to fluently create and handle eventual provider modules.
   */
  public static final class Builder {
    private final List<Module> modules = Lists.newArrayList();
    private final List<Providers<?>> partials = Lists.newArrayList();
    private boolean skipFailed;

    public Builder add(Module module) {
      modules.add(checkNotNull(module));
      return this;
    }

    public Builder add(Object providersInstance) {
      partials.add(createPartial(checkNotNull(providersInstance)));
      return this;
    }

    public Builder add(Class<?> providersClass) {
      return add((Object) providersClass);
    }

    public Builder skipFailed() {
      skipFailed = true;
      return this;
    }

    public ListenableFuture<Module> toFuture() {
      Injector futureInjecting = Guice.createInjector(eventualModules());
      return skipFailed
          ? successfulFrom(futureInjecting)
          : completedFrom(futureInjecting);
    }

    public Injector joinInjector() {
      try {
        Module module = Futures.getUnchecked(toFuture());
        return Guice.createInjector(module);
      } catch (UncheckedExecutionException ex) {
        Throwables.propagateIfInstanceOf(ex.getCause(), RuntimeException.class);
        throw Throwables.propagate(ex);
      } catch (Exception ex) {
        throw Throwables.propagate(ex);
      }
    }

    private List<Module> eventualModules() {
      List<Module> result = Lists.newArrayList(modules);
      result.add(new EventualModule(Iterables.toArray(partials, Providers.class)));
      return result;
    }
  }
}
