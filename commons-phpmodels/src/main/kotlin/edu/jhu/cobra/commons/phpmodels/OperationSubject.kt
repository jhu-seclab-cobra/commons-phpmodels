package edu.jhu.cobra.commons.phpmodels

import com.fasterxml.jackson.annotation.JsonCreator

/** A library-defined consuming operation, independent of the declaration a call resolves to. */
public class OperationSubject private constructor(name: String) : NamedSubject("operation", name) {
    public companion object {
        /** The dynamic invocation operation; its selector is the callee input. */
        public val DYNAMIC_INVOCATION: OperationSubject = OperationSubject("dynamic_invocation")

        /** @throws IllegalArgumentException If [raw] names no defined operation. */
        @JvmStatic
        @JsonCreator
        public fun parse(raw: String): OperationSubject {
            require(raw == DYNAMIC_INVOCATION.name) { "Unknown operation subject: '$raw'" }
            return DYNAMIC_INVOCATION
        }
    }
}
