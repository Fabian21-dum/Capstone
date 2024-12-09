package com.example.capstone.ui.alphabets.detail

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.capstone.R
import com.example.capstone.databinding.FragmentAlphabetDetailBinding
import com.example.capstone.databinding.FragmentAlphabetsBinding

class AlphabetDetailFragment : Fragment() {

    private var _binding: FragmentAlphabetDetailBinding? = null
    private val binding get() = _binding!!

    private val args by navArgs<AlphabetDetailFragmentArgs>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAlphabetDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {
            tvTitle.text = args.alphabet.alphabet
            tvDescription.text = args.alphabet.description

            btnBack.setOnClickListener { findNavController().popBackStack() }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}