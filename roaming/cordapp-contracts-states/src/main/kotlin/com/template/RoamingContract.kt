package com.template

import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.transactions.LedgerTransaction

class RoamingContract : Contract {
    companion object {
        // Used to identify our contract when building a transaction.
        @JvmStatic
        val ID = "com.template.RoamingContract"
    }

    // A transaction is valid if the verify() function of the contract of all the transaction's input and output states
    // does not throw an exception.
    override fun verify(tx: LedgerTransaction) {
        // Verification logic goes here.
    }

    // Used to indicate the transaction's intent.
    interface Commands : CommandData {
        class FileInitiate :  Commands
    }
}