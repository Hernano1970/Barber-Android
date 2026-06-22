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

                // Backup QR images
                val filesDir = context.filesDir
                filesDir.listFiles()?.forEach { file ->
                    if (file.isFile && file.name.startsWith("qr_")) {
                        zos.putNextEntry(java.util.zip.ZipEntry("files/${file.name}"))
                        FileInputStream(file).use { it.copyTo(zos) }
                        zos.closeEntry()
                    }
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
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4)
            // Se elimina fallbackToDestructiveMigration() para evitar borrado silente en evaluación.
            // Si el backup contiene una BD de versión muy antigua o corrupta, Room fallará e informaremos explícitamente en el catch.
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
                try {
                    val matcher = java.util.regex.Pattern.compile("name=\"absencesList\">([^<]*)</string>").matcher(contents)
                    if (matcher.find()) {
                        val jsonStr = matcher.group(1)?.replace("&quot;", "\"") ?: "[]"
                        absences = org.json.JSONArray(jsonStr).length()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
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
            return BackupInfo(
                fileName = getFileName(context, uri),
                date = "-",
                version = "-",
                clientCount = 0,
                apptCount = 0,
                paymentCount = 0,
                serviceCount = 0,
                absenceCount = 0,
                isError = true,
                errorMessage = "Error al leer el archivo de base de datos de respaldo: es de una versión incompatible o está corrupto. (${e.message})"
            )
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
            
            // Crear copia de seguridad temporal de emergencia antes de restaurar
            try {
                val appSettings = com.example.data.AppSettings(context)
                var backupsDir = File(appSettings.backupLocation)
                if (!backupsDir.exists()) {
                    if (!backupsDir.mkdirs()) {
                        backupsDir = File(context.cacheDir, "backups")
                        if (!backupsDir.exists()) backupsDir.mkdirs()
                    }
                }
                
                val emergencyBackupName = "pre_restore_backup_${java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())}.zip"
                val emergencyBackupPath = File(backupsDir, emergencyBackupName)
                java.util.zip.ZipOutputStream(FileOutputStream(emergencyBackupPath)).use { zos ->
                    if (currentDB.exists()) {
                        zos.putNextEntry(java.util.zip.ZipEntry("barberapp_database"))
                        FileInputStream(currentDB).use { it.copyTo(zos) }
                        zos.closeEntry()
                    }
                    if (shm.exists()) {
                        zos.putNextEntry(java.util.zip.ZipEntry("barberapp_database-shm"))
                        FileInputStream(shm).use { it.copyTo(zos) }
                        zos.closeEntry()
                    }
                    if (wal.exists()) {
                        zos.putNextEntry(java.util.zip.ZipEntry("barberapp_database-wal"))
                        FileInputStream(wal).use { it.copyTo(zos) }
                        zos.closeEntry()
                    }
                    if (prefsFile.exists()) {
                        zos.putNextEntry(java.util.zip.ZipEntry("barberapp_settings.xml"))
                        FileInputStream(prefsFile).use { it.copyTo(zos) }
                        zos.closeEntry()
                    }
                }
                android.util.Log.i("BackupHelper", "Copia de emergencia creada en: ${emergencyBackupPath.absolutePath}")
                
                // Rotar los backups de emergencia (mantener últimos 3)
                val allEmergencyBackups = backupsDir.listFiles()?.filter { it.name.startsWith("pre_restore_backup_") }
                    ?.sortedByDescending { it.lastModified() }
                if (allEmergencyBackups != null && allEmergencyBackups.size > 3) {
                    val toDelete = allEmergencyBackups.drop(3)
                    toDelete.forEach { it.delete() }
                }
                
            } catch (e: Exception) {
                android.util.Log.e("BackupHelper", "Fallo al crear copia de emergencia: ${e.message}")
            }
            
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
                        } else if (entry.name.startsWith("files/qr_")) {
                            val fileName = entry.name.substring("files/".length)
                            val file = File(context.filesDir, fileName)
                            FileOutputStream(file).use { it.write(zis.readBytes()) }
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
        val absenceCount: Int,
        val isError: Boolean = false,
        val errorMessage: String = ""
    )
}
