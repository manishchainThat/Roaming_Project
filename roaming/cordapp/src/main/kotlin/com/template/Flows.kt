package com.template

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.Command
import net.corda.core.contracts.requireThat
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import com.template.RoamingContract.Companion.ID
import net.corda.core.utilities.ProgressTracker.Step

// *********
// * Flows *
// *********
@InitiatingFlow
@StartableByRPC
class FileInitiateFlow(
        val receiver: Party,
        val comment: String,
        val hash: SecureHash.SHA256) : FlowLogic<SignedTransaction>() {
        companion object {
        object GENERATING_TRANSACTION : Step("Generating transaction")
        object VERIFYING_TRANSACTION : Step("Verifying contract constraints.")
        object SIGNING_TRANSACTION : Step("Signing transaction with sender private key.")
        object GATHERING_SIGS : Step("Gathering the receiver's signature.") {
            override fun childProgressTracker() = CollectSignaturesFlow.tracker()
        }

        object FINALISING_TRANSACTION : Step("Obtaining notary signature and recording transaction.") {
            override fun childProgressTracker() = FinalityFlow.tracker()
        }

        fun tracker() = ProgressTracker(
                GENERATING_TRANSACTION,
                VERIFYING_TRANSACTION,
                SIGNING_TRANSACTION,
                GATHERING_SIGS,
                FINALISING_TRANSACTION
        )
    }

    override val progressTracker = tracker()


    @Suspendable
    override fun call(): SignedTransaction {
        // Obtain a reference to the notary we want to use.
        val notary = serviceHub.networkMapCache.notaryIdentities[0]
        val sender = serviceHub.myInfo.legalIdentities.first()
        // Stage 1.
        progressTracker.currentStep = GENERATING_TRANSACTION
        // Generate an unsigned transaction.
        val fileState = FileState(sender, receiver,comment)
        val txCommand = Command(RoamingContract.Commands.FileInitiate(), fileState.participants.map { it.owningKey })
        val txBuilder = TransactionBuilder(notary)
                .addOutputState(fileState, ID)
                .addCommand(txCommand)
                .addAttachment(hash)

        // Stage 2.
        progressTracker.currentStep = VERIFYING_TRANSACTION
        // Verify that the transaction is valid.
        txBuilder.verify(serviceHub)

        // Stage 3.
        progressTracker.currentStep = SIGNING_TRANSACTION
        // Sign the transaction.
        val partSignedTx = serviceHub.signInitialTransaction(txBuilder)

        // Stage 4.
        progressTracker.currentStep = GATHERING_SIGS
        // Send the state to the counterparty, and receive it back with their signature.
        val otherPartyFlow = initiateFlow(receiver)
        val fullySignedTx = subFlow(CollectSignaturesFlow(partSignedTx, setOf(otherPartyFlow), GATHERING_SIGS.childProgressTracker()))

        // Stage 5.
        progressTracker.currentStep = FINALISING_TRANSACTION
        // Notarise and record the transaction in both parties' vaults.
        return subFlow(FinalityFlow(fullySignedTx, FINALISING_TRANSACTION.childProgressTracker()))
    }
}

@InitiatedBy(FileInitiateFlow::class)
class FileInitiateRespond(val senderFlow: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction{
        val signedTransactionFlow = object : SignTransactionFlow(senderFlow) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                val output = stx.tx.outputs.single().data
                "This must be an File State" using (output is FileState)
            }
        }
        return subFlow(signedTransactionFlow)
    }
}
