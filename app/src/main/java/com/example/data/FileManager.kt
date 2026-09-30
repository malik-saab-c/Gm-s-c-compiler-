package com.example.data

import android.content.Context
import android.util.Log
import com.example.model.CppFile
import java.io.File
import java.util.UUID

object FileManager {
  private const val TAG = "FileManager"
  private const val PROJECTS_DIR_NAME = "cpp_projects"

  private fun getProjectsDir(context: Context): File {
    val dir = File(context.filesDir, PROJECTS_DIR_NAME)
    if (!dir.exists()) {
      dir.mkdirs()
    }
    return dir
  }

  /**
   * Loads all files from internal storage. If none exist (first launch), initializes default files.
   */
  fun loadFiles(context: Context): List<CppFile> {
    val dir = getProjectsDir(context)
    val filesOnDisk = dir.listFiles { file ->
      file.isFile && (file.name.endsWith(".cpp") || file.name.endsWith(".h") || file.name.endsWith(".hpp"))
    }

    if (filesOnDisk.isNullOrEmpty()) {
      // First launch: initialize default starter files
      val main = DefaultProjects.defaultMainFile
      val header = DefaultProjects.defaultHeaderFile
      saveFile(context, main)
      saveFile(context, header)
      return listOf(main, header)
    }

    // Load existing files sorted with main.cpp first
    val loaded = filesOnDisk.map { file ->
      val name = file.name
      val content = file.readText()
      CppFile(
        id = name, // Use stable filename as ID
        name = name,
        content = content,
        isMain = name == "main.cpp"
      )
    }.sortedWith(compareBy({ !it.isMain }, { it.name }))

    return if (loaded.isEmpty()) {
      listOf(DefaultProjects.defaultMainFile)
    } else {
      loaded
    }
  }

  /**
   * Persists a CppFile to disk
   */
  fun saveFile(context: Context, file: CppFile): Boolean {
    return try {
      val dir = getProjectsDir(context)
      val target = File(dir, file.name)
      target.writeText(file.content)
      Log.d(TAG, "Saved file ${file.name} to disk (${file.content.length} chars)")
      true
    } catch (e: Exception) {
      Log.e(TAG, "Failed to save file ${file.name}", e)
      false
    }
  }

  /**
   * Deletes a file from disk
   */
  fun deleteFile(context: Context, fileName: String): Boolean {
    return try {
      val dir = getProjectsDir(context)
      val target = File(dir, fileName)
      if (target.exists()) {
        val deleted = target.delete()
        Log.d(TAG, "Deleted file $fileName from disk: $deleted")
        deleted
      } else {
        true
      }
    } catch (e: Exception) {
      Log.e(TAG, "Failed to delete file $fileName", e)
      false
    }
  }

  /**
   * Renames a file on disk
   */
  fun renameFile(context: Context, oldName: String, newName: String, content: String): Boolean {
    return try {
      val dir = getProjectsDir(context)
      val oldFile = File(dir, oldName)
      val newFile = File(dir, newName)
      if (oldFile.exists()) {
        oldFile.delete()
      }
      newFile.writeText(content)
      Log.d(TAG, "Renamed file on disk from $oldName to $newName")
      true
    } catch (e: Exception) {
      Log.e(TAG, "Failed to rename file $oldName", e)
      false
    }
  }
}
