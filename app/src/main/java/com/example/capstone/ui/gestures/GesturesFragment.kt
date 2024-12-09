package com.example.capstone.ui.gestures

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.capstone.databinding.FragmentGesturesBinding

class GesturesFragment : Fragment() {

    private var _binding: FragmentGesturesBinding? = null
    private val binding get() = _binding!!

    private val wordListAdapter by lazy { WordListAdapter() }
    private lateinit var gesturesViewModel: GesturesViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        gesturesViewModel = ViewModelProvider(this)[GesturesViewModel::class.java]
        _binding = FragmentGesturesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {
            rvWords.apply {
                layoutManager = LinearLayoutManager(requireContext())
                isNestedScrollingEnabled = false
                setHasFixedSize(false)
                adapter = wordListAdapter

                wordListAdapter.onItemClickListener = { word ->
                    val action = GesturesFragmentDirections.actionNavigationGesturesToGestureDetailFragment(word)
                    findNavController().navigate(action)
                }
            }
        }

        gesturesViewModel.words.observe(viewLifecycleOwner) {
            wordListAdapter.submitList(it)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}