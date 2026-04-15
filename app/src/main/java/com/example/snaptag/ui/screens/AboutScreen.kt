package com.example.snaptag.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.snaptag.ui.components.TopBar

@Composable
fun AboutScreen() {
    Scaffold(
        topBar = { TopBar("About SnapTag") }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Tag,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(80.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "SnapTag",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Stock Management Reimagined",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(32.dp))
            AboutSection(
                icon = Icons.Default.Description,
                title = "Description",
                description = "SnapTag is a powerful inventory tool that uses OCR (Optical Character Recognition) to quickly scan and manage product data. Streamline your workflow by extracting prices directly from tags."
            )
            Spacer(modifier = Modifier.height(24.dp))
            AboutSection(
                icon = Icons.Default.PrivacyTip,
                title = "Privacy",
                description = "We value your privacy. All scans and data are processed locally on your device. We do not store or transmit any personal information to external servers."
            )
            Spacer(modifier = Modifier.height(24.dp))
            AboutSection(
                icon = Icons.AutoMirrored.Filled.Help,
                title = "Support",
                description = "Need help? Contact our support team at support@snaptag.app or visit our website for more information and FAQs."
            )
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "© 2024 SnapTag. All rights reserved.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun AboutSection(
    icon: ImageVector,
    title: String,
    description: String
) {
    Column(horizontalAlignment = Alignment.Start) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Justify
        )
    }
}
