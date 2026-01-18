package com.zibete.proyecto1.ui.media

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.zibete.proyecto1.R
import com.zibete.proyecto1.databinding.SelectSourcePicBinding

class PhotoSourceSheet : BottomSheetDialogFragment() {

    override fun getTheme(): Int = R.style.Zibe_BottomSheetTheme
    private var _binding: SelectSourcePicBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): BottomSheetDialog {
        val dialog = BottomSheetDialog(requireContext(), getTheme())
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        dialog.behavior.skipCollapsed = true
        dialog.setCanceledOnTouchOutside(true)
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SelectSourcePicBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        val showDelete = requireArguments().getBoolean(ARG_SHOW_DELETE, true)
        val titleRes = requireArguments().getInt(ARG_TITLE_RES, R.string.edit_picture)

        binding.cardDeleteProfilePhotoSelected.isVisible = showDelete
        binding.tvTitle.text = getString(titleRes)

        binding.cardCameraSelected.setOnClickListener {
            parentFragmentManager.setFragmentResult(REQ_KEY, Bundle().apply {
                putString(RES_ACTION, ACTION_CAMERA)
            })
            dismiss()
        }

        binding.cardGallerySelected.setOnClickListener {
            parentFragmentManager.setFragmentResult(REQ_KEY, Bundle().apply {
                putString(RES_ACTION, ACTION_GALLERY)
            })
            dismiss()
        }

        binding.cardDeleteProfilePhotoSelected.setOnClickListener {
            parentFragmentManager.setFragmentResult(REQ_KEY, Bundle().apply {
                putString(RES_ACTION, ACTION_DELETE)
            })
            dismiss()
        }

        binding.btnClose.setOnClickListener { dismiss() }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val ARG_SHOW_DELETE = "show_delete"
        private const val ARG_TITLE_RES = "title_res"
        const val TAG = "PhotoSourceSheet"
        const val REQ_KEY = "photo_source_sheet_result"
        const val RES_ACTION = "action"
        const val ACTION_CAMERA = "camera"
        const val ACTION_GALLERY = "gallery"
        const val ACTION_DELETE = "delete"

        fun newInstance(
            showDelete: Boolean,
            titleRes: Int
        ) = PhotoSourceSheet().apply {
            arguments = Bundle().apply {
                putBoolean(ARG_SHOW_DELETE, showDelete)
                putInt(ARG_TITLE_RES, titleRes)
            }
        }
    }
}