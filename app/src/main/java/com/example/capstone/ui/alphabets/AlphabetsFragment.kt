package com.example.capstone.ui.alphabets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.capstone.databinding.FragmentAlphabetsBinding

class AlphabetsFragment : Fragment() {

    private var _binding: FragmentAlphabetsBinding? = null
    private val binding get() = _binding!!

    private val alphabetListAdapter by lazy { AlphabetListAdapter() }
    private lateinit var alphabetsViewModel: AlphabetsViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        alphabetsViewModel = ViewModelProvider(this)[AlphabetsViewModel::class.java]
        _binding = FragmentAlphabetsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {
            rvAlphabets.apply {
                layoutManager = GridLayoutManager(requireContext(), 4)
                isNestedScrollingEnabled = false
                setHasFixedSize(false)
                adapter = alphabetListAdapter

                alphabetListAdapter.onItemClickListener = { alphabet ->
                    val action = AlphabetsFragmentDirections.actionNavigationAlphabetsToAlphabetDetailFragment(alphabet)
                    findNavController().navigate(action)
                }
            }
        }

        alphabetsViewModel.alphabets.observe(viewLifecycleOwner) {
            alphabetListAdapter.submitList(it)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}