package gg.tropic.practice.party

import gg.scala.parties.model.Party

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
    fun allPlayersOnline() = Unit
}
