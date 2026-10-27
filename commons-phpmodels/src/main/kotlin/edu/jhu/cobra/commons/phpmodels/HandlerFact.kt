package edu.jhu.cobra.commons.phpmodels

/** The slots of an object-handler table a fact describes. */
public enum class HandlerSlot { CAST, COMPARE, OPERATION }

/**
 * How a string cast of an object consults the effective `__toString` method:
 * no method call, only the method, the method before native conversion, or
 * native conversion before the method. Never a success flag.
 */
public enum class StringDispatch { NONE, METHOD_ONLY, METHOD_FIRST, NATIVE_FIRST }

/**
 * One target of a present fact: the source-qualified C function serving the
 * slot, with the normal-result kinds it can produce and, for a string cast,
 * its dispatch. Results and dispatch stay together with their target.
 *
 * @property symbol The source-qualified C function identity.
 * @property results Kinds of the possible normal results; empty when the
 *   target has no normal return for the request.
 * @property stringDispatch The string dispatch order; set exactly on a cast to
 *   string and checked by the owning [HandlerFact].
 * @throws IllegalArgumentException If the symbol is blank or a result is null.
 */
public data class HandlerTarget(
    val symbol: String,
    val results: Set<ReturnKind>,
    val stringDispatch: StringDispatch? = null,
) {
    init {
        require(symbol.isNotBlank()) { "Handler target declares a blank symbol" }
        require(results.none { (it as ReturnKind?) == null }) { "Handler target $symbol holds a null result kind" }
    }
}

/**
 * One slot of a handler table for one request: positively absent (the slot is
 * NULL in this table) or present with at least one target.
 *
 * @property slot The described slot.
 * @property request The cast target or operation opcode; null for the compare
 *   slot and for an absent fact covering the whole slot.
 * @property targets The targets of a present fact; empty for an absent fact.
 * @property absent True when the slot is NULL in the table.
 * @throws IllegalArgumentException If a request does not belong to the slot; a
 *   present cast or operation fact has no request or a compare fact has one;
 *   an absent fact has targets or a present fact has none; a target is null; a
 *   cast to string lacks a dispatch or another request states one.
 */
public data class HandlerFact(
    val slot: HandlerSlot,
    val request: HandlerRequest? = null,
    val targets: List<HandlerTarget> = emptyList(),
    val absent: Boolean = false,
) {
    init {
        require(request == null || request.slot == slot) { "Request $request does not belong to slot $slot" }
        require(targets.none { (it as HandlerTarget?) == null }) { "Handler fact holds a null target" }
        if (absent) {
            require(targets.isEmpty()) { "An absent $slot fact states no target" }
        } else {
            require(targets.isNotEmpty()) { "A present $slot fact states at least one target" }
            require((slot == HandlerSlot.COMPARE) == (request == null)) {
                "A present $slot fact ${if (slot == HandlerSlot.COMPARE) "takes no" else "requires a"} request"
            }
            val wantsDispatch = request == HandlerRequest.IS_STRING
            targets.forEach { target ->
                require((target.stringDispatch != null) == wantsDispatch) {
                    val need = if (wantsDispatch) "requires" else "takes no"
                    "Target ${target.symbol} of $slot $request $need a string dispatch"
                }
            }
        }
    }
}

/**
 * One effective object-handler table alternative of a class, identified by its
 * source. An unresolved reachable table has no facts; a table with partial
 * coverage omits its unestablished requests. List order gives no priority.
 *
 * @property id The source provenance: relative source path and table symbol,
 *   with a deterministic suffix for different effective contents; opaque to
 *   consumers.
 * @property facts The established facts.
 * @throws IllegalArgumentException If the id is blank; a fact is null; two
 *   facts share a slot and request; a slot holds an absent and a present
 *   fact, whatever their requests; or a whole-slot absence is not the sole
 *   fact of its slot.
 */
public data class HandlerTable(
    val id: String,
    val facts: List<HandlerFact>,
) {
    init {
        require(id.isNotBlank()) { "Handler table declares a blank id" }
        require(facts.none { (it as HandlerFact?) == null }) { "Handler table $id holds a null fact" }
        val duplicate = facts.groupingBy { it.slot to it.request }.eachCount().entries.firstOrNull { it.value > 1 }
        require(duplicate == null) { "Handler table $id repeats ${duplicate?.key}" }
        facts.groupBy { it.slot }.forEach { (slot, ofSlot) ->
            require(ofSlot.all { it.absent } || ofSlot.none { it.absent }) {
                "Handler table $id states $slot both absent and present"
            }
            require(ofSlot.size == 1 || ofSlot.none { it.absent && it.request == null }) {
                "Handler table $id states a whole-slot absence of $slot beside other facts"
            }
        }
    }
}
