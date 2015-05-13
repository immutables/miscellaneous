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

import javax.inject.Singleton;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Exposed;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import org.junit.Test;
import static org.immutables.check.Checkers.check;

public class CompletedFutureModuleTest {
  @Test
  public void futuresDereferencing() {
    Injector injectorWithFutures = Guice.createInjector(EventualModules.providedBy(new SampleFutureProvider()));
    ListenableFuture<Module> futureModule = EventualModules.completedFrom(injectorWithFutures);
    Injector resultModule = Guice.createInjector(Futures.getUnchecked(futureModule));

    check(resultModule.getInstance(Integer.class)).is(1);
    check(resultModule.getInstance(String.class)).is("a");
  }

  @Singleton
  static class SampleFutureProvider {
    @Exposed
    @Eventually.Provides
    Integer integer() {
      return 1;
    }

    @Exposed
    @Eventually.Provides
    String string() {
      return "a";
    }
  }
}
