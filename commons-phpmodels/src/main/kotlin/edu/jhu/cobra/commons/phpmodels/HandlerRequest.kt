package edu.jhu.cobra.commons.phpmodels

/**
 * The consumed request vocabulary of the effective object-handler facts: the
 * cast target type or the opcode one slot of a handler table is asked about.
 * Every member belongs to exactly one slot; the compare slot has no member
 * because a comparison states no request. The names are source tokens of the
 * engine's dispatch sites, protocol constants and never run settings.
 *
 * @property slot The handler slot a request is asked of.
 */
@Suppress("EnumNaming") // the names are the engine's source tokens
public enum class HandlerRequest(
    public val slot: HandlerSlot,
) {
    IS_STRING(HandlerSlot.CAST),
    IS_LONG(HandlerSlot.CAST),
    IS_DOUBLE(HandlerSlot.CAST),
    _IS_BOOL(HandlerSlot.CAST),
    _IS_NUMBER(HandlerSlot.CAST),
    IS_ARRAY(HandlerSlot.CAST),
    IS_RESOURCE(HandlerSlot.CAST),
    ZEND_ADD(HandlerSlot.OPERATION),
    ZEND_SUB(HandlerSlot.OPERATION),
    ZEND_MUL(HandlerSlot.OPERATION),
    ZEND_DIV(HandlerSlot.OPERATION),
    ZEND_MOD(HandlerSlot.OPERATION),
    ZEND_POW(HandlerSlot.OPERATION),
    ZEND_SL(HandlerSlot.OPERATION),
    ZEND_SR(HandlerSlot.OPERATION),
    ZEND_BW_OR(HandlerSlot.OPERATION),
    ZEND_BW_AND(HandlerSlot.OPERATION),
    ZEND_BW_XOR(HandlerSlot.OPERATION),
    ZEND_BW_NOT(HandlerSlot.OPERATION),
    ZEND_CONCAT(HandlerSlot.OPERATION),
    ZEND_BOOL_XOR(HandlerSlot.OPERATION),
    ZEND_BOOL_NOT(HandlerSlot.OPERATION),
}
