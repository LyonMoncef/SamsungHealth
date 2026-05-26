package fr.datasaillance.nightfall.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.datasaillance.nightfall.ui.theme.DataSaillance

/**
 * Composables partagés inspirés du design system DataSaillance (Nightfall kit).
 * Mirrors `Atoms.jsx` du kit : Eyebrow, DsCard, StatusChip, DsTopBar, SaillanceBanner.
 *
 * Convention :
 * - "Ds" prefix pour éviter les conflits avec Material 3 (Card, TopAppBar)
 * - Tokens : `MaterialTheme.colorScheme.*` + `DataSaillance.extras.*` uniquement
 * - Borders 1dp hairline systématiques (pas de shadow/elevation glow)
 */

/** Eyebrow — petit label en uppercase tracked, au-dessus des titres et sections. */
@Composable
fun Eyebrow(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = DataSaillance.extras.textMuted,
) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.5.sp,
        color = color,
    )
}

/** TopBar fixe 56dp — title + optionnel eyebrow + actions. */
@Composable
fun DsTopBar(
    title: String,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    leading: @Composable (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            Box(modifier = Modifier.defaultMinSize(minWidth = 40.dp)) { leading() }
        } else {
            Spacer(modifier = Modifier.padding(end = 40.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            if (eyebrow != null) {
                Eyebrow(text = eyebrow)
                Spacer(modifier = Modifier.height(2.dp))
            }
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = DataSaillance.extras.textStrong,
            )
        }
        if (trailing != null) trailing()
    }
}

/** Card design system — surface + 1dp border, padding configurable. */
@Composable
fun DsCard(
    modifier: Modifier = Modifier,
    pad: Dp = 16.dp,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(modifier = Modifier.padding(pad)) { content() }
    }
}

/** Status chip pill — tone success / warning / danger / info. */
enum class StatusTone { SUCCESS, WARNING, DANGER, INFO }

@Composable
fun StatusChip(
    text: String,
    tone: StatusTone = StatusTone.INFO,
    modifier: Modifier = Modifier,
) {
    val (fg, bg) = when (tone) {
        StatusTone.SUCCESS -> MaterialTheme.colorScheme.tertiary.copy(alpha = 1f) to
            DataSaillance.extras.success.copy(alpha = 0.16f)
        StatusTone.WARNING -> DataSaillance.extras.warning to
            DataSaillance.extras.warning.copy(alpha = 0.16f)
        StatusTone.DANGER -> MaterialTheme.colorScheme.error to
            MaterialTheme.colorScheme.error.copy(alpha = 0.16f)
        StatusTone.INFO -> DataSaillance.extras.highlight to
            DataSaillance.extras.highlightSoft
    }
    Box(
        modifier = modifier
            .background(bg, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .heightIn(min = 22.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp,
            color = fg,
        )
    }
}

/**
 * Saillance banner — icône info + titre + sous-texte, fond cyan soft.
 * Style narratif pour annoncer un signal "qui sort du brouillard".
 */
@Composable
fun SaillanceBanner(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                DataSaillance.extras.highlightSoft,
                RoundedCornerShape(12.dp),
            )
            .padding(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .padding(end = 12.dp, top = 2.dp)
                .background(
                    DataSaillance.extras.highlight.copy(alpha = 0.4f),
                    RoundedCornerShape(4.dp),
                )
                .padding(2.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = DataSaillance.extras.textStrong,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = DataSaillance.extras.textMuted,
            )
        }
    }
}

/** KPI tile — Eyebrow + valeur 32sp + delta. Pour rangées de 2-3 tiles. */
@Composable
fun KpiTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    unit: String? = null,
    delta: String? = null,
    deltaTone: DeltaTone = DeltaTone.MUTED,
) {
    val deltaColor = when (deltaTone) {
        DeltaTone.UP -> DataSaillance.extras.success
        DeltaTone.DOWN -> MaterialTheme.colorScheme.error
        DeltaTone.ACCENT -> MaterialTheme.colorScheme.secondary
        DeltaTone.MUTED -> DataSaillance.extras.textMuted
    }
    DsCard(modifier = modifier) {
        Eyebrow(text = label)
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.4).sp,
                color = DataSaillance.extras.textStrong,
            )
            if (unit != null) {
                Spacer(modifier = Modifier.padding(end = 6.dp))
                Text(
                    text = unit,
                    fontSize = 13.sp,
                    color = DataSaillance.extras.textMuted,
                )
            }
        }
        if (delta != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = delta,
                fontSize = 12.sp,
                color = deltaColor,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

enum class DeltaTone { UP, DOWN, ACCENT, MUTED }
