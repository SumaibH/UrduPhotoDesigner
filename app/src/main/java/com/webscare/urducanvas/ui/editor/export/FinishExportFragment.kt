package com.webscare.urducanvas.ui.editor.export

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.webscare.urducanvas.databinding.DialogLoadingProgressBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.content.FileProvider
import androidx.core.content.ContextCompat
import com.webscare.urducanvas.BuildConfig
import com.webscare.ads.NativeSize
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.print.PrintHelper
import com.webscare.urducanvas.R
import com.webscare.urducanvas.common.canvas.CanvasViewModel
import com.webscare.urducanvas.common.utils.ImageProcessor
import com.webscare.urducanvas.common.utils.Utils.addPressEffect
import com.webscare.urducanvas.common.utils.Utils.copyToClipboard
import com.webscare.urducanvas.databinding.FragmentFinishExportBinding
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import androidx.core.graphics.scale
import com.google.android.material.snackbar.Snackbar
import com.webscare.urducanvas.di.AppReviewManager
import javax.inject.Inject
import com.webscare.urducanvas.common.utils.InsetUtils.applyStatusBarTopPadding
import com.webscare.urducanvas.analytics.AnalyticsTracker
import com.webscare.urducanvas.analytics.session.SessionStateManager

@AndroidEntryPoint
class FinishExportFragment : androidx.fragment.app.Fragment() {
    private var _binding: FragmentFinishExportBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var appReviewManager: AppReviewManager

    @Inject
    lateinit var analyticsTracker: AnalyticsTracker

    @Inject
    lateinit var sessionStateManager: SessionStateManager

    @Inject
    lateinit var adAnalyticsCoordinator: com.webscare.urducanvas.analytics.ads.AdAnalyticsCoordinator

    val viewModel: CanvasViewModel by activityViewModels()

    // Debug zip export state (only reachable when !IS_PROD_LOGIC)
    private var zipExportJob: Job? = null
    private var zipDialog: Dialog? = null
    private var zipDialogBinding: DialogLoadingProgressBinding? = null
    private var zipIconRotation: ObjectAnimator? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFinishExportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Edge to edge: the window no longer reserves the status bar, so leave the margin here.
        view.applyStatusBarTopPadding()
        
        binding.exportSuccessNativeAd.setAdUnitIdAndSize(BuildConfig.AD_NATIVE_EXPORT_SUCCESS, NativeSize.MEDIUM)
        // The attach is the opportunity — an in-layout native has no "show" call. The matching
        // impression comes from AdConfig.onAdImpression, wired in MyApplication, which is a
        // real AdMob callback rather than an inference, so this placement gets both halves.
        adAnalyticsCoordinator.onAdSlotAttached(
            adUnitName = "native_export_success",
            adUnitId = BuildConfig.AD_NATIVE_EXPORT_SUCCESS,
            adFormat = "native",
            triggerFeature = "export_success"
        )

        view.alpha = 0f
        view.translationY = 80f
        view.animate().alpha(1f).translationY(0f).setDuration(350).start()
        setEvents()
        initObservers()

        appReviewManager.requestReviewIfEligible(requireActivity())
    }

    private fun formatFileSize(sizeMB: Double?): String {
        val size = sizeMB ?: return "0 KB"
        return if (size < 1.0) {
            val sizeKB = size * 1024.0
            "%.0f KB".format(sizeKB)
        } else {
            "%.1f MB".format(size)
        }
    }

    private fun initObservers() {
        viewModel.exportResult.observe(viewLifecycleOwner) { result ->
            val tip = if (result?.pdfPath != null) {
                requireActivity().getString(R.string.exportTip_pdf)
            } else {
                requireActivity().getString(R.string.exportTip_image)
            }
            binding.tip.text = tip
            binding.fileName.text = result?.fileName
            binding.fileNameDetail.text = result?.fileName
            binding.fileType.text = "${result?.format} File"
            binding.fileSizeDetail.text = formatFileSize(result?.fileSizeMB)
            binding.fileResolutionDetail.text = result?.resolution
            binding.fileQualityDetail.text = result?.quality
            binding.fileLocationDetail.text = result?.pdfPath ?: result?.imagePath
            result?.imagePath?.let { path ->
                ImageProcessor.filePathToBitmap(path)?.let { bitmap ->
                    binding.previewImage.setImageBitmap(bitmap)
                }
            }        }
    }

    private fun setEvents() {
        binding.preview.addPressEffect {
            val export = viewModel.exportResult.value ?: return@addPressEffect
            val bundle = Bundle().apply {
                putString("imagePath", export.imagePath)
            }
            view?.post {
                findNavController().navigate(R.id.previewExportFragment, bundle)
            }
        }

        binding.fileLocationDetail.addPressEffect {
            val context = requireContext()
            requireActivity().copyToClipboard(requireView(), "Exported Path", binding.fileLocationDetail.text.toString())
            
            // Swap icon to ic_done tinted with appColor
            val doneDrawable = ContextCompat.getDrawable(context, R.drawable.ic_done)?.mutate()
            doneDrawable?.setTint(ContextCompat.getColor(context, R.color.appColor))
            binding.fileLocationDetail.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, doneDrawable, null)
            
            // Post delayed to switch back to ic_copy tinted with black
            binding.fileLocationDetail.postDelayed({
                if (isAdded) {
                    val copyDrawable = ContextCompat.getDrawable(context, R.drawable.ic_copy)?.mutate()
                    copyDrawable?.setTint(ContextCompat.getColor(context, R.color.black))
                    binding.fileLocationDetail.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, copyDrawable, null)
                }
            }, 2000)
        }

        binding.back.addPressEffect { findNavController().navigateUp() }
        binding.btnExportAnother.addPressEffect { findNavController().popBackStack(R.id.editorFragment, false) }
        binding.backToHome.addPressEffect {
            val navOptions = NavOptions.Builder()
                .setPopUpTo(R.id.homeFragment, false)
                .build()
            view?.post { findNavController().navigate(R.id.homeFragment, null, navOptions) }
        }

        // 🔹 Share logic
        // 🔹 Share logic
        binding.share.addPressEffect {
            val export = viewModel.exportResult.value ?: return@addPressEffect
            analyticsTracker.logShareInitiated(
                channel = "system_share",
                format = export.format ?: "image",
                templateId = export.sourceTemplateId ?: sessionStateManager.activeTemplateId
            )

            // Share the final exported file (image or PDF) — what the user actually made.
            // Project sharing (.urdc) is on the Export Settings screen; the debug zip
            // (json + thumbnail) has its own button below.
            val filePath = export.pdfPath ?: export.imagePath
            val file = File(filePath)
            if (!file.exists()) return@addPressEffect

            val mimeType = when {
                filePath.endsWith(".pdf", true) -> "application/pdf"
                filePath.endsWith(".png", true) -> "image/png"
                filePath.endsWith(".jpg", true) || filePath.endsWith(".jpeg", true) -> "image/jpeg"
                filePath.endsWith(".webp", true) -> "image/webp"
                else -> "image/*"
            }
            val uri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Share"))
        }

        // 🔹 Debug only: export the project json + thumbnail as a zip into Downloads
        binding.exportZip.isVisible = !BuildConfig.IS_PROD_LOGIC
        binding.exportZip.addPressEffect {
            val export = viewModel.exportResult.value ?: return@addPressEffect
            if (zipExportJob?.isActive == true) return@addPressEffect
            exportDebugZip(export.jsonPath, export.imagePath)
        }

        // 🔹 Open logic (PDF or Image)
        binding.open.addPressEffect {
            val export = viewModel.exportResult.value ?: return@addPressEffect
            analyticsTracker.logShareInitiated(
                channel = "open_with",
                format = export.format ?: "image",
                templateId = export.sourceTemplateId ?: sessionStateManager.activeTemplateId
            )
            val filePath = export.pdfPath ?: export.imagePath
            val file = File(filePath)
            if (!file.exists()) return@addPressEffect

            // Use FileProvider to give safe Uri
            val uri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                file
            )

            val mimeType = if (filePath.endsWith(".pdf", true)) {
                "application/pdf"
            } else {
                "image/*"   // restrict to image viewers only
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Open with"))
        }

        // 🔹 Print logic
        binding.print.addPressEffect {
            val export = viewModel.exportResult.value ?: return@addPressEffect
            analyticsTracker.logShareInitiated(
                channel = "print",
                format = export.format ?: "image",
                templateId = export.sourceTemplateId ?: sessionStateManager.activeTemplateId
            )

            export.pdfPath?.let { pdfPath ->
                val pdfFile = File(pdfPath)
                if (pdfFile.exists()) {
                    val uri = FileProvider.getUriForFile(
                        requireContext(),
                        "${requireContext().packageName}.fileprovider",
                        pdfFile
                    )
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        data = uri
                        type = "application/pdf"
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    startActivity(Intent.createChooser(intent, "Print PDF"))
                }
            } ?: run {
                val imagePath = export.imagePath ?: return@addPressEffect
                val bitmap = ImageProcessor.filePathToBitmap(imagePath) ?: return@addPressEffect
                val fileName = export.fileName ?: "Design"

                val activity = requireActivity()
                if (!PrintHelper.systemSupportsPrint()) {
                    Snackbar.make(binding.root, "Printing is not supported on this device", Snackbar.LENGTH_SHORT).show()
                    return@addPressEffect
                }

                activity.window.decorView.post {
                    if (!activity.isFinishing && !activity.isDestroyed) {
                        try {
                            PrintHelper(activity).apply {
                                scaleMode = PrintHelper.SCALE_MODE_FIT
                            }.printBitmap(fileName, bitmap)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Snackbar.make(binding.root, "Failed to start printing", Snackbar.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    // ── Debug zip export ──────────────────────────────────────────────────────────

    private fun exportDebugZip(jsonPath: String, imagePath: String) {
        val jsonFile = File(jsonPath)
        val imageFile = File(imagePath)
        if (!jsonFile.exists() || !imageFile.exists()) {
            Snackbar.make(binding.root, "Project files not found — nothing to zip", Snackbar.LENGTH_SHORT).show()
            return
        }

        showZipProgressDialog()
        zipExportJob = viewLifecycleOwner.lifecycleScope.launch {
            var thumbnailFile: File? = null
            try {
                setZipProgress(10, "Creating thumbnail…")
                thumbnailFile = withContext(Dispatchers.IO) { createThumbnail(imageFile, imagePath) }
                if (thumbnailFile == null) {
                    dismissZipProgressDialog()
                    Snackbar.make(binding.root, "Could not read the exported image", Snackbar.LENGTH_SHORT).show()
                    return@launch
                }

                setZipProgress(40, "Compressing project files…")
                val downloadFolder = android.os.Environment
                    .getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                val zipFile = File(downloadFolder, "design_${System.currentTimeMillis()}.zip")
                withContext(Dispatchers.IO) {
                    downloadFolder.mkdirs()
                    createZipFromFiles(listOf(jsonFile, thumbnailFile), zipFile)
                }

                setZipProgress(100, "Done")
                delay(250) // let the bar visibly reach 100% before the dialog goes
                dismissZipProgressDialog()
                Snackbar.make(
                    binding.root,
                    "Zip exported: Download/${zipFile.name}",
                    Snackbar.LENGTH_LONG
                ).show()
            } catch (e: Exception) {
                e.printStackTrace()
                dismissZipProgressDialog()
                if (isAdded) {
                    Snackbar.make(binding.root, "Zip export failed: ${e.message}", Snackbar.LENGTH_LONG).show()
                }
            } finally {
                thumbnailFile?.delete()
            }
        }
    }

    private fun showZipProgressDialog() {
        if (!isAdded || zipDialog?.isShowing == true) return
        val dialogBinding = DialogLoadingProgressBinding.inflate(LayoutInflater.from(requireActivity()))
        zipDialogBinding = dialogBinding
        dialogBinding.title.text = "Exporting Zip"
        dialogBinding.subtitle.text = "Processing…"
        dialogBinding.tvProgressPercent.text = "0% complete"
        dialogBinding.progressBar.progress = 0
        dialogBinding.cancel.isVisible = false

        zipDialog = Dialog(requireContext()).apply {
            setContentView(dialogBinding.root)
            setCancelable(false)
            window?.setBackgroundDrawableResource(android.R.color.transparent)
            val params = window?.attributes
            params?.width = (resources.displayMetrics.widthPixels * 0.8).toInt()
            params?.height = ViewGroup.LayoutParams.WRAP_CONTENT
            window?.attributes = params
            window?.setGravity(Gravity.CENTER)
            show()
        }

        zipIconRotation = ObjectAnimator.ofFloat(dialogBinding.view4, View.ROTATION, 0f, 360f).apply {
            duration = 1000L
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            start()
        }
    }

    private fun setZipProgress(percent: Int, stage: String) {
        val b = zipDialogBinding ?: return
        b.subtitle.text = stage
        b.progressBar.progress = percent
        b.tvProgressPercent.text = "$percent% complete"
    }

    private fun dismissZipProgressDialog() {
        zipIconRotation?.cancel()
        zipIconRotation = null
        zipDialog?.dismiss()
        zipDialog = null
        zipDialogBinding = null
    }

    private fun createThumbnail(originalFile: File, imagePath: String): File? {
        val original = ImageProcessor.filePathToBitmap(imagePath) ?: return null

        val maxDim = 512
        val scale = maxDim.toFloat() / maxOf(original.width, original.height)
        val thumbWidth = (original.width * scale).toInt()
        val thumbHeight = (original.height * scale).toInt()

        val thumbnail = original.scale(thumbWidth, thumbHeight)

        val thumbFile = File(requireContext().cacheDir, "thumb_${originalFile.nameWithoutExtension}.jpg")
        thumbFile.outputStream().use { out ->
            thumbnail.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, out)
        }
        thumbnail.recycle()
        original.recycle()

        return thumbFile
    }

    fun createZipFromFiles(files: List<File>, outputZip: File) {
        java.util.zip.ZipOutputStream(FileOutputStream(outputZip)).use { zos ->
            files.forEach { file ->
                FileInputStream(file).use { fis ->
                    val entry = java.util.zip.ZipEntry(file.name)
                    zos.putNextEntry(entry)
                    fis.copyTo(zos)
                    zos.closeEntry()
                }
            }
        }
    }

    override fun onDestroyView() {
        zipExportJob?.cancel()
        zipExportJob = null
        dismissZipProgressDialog()
        _binding?.previewImage?.setImageBitmap(null)
        super.onDestroyView()
        _binding = null
    }
}