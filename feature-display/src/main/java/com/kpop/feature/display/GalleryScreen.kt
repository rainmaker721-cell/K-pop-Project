package com.kpop.feature.display

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kpop.core.model.GalleryItem
import com.kpop.core.model.ImageOrigin

@Composable
fun ScanHomeScreen(
    items: List<GalleryItem>,
    onOpenImage: (GalleryItem) -> Unit,
    onImportImages: () -> Unit,
    onStartSlideshow: () -> Unit,
    errorMessage: String?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF08080C), Color(0xFF161021)),
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
        ) {
            Text(
                text = "K-POP PHOTO CARD DISPLAY",
                color = Color(0xFFE759FF),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "포토카드를 스캔하세요",
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "실물 포토카드를 스캔하면 4K급으로 변환해 대형 패드에 자동 전시합니다.",
                color = Color(0xFFBDB7C9),
                style = MaterialTheme.typography.bodyMedium,
            )

            Spacer(Modifier.height(14.dp))

            Surface(
                color = Color(0xFF2B2315),
                shape = RoundedCornerShape(12.dp),
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp)) {
                    Text(
                        text = "스캐너 연동 예정",
                        color = Color(0xFFFFD27A),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "장비 선정 후 USB 스캐너가 연결됩니다. 지금은 ‘테스트 스캔’으로 전체 전시 흐름을 사용할 수 있습니다.",
                        color = Color(0xFFD7C8AA),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onImportImages) {
                    Text("테스트 스캔")
                }
                OutlinedButton(
                    onClick = onStartSlideshow,
                    enabled = items.isNotEmpty(),
                ) {
                    Text("전시 데모")
                }
            }

            Spacer(Modifier.height(22.dp))
            Text(
                text = "최근 스캔 및 데모 카드",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )

            if (errorMessage != null) {
                Spacer(Modifier.height(12.dp))
                Surface(
                    color = Color(0xFF4A1620),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Text(
                        text = errorMessage,
                        color = Color(0xFFFFC2CC),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    )
                }
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 180.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 24.dp,
                end = 24.dp,
                bottom = 28.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(items, key = GalleryItem::id) { item ->
                GalleryCard(item = item, onClick = { onOpenImage(item) })
            }
        }
    }
}

@Composable
private fun GalleryCard(item: GalleryItem, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF211C28)),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Box {
            Artwork(
                item = item,
                contentScale = ContentScale.Crop,
                maximumDecodeDimension = 720,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f),
            )
            Text(
                text = if (item.origin == ImageOrigin.SAMPLE) "DEMO" else "SCAN",
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .background(Color(0xB8000000), RoundedCornerShape(50))
                    .padding(horizontal = 9.dp, vertical = 5.dp),
            )
        }
        Text(
            text = item.title,
            color = Color.White,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
        )
    }
}
