package com.example.conti.data.excel

import com.example.conti.data.database.entities.Movimento
import com.example.conti.utils.CurrencyUtils
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileInputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Classe per leggere e parsare file Excel contenenti movimenti bancari.
 *
 * ADATTATO PER IL FORMATO:
 * - Data operazione | Data contabile | Iban | Tipologia | Nome | Descrizione | Importo ( € )
 *
 * Utilizza Apache POI per leggere file .xlsx e .xls.
 */
class ExcelReader {

    /**
     * Risultato della lettura di un file Excel.
     */
    data class ExcelResult(
        val movimenti: List<Movimento>,
        val errori: List<String>
    )

    /**
     * Indici delle colonne nel file Excel
     */
    private object ColonneExcel {
        const val DATA_OPERAZIONE = 0      // A - Data operazione
        const val DATA_CONTABILE = 1       // B - Data contabile
        const val IBAN = 2                 // C - Iban
        const val TIPOLOGIA = 3            // D - Tipologia
        const val NOME = 4                 // E - Nome
        const val DESCRIZIONE = 5          // F - Descrizione
        const val IMPORTO = 6              // G - Importo ( € )
    }

    /**
     * Formatter per le date in formato dd/MM/yyyy
     */
    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    /**
     * Legge un file Excel e restituisce una lista di movimenti.
     *
     * @param filePath Percorso completo del file Excel
     * @param contoId ID del conto a cui associare i movimenti
     * @return ExcelResult con movimenti ed eventuali errori
     */
    fun leggiExcel(filePath: String, contoId: Long): ExcelResult {
        android.util.Log.d("ExcelReader", "=== INIZIO LETTURA EXCEL ===")
        android.util.Log.d("ExcelReader", "File path: $filePath")
        android.util.Log.d("ExcelReader", "Conto ID: $contoId")

        val movimenti = mutableListOf<Movimento>()
        val errori = mutableListOf<String>()

        try {
            val file = File(filePath)
            android.util.Log.d("ExcelReader", "File exists: ${file.exists()}")
            android.util.Log.d("ExcelReader", "File size: ${file.length()} bytes")

            // Verifica esistenza file
            if (!file.exists()) {
                val errore = "File non trovato: $filePath"
                android.util.Log.e("ExcelReader", errore)
                errori.add(errore)
                return ExcelResult(emptyList(), errori)
            }

            // Verifica estensione
            val estensione = file.extension.lowercase()
            android.util.Log.d("ExcelReader", "Estensione file: $estensione")

            if (estensione != "xlsx" && estensione != "xls") {
                val errore = "Formato file non supportato: .$estensione"
                android.util.Log.e("ExcelReader", errore)
                errori.add(errore)
                return ExcelResult(emptyList(), errori)
            }

            android.util.Log.d("ExcelReader", "Apertura file Excel...")

            // Apri il file Excel
            FileInputStream(file).use { fis ->
                val workbook: Workbook = XSSFWorkbook(fis)
                val sheet = workbook.getSheetAt(0)

                android.util.Log.d("ExcelReader", "Foglio caricato: ${sheet.sheetName}")
                android.util.Log.d("ExcelReader", "Numero righe: ${sheet.physicalNumberOfRows}")

                // Verifica che ci siano righe
                if (sheet.physicalNumberOfRows <= 1) {
                    val errore = "Il file Excel è vuoto o contiene solo l'intestazione"
                    android.util.Log.e("ExcelReader", errore)
                    errori.add(errore)
                    return ExcelResult(emptyList(), errori)
                }

                // Salta la riga di intestazione (riga 0) e leggi i dati
                for (rowIndex in 1 until sheet.physicalNumberOfRows) {
                    val row = sheet.getRow(rowIndex) ?: continue

                    try {
                        val movimento = parsaRiga(row, contoId, rowIndex + 1)
                        if (movimento != null) {
                            movimenti.add(movimento)
                            if (rowIndex <= 3) { // Log solo per le prime 3 righe
                                android.util.Log.d("ExcelReader", "Riga $rowIndex parsata: ${movimento.descrizione} - ${movimento.importo}€")
                            }
                        } else {
                            android.util.Log.w("ExcelReader", "Riga $rowIndex: movimento null")
                        }
                    } catch (e: Exception) {
                        val errore = "Errore riga ${rowIndex + 1}: ${e.message}"
                        android.util.Log.e("ExcelReader", errore, e)
                        errori.add(errore)
                    }
                }

                workbook.close()
                android.util.Log.d("ExcelReader", "File chiuso correttamente")
            }

        } catch (e: Exception) {
            val errore = "Errore lettura file: ${e.message}"
            android.util.Log.e("ExcelReader", errore, e)
            errori.add(errore)
        }

        android.util.Log.d("ExcelReader", "=== FINE LETTURA EXCEL ===")
        android.util.Log.d("ExcelReader", "Movimenti parsati: ${movimenti.size}")
        android.util.Log.d("ExcelReader", "Errori: ${errori.size}")

        return ExcelResult(movimenti, errori)
    }

    /**
     * Parsa una singola riga del file Excel e crea un Movimento.
     *
     * @param row La riga da parsare
     * @param contoId ID del conto
     * @param numeroRiga Numero della riga (per messaggi di errore)
     * @return Movimento parsato o null se la riga non è valida
     */
    private fun parsaRiga(row: Row, contoId: Long, numeroRiga: Int): Movimento? {
        // === LEGGI DATA OPERAZIONE (colonna A) ===
        val dataOperazioneCell = row.getCell(ColonneExcel.DATA_OPERAZIONE) ?: return null
        val data = parseData(dataOperazioneCell) ?: return null

        // === LEGGI TIPOLOGIA (colonna D) ===
        val tipologiaCell = row.getCell(ColonneExcel.TIPOLOGIA)
        val tipologia = getCellStringValue(tipologiaCell) ?: "Altro"

        // === LEGGI NOME (colonna E) ===
        val nomeCell = row.getCell(ColonneExcel.NOME)
        val nome = getCellStringValue(nomeCell) ?: ""

        // === LEGGI DESCRIZIONE (colonna F) ===
        val descrizioneCell = row.getCell(ColonneExcel.DESCRIZIONE)
        val descrizioneCompleta = getCellStringValue(descrizioneCell) ?: ""

        // Combina Nome e Descrizione per creare la descrizione del movimento
        val descrizione = if (nome.isNotBlank() && descrizioneCompleta.isNotBlank()) {
            "$nome - $descrizioneCompleta"
        } else if (nome.isNotBlank()) {
            nome
        } else if (descrizioneCompleta.isNotBlank()) {
            descrizioneCompleta
        } else {
            "Movimento"
        }

        // === LEGGI IMPORTO (colonna G) ===
        val importoCell = row.getCell(ColonneExcel.IMPORTO) ?: return null
        val importo = parseImporto(importoCell) ?: return null

        // === DETERMINA CATEGORIA AUTOMATICAMENTE ===
        val categoria = determinaCategoria(tipologia, nome, descrizioneCompleta, importo)

        // === CREA IL MOVIMENTO ===
        return Movimento(
            contoId = contoId,
            data = data,
            descrizione = descrizione.trim(),
            importo = importo,
            categoria = categoria,
            note = "Importato da Excel - Tipologia: $tipologia",
            isRicorrente = false,
            dataInserimento = LocalDate.now()
        )
    }

    /**
     * Legge una data da una cella.
     * Supporta sia date formattate che stringhe.
     */
    private fun parseData(cell: Cell?): LocalDate? {
        return try {
            when (cell?.cellType) {
                CellType.NUMERIC -> {
                    if (DateUtil.isCellDateFormatted(cell)) {
                        // Data in formato Excel
                        val javaDate = cell.dateCellValue
                        javaDate.toInstant()
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate()
                    } else {
                        null
                    }
                }
                CellType.STRING -> {
                    // Data in formato stringa (es. "30/09/2025")
                    val dateString = cell.stringCellValue.trim()
                    LocalDate.parse(dateString, dateFormatter)
                }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Legge un importo da una cella.
     * Supporta sia valori numerici che stringhe.
     */
    private fun parseImporto(cell: Cell?): Double? {
        return try {
            when (cell?.cellType) {
                CellType.NUMERIC -> cell.numericCellValue
                CellType.STRING -> {
                    val importoString = cell.stringCellValue.trim()
                    CurrencyUtils.parseImporto(importoString)
                }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Ottiene il valore stringa di una cella.
     */
    private fun getCellStringValue(cell: Cell?): String? {
        return try {
            when (cell?.cellType) {
                CellType.STRING -> cell.stringCellValue.trim()
                CellType.NUMERIC -> cell.numericCellValue.toString()
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Determina automaticamente la categoria in base alla tipologia, nome e importo.
     *
     * Utilizza euristiche per categorizzare i movimenti.
     */
    private fun determinaCategoria(
        tipologia: String,
        nome: String,
        descrizione: String,
        importo: Double
    ): String {
        val tipologiaLower = tipologia.lowercase()
        val nomeLower = nome.lowercase()
        val descrizoneLower = descrizione.lowercase()

        // ENTRATE
        if (importo > 0) {
            return when {
                tipologiaLower.contains("bonifico") -> "Bonifico"
                tipologiaLower.contains("stipendio") ||
                        nomeLower.contains("stipendio") -> "Stipendio"
                tipologiaLower.contains("accredito") -> "Entrata"
                else -> "Entrata"
            }
        }

        // USCITE
        return when {
            // Abbonamenti e servizi
            nomeLower.contains("netflix") || nomeLower.contains("spotify") ||
                    nomeLower.contains("amazon prime") || nomeLower.contains("disney") ||
                    nomeLower.contains("xbox") || nomeLower.contains("playstation") -> "Abbonamento"

            // Spesa alimentare
            nomeLower.contains("conad") || nomeLower.contains("esselunga") ||
                    nomeLower.contains("lidl") || nomeLower.contains("eurospin") ||
                    nomeLower.contains("carrefour") || nomeLower.contains("coop") ||
                    nomeLower.contains("iper") || nomeLower.contains("md discount") -> "Spesa"

            // Ristoranti e bar
            nomeLower.contains("ristorante") || nomeLower.contains("pizzeria") ||
                    nomeLower.contains("bar") || nomeLower.contains("trattoria") ||
                    nomeLower.contains("osteria") || nomeLower.contains("pub") ||
                    nomeLower.contains("mc donald") || nomeLower.contains("burger king") -> "Ristorante"

            // Trasporti e carburante
            nomeLower.contains("eni") || nomeLower.contains("ip") ||
                    nomeLower.contains("q8") || nomeLower.contains("tamoil") ||
                    nomeLower.contains("benzina") || nomeLower.contains("diesel") -> "Benzina"

            nomeLower.contains("trenitalia") || nomeLower.contains("italo") ||
                    nomeLower.contains("gtt") || nomeLower.contains("atm") ||
                    nomeLower.contains("taxi") || nomeLower.contains("uber") -> "Trasporti"

            // Bollette e utenze
            nomeLower.contains("enel") || nomeLower.contains("eni gas") ||
                    nomeLower.contains("tim") || nomeLower.contains("vodafone") ||
                    nomeLower.contains("wind") || nomeLower.contains("iliad") ||
                    nomeLower.contains("bolletta") -> "Bollette"

            // Shopping
            nomeLower.contains("zara") || nomeLower.contains("h&m") ||
                    nomeLower.contains("decathlon") || nomeLower.contains("ikea") ||
                    nomeLower.contains("mediaworld") || nomeLower.contains("euronics") -> "Shopping"

            // PayPal e pagamenti online
            nomeLower.contains("paypal") -> "Pagamento Online"

            // Prelievi
            tipologiaLower.contains("prelievo") ||
                    nomeLower.contains("bancomat") -> "Prelievo"

            // Default: usa la tipologia come categoria
            tipologiaLower.contains("bonifico") -> "Bonifico"
            tipologiaLower.contains("pagamento") -> "Pagamento"

            else -> "Altro"
        }
    }

    /**
     * Metodo helper per verificare se un file Excel è valido prima di leggerlo.
     *
     * @param filePath Percorso del file
     * @return Pair<Boolean, String> - (isValid, messaggioErrore)
     */
    fun verificaFileExcel(filePath: String): Pair<Boolean, String> {
        try {
            val file = File(filePath)

            if (!file.exists()) {
                return Pair(false, "File non trovato")
            }

            if (!file.canRead()) {
                return Pair(false, "Impossibile leggere il file (permessi negati)")
            }

            val estensione = file.extension.lowercase()
            if (estensione != "xlsx" && estensione != "xls") {
                return Pair(false, "Formato file non valido (deve essere .xlsx o .xls)")
            }

            // Prova ad aprire il file
            FileInputStream(file).use { fis ->
                val workbook = XSSFWorkbook(fis)
                if (workbook.numberOfSheets == 0) {
                    return Pair(false, "Il file non contiene fogli")
                }
                workbook.close()
            }

            return Pair(true, "File valido")

        } catch (e: Exception) {
            return Pair(false, "Errore durante la verifica: ${e.message}")
        }
    }
}