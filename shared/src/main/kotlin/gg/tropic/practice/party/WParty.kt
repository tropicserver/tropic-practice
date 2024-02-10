package gg.tropic.practice.party

import gg.scala.lemon.util.QuickAccess
import gg.scala.parties.model.Party
import gg.tropic.practice.lobbyGroup
import gg.tropic.practice.suffixWhenDev
import java.util.concurrent.CompletableFuture

/**
 * @author GrowlyX
 * @since 2/9/2024
 */
data class WParty(var delegate: Party)
{
    fun update(party: Party)
    {
        this.delegate = party
    }

    fun isInParty() = delegate
    fun onlinePracticePlayersInLobby() = CompletableFuture.supplyAsync {
        delegate.includedMembers()
            .associateWith { QuickAccess.server(it).join() }
            .filter {
                it.value?.groups
                    ?.contains(lobbyGroup().suffixWhenDev()) == true
            }
    }
}
