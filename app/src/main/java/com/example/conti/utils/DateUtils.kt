package com.example.conti.utils

import com.example.conti.utils.Constants.Formato
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

/**
 * Utility class per gestire operazioni sulle date.
 *
 * Fornisce metodi helper per:
 * - Formattare date in vari formati
 * - Calcolare intervalli (mese corrente, anno corrente, ecc.)
 * - Convertire tra formati
 * - Operazioni comuni sulle date
 */
object DateUtils {

    private val localeItaliana = Locale.ITALIAN

    // ========================================
    // FORMATTATORI
    // ========================================

    private val formatoItaliano = DateTimeFormatter.ofPattern(Formato.DATA_ITALIANA, localeItaliana)
    private val formatoEsteso = DateTimeFormatter.ofPattern(Formato.DATA_ESTESA, localeItaliana)
    private val formatoMeseAnno = DateTimeFormatter.ofPattern(Formato.MESE_ANNO, localeItaliana)

    /**
     * Formatta una data in formato italiano (gg/mm/aaaa).
     * Esempio: 21/10/2025
     */
    fun formatDataItaliana(data: LocalDate): String {
        return data.format(formatoItaliano)
    }

    /**
     * Formatta una data in formato esteso con nome del mese.
     * Esempio: 21 Ottobre 2025
     */
    fun formatDataEstesa(data: LocalDate): String {
        return data.format(formatoEsteso)
    }

    /**
     * Formatta mese e anno.
     * Esempio: Ottobre 2025
     */
    fun formatMeseAnno(data: LocalDate): String {
        return data.format(formatoMeseAnno)
    }

    /**
     * Formatta mese e anno da YearMonth.
     * Esempio: Ottobre 2025
     */
    fun formatMeseAnno(yearMonth: YearMonth): String {
        return yearMonth.format(formatoMeseAnno)
    }

    /**
     * Ottiene il nome del mese in italiano.
     * Esempio: "Gennaio", "Febbraio", ecc.
     */
    fun getNomeMese(data: LocalDate): String {
        return data.month.getDisplayName(TextStyle.FULL, localeItaliana)
            .replaceFirstChar { it.uppercase() }
    }

    /**
     * Ottiene il nome del giorno della settimana in italiano.
     * Esempio: "Lunedì", "Martedì", ecc.
     */
    fun getNomeGiorno(data: LocalDate): String {
        return data.dayOfWeek.getDisplayName(TextStyle.FULL, localeItaliana)
            .replaceFirstChar { it.uppercase() }
    }

    // ========================================
    // PARSING
    // ========================================

    /**
     * Converte una stringa in formato gg/mm/aaaa in LocalDate.
     * Ritorna null se il formato non è valido.
     */
    fun parseDataItaliana(dataString: String): LocalDate? {
        return try {
            LocalDate.parse(dataString, formatoItaliano)
        } catch (e: Exception) {
            null
        }
    }

    // ========================================
    // INTERVALLI DATE
    // ========================================

    /**
     * Ottiene il primo giorno del mese corrente.
     */
    fun getPrimoGiornoMeseCorrente(): LocalDate {
        return LocalDate.now().with(TemporalAdjusters.firstDayOfMonth())
    }

    /**
     * Ottiene l'ultimo giorno del mese corrente.
     */
    fun getUltimoGiornoMeseCorrente(): LocalDate {
        return LocalDate.now().with(TemporalAdjusters.lastDayOfMonth())
    }

    /**
     * Ottiene il primo giorno dell'anno corrente.
     */
    fun getPrimoGiornoAnnoCorrente(): LocalDate {
        return LocalDate.now().with(TemporalAdjusters.firstDayOfYear())
    }

    /**
     * Ottiene l'ultimo giorno dell'anno corrente.
     */
    fun getUltimoGiornoAnnoCorrente(): LocalDate {
        return LocalDate.now().with(TemporalAdjusters.lastDayOfYear())
    }

    /**
     * Ottiene il primo e ultimo giorno di un mese specifico.
     *
     * @param yearMonth Il mese di cui ottenere l'intervallo
     * @return Pair con (primo giorno, ultimo giorno)
     */
    fun getIntervalloMese(yearMonth: YearMonth): Pair<LocalDate, LocalDate> {
        val primoGiorno = yearMonth.atDay(1)
        val ultimoGiorno = yearMonth.atEndOfMonth()
        return Pair(primoGiorno, ultimoGiorno)
    }

    /**
     * Ottiene il primo e ultimo giorno di un anno specifico.
     *
     * @param anno L'anno di cui ottenere l'intervallo
     * @return Pair con (primo giorno, ultimo giorno)
     */
    fun getIntervalloAnno(anno: Int): Pair<LocalDate, LocalDate> {
        val primoGiorno = LocalDate.of(anno, 1, 1)
        val ultimoGiorno = LocalDate.of(anno, 12, 31)
        return Pair(primoGiorno, ultimoGiorno)
    }

    /**
     * Ottiene l'intervallo degli ultimi N giorni (incluso oggi).
     *
     * @param giorni Numero di giorni
     * @return Pair con (data inizio, data fine = oggi)
     */
    fun getUltimiGiorni(giorni: Int): Pair<LocalDate, LocalDate> {
        val fine = LocalDate.now()
        val inizio = fine.minusDays(giorni.toLong() - 1)
        return Pair(inizio, fine)
    }

    /**
     * Ottiene l'intervallo degli ultimi N mesi completi.
     *
     * @param mesi Numero di mesi
     * @return Pair con (primo giorno del primo mese, ultimo giorno dell'ultimo mese)
     */
    fun getUltimiMesi(mesi: Int): Pair<LocalDate, LocalDate> {
        val oggi = LocalDate.now()
        val fine = oggi.with(TemporalAdjusters.lastDayOfMonth())
        val inizio = oggi.minusMonths(mesi.toLong() - 1)
            .with(TemporalAdjusters.firstDayOfMonth())
        return Pair(inizio, fine)
    }

    // ========================================
    // COMPARAZIONI
    // ========================================

    /**
     * Verifica se una data è oggi.
     */
    fun isOggi(data: LocalDate): Boolean {
        return data == LocalDate.now()
    }

    /**
     * Verifica se una data è nel mese corrente.
     */
    fun isNelMeseCorrente(data: LocalDate): Boolean {
        val oggi = LocalDate.now()
        return data.year == oggi.year && data.month == oggi.month
    }

    /**
     * Verifica se una data è nell'anno corrente.
     */
    fun isNellAnnoCorrente(data: LocalDate): Boolean {
        return data.year == LocalDate.now().year
    }

    /**
     * Verifica se una data è nel passato.
     */
    fun isPassato(data: LocalDate): Boolean {
        return data.isBefore(LocalDate.now())
    }

    /**
     * Verifica se una data è nel futuro.
     */
    fun isFuturo(data: LocalDate): Boolean {
        return data.isAfter(LocalDate.now())
    }

    /**
     * Calcola i giorni tra due date.
     *
     * @return Numero di giorni (positivo se dataFine è dopo dataInizio)
     */
    fun giorniTra(dataInizio: LocalDate, dataFine: LocalDate): Long {
        return java.time.temporal.ChronoUnit.DAYS.between(dataInizio, dataFine)
    }

    /**
     * Calcola i giorni mancanti da oggi a una data futura.
     *
     * @return Numero di giorni mancanti (0 se la data è oggi o nel passato)
     */
    fun giorniMancanti(dataFutura: LocalDate): Long {
        val giorni = giorniTra(LocalDate.now(), dataFutura)
        return if (giorni < 0) 0 else giorni
    }

    // ========================================
    // UTILITY PER ABBONAMENTI
    // ========================================

    /**
     * Calcola la prossima data di rinnovo per un abbonamento.
     *
     * @param dataUltimoRinnovo Data dell'ultimo rinnovo
     * @param mesiFrequenza Frequenza in mesi (1 = mensile, 3 = trimestrale, ecc.)
     * @return Prossima data di rinnovo
     */
    fun calcolaProssimoRinnovo(dataUltimoRinnovo: LocalDate, mesiFrequenza: Int): LocalDate {
        return dataUltimoRinnovo.plusMonths(mesiFrequenza.toLong())
    }

    /**
     * Formatta una data relativa (es. "Oggi", "Domani", "Ieri", o la data formattata).
     */
    fun formatDataRelativa(data: LocalDate): String {
        val oggi = LocalDate.now()
        return when {
            data == oggi -> "Oggi"
            data == oggi.plusDays(1) -> "Domani"
            data == oggi.minusDays(1) -> "Ieri"
            else -> formatDataItaliana(data)
        }
    }
}