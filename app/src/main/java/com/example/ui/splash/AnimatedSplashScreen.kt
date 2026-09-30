package com.example.ui.splash

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.IdeBlueLight
import com.example.ui.theme.IdeBluePrimary
import com.example.ui.theme.IdeCyanAccent
import com.example.ui.theme.IdeEmeraldGreen
import com.example.ui.theme.IdeTextMuted
import com.example.ui.theme.IdeTextPrimary
import com.example.ui.theme.IdeTextSecondary
import com.example.ui.theme.IdeWhite
import kotlinx.coroutines.delay

@Composable
fun AnimatedSplashScreen(
  onDismiss: () -> Unit
) {
  val logoScale = remember { Animatable(0.4f) }
  val logoAlpha = remember { Animatable(0f) }
  var showContent by remember { mutableStateOf(false) }
  var showDeveloperBadge by remember { mutableStateOf(false) }
  var progressStatusIndex by remember { mutableIntStateOf(0) }

  val statusMessages = listOf(
    "Initializing LLVM / Clang Environment...",
    "Loading C++ Standard Libraries & libc++...",
    "Configuring Mobile Code Workspace...",
    "Launching GM'S c++ IDE..."
  )

  // Infinite transition for pulse ring
  val infiniteTransition = rememberInfiniteTransition(label = "pulse")
  val pulseScale by infiniteTransition.animateFloat(
    initialValue = 1f,
    targetValue = 1.35f,
    animationSpec = infiniteRepeatable(
      animation = tween(1200, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "pulseScale"
  )

  LaunchedEffect(Unit) {
    logoAlpha.animateTo(1f, animationSpec = tween(400))
    logoScale.animateTo(1f, animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f))
    showContent = true
    delay(300)
    showDeveloperBadge = true

    // Cycle through status messages
    for (i in 0 until statusMessages.size) {
      progressStatusIndex = i
      delay(650)
    }
    delay(400)
    onDismiss()
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(IdeWhite)
      .statusBarsPadding()
      .navigationBarsPadding()
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onDismiss
      )
      .testTag("splash_screen_container"),
    contentAlignment = Alignment.Center
  ) {
    // Background subtle geometric glow
    Box(
      modifier = Modifier
        .size(360.dp)
        .scale(pulseScale)
        .clip(CircleShape)
        .background(
          Brush.radialGradient(
            colors = listOf(
              IdeBlueLight.copy(alpha = 0.08f),
              IdeCyanAccent.copy(alpha = 0.03f),
              Color.Transparent
            )
          )
        )
    )

    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 24.dp)
    ) {
      // 1. App Icon with C++ branding & pulse ring
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .size(130.dp)
          .scale(logoScale.value)
      ) {
        // Outer glow circle
        Box(
          modifier = Modifier
            .size(126.dp)
            .clip(RoundedCornerShape(34.dp))
            .background(
              Brush.linearGradient(
                colors = listOf(IdeBluePrimary, IdeCyanAccent)
              )
            )
            .shadow(16.dp, RoundedCornerShape(34.dp), ambientColor = IdeBluePrimary, spotColor = IdeCyanAccent)
        )

        // Inner white plate
        Surface(
          modifier = Modifier.size(118.dp),
          shape = RoundedCornerShape(30.dp),
          color = IdeWhite,
          shadowElevation = 4.dp
        ) {
          Box(contentAlignment = Alignment.Center) {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.Center
            ) {
              Text(
                text = "C++",
                fontSize = 38.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                color = IdeBluePrimary
              )
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
              ) {
                Icon(
                  imageVector = Icons.Default.Terminal,
                  contentDescription = null,
                  tint = IdeCyanAccent,
                  modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                  text = "IDE",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 2.sp,
                  color = IdeCyanAccent
                )
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(28.dp))

      // 2. App Name & Title
      AnimatedVisibility(
        visible = showContent,
        enter = fadeIn(tween(400)) + slideInVertically(spring(dampingRatio = 0.7f)) { 40 }
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(
            text = "GM'S c++",
            fontSize = 36.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-0.5).sp,
            color = IdeTextPrimary,
            textAlign = TextAlign.Center
          )

          Spacer(modifier = Modifier.height(6.dp))

          Text(
            text = "Professional C++ Mobile Compiler & Runner",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = IdeTextSecondary,
            textAlign = TextAlign.Center
          )
        }
      }

      Spacer(modifier = Modifier.height(36.dp))

      // 3. Developer Card (Sir Ghulam Mustafa)
      AnimatedVisibility(
        visible = showDeveloperBadge,
        enter = fadeIn(tween(500)) + slideInVertically(spring(dampingRatio = 0.65f)) { 60 }
      ) {
        Card(
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = IdeWhite),
          elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
          modifier = Modifier
            .fillMaxWidth(0.92f)
            .border(
              width = 1.dp,
              brush = Brush.horizontalGradient(
                colors = listOf(IdeBlueLight.copy(alpha = 0.5f), IdeCyanAccent.copy(alpha = 0.5f))
              ),
              shape = RoundedCornerShape(20.dp)
            )
            .testTag("developer_badge_card")
        ) {
          Row(
            modifier = Modifier
              .padding(vertical = 16.dp, horizontal = 18.dp)
              .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(
                  Brush.linearGradient(
                    listOf(IdeBluePrimary, IdeCyanAccent)
                  )
                ),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Code,
                contentDescription = null,
                tint = IdeWhite,
                modifier = Modifier.size(24.dp)
              )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  text = "Lead Developer & Architect",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 0.8.sp,
                  color = IdeCyanAccent
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                  imageVector = Icons.Default.Verified,
                  contentDescription = "Verified Developer",
                  tint = IdeBluePrimary,
                  modifier = Modifier.size(15.dp)
                )
              }
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = "Sir Ghulam Mustafa",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = IdeTextPrimary
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(48.dp))

      // 4. Progress and Loading Status
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(0.75f)
      ) {
        LinearProgressIndicator(
          modifier = Modifier
            .fillMaxWidth()
            .height(5.dp)
            .clip(RoundedCornerShape(3.dp)),
          color = IdeBluePrimary,
          trackColor = IdeBlueLight.copy(alpha = 0.15f)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
          text = statusMessages.getOrElse(progressStatusIndex) { "Loading..." },
          fontSize = 12.sp,
          fontFamily = FontFamily.Monospace,
          color = IdeTextMuted,
          textAlign = TextAlign.Center
        )
      }
    }

    // Skip notice at bottom
    Text(
      text = "Tap to skip",
      fontSize = 11.sp,
      color = IdeTextMuted,
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .padding(bottom = 24.dp)
    )
  }
}
