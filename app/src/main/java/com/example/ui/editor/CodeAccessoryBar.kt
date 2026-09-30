package com.example.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.IdeBackground
import com.example.ui.theme.IdeBluePrimary
import com.example.ui.theme.IdeBorder
import com.example.ui.theme.IdeCyanAccent
import com.example.ui.theme.IdeSurface
import com.example.ui.theme.IdeTextPrimary
import com.example.ui.theme.IdeWhite

@Composable
fun CodeAccessoryBar(
  onInsertSymbol: (String) -> Unit,
  onUndo: () -> Unit,
  onRedo: () -> Unit,
  onFormat: () -> Unit,
  modifier: Modifier = Modifier
) {
  val symbols = listOf(
    "TAB" to "    ",
    "{" to "{\n    \n}",
    "}" to "}",
    "(" to "()",
    ")" to ")",
    ";" to ";",
    "::" to "::",
    "<<" to " << ",
    ">>" to " >> ",
    "->" to "->",
    "\"" to "\"\"",
    "'" to "''",
    "<" to "<",
    ">" to ">",
    "[" to "[]",
    "]" to "]",
    "=" to " = ",
    "+" to "+",
    "-" to "-",
    "*" to "*",
    "&" to "&",
    "std::" to "std::",
    "cout" to "std::cout << ",
    "cin" to "std::cin >> ",
    "endl" to "std::endl;"
  )

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .height(46.dp)
      .testTag("code_accessory_bar"),
    color = IdeWhite,
    shadowElevation = 3.dp
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 6.dp, vertical = 5.dp)
        .horizontalScroll(rememberScrollState()),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Undo action
      AccessoryIconBtn(
        icon = Icons.Default.Undo,
        description = "Undo",
        onClick = onUndo
      )

      // Redo action
      AccessoryIconBtn(
        icon = Icons.Default.Redo,
        description = "Redo",
        onClick = onRedo
      )

      // Format code
      AccessoryIconBtn(
        icon = Icons.Default.AutoFixHigh,
        description = "Format Code",
        onClick = onFormat
      )

      Spacer(
        modifier = Modifier
          .width(1.dp)
          .height(26.dp)
          .background(IdeBorder)
          .padding(horizontal = 4.dp)
      )

      // Symbols row
      symbols.forEach { (label, insertValue) ->
        Box(
          modifier = Modifier
            .padding(horizontal = 3.dp)
            .height(34.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(IdeBackground)
            .border(1.dp, IdeBorder, RoundedCornerShape(8.dp))
            .clickable { onInsertSymbol(insertValue) }
            .padding(horizontal = 11.dp),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace,
            color = if (label == "TAB" || label.startsWith("std")) IdeBluePrimary else IdeTextPrimary
          )
        }
      }
    }
  }
}

@Composable
private fun AccessoryIconBtn(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  description: String,
  onClick: () -> Unit
) {
  Box(
    modifier = Modifier
      .padding(horizontal = 2.dp)
      .size(34.dp)
      .clip(RoundedCornerShape(8.dp))
      .background(IdeBackground)
      .border(1.dp, IdeBorder, RoundedCornerShape(8.dp))
      .clickable(onClick = onClick),
    contentAlignment = Alignment.Center
  ) {
    Icon(
      imageVector = icon,
      contentDescription = description,
      tint = IdeCyanAccent,
      modifier = Modifier.size(17.dp)
    )
  }
}
