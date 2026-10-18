package dev.railson.ticketsales.core.domain.usecase.payment

import dev.railson.ticketsales.core.domain.terminal.ListedOrder
import dev.railson.ticketsales.core.domain.terminal.OrdersPage

/**
 * O plano da busca de um `reference` na lista de Cielo Orders (ADR 0001, item 4). Não sabe nada de disco nem de
 * maquininha: recebe a página que chegou e diz o que fazer em seguida.
 *
 * A ordem da lista não é documentada, então a busca lê das duas pontas para o meio (1, N, 2, N-1…). Para quando:
 * - acha o `reference`, depois de ler as páginas vizinhas (uma cobrança duplicada cai ao lado);
 * - as pontas se encontram;
 * - o trecho ainda não lido, entre o último `createdAt` lido de cada ponta, não chega perto da hora da Purchase.
 *   Vale para as duas ordens (mais antiga ou mais nova primeiro), e só se as duas pontas trouxerem `createdAt`;
 * - lê [maxPages] páginas sem achar nada: inconclusivo.
 */
internal class OrdersSearch(
    private val reference: String,
    purchasedAtEpochMillis: Long,
    slackMillis: Long,
    private val maxPages: Int,
) {

    sealed interface Next {
        data class Read(val page: Int) : Next

        /** A lista respondeu: [matches] diz se a Order existe e como ficou. */
        data object Finished : Next

        data object Inconclusive : Next
    }

    private val window = (purchasedAtEpochMillis - slackMillis)..(purchasedAtEpochMillis + slackMillis)
    private val read = mutableMapOf<Int, List<ListedOrder>>()
    private var totalPages = 0

    /** As Orders do `reference` já lidas, na ordem da lista. A mesma Order em duas páginas conta uma vez. */
    val matches: List<ListedOrder>
        get() = read.toSortedMap().values.flatten().filter { it.reference == reference }.distinctBy { it.cieloOrder.orderId }

    fun accept(page: OrdersPage): Next {
        if (read.isEmpty()) totalPages = page.totalPages
        read[page.number] = page.orders

        val pagesWithMatch = read.filterValues { orders -> orders.any { it.reference == reference } }.keys
        if (pagesWithMatch.isNotEmpty()) {
            val neighbor = pagesWithMatch.flatMap { listOf(it - 1, it + 1) }.firstOrNull { it in 1..totalPages && it !in read }
            return if (neighbor == null || read.size >= maxPages) Next.Finished else Next.Read(neighbor)
        }

        val unread = searchOrder().filterNot { it in read }
        return when {
            unread.isEmpty() || unreadMiddleMissesThePurchase() -> Next.Finished
            read.size >= maxPages -> Next.Inconclusive
            else -> Next.Read(unread.first())
        }
    }

    /** 1, N, 2, N-1… até as pontas se encontrarem. */
    private fun searchOrder(): List<Int> = buildList {
        var front = 1
        var back = totalPages
        while (front <= back) {
            add(front)
            if (back != front) add(back)
            front++
            back--
        }
    }

    private fun unreadMiddleMissesThePurchase(): Boolean {
        val front = generateSequence(1) { it + 1 }.takeWhile { it in read }.lastOrNull() ?: return false
        val back = generateSequence(totalPages) { it - 1 }.takeWhile { it in read }.lastOrNull() ?: return false
        val frontEdge = read.getValue(front).lastOrNull()?.createdAtEpochMillis ?: return false
        val backEdge = read.getValue(back).firstOrNull()?.createdAtEpochMillis ?: return false
        return maxOf(frontEdge, backEdge) < window.first || minOf(frontEdge, backEdge) > window.last
    }
}
