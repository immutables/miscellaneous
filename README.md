# Miscellaneous

[![Build Status](https://travis-ci.org/immutables/miscellaneous.svg?branch=master)](https://travis-ci.org/immutables/immutables)

## org.immutables.eventual

Guice add-ons to resolve future-provided dependencies.

Creates special mix-in module created from defining class with special asynchronous
transformation methods annotated with `@EventuallyProvides`, witch is asyncronous analog to Guice's `@Provides`. Used to annotate asynchronous provider methods used to describe transformation of async values.

```java

@Eventually.Provides
C combine(A a, B b) {
  return new C(a.a(), b.b());
}
```

In this provider method above we created binding for `ListenableFuture<C>` that will be functionally equivalent to the following regular `@Provides` method:

```java
@Provides
ListenableFuture<B> combine(ListenableFuture<A> a, ListenableFuture<B> b) {
  return Futures.transform(Futures.allAsList(Arrays.asList(a, b)),
      (List<Object> input) -> {
        A a = (A) input.get(0);
        B b = (B) input.get(1);
        return new B(a.a(), b.b());
      });
}
```

Here's more involved example

```java
@Singleton // all futures will be singletons
public class Providers { // no need to extend AbstractModule or implement Module

  @Eventually.Provides
  A createA() {
    return ...;
  }

  @Eventually.Provides
  ListenableFuture<B> loadB() {
    // loading of B is itself asyncronous, so we return future
    return ...;
  }

  @Eventually.Provides
  C combine(A a, B b) {
    // when A and B ready, we calculate C
    return new C(a.value(), b.calculate());
  }

  // Only exposed values can be injected between modules,
  // All other are hidden inside private module
  @Exposed
  @Eventually.Provides
  Z transformed(C c) {
    // when C is ready, we calculate Z from it
    return c.transformed();
  }
}

Module futureModule = EventualModules.definedBy(new Providers());

ListenableFuture<Module> completedModule =
    EventualModules.completedFrom(Guice.createInjector(futureModule));

// inject Z when all futures completed
Z z = Guice.createInjector(completedModule.get()).getInstance(Z.class);
```

Having dependency on `ListenableFuture<A>` and `ListenableFuture<B>`, this module exposed
combined and transformed `ListenableFuture<Z>` available to injector.

While super-classes could be used and will be scanned for such methods, method overriding is
not handled properly so avoid overriding provider methods. Use delegation to regular methods if some functionality should be implemented or overridden.

In order to customize dispatching, injector could provided with binding to
`@Eventually.Async Executor`. So you need to mix in

On the very abstract level this library achives transformation: `P(A, B... Z) -> I(A, B... Z)`:

* Define providers `P(A, B... Z)`
* and use it create (_definedBy_) a module `M(F[A], F[B]... F[Z])`,
* then use it to create (_createInjector_) injector `I(F[A], F[B]... F[Z])`
* use injector to get future (_completedFrom_) module `F[M(A, B... Z)]`
* when future is fulfilled, you create injector `I(A, B... Z)` from the dereferenced module.

The alternative would be to use plain composition of futures. But even with java 8 lambdas, it's still might be cumbersome to reason about complicated chains of transformation. But, definitely, this kind of utility might be also be built specifically for Java 8 without using Guice.
This is built with Guava's `ListenableFuture` and not with `CompletableFuture`, sorry.
