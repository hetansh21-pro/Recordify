package com.audiovault.app.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.audiovault.app.data.model.CountryInfo
import com.audiovault.app.data.model.MusicPreferenceCatalog
import com.audiovault.app.ui.theme.SpotifyBlack
import com.audiovault.app.ui.theme.SpotifyCardSurface
import com.audiovault.app.ui.theme.SpotifyDarkSurface
import com.audiovault.app.ui.theme.SpotifyGreenBright

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
    initialCountryCode: String = "IN",
    initialCountryName: String = "India",
    initialPrimaryLanguage: String = "Hindi",
    initialPreferredLanguages: Set<String> = setOf("Hindi", "Gujarati", "English"),
    initialFavoriteGenres: Set<String> = setOf("Bollywood", "Gujarati", "Romantic", "Pop"),
    initialFavoriteArtists: Set<String> = emptySet(),
    onFinishOnboarding: (
        countryCode: String,
        countryName: String,
        primaryLang: String,
        preferredLangs: Set<String>,
        favoriteGenres: Set<String>,
        favoriteArtists: Set<String>
    ) -> Unit
) {
    var currentStep by remember { mutableIntStateOf(1) } // 1: Country, 2: Languages, 3: Taste

    var selectedCountry by remember {
        mutableStateOf(
            MusicPreferenceCatalog.COUNTRIES.firstOrNull { it.code.equals(initialCountryCode, ignoreCase = true) }
                ?: MusicPreferenceCatalog.getDefaultCountry()
        )
    }

    var selectedLanguages by remember { mutableStateOf(initialPreferredLanguages.toMutableSet()) }
    var primaryLanguage by remember { mutableStateOf(initialPrimaryLanguage) }

    var selectedGenres by remember { mutableStateOf(initialFavoriteGenres.toMutableSet()) }
    var selectedArtists by remember { mutableStateOf(initialFavoriteArtists.toMutableSet()) }
    var customArtistInput by remember { mutableStateOf("") }

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SpotifyBlack)
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        // Step Progress Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (currentStep > 1) {
                IconButton(
                    onClick = { currentStep-- },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(36.dp))
            }

            Text(
                text = "Step $currentStep of 3",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFB3B3B3)
                )
            )

            if (currentStep == 3) {
                TextButton(
                    onClick = {
                        onFinishOnboarding(
                            selectedCountry.code,
                            selectedCountry.name,
                            primaryLanguage,
                            selectedLanguages,
                            selectedGenres,
                            selectedArtists
                        )
                    }
                ) {
                    Text(
                        text = "Skip",
                        color = Color(0xFFB3B3B3),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(36.dp))
            }
        }

        LinearProgressIndicator(
            progress = { currentStep / 3f },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = SpotifyGreenBright,
            trackColor = SpotifyCardSurface
        )

        Spacer(modifier = Modifier.height(16.dp))

        AnimatedContent(
            targetState = currentStep,
            transitionSpec = {
                if (targetState > initialState) {
                    slideInHorizontally { width -> width } + fadeIn() togetherWith
                            slideOutHorizontally { width -> -width } + fadeOut()
                } else {
                    slideInHorizontally { width -> -width } + fadeIn() togetherWith
                            slideOutHorizontally { width -> width } + fadeOut()
                }
            },
            label = "OnboardingStepTransition",
            modifier = Modifier.weight(1f)
        ) { step ->
            when (step) {
                1 -> {
                    // STEP 1: COUNTRY SELECTION
                    CountrySelectionStep(
                        selectedCountry = selectedCountry,
                        onCountrySelected = { selectedCountry = it }
                    )
                }
                2 -> {
                    // STEP 2: LANGUAGE SELECTION
                    LanguageSelectionStep(
                        selectedLanguages = selectedLanguages,
                        primaryLanguage = primaryLanguage,
                        onToggleLanguage = { lang ->
                            val updated = selectedLanguages.toMutableSet()
                            if (updated.contains(lang)) {
                                if (updated.size > 1) {
                                    updated.remove(lang)
                                    if (primaryLanguage == lang) {
                                        primaryLanguage = updated.first()
                                    }
                                }
                            } else {
                                updated.add(lang)
                            }
                            selectedLanguages = updated
                        },
                        onSelectPrimary = { primaryLanguage = it }
                    )
                }
                3 -> {
                    // STEP 3: GENRES & ARTISTS
                    MusicTasteSelectionStep(
                        selectedGenres = selectedGenres,
                        selectedArtists = selectedArtists,
                        customArtistInput = customArtistInput,
                        onCustomArtistChange = { customArtistInput = it },
                        onAddCustomArtist = {
                            val trimmed = customArtistInput.trim()
                            if (trimmed.isNotBlank() && !selectedArtists.contains(trimmed)) {
                                val updated = selectedArtists.toMutableSet()
                                updated.add(trimmed)
                                selectedArtists = updated
                                customArtistInput = ""
                                keyboardController?.hide()
                                focusManager.clearFocus()
                            }
                        },
                        onToggleGenre = { genre ->
                            val updated = selectedGenres.toMutableSet()
                            if (updated.contains(genre)) {
                                if (updated.size > 1) updated.remove(genre)
                            } else {
                                updated.add(genre)
                            }
                            selectedGenres = updated
                        },
                        onToggleArtist = { artist ->
                            val updated = selectedArtists.toMutableSet()
                            if (updated.contains(artist)) {
                                updated.remove(artist)
                            } else {
                                updated.add(artist)
                            }
                            selectedArtists = updated
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Bottom Action Button
        Button(
            onClick = {
                if (currentStep < 3) {
                    currentStep++
                } else {
                    onFinishOnboarding(
                        selectedCountry.code,
                        selectedCountry.name,
                        primaryLanguage,
                        selectedLanguages,
                        selectedGenres,
                        selectedArtists
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreenBright),
            shape = RoundedCornerShape(26.dp)
        ) {
            Text(
                text = if (currentStep < 3) "Continue" else "Start Listening",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun CountrySelectionStep(
    selectedCountry: CountryInfo,
    onCountrySelected: (CountryInfo) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredCountries = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            MusicPreferenceCatalog.COUNTRIES
        } else {
            MusicPreferenceCatalog.COUNTRIES.filter {
                it.name.contains(searchQuery.trim(), ignoreCase = true) ||
                        it.code.contains(searchQuery.trim(), ignoreCase = true)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Where are you listening?",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Select your country to unlock trending charts, regional releases, and top hits made for your area.",
            style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFFB3B3B3))
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search country...", color = Color(0xFF888888)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = Color(0xFF888888)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear",
                            tint = Color.White
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SpotifyCardSurface,
                unfocusedContainerColor = SpotifyCardSurface,
                focusedBorderColor = SpotifyGreenBright,
                unfocusedBorderColor = Color.Transparent,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(filteredCountries, key = { it.code }) { country ->
                val isSelected = country.code == selectedCountry.code
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) SpotifyDarkSurface else SpotifyCardSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCountrySelected(country) }
                        .border(
                            width = if (isSelected) 1.5.dp else 0.dp,
                            color = if (isSelected) SpotifyGreenBright else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = country.flagEmoji,
                                fontSize = 24.sp
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = country.name,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = Color.White
                                )
                            )
                        }

                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(SpotifyGreenBright),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LanguageSelectionStep(
    selectedLanguages: Set<String>,
    primaryLanguage: String,
    onToggleLanguage: (String) -> Unit,
    onSelectPrimary: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "What languages do you listen to?",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Select all the languages you enjoy. We will curate personalized mixes in your selected languages.",
            style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFFB3B3B3))
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(MusicPreferenceCatalog.LANGUAGES, key = { it.code }) { lang ->
                val isSelected = selectedLanguages.contains(lang.name)
                val isPrimary = primaryLanguage == lang.name

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) SpotifyDarkSurface else SpotifyCardSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleLanguage(lang.name) }
                        .border(
                            width = if (isSelected) 1.5.dp else 0.dp,
                            color = if (isSelected) SpotifyGreenBright else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = lang.name,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                            Text(
                                text = lang.nativeName,
                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFB3B3B3))
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isSelected) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isPrimary) SpotifyGreenBright else SpotifyCardSurface,
                                    modifier = Modifier
                                        .clickable { onSelectPrimary(lang.name) }
                                        .padding(end = 8.dp)
                                ) {
                                    Text(
                                        text = if (isPrimary) "Primary" else "Set Primary",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (isPrimary) Color.Black else Color(0xFFB3B3B3),
                                            fontWeight = FontWeight.Bold
                                        ),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(SpotifyGreenBright),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MusicTasteSelectionStep(
    selectedGenres: Set<String>,
    selectedArtists: Set<String>,
    customArtistInput: String,
    onCustomArtistChange: (String) -> Unit,
    onAddCustomArtist: () -> Unit,
    onToggleGenre: (String) -> Unit,
    onToggleArtist: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Choose your favorite music",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Pick genres and artists you love to train your personalized Made for You recommendation feed.",
                style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFFB3B3B3))
            )
        }

        // GENRES SECTION
        item {
            Text(
                text = "FAVORITE GENRES",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
            )
            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MusicPreferenceCatalog.GENRES.forEach { genre ->
                    val isSelected = selectedGenres.contains(genre.name)
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) SpotifyGreenBright else SpotifyCardSurface,
                        modifier = Modifier
                            .clickable { onToggleGenre(genre.name) }
                            .border(
                                width = if (isSelected) 0.dp else 1.dp,
                                color = Color(0x33FFFFFF),
                                shape = RoundedCornerShape(20.dp)
                            )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = genre.name,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.Black else Color.White
                                )
                            )
                            if (isSelected) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // ARTISTS SECTION
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "FAVORITE ARTISTS",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Add custom artist input
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = customArtistInput,
                    onValueChange = onCustomArtistChange,
                    placeholder = { Text("Add artist (e.g. Taylor Swift)", color = Color(0xFF888888)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onAddCustomArtist() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SpotifyCardSurface,
                        unfocusedContainerColor = SpotifyCardSurface,
                        focusedBorderColor = SpotifyGreenBright,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = onAddCustomArtist,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(SpotifyGreenBright)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Artist",
                        tint = Color.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Pre-populated catalog artists + user added artists
                val allArtistOptions = (selectedArtists.toList() + MusicPreferenceCatalog.POPULAR_ARTISTS.map { it.name }).distinct()

                allArtistOptions.forEach { artistName ->
                    val isSelected = selectedArtists.contains(artistName)
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) SpotifyGreenBright else SpotifyCardSurface,
                        modifier = Modifier
                            .clickable { onToggleArtist(artistName) }
                            .border(
                                width = if (isSelected) 0.dp else 1.dp,
                                color = Color(0x33FFFFFF),
                                shape = RoundedCornerShape(20.dp)
                            )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = artistName,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.Black else Color.White
                                )
                            )
                            if (isSelected) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
