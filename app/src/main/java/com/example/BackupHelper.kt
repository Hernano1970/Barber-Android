package com.example

import android.content.Context
import android.net.Uri
import androidx.room.Room
import com.example.data.AppDatabase
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.first

object BackupHelper {
    
    fun createBackup(context: Context): File? {
        try {
            val dbName = "barberapp_database"
            val currentDB = context.getDatabasePath(dbName)
            if (!currentDB.exists()) return null
            
            val database = AppDatabase.getDatabase(context)
            database.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").close()

            val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault()).format(Date())
            val backupFileName = "BarberApp_Backup_${timestamp}.db"
            
            val appSettings = com.example.data.AppSettings(context)
            var backupsDir = File(appSettings.backupLocation)
            if (!backupsDir.exists()) {
                val created = backupsDir.mkdirs()
                if (!created) {
                    // Fallback to cache directory if unable to create in Documents
                    backupsDir = File(context.cacheDir, "backups")
                    if (!backupsDir.exists()) backupsDir.mkdirs()
                }
            }
            
            val backupFile = File(backupsDir, backupFileName)
            
            val prefsFile = File(context.applicationInfo.dataDir, "shared_prefs/barberapp_settings.xml")

            java.util.zip.ZipOutputStream(FileOutputStream(backupFile)).use { zos ->
                // Backup main db
                zos.putNextEntry(java.util.zip.ZipEntry("barberapp_database"))
                FileInputStream(currentDB).use { it.copyTo(zos) }
                zos.closeEntry()
                
                val shmFile = File("${currentDB.absolutePath}-shm")
                if (shmFile.exists()) {
                    zos.putNextEntry(java.util.zip.ZipEntry("barberapp_database-shm"))
                    FileInputStream(shmFile).use { it.copyTo(zos) }
                    zos.closeEntry()
                }

                val walFile = File("${currentDB.absolutePath}-wal")
                if (walFile.exists()) {
                    zos.putNextEntry(java.util.zip.ZipEntry("barberapp_database-wal"))
                    FileInputStream(walFile).use { it.copyTo(zos) }
                    zos.closeEntry()
                }

                // Backup prefs
                if (prefsFile.exists()) {
                    zos.putNextEntry(java.util.zip.ZipEntry("barberapp_settings.xml"))
                    FileInputStream(prefsFile).use { it.copyTo(zos) }
                    zos.closeEntry()
                }
            }
            
            // Clean up old backups if limit is set
            if (appSettings.maxAutoBackups > 0) {
                val allBackups = backupsDir.listFiles()?.filter { it.name.startsWith("BarberApp_Backup_") }
                    ?.sortedByDescending { it.lastModified() }
                
                if (allBackups != null && allBackups.size > appSettings.maxAutoBackups) {
                    val toDelete = allBackups.drop(appSettings.maxAutoBackups)
                    toDelete.forEach { it.delete() }
                }
            }
            
            return backupFile
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    suspend fun checkAutoBackup(context: Context) {
        val appSettings = com.example.data.AppSettings(context)
        val freq = appSettings.autoBackupFrequency
        if (freq == "Desactivado") return

        val backupsDir = File(appSettings.backupLocation)
        if (!backupsDir.exists()) backupsDir.mkdirs()
        
        val backups = backupsDir.listFiles()?.toList()?.sortedByDescending { it.lastModified() } ?: emptyList()
        val lastDate = backups.firstOrNull()?.lastModified() ?: 0L
        val now = System.currentTimeMillis()
        
        val interval = when (freq) {
            "Diario" -> 86400000L
            "Semanal" -> 86400000L * 7L
            "Mensual" -> 86400000L * 30L
            else -> 0L
        }
        
        if (interval > 0L && (now - lastDate) >= interval) {
            createBackup(context)
        }
    }

    suspend fun examineDb(context: Context, uri: Uri): BackupInfo? {
        try {
            val tempDbFile = File(context.cacheDir, "temp_examine.db")
            if (tempDbFile.exists()) tempDbFile.delete()
            
            val tempPrefsFile = File(context.cacheDir, "temp_examine_prefs.xml")
            if (tempPrefsFile.exists()) tempPrefsFile.delete()
            
            context.contentResolver.openInputStream(uri)?.use { input ->
                java.util.zip.ZipInputStream(input).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        if (entry.name == "barberapp_database") {
                            FileOutputStream(tempDbFile).use { it.write(zis.readBytes()) }
                        } else if (entry.name == "barberapp_database-shm") {
                            val tempShm = File("${tempDbFile.absolutePath}-shm")
                            FileOutputStream(tempShm).use { it.write(zis.readBytes()) }
                        } else if (entry.name == "barberapp_database-wal") {
                            val tempWal = File("${tempDbFile.absolutePath}-wal")
                            FileOutputStream(tempWal).use { it.write(zis.readBytes()) }
                        } else if (entry.name == "barberapp_settings.xml") {
                            FileOutputStream(tempPrefsFile).use { it.write(zis.readBytes()) }
                        }
                        entry = zis.nextEntry
                    }
                }
            }
            
            if (!tempDbFile.exists()) return null
            
            val tempDb = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                tempDbFile.absolutePath
            )
            .fallbackToDestructiveMigration()
            .build()
            
            val dao = tempDb.appointmentDao()
            val clientDao = tempDb.clientDao()
            val serviceDao = tempDb.serviceDao()
            
            val clientCount = clientDao.getAllClients().first().size
            val appointmentsList = dao.getAllAppointments().first()
            val apptCount = appointmentsList.size
            val serviceCount = serviceDao.getActiveServices().first().size
            val payments = appointmentsList.count { it.isPaid }
            
            var absences = 0
            if (tempPrefsFile.exists()) {
                val contents = tempPrefsFile.readText()
                absences = contents.split("start").size - 1 // very rough count without xml parsing
            }
            
            tempDb.close()
            
            return BackupInfo(
                fileName = getFileName(context, uri),
                date = SimpleDateFormat("dd/MM/yyyy HH:mm").format(Date(tempDbFile.lastModified())),
                version = "V1.4",
                clientCount = clientCount,
                apptCount = apptCount,
                paymentCount = payments,
                serviceCount = serviceCount,
                absenceCount = absences
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun restoreDb(context: Context, uri: Uri): Boolean {
        try {
            val dbName = "barberapp_database"
            val currentDB = context.getDatabasePath(dbName)
            val shm = File("${currentDB.absolutePath}-shm")
            val wal = File("${currentDB.absolutePath}-wal")
            
            // Close the database to ensure we can overwrite it safely
            AppDatabase.getDatabase(context).close()

            val prefsFile = File(context.applicationInfo.dataDir, "shared_prefs/barberapp_settings.xml")
            
            var foundDb = false
            var foundShm = false
            var foundWal = false
            context.contentResolver.openInputStream(uri)?.use { input ->
                java.util.zip.ZipInputStream(input).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        if (entry.name == "barberapp_database") {
                            FileOutputStream(currentDB).use { it.write(zis.readBytes()) }
                            foundDb = true
                        } else if (entry.name == "barberapp_database-shm") {
                            FileOutputStream(shm).use { it.write(zis.readBytes()) }
                            foundShm = true
                        } else if (entry.name == "barberapp_database-wal") {
                            FileOutputStream(wal).use { it.write(zis.readBytes()) }
                            foundWal = true
                        } else if (entry.name == "barberapp_settings.xml") {
                            FileOutputStream(prefsFile).use { it.write(zis.readBytes()) }
                        }
                        entry = zis.nextEntry
                    }
                }
            }
            
            if (foundDb) {
                if (!foundShm && shm.exists()) shm.delete()
                if (!foundWal && wal.exists()) wal.delete()
                return true
            }
            return false
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
    
    private fun getFileName(context: Context, uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val idx = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) {
                        result = it.getString(idx)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result ?: "backup.db"
    }

    data class BackupInfo(
        val fileName: String,
        val date: String,
        val version: String,
        val clientCount: Int,
        val apptCount: Int,
        val paymentCount: Int,
        val serviceCount: Int,
        val absenceCount: Int
    )
}
