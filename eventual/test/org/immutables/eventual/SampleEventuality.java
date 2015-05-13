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

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Exposed;
import com.google.inject.Singleton;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import static com.google.common.base.Preconditions.checkArgument;

@Singleton
public class SampleEventuality {

  @Inject
  List<Integer> tracker;

  @Exposed
  @Eventually.Provides
  ListenableFuture<Boolean> getInput(@Named("input") String input) {
    tracker.add(1);
    return Futures.immediateFuture(Boolean.parseBoolean(input));
  }

  @Eventually.Provides
  @Named("second")
  String getSecond(Boolean go) {
    tracker.add(2);
    return "second=" + go;
  }

  @Eventually.Provides
  @Named("first")
  String getFirst(Boolean go) {
    checkArgument(go);
    tracker.add(2);
    return "first";
  }

  @Exposed
  @Eventually.Provides
  @Named("separator")
  String separator() {
    tracker.add(0);
    return ":";
  }

  @Exposed
  @Eventually.Provides
  @Named("output")
  String output(
      @Named("first") String first,
      @Named("second") String second,
      @Named("separator") String separator) {
    tracker.add(3);
    return first + separator + second;
  }
}
