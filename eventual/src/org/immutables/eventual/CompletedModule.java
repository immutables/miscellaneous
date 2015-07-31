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

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Binder;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;
import java.lang.reflect.ParameterizedType;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;

final class CompletedModule implements Module {
  private final Key<?>[] keys;
  private final Object[] instances;

  private CompletedModule(Key<?>[] keys, Object[] instances) {
    assert keys.length == instances.length;
    this.keys = keys;
    this.instances = instances;
  }

  @Override
  public void configure(Binder binder) {
    for (int i = 0; i < keys.length; i++) {
      // safe cast, key and value known to have corresponding types
      @SuppressWarnings("unchecked") Key<Object> key = (Key<Object>) keys[i];
      @Nullable Object instance = instances[i];
      // instance might be null for failed/skipped futures
      if (instance != null) {
        binder.bind(key).toInstance(instance);
      }
    }
  }

  enum CompletionCriteria {
    ALL,
    SUCCESSFUL
  }

  static ListenableFuture<Module> from(Injector injectingFutures, CompletionCriteria criteria) {
    LinkedHashMap<Key<?>, Key<?>> keyMapping = mapUnfutureKeys(injectingFutures);
    List<ListenableFuture<?>> listOfFutures = getFutureInstances(keyMapping.keySet(), injectingFutures);

    ListenableFuture<List<Object>> futureOfList =
        criteria == CompletionCriteria.SUCCESSFUL
            ? Futures.successfulAsList(listOfFutures)
            : Futures.allAsList(listOfFutures);

    return Futures.transform(futureOfList, new Function<List<Object>, Module>() {
      Key<?>[] keys = Iterables.toArray(keyMapping.values(), Key.class);

      @Override
      public Module apply(List<Object> instances) {
        return new CompletedModule(keys, instances.toArray());
      }
    });
  }

  private static List<ListenableFuture<?>> getFutureInstances(Set<Key<?>> keys, Injector injector) {
    List<ListenableFuture<?>> futures = Lists.newArrayList();
    for (Key<?> key : keys) {
      futures.add((ListenableFuture<?>) injector.getInstance(key));
    }
    return futures;
  }

  private static LinkedHashMap<Key<?>, Key<?>> mapUnfutureKeys(Injector injector) {
    LinkedHashMap<Key<?>, Key<?>> keyMapping = Maps.newLinkedHashMap();

    for (Key<?> key : injector.getBindings().keySet()) {
      TypeLiteral<?> typeLiteral = key.getTypeLiteral();

      if (ListenableFuture.class.isAssignableFrom(typeLiteral.getRawType())) {
        ParameterizedType parametrizedType = ((ParameterizedType) typeLiteral.getType());
        TypeLiteral<?> deferefencedType = TypeLiteral.get(parametrizedType.getActualTypeArguments()[0]);
        keyMapping.put(key, key.ofType(deferefencedType));
      }
    }
    return keyMapping;
  }
}
