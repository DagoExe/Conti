package com.example.conti.ui.movimenti

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.conti.databinding.FragmentMovimentiBinding

class MovimentiFragment : Fragment() {

    private var _binding: FragmentMovimentiBinding? = null
    private val binding get() = _binding!!

    // ✅ Quando creerai MovimentiViewModel, sarà così:
    // private val viewModel: MovimentiViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMovimentiBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Placeholder per ora
        binding.tvPlaceholder.text = "Movimenti\n\n(In sviluppo)"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}