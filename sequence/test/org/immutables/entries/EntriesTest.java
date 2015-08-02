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

import org.immutables.sequence.Entries;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import org.junit.Test;
import static org.immutables.check.Checkers.check;

// Need more tests
public class EntriesTest {
  @Test
  public void zippingIndex() {
    check(Entries.zippingIndex(ImmutableList.of("a", "b"))
        .map((i, s) -> s + i)
        .toMap()).hasToString("{0=a0, 1=b1}");
  }

  @Test
  public void zippingWith() {
    check(Entries.zippingWith(ImmutableList.of("a", "b", "a"), a -> "_" + a)
        .mapValues(a -> a + "_")
        .values()
        .toList())
        .isOf("_a_", "_b_", "_a_");
  }

  @Test
  public void zippingAndCollapse() {
    check(Entries.zipping(ImmutableList.of("a", "b"), ImmutableList.of(1, 1))
        .inverse()
        .groupByKey()
        .mapValues(v -> Joiner.on(':').join(v))
        .first()
        .get()
        .getValue()).is("a:b");
  }
}
