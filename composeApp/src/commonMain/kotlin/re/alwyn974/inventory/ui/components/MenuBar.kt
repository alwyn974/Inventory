package re.alwyn974.inventory.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuBar(
    onManageCategories: () -> Unit,
    onManageTags: () -> Unit,
    onManageFolders: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Manage Categories Button
            OutlinedButton(
                onClick = onManageCategories,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    Icons.Default.Category,
                    contentDescription = "Categories",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Categories")
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Manage Tags Button
            OutlinedButton(
                onClick = onManageTags,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    Icons.Default.Tag,
                    contentDescription = "Tags",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Tags")
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Manage Folders Button
            OutlinedButton(
                onClick = onManageFolders,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    Icons.Default.Folder,
                    contentDescription = "Folders",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Folders")
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Logout Button
            Button(
                onClick = onLogout,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = "Logout",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Logout")
            }
        }
    }
}
