package com.sglenn.dosepulse.ui.database

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sglenn.dosepulse.data.MedicationReference
import com.sglenn.dosepulse.ui.theme.*
import com.sglenn.dosepulse.ui.viewmodel.DashboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationDatabaseScreen(
    viewModel: DashboardViewModel,
    onMedicationSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val references by viewModel.filteredReferences.collectAsState()
    val categories by viewModel.referenceCategories.collectAsState(initial = emptyList())
    val searchQuery by viewModel.searchQuery.collectAsState()

    var selectedCategory by remember { mutableStateOf<String?>(null) }

    val displayList = remember(references, selectedCategory) {
        if (selectedCategory == null) references
        else references.filter { it.category == selectedCategory }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Medication Guide",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = TextPrimaryLight
                        )
                        Text(
                            text = "Offline general reference catalog",
                            fontSize = 11.sp,
                            color = TextSecondaryLight
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SoftMistSlate)
            )
        },
        containerColor = SoftMistSlate
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Text(
                text = "Reference information only. Use the dose and instructions on your prescription label and ask your pharmacist or clinician about medication questions.",
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            )
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Search by name (e.g. Lisinopril, Metformin)...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MedicalTeal) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = SurfaceWhite,
                    focusedContainerColor = SurfaceWhite
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Category Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { selectedCategory = null },
                    label = { Text("All (${references.size})") },
                    shape = RoundedCornerShape(10.dp)
                )

                categories.forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = {
                            selectedCategory = if (selectedCategory == cat) null else cat
                        },
                        label = { Text(cat) },
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Reference Drug List
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(displayList, key = { it.id }) { ref ->
                    ReferenceMedCard(
                        ref = ref,
                        onAddAsMyMed = { onMedicationSelected(ref.name.substringBefore(" (")) }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
fun ReferenceMedCard(
    ref: MedicationReference,
    onAddAsMyMed: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val pillColor = Color(ref.suggestedColorHex)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, OutlineBorder, RoundedCornerShape(16.dp))
            .clickable { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(pillColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Medication,
                        contentDescription = null,
                        tint = pillColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = ref.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryLight
                    )
                    Text(
                        text = ref.category,
                        fontSize = 12.sp,
                        color = TextSecondaryLight
                    )
                }

                Button(
                    onClick = onAddAsMyMed,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyanContainer,
                        contentColor = MedicalTealDark
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Use name", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Expandable clinical guidance
            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = OutlineBorder)
                Spacer(modifier = Modifier.height(10.dp))

                Text("Common reference strengths (not a dose recommendation):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimaryLight)
                Text(
                    text = ref.commonDosages.joinToString(" • "),
                    fontSize = 12.sp,
                    color = MedicalTealDark,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(6.dp))
                Text("General reference notes:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimaryLight)
                Text(text = ref.intakeGuidance, fontSize = 12.sp, color = TextSecondaryLight)

                if (ref.clinicalWarning.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("⚠️ Catalog caution (verify with your prescription label):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = StatusSnoozed)
                    Text(text = ref.clinicalWarning, fontSize = 12.sp, color = TextSecondaryLight)
                }
            }
        }
    }
}
