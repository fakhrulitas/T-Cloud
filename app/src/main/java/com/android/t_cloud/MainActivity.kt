package com.android.t_cloud

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.JsonParser
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {

    private lateinit var appTitle: TextView
    private lateinit var tvInstruction: TextView
    private lateinit var ivHeaderIcon: ImageView
    private lateinit var ivSortIcon: ImageView
    private lateinit var btnSort: LinearLayout
    private lateinit var btnUpload: FloatingActionButton
    private lateinit var btnNewFolder: FloatingActionButton
    private lateinit var recyclerView: RecyclerView
    private lateinit var bottomNav: BottomNavigationView

    private val allDocuments = mutableListOf<Document>()
    private var currentFolderId: String = "root"
    private var isSortingAZ: Boolean = true
    private var currentMenu: String = "files"
    private var loadingDialog: AlertDialog? = null

    private val pickFile = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { uploadToTelegram(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inisialisasi (Sesuaikan dengan ID di XML kamu)
        appTitle = findViewById(R.id.appTitle)
        tvInstruction = findViewById(R.id.tvInstruction)
        ivHeaderIcon = findViewById(R.id.ivHeaderIcon)
        ivSortIcon = findViewById(R.id.ivSortIcon)
        btnSort = findViewById(R.id.btnSort)
        btnUpload = findViewById(R.id.btnUpload)
        btnNewFolder = findViewById(R.id.btnNewFolder) // Pastikan sudah ditambah di XML
        recyclerView = findViewById(R.id.recyclerView)
        bottomNav = findViewById(R.id.bottomNavigation)

        loadFromDisk()
        setupBottomNav()

        // Default awal ke menu Files
        bottomNav.selectedItemId = R.id.nav_files

        btnSort.setOnClickListener {
            isSortingAZ = !isSortingAZ
            updateListUI()
        }

        btnUpload.setOnClickListener { pickFile.launch("*/*") }
        btnNewFolder.setOnClickListener { showCreateFolderDialog() }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (currentMenu == "files" && currentFolderId != "root") {
                    currentFolderId = "root"
                    updateListUI()
                } else finish()
            }
        })

        fetchChatId()
    }

    private fun setupBottomNav() {
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    currentMenu = "home"
                    item.setIcon(R.drawable.ic_home_filled)
                    bottomNav.menu.findItem(R.id.nav_files).setIcon(R.drawable.ic_files_outline)

                    appTitle.text = "Terbaru"
                    ivHeaderIcon.visibility = View.VISIBLE
                    btnSort.visibility = View.GONE
                    btnNewFolder.visibility = View.GONE // Sembunyi di Home

                    recyclerView.layoutManager = GridLayoutManager(this, 2)
                    updateListUI()
                    true
                }
                R.id.nav_files -> {
                    currentMenu = "files"
                    item.setIcon(R.drawable.ic_files_filled)
                    bottomNav.menu.findItem(R.id.nav_home).setIcon(R.drawable.ic_home_outline)

                    appTitle.text = "T-Cloud"
                    ivHeaderIcon.visibility = View.GONE
                    btnSort.visibility = View.VISIBLE
                    btnNewFolder.visibility = View.VISIBLE // Muncul di Files

                    recyclerView.layoutManager = LinearLayoutManager(this)
                    updateListUI()
                    true
                }
                else -> false
            }
        }
    }

    private fun updateListUI() {
        if (currentMenu == "home") {
            // Tampilan Home: Ambil file (bukan folder) terbaru
            val recentFiles = allDocuments.filter { !it.isFolder }.take(10)
            renderAdapter(recentFiles)
        } else {
            // Tampilan Files
            tvInstruction.text = if (isSortingAZ) "A-Z" else "Baru"
            ivSortIcon.setImageResource(if (isSortingAZ) android.R.drawable.ic_menu_sort_alphabetically else android.R.drawable.ic_menu_recent_history)

            val filteredList = allDocuments.filter { it.parentId == currentFolderId }
            val sortedList = if (isSortingAZ) {
                filteredList.sortedWith(compareByDescending<Document> { it.isFolder }.thenBy { it.file_name?.lowercase() ?: "" })
            } else {
                filteredList.sortedByDescending { it.isFolder }
            }
            renderAdapter(sortedList)
        }
    }

    private fun renderAdapter(list: List<Document>) {
        recyclerView.adapter = FileAdapter(list,
            onClick = { doc ->
                if (doc.isFolder) {
                    currentFolderId = doc.file_id ?: ""
                    updateListUI()
                } else {
                    if (doc.file_name?.lowercase()?.endsWith(".pdf") == true) downloadFile(doc.file_id ?: "")
                    else Toast.makeText(this, "Buka di Telegram Bot", Toast.LENGTH_SHORT).show()
                }
            },
            onLongClick = { doc, _ -> showDeleteDialog(doc) }
        )
    }

    private fun uploadToTelegram(uri: Uri) {
        val chatId = getChatId() ?: return
        showLoading(true)
        var fileName = "file"
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst()) fileName = cursor.getString(nameIndex)
        }

        try {
            val inputStream = contentResolver.openInputStream(uri) ?: return
            val bytes = inputStream.readBytes()
            inputStream.close()

            val requestFile = bytes.toRequestBody("application/octet-stream".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("document", fileName, requestFile)

            RetrofitClient.api.sendDocument(body, chatId).enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    runOnUiThread { showLoading(false) }
                    if (response.isSuccessful) {
                        val jsonString = response.body()?.string() ?: ""
                        val result = JsonParser().parse(jsonString).asJsonObject.getAsJsonObject("result")
                        val fileId = if (result.has("document")) result.getAsJsonObject("document").get("file_id").asString
                        else "msg_${result.get("message_id").asString}"

                        allDocuments.add(0, Document(fileId, fileName, false, 0, currentFolderId))
                        saveToDisk()
                        runOnUiThread { updateListUI() }
                    } else {
                        val errorInfo = response.errorBody()?.string() ?: "Unknown Error"
                        Toast.makeText(this@MainActivity, "Gagal: $errorInfo", Toast.LENGTH_LONG).show()
                    }
                }
                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    runOnUiThread {
                        showLoading(false)
                        Toast.makeText(this@MainActivity, "Koneksi Gagal: ${t.message}", Toast.LENGTH_LONG).show()
                    }
                }
            })
        } catch (e: Exception) {
            showLoading(false)
            Toast.makeText(this, "Sistem Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun downloadFile(fileId: String) {
        RetrofitClient.api.getFile(fileId).enqueue(object : Callback<FileResponse> {
            override fun onResponse(call: Call<FileResponse>, response: Response<FileResponse>) {
                if (response.isSuccessful) {
                    val path = response.body()?.result?.file_path
                    if (path != null) {
                        val token = "8550143265:AAE3fsI6k692b2XirLh_tHHwGv2rVY1nfqc"
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://api.telegram.org/file/bot$token/$path")))
                    }
                }
            }
            override fun onFailure(call: Call<FileResponse>, t: Throwable) {}
        })
    }

    private fun showCreateFolderDialog() {
        val input = EditText(this)
        input.hint = "Nama Folder"
        AlertDialog.Builder(this).setTitle("Buat Folder").setView(input)
            .setPositiveButton("Buat") { _, _ ->
                val name = input.text.toString()
                if (name.isNotEmpty()) {
                    allDocuments.add(0, Document("fld_${System.currentTimeMillis()}", name, true, 0, currentFolderId))
                    saveToDisk(); updateListUI()
                }
            }.show()
    }

    private fun showDeleteDialog(doc: Document) {
        AlertDialog.Builder(this).setTitle("Hapus").setMessage("Hapus ${doc.file_name}?")
            .setPositiveButton("Ya") { _, _ -> allDocuments.remove(doc); saveToDisk(); updateListUI() }.show()
    }

    private fun saveToDisk() {
        val prefs = getSharedPreferences("tcloud_prefs", MODE_PRIVATE)
        val dataString = allDocuments.joinToString(";") { "${it.file_id ?: ""}|${it.file_name ?: ""}|${if (it.isFolder) 1 else 0}|${it.parentId}" }
        prefs.edit().putString("saved_files", dataString).apply()
    }

    private fun loadFromDisk() {
        val prefs = getSharedPreferences("tcloud_prefs", MODE_PRIVATE)
        val dataString = prefs.getString("saved_files", "") ?: ""
        if (dataString.isNotEmpty()) {
            allDocuments.clear()
            dataString.split(";").forEach {
                val parts = it.split("|")
                if (parts.size >= 4) allDocuments.add(Document(parts[0], parts[1], parts[2] == "1", 0, parts[3]))
            }
        }
    }

    private fun getChatId() = getSharedPreferences("app_prefs", MODE_PRIVATE).getString("chat_id", null)

    private fun fetchChatId() {
        RetrofitClient.api.getUpdates().enqueue(object : Callback<UpdateResponse> {
            override fun onResponse(call: Call<UpdateResponse>, response: Response<UpdateResponse>) {
                response.body()?.result?.lastOrNull()?.message?.chat?.let {
                    val id = it.id.toString()
                    getSharedPreferences("app_prefs", MODE_PRIVATE).edit().putString("chat_id", id).apply()
                }
            }
            override fun onFailure(call: Call<UpdateResponse>, t: Throwable) {}
        })
    }

    private fun showLoading(isShowing: Boolean) {
        if (isShowing) {
            val builder = AlertDialog.Builder(this)
            val layout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(60, 60, 60, 60)
                gravity = Gravity.CENTER_VERTICAL
            }
            val progressBar = ProgressBar(this).apply { isIndeterminate = true; setPadding(0, 0, 40, 0) }
            val textView = TextView(this).apply { text = "Mengunggah..."; setTextColor(Color.BLACK) }
            layout.addView(progressBar); layout.addView(textView)
            loadingDialog = builder.setView(layout).setCancelable(false).create()
            loadingDialog?.show()
        } else loadingDialog?.dismiss()
    }
}