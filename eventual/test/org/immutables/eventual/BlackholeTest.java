package org.immutables.eventual;

import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Singleton;
import org.junit.Test;
import static org.immutables.check.Checkers.check;

@Singleton
class SampleBlackholeProviders {
  final AtomicInteger counter = new AtomicInteger();

  @Eventually.Provides
  Integer value() {
    return 10;
  }

  @Eventually.Provides
  void init() {
    counter.incrementAndGet();
  }

  @Eventually.Provides
  void init(Integer value) {
    counter.addAndGet(value);
  }
}

public class BlackholeTest {

  @Test
  public void blackhole() {
    SampleBlackholeProviders providers = new SampleBlackholeProviders();

    new EventualModules.Builder()
        .add(providers)
        .joinInjector();

    check(providers.counter.get()).is(11);
  }
}
