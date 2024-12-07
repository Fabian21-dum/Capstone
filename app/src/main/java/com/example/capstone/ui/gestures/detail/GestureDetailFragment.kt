package com.example.capstone.ui.gestures.detail

import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.core.view.isVisible
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.navigation.NavArgs
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.capstone.R
import com.example.capstone.databinding.FragmentGestureDetailBinding
import com.example.capstone.databinding.FragmentGesturesBinding

class GestureDetailFragment : Fragment() {

    private var _binding: FragmentGestureDetailBinding? = null
    private val binding get() = _binding!!

    private val args by navArgs<GestureDetailFragmentArgs>()
    private lateinit var player: ExoPlayer

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGestureDetailBinding.inflate(layoutInflater, container, false)
        return  binding.root
    }

    @OptIn(UnstableApi::class)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {
            tvTitle.text = args.word.title
            tvDescription.text = args.word.description

            btnBack.setOnClickListener { findNavController().popBackStack() }

            player = ExoPlayer.Builder(requireContext()).build()
            playerView.player = player
            playerView.hideController()
            playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            playerView.useController = false

            val mediaItem = MediaItem.fromUri(Uri.parse(args.word.videoUrl))
            player.addMediaItem(mediaItem)
            player.prepare()
            player.playWhenReady = true

            btnPlay.setOnClickListener {
                player.play()
                btnPlay.isVisible = false
                imageThumbnail.isVisible = false
            }

            imageThumbnail.setOnClickListener {
                imageThumbnail.isVisible = false
                btnPlay.isVisible = false
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        player.release()
        _binding = null
    }
}