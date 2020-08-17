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
package org.bubenheimer.rulez.rules

import kotlinx.coroutines.CoroutineScope
import org.bubenheimer.rulez.facts.Fact
import org.bubenheimer.rulez.state.State
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * A rule
 */
interface Rule {
    /**
     *  A disjunction of conjunctions that must hold to let the rule fire
     */
    val conditions: Iterable<FactVector>

    /**
     * A disjunction of conjunctions that must not hold to let the rule fire
     */
    val negConditions: Iterable<FactVector>

    /**
     * The rule action to execute when the rule fires
     */
    val action: RuleAction

    /**
     * Evaluates the rule's left-hand side
     * @param state the fact state to use for evaluation
     * @return whether the left-hand side matches the fact state
     */
    //TODO optimization possible for the common case of disjoint negated conditions
    fun eval(state: State): Boolean =
        conditions.all(state::matches) && !negConditions.any(state::matches)
}

/**
 * Result of rule action.
 */
@Suppress("NON_PUBLIC_PRIMARY_CONSTRUCTOR_OF_INLINE_CLASS")
inline class ActionResult internal constructor(private val value: ULong) {
    /**
     * Empty result. Prefer to use [VOID] constant instead.
     */
    constructor() : this(FactVector.EMPTY, FactVector.EMPTY)

    /**
     * @param remove single [Fact] to remove
     * @param add single [Fact] to add
     */
    constructor(remove: Fact? = null, add: Fact? = null) :
            this(remove?.toVector() ?: FactVector.EMPTY, add?.toVector() ?: FactVector.EMPTY)

    /**
     * @param remove vector of [Fact]s to remove
     * @param add vector of [Fact]s to add
     */
    constructor(remove: FactVector = FactVector.EMPTY, add: FactVector = FactVector.EMPTY) :
            this((remove.value.toULong() shl UInt.SIZE_BITS) or add.value.toULong())

    /**
     * Obtain vector of [Fact]s to remove
     */
    internal val removeVector get() = FactVector((value shr UInt.SIZE_BITS).toUInt())

    /**
     * Obtain vector of [Fact]s to add
     */
    internal val addVector get() = FactVector(value.toUInt())

    override fun toString() = "{remove = $removeVector, add = $addVector}"

    companion object {
        /**
         * An empty result that does not cause any changes.
         */
        val VOID = ActionResult()
    }
}

/**
 * Represents the action (right-hand side) of a rule with code to execute when the left-hand side
 * matches during rule evaluation.
 */
interface RuleAction {
    /**
     * Context to launch rule action coroutine in. This context is merged with its parent
     * CoroutineContext and a fresh `Job`.
     */
    val context: CoroutineContext

    /**
     * Rule action coroutine. The CoroutineScope receiver contains the merged context and the new
     * `Job`.
     */
    val block: suspend CoroutineScope.() -> ActionResult
}

/**
 * Creates a [RuleAction]
 */
@Suppress("FunctionName")
fun RuleAction(
    /**
     * Context to launch rule action coroutine in. This context is merged with its parent
     * CoroutineContext and a fresh `Job`.
     */
    context: CoroutineContext = EmptyCoroutineContext,
    /**
     * Rule action coroutine. The CoroutineScope receiver contains the merged context and the new
     * `Job`.
     */
    block: suspend CoroutineScope.() -> ActionResult
) = object : RuleAction {
    override val context = context
    override val block = block
}

/**
 * A named rule
 *
 * @param name the rule name for debugging
 * @param rule the base rule
 */
open class NamedRule(val name: String, private val rule: Rule) : Rule by rule {
    override fun toString() = name
}

/**
 * A conjunction of [Fact]s. Any [Conjunction] is also considered a single-element [Disjunction].
 */
interface Conjunction : Disjunction {
    val conjuncts: Iterable<Fact>

    override val disjuncts get() = listOf(this)
}

/**
 * A disjunction of conjunctions of facts
 */
interface Disjunction {
    val disjuncts: Iterable<Conjunction>
}

/**
 * A disjunction that must hold for a rule to fire.
 */
interface Given : Disjunction

/**
 * Represents a rule's left-hand side in the DSL
 */
interface Proposition {
    /**
     * A disjunction of conjunctions of facts. The rule premise is invalidated if all
     * conjunctions are invalid.
     */
    val given: Given

    /**
     * A disjunction of conjunctions of facts. The rule premise is invalidated if any
     * conjunction holds.
     */
    val givenNot: Disjunction
}

/**
 * What's [Given] to be true
 */
fun given(disjunction: Disjunction): Given = Giv(disjunction.disjuncts)

/**
 * What's given to not be true
 */
fun givenNot(disjunction: Disjunction): Proposition = Propo(Giv(emptyList()), disjunction)

/**
 * Combine conjunctions
 */
infix fun Conjunction.and(conjunction: Conjunction): Conjunction =
    Con(conjuncts + conjunction.conjuncts)

/**
 * Combine disjunctions
 */
infix fun Disjunction.or(disjunction: Disjunction): Disjunction =
    Dis(disjuncts + disjunction.disjuncts)

infix fun Given.andNot(disjunction: Disjunction): Proposition = Propo(this, disjunction)

/**
 * Defines a rule with an empty left-hand side that always fires.
 */
fun then(action: RuleAction): Rule =
    BaseRule(
        conditions = emptyList(),
        negConditions = emptyList(),
        action = action
    )

/**
 * Defines a rule
 */
infix fun Given.then(action: RuleAction): Rule =
    BaseRule(
        conditions = disjuncts.map { it.conjuncts.toVector() },
        negConditions = emptyList(),
        action = action
    )

/**
 * Defines a rule
 */
infix fun Proposition.then(action: RuleAction): Rule =
    BaseRule(
        conditions = given.disjuncts.map { it.conjuncts.toVector() },
        negConditions = givenNot.disjuncts.map { it.conjuncts.toVector() },
        action = action
    )

private class Con(override val conjuncts: Iterable<Fact>) : Conjunction

private open class Dis(override val disjuncts: Iterable<Conjunction>) : Disjunction

private class Giv(disjuncts: Iterable<Conjunction>) : Dis(disjuncts), Given

private class Propo(
    override val given: Given,
    override val givenNot: Disjunction
) : Proposition

/**
 * A basic rule without a name
 *
 * @param conditions a disjunction of conjunctions that must hold to let the rule fire
 * @param negConditions a disjunction of conjunctions that must not hold to let the rule fire
 * @param action the rule action to execute when the rule fires
 */
private class BaseRule(
    override val conditions: Iterable<FactVector>,
    override val negConditions: Iterable<FactVector>,
    override val action: RuleAction
) : Rule
