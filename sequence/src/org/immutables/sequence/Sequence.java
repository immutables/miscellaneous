/*
 * Copyright (C) 2008 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.immutables.sequence;

import com.google.common.annotations.Beta;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Ordering;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * {@code Sequence} provides a rich interface for manipulating {@code Iterable} instances in a
 * chained fashion. A {@code Sequence} can be created from an {@code Iterable}, or from a set
 * of elements. The following types of methods are provided on {@code Sequence}:
 * <ul>
 * <li>chained methods which return a new {@code Sequence} based in some way on the contents of the
 * current one (for example {@link #map})
 * <li>conversion methods which copy the {@code Sequence}'s contents into a new collection or array
 * (for example {@link #toList})
 * <li>element extraction methods which facilitate the retrieval of certain elements (for example
 * {@link #last})
 * <li>query methods which answer questions about the {@code Sequence}'s contents (for example
 * {@link #anyMatch})
 * </ul>
 * <p>
 * Here is an example that merges the lists returned by two separate database calls, transforms it
 * by invoking {@code toString()} on each element, and returns the first 10 elements as an
 * {@code ImmutableList}:
 * 
 * <pre>
 * {@code
 * 
 *   Sequence
 *       .from(database.getClientList())
 *       .filter(activeInLastMonth())
 *       .transform(Functions.toStringFunction())
 *       .limit(10)
 *       .toList();}
 * </pre>
 * <p>
 * Anything which can be done using {@code Sequence} could be done in a different fashion (often
 * with {@link Iterables}), however the use of {@code Sequence} makes many sets of operations
 * significantly more concise.
 * @author Marcin Mikosik
 * @param <E> element type
 * @since 12.0
 */
public abstract class Sequence<E> implements Iterable<E> {
  // We store 'iterable' and use it instead of 'this' to allow Iterables to perform instanceof
  // checks on the _original_ iterable when Sequence.from is used.
  private final Iterable<E> iterable;

  /** Constructor for use by subclasses. */
  protected Sequence() {
    this.iterable = this;
  }

  /**
   * Instantiates a new sequence.
   * @param iterable the iterable
   */
  Sequence(Iterable<E> iterable) {
    this.iterable = checkNotNull(iterable);
  }

  /**
   * Returns a fluent iterable that wraps {@code iterable}, or {@code iterable} itself if it
   * is already a {@code Sequence}.
   * @param <E> the element type
   * @param iterable the iterable
   * @return the sequence
   */
  public static <E> Sequence<E> from(final Iterable<E> iterable) {
    return (iterable instanceof Sequence) ? (Sequence<E>) iterable
        : new Sequence<E>(iterable) {
          @Override
          public Iterator<E> iterator() {
            return iterable.iterator();
          }
        };
  }

  /**
   * Construct a fluent iterable from another fluent iterable. This is obviously never necessary,
   * but is intended to help call out cases where one migration from {@code Iterable} to
   * {@code Sequence} has obviated the need to explicitly convert to a {@code Sequence}.
   * @param <E> the element type
   * @param iterable the iterable
   * @return the sequence
   * @deprecated instances of {@code Sequence} don't need to be converted to {@code Sequence}
   */
  @Deprecated
  public static <E> Sequence<E> from(Sequence<E> iterable) {
    return checkNotNull(iterable);
  }

  /**
   * Returns a fluent iterable containing {@code elements} in the specified order.
   * @param <E> the element type
   * @param elements the elements
   * @return the sequence
   * @since 18.0
   */
  @Beta
  public static <E> Sequence<E> of(E[] elements) {
    return from(Lists.newArrayList(elements));
  }

  /**
   * Returns a string representation of this fluent iterable, with the format
   * {@code [e1, e2, ..., en]}.
   * @return the string
   */
  @Override
  public String toString() {
    return Iterables.toString(iterable);
  }

  /**
   * Returns the number of elements in this fluent iterable.
   * @return the int
   */
  public final int size() {
    return Iterables.size(iterable);
  }

  /**
   * Returns {@code true} if this fluent iterable contains any object for which
   * {@code equals(element)} is true.
   * @param element the element
   * @return true, if successful
   */
  public final boolean contains(@Nullable Object element) {
    return Iterables.contains(iterable, element);
  }

  /**
   * Returns a fluent iterable whose {@code Iterator} cycles indefinitely over the elements of
   * this fluent iterable.
   * <p>
   * That iterator supports {@code remove()} if {@code iterable.iterator()} does. After
   * {@code remove()} is called, subsequent cycles omit the removed element, which is no longer in
   * this fluent iterable. The iterator's {@code hasNext()} method returns {@code true} until this
   * fluent iterable is empty.
   * <p>
   * <b>Warning:</b> Typical uses of the resulting iterator may produce an infinite loop. You should
   * use an explicit {@code break} or be certain that you will eventually remove all the elements.
   * @return the sequence
   */
  @CheckReturnValue
  public final Sequence<E> cycle() {
    return from(Iterables.cycle(iterable));
  }

  /**
   * Returns a fluent iterable whose iterators traverse first the elements of this fluent iterable,
   * followed by those of {@code other}. The iterators are not polled until necessary.
   * <p>
   * The returned iterable's {@code Iterator} supports {@code remove()} when the corresponding
   * {@code Iterator} supports it.
   * @param other the other
   * @return derived sequence
   * @since 18.0
   */
  @Beta
  @CheckReturnValue
  public final Sequence<E> append(Iterable<? extends E> other) {
    return from(Iterables.concat(iterable, other));
  }

  /**
   * Returns a fluent iterable whose iterators traverse first the elements of this fluent iterable,
   * followed by {@code elements}.
   * @param elements elements to apped
   * @return derived sequence
   * @since 18.0
   */
  @Beta
  @CheckReturnValue
  @SafeVarargs
  public final Sequence<E> append(E... elements) {
    return from(Iterables.concat(iterable, Arrays.asList(elements)));
  }

  public final <V> Entries<E, V> zip(Iterable<V> iterable) {
    return Entries.zip(this, iterable);
  }

  public final Entries<Integer, E> zipIndex() {
    return Entries.zipIndex(this);
  }

  public final <W> Entries<E, W> zipWith(Function<? super E, W> function) {
    return Entries.zipWith(this, function);
  }

  /**
   * Returns the elements from this fluent iterable that satisfy a predicate. The
   * resulting fluent iterable's iterator does not support {@code remove()}.
   * @param predicate the predicate
   * @return the sequence
   */
  @CheckReturnValue
  public final Sequence<E> filter(Predicate<? super E> predicate) {
    return from(Iterables.filter(iterable, predicate::test));
  }

  /**
   * Returns the elements from this fluent iterable that are instances of class {@code type}.
   * @param <T> the generic type
   * @param type the type of elements desired
   * @return the sequence
   */
  @CheckReturnValue
  public final <T> Sequence<T> filter(Class<T> type) {
    return from(Iterables.filter(iterable, type));
  }

  /**
   * Returns {@code true} if any element in this fluent iterable satisfies the predicate.
   * @param predicate the predicate
   * @return true, if successful
   */
  public final boolean anyMatch(Predicate<? super E> predicate) {
    return Iterables.any(iterable, predicate::test);
  }

  /**
   * Returns {@code true} if every element in this fluent iterable satisfies the predicate.
   * If this fluent iterable is empty, {@code true} is returned.
   * @param predicate the predicate
   * @return true, if successful
   */
  public final boolean allMatch(Predicate<? super E> predicate) {
    return Iterables.all(iterable, predicate::test);
  }

  /**
   * Returns an {@link Optional} containing the first element in this fluent iterable that
   * satisfies the given predicate, if such an element exists.
   * <p>
   * <b>Warning:</b> avoid using a {@code predicate} that matches {@code null}. If {@code null} is
   * matched in this fluent iterable, a {@link NullPointerException} will be thrown.
   * @param predicate the predicate
   * @return the optional
   */
  public final Optional<E> firstMatch(Predicate<? super E> predicate) {
    return Optional.ofNullable(Iterables.tryFind(iterable, predicate::test).orNull());
  }

  /**
   * Returns a fluent iterable that applies {@code function} to each element of this
   * fluent iterable.
   * <p>
   * The returned fluent iterable's iterator supports {@code remove()} if this iterable's iterator
   * does. After a successful {@code remove()} call, this fluent iterable no longer contains the
   * corresponding element.
   * @param <T> the generic type
   * @param function the function
   * @return the sequence
   */
  public final <T> Sequence<T> map(Function<? super E, T> function) {
    return from(Iterables.transform(iterable, function::apply));
  }

  /**
   * Applies {@code function} to each element of this fluent iterable and returns
   * a fluent iterable with the concatenated combination of results. {@code function} returns an
   * Iterable of results.
   * <p>
   * The returned fluent iterable's iterator supports {@code remove()} if this function-returned
   * iterables' iterator does. After a successful {@code remove()} call, the returned fluent
   * iterable no longer contains the corresponding element.
   * @param <T> derived sequence element type
   * @param function flatmap function
   * @return derived flattened sequence
   * @since 13.0 (required {@code Function<E, Iterable<T>>} until 14.0)
   */
  public <T> Sequence<T> flatMap(
      Function<? super E, ? extends Iterable<? extends T>> function) {
    return from(Iterables.concat(map(function)));
  }

  /**
   * Returns an {@link Optional} containing the first element in this fluent iterable.
   * If the iterable is empty, {@code Optional.absent()} is returned.
   * @return optional first element
   */
  public final Optional<E> first() {
    Iterator<E> iterator = iterable.iterator();
    return iterator.hasNext()
        ? Optional.of(iterator.next())
        : Optional.<E>empty();
  }

  /**
   * Returns an {@link Optional} containing the last element in this fluent iterable.
   * If the iterable is empty, {@code Optional.absent()} is returned.
   * @return the optional
   */
  public final Optional<E> last() {
    // Iterables#getLast was inlined here so we don't have to throw/catch a NSEE

    // TODO(kevinb): Support a concurrently modified collection?
    if (iterable instanceof List) {
      List<E> list = (List<E>) iterable;
      if (list.isEmpty()) {
        return Optional.empty();
      }
      return Optional.of(list.get(list.size() - 1));
    }
    Iterator<E> iterator = iterable.iterator();
    if (!iterator.hasNext()) {
      return Optional.empty();
    }

    /*
     * TODO(kevinb): consider whether this "optimization" is worthwhile. Users
     * with SortedSets tend to know they are SortedSets and probably would not
     * call this method.
     */
    if (iterable instanceof SortedSet) {
      SortedSet<E> sortedSet = (SortedSet<E>) iterable;
      return Optional.of(sortedSet.last());
    }

    while (true) {
      E current = iterator.next();
      if (!iterator.hasNext()) {
        return Optional.of(current);
      }
    }
  }

  /**
   * Returns a view of this fluent iterable that skips its first {@code numberToSkip} elements. If
   * this fluent iterable contains fewer than {@code numberToSkip} elements,
   * the returned fluent iterable skips all of its elements.
   * <p>
   * Modifications to this fluent iterable before a call to {@code iterator()} are reflected in the
   * returned fluent iterable. That is, the its iterator skips the first {@code numberToSkip}
   * elements that exist when the iterator is created, not when {@code skip()} is called.
   * <p>
   * The returned fluent iterable's iterator supports {@code remove()} if the {@code Iterator} of
   * this fluent iterable supports it. Note that it is <i>not</i> possible to delete the last
   * skipped element by immediately calling {@code remove()} on the returned fluent iterable's
   * iterator, as the {@code Iterator} contract states that a call to {@code * remove()} before a
   * call to {@code next()} will throw an {@link IllegalStateException}.
   * @param numberToSkip the number to skip
   * @return the sequence
   */
  @CheckReturnValue
  public final Sequence<E> skip(int numberToSkip) {
    return from(Iterables.skip(iterable, numberToSkip));
  }

  /**
   * Creates a fluent iterable with the first {@code size} elements of this
   * fluent iterable. If this fluent iterable does not contain that many elements,
   * the returned fluent iterable will have the same behavior as this fluent iterable.
   * The returned fluent iterable's iterator supports {@code remove()} if this
   * fluent iterable's iterator does.
   * @param size the maximum number of elements in the returned fluent iterable
   * @return the sequence
   */
  @CheckReturnValue
  public final Sequence<E> limit(int size) {
    return from(Iterables.limit(iterable, size));
  }

  /**
   * Determines whether this fluent iterable is empty.
   * @return true, if is empty
   */
  public final boolean isEmpty() {
    return !iterable.iterator().hasNext();
  }

  /**
   * Returns an {@code ImmutableList} containing all of the elements from this fluent iterable in
   * proper sequence.
   * @return the immutable list
   * @since 14.0 (since 12.0 as {@code toImmutableList()}).
   */
  public final ImmutableList<E> toList() {
    return ImmutableList.copyOf(iterable);
  }

  /**
   * Returns an {@code ImmutableList} containing all of the elements from this {@code Sequence} in
   * the order specified by {@code comparator}. To produce an {@code ImmutableList} sorted by its
   * natural ordering, use {@code toSortedList(Ordering.natural())}.
   * @param comparator the function by which to sort list elements
   * @return the immutable list
   * @since 14.0 (since 13.0 as {@code toSortedImmutableList()}).
   */
  public final ImmutableList<E> toSortedList(Comparator<? super E> comparator) {
    return Ordering.from(comparator).immutableSortedCopy(iterable);
  }

  /**
   * Returns an {@code ImmutableSet} containing all of the elements from this fluent iterable with
   * duplicates removed.
   * @return the immutable set
   * @since 14.0 (since 12.0 as {@code toImmutableSet()}).
   */
  public final ImmutableSet<E> toSet() {
    return ImmutableSet.copyOf(iterable);
  }

  /**
   * Returns an {@code ImmutableSortedSet} containing all of the elements from this {@code Sequence}
   * in the order specified by {@code comparator}, with duplicates
   * (determined by {@code comparator.compare(x, y) == 0}) removed. To produce an
   * {@code ImmutableSortedSet} sorted
   * by its natural ordering, use {@code toSortedSet(Ordering.natural())}.
   * @param comparator the function by which to sort set elements
   * @return the immutable sorted set
   * @since 14.0 (since 12.0 as {@code toImmutableSortedSet()}).
   */
  public final ImmutableSortedSet<E> toSortedSet(Comparator<? super E> comparator) {
    return ImmutableSortedSet.copyOf(comparator, iterable);
  }

  /**
   * Returns an immutable map for which the elements of this {@code Sequence} are the keys in
   * the same order, mapped to values by the given function. If this iterable contains duplicate
   * elements, the returned map will contain each distinct element once in the order it first
   * appears.
   * @param <V> the value type
   * @param valueFunction the value function
   * @return the immutable map
   * @since 14.0
   */
  public final <V> ImmutableMap<E, V> toMap(Function<? super E, V> valueFunction) {
    return Maps.toMap(iterable, valueFunction::apply);
  }

  /**
   * Creates an index {@code ImmutableListMultimap} that contains the results of applying a
   * specified function to each item in this {@code Sequence} of values. Each element of this
   * iterable will be stored as a value in the resulting multimap, yielding a multimap with the same
   * size as this iterable. The key used to store that value in the multimap will be the result of
   * calling the function on that value. The resulting multimap is created as an immutable snapshot.
   * In the returned multimap, keys appear in the order they are first encountered, and the values
   * corresponding to each key appear in the same order as they are encountered.
   * @param <K> the key type
   * @param keyFunction the function used to produce the key for each value
   * @return the immutable list multimap
   * @since 14.0
   */
  public final <K> ImmutableListMultimap<K, E> index(Function<? super E, K> keyFunction) {
    return Multimaps.index(iterable, keyFunction::apply);
  }

  /**
   * Returns an immutable map for which the {@link java.util.Map#values} are the elements of this
   * {@code Sequence} in the given order, and each key is the product of invoking a supplied
   * function on its corresponding value.
   * @param <K> the key type
   * @param keyFunction the function used to produce the key for each value
   * @return the immutable map
   * @since 14.0
   */
  public final <K> ImmutableMap<K, E> uniqueIndex(Function<? super E, K> keyFunction) {
    return Maps.uniqueIndex(iterable, keyFunction::apply);
  }

  /**
   * Returns an array containing all of the elements from this fluent iterable in iteration order.
   * @param type the type of the elements
   * @return a newly-allocated array into which all the elements of this fluent iterable have
   *         been copied
   */
  public final E[] toArray(Class<E> type) {
    return Iterables.toArray(iterable, type);
  }

  public Stream<E> stream() {
    return StreamSupport.stream(spliterator(), false);
  }

  /**
   * Copies all the elements from this fluent iterable to {@code collection}. This is equivalent to
   * calling {@code Iterables.addAll(collection, this)}.
   * @param <C> the generic type
   * @param collection the collection to copy elements to
   * @return {@code collection}, for convenience
   * @since 14.0
   */
  public final <C extends Collection<? super E>> C copyInto(C collection) {
    checkNotNull(collection);
    if (iterable instanceof Collection<?>) {
      collection.addAll(castIterable(iterable));
    } else {
      for (E item : iterable) {
        collection.add(item);
      }
    }
    return collection;
  }

  /**
   * Used to avoid http://bugs.sun.com/view_bug.do?bug_id=6558557
   */
  private static <T> Collection<T> castIterable(Iterable<T> iterable) {
    return (Collection<T>) iterable;
  }

  /**
   * Returns a {@link String} containing all of the elements of this fluent iterable joined with
   * {@code joiner}.
   * @param joiner the joiner
   * @return the string
   * @since 18.0
   */
  @Beta
  public final String join(Joiner joiner) {
    return joiner.join(this);
  }

  /**
   * Returns the element at the specified position in this fluent iterable.
   * @param position position of the element to return
   * @return the element at the specified position in this fluent iterable
   */
  public final E get(int position) {
    return Iterables.get(iterable, position);
  }

  public static <E> Function<Iterable<E>, Sequence<E>> fromIterable() {
    return FromIterableFunction.instance();
  }

  public static <T> Collector<T, Sequence<T>, Sequence<T>> toSequence() {
    return Collector.of(
        () -> Sequence.from(ImmutableList.<T>of()),
        (a, t) -> a.append(t),
        (a, b) -> a.append(b));
  }

  /**
   * Function that transforms {@code Iterable<E>} into a fluent iterable.
   */
  private static class FromIterableFunction<E>
      implements Function<Iterable<E>, Sequence<E>> {
    private static final Object INSTANCE = new FromIterableFunction<>();

    // safe unchecked: should be safe given how it will be used
    @SuppressWarnings("unchecked")
    static <E> Function<Iterable<E>, Sequence<E>> instance() {
      return (Function<Iterable<E>, Sequence<E>>) INSTANCE;
    }

    @Override
    public Sequence<E> apply(Iterable<E> fromObject) {
      return Sequence.from(fromObject);
    }
  }
}
