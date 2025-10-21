package com.example.conti.utils

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Utility class per gestire la formattazione delle valute.
 *
 * Fornisce metodi per:
 * - Formattare importi in formato valuta italiana (€)
 * - Convertire stringhe in importi
 * - Gestire colori basati su entrate/uscite
 */
object CurrencyUtils {

    private val localeItaliana = Locale.ITALIAN

    // Simboli personalizzati per il formato italiano
    private val symbols = DecimalFormatSymbols(localeItaliana).apply {
        decimalSeparator = ','
        groupingSeparator = '.'
    }

    // Formatter per valute con 2 decimali
    private val currencyFormatter = DecimalFormat("#,##0.00", symbols)

    // Formatter senza decimali (per importi interi)
    private val currencyFormatterNoDecimals = DecimalFormat("#,##0", symbols)

    /**
     * Formatta un importo in formato valuta italiana con simbolo €.
     *
     * Esempi:
     * - 1234.56 → "€ 1.234,56"
     * - -50.00 → "€ -50,00"
     * - 1000000.00 → "€ 1.000.000,00"
     *
     * @param importo L'importo da formattare
     * @param mostraSimboloPositivo Se true, mostra "+" per importi positivi
     * @return Stringa formattata con simbolo €
     */
    fun formatImporto(importo: Double, mostraSimboloPositivo: Boolean = false): String {
        val formatted = currencyFormatter.format(importo)
        val prefix = when {
            importo > 0 && mostraSimboloPositivo -> "+ €"
            importo < 0 -> "€"
            else -> "€"
        }
        return "$prefix $formatted"
    }

    /**
     * Formatta un importo senza decimali (utile per grandi cifre).
     *
     * Esempi:
     * - 1234.56 → "€ 1.235"
     * - 1000000.00 → "€ 1.000.000"
     */
    fun formatImportoSenzaDecimali(importo: Double): String {
        val rounded = kotlin.math.round(importo)
        return "€ ${currencyFormatterNoDecimals.format(rounded)}"
    }

    /**
     * Formatta un importo colorato in base al segno (verde per positivo, rosso per negativo).
     * Restituisce una coppia (stringa formattata, codice colore esadecimale).
     *
     * @return Pair<String, String> - (importo formattato, colore hex)
     */
    fun formatImportoColorato(importo: Double): Pair<String, String> {
        val formatted = formatImporto(importo, mostraSimboloPositivo = true)
        val colore = when {
            importo > 0 -> "#4CAF50" // Verde per entrate
            importo < 0 -> "#F44336" // Rosso per uscite
            else -> "#757575" // Grigio per zero
        }
        return Pair(formatted, colore)
    }

    /**
     * Formatta un importo come entrata (sempre positivo, prefisso "+").
     *
     * Esempio: 1234.56 → "+ € 1.234,56"
     */
    fun formatEntrata(importo: Double): String {
        val importoPositivo = kotlin.math.abs(importo)
        return "+ € ${currencyFormatter.format(importoPositivo)}"
    }

    /**
     * Formatta un importo come uscita (sempre negativo, prefisso "-").
     *
     * Esempio: 1234.56 → "- € 1.234,56"
     */
    fun formatUscita(importo: Double): String {
        val importoPositivo = kotlin.math.abs(importo)
        return "- € ${currencyFormatter.format(importoPositivo)}"
    }

    /**
     * Formatta solo il valore numerico senza simbolo € (utile per input).
     *
     * Esempio: 1234.56 → "1.234,56"
     */
    fun formatNumero(importo: Double): String {
        return currencyFormatter.format(importo)
    }

    // ========================================
    // PARSING
    // ========================================

    /**
     * Converte una stringa in Double.
     * Gestisce vari formati di input:
     * - "1234,56" → 1234.56
     * - "1.234,56" → 1234.56
     * - "€ 1.234,56" → 1234.56
     * - "1234.56" → 1234.56 (formato americano)
     *
     * @return Double o null se la stringa non è valida
     */
    fun parseImporto(importoString: String): Double? {
        return try {
            // Rimuovi spazi, simboli €, + e -
            var cleaned = importoString.trim()
                .replace("€", "")
                .replace("+", "")
                .replace(" ", "")

            // Gestisci il segno negativo
            val isNegativo = cleaned.startsWith("-")
            cleaned = cleaned.replace("-", "")

            // Converti formato italiano in formato standard
            // Se contiene sia "." che ",", è formato italiano (1.234,56)
            val value = when {
                cleaned.contains(",") && cleaned.contains(".") -> {
                    // Formato: 1.234,56 → rimuovi punti e sostituisci virgola con punto
                    cleaned.replace(".", "").replace(",", ".").toDouble()
                }
                cleaned.contains(",") -> {
                    // Formato: 1234,56 → sostituisci virgola con punto
                    cleaned.replace(",", ".").toDouble()
                }
                else -> {
                    // Formato: 1234.56 o 1234 → già nel formato corretto
                    cleaned.toDouble()
                }
            }

            if (isNegativo) -value else value
        } catch (e: Exception) {
            null
        }
    }

    // ========================================
    // VALIDAZIONE
    // ========================================

    /**
     * Valida un importo secondo i limiti definiti in Constants.
     *
     * @return true se l'importo è valido, false altrimenti
     */
    fun isImportoValido(importo: Double): Boolean {
        val importoAssoluto = kotlin.math.abs(importo)
        return importoAssoluto >= Constants.Limiti.MIN_IMPORTO &&
                importoAssoluto <= Constants.Limiti.MAX_IMPORTO
    }

    /**
     * Arrotonda un importo a 2 decimali.
     */
    fun arrotonda(importo: Double): Double {
        return kotlin.math.round(importo * 100) / 100.0
    }

    // ========================================
    // UTILITY PER STATISTICHE
    // ========================================

    /**
     * Calcola la percentuale di un importo rispetto a un totale.
     *
     * @param importo Importo parziale
     * @param totale Importo totale
     * @return Percentuale (0-100), o 0 se il totale è 0
     */
    fun calcolaPercentuale(importo: Double, totale: Double): Double {
        return if (totale == 0.0) 0.0 else (importo / totale) * 100
    }

    /**
     * Formatta una percentuale.
     *
     * Esempio: 45.6789 → "45,68%"
     */
    fun formatPercentuale(percentuale: Double): String {
        return "${currencyFormatter.format(percentuale)}%"
    }

    /**
     * Calcola la media di una lista di importi.
     */
    fun calcolaMedia(importi: List<Double>): Double {
        return if (importi.isEmpty()) 0.0 else importi.sum() / importi.size
    }

    /**
     * Determina il colore da usare per un importo nell'UI.
     *
     * @return Codice colore esadecimale
     */
    fun getColoreImporto(importo: Double): String {
        return when {
            importo > 0 -> "#4CAF50" // Verde per entrate
            importo < 0 -> "#F44336" // Rosso per uscite
            else -> "#757575" // Grigio per zero
        }
    }

    /**
     * Verifica se un importo è un'entrata.
     */
    fun isEntrata(importo: Double): Boolean = importo > 0

    /**
     * Verifica se un importo è un'uscita.
     */
    fun isUscita(importo: Double): Boolean = importo < 0
}