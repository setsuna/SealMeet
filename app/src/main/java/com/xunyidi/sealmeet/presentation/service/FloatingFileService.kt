package com.xunyidi.sealmeet.presentation.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import com.xunyidi.sealmeet.R
import com.xunyidi.sealmeet.data.local.database.dao.AgendaDao
import com.xunyidi.sealmeet.data.local.database.dao.FileDao
import com.xunyidi.sealmeet.data.local.database.dao.MeetingDao
import com.xunyidi.sealmeet.data.local.database.entity.MeetingAgendaEntity
import com.xunyidi.sealmeet.data.local.database.entity.MeetingFileEntity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * 悬浮文件选择器服务
 * 
 * 用于在第三方应用上方显示悬浮按钮和文件列表，方便快速切换文件
 */
@AndroidEntryPoint
class FloatingFileService : Service() {

    @Inject
    lateinit var meetingDao: MeetingDao
    
    @Inject
    lateinit var agendaDao: AgendaDao
    
    @Inject
    lateinit var fileDao: FileDao

    private var windowManager: WindowManager? = null
    private var floatingButton: View? = null
    private var floatingPanel: View? = null
    
    private var currentMeetingId: String? = null
    private var agendaWithFiles: List<AgendaWithFiles> = emptyList()
    
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    
    // 面板是否展开
    private var isPanelExpanded = false

    data class AgendaWithFiles(
        val agenda: MeetingAgendaEntity,
        val files: List<MeetingFileEntity>
    )

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Timber.d("FloatingFileService onCreate")
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val meetingId = intent?.getStringExtra(EXTRA_MEETING_ID)
        if (meetingId != null && meetingId != currentMeetingId) {
            currentMeetingId = meetingId
            loadData(meetingId)
        }
        
        // 如果按钮还没创建，创建它
        if (floatingButton == null) {
            createFloatingButton()
        }
        
        return START_NOT_STICKY
    }

    private fun loadData(meetingId: String) {
        serviceScope.launch {
            try {
                val agendas = withContext(Dispatchers.IO) {
                    agendaDao.getByMeetingId(meetingId)
                }
                
                agendaWithFiles = agendas.map { agenda ->
                    val files = withContext(Dispatchers.IO) {
                        fileDao.getByAgendaId(agenda.id)
                    }
                    AgendaWithFiles(agenda, files)
                }
                
                Timber.d("加载数据完成: ${agendaWithFiles.size} 个议题")
            } catch (e: Exception) {
                Timber.e(e, "加载数据失败")
            }
        }
    }

    private fun createFloatingButton() {
        floatingButton = LayoutInflater.from(this).inflate(R.layout.floating_button, null)
        
        val layoutParams = WindowManager.LayoutParams().apply {
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            format = PixelFormat.TRANSLUCENT
            gravity = Gravity.START or Gravity.CENTER_VERTICAL  // 左侧居中
            x = 0  // 贴边
            y = 0
        }
        
        floatingButton?.setOnClickListener {
            togglePanel()
        }
        
        try {
            windowManager?.addView(floatingButton, layoutParams)
            Timber.i("悬浮按钮创建成功")
        } catch (e: Exception) {
            Timber.e(e, "悬浮按钮创建失败")
        }
    }

    private fun togglePanel() {
        if (isPanelExpanded) {
            hidePanel()
        } else {
            showPanel()
        }
    }

    @SuppressLint("InflateParams")
    private fun showPanel() {
        if (floatingPanel != null) return
        
        floatingPanel = LayoutInflater.from(this).inflate(R.layout.floating_panel, null)
        
        val panelLayoutParams = WindowManager.LayoutParams().apply {
            width = dpToPx(320)
            height = WindowManager.LayoutParams.MATCH_PARENT
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            format = PixelFormat.TRANSLUCENT
            gravity = Gravity.TOP or Gravity.START  // 左侧弹出
            x = 0
            y = 0
        }
        
        // 设置关闭按钮
        floatingPanel?.findViewById<ImageView>(R.id.btn_close)?.setOnClickListener {
            hidePanel()
        }
        
        // 设置退出按钮（关闭整个服务）
        floatingPanel?.findViewById<ImageView>(R.id.btn_exit)?.setOnClickListener {
            stopSelf()
        }
        
        // 填充议题和文件列表
        populateAgendaList()
        
        try {
            windowManager?.addView(floatingPanel, panelLayoutParams)
            isPanelExpanded = true
            
            // 隐藏悬浮按钮
            floatingButton?.isVisible = false
            
            Timber.d("面板展开")
        } catch (e: Exception) {
            Timber.e(e, "面板创建失败")
        }
    }

    private fun hidePanel() {
        floatingPanel?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {
                Timber.e(e, "移除面板失败")
            }
        }
        floatingPanel = null
        isPanelExpanded = false
        
        // 显示悬浮按钮
        floatingButton?.isVisible = true
        
        Timber.d("面板关闭")
    }

    private fun populateAgendaList() {
        val container = floatingPanel?.findViewById<LinearLayout>(R.id.agenda_container) ?: return
        container.removeAllViews()
        
        agendaWithFiles.forEachIndexed { index, item ->
            // 议题标题
            val agendaView = LayoutInflater.from(this).inflate(R.layout.item_floating_agenda, container, false)
            agendaView.findViewById<TextView>(R.id.tv_agenda_title).text = "${index + 1}. ${item.agenda.title}"
            
            val filesContainer = agendaView.findViewById<LinearLayout>(R.id.files_container)
            
            // 文件列表
            item.files.forEach { file ->
                val fileView = LayoutInflater.from(this).inflate(R.layout.item_floating_file, filesContainer, false)
                fileView.findViewById<TextView>(R.id.tv_file_name).text = file.originalName
                fileView.findViewById<ImageView>(R.id.iv_file_icon).setImageResource(getFileIconRes(file))
                
                fileView.setOnClickListener {
                    openFile(file)
                    hidePanel()
                }
                
                filesContainer.addView(fileView)
            }
            
            // 点击议题标题展开/折叠文件列表
            val headerView = agendaView.findViewById<View>(R.id.agenda_header)
            val arrowView = agendaView.findViewById<ImageView>(R.id.iv_arrow)
            
            headerView.setOnClickListener {
                if (filesContainer.isVisible) {
                    filesContainer.isVisible = false
                    arrowView.rotation = 0f
                } else {
                    filesContainer.isVisible = true
                    arrowView.rotation = 90f
                }
            }
            
            // 默认展开第一个
            if (index == 0) {
                filesContainer.isVisible = true
                arrowView.rotation = 90f
            } else {
                filesContainer.isVisible = false
                arrowView.rotation = 0f
            }
            
            container.addView(agendaView)
        }
    }

    private fun openFile(file: MeetingFileEntity) {
        try {
            val intent = createFileViewerIntent(file.localPath, file.originalName)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            Timber.d("打开文件: ${file.originalName}")
        } catch (e: Exception) {
            Timber.e(e, "打开文件失败")
            Toast.makeText(this, "打开文件失败", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createFileViewerIntent(filePath: String, fileName: String): Intent {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        
        return when (extension) {
            "ofd" -> {
                Intent(Intent.ACTION_VIEW).apply {
                    component = ComponentName(
                        "com.westone.ofdreader",
                        "com.westone.ofdreader.MainActivity"
                    )
                    putExtra("ofdFilePath", filePath)
                }
            }
            "doc", "docx", "xls", "xlsx", "ppt", "pptx", "pdf" -> {
                Intent("Start_YOZO_Office").apply {
                    putExtra("File_Name", filePath)
                    putExtra("File_Path", filePath)
                }
            }
            else -> {
                Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(Uri.fromFile(File(filePath)), getMimeType(extension))
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
        }
    }

    private fun getMimeType(extension: String): String {
        return when (extension) {
            "pdf" -> "application/pdf"
            "doc" -> "application/msword"
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            "xls" -> "application/vnd.ms-excel"
            "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            "ppt" -> "application/vnd.ms-powerpoint"
            "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
            "txt" -> "text/plain"
            else -> "*/*"
        }
    }

    private fun getFileIconRes(file: MeetingFileEntity): Int {
        val mimeType = file.mimeType
        val fileName = file.originalName
        
        return when {
            mimeType.contains("word", ignoreCase = true) ||
            fileName.endsWith(".doc", ignoreCase = true) ||
            fileName.endsWith(".docx", ignoreCase = true) -> R.drawable.ic_file_word
            
            mimeType.contains("excel", ignoreCase = true) ||
            fileName.endsWith(".xls", ignoreCase = true) ||
            fileName.endsWith(".xlsx", ignoreCase = true) -> R.drawable.ic_file_excel
            
            mimeType.contains("powerpoint", ignoreCase = true) ||
            fileName.endsWith(".ppt", ignoreCase = true) ||
            fileName.endsWith(".pptx", ignoreCase = true) -> R.drawable.ic_file_ppt
            
            mimeType.contains("pdf", ignoreCase = true) ||
            fileName.endsWith(".pdf", ignoreCase = true) -> R.drawable.ic_file_pdf
            
            else -> R.drawable.ic_file_other
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("FloatingFileService onDestroy")
        
        hidePanel()
        
        floatingButton?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {
                Timber.e(e, "移除悬浮按钮失败")
            }
        }
        floatingButton = null
    }

    companion object {
        private const val EXTRA_MEETING_ID = "meeting_id"
        
        fun start(context: Context, meetingId: String) {
            val intent = Intent(context, FloatingFileService::class.java).apply {
                putExtra(EXTRA_MEETING_ID, meetingId)
            }
            context.startService(intent)
        }
        
        fun stop(context: Context) {
            context.stopService(Intent(context, FloatingFileService::class.java))
        }
    }
}
