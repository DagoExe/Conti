package com.example.conti.ui.abbonamenti

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.conti.databinding.FragmentAbbonamentiBinding

class AbbonamentiFragment : Fragment() {

    private var _binding: FragmentAbbonamentiBinding? = null
    private val binding get() = _binding!!

    // ✅ Quando creerai AbbonamentiViewModel, sarà così:
    // private val viewModel: AbbonamentiViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAbbonamentiBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Placeholder per ora
        binding.tvPlaceholder.text = "Abbonamenti\n\n(In sviluppo)"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}