package com.example.conti.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.conti.data.repository.FirestoreRepository
import com.example.conti.ui.home.HomeViewModel

/**
 * Factory per creare ViewModel con dipendenze personalizzate.
 *
 * Android richiede una factory per creare ViewModel che hanno parametri nel costruttore
 * (in questo caso, il FirestoreRepository).
 *
 * @param repository Il repository Firestore da iniettare nei ViewModel
 */
class ViewModelFactory(
    private val repository: FirestoreRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(HomeViewModel::class.java) -> {
                HomeViewModel(repository) as T
            }
            // Aggiungi qui altri ViewModel man mano che li crei
            // modelClass.isAssignableFrom(MovimentiViewModel::class.java) -> {
            //     MovimentiViewModel(repository) as T
            // }
            // modelClass.isAssignableFrom(AbbonamentiViewModel::class.java) -> {
            //     AbbonamentiViewModel(repository) as T
            // }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}