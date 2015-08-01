/*
    Copyright 2015 Immutables Authors and Contributors

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
package org.immutables.entries;

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * Lazily transformed fluent iterable of {@code Map.Entry<K, V>}
 * @param <K> key, left type
 * @param <V> va
 */
@NotThreadSafe
public abstract class EntryIterable<K, V> implements Iterable<Entry<K, V>> {
  protected EntryIterable() {}

  public <T, W> EntryIterable<T, W> mapEntries(BiFunction<? super K, ? super V, Entry<T, W>> function) {
    return from(transform(e -> function.apply(e.getKey(), e.getValue())));
  }

  private <T, W> Iterable<Entry<T, W>> transform(Function<Entry<K, V>, Entry<T, W>> function) {
    return entries().transform(function::apply);
  }

  public EntryIterable<K, V> filter(BiPredicate<? super K, ? super V> predicate) {
    return from(entries().filter(e -> predicate.test(e.getKey(), e.getValue())));
  }

  public static <K, V> EntryIterable<K, V> of(K key, V value) {
    return from(ImmutableMap.of(key, value));
  }

  public static <V> EntryIterable<Integer, V> zippingIndex(Iterable<? extends V> values) {
    return from(new Iterable<Entry<Integer, V>>() {
      @Override
      public Iterator<Entry<Integer, V>> iterator() {
        return new AbstractIterator<Entry<Integer, V>>() {
          Iterator<? extends V> vs = values.iterator();
          int index;

          @Override
          protected Entry<Integer, V> computeNext() {
            return vs.hasNext()
                ? entry(index++, vs.next())
                : endOfData();
          }
        };
      }
    });
  }

  public static <V, T> EntryIterable<V, T> zippingWith(
      Iterable<? extends V> values,
      Function<? super V, T> function) {
    return from(new Iterable<Entry<V, T>>() {
      @Override
      public Iterator<Entry<V, T>> iterator() {
        return new AbstractIterator<Entry<V, T>>() {
          Iterator<? extends V> vs = values.iterator();

          @Override
          protected Entry<V, T> computeNext() {
            if (vs.hasNext()) {
              V v = vs.next();
              return entry(v, function.apply(v));
            }
            return endOfData();
          }
        };
      }
    });
  }

  public static <K, V> EntryIterable<K, V> zipping(
      Iterable<? extends K> keys,
      Iterable<? extends V> values) {
    return from(new Iterable<Entry<K, V>>() {
      @Override
      public Iterator<Entry<K, V>> iterator() {
        return new AbstractIterator<Entry<K, V>>() {
          final Iterator<? extends K> ks = keys.iterator();
          final Iterator<? extends V> vs = values.iterator();

          @Override
          protected Entry<K, V> computeNext() {
            return ks.hasNext() && vs.hasNext()
                ? entry(ks.next(), vs.next())
                : endOfData();
          }
        };
      }
    });
  }

  public void forEach(BiConsumer<K, V> consumer) {
    entries().forEach(e -> consumer.accept(e.getKey(), e.getValue()));
  }

  public static <K, V> EntryIterable<K, V> index(
      Iterable<V> values,
      Function<? super V, K> keyFunction) {
    return from(new Iterable<Entry<K, V>>() {
      @Override
      public Iterator<Entry<K, V>> iterator() {
        return Multimaps.index(values, v -> keyFunction.apply(v))
            .entries()
            .iterator();
      }
    });
  }

  public static <K, V> EntryIterable<K, V> indexAll(
      Iterable<V> values,
      Function<? super V, ? extends Iterable<? extends K>> keysFunction) {
    return from(new Iterable<Entry<K, V>>() {
      @Override
      public Iterator<Entry<K, V>> iterator() {
        ListMultimap<K, V> listMultimap = ArrayListMultimap.create();
        for (V v : values) {
          for (K k : keysFunction.apply(v)) {
            listMultimap.put(k, v);
          }
        }
        return listMultimap.entries().iterator();
      }
    });
  }

  public EntryIterable<K, V> unique() {
    EntryIterable<K, V> self = this;
    return from(new Iterable<Entry<K, V>>() {
      @Override
      public Iterator<Entry<K, V>> iterator() {
        return HashMultimap.create(self.toMultimap())
            .entries()
            .iterator();
      }
    });
  }

  public EntryIterable<K, Collection<V>> groupByKey() {
    return from(new Iterable<Entry<K, Collection<V>>>() {
      @Override
      public Iterator<Entry<K, Collection<V>>> iterator() {
        return toMultimap().asMap().entrySet().iterator();
      }
    });
  }

  public FluentIterable<V> values() {
    return entries().transform(Entry::getValue);
  }

  public FluentIterable<K> keys() {
    return entries().transform(Entry::getKey);
  }

  public FluentIterable<Entry<K, V>> entries() {
    return FluentIterable.from(this);
  }

  public <T, W> EntryIterable<T, W> map(
      Function<? super K, T> keyFunction,
      Function<? super V, W> valueFunction) {
    return from(transform(e -> {
      T t = keyFunction.apply(e.getKey());
      W w = valueFunction.apply(e.getValue());
      return entry(t, w);
    }));
  }

  public <W> EntryIterable<K, W> map(BiFunction<? super K, ? super V, W> function) {
    return from(transform(e -> {
      K k = e.getKey();
      W w = function.apply(k, e.getValue());
      return entry(k, w);
    }));
  }

  public <W> EntryIterable<K, W> flatMap(BiFunction<? super K, ? super V, ? extends Iterable<W>> function) {
    return from(entries().transformAndConcat(e -> {
      K k = e.getKey();
      V v = e.getValue();

      Iterable<W> c = function.apply(k, v);

      Iterable<Entry<K, W>> entries =
          Iterables.transform(c, w -> entry(k, w));

      return entries;
    }));
  }

  public <W> EntryIterable<K, W> flatMapValues(Function<? super V, ? extends Iterable<W>> function) {
    return from(entries().transformAndConcat(e -> {
      K k = e.getKey();
      V v = e.getValue();

      Iterable<W> c = function.apply(v);

      Iterable<Entry<K, W>> entries =
          Iterables.transform(c, w -> entry(k, w));

      return entries;
    }));
  }

  public <W> EntryIterable<K, W> mapValues(Function<? super V, W> function) {
    return from(transform(e -> {
      K k = e.getKey();
      W w = function.apply(e.getValue());
      return entry(k, w);
    }));
  }

  public <T> EntryIterable<T, V> mapKeys(Function<? super K, T> function) {
    return from(transform(e -> {
      T t = function.apply(e.getKey());
      V v = e.getValue();
      return entry(t, v);
    }));
  }

  public ImmutableMap<K, V> toMap() {
    ImmutableMap.Builder<K, V> builder = ImmutableMap.builder();
    forEach(e -> builder.put(e));
    return builder.build();
  }

  public ImmutableListMultimap<K, V> toMultimap() {
    ImmutableListMultimap.Builder<K, V> builder = ImmutableListMultimap.builder();
    forEach(e -> builder.put(e));
    return builder.build();
  }

  public Optional<Entry<K, V>> first() {
    return Optional.ofNullable(
        entries().first().orNull());
  }

  public static <K, V> Entry<K, V> entry(K key, V value) {
    return Maps.immutableEntry(key, value);
  }

  public static <K, V> Entry<V, K> inverseEntry(Entry<K, V> entry) {
    return entry(entry.getValue(), entry.getKey());
  }

  public EntryIterable<V, K> inverse() {
    return from(transform(e -> inverseEntry(e)));
  }

  public static <K, V> EntryIterable<K, V> from(Map<? extends K, ? extends V> map) {
    return from(map.entrySet());
  }

  public static <K, V> EntryIterable<K, V> from(Multimap<? extends K, ? extends V> map) {
    return from(map.entries());
  }

  // safe unchecked: cannot insert anything and types are compartible on read
  @SuppressWarnings("unchecked")
  public static <K, V> EntryIterable<K, V> from(
      Iterable<? extends Entry<? extends K, ? extends V>> iterable) {
    if (iterable instanceof EntryIterable<?, ?>) {
      return (EntryIterable<K, V>) iterable;
    }
    return new EntryIterable<K, V>() {
      @Override
      public Iterator<Entry<K, V>> iterator() {
        return (Iterator<Entry<K, V>>) iterable.iterator();
      }
    };
  }
}
