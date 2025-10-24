package com.example.conti.ui.conti

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.conti.databinding.FragmentContiBinding
import com.example.conti.ui.account.AccountsViewModel

class ContiFragment : Fragment() {

    private var _binding: FragmentContiBinding? = null
    private val binding get() = _binding!!

    // ✅ Nessuna Factory
    private val viewModel: AccountsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentContiBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Setup UI...
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}