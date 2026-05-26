package fr.datasaillance.nightfall.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.LockReset
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.datasaillance.nightfall.ui.components.DsCard
import fr.datasaillance.nightfall.ui.components.DsTopBar
import fr.datasaillance.nightfall.ui.components.StatusChip
import fr.datasaillance.nightfall.ui.components.StatusTone
import fr.datasaillance.nightfall.ui.theme.DataSaillance

@Composable
fun ProfileScreen(
    onImport: () -> Unit = {},
    onSettings: () -> Unit = {},
    onLogout: () -> Unit = {},
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            DsTopBar(title = "Profil")
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Avatar + nom + email + RGPD chip
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                CircleShape,
                            )
                            .border(
                                width = 1.dp,
                                color = DataSaillance.extras.borderStrong,
                                shape = CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "M",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary,  // amber
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Moncef",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DataSaillance.extras.textStrong,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "moncef@datasaillance.fr",
                        style = MaterialTheme.typography.bodySmall,
                        color = DataSaillance.extras.textMuted,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    StatusChip(text = "RGPD · Art.9 chiffré", tone = StatusTone.SUCCESS)
                }

                // Settings list — cards with divider between rows, sans padding container.
                DsCard(pad = 0.dp) {
                    SettingsRow(
                        icon = Icons.Outlined.CloudSync,
                        label = "Sources de données",
                        subtitle = "Samsung · Health Connect",
                        onClick = onImport,
                    )
                    HorizontalDivider(
                        color = DataSaillance.extras.divider,
                        thickness = 1.dp,
                    )
                    SettingsRow(
                        icon = Icons.Outlined.Download,
                        label = "Exporter mes données",
                        subtitle = "JSON · CSV · FHIR",
                    )
                    HorizontalDivider(
                        color = DataSaillance.extras.divider,
                        thickness = 1.dp,
                    )
                    SettingsRow(
                        icon = Icons.Outlined.LockReset,
                        label = "Clés de chiffrement",
                        subtitle = "Rotation : il y a 14 j",
                    )
                    HorizontalDivider(
                        color = DataSaillance.extras.divider,
                        thickness = 1.dp,
                    )
                    SettingsRow(
                        icon = Icons.Outlined.DarkMode,
                        label = "Apparence",
                        subtitle = "Système · sombre · clair",
                    )
                    HorizontalDivider(
                        color = DataSaillance.extras.divider,
                        thickness = 1.dp,
                    )
                    SettingsRow(
                        icon = Icons.Outlined.Settings,
                        label = "Paramètres",
                        subtitle = "URL serveur, debug",
                        onClick = onSettings,
                    )
                    HorizontalDivider(
                        color = DataSaillance.extras.divider,
                        thickness = 1.dp,
                    )
                    SettingsRow(
                        icon = Icons.Outlined.DeleteForever,
                        label = "Effacer mes données",
                        subtitle = "RGPD Art.17",
                        danger = true,
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "Se déconnecter",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    label: String,
    subtitle: String,
    danger: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val labelColor = if (danger) MaterialTheme.colorScheme.error
                     else MaterialTheme.colorScheme.onSurface
    val iconColor = if (danger) MaterialTheme.colorScheme.error
                    else DataSaillance.extras.textMuted
    val rowMod = if (onClick != null) Modifier.clickable { onClick() } else Modifier
    Row(
        modifier = rowMod
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(22.dp),
        )
        Spacer(modifier = Modifier.padding(end = 14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = labelColor,
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = DataSaillance.extras.textMuted,
            )
        }
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = DataSaillance.extras.textFaint,
            modifier = Modifier.size(20.dp),
        )
    }
}
