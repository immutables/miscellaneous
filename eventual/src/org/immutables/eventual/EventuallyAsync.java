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

import com.google.common.annotations.Beta;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.Executor;
import javax.annotation.meta.TypeQualifier;
import javax.inject.Qualifier;

/**
 * Qualifier annotation for {@link Executor} so it could be injected for asynchronous
 * transformation dispatching when used inside {@link EventualModules#providedBy(Class)}.
 * @see EventualModules
 */
@Retention(RetentionPolicy.RUNTIME)
@Qualifier
@TypeQualifier(applicableTo = Executor.class)
@Beta
public @interface EventuallyAsync {}
