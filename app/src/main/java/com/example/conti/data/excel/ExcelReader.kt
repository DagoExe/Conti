package com.example.conti.data.excel

import com.example.conti.data.database.entities.Movimento
import com.example.conti.utils.Constants
import com.example.conti.utils.CurrencyUtils
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileInputStream
import java.time.LocalDate
import java.time.ZoneId

/**
 * Classe per leggere e parsare file Excel contenenti movimenti bancari.
 *
 * Utilizza Apache POI per leggere file .xlsx e .xls.
 *
 * Struttura attesa del file Excel:
 * - Prima riga: intestazioni (Data, Descrizione, Importo, Categoria, Note)
 * - Righe successive: dati dei movimenti
 *
 * Esempio:
 * | Data       | Descrizione          | Importo  | Categoria    | Note        |
 * |------------|---------------------|----------|--------------|-------------|
 * | 01/09/2025 | Stipendio           | 2500.00  | Stipendio    |             |
 * | 05/09/2025 | Netflix             | -12.99   | Abbonamento  | Piano Basic |
 * | 10/09/2025 | Spesa Supermercato  | -85.50   | Spesa        |             |
 */
class ExcelReader {

    /**
     * Risultato della lettura di un file Excel.
     *
     * @param movimenti Lista di movimenti parsati con successo
     * @param errori Lista di errori incontrati durante il parsing
     */
    data class ExcelResult(
        val movimenti: List<Movimento>,
        val errori: List<String>
    )

    /**
     * Legge un file Excel e restituisce una lista di movimenti.
     *
     * @param filePath Percorso completo del file Excel
     * @param contoId ID del conto a cui associare i movimenti
     * @return ExcelResult con movimenti ed eventuali errori
     */
    fun leggiExcel(filePath: String, contoId: Long): ExcelResult {
        val movimenti = mutableListOf<Movimento>()
        val errori = mutableListOf<String>()

        try {
            val file = File(filePath)

            // Verifica esistenza file
            if (!file.exists()) {
                errori.add("File non trovato: $filePath")
                return ExcelResult(emptyList(), errori)
            }

            // Verifica estensione
            val estensione = file.extension.lowercase()
            if (!Constants.Excel.ESTENSIONI_SUPPORTATE.contains(".$estensione")) {
                errori.add("Formato file non supportato: .$estensione")
                return ExcelResult(emptyList(), errori)
            }

            // Apri il file Excel
            FileInputStream(file).use { fis ->
                val workbook: Workbook = XSSFWorkbook(fis)
                val sheet = workbook.getSheetAt(0) // Leggi il primo foglio

                // Verifica che ci siano righe
                if (sheet.physicalNumberOfRows == 0) {
                    errori.add("Il file Excel è vuoto")
                    return ExcelResult(emptyList(), errori)
                }

                // Leggi la riga di intestazione (prima riga)
                val headerRow = sheet.getRow(0)
                if (headerRow == null) {
                    errori.add("Intestazione mancante")
                    return ExcelResult(emptyList(), errori)
                }

                // Identifica gli indici delle colonne
                val colonneMap = mappaColonne(headerRow)

                if (colonneMap.isEmpty()) {
                    errori.add("Intestazioni non valide. Assicurati che le colonne siano: Data, Descrizione, Importo")
                    return ExcelResult(emptyList(), errori)
                }

                // Leggi i dati (dalla seconda riga in poi)
                for (i in 1 until sheet.physicalNumberOfRows) {
                    val row = sheet.getRow(i) ?: continue

                    try {
                        val movimento = parsaRiga(row, colonneMap, contoId)
                        if (movimento != null) {
                            movimenti.add(movimento)
                        }
                    } catch (e: Exception) {
                        errori.add("Errore riga ${i + 1}: ${e.message}")
                    }
                }

                workbook.close()
            }

        } catch (e: Exception) {
            errori.add("Errore lettura file: ${e.message}")
        }

        return ExcelResult(movimenti, errori)
    }

    /**
     * Mappa le colonne del file Excel in base alle intestazioni.
     *
     * @return Map con nome colonna → indice
     */
    private fun mappaColonne(headerRow: Row): Map<String, Int> {
        val mappa = mutableMapOf<String, Int>()

        for (cellIndex in 0 until headerRow.lastCellNum) {
            val cell = headerRow.getCell(cellIndex) ?: continue
            val headerName = cell.stringCellValue.trim().lowercase()

            when {
                headerName.contains("data") -> mappa["data"] = cellIndex
                headerName.contains("descrizione") || headerName.contains("causale") ->
                    mappa["descrizione"] = cellIndex
                headerName.contains("importo") || headerName.contains("ammontare") ->
                    mappa["importo"] = cellIndex
                headerName.contains("categoria") -> mappa["categoria"] = cellIndex
                headerName.contains("note") -> mappa["note"] = cellIndex
            }
        }

        return mappa
    }

    /**
     * Parsa una singola riga del file Excel e crea un Movimento.
     */
    private fun parsaRiga(row: Row, colonneMap: Map<String, Int>, contoId: Long): Movimento? {
        // Colonne obbligatorie
        val dataIndex = colonneMap["data"] ?: return null
        val descrizioneIndex = colonneMap["descrizione"] ?: return null
        val importoIndex = colonneMap["importo"] ?: return null

        // Colonne opzionali
        val categoriaIndex = colonneMap["categoria"]
        val noteIndex = colonneMap["note"]

        // Leggi DATA
        val dataCell = row.getCell(dataIndex) ?: return null
        val data = when (dataCell.cellType) {
            CellType.NUMERIC -> {
                if (DateUtil.isCellDateFormatted(dataCell)) {
                    // Data in formato Excel
                    dataCell.dateCellValue.toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                } else {
                    return null
                }
            }
            CellType.STRING -> {
                // Data in formato stringa (es. "01/09/2025")
                parseDataFromString(dataCell.stringCellValue)
            }
            else -> return null
        } ?: return null

        // Leggi DESCRIZIONE
        val descrizioneCell = row.getCell(descrizioneIndex) ?: return null
        val descrizione = when (descrizioneCell.cellType) {
            CellType.STRING -> descrizioneCell.stringCellValue.trim()
            CellType.NUMERIC -> descrizioneCell.numericCellValue.toString()
            else -> return null
        }

        if (descrizione.isBlank()) return null

        // Leggi IMPORTO
        val importoCell = row.getCell(importoIndex) ?: return null
        val importo = when (importoCell.cellType) {
            CellType.NUMERIC -> importoCell.numericCellValue
            CellType.STRING -> CurrencyUtils.parseImporto(importoCell.stringCellValue)
            else -> null
        } ?: return null

        // Leggi CATEGORIA (opzionale)
        val categoria = categoriaIndex?.let { idx ->
            val cell = row.getCell(idx)
            when (cell?.cellType) {
                CellType.STRING -> cell.stringCellValue.trim()
                else -> null
            }
        } ?: determinaCategoriaDaDescrizione(descrizione, importo)

        // Leggi NOTE (opzionale)
        val note = noteIndex?.let { idx ->
            val cell = row.getCell(idx)
            when (cell?.cellType) {
                CellType.STRING -> cell.stringCellValue.trim().takeIf { it.isNotBlank() }
                else -> null
            }
        }

        return Movimento(
            contoId = contoId,
            data = data,
            descrizione = descrizione,
            importo = importo,
            categoria = categoria,
            note = note,
            isRicorrente = false,
            dataInserimento = LocalDate.now()
        )
    }

    /**
     * Prova a parsare una data da una stringa in vari formati.
     */
    private fun parseDataFromString(dataString: String): LocalDate? {
        val formati = listOf(
            "dd/MM/yyyy",
            "dd-MM-yyyy",
            "yyyy-MM-dd",
            "dd/MM/yy",
            "dd-MM-yy"
        )

        for (formato in formati) {
            try {
                val formatter = java.time.format.DateTimeFormatter.ofPattern(formato)
                return LocalDate.parse(dataString, formatter)
            } catch (e: Exception) {
                // Prova il prossimo formato
                continue
            }
        }

        return null
    }

    /**
     * Determina automaticamente la categoria in base alla descrizione e all'importo.
     *
     * Questa è una logica di base. Puoi espanderla con regole più sofisticate.
     */
    private fun determinaCategoriaDaDescrizione(descrizione: String, importo: Double): String {
        val desc = descrizione.lowercase()

        return when {
            // Entrate
            importo > 0 && (desc.contains("stipendio") || desc.contains("salary")) -> "Stipendio"
            importo > 0 && desc.contains("bonifico") -> "Bonifico"
            importo > 0 -> "Entrata"

            // Uscite
            desc.contains("netflix") || desc.contains("spotify") ||
                    desc.contains("abbonamento") -> "Abbonamento"

            desc.contains("supermercato") || desc.contains("conad") ||
                    desc.contains("esselunga") -> "Spesa"

            desc.contains("ristorante") || desc.contains("pizzeria") ||
                    desc.contains("bar") -> "Ristorante"

            desc.contains("benzina") || desc.contains("carburante") -> "Benzina"

            desc.contains("bolletta") || desc.contains("enel") ||
                    desc.contains("gas") -> "Bollette"

            desc.contains("affitto") || desc.contains("rent") -> "Affitto"

            desc.contains("prelievo") || desc.contains("bancomat") -> "Prelievo"

            else -> "Altro"
        }
    }
}