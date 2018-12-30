package com.template

import net.corda.core.contracts.*
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.transactions.LedgerTransaction

// *********
// * State *
// *********
data class FileState (
        val sender: Party,
        val receiver: Party,
        val comment: String,
        override val linearId: UniqueIdentifier = UniqueIdentifier()):
        LinearState {
        override val participants: List<AbstractParty>
        get() = listOf(sender, receiver)
}

