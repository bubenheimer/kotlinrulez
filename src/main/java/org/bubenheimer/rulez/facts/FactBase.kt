/*
 * Copyright (c) 2015-2020 Uli Bubenheimer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.bubenheimer.rulez.facts

import java.util.concurrent.atomic.AtomicInteger

/**
 * Fact tracker tied to a specific rule base
 */
public class FactBase {
    /**
     * The current number of generated [Fact]s. Bounded by [MAX_FACTS].
     */
    private var factCount = AtomicInteger()

    /**
     * Allocates a new [Fact.id]
     *
     * @throws IndexOutOfBoundsException when the number of facts exceeds [MAX_FACTS]
     */
    @Throws(IndexOutOfBoundsException::class)
    internal fun allocateFactId() = factCount.getAndIncrement().also {
        if (MAX_FACTS <= it) throw IndexOutOfBoundsException("Too many facts")
    }

    public companion object {
        /**
         * The maximum number of [Fact]s
         */
        public const val MAX_FACTS: Int = UInt.SIZE_BITS
    }
}
