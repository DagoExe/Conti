package com.example.conti.utils

/**
 * Costanti utilizzate nell'applicazione.
 *
 * Centralizzare le costanti qui evita:
 * - Duplicazione di valori "magic numbers" nel codice
 * - Errori di battitura
 * - Difficoltà nel modificare valori globali
 */
object Constants {

    // ========================================
    // CATEGORIE MOVIMENTI
    // ========================================

    /**
     * Categorie predefinite per i movimenti.
     * L'utente può anche inserire categorie personalizzate.
     */
    val CATEGORIE_DEFAULT = listOf(
        "Stipendio",
        "Abbonamento",
        "Spesa",
        "Ristorante",
        "Trasporti",
        "Benzina",
        "Bollette",
        "Affitto",
        "Salute",
        "Intrattenimento",
        "Shopping",
        "Risparmio",
        "Investimenti",
        "Bonifico",
        "Prelievo",
        "Altro"
    )

    // ========================================
    // COLORI CONTI
    // ========================================

    /**
     * Colori predefiniti per i conti bancari (formato esadecimale).
     */
    val COLORI_CONTI = listOf(
        "#4CAF50", // Verde
        "#2196F3", // Blu
        "#FF9800", // Arancione
        "#9C27B0", // Viola
        "#F44336", // Rosso
        "#00BCD4", // Ciano
        "#FFC107", // Giallo ambra
        "#E91E63", // Rosa
        "#3F51B5", // Indigo
        "#8BC34A"  // Verde chiaro
    )

    // ========================================
    // EXCEL
    // ========================================

    /**
     * Nomi delle colonne attese nel file Excel.
     * Modifica questi valori in base alla struttura del tuo file Excel.
     */
    object Excel {
        const val COLONNA_DATA = "Data"
        const val COLONNA_DESCRIZIONE = "Descrizione"
        const val COLONNA_IMPORTO = "Importo"
        const val COLONNA_CATEGORIA = "Categoria"
        const val COLONNA_NOTE = "Note"

        /**
         * Estensioni file Excel supportate
         */
        val ESTENSIONI_SUPPORTATE = listOf(".xlsx", ".xls")
    }

    // ========================================
    // FORMATO DATE E VALUTE
    // ========================================

    object Formato {
        /**
         * Pattern per formattare le date in formato italiano (gg/mm/aaaa)
         */
        const val DATA_ITALIANA = "dd/MM/yyyy"

        /**
         * Pattern per formattare le date con nome del mese (es. "15 Gennaio 2025")
         */
        const val DATA_ESTESA = "dd MMMM yyyy"

        /**
         * Pattern per formattare mese e anno (es. "Gennaio 2025")
         */
        const val MESE_ANNO = "MMMM yyyy"

        /**
         * Simbolo della valuta
         */
        const val SIMBOLO_VALUTA = "€"

        /**
         * Locale italiana per formattazione numeri
         */
        const val LOCALE = "it_IT"
    }

    // ========================================
    // SCADENZE E NOTIFICHE
    // ========================================

    object Scadenze {
        /**
         * Numero di giorni prima della scadenza per mostrare un avviso
         */
        const val GIORNI_PREAVVISO = 7

        /**
         * Numero di giorni futuri per cui mostrare gli abbonamenti in scadenza
         */
        const val GIORNI_SCADENZA_IMMINENTE = 30
    }

    // ========================================
    // STATISTICHE
    // ========================================

    object Statistiche {
        /**
         * Numero massimo di categorie da mostrare nei grafici
         * (le altre vengono raggruppate in "Altro")
         */
        const val MAX_CATEGORIE_GRAFICO = 5

        /**
         * Numero di mesi da visualizzare nei grafici storici
         */
        const val MESI_STORICO_DEFAULT = 6
    }

    // ========================================
    // PREFERENZE (SharedPreferences)
    // ========================================

    object Preferenze {
        const val NOME_FILE = "conti_preferences"
        const val KEY_PRIMO_AVVIO = "primo_avvio"
        const val KEY_TEMA_SCURO = "tema_scuro"
        const val KEY_NOTIFICHE_ABILITATE = "notifiche_abilitate"
    }

    // ========================================
    // LIMITI E VALIDAZIONI
    // ========================================

    object Limiti {
        /**
         * Lunghezza massima per il nome di un conto
         */
        const val MAX_LUNGHEZZA_NOME_CONTO = 50

        /**
         * Lunghezza massima per la descrizione di un movimento
         */
        const val MAX_LUNGHEZZA_DESCRIZIONE = 200

        /**
         * Importo massimo accettato (per evitare errori di input)
         */
        const val MAX_IMPORTO = 1_000_000.0

        /**
         * Importo minimo accettato
         */
        const val MIN_IMPORTO = 0.01
    }
}