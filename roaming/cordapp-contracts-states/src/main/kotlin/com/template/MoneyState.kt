package com.template

import net.corda.core.contracts.*
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.transactions.LedgerTransaction

// *********
// * State *
// *********
data class MoneyState(
        val sender: Party,
        val receiver: Party,
        val amount: Double,
        val currency: String,
        val comment: String):ContractState {
    override val participants: List<AbstractParty>
        get() = listOf(sender, receiver)
}